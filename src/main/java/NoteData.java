class NoteData extends NoteInfo {
    LinkTargets linkTargets;

    NoteData() {
        super();
    } // end constructor


    // The copy constructor (clone).  Primary usage is by the 'swap' methods,
    // and when child classes need to have their additional members stripped off
    // so that the result is an an isolated copy of the base class members from
    // the original note (for pasting from one type
    // of NoteData child to a different type).  Secondary usage is to provide
    // a true object copy and not just a reference, for editing and undoing.
    NoteData(NoteData ndCopy) {
        super(ndCopy);
        this.linkTargets = (LinkTargets) ndCopy.linkTargets.clone();
    }// end of the copy constructor

    NoteData(NoteInfo noteInfo) {
        super(noteInfo);
        // This usage comes from LinkNoteComponent.  We leave the linkTargets null.
    }

    void clear() {
        super.clear();
        linkTargets = new LinkTargets();
    } // end clear

    // A copy constructor cannot be called from a reference;
    //   this method can be.  Child classes will override it so that a calling
    //   context does not need to know what generation of NoteData it is
    //   really getting, just that it will look like a NoteData.
    protected NoteData copy() {
        return new NoteData(this);
    }

} // end class NoteData
