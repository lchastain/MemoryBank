import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Vector;

@SuppressWarnings("rawtypes")
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


    String chooseIcon();
    boolean createArea(DataArea dataArea); // TODO tie in usage of this with 'add new group'
    boolean createArchive();
    boolean[][] findDataDays(int year);

    default String getArchiveStorageName(String archiveName) {
        return archiveName; // Override this when the 'storage' name differs from the archive name.
    }
    AppOptions getAppOptions();

    String[] getArchiveNames();
    Image[] getIconArray(int year, int month, int day);
    ImageIcon getImageIcon(IconInfo iconInfo);
    ImageIcon getImageIcon(IconNoteData iconNoteData);
    ImageIcon[] getImageIcons(DataArea dataArea, String lowerPath);

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

    Locations loadLocations();
    Vector<String> loadSubjects(String defaultSubject);
    boolean saveLocations(Locations theLocations);
    boolean saveSubjects(String defaultSubject, Vector<String> subjects);

    boolean removeArchive(LocalDateTime localDateTime);
    void saveAppOptions();
    Vector<NoteData> scanData(SearchPanel searchPanel);
}
