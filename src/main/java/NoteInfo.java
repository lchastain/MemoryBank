

class NoteInfo extends BaseData {

    String noteString;
    String subjectString;
    String extendedNoteString;

    NoteInfo() {
        super();
        clear();
    } // end constructor


    // The copy constructor (clone).  Primary usage is by the 'swap' methods,
    // and when child classes need to have their additional members stripped off
    // so that the result is an an isolated copy of the base class members from
    // the original note (for Link Target serialization or pasting from one type
    // of NoteData child to a different type).
    NoteInfo(NoteInfo ndCopy) {
        this();
        if(ndCopy == null) return;
        this.instanceId = ndCopy.instanceId;
        this.extendedNoteString = ndCopy.extendedNoteString;
        this.noteString = ndCopy.noteString;
        this.subjectString = ndCopy.subjectString;
        this.zdtLastModString = ndCopy.zdtLastModString;
    }// end of the copy constructor

    void clear() {
        noteString = "";

        // initialize subject to null to indicate that a group-specified default subject should be used.
        // If someone actually enters a value of "" then that's what they will get, vs the default.
        subjectString = null;  // null, not "".

        extendedNoteString = "";
    } // end clear

    // A copy constructor cannot be called from a reference;
    //   this method can be.  Child classes will override it so that a calling
    //   context does not need to know what generation of NoteData it is
    //   really getting, just that it will look like a NoteData.
    protected NoteInfo copy() {
        return new NoteInfo(this);
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
        touchLastMod();
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
