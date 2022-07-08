public interface NoteDataEditor {

    // We only have two implementations of this interface; when this method is called, the one that is
    //   called upon will need no clarification; it will construct and return the 'other' one.
    NoteDataEditor getAlternateEditor();

    // The numeric result of the modal dialog that displays the Editor.
    // Return value is the String-array index to the text of the user-selected button.
    int getEditingDirective(String title);

    // With NoteData there are two Strings to consider but the 'noteString' is being managed by the
    //   NoteGroupPanel, so the one we want here is the 'extendedNoteString'.
    // It may be a string-ified JSON object and if so then that is what is wanted, NOT just the plain text.
    String getExtendedNoteString();

    void setExtendedNoteString(String theTextObject);
}
