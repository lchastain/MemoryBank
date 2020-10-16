import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Vector;

// A NoteGroup consists of two Objects:
//      1.  Group Properties
//      2.  Vector of NoteData
// Children of this class have specialized GroupProperties and NoteData.

@SuppressWarnings("rawtypes")
class NoteGroup {
    NoteGroupDataAccessor dataAccessor; // Provides the way to persist and retrieve the Group data

    // Container for the (complete collection of) Group data objects.
    // It may hold more than the PAGE_SIZE number of visible notes.
    Vector noteGroupDataVector;

    private Object[] theData;
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

    public void setGroupProperties(GroupProperties groupProperties) {
//        // Need to keep these two in sync, but even better would be to consolidate...
//        theData[0] = groupProperties;
        myProperties = groupProperties;
    }

    public void add(Vector<NoteData> incomingNotes) {
        noteGroupDataVector = incomingNotes;
    }

    public Object[] getTheData() {
        theData[0] = myProperties;
        theData[1] = noteGroupDataVector;
        return theData;
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


    void setTheData(Object[] theData) {
        this.theData = theData;
    }

    public static void main(String[] args) {
        NoteGroup noteGroup = new NoteGroup();
        System.out.println(AppUtil.toJsonString(noteGroup));

    }

}
