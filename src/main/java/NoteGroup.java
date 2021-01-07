import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

// A NoteGroup consists of two Objects:
//      1.  Group Properties
//      2.  Vector of NoteData
// Its properties are the base Properties class and its vector holds NoteData type notes.
// Children of this class have specialized GroupProperties and NoteData.

@SuppressWarnings("rawtypes")
class NoteGroup implements LinkHolder {
    private static final Logger log = LoggerFactory.getLogger(NoteGroup.class);

    NoteGroupDataAccessor groupDataAccessor; // Provides a way to persist and retrieve the Group data

    // Currently we don't save a NoteGroup, but we do save its relevant content.
    // And since that is true, the use of transient or not - is meaningless.  But I'm using it anyway
    // (in unexplained, inconsistent places) because things change.
    transient boolean groupChanged;  // Flag used to determine if saving data might be necessary.

    // This Vector holds the complete collection of group notes.
    // The Notes will either be of class NoteData or one of its children; each child of this class
    //   will set for itself the type of note held here, hence the rawtypes warning suppress.
    Vector noteGroupDataVector;

    protected GroupProperties myProperties; // All children can access this directly, but better to use a getter.
    protected GroupInfo myGroupInfo;

    NoteGroupPanel myNoteGroupPanel; // This might remain null; depends on usage of the NoteGroup.


    NoteGroup(GroupInfo groupInfo) {
        log.debug("Constructing NoteGroup (base class) for: " + groupInfo.groupType);
        myGroupInfo = groupInfo; // Everything else comes from this.

        // Initialize the data.  (myProperties is already null at this point)
        noteGroupDataVector = new Vector<>(0, 1);

        // Get a data accessor for this group.
        groupDataAccessor = MemoryBank.dataAccessor.getNoteGroupDataAccessor(groupInfo);  // currently this can only be a new NoteGroupFile.

        // Load the group data (using the accessor).
        BaseData.loading = true;
        loadNoteGroup();
        // For all NoteGroup children, if we have a loaded data set then we will be able to deserialize our
        //   properties from that data.  But when there IS no persisted data then the GroupProperties will remain
        //   null after a data load, so the condition below takes care of that, right?
        // Well no, not completely.  All NoteGroups get constructed with their group name, but the
        //   CalendarNoteGroup (CNG) types get a new name every time the date changes, and yet when they are used
        //   in Panels they only get constructed upon the first access of the data type.  The 'fix' below will only
        //   happen during construction; it does not happen again when the group is loaded via the updateGroup method,
        //   primarily because that would be a lot of unneeded effort when the group is not a CNG type because
        //   any other group type needing 'update' will be known to have available persisted data, whereas the
        //   CNGs may not.
        // So what do we do for a CNG type group after it got updated and has no properties?  Nothing, until something
        //   changes and it needs saving.  At that time, the CalendarNoteGroupPanel override of getPanelData will set
        //   the GroupProperties correctly so that the data is not persisted with a null in that data member.
        // Now you ask why am I talking about Panels in this class?  Mainly because Panels are the primary customer of
        //   the NoteGroup constructor, and CNG type Panels are the ones trying to retrieve Groups that have no data.
        BaseData.loading = false;

        // Make the basic default properties, if none were loaded.
        if(myProperties == null) {
            myProperties = makeGroupProperties(); // This method might be overridden, in child classes of NoteGroup.
        }
    }

    @SuppressWarnings("unchecked")
    // A NoteData or any one of its children can be sent here.
    void addNote(NoteData noteData) {
        noteGroupDataVector.add(noteData);
        noteData.setMyNoteGroup(this);
        setGroupChanged(true);
    }

    // This is used by data-accessor contexts, prior to saving.  But tests may also use it.
    public Object[] getTheData() {
        Object[] theData = new Object[2];
        theData[0] = getGroupProperties();
        theData[1] = noteGroupDataVector;
        return theData;
    }


    // Make a 'reverse' link from a 'forward' one where this
    // entity is the source; in the return link it will be the target.
    // The calling context will attach the result to the correct LinkHolder.
    public LinkedEntityData createReverseLink(LinkedEntityData linkedEntityData) {
        LinkedEntityData reverseLinkedEntityData;

        // Now we need to get our properties.  Direct access does not work correctly for all
        // NoteGroup children, so properties are acquired via the (possibly overridden) getter.
        GroupProperties groupProperties = getGroupProperties();

        // We need to be sure that we have non-null properties.
        assert groupProperties != null;

        // But we don't actually want the full Properties; just the info from them, so -
        GroupInfo groupInfo = new GroupInfo(groupProperties); // This strips off the linkTargets.

        // And now we can start making the reverse link.
        // Initially it will look just like a standard 'forward' link from this group
        reverseLinkedEntityData = new LinkedEntityData(groupInfo, null);

        // But now - give it the same ID as the forward one.  This will help with any
        // subsequent operations where the two will need to be 'in sync'.
        reverseLinkedEntityData.instanceId = linkedEntityData.instanceId;

        // Then give it a type that is the reverse of the forward link's type -
        reverseLinkedEntityData.linkType = linkedEntityData.reverseLinkType(linkedEntityData.linkType);

        // and then raise the 'reversed' flag.
        reverseLinkedEntityData.reversed = true;

        // It might appear that the type of this 'new' link could changed, but the editor panel will stop that when
        // it is displayed because this reversed link will already be in the list of links to be shown, whereas
        // links that are truly 'new' will have only been created by the panel while it is active.
        // Point being - there is no need to set 'reverseLinkedEntityData.retypeMe' to false.
        return reverseLinkedEntityData;
    }

