import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class EventNoteData extends IconNoteData {
    private static final long serialVersionUID = 9011997727379920195L;
    public static final int START_DATE_KNOWN = 1;
    public static final int START_TIME_KNOWN = 2;
    public static final int END_DATE_KNOWN = 4;
    public static final int END_TIME_KNOWN = 8;
    public static final int START_KNOWN = START_DATE_KNOWN + START_TIME_KNOWN;
    public static final int END_KNOWN = END_DATE_KNOWN + END_TIME_KNOWN;

    private Date dateEventStart; // Composite of both Date and Time.
    private Date dateEventEnd;   // Composite of both Date and Time.
    private String strDateFormat;
    private String strLocation;

    private String strRecurrence;
    private int intDatKnown;   // Date And Time Known indicator.
    private boolean blnRetainNote;

    // Create a temporary Calendar variable, for get/set operations.
    private static GregorianCalendar calTmp;
    private static SimpleDateFormat sdf;

    private static boolean blnSettingDuration;
    private transient Long lngDurationValue;
    private transient String strDurationUnits;

    static {
        sdf = new SimpleDateFormat();
        blnSettingDuration = false;

        calTmp = new GregorianCalendar();
        calTmp.setGregorianChange(new GregorianCalendar(1752,
                Calendar.SEPTEMBER, 14).getTime());
    } // end static section

    public EventNoteData() {
        super();
        clearEventNoteData();
    } // end constructor


    // The copy constructor (clone)
    public EventNoteData(EventNoteData endCopy) {
        this();
        this.blnRetainNote = endCopy.blnRetainNote;
        this.dateEventEnd = endCopy.dateEventEnd;
        this.dateEventStart = endCopy.dateEventStart;
        this.extendedNoteHeightInt = endCopy.extendedNoteHeightInt;
        this.extendedNoteString = endCopy.extendedNoteString;
        this.extendedNoteWidthInt = endCopy.extendedNoteWidthInt;
        this.iconFileString = endCopy.iconFileString;
        this.intDatKnown = endCopy.intDatKnown;
        this.lngDurationValue = endCopy.lngDurationValue;
        this.noteString = endCopy.noteString;
        this.showIconOnMonthBoolean = endCopy.showIconOnMonthBoolean;
        this.strDateFormat = endCopy.strDateFormat;
        this.strDurationUnits = endCopy.strDurationUnits;
        this.strLocation = endCopy.strLocation;
        this.strRecurrence = endCopy.strRecurrence;
        this.subjectString = endCopy.subjectString;
    } // end constructor


    // NOTES:
    //
    // When setting a date or time it is true that the fields may
    //   have previously contained a duration.  However, (per
    //   design requirements) ANY such setting must invalidate a
    //   duration because it should then be recalculated.
    //--------------------------------------------------------------


    public void clear() {
        super.clear();
        clearEventNoteData();
    } // end clear


    // This is (may be?) needed as a separate method because of base
    //  class behaviors calling base clears.
    public void clearEventNoteData() {
        dateEventStart = null;
        dateEventEnd = null;
        strDateFormat = "";
        strLocation = "";
        strRecurrence = "";
        intDatKnown = 0;
        blnRetainNote = true;
    } // end clearEventNoteData


    public String getDateFormat() {
        return strDateFormat;
    }

    public Long getDurationValue() {
        if (strDurationUnits == null) {
            // This can happen when an EventNoteData is loaded via
            //   serialization, as opposed to construction and
            //   setting the individual members.  In that case
            //   the transient values will not have been initialized.
            blnSettingDuration = true;
            recalcDuration();
            blnSettingDuration = false;
        } // end if

        return lngDurationValue;
    } // end getDurationValue


    public String getDurationUnits() {
        if (strDurationUnits == null) {
            // This can happen when an EventNoteData is loaded via
            //   serialization, as opposed to construction and
            //   setting the individual members.  In that case
            //   the transient values will not have been initialized.
            blnSettingDuration = true;
            recalcDuration();
            blnSettingDuration = false;
        } // end if

        return strDurationUnits;
    } // end getDurationUnits


    public Date getEndDate() {
        if (!isEndDateKnown()) return null;

        // If we are managing the intDatKnown variable properly
        //   then the dateEventEnd should not be null...

        // Initialize the temporary Calendar to the Event End
        calTmp.setTime(dateEventEnd);

        // Maximize the time of the temporary calendar to
        //   the last possible second of this day.
        calTmp.set(Calendar.HOUR_OF_DAY, 23);
        calTmp.set(Calendar.MINUTE, 59);
        calTmp.set(Calendar.SECOND, 59);

        // We set the time to the last second of the day because as
        //   an 'End Date' it may be tested against a deadline or
        //   similar value.  In that case we don't want to be subject
        //   to times that were set randomly set as a result of setting
        //   a date-only value.  If the user expects that the time
        //   component of the EventEnd is a valid value then they
        //   should be calling 'getEventEnd' rather than this method.

        // Return the Date extracted from the temporary Calendar.
        return calTmp.getTime();
    } // end getEndDate


    // Note that since the associated Date is irrelevant but
    //   not removable, we take the easiest route and simply
    //   return the entire EventEnd, if the time is known
    //   at all, that is.
    public Date getEndTime() {
        if (!isEndTimeKnown()) return null;
        return dateEventEnd;
    } // end getEndTime


    // Used in Duration calculations.
    public Date getEventEnd() {
        return dateEventEnd;
    }

    public Date getEventStart() {
        return dateEventStart;
    }


    public String getLocation() {
        return strLocation;
    }

    //  @Override not needed, but trying to learn annotations..
    @Override
    protected Date getNoteDate() {
        return getStartDate();
    } // end getNoteDate

    public String getRecurrence() {
        return strRecurrence;
    }

    public boolean getRetainNote() {
        return blnRetainNote;
    }


    public static String getRecurrenceSummary(String strSetting) {
        if (strSetting.equals("")) return "None";

        String strRecurSummary = "";
        String strDescription;

        int intUnderscore1 = strSetting.indexOf('_');
        int intUnderscore2 = strSetting.lastIndexOf('_');

        if (strSetting.startsWith("D")) {
            strRecurSummary += "Repeats at ";
            strRecurSummary += strSetting.substring(1, intUnderscore1);
            strRecurSummary += " day intervals";
        } else if (strSetting.startsWith("W")) {
            strDescription = strSetting.substring(intUnderscore1 + 1, intUnderscore2);
            int intDayCount = strDescription.length() / 2;
            if (intDayCount > 1) {
                strRecurSummary += "Repeats on " + intDayCount + " days of the week at ";
            } else {
                if (strDescription.equals("Su")) {
                    strRecurSummary += "Repeats on Sundays at ";
                } else if (strDescription.equals("Mo")) {
                    strRecurSummary += "Repeats on Mondays at ";
                } else if (strDescription.equals("Tu")) {
                    strRecurSummary += "Repeats on Tuesdays at ";
                } else if (strDescription.equals("We")) {
                    strRecurSummary += "Repeats on Wednesdays at ";
                } else if (strDescription.equals("Th")) {
                    strRecurSummary += "Repeats on Thursdays at ";
                } else if (strDescription.equals("Fr")) {
                    strRecurSummary += "Repeats on Fridays at ";
                } else if (strDescription.equals("Sa")) {
                    strRecurSummary += "Repeats on Saturdays at ";
                } // end if
            }
            strRecurSummary += strSetting.substring(1, intUnderscore1);
            strRecurSummary += " week intervals";
        } else if (strSetting.startsWith("M")) {
            strRecurSummary += "Repeats at ";
            strRecurSummary += strSetting.substring(1, intUnderscore1);
            strRecurSummary += " month intervals on ";
            strDescription = strSetting.substring(intUnderscore1 + 1, intUnderscore2);
            strRecurSummary += strDescription;
        } else {  // Year
            strRecurSummary += "Repeats on ";
            strDescription = strSetting.substring(intUnderscore1 + 1, intUnderscore2);
            strRecurSummary += strDescription;
            strRecurSummary += " of every year";
        } // end if

        if (!strSetting.endsWith("_")) {
            String strRecurEnd;
            strRecurEnd = strSetting.substring(intUnderscore2 + 1);
            if (strRecurEnd.length() < 4) { // Stop After
                strRecurSummary += " and stops after ";
                strRecurSummary += strRecurEnd + " times";
            } else {                       // Stop By
                sdf.applyPattern("yyyyMMdd");
                try {
                    Date d = sdf.parse(strRecurEnd);
                    sdf.applyPattern("EEEE MMM dd, yyyy");
                    strRecurSummary += " and stops by ";
                    strRecurSummary += sdf.format(d);
                } catch (Exception pe) {
                }
            }
        } // end if there is an end recurrence range

        return strRecurSummary;
    } // end getRecurrenceSummary


    // Return the Date only (no time component)
    public Date getStartDate() {
        if (!isStartDateKnown()) return null;

        // If we are managing the intDatKnown variable properly
        //   then when we get to this point the dateEventStart
        //   should not be null...

        // Initialize the temporary Calendar to the Event Start
        calTmp.setTime(dateEventStart);

        // Truncate the time from the temporary calendar
        calTmp.set(Calendar.HOUR, 0);
        calTmp.set(Calendar.MINUTE, 0);
        calTmp.set(Calendar.SECOND, 0);

        // Return the Date extracted from the temporary Calendar.
        return calTmp.getTime();
    } // end getStartDate


    // Note that since the associated Date is irrelevant but
    //   not removable, we take the easiest route and simply
    //   return the entire EventStart, if the time is known
    //   at all, that is.
    public Date getStartTime() {
        if (!isStartTimeKnown()) return null;
        return dateEventStart;
    } // end getStartTime


    //---------------------------------------------------
    // Method Name: getSummary
    //
    // Returns a textual description of the event, in
    //   an HTML-formatted string.
    //---------------------------------------------------
    public String getSummary() {
        String strTmp;
        String strTheSummary = "";

        // Category
        if (subjectString == null) strTmp = "This event";
        else {
            // The length of this abbreviation was chosen from the known
            //   constraints of the editor interface that provides the
            //   information - ie, font, viewable length of the text in
            //   the edit control.  There may be a better way...
            if (subjectString.length() > 60) {
                strTmp = subjectString.substring(0, 60);
                strTmp += "...";
            } else {
                strTmp = subjectString;
            }
        } // end if we have a category (subject)

        // Convert potentially malicious characters.
        strTmp = strTmp.replace("&", "&amp;");
        strTmp = strTmp.replace("<", "&lt;");
        strTheSummary += strTmp;

        // Event Start
        if (isStartDateKnown()) {
            strTheSummary += " starts on ";
            sdf.applyPattern("EEEE MMMM dd, yyyy");
            strTheSummary += sdf.format(dateEventStart);
            if (isStartTimeKnown()) {
                strTheSummary += " at ";
                sdf.applyPattern("hh:mm a");
                strTheSummary += sdf.format(dateEventStart);
            }
        } else if (isStartTimeKnown()) {
            strTheSummary += " starts on an unknown date at ";
            sdf.applyPattern("hh:mm a");
            strTheSummary += sdf.format(dateEventStart);
        } else {  // unknown start date and time
            strTheSummary += " has an unknown start";
        } // end if

        // Duration
        if (getDurationValue() != null) {
            strTheSummary += " and lasts for ";
            strTheSummary += String.valueOf(lngDurationValue);
            strTheSummary += " " + strDurationUnits;
        } // end if

        // End the sentence; do not let it run on even if
        //   we don't have much here yet.
        strTheSummary += ".&nbsp; &nbsp;";

        // Location
        if (!strLocation.trim().equals("")) {

            strTheSummary += "Location: ";

            // The length of this abbreviation was chosen from the known
            //   constraints of the editor interface that provides the
            //   information - ie, font, viewable length of the text in
            //   the edit control.  There may be a better way...
            if (strLocation.length() > 70) {
                strTmp = strLocation.substring(0, 70);
                strTmp += "...";
            } else {
                strTmp = strLocation;
            } // end if too long

            // Convert potentially malicious characters.
            strTmp = strTmp.replace("&", "&amp;");
            strTmp = strTmp.replace("<", "&lt;");
            if (!strTmp.endsWith(".")) strTmp += ".";

            strTheSummary += strTmp;
            strTheSummary += "&nbsp; &nbsp;";
        } // end if

        // We may be able to say when it ends, but we
        //   should only do so if it ends on a different day
        //   (or if we do not know the start date).
        sdf.applyPattern("EEEE MMMM dd, yyyy");
        if (isEndDateKnown()) {
            if (!isEndSameDay()) {
                strTheSummary += "It ends on " + sdf.format(dateEventEnd);
                strTheSummary += ".&nbsp; &nbsp;";
            }
        }

        if (!strRecurrence.trim().equals("")) {
            strTheSummary += getRecurrenceSummary(strRecurrence);
            strTheSummary += ".";
        } // end if

        strTheSummary = strTheSummary.trim();

        // We wrap this in HTML here rather than a calling context,
        //   because we also have embedded &nbsp; and we have
        //   provided no linefeeds.
        strTheSummary = "<html>" + strTheSummary + "</html>";

        return strTheSummary;
    } // end getSummary


    //------------------------------------------------------
    // Method Name: goForward
    //
    // This method will examine its recurrence setting and
    //   then will either adjust its start and end (and
    //   recurrence 'stop after' setting, if it has one) and
    //   return true, or it will find that the recurrence
    //   setting and/or range does not allow it, in which
    //   case it will return a false.  Since the call to
    //   this method is only made while loading, if we
    //   return a false then we do not need to make any
    //   other settings because the loader will not keep
    //   the data anyway.
    //
    // In considering the 'Stop After' count, we do not need
    //   to refuse to goForward if the count is down to one
    //   because as soon as it goes below two there is no
    //   more recurrence at all.
    //------------------------------------------------------
    public boolean goForward() {
        if (strRecurrence.equals("")) return false;

        // Unlikely that this method was even called in this
        //   case, but test it to be sure since we're using
        //   it later and expecting it to be non-null.
        if (dateEventStart == null) return false;

        int intTheInterval;
        String strDescription;

        // Preserve duration so we can restore, if needed.
        Long lngTheDuration = getDurationValue();
        if (strDurationUnits.toLowerCase().startsWith("hour")) {
            lngTheDuration = lngDurationValue * 60;
        } else if (strDurationUnits.toLowerCase().startsWith("day")) {
            lngTheDuration = lngDurationValue * 60 * 24;
        } else if (strDurationUnits.toLowerCase().startsWith("week")) {
            lngTheDuration = lngDurationValue * 60 * 24 * 7;
        } else {
            lngTheDuration = lngDurationValue;
        } // end if/else
        // Now, lngTheDuration is either in minutes, or is null.

        // System.out.println("  Duration in minutes: " + lngTheDuration);

        int intUnderscore1 = strRecurrence.indexOf('_');
        int intUnderscore2 = strRecurrence.lastIndexOf('_');

        // Calculate the proposed new start date
        calTmp.setTime(dateEventStart);
        if (strRecurrence.startsWith("D")) {
            intTheInterval = Integer.parseInt(strRecurrence.substring(1, intUnderscore1));
            calTmp.add(Calendar.DATE, intTheInterval);
        } else if (strRecurrence.startsWith("W")) {
            strDescription = strRecurrence.substring(intUnderscore1 + 1, intUnderscore2);
            intTheInterval = Integer.parseInt(strRecurrence.substring(1, intUnderscore1));

            int intTmp;
            while (true) {
                // If we are at the end of the week, jump the
                //   interval before we add another day.
                intTmp = calTmp.get(Calendar.DAY_OF_WEEK);
                if (intTmp == Calendar.SATURDAY) {
                    if (intTheInterval > 1) calTmp.add(Calendar.DATE, 7 * (intTheInterval - 1));
                } // end if

                calTmp.add(Calendar.DATE, 1); // Add one day.

                // Now check to see if it 'counts'.
                intTmp = calTmp.get(Calendar.DAY_OF_WEEK);
                if (intTmp == Calendar.SUNDAY) {
                    if (strDescription.contains("Su")) break;
                } else if (intTmp == Calendar.MONDAY) {
                    if (strDescription.contains("Mo")) break;
                } else if (intTmp == Calendar.TUESDAY) {
                    if (strDescription.contains("Tu")) break;
                } else if (intTmp == Calendar.WEDNESDAY) {
                    if (strDescription.contains("We")) break;
                } else if (intTmp == Calendar.THURSDAY) {
                    if (strDescription.contains("Th")) break;
                } else if (intTmp == Calendar.FRIDAY) {
                    if (strDescription.contains("Fr")) break;
                } else if (intTmp == Calendar.SATURDAY) {
                    if (strDescription.contains("Sa")) break;
                } // end testing to see if the day matters
            } // end while
        } else if (strRecurrence.startsWith("M")) {
            strDescription = strRecurrence.substring(intUnderscore1 + 1, intUnderscore2);
            intTheInterval = Integer.parseInt(strRecurrence.substring(1, intUnderscore1));
            calTmp.setTime(goForwardMonths(intTheInterval, strDescription));
        } else {  // Year
            strDescription = strRecurrence.substring(intUnderscore1 + 1, intUnderscore2);
            calTmp.setTime(goForwardMonths(12, strDescription));
        } // end if

        // Examine our Recurrence Range End
        if (!strRecurrence.endsWith("_")) {
            String strRecurEnd;
            strRecurEnd = strRecurrence.substring(intUnderscore2 + 1);
            if (strRecurEnd.length() < 4) {
                // Adjust the 'Stop After' value
                int intAfter = Integer.parseInt(strRecurEnd);
                intAfter--; // Decrease the 'Stop After' by one
                if (intAfter > 1) {
                    strRecurrence = strRecurrence.substring(0, intUnderscore2 + 1);
                    strRecurrence += String.valueOf(intAfter);
                } else {
                    // Now we are down to the last one so remove the recurrence
                    //   for next time but we still goForward.
                    strRecurrence = "";
                } // end if there will be more
            } else {                       // Stop By
                sdf.applyPattern("yyyyMMdd");
                try {
                    Date d = sdf.parse(strRecurEnd);
                    if (d.before(calTmp.getTime())) return false;
                } catch (Exception pe) {
                }
            } // end if/else - Stop    After or By
        } // end if there is an end recurrence range

        // Preserve the value from calTmp from unintended changes
        //   that the other 'set' methods will make.
        Date dateTheNewStart = calTmp.getTime();

        String strKeepRecurrence = strRecurrence;
        // System.out.println("  Adjusting start date to: " + dateTheNewStart);
        setEndDate(null);
        setStartDate(dateTheNewStart);
        if (lngTheDuration != null) setDuration(lngTheDuration);
        strRecurrence = strKeepRecurrence;

        return true;
    } // end goForward


    //------------------------------------------------------------
    // Method Name: goForwardMonths
    //
    // Calculates and returns the end date that occurs after
    //   the specified number of input months, adjusted to
    //   fit the pattern.
    //------------------------------------------------------------
    private Date goForwardMonths(int months, String strMonthPattern) {
        Date dateTheEndDate;
        calTmp.setTime(dateEventStart);

        // Get our start day, for multiple uses below.
        String strWhichOne = "first";
        int intDayOfWeek = calTmp.get(Calendar.DAY_OF_WEEK);

        // Keep the last known 'good' date, as we scan forward.
        Date dateGood;

        // This calculation works for a simple numeric date and
        // does not consider the Monthly pattern.
        calTmp.add(Calendar.MONTH, months);
        dateTheEndDate = calTmp.getTime();

        // Preserve the month value.
        int intMonth = calTmp.get(Calendar.MONTH);

        // Examine the user-selected recurrence pattern.
        if (Character.isDigit(strMonthPattern.charAt(4))) {
            // If the pattern is the simple numeric, then we are done.
        } else if (strMonthPattern.toLowerCase().contains("weekend")) {
            // System.out.println("generalized - weekend");
            // Now set the calendar to the first one in this month -
            calTmp.set(Calendar.DAY_OF_MONTH, 1);
            while (RecurrencePanel.isWeekday(calTmp)) calTmp.add(Calendar.DATE, 1);
            dateGood = calTmp.getTime();
            // System.out.println("Adjusted to correct day: " + calTmp.getTime());

            while (!strMonthPattern.toLowerCase().contains(strWhichOne)) {
                if (strWhichOne.equals("first")) strWhichOne = "second";
                else if (strWhichOne.equals("second")) strWhichOne = "third";
                else if (strWhichOne.equals("third")) strWhichOne = "fourth";
                else strWhichOne = "keep going...";

                calTmp.add(Calendar.DATE, 1); // add a day

                // and keep going, if we need to,
                // to get to the next weekend day.
                while (RecurrencePanel.isWeekday(calTmp)) calTmp.add(Calendar.DATE, 1);

                // System.out.println(strWhichOne + " " + calTmp.getTime());
                if (calTmp.get(Calendar.MONTH) != intMonth) {
                    // System.out.println("Shot past - resetting.");
                    calTmp.setTime(dateGood);
                    break;
                } else {
                    if (!RecurrencePanel.isWeekday(calTmp)) dateGood = calTmp.getTime();
                } // end if/else
            } // end while
            dateTheEndDate = calTmp.getTime();
        } else if (strMonthPattern.toLowerCase().contains("weekday")) {
            // System.out.println("generalized - weekday");
            // Now set the calendar to the first one in this month -
            calTmp.set(Calendar.DAY_OF_MONTH, 1);
            while (!RecurrencePanel.isWeekday(calTmp)) calTmp.add(Calendar.DATE, 1);
            dateGood = calTmp.getTime();
            // System.out.println("Adjusted to correct day: " + calTmp.getTime());

            while (!strMonthPattern.contains(strWhichOne)) {
                if (strWhichOne.equals("first")) strWhichOne = "second";
                else if (strWhichOne.equals("second")) strWhichOne = "third";
                else if (strWhichOne.equals("third")) strWhichOne = "fourth";
                else strWhichOne = "keep going...";

                calTmp.add(Calendar.DATE, 1); // add a day

                // and keep going, if we need to,
                // to get to the next weekend day.
                while (!RecurrencePanel.isWeekday(calTmp)) calTmp.add(Calendar.DATE, 1);

                // System.out.println(strWhichOne + " " + calTmp.getTime());
                if (calTmp.get(Calendar.MONTH) != intMonth) {
                    // System.out.println("Shot past - resetting.");
                    calTmp.setTime(dateGood);
                    break;
                } else {
                    if (RecurrencePanel.isWeekday(calTmp)) dateGood = calTmp.getTime();
                } // end if/else
            } // end while
            dateTheEndDate = calTmp.getTime();
        } else if (strMonthPattern.toLowerCase().contains("last day")) {
            // System.out.println("last day");
            while (true) {
                dateGood = calTmp.getTime();
                calTmp.add(Calendar.DATE, 1); // add a day

                if (calTmp.get(Calendar.MONTH) != intMonth) {
                    // System.out.println("Shot past - resetting.");
                    calTmp.setTime(dateGood);
                    break;
                } // end if
            } // end while
            dateTheEndDate = calTmp.getTime();
        } else {
            // System.out.println("specific day");
            // Now set the calendar to the first one in this month -
            calTmp.set(Calendar.DAY_OF_MONTH, 1);
            while (calTmp.get(Calendar.DAY_OF_WEEK) != intDayOfWeek) {
                calTmp.add(Calendar.DATE, 1);
            } // end while
            // System.out.println("Adjusted to correct day: " + calTmp.getTime());

            while (!strMonthPattern.contains(strWhichOne)) {
                if (strWhichOne.equals("first")) strWhichOne = "second";
                else if (strWhichOne.equals("second")) strWhichOne = "third";
                else if (strWhichOne.equals("third")) strWhichOne = "fourth";
                else strWhichOne = "keep going...";

                dateGood = calTmp.getTime();
                calTmp.add(Calendar.DATE, 7); // add a week

                // System.out.println(strWhichOne + " " + calTmp.getTime());
                if (calTmp.get(Calendar.MONTH) != intMonth) {
                    // System.out.println("Shot past - resetting.");
                    calTmp.setTime(dateGood);
                    break;
                } // end if
            } // end while
            dateTheEndDate = calTmp.getTime();
        } // end if/else - general or specific or last

        return dateTheEndDate;
    } // end goForwardMonths


    //----------------------------------------------------------------
    // Method Name: getDayNoteData
    //
    // Returns a DayNoteData that is made from this EventNoteData.
    //   DayNoteData is not robust enough to hold all the discrete data
    //   elements from here but it CAN hold text that can adequately describe the
    //   the additional fields, and this is good enough for our purposes, which
    //   is to prepare this event to be archived in the Notes.
    // Note that although a Day does not usually hold its correct calendar date
    //   in the 'time' field, in this case it must, in order for
    //   NoteGroup.addNote to place it in the correct file.
    //----------------------------------------------------------------
    public DayNoteData getDayNoteData() {
        DayNoteData dnd = new DayNoteData(this);
        dnd.setTimeOfDayDate(dateEventStart);
        dnd.setSubjectString("Event");

        // Insert the Event category (if there is one) into
        //   the extended note.
        String s = this.extendedNoteString;
        if (this.subjectString != null) {
            s = this.subjectString + "\n" + s;
        } // end if

        // And append the location, if there is one.
        if (!this.strLocation.trim().equals("")) {
            s += "\nLocation: " + this.strLocation;
        } // end if
        dnd.setExtendedNoteString(s);

        return dnd;
    } // end getDayNoteData


    //---------------------------------------------------------
    // Method Name: hasStarted
    //
    // Compares the known Event start to the current time.
    //---------------------------------------------------------
    public boolean hasStarted() {
        Date datNow = new Date();

        if (!isStartDateKnown()) return false;

        if (isStartTimeKnown()) return datNow.after(dateEventStart);
        else return datNow.after(getStartDate());
    } // end hasStarted


    //------------------------------------------------------------
    // Method Name: isAnyKnown
    //
    // Returns a true if any of the four boundary
    //   chronological fields is known.
    //------------------------------------------------------------
    public boolean isAnyKnown() {
        return (intDatKnown > 0);
    } // end isAnyKnown


    //------------------------------------------------------------
    // Method Name: isAnyUnknown
    //
    // Returns a true if any of the four boundary
    //   chronological fields is not known.
    //------------------------------------------------------------
    public boolean isAnyUnknown() {
        int total;

        total = START_DATE_KNOWN + START_TIME_KNOWN +
                END_DATE_KNOWN + END_TIME_KNOWN;

        return (intDatKnown < total);
    } // end isAnyUnknown


    public boolean isEndDateKnown() {
        boolean b = (intDatKnown & END_DATE_KNOWN) == END_DATE_KNOWN;
        return b;
    }


    //---------------------------------------------------
    // Method Name: isEndSameDay
    //
    // Answers the question - does the event
    //   end on the same day that it started?
    //---------------------------------------------------
    public boolean isEndSameDay() {
        if (!isStartDateKnown()) return false; // Can't determine == no
        if (!isEndDateKnown()) return false;

        calTmp.setTime(dateEventStart);
        int intTheYear = calTmp.get(Calendar.YEAR);
        int intTheDay = calTmp.get(Calendar.DAY_OF_YEAR);

        calTmp.setTime(dateEventEnd);
        if (calTmp.get(Calendar.YEAR) != intTheYear) return false;
        if (calTmp.get(Calendar.DAY_OF_YEAR) != intTheDay) return false;
        return true;
    } // end isEndSameDay


    public boolean isEndTimeKnown() {
        boolean b = (intDatKnown & END_TIME_KNOWN) == END_TIME_KNOWN;
        return b;
    } // end isEndTimeKnown


    public boolean isStartDateKnown() {
        boolean b = (intDatKnown & START_DATE_KNOWN) == START_DATE_KNOWN;
        return b;
    }


    public boolean isStartTimeKnown() {
        boolean b = (intDatKnown & START_TIME_KNOWN) == START_TIME_KNOWN;
        return b;
    }


    //---------------------------------------------------------
    // Method Name: isTimesKnown
    //
    // Returns true if both the Start Time and the End Time
    //   are known; otherwise returns false.
    //---------------------------------------------------------
    public boolean isTimesKnown() {
        boolean b = (isStartTimeKnown() && isEndTimeKnown());
        return b;
    } // end isTimesKnown


    //-----------------------------------------------------------
    // Method Name: recalcDuration
    //
    // There are 17 distinct cases to cover - a duration
    //   determination is only possible for 6 of them.
    //
    // If the calculation cannot be made, a false is returned.
    // If the calculation can be made, the return value is true
    //   and the duration (long) value
    //   will be in units of Minutes, since this is currently
    //   the finest granularity of the EventEditor.
    //-----------------------------------------------------------
    private boolean recalcDuration() {
        boolean blnDatePair = isStartDateKnown() & isEndDateKnown();
        boolean blnTimePair = isStartTimeKnown() & isEndTimeKnown();
        boolean blnAnyKnown;

        blnAnyKnown = isStartDateKnown() || isEndDateKnown() ||
                isStartTimeKnown() || isEndTimeKnown();

        // Defaults
        lngDurationValue = null;
        strDurationUnits = "unknown";

        // First, cover the 11 cases where we know we can't.
        //---------------------------------------------------------
        if (dateEventEnd == null) return false;   // 4 cases
        if (dateEventStart == null) return false; // 4 cases
        // (but one of the 8 is an overlap so there are still 4 more)

        // Start (Date known, Time not), End (Date not, Time known)
        // Start (Date not, Time known), End (Date known, Time not)
        if (blnAnyKnown && !blnDatePair && !blnTimePair) return false;

        // We know both times but only one or the other date
        if (blnTimePair) {
            if (isStartDateKnown() && !isEndDateKnown()) return false;
            if (!isStartDateKnown() && isEndDateKnown()) return false;
        }
        //---------------------------------------------------------

        // Now, presumably, we have enough to work with.

        strDurationUnits = "minute";

        // Perform the (same) calculation for each of the remaining 6 cases.
        // If a 'placeholder' value was set in order to hold a duration
        //   value then this calculation will include it.
        lngDurationValue = dateEventEnd.getTime() - dateEventStart.getTime();

        // Convert to Seconds (from milliseconds)
        lngDurationValue /= 1000;  // Up to .999 second loss of precision.

        // Convert to Minutes (from seconds)
        lngDurationValue /= 60;  // Up to 59 seconds loss of precision.

        // The strict handling of user entry of known dates and times
        // is expected to prevent 'phantom' durations and to keep the
        // result as a positive value, or if negative it will have a
        // magnitude of less than one day, which we can normalize by
        // adding 1440 minutes (one day).
        if (lngDurationValue < 0) lngDurationValue += 1440;


        // Debug section for duration values -
//    System.out.println("Duration value: " + lngDurationValue);
//    System.out.println("  Start Date known: " + end.isStartDateKnown());
//    System.out.println("  Start Time known: " + end.isStartTimeKnown());
//    System.out.println("  End Date known: " + end.isEndDateKnown());
//    System.out.println("  End Time known: " + end.isEndTimeKnown());

        // Now compress the units, if reasonable to do so.
        //--------------------------------------------------------
        // We know that our display field can show a maximum value
        //   of five digits (ie, 99999), so first we should check to
        //   see if we have an 'overflow' condition, and prevent it.
        if (lngDurationValue > 99999) {
            // Convert Minutes to Hours
            lngDurationValue /= 60;
            strDurationUnits = "hour";

            if (lngDurationValue > 99999) {
                // Convert Hours to Days
                lngDurationValue /= 24;
                strDurationUnits = "day";

                if (lngDurationValue > 99999) {
                    // Convert Days to Weeks
                    lngDurationValue /= 7;
                    strDurationUnits = "week";

                    if (lngDurationValue > 99999) { // Just too big to show.
                        return false;
                    }
                } // end if Days is too small a unit
            } // end if Hours is too small a unit
        } // end if Minutes is too small a unit

        boolean blnShrink = false;
        // Determine whether the granularity of the duration
        //   calculation should be finer than 'days'.

        // If either time component (or both) is not known then
        //   we should only show Days or Weeks, UNLESS we are
        //   here due to an explicit duration setting.
        if (!isStartTimeKnown() || !isEndTimeKnown()) {
            if (!blnSettingDuration) blnShrink = true;
        } // end if

        if (blnShrink) {
            if (strDurationUnits.equals("minute")) {
                lngDurationValue /= (60 * 24);
                strDurationUnits = "Days";
            } // end if
            if (strDurationUnits.equals("hour")) {
                lngDurationValue /= 24;
                strDurationUnits = "day";
            } // end if
        } // end if

        // If we have more than an hour's worth of minutes
        if (strDurationUnits.equals("minute") && (lngDurationValue >= 60)) {

            // And if we would not lose precision by converting...
            if ((lngDurationValue % 60) == 0) {
                // Then convert to Hours.
                lngDurationValue /= 60;
                strDurationUnits = "hour";
            }
        }

        // If we have more than a day's worth of hours
        if (strDurationUnits.equals("hour") && (lngDurationValue >= 24)) {

            // And if we would not lose precision by converting...
            if ((lngDurationValue % 24) == 0) {
                // Then convert to Days.
                lngDurationValue /= 24;
                strDurationUnits = "day";
            }
        }

        // If we have more than a week's worth of days
        if (strDurationUnits.equals("Days") && (lngDurationValue >= 7)) {

            // And if we would not lose precision by converting...
            if ((lngDurationValue % 7) == 0) {
                // Then convert to Weeks.
                lngDurationValue /= 7;
                strDurationUnits = "week";
            }
        }

        // A duration of zero should be in units of either minutes
        //   or Days - Minutes if the user has specified both time
        //   components; otherwise days.
        if (lngDurationValue == 0) {
            strDurationUnits = "minutes";
            if (!isStartTimeKnown() || !isEndTimeKnown()) {
                if (isStartDateKnown() && isEndDateKnown()) {
                    // If we don't know one (or both) of the time fields
                    //   but we do know the date (a duration of zero
                    //   indicates that it is the same value in both fields).
                    strDurationUnits = "days";
                }
            }
        } // end if duration is zero

        if (lngDurationValue > 1) strDurationUnits += "s";

        return true;
    } // end recalcDuration


    public void setDateFormat(String s) {
        strDateFormat = s;
    }

    //------------------------------------------------------
    // Method Name: setDuration
    //
    // The input parameter should be in Minutes.
    //
    // Internals Note:  If a previous call to this method
    //   (with the same input value) caused an unknown Start
    //   field to be calculated then the duration
    //   calculation made now will be a different one
    //   (although the result should be the same).  Also, if
    //   a new instance was used for this second call (after
    //   setting all the known values) and if a
    //   placeholder had initially been put into a Start
    //   field, it will now be placed in the End field.
    //------------------------------------------------------
    public void setDuration(long lngDuration) {
        long tmpLong;
        blnSettingDuration = true;

        // Convert the input to milliseconds.
        lngDuration *= (60 * 1000);

        // The actions taken will depend on which values
        //   are already known.  There are 16 unique cases.
        //   The line numbers in the comments refer to the
        //   related table in the documentation.
        // We DO NOT simply set our Event dates to the calculation
        //   results and then call the 'set' methods with those
        //   values, because the method calls will change them.
        // So, we call the 'set' methods with a new Date each time
        //   and only directly manipulate the Event dates when
        //   assigning a placeholder value, AFTERwards, because
        //   those methods are written to drop out such values.
        //--------------------------------------------------------
        if (intDatKnown == 0) {
            // Line 1 - p(SD ST ED ET)
            // Nothing already known; the two internal dates
            //   must contain 'placeholder' values, and they
            //   should be set without affecting the 'known'
            //   status tracking value (intDatKnown).
            dateEventStart = new Date();
            tmpLong = dateEventStart.getTime();
            dateEventEnd = new Date(tmpLong + lngDuration);
        } else if (intDatKnown == END_TIME_KNOWN) {
            // Line 2 - p(SD) & ST
            tmpLong = dateEventEnd.getTime();
            setStartTime(new Date(tmpLong - lngDuration));    // ST
            dateEventStart = new Date(tmpLong - lngDuration); // p(SD)
        } else if (intDatKnown == END_DATE_KNOWN) {
            // Line 3 - SD & p(ST)
            tmpLong = dateEventEnd.getTime();
            setStartDate(new Date(tmpLong - lngDuration));    // SD
            dateEventStart = new Date(tmpLong - lngDuration); // p(ST)
        } else if (intDatKnown == (END_DATE_KNOWN + END_TIME_KNOWN)) {
            // Line 4 - SD & ST
            tmpLong = dateEventEnd.getTime();
            setStartDate(new Date(tmpLong - lngDuration));  // SD
            setStartTime(new Date(tmpLong - lngDuration));  // ST
        } else if (intDatKnown == START_TIME_KNOWN) {
            // Line 5 - p(ED) & ET
            tmpLong = dateEventStart.getTime();
            setEndTime(new Date(tmpLong + lngDuration));     // ET
            dateEventEnd = new Date(tmpLong + lngDuration);  // p(ED)
        } else if (intDatKnown == (START_TIME_KNOWN + END_TIME_KNOWN)) {
            // Line 6 - rET & p(ED)
            tmpLong = dateEventStart.getTime();
            setEndTime(new Date(tmpLong + lngDuration));     // rET
            dateEventEnd = new Date(tmpLong + lngDuration);  // p(ED)
        } else if (intDatKnown == (START_TIME_KNOWN + END_DATE_KNOWN)) {
            // Line 7 - SD & ET
            // Less straightforward on how to proceed; here is the logic:
            // Add the duration to the start time so that we can have
            // a known end time.  Then use the composite end to get the
            // correct start date.
            tmpLong = dateEventStart.getTime();
            setEndTime(new Date(tmpLong + lngDuration));    // ET
            tmpLong = dateEventEnd.getTime();
            setStartDate(new Date(tmpLong - lngDuration));  // SD
        } else if (intDatKnown == (START_TIME_KNOWN + END_KNOWN)) {
            // Line 8 - SD & rST
            tmpLong = dateEventEnd.getTime();
            setStartDate(new Date(tmpLong - lngDuration));  // SD
            setStartTime(new Date(tmpLong - lngDuration));  // rST
        } else if (intDatKnown == START_DATE_KNOWN) {
            // Line 9 - ED & p(ET)
            tmpLong = dateEventStart.getTime();
            setEndDate(new Date(tmpLong + lngDuration));    // ED
            dateEventEnd = new Date(tmpLong + lngDuration); // p(ET)
        } else if (intDatKnown == (START_DATE_KNOWN + END_TIME_KNOWN)) {
            // Line 10 - ST & ED
            // Less straightforward on how to proceed; here is the logic:
            // Subtract the duration from the end time so that we can have
            // a known start time.  Then use the composite start to get the
            // correct end date.
            tmpLong = dateEventEnd.getTime();
            setStartTime(new Date(tmpLong - lngDuration));  // ST
            tmpLong = dateEventStart.getTime();
            setEndDate(new Date(tmpLong + lngDuration));    // ED
        } else if (intDatKnown == (START_DATE_KNOWN + END_DATE_KNOWN)) {
            // Line 11 - rED & p(ET)
            tmpLong = dateEventStart.getTime();
            setEndDate(new Date(tmpLong + lngDuration));     // rED
            dateEventEnd = new Date(tmpLong + lngDuration);  // p(ET)
        } else if (intDatKnown == (START_DATE_KNOWN + END_KNOWN)) {
            // Line 12 - rSD & ST
            tmpLong = dateEventEnd.getTime();
            setStartDate(new Date(tmpLong - lngDuration));  // rSD
            setStartTime(new Date(tmpLong - lngDuration));  // ST
        } else if (intDatKnown == (START_KNOWN)) {
            // Line 13 - ED & ET
            tmpLong = dateEventStart.getTime();
            setEndDate(new Date(tmpLong + lngDuration)); // ED
            setEndTime(new Date(tmpLong + lngDuration)); // ET
        } else if (intDatKnown == (START_KNOWN + END_TIME_KNOWN)) {
            // Line 14 - ED & rET
            tmpLong = dateEventStart.getTime();
            setEndDate(new Date(tmpLong + lngDuration)); // ED
            setEndTime(new Date(tmpLong + lngDuration)); // rET
        } else if (intDatKnown == (START_KNOWN + END_DATE_KNOWN)) {
            // Line 15 - rED & ET
            tmpLong = dateEventStart.getTime();
            setEndDate(new Date(tmpLong + lngDuration)); // rED
            setEndTime(new Date(tmpLong + lngDuration)); // ET
        } else if (intDatKnown == (START_KNOWN + END_KNOWN)) {
            // Line 16 - rED & rET
            tmpLong = dateEventStart.getTime();
            setEndDate(new Date(tmpLong + lngDuration)); // rED
            setEndTime(new Date(tmpLong + lngDuration)); // rET
        } // end if/else - all 16 cases

        recalcDuration();
        blnSettingDuration = false;
    } // end setDuration


    //------------------------------------------------------------
    // Method Name: setEndDate
    //
    // Tests the proposed new composite End against the already
    //   established composite Start.  If it would be earlier
    //   then no change is made and the return value is false.
    //   Otherwise the setting is accepted and the return value
    //   is true.
    //------------------------------------------------------------
    public boolean setEndDate(Date d) {
        if (d == null) { // The user is 'un-setting' the date.
            if (isEndDateKnown()) intDatKnown -= END_DATE_KNOWN;
            // If we also now have no time component as well, then
            //   we can set the EventEnd to null.  If it had been
            //   in use as a Duration placeholder then this is still
            //   OK since without either End component, the duration
            //   calculation cannot be made.
            if (!isEndTimeKnown()) {
                dateEventEnd = null;
            } else { // Need to prevent phantom durations
                // This will re-initialize the 'unknown' Date component
                //   so that no unwanted duration remains.
                setEndTime(dateEventEnd);
            } // end if
            recalcDuration();
            return true;
        }

        // Put the input Date into the temporary calendar.
        calTmp.setTime(d);

        // Add the time component, if any.
        //----------------------------------------------
        if (!isEndTimeKnown()) {
            // There is no Time component - max it out.
            calTmp.set(Calendar.HOUR_OF_DAY, 23);
            calTmp.set(Calendar.MINUTE, 59);
            calTmp.set(Calendar.SECOND, 59);
            calTmp.set(Calendar.MILLISECOND, 999);
        } else {
            // There IS a time component.  Extract the input Date components
            // and then use the previous dateEventEnd (with its valid time
            // component) as the temporary calendar's base.
            int intYear = calTmp.get(Calendar.YEAR);
            int intMonth = calTmp.get(Calendar.MONTH);
            int intDay = calTmp.get(Calendar.DATE);

            calTmp.setTime(dateEventEnd);

            // Set the temporary calendar to the input Date
            //   (leaving time unmodified)
            calTmp.set(intYear, intMonth, intDay);
        }

        // Get the temporary composite End, for comparison.
        Date dateTmpEventEnd = calTmp.getTime();

        // Ensure that the proposed composite End is not before the EventStart.
        if (isStartDateKnown() && dateTmpEventEnd.before(dateEventStart)) return false;

        // Get the final Date from the temporary Calendar
        dateEventEnd = dateTmpEventEnd;

        // Set the 'known' indicator
        intDatKnown |= END_DATE_KNOWN;

        recalcDuration();
        return true;
    } // end setEndDate


    //------------------------------------------------------------
    // Method Name: setEndTime
    //
    // Tests the proposed new composite End against the composite
    //   Start, if one has already been established.  If it would
    //   be earlier then no change is made and the return value
    //   is false.  Otherwise the setting is accepted and the
    //   return value is true.
    //------------------------------------------------------------
    public boolean setEndTime(Date d) {
        if (d == null) { // The user is 'un-setting' the time.
            if (isEndTimeKnown()) intDatKnown -= END_TIME_KNOWN;

            // Now either put the date component back to the default
            //   (no time) or set the entire EventEnd to null.
            if (isEndDateKnown()) dateEventEnd = getEndDate();
            else dateEventEnd = null;
            recalcDuration();
            return true;
        }

        // Initialize the temporary Calendar to the input Date
        calTmp.setTime(d);

        // Extract the Time from the temporary calendar.
        int intHours = calTmp.get(Calendar.HOUR_OF_DAY);
        int intMinutes = calTmp.get(Calendar.MINUTE);
        int intSeconds = calTmp.get(Calendar.SECOND);
        int intMillis = 0;

        // Obtain a valid Date component for the EventEnd.
        if (isEndDateKnown()) {
            calTmp.setTime(dateEventEnd);
        } else {
            calTmp.setTime(new Date());

            // Prevent a 'phantom' duration -
            if ((dateEventStart != null) && (!isStartDateKnown())) {
                // When an unknown Start Date was initialized and retained in
                //   order to hold a known Start Time, and then a few days
                //   later an unknown End Date is initialized and retained
                //   in order to hold a known End Time (we just did that,
                //   above), the duration calculation will yield a value
                //   greater than one day even though the user has specified
                //   no date basis for such a result.  To normalize the
                //   duration to less than one day, we do the following:
                calTmp.setTime(dateEventStart);
            }
        } // end if

        // Adjust to the input's time (leaving date unmodified)
        calTmp.set(Calendar.HOUR_OF_DAY, intHours);
        calTmp.set(Calendar.MINUTE, intMinutes);
        calTmp.set(Calendar.SECOND, intSeconds);
        calTmp.set(Calendar.MILLISECOND, intMillis);

        // Get the temporary composite End, for comparison.
        Date dateTmpEventEnd = calTmp.getTime();

        // Ensure that the proposed composite End is not before the EventStart.
        if (isStartDateKnown() && isEndDateKnown()) {
            // If both dates are not known then we have
            //   no basis for comparison.
            if (dateTmpEventEnd.before(dateEventStart)) return false;
        } // end if

        // Set the composite EventEnd
        dateEventEnd = dateTmpEventEnd;

        // Set the 'known' indicator
        intDatKnown |= END_TIME_KNOWN;

        recalcDuration();
        return true;
    } // end setEndTime

    public void setLocation(String s) {
        strLocation = s;
    }

    public void setRecurrence(String s) {
        strRecurrence = s;
    }

    public void setRetainNote(boolean b) {
        blnRetainNote = b;
    }

    //------------------------------------------------------------
    // Method Name: setStartDate
    //
    // Returns an indication of success or failure.  Currently the
    //   only tested requirement is that the composite Start NOT
    //   be later than the composite End.  Otherwise, the setting
    //   is accepted and the return is true.
    //------------------------------------------------------------
    public boolean setStartDate(Date d) {
        if (d == null) { // The user is 'un-setting' the date.
            if (isStartDateKnown()) intDatKnown -= START_DATE_KNOWN;
            // If we also now have no time component as well, then
            //   we can set the entire EventStart to null.  If it had
            //   been in use as a Duration placeholder then this is
            //   still OK since without either Start component, the
            //   duration calculation cannot be made.
            if (!isStartTimeKnown()) {
                dateEventStart = null;
            } else { // Need to prevent phantom durations
                // This will re-initialize the 'unknown' Date component
                //   so that no unwanted duration remains.
                setStartTime(dateEventStart);
            } // end if
            strRecurrence = "";
            recalcDuration();
            return true;
        } // end if UNsetting a date

        // Put the input Date into the temporary calendar.
        calTmp.setTime(d);

        // Add the time component, if any.
        //----------------------------------------------
        if (!isStartTimeKnown()) {
            // There is no Time component - zero it out.
            calTmp.set(Calendar.HOUR_OF_DAY, 0);
            calTmp.set(Calendar.MINUTE, 0);
            calTmp.set(Calendar.SECOND, 0);
            calTmp.set(Calendar.MILLISECOND, 0);
        } else {
            // There IS a time component.  Extract the input Date components
            // and then use the previous dateEventStart (with its valid time
            // component) as the temporary calendar's base.
            int intYear = calTmp.get(Calendar.YEAR);
            int intMonth = calTmp.get(Calendar.MONTH);
            int intDay = calTmp.get(Calendar.DATE);
            calTmp.setTime(dateEventStart);

            // Set the temporary calendar to the input Date
            //   (leaving time unmodified)
            calTmp.set(intYear, intMonth, intDay);
        }

        // Get the temporary composite Start, for comparison.
        Date dateTmpEventStart = calTmp.getTime();

        // Ensure that the proposed composite Start is not after the EventEnd.
        if (isEndDateKnown() && dateTmpEventStart.after(dateEventEnd)) return false;

        // Get the final Date from the temporary Calendar
        dateEventStart = dateTmpEventStart;

        // Set the 'known' indicator
        intDatKnown |= START_DATE_KNOWN;

        strRecurrence = "";
        recalcDuration();
        return true;
    } // end setStartDate


    //------------------------------------------------------------
    // Method Name: setStartTime
    //
    // Tests the proposed new composite Start against the composite
    //   End, if one has already been established.  If it would
    //   be later then no change is made and the return value
    //   is false.  Otherwise the setting is accepted and the
    //   return value is true.
    // When setting a time it is true that the fields may have
    //   previously contained a duration.  However, ANY time
    //   setting must invalidate that, so our concern here is
    //   not to preserve any preexisting duration but to avoid
    //   creating any 'phantom' ones that may be generated by a
    //   time-only setting when the Date field is initialized.
    //------------------------------------------------------------
    public boolean setStartTime(Date d) {
        if (d == null) { // The user is 'un-setting' the time.
            if (isStartTimeKnown()) intDatKnown -= START_TIME_KNOWN;

            // Now either put the date component back to the default
            //   (no time) or set the entire EventStart to null.
            if (isStartDateKnown()) dateEventStart = getStartDate();
            else dateEventStart = null;
            return true;
        }

        // Initialize the temporary Calendar to the input Date
        calTmp.setTime(d);

        // Extract the Time from the temporary calendar.
        int intHours = calTmp.get(Calendar.HOUR_OF_DAY);
        int intMinutes = calTmp.get(Calendar.MINUTE);
        int intSeconds = calTmp.get(Calendar.SECOND);
        int intMillis = 0;

        // Reinitialize the temporary Calendar to the EventStart.
        if (isStartDateKnown()) {
            calTmp.setTime(dateEventStart);
        } else {
            calTmp.setTime(new Date());

            // Prevent a 'phantom' duration -
            if ((dateEventEnd != null) && (!isEndDateKnown())) {
                // When an unknown End Date was initialized and retained in
                //   order to hold a known End Time and then a few days
                //   later an unknown Start Date is initialized and retained
                //   in order to hold a known Start Time (we just did that,
                //   above), the duration calculation will yield a negative
                //   value because those Dates were not subjected to the
                //   Start/End ordering checks that the user input must go
                //   thru.  To normalize the duration to less than one day,
                //   we do the following:
                calTmp.setTime(dateEventEnd);
            }
        } // end if

        // Adjust to the input's time (leaving date unmodified)
        calTmp.set(Calendar.HOUR_OF_DAY, intHours);
        calTmp.set(Calendar.MINUTE, intMinutes);
        calTmp.set(Calendar.SECOND, intSeconds);
        calTmp.set(Calendar.MILLISECOND, intMillis);

        // Get the temporary composite Start, for comparison.
        Date dateTmpEventStart = calTmp.getTime();

        // Ensure that the proposed composite Start is not after the EventEnd.
        if (isStartDateKnown() && isEndDateKnown()) {
            // If both dates are not known then we have
            //   no basis for comparison.
            if (dateTmpEventStart.after(dateEventEnd)) return false;
        } // end if

        // Set the composite EventStart
        dateEventStart = dateTmpEventStart;

        // Set the 'known' indicator
        intDatKnown |= START_TIME_KNOWN;

        return true;
    } // end setStartTime

} // end class EventNoteData
