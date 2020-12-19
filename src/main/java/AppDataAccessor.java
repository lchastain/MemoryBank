
public interface AppDataAccessor {
    enum AccessType {
        DATABASE("Database"),
        FILE("File");

        private final String display;

        AccessType(String s) {
            display = s;
        }


        @Override
        public String toString() {
            return display;
        }
    }
    boolean createArea(DataArea dataArea);
    boolean createArchive();
    NoteGroupDataAccessor getNoteGroupDataAccessor(GroupInfo groupInfo);

    static AppDataAccessor getAppDataAccessor(AccessType accessType) {
        if(accessType == AccessType.FILE) {
            return new FileDataAccessor();
        }
        return new FileDataAccessor(); // Currently it's the only choice, anyway.
    }
}
