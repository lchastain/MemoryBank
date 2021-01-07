import java.io.File;
import java.io.FileFilter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public abstract class CalendarNoteGroup extends NoteGroup {

    enum Direction {
        BACKWARD,
        FORWARD
    }

    CalendarNoteGroup(GroupInfo groupInfo) {
        super(groupInfo);
    }

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

    static String getGroupNameForDate(LocalDate theDate, GroupType theType) {
        String theName;
        DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        switch(theType) {
            case YEAR_NOTES:
                dtf = DateTimeFormatter.ofPattern("yyyy");
                break;
            case MONTH_NOTES:
                dtf = DateTimeFormatter.ofPattern("MMMM yyyy");
                break;
            case DAY_NOTES:
                dtf = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
                break;
        }
        theName = dtf.format(theDate);
        return theName;
    }

    static class CalendarFileFilter implements FileFilter {
        String which; // "D", "M", or "Y"

        CalendarFileFilter(ChronoUnit dateType) {
            switch(dateType) {
                case DAYS:
                    which = "D";
                    break;
                case MONTHS:
                    which = "M";
                    break;
                case YEARS:
                case DECADES:
                    which = "Y";
                    break;
            }
        } // end constructor

        public boolean accept(File theFile) {
            if(theFile.isDirectory()) return true; // For recursion, from 'Years' and further down
            String theName = theFile.getName();
            boolean b1 = theName.startsWith(which);
            boolean b2 = theName.endsWith(".json");
            return b1 & b2;
        } // end accept
    } // end class calendarFileFilter



}
