import javax.swing.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Vector;

public interface DataAccessor {

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
    
    
    boolean createArea(DataArea dataArea); // TODO tie in usage of this with 'add new group'
    boolean createArchive();
    default String getArchiveStorageName(String archiveName) {
        return archiveName; // Override this when the 'storage' name differs from the archive name.
    }
    AppOptions getAppOptions();

    String[] getArchiveNames();
    ImageIcon getImageIcon(IconInfo iconInfo);
    AppOptions getArchiveOptions(String archiveName);

    // Convert the archive name into a LocalDateTime
    LocalDateTime getDateTimeForArchiveName(String archiveName);

    ArrayList getGroupNames(GroupType groupType, boolean filterInactive);

    NoteGroupDataAccessor getNoteGroupDataAccessor(GroupInfo groupInfo);


    static DataAccessor getDataAccessor(AccessType accessType) {
        if(accessType == AccessType.FILE) {
            return new FileDataAccessor();
        } else {
            return new FileDataAccessor(); // Currently it's the only choice, anyway.
        }
    }

    Vector<String> loadSubjects(String defaultSubject);
    boolean saveSubjects(String defaultSubject, Vector<String> subjects);

    boolean removeArchive(LocalDateTime localDateTime);
    void saveAppOptions();
    Vector<NoteData> scanData(SearchPanel searchPanel);
}
