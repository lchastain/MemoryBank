import java.util.Vector;

// Data for a NoteGroup.
// Consists of two Objects, in an Object array:
//      1.  Group Properties
//      2.  Vector of NoteData
public class NoteGroupData {
    private final Object[] theData;

    public NoteGroupData() {
        super();
        theData = new Object[2];
    }

    public NoteGroupData(Object[] incomingData) {
        theData = incomingData;  // by reference; be careful with reachbacks.
    }

    public void add(GroupInfo groupInfo) {
        theData[0] = groupInfo;
    }

    public void add(Vector<NoteData> incomingNotes) {
        theData[1] = incomingNotes;
    }

    public Object[] getTheData() {
        return theData;
    }

    public static void main(String[] args) {
        NoteGroupData noteGroupData = new NoteGroupData();
        System.out.println(AppUtil.toJsonString(noteGroupData));

    }

}