    // By 'saving' a null in place of the data, the Group is removed.
    void deleteNoteGroup() {
        groupDataAccessor.deleteNoteGroupData();
    }

    boolean exists() {
        return groupDataAccessor.exists();
    }

    // We don't provide a getGroupInfo(); if you need that, use the GroupInfo copy constructor with the
    //    GroupProperties as the input param, and use getGroupProperties to get them, if you need to.


    ArrayList getGroupNames() {
        return groupDataAccessor.getGroupNames();
    }


    // All higher contexts that need this info are encouraged to use this getter to retrieve 'myProperties'.
    // They may have direct access to 'myProperties', but this ensures uniform access, one-stop shopping.
    public GroupProperties getGroupProperties() {
        // If we loaded our properties member from a data store then we need to use that one.
        //   (it may already contain linkages).
        if(myProperties != null) return myProperties;

        // But a CalendarNoteGroup has a different GroupProperties for every choice.  They can be set at construction
        // but then they get nulled out when there is an attempt to load new data.  But if the specified data is not
        // there, the properties remain null.  That is when this part is needed.
        switch(myGroupInfo.groupType) {
            case DAY_NOTES:
                setGroupProperties(new GroupProperties(myGroupInfo.getGroupName(), GroupType.DAY_NOTES));
                break;
            case MONTH_NOTES:
                setGroupProperties(new GroupProperties(myGroupInfo.getGroupName(), GroupType.MONTH_NOTES));
                break;
            case YEAR_NOTES:
                setGroupProperties(new GroupProperties(myGroupInfo.getGroupName(), GroupType.YEAR_NOTES));
                break;
        }
        return myProperties;
    }



    // Given a NoteInfo with an ID that matches a note in this group, find and return a reference
    // to that note's link targets.  Any changes that the calling context then makes via that
    // reference, WILL be seen in the note in this group.
    @SuppressWarnings("rawtypes")
    public LinkTargets getLinkTargets(NoteInfo noteInfo) {
        String theNoteId = noteInfo.noteId.toString();
        for(Object vectorObject: noteGroupDataVector) {
            NoteData noteData = (NoteData) vectorObject;
            // For this 'search' we cannot rely on the BaseData.equals() method because the classes are different.
            // So - we go down to the level of their IDs.

            // OR - cast to a NoteInfo in the loop, then when 'found', recast so you can access
            // and return the linkTargets.  If that works then this same logic could be used to return the
            // full NoteData, if you have a need for that.  just sayin..
            // (but I don't have the time right now to try this; other areas are in more critical condition).
            if(noteData.instanceId.toString().equals(theNoteId)) return noteData.linkTargets;
        }
        return null;
    }

    void loadNoteGroup() {
        // First, load the raw data (if any) for the group.  If not then theData remains null.
        Object[] theData = groupDataAccessor.loadNoteGroupData(myGroupInfo);

        // Now get any pre-existing data members cleared out to make way for whatever came in (if anything) from the
        // data store.  This is important even when no new data came in, so there are no 'leftovers'.
        noteGroupDataVector.clear(); // Clear, for a zero-length, will be better than 'null'.
        setGroupProperties(null); // If no persisted data came in then the calling context can set new properties.

        if(theData == null) return;
        // Not that uncommon; many CalendarNote groups will have no data to load.
        // There is also still the (theoretical) possibility that theData is not null but is an array of zero length,
        //   but operationally we have no such logical code path that would result in that situation, so not going
        //   to try to handle it here.  If I'm wrong then we will see a failure here and I'll look into it at that time.

        // Now the length can either be 1 or 2.  Regardless of which, the LAST element will be the Notes for the group.
        int theLength = theData.length;
        int notesLoaded = ((List) theData[theLength - 1]).size();
        MemoryBank.debug("  Count of notes loaded: " + notesLoaded);
        //System.out.println("NoteGroup data from JSON file: " + AppUtil.toJsonString(theData));

        // The reason that the data length could be 1 OR 2 is that previously a NoteGroup did not necessarily have
        // properties; it was a possibility, not a certainty like it is now.  So there are several years-worth of data
        // files already out there, where the only element is the group data, and rather than attempting to fix old
        // data, the decision is to examine the content first, then load the correct type.  We can revisit this
        // variant if/when/after the data is ever cleaned up, possibly via a port from files to database.

        // The 'set' methods below will be overridden by child classes so they can set the proper data type.
        if (theLength == 1) { // Then this is old, legacy data that was originally saved without GroupProperties.
            setNotes(theData[0]);
        } else { // then theLength == 2 (or more, but we only know of two, for now)
            // GroupProperties can be much more than just the GroupInfo, so this is definitely needed.
            setGroupProperties(theData[0]);

            // But across a few variants of earlier data, there can be inaccuracies with groupName and groupType.
            // So we just overwrite those two with what we 'know' to be correct.
            myProperties.setGroupName(myGroupInfo.getGroupName());
            myProperties.groupType = myGroupInfo.groupType;

            // And finally, set all the Notes in the group.
            setNotes(theData[1]);
        }
        setGroupChanged(false); // After a fresh load, no changes.
    } // end loadNoteGroup


