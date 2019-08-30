/*  Displays a calendar-based group of NoteComponent.  The
    calendar is set to one of DAY, MONTH, YEAR.

 */

import java.text.SimpleDateFormat;
import java.util.*; // Calendar, more ??

public abstract class CalendarNoteGroup extends NoteGroup {
    private static final long serialVersionUID = 1L;

    protected Date choice;
    protected SimpleDateFormat sdf;

    private int calType = 0;

    // Holds the 'current' date of the displayed Group.
    private GregorianCalendar cal;

    CalendarNoteGroup(String defaultSubject) {
        super(defaultSubject);

        sdf = new SimpleDateFormat();
        cal = (GregorianCalendar) Calendar.getInstance();
        // Note: getInstance at this time returns a Calendar that
        //   is actually a GregorianCalendar, but since the return
        //   type is Calendar, it must be cast in order to assign.

        cal.setGregorianChange(new GregorianCalendar(1752,
                Calendar.SEPTEMBER, 14).getTime());

        choice = new Date();

        if (defaultSubject.equals("Day Note")) calType = Calendar.DATE;
        if (defaultSubject.equals("Month Note")) calType = Calendar.MONTH;
        if (defaultSubject.equals("Year Note")) calType = Calendar.YEAR;

        updateGroup();
    } // end constructor


    // A NoteGroup does not have a 'choice'; a CalendarNoteGroup does.
    public Date getChoice() {
        return choice;
    }


    //------------------------------------------------------
    // Method Name: getGroupFilename
    //
    //------------------------------------------------------
    @Override
    public String getGroupFilename() {
        String s;

        if (intSaveGroupStatus == ONGOING) {
            if (calType == Calendar.DATE) s = AppUtil.makeFilename(cal, "D");
            else if (calType == Calendar.MONTH) s = AppUtil.makeFilename(cal, "M");
            else s = AppUtil.makeFilename(cal, "Y");
            return s;
        } else {  // Results of a findFilename may be "".
            if (calType == Calendar.DATE) s = AppUtil.findFilename(cal, "D");
            else if (calType == Calendar.MONTH) s = AppUtil.findFilename(cal, "M");
            else s = AppUtil.findFilename(cal, "Y");
            return s;
        } // end if saving else not saving
    } // end getGroupFilename


    //--------------------------------------------------------------
    // Method Name: setChoice
    //
    // A calling context should only make this call if it is
    //   needed, because it causes a reload of the group.
    //--------------------------------------------------------------
    public void setChoice(Date d) {
        cal.setTime(d);
        choice = cal.getTime();
        updateGroup();
    } // end setChoice


    public void setOneBack() {
        preClose();
        cal.add(calType, -1);
        choice = cal.getTime();
    } // end setOneBack


    public void setOneForward() {
        preClose();
        cal.add(calType, 1);
        choice = cal.getTime();
    } // end setOneForward

} // end class CalendarNoteGroup
