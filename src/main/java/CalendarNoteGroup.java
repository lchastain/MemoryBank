import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// This is the first class of the re-architecting; baby steps - first, needed static methods.

public class CalendarNoteGroup extends NoteGroup {
    static LocalDate getDateFromGroupName(GroupInfo groupInfo) {
        DateTimeFormatter dtf;
        LocalDate theDate = null;
        switch(groupInfo.groupType) {
            case YEAR_NOTES:
                theDate = LocalDate.now(); // Example toString():  2020-10-16
                int theYear = Integer.parseInt(groupInfo.getGroupName());
                theDate = theDate.withYear(theYear);
                break;
            case MONTH_NOTES:
                dtf = DateTimeFormatter.ofPattern("d MMMM yyyy");
                theDate = LocalDate.parse("1 " + groupInfo.getGroupName(), dtf);
                break;
            case DAY_NOTES:
                dtf = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
                theDate = LocalDate.parse(groupInfo.getGroupName(), dtf);
                break;
        }
        return theDate;
    }
}
