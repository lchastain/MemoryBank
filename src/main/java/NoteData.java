import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.ZonedDateTime;
import java.util.Date;

class NoteData {
    @JsonIgnore
    private Date dateLastMod; // This can be removed after all data is converted.

    private String zdtLastModString;
    String noteString;
    String subjectString;
    protected String extendedNoteString;
    int extendedNoteWidthInt;
    int extendedNoteHeightInt;
    static boolean loading = false;

    public NoteData() {
        if(!loading) zdtLastModString = ZonedDateTime.now().toString();
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
        this.zdtLastModString = ndCopy.zdtLastModString;
    } // end constructor

    // Construct a NoteData from a TodoNoteData.
    public NoteData(TodoNoteData ndCopy) {
        this();
        this.noteString = ndCopy.noteString;
        this.subjectString = ndCopy.subjectString;
        this.extendedNoteHeightInt = ndCopy.extendedNoteHeightInt;
        this.extendedNoteString = ndCopy.extendedNoteString;
        this.extendedNoteWidthInt = ndCopy.extendedNoteWidthInt;
        this.zdtLastModString = ndCopy.getLastModDate().toString();
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
    protected NoteData copy() {
        return new NoteData(this);
    }

    ZonedDateTime getLastModDate() {
        if(zdtLastModString == null) return null;
        return ZonedDateTime.parse(zdtLastModString);
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

    public String getNoteString() {
        return noteString;
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
        if(!loading) zdtLastModString = ZonedDateTime.now().toString();
    }

    void setExtendedNoteWidthInt(int val) {
        extendedNoteWidthInt = val;
    }

    void setNoteString(String value) {
        noteString = value;
        if(!loading) zdtLastModString = ZonedDateTime.now().toString();
    }

    public void setSubjectString(String value) {
        subjectString = value;
        if(!loading) zdtLastModString = ZonedDateTime.now().toString();
    }

} // end class NoteData