    GroupProperties makeGroupProperties() {
        return new GroupProperties(myGroupInfo.getGroupName(), myGroupInfo.groupType);
    }


    void saveNoteGroup() {
        groupDataAccessor.saveNoteGroupData(getTheData());
        setGroupChanged(false); // The 'save' preserved all changes to this point (we hope), so we reset the flag.
        // Note that we didn't check the result of the save.  A few reasons for that; primarily because the 'happy'
        // path would have been successful and a success needs no further attention.  In the unsuccessful case we don't
        // have a lot of remediation options; we wouldn't want to halt execution or attempt interaction with the user
        // because there is nothing to be done about it and processing continues in any case.  Simply notifying the
        // user that there was some problem would have limited value but saving is an operation that happens often,
        // sometimes per user action and sometimes automatically, and not all of them will be a good time to
        // stop and complain to the user with an error dialog that they must review and dismiss.  So effectively
        // we just don't need to know.
        //   Future - we may be able to tell the difference between automatic and user-directed save operations and
        //   act accordingly, maybe with another param to this method.  not all operations come directly here; there
        //   is also the preClose code for Panels that calls this one, and that one could also be auto or user directed.
        //
        // But at least when errors are encountered they are trapped and printed to the screen.
        // Status is set at various points; calling contexts that 'care' about the status should check it and adjust
        // their operations according those status values.
    }


    public void setGroupChanged(boolean b) {
        groupChanged = b;
        if(myProperties != null) myProperties.touchLastMod();
    } // end setGroupChanged


    public void setGroupProperties(GroupProperties groupProperties) {
        myProperties = groupProperties;
//        setGroupChanged(true);
    }


    // This method is called with the raw data that is the GroupProperties.
    // Child groups with properties that are children of GroupProperties should override
    // so they can convert it to the correct child type.
    // This 'set' method should not affect the Last Mod date of the group.
    protected void setGroupProperties(Object propertiesObject) {
        myProperties = AppUtil.mapper.convertValue(propertiesObject, GroupProperties.class);
        //myGroupInfo = new GroupInfo(myProperties);  // Set GroupInfo per the data that came from storage.
        // If we do that ^^^ then when the file data does not match the tree node text, no (or wrong) data gets loaded.
        // But this all seems wrong; may want to re-enable that line...
    }



    // Learned how to do this (convert an ArrayList element that is a LinkedHashMap, to a Vector of NoteData),
    // from: https://stackoverflow.com/questions/15430715/casting-linkedhashmap-to-complex-object
    // Previously, I just cycled thru the LinkedHashMap by accepting the entries as Object, then converted them
    // to JSON string, then parsed the string back in to a NoteData and added it to a new Vector.  But that was
    // a several-line method; this conversion is a one-liner, and my version had the possibility of throwing an
    // Exception that needed to be caught.
    //
    // This method is called with the raw data that is the data Vector.
    // Child groups with notes that are children of NoteData should override.
    protected void setNotes(Object vectorObject) {
        noteGroupDataVector = AppUtil.mapper.convertValue(vectorObject, new TypeReference<Vector<NoteData>>() { });
    }


    public void setNotes(Vector<NoteData> incomingNotes) {
        if(incomingNotes == null) {
            noteGroupDataVector.clear(); // null not allowed here.
        } else {
            noteGroupDataVector = incomingNotes;
        }
//        setGroupChanged(true);
    }


// Use this if working on serialization; otherwise leave disabled.
//    public static void main(String[] args) {
//        NoteGroup noteGroup = new NoteGroup();
//        System.out.println(AppUtil.toJsonString(noteGroup));
//
//    }

}
