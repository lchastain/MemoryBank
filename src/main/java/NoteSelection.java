
// The NoteComponent class uses this interface.

public interface NoteSelection {
    default void noteSelected(NoteData noteData) { }
}
