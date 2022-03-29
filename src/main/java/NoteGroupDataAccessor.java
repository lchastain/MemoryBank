import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@SuppressWarnings("rawtypes")
public interface NoteGroupDataAccessor {

    void deleteNoteGroupData();

    boolean exists();

    LocalDate getNextDateWithData(LocalDate currentDate, ChronoUnit dateDelta, CalendarNoteGroup.Direction direction);

    Object[] loadNoteGroupData(); // Calls the other one with a GroupInfo that is held in the implementation.
    Object[] loadNoteGroupData(GroupInfo groupInfo); // The param supports this accessor loading Other groups.

    String getObjectionToName(String theName);

    void saveNoteGroupData(Object[] theData);

    boolean renameNoteGroupData(GroupType theType, String nodeName, String renamedTo);

    // void addNoteGroup(GroupInfo groupInfo)

}
