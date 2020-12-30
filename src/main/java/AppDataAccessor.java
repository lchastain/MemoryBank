import java.time.LocalDateTime;

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
    boolean createArea(DataArea dataArea); // tie in usage of this with 'add new group'
    boolean createArchive();
    String[] getArchiveNames();
    AppOptions getArchiveOptions(String archiveName);

    // Convert the archive name into a LocalDateTime
    LocalDateTime getDateTimeForArchiveName(String archiveName);

    NoteGroupDataAccessor getNoteGroupDataAccessor(GroupInfo groupInfo);


    static AppDataAccessor getAppDataAccessor(AccessType accessType) {
        if(accessType == AccessType.FILE) {
            return new FileDataAccessor();
        } else {
            return new FileDataAccessor(); // Currently it's the only choice, anyway.
        }
    }

    boolean removeArchive(LocalDateTime localDateTime);
}
