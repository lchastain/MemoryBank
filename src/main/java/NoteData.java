import java.io.Serializable;
import java.util.Date;

class NoteData implements Serializable {
    private static final long serialVersionUID = 5299342314918199917L;

    private Date dateLastMod;
    String noteString;
    String subjectString;
    protected String extendedNoteString;
    int extendedNoteWidthInt;
    int extendedNoteHeightInt;

    public NoteData() {
        super();  // whatever happens for a generic object...
        dateLastMod = new Date();
        clear();
    } // end constructor


    // The copy constructor (clone)
    public NoteData(NoteData ndCopy) {
        this();
        this.extendedNoteHeightInt = ndCopy.extendedNoteHeightInt;
        this.extendedNoteString = ndCopy.extendedNoteString;
        this.extendedNoteWidthInt = ndCopy.extendedNoteWidthInt;
        this.noteString = ndCopy.noteString;
        this.subjectString = ndCopy.subjectString;
        this.dateLastMod = ndCopy.dateLastMod;
    } // end constructor


    protected void clear() {
        noteString = "";

        // initialize subject to null to indicate that a group-specified default subject should be used.
        // If someone actually enters a value of "" then that's what they will get, vs the default.
        subjectString = null;  // null, not "".

        extendedNoteString = "";
        extendedNoteWidthInt = 300;
        extendedNoteHeightInt = 200;
    } // end clear

    // A copy constructor cannot be called in a class-unspecified manner;
    //   this method can be.  Child classes will override so that a calling
    //   context does not need to know what generation it is getting.
    protected NoteData copy() {
        return new NoteData(this);
    }

    int getExtendedNoteHeightInt() {
        return extendedNoteHeightInt;
    }

    public String getExtendedNoteString() {
        return extendedNoteString;
    }

    int getExtendedNoteWidthInt() {
        return extendedNoteWidthInt;
    }

    // This one is only used by certain child classes
    protected Date getNoteDate() {
        return null;
    }

    public String getNoteString() {
        return noteString;
    }

    Date getLastModDate() {
        return dateLastMod;
    }

    public String getSubjectString() {
        return subjectString;
    }

    boolean hasText() {
        return !noteString.trim().equals("") || !extendedNoteString.trim().equals("");
    } // end hasText()

    void setExtendedNoteHeightInt(int val) {
        extendedNoteHeightInt = val;
    }

    public void setExtendedNoteString(String val) {
        extendedNoteString = val;
        dateLastMod = new Date();
    }

    void setExtendedNoteWidthInt(int val) {
        extendedNoteWidthInt = val;
    }

    void setNoteString(String value) {
        noteString = value;
        dateLastMod = new Date();
    }

    public void setSubjectString(String value) {
        subjectString = value;
        dateLastMod = new Date();
    }
} // end class NoteData
