
// The NoteComponent class uses this interface.

public interface NoteSelection {

    // The 'set' does its setting in the only context that wants it; no 'get' needed.
    // Consider removing this method.
    default NoteData getSelection() {
        return null;
    }

    default void noteSelected(NoteData noteData) { }
}
