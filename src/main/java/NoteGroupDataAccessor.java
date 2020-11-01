public interface NoteGroupDataAccessor {

    void deleteNoteGroupData();

    Object[] loadNoteGroupData(GroupInfo groupInfo);

    void saveNoteGroupData(Object[] theData);

    boolean exists();

    // void addNoteGroup(GroupInfo groupInfo)

}
