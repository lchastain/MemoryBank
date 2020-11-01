import java.time.ZonedDateTime;

class NoteInfo extends BaseData {
    // We do not want the BaseData member zdtLastModString (LMD) to be updated when instances of NoteInfo
    // children are loaded in as Notes of Groups.  But NoteInfo has (and some of its children have) additional
    // members that when changed can & should update the LMD.  The problem comes in when those 'set' methods
    // are called during the construction of a Group; in those cases the values being set have come
    // from the data store and in that case should not affect the LMD.  This flag is used in those 'set'
    // methods so they will know whether or not to update the LMD.
    static boolean loading = false;

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


    ZonedDateTime getLastModDate() {
        // We don't keep an actual date; we keep the string from it.
        // So when the request comes in, if somehow that string hasn't been set then we don't want
        //   to default to current date & time, so the only other answer is to send back a null.
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
