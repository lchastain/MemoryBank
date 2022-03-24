import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@SuppressWarnings("rawtypes")
public interface NoteGroupDataAccessor {

    void deleteNoteGroupData();

    boolean exists();

    ArrayList getGroupNames(boolean filterInactive);

    LocalDate getNextDateWithData(LocalDate currentDate, ChronoUnit dateDelta, CalendarNoteGroup.Direction direction);

    Object[] loadNoteGroupData();

    String getObjectionToName(String theName);

    void saveNoteGroupData(Object[] theData);

    boolean renameNoteGroupData(GroupType theType, String nodeName, String renamedTo);

    // void addNoteGroup(GroupInfo groupInfo)

}
