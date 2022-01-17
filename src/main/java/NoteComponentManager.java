// All methods here are 'default' so that any class (ie, Test classes) can claim
// implementation while doing none of the actual work.  Of course that only works
// for them if they don't really need the intended results.

public interface NoteComponentManager {
    default void activateNextNote(int index) {}
    default boolean editExtendedNoteComponent(NoteData tmpNoteData) { return true; }
    default int getLastVisibleNoteIndex() { return 0; }
    default void setGroupChanged(boolean b) {}
    default void setStatusMessage(String s) {}
    void shiftDown(int index);
    void shiftUp(int index);
}
