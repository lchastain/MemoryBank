import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@SuppressWarnings("rawtypes")
public interface NoteGroupDataAccessor {

    void deleteNoteGroupData();

    ArrayList getGroupNames();

    LocalDate getNextDateWithData(LocalDate currentDate, ChronoUnit dateDelta, CalendarNoteGroup.Direction direction);

    Object[] loadNoteGroupData(GroupInfo groupInfo);

    void saveNoteGroupData(Object[] theData);

    boolean exists();

    // void addNoteGroup(GroupInfo groupInfo)

}
