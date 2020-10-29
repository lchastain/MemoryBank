import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public abstract class CalendarNoteGroup extends NoteGroup {

    CalendarNoteGroup(GroupInfo groupInfo) {
        super(groupInfo);
    }

    static String getGroupNameForDate(LocalDate theDate, GroupInfo.GroupType theType) {
        String theName;
        DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        switch(theType) {
            case YEAR_NOTES:
                dtf = DateTimeFormatter.ofPattern("yyyy");
                break;
            case MONTH_NOTES:
                dtf = DateTimeFormatter.ofPattern("d MMMM yyyy");
                break;
            case DAY_NOTES:
                dtf = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
                break;
        }
        theName = dtf.format(theDate);
        return theName;
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

    // A CalendarNoteGroup has a different GroupProperties for every choice.  They can be set at construction but
    // then they get nulled out when there is an attempt to load new data.  But if the specified data is not there,
    // the properties remain null.  That is when this method is needed.
    @Override
    public GroupProperties getGroupProperties() {
        if(myProperties == null) {
            // If we loaded our properties member from a data store then we need to use that one because it may
            // already contain linkages.  Otherwise it will be null and we can just make one right now.
            switch(myGroupInfo.groupType) {
                case DAY_NOTES:
                    setGroupProperties(new GroupProperties(myGroupInfo.getGroupName(), GroupInfo.GroupType.DAY_NOTES));
                    break;
                case MONTH_NOTES:
                    setGroupProperties(new GroupProperties(myGroupInfo.getGroupName(), GroupInfo.GroupType.MONTH_NOTES));
                    break;
                case YEAR_NOTES:
                    setGroupProperties(new GroupProperties(myGroupInfo.getGroupName(), GroupInfo.GroupType.YEAR_NOTES));
                    break;
                default:
                    setGroupProperties(new GroupProperties(myGroupInfo.getGroupName(), GroupInfo.GroupType.NOTES));
            }
        }
        return myProperties;
    }

    // This is used by data-accessor contexts, prior to saving.  But tests may also use it.
    @Override
    public Object[] getTheData() {
        Object[] theData = new Object[2];
        theData[0] = getGroupProperties(); // This is the difference from the base method.
        theData[1] = noteGroupDataVector;
        return theData;
    }



}
