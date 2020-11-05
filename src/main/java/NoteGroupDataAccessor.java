import java.util.ArrayList;

public interface NoteGroupDataAccessor {

    void deleteNoteGroupData();

    ArrayList getGroupNames();

    Object[] loadNoteGroupData(GroupInfo groupInfo);

    void saveNoteGroupData(Object[] theData);

    boolean exists();

    // void addNoteGroup(GroupInfo groupInfo)

}
