/*  Displays a calendar-based group of NoteComponent.  The
    calendar is set to one of DAY, MONTH, YEAR.

 */

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public abstract class CalendarNoteGroup extends NoteGroup {
    private static final long serialVersionUID = 1L;

    LocalDate theChoice;  // Holds the 'current' date of the displayed Group.
    DateTimeFormatter dtf;

    private ChronoUnit dateType;

    CalendarNoteGroup(String defaultSubject) {
        super();
        super.setDefaultSubject(defaultSubject);

        theChoice = LocalDate.now();

        if (defaultSubject.equals("Day Note")) dateType = ChronoUnit.DAYS;
        if (defaultSubject.equals("Month Note")) dateType = ChronoUnit.MONTHS;
        if (defaultSubject.equals("Year Note")) dateType = ChronoUnit.YEARS;

        updateGroup();
    } // end constructor


    // A NoteGroup does not have a 'choice'; a CalendarNoteGroup does.
    public LocalDate getChoice() {
        return theChoice;
    }


    //------------------------------------------------------
    // Method Name: getGroupFilename
    //
    //------------------------------------------------------
    @Override
    public String getGroupFilename() {
        String s;

        if (intSaveGroupStatus == ONGOING) {
            if (dateType == ChronoUnit.DAYS) s = AppUtil.makeFilename(theChoice, "D");
            else if (dateType == ChronoUnit.MONTHS) s = AppUtil.makeFilename(theChoice, "M");
            else s = AppUtil.makeFilename(theChoice, "Y");
            return s;
        } else {  // Results of a findFilename may be "".
            if (dateType == ChronoUnit.DAYS) s = AppUtil.findFilename(theChoice, "D");
            else if (dateType == ChronoUnit.MONTHS) s = AppUtil.findFilename(theChoice, "M");
            else s = AppUtil.findFilename(theChoice, "Y");
            return s;
        } // end if saving else not saving
    } // end getGroupFilename


    //--------------------------------------------------------------
    // Method Name: setChoice
    //
    // A calling context should only make this call if it is
    //   needed, because it causes a reload of the group.
    //--------------------------------------------------------------
    public void setChoice(LocalDate theNewChoice) {
        theChoice = theNewChoice;
        updateGroup();
    } // end setChoice


    public void setOneBack() {
        preClose();
        switch(dateType) {
            case DAYS:
                theChoice = theChoice.minusDays(1);
                break;
            case MONTHS:
                theChoice = theChoice.minusMonths(1);
                break;
            case YEARS:
                theChoice = theChoice.minusYears(1);
                break;
        }
    } // end setOneBack


    public void setOneForward() {
        preClose();
        switch(dateType) {
            case DAYS:
                theChoice = theChoice.plusDays(1);
                break;
            case MONTHS:
                theChoice = theChoice.plusMonths(1);
                break;
            case YEARS:
                theChoice = theChoice.plusYears(1);
                break;
        }
    } // end setOneForward

} // end class CalendarNoteGroup
