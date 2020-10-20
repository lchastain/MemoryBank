import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

// A NoteGroup consists of two Objects:
//      1.  Group Properties
//      2.  Vector of NoteData
// Children of this class have specialized GroupProperties and NoteData.

@SuppressWarnings("rawtypes")
class NoteGroup implements LinkHolder {
    NoteGroupDataAccessor dataAccessor; // Provides a way to persist and retrieve the Group data
    transient boolean groupChanged;  // Flag used to determine if saving data might be necessary.

    // This Vector holds the complete collection of group notes.
    // The Notes will either be of class NoteData or one of its children; each child of this class
    //   will set for itself the type of note held here, hence the rawtypes warning suppress.
    Vector noteGroupDataVector;

    private Object[] theData; // (currently) we don't save a NoteGroup, but we do save its relevant content.
    // And since that is true, the use of transient or not - is meaningless.  But I'm using it anyway
    // (in unexplained, inconsistent places) because things change.

    TypeReference myGroupDataType;
    protected GroupProperties myProperties; // All children can access this directly, but CNGPs use a getter.

    // A NoteGroupPanel extends (possibly indirectly) from NoteGroup (this class), so this member adds a grandchild
    // to our class definition, which other experts will call a poor design.  Sorry, my bad.  To somewhat atone, we
    // keep this reference in a transient member.  Then we populate it with the grandchild if/when it comes into scope.
    transient NoteGroupPanel myNoteGroupPanel;

    public NoteGroup() {
        super();
        theData = new Object[2];
    }

    public void add(Vector<NoteData> incomingNotes) {
        noteGroupDataVector = incomingNotes;
    }


    @SuppressWarnings("unchecked")
    // A NoteData or any one of its children can be sent here.
    void addNote(NoteData noteData) {
        noteGroupDataVector.add(noteData);
        noteData.setMyNoteGroup(this);
        setGroupChanged(true);
    }

    public Object[] getTheData() {
        theData[0] = myProperties;
        theData[1] = noteGroupDataVector;
        return theData;
    }


    // Make a 'reverse' link from a 'forward' one where this
    // entity WAS the source; now it will be the target.
    // The calling context will attach the result to the correct LinkHolder.
    public LinkedEntityData createReverseLink(LinkedEntityData linkedEntityData) {
        LinkedEntityData reverseLinkedEntityData;

        // From this group we can get its properties -
        GroupProperties groupProperties = getGroupProperties();

        // We need to be sure that we have non-null properties.
        assert groupProperties != null;

        // But we don't actually want the full Properties; just the info from them, so -
        GroupInfo groupInfo = new GroupInfo(groupProperties);

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

        return reverseLinkedEntityData;
    }


    public GroupProperties getGroupProperties() {
        // The preference is to recreate the properties each time from loaded data, if there is any.
        if(theData[0] != null) {
            String theClass = theData[0].getClass().getName();
            if (theClass.equals("java.util.LinkedHashMap")) {
                myProperties = AppUtil.mapper.convertValue(theData[0], new TypeReference<GroupProperties>() {});
            }
        }

        return myProperties;
    }

    // Given a NoteInfo with an ID that matches a note in this group, find and return a reference
    // to that note's link targets.  Any changes that the calling context then makes via that
    // reference, WILL be seen in the note in this group.
    @SuppressWarnings("rawtypes")
    public LinkTargets getLinkTargets(NoteInfo noteInfo) {
        String theNoteId = noteInfo.instanceId.toString();
        for(Object vectorObject: noteGroupDataVector) {
            NoteData noteData = (NoteData) vectorObject;
            // For this 'search' we cannot rely on the BaseData.equals() method because the classes are different.
            // So - we go down to the level of their IDs.
            if(noteData.instanceId.toString().equals(theNoteId)) return noteData.linkTargets;
        }
        return null;
    }


    @SuppressWarnings("rawtypes")
    public int getNoteCount() {
        Vector theNotes = (Vector) theData[1];
        return theNotes.size();
    }


    @SuppressWarnings("rawtypes")
    boolean isEmpty() {
        if(myProperties != null) {
            if (myProperties.linkTargets.size() > 0) return false;
        }

        if(noteGroupDataVector != null) {
            return noteGroupDataVector.size() <= 0;
        }

        return true;
    }


    public void setGroupChanged(boolean b) {
        if(groupChanged == b) return;
        groupChanged = b;
    } // end setGroupChanged


    public void setGroupProperties(GroupProperties groupProperties) {
        myProperties = groupProperties;
    }


    void setTheData(Object[] theData) {
        this.theData = theData;
    }


    public static void main(String[] args) {
        NoteGroup noteGroup = new NoteGroup();
        System.out.println(AppUtil.toJsonString(noteGroup));

    }

}
