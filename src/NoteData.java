import java.io.Serializable;
import java.util.Date;

class NoteData implements Serializable {
    private static final long serialVersionUID = 5299342314918199917L;

    private Date dateLastMod;
    protected String noteString;
    protected String subjectString;
    protected String extendedNoteString;
    protected int extendedNoteWidthInt;
    protected int extendedNoteHeightInt;

    public NoteData() {
        super();  // whatever happens for a generic object...

        dateLastMod = new Date();
        clear();
    } // end constructor


    // The copy constructor (clone)
    public NoteData(NoteData ndCopy) {
        this();  // Provides a unique strNoteId
        this.extendedNoteHeightInt = ndCopy.extendedNoteHeightInt;
        this.extendedNoteString = ndCopy.extendedNoteString;
        this.extendedNoteWidthInt = ndCopy.extendedNoteWidthInt;
        this.noteString = ndCopy.noteString;
        this.subjectString = ndCopy.subjectString;
        this.dateLastMod = ndCopy.dateLastMod;
    } // end constructor


    protected void clear() {
        noteString = "";

        // initialize subject to null to indicate that a default should
        // be used.  A value of "" should stay "".
        subjectString = null;

        extendedNoteString = "";
        extendedNoteWidthInt = 300;
        extendedNoteHeightInt = 200;
    } // end clear

    public int getExtendedNoteHeightInt() {
        return extendedNoteHeightInt;
    }

    public String getExtendedNoteString() {
        return extendedNoteString;
    }

    public int getExtendedNoteWidthInt() {
        return extendedNoteWidthInt;
    }

    protected Date getNoteDate() {
        return null;
    }

    public String getNoteString() {
        return noteString;
    }

    public Date getLastModDate() {
        return dateLastMod;
    }

    public String getSubjectString() {
        return subjectString;
    }

    public boolean hasText() {
        return !noteString.trim().equals("") || !extendedNoteString.trim().equals("");
    } // end hasText()

    public void setExtendedNoteHeightInt(int val) {
        extendedNoteHeightInt = val;
    }


    public void setExtendedNoteString(String val) {
        extendedNoteString = val;
        dateLastMod = new Date();
    }


    public void setExtendedNoteWidthInt(int val) {
        extendedNoteWidthInt = val;
    }


    public void setNoteString(String value) {
        noteString = value;
        dateLastMod = new Date();
    }


    public void setSubjectString(String value) {
        subjectString = value;
        dateLastMod = new Date();
    }
} // end class NoteData
