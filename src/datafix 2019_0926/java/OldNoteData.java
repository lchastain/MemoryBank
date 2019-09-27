import java.util.Date;

class OldNoteData {
    private Date dateLastMod;
    String noteString;
    String subjectString;
    protected String extendedNoteString;
    int extendedNoteWidthInt;
    int extendedNoteHeightInt;
    static boolean loading = false;

    public OldNoteData() {
        if(!loading) dateLastMod = new Date();
        clear();
    } // end constructor


    // The copy constructor (clone)
    public OldNoteData(OldNoteData ndCopy) {
        this();
        this.extendedNoteHeightInt = ndCopy.extendedNoteHeightInt;
        this.extendedNoteString = ndCopy.extendedNoteString;
        this.extendedNoteWidthInt = ndCopy.extendedNoteWidthInt;
        this.noteString = ndCopy.noteString;
        this.subjectString = ndCopy.subjectString;
        this.dateLastMod = ndCopy.dateLastMod;
    } // end constructor

    // Construct a NoteData from a TodoNoteData.
    public OldNoteData(TodoNoteData ndCopy) {
        this();
        this.noteString = ndCopy.noteString;
        this.subjectString = ndCopy.subjectString;
        this.extendedNoteHeightInt = ndCopy.extendedNoteHeightInt;
        this.extendedNoteString = ndCopy.extendedNoteString;
        this.extendedNoteWidthInt = ndCopy.extendedNoteWidthInt;
        this.dateLastMod = Date.from(ndCopy.getLastModDate().toInstant());
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
    //   context does not need to know what generation of NoteData it is
    //   really getting, just that it will look like a NoteData.
    protected OldNoteData copy() {
        return new OldNoteData(this);
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
        if(!loading) dateLastMod = new Date();
    }

    void setExtendedNoteWidthInt(int val) {
        extendedNoteWidthInt = val;
    }

    void setNoteString(String value) {
        noteString = value;
        if(!loading) dateLastMod = new Date();
    }

    public void setSubjectString(String value) {
        subjectString = value;
        if(!loading) dateLastMod = new Date();
    }
} // end class OldNoteData
