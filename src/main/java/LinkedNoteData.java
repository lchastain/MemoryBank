import java.util.UUID;
import java.util.Vector;

public class LinkedNoteData extends NoteData {
    // A LinkedNoteData instance is a copy of a NoteData that can have zero or more links to a LinkTarget.
    //   The ID of this LinkedNoteData will be the same as the ID of the NoteData that has been linked (from).

    private UUID myGroupId;
    Vector<LinkTarget> linkTargets;

    public LinkedNoteData() {
        super();
        linkTargets = new Vector<>();
    }

    public LinkedNoteData(UUID groupId, NoteData noteData) {
        this();
        myGroupId = groupId;
        zdtLastModString = noteData.zdtLastModString; // needed?
        instanceId = noteData.instanceId;
        noteString = noteData.noteString;
        extendedNoteString = noteData.extendedNoteString;
        subjectString = noteData.subjectString;
    }

    public LinkedNoteData(LinkedNoteData linkedNoteData) {
        this();
        myGroupId = linkedNoteData.myGroupId;
        zdtLastModString = linkedNoteData.zdtLastModString;
        instanceId = linkedNoteData.instanceId;
        noteString = linkedNoteData.noteString;
        extendedNoteString = linkedNoteData.extendedNoteString;
        subjectString = linkedNoteData.subjectString;
        linkTargets = linkedNoteData.linkTargets;
    }

    // Either retrieve the one that is already known, or make a new one.
    static LinkedNoteData getLinkedNoteData(NoteData noteData) {
        // Find and return, if it already exists.
        for (LinkedNoteData linkedNoteData : MemoryBank.appOpts.linkages) {
            if (linkedNoteData.instanceId == noteData.instanceId) {
                return linkedNoteData;
            }
        }

        // We got here, so it didn't already exist.  Make a new one.
        // but a null groupId is probably a problem....
        return new LinkedNoteData(null, noteData);
    }

    UUID getMyGroupId() { return myGroupId; }

//    @JsonIgnore
//    public NoteData getSourceNoteData() {
//        return (NoteData) this;
//    }

    void setMyGroupId(UUID groupId) {
        myGroupId = groupId;
    }


}
