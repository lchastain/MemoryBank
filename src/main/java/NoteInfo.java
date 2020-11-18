import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

class NoteInfo {
    UUID noteId;
    String noteString;
    String extendedNoteString;

    // These members were previously defined here or inherited, but not now and so we need to acknowledge
    //   that we might see them in previously persisted data, but 'ignore' them otherwise.
    //   This is so that they do not gum up the type conversions when this object gets deserialized.
    //--------------------------------------------------
    @JsonIgnore
    protected String zdtLastModString;

    @JsonIgnore
    protected UUID instanceId;

    @JsonIgnore
    protected String subjectString;
    //--------------------------------------------------


    private NoteInfo() { } // For JSON deserialization, only.  noteId is not allowed to be null.


    NoteInfo(NoteData noteData) {
        noteId = noteData.instanceId;
        noteString = noteData.noteString;
        extendedNoteString = noteData.extendedNoteString;
    }

} // end class NoteInfo
