
public interface NoteComponentManager {
    void activateNextNote(int index);
    boolean editExtendedNoteComponent(NoteData tmpNoteData);
    int getLastVisibleNoteIndex();
    default void setGroupChanged(boolean b) {}
    default void setStatusMessage(String s) {}
    void shiftDown(int index);
    void shiftUp(int index);
}
