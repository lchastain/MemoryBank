class NoteData extends BaseData implements LinkHolder {
    String noteString;
    String subjectString;
    String extendedNoteString;
    LinkTargets linkTargets;

    // This member is used in linking.  Not always present; needs to be set by a higher context.
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
        this.linkTargets = (LinkTargets) ndCopy.linkTargets.clone();
    }// end of the copy constructor

//    NoteData(NoteInfo noteInfo) {
//        super(noteInfo);
//        // This usage comes from LinkNoteComponent.  We leave the linkTargets null.
//    }


    void clear() {
        noteString = "";

        // initialize subject to null to indicate that a group-specified default subject should be used.
        // If someone actually enters a value of "" then that's what they will get, vs the default.
        subjectString = null;  // null, not "".

        extendedNoteString = "";
        linkTargets = new LinkTargets();
    } // end clear

    // A copy constructor cannot be called from a reference;
    //   this method can be.  Child classes will override it so that a calling
    //   context does not need to know what generation of NoteData it is
    //   really getting, just that it will look like a NoteData.
    protected NoteData copy() {
        return new NoteData(this);
    }


    // Make a 'reverse' link from a 'forward' one where this
    // entity WAS the source; now it will be the target.
    // The calling context will attach the result to the correct LinkHolder.
    public LinkedEntityData createReverseLink(LinkedEntityData linkedEntityData) {
        LinkedEntityData reverseLinkedEntityData;

        // Get the group that this NoteData belongs to.
        NoteGroup myNoteGroup = getMyNoteGroup();

        // All linked notes belong to a group, so we do not expect this transient member to be null.
        // But it is up to the calling context to have previously set the value, so it does need to be verified:
        assert myNoteGroup != null;

        // Now from the group we can get its properties -
        GroupProperties groupProperties = myNoteGroup.getGroupProperties();

        // Another not-null verification
        assert groupProperties != null;

        // But we don't actually want the full Properties; just the info from them, so -
        GroupInfo groupInfo = new GroupInfo(groupProperties);

        // And now we can start making the reverse link.
        // Initially it will look just like a standard 'forward' link.
        reverseLinkedEntityData = new LinkedEntityData(groupInfo, new NoteInfo(this));

        // But now - give it the same ID as the forward one.  This will help with any
        // subsequent operations where the two will need to be 'in sync'.
        reverseLinkedEntityData.instanceId = linkedEntityData.instanceId;

        // Then give it a type that is the reverse of the forward link's type -
        reverseLinkedEntityData.linkType = linkedEntityData.reverseLinkType(linkedEntityData.linkType);

        // and then raise the 'reversed' flag.
        reverseLinkedEntityData.reversed = true;

        // It might appear that the type of this 'new' link could changed, but the editor panel will stop that when
        // it is displayed because this reversed link will already be in the list of links to be shown, whereas
        // links that are truly 'new' will have only been created by the panel while it is active.
        // Point being - there is no need to set 'reverseLinkedEntityData.retypeMe' to false.
        return reverseLinkedEntityData;
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
