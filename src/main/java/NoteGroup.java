import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Vector;

// A NoteGroup consists of two Objects:
//      1.  Group Properties
//      2.  Vector of NoteData
// Its properties are the base Properties class and its vector holds NoteData type notes.
// Children of this class have specialized GroupProperties and NoteData.

@SuppressWarnings("rawtypes")
class NoteGroup {
    private static final Logger log = LoggerFactory.getLogger(NoteGroup.class);

    NoteGroupDataAccessor groupDataAccessor; // Provides a way to persist and retrieve the Group data

    // Currently we don't save a NoteGroup (but we do save its relevant content).
    // And since that is true, the use of transient or not - is meaningless.
    // But I'm using it anyway because things can change and it gives a more accurate description of the
    //   usage of the data member.
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
        //   happen during construction; it does not happen again when the group is loaded via the refresh method,
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
        if (myProperties == null) {
            myProperties = makeGroupProperties(); // This method might be overridden, in child classes of NoteGroup.
        }
    }

    @SuppressWarnings("unchecked")
        // A NoteData or any one of its children can be sent here.
    void appendNote(NoteData noteData) {
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

    void deleteNoteGroup() {
        groupDataAccessor.deleteNoteGroupData();
        if (groupDataAccessor.getFailureReason() == null) {
            if (myNoteGroupPanel != null) {
                // Let the Panel know that its data has been deleted.  For a standard Panel this is just a no-op
                //  because it is going away, but Panels that are NoteGroup Groups will want to 'know' so they
                //  can do a bit of cleanup before they disappear.
                myNoteGroupPanel.deletePanel();
            }
        }
    }


    boolean exists() {
        return groupDataAccessor.exists();
    }

    // We don't provide a getGroupInfo(); if you need that, use the GroupInfo copy constructor with the
    //    GroupProperties as the input param, and use getGroupProperties to get them, if you need to.


    // All higher contexts that need this info are encouraged to use this getter to retrieve 'myProperties'.
    // They may have direct access to 'myProperties', but this ensures uniform (debuggable) access, one-stop shopping.
    public GroupProperties getGroupProperties() {
        // If we loaded our properties member from a data store then we need to use that one.
        if (myProperties != null) return myProperties;

        // But a CalendarNoteGroup has a different GroupProperties for every Date choice.  They can be set at
        // construction but then they get nulled out when there is an attempt to load new data due to a change
        // of selected Date.  But if there is no data for the specified new Date,
        // the properties remain null.  That is when this part is needed.
        switch (myGroupInfo.groupType) {
            case DAY_NOTES -> setGroupProperties(new GroupProperties(myGroupInfo.getGroupName(), GroupType.DAY_NOTES));
            case MONTH_NOTES -> setGroupProperties(new GroupProperties(myGroupInfo.getGroupName(), GroupType.MONTH_NOTES));
            case YEAR_NOTES -> setGroupProperties(new GroupProperties(myGroupInfo.getGroupName(), GroupType.YEAR_NOTES));
        }
        return myProperties;
    }


    void loadNoteGroup() {
        // First, load the raw data (if any) for the group.  If not then theData remains null.
        Object[] theData = groupDataAccessor.loadNoteGroupData();

        // Now get any pre-existing data members cleared out to make way for whatever came in (if anything) from the
        // data store.  This is important even when no new data came in, so there are no 'leftovers'.
        noteGroupDataVector.clear(); // Clear, for a zero-length, will be better than 'null'.

        // Reinitialize the properties as well; there may have been a name change.
        makeGroupProperties(); // If persisted data came in then this will be overwritten, below.

        if (theData == null) return;
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


    @SuppressWarnings("unchecked")
        // A NoteData or any one of its children can be sent here.
    void prependNote(NoteData noteData) {
        noteGroupDataVector.add(0, noteData);
        noteData.setMyNoteGroup(this);
        setGroupChanged(true);
    }

    void renameNoteGroup(String renameTo) {
        if (groupDataAccessor.renameNoteGroupData(myGroupInfo.groupType, myGroupInfo.getGroupName(), renameTo)) {
            if (myNoteGroupPanel != null) {
                // Let the Panel know that its NoteGroup data has been renamed.  This will need to cascade through
                //   to panel title and possibly other visual elements.
                myNoteGroupPanel.renamePanel(renameTo);
            }
        }
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
        // But at least when errors are encountered they are trapped and printed to the console.
        // Status is set at various points; calling contexts that 'care' about the status should check it and adjust
        // their operations according those status values.
    }


    public void setGroupChanged(boolean b) {
        groupChanged = b;
        if (myProperties != null) myProperties.touchLastMod();
    } // end setGroupChanged


    // This method is called with the raw data that is the GroupProperties.
    // Child groups with properties that are children of GroupProperties should override
    // so they can convert it to the correct child type.
    // This 'set' method should not affect the Last Mod date of the group.
    protected void setGroupProperties(Object propertiesObject) {
        if (propertiesObject instanceof GroupProperties) {
            myProperties = (GroupProperties) propertiesObject;
        } else {
            myProperties = AppUtil.mapper.convertValue(propertiesObject, GroupProperties.class);
        }
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
    void setNotes(Object vectorObject) {
        if (vectorObject == null) {
            noteGroupDataVector.clear(); // null not allowed here.
        } else if (vectorObject instanceof Vector) {
            noteGroupDataVector = (Vector) vectorObject;
        } else {
            noteGroupDataVector = AppUtil.mapper.convertValue(vectorObject, new TypeReference<Vector<NoteData>>() { });
        }
    }

}
