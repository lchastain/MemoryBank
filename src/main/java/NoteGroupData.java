import java.util.Vector;

// Data for a NoteGroup.
// Consists of two Objects, in an Object array:
//      1.  Group Properties
//      2.  Vector of NoteData

class NoteGroupData {
    private final Object[] theData;

    public NoteGroupData() {
        super();
        theData = new Object[2];
    }

    public NoteGroupData(Object[] incomingData) {
        theData = incomingData;  // by reference; be careful with reachbacks.
    }

    public void add(GroupProperties groupProperties) {
        theData[0] = groupProperties;
    }

    public void add(Vector<NoteData> incomingNotes) {
        theData[1] = incomingNotes;
    }

    public Object[] getTheData() {
        return theData;
    }


    @SuppressWarnings("rawtypes")
    public int getNoteCount() {
        Vector theNotes = (Vector) theData[1];
        return theNotes.size();
    }


    @SuppressWarnings("rawtypes")
    boolean isEmpty() {
        if(theData[0] != null) {
            GroupProperties groupProperties = (GroupProperties) theData[0];
            if (groupProperties.linkTargets.size() > 0) return false;
        }

        if(theData[1] != null) {
            Vector theNotes = (Vector) theData[1];
            return theNotes.size() <= 0;
        }

        return true;
    }


    public static void main(String[] args) {
        NoteGroupData noteGroupData = new NoteGroupData();
        System.out.println(AppUtil.toJsonString(noteGroupData));

    }

}
