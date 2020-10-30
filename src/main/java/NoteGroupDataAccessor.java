public interface NoteGroupDataAccessor {

    Object[] loadNoteGroupData(GroupInfo groupInfo);

    void saveNoteGroupData(Object[] theData);

}
