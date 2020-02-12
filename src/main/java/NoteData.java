import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.UUID;

class NoteData {
    protected String zdtLastModString;
    UUID noteId;
    String noteString;
    String subjectString;
    String extendedNoteString;
    ArrayList<Linkage> linkages;

    static boolean loading = false;

    NoteData() {
        noteId = UUID.randomUUID();
        if(!loading) {
            zdtLastModString = ZonedDateTime.now().toString();
        }
        clear();
    } // end constructor


    // The copy constructor (clone)
    NoteData(NoteData ndCopy) {
        this();
        this.noteId = ndCopy.noteId;
        this.extendedNoteString = ndCopy.extendedNoteString;
        this.noteString = ndCopy.noteString;
        this.subjectString = ndCopy.subjectString;
        this.zdtLastModString = ndCopy.zdtLastModString;
        this.linkages = ndCopy.linkages;
    } // end constructor

    // Construct a NoteData from a TodoNoteData.
    NoteData(TodoNoteData ndCopy) {
        this();
        this.noteId = ndCopy.noteId;
        this.noteString = ndCopy.noteString;
        this.subjectString = ndCopy.subjectString;
        this.extendedNoteString = ndCopy.extendedNoteString;
        this.zdtLastModString = ndCopy.getLastModDate().toString();
        this.linkages = ndCopy.linkages;
    } // end constructor

    void clear() {
        noteString = "";

        // initialize subject to null to indicate that a group-specified default subject should be used.
        // If someone actually enters a value of "" then that's what they will get, vs the default.
        subjectString = null;  // null, not "".

        extendedNoteString = "";
    } // end clear

    // A copy constructor cannot be called in a class-unspecified manner;
    //   this method can be.  Child classes will override so that a calling
    //   context does not need to know what generation of NoteData it is
    //   really getting, just that it will look like a NoteData.
    protected NoteData copy() {
        return new NoteData(this);
    }

    ZonedDateTime getLastModDate() {
        if(zdtLastModString == null) return null;
        return ZonedDateTime.parse(zdtLastModString);
    }

    public String getExtendedNoteString() {
        return extendedNoteString;
    }

    public String getNoteString() {
        return noteString;
    }

    public String getSubjectString() {
        return subjectString;
    }

    boolean hasText() {
        return !noteString.trim().equals("") || !extendedNoteString.trim().equals("");
    } // end hasText()

    public void setExtendedNoteString(String val) {
        extendedNoteString = val;
        if(!loading) zdtLastModString = ZonedDateTime.now().toString();
    }

    void setNoteString(String value) {
        noteString = value;
        if(!loading) zdtLastModString = ZonedDateTime.now().toString();
    }

    void setSubjectString(String value) {
        subjectString = value;
        if(!loading) zdtLastModString = ZonedDateTime.now().toString();
    }

} // end class NoteData
