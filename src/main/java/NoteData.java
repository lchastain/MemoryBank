import com.fasterxml.jackson.annotation.JsonIgnore;

class NoteData extends BaseData {
    String noteString;
    String subjectString;
    String extendedNoteString;
    boolean multiline;

    @JsonIgnore // Now unused, but linkTargets is already in too many data files.
    LinkTargets[] linkTargets;

    // This member is used in search result tracing.
    // Not always present, needs to be set by a higher context via the 'set' method.
    private transient NoteGroup myNoteGroup;

    NoteData() {
        clear();
    } // end constructor

    NoteData(String noteString) {
        this();
        this.noteString = noteString;
    }


    // The copy constructor (clone).  Primary usage is by the 'swap' methods,
    // and when child classes need to have their additional members stripped off
    // so that the result is an an isolated copy of the base class members from
    // the original note (for pasting from one type of NoteData child to a different type).
    // Secondary usage is to provide a true object copy and not just a reference, for editing and undoing.
    NoteData(NoteData ndCopy) {
        super(ndCopy);
        this.extendedNoteString = ndCopy.extendedNoteString;
        this.noteString = ndCopy.noteString;
        this.subjectString = ndCopy.subjectString;
        this.multiline = ndCopy.multiline;
    }// end of the copy constructor


    void clear() {
        noteString = "";

        // initialize subject to null to indicate that a group-specified default subject should be used.
        // If someone actually enters a value of "" then that's what they will get, vs the default.
        subjectString = null;  // null, not "".

        extendedNoteString = ""; // Never null.
    } // end clear

    // A copy constructor cannot be called from a reference;
    //   this method can be.  Child classes will override it so that a calling
    //   context does not need to know what generation of NoteData it is
    //   really getting, just that it will look like a NoteData.
    protected NoteData copy() {
        return new NoteData(this);
    }

    public String getExtendedNoteString() {
        return extendedNoteString;
    }

    NoteGroup getMyNoteGroup() {
        return myNoteGroup;
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
        touchLastMod();
    }

    void setMyNoteGroup(NoteGroup myNoteGroup) {
        this.myNoteGroup = myNoteGroup;
    }

    void setNoteString(String value) {
        noteString = value;
        touchLastMod();
    }

    void setSubjectString(String value) {
        subjectString = value;
        touchLastMod();
    }
} // end class NoteData
