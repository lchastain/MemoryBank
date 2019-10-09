import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class EventNoteData extends IconNoteData {
    // Capturing these values from the old Calendar class, as it goes away -
    private static final int SUNDAY = 1;
    private static final int MONDAY = 2;
    private static final int TUESDAY = 3;
    private static final int WEDNESDAY = 4;
    private static final int THURSDAY = 5;
    private static final int FRIDAY = 6;
    private static final int SATURDAY = 7;

    private String locationString;

    private String eventStartDateString = null;
    private String eventStartTimeString;
    private String eventEndDateString = null;
    private String eventEndTimeString;

    private String recurrenceString;
    private boolean blnRetainNote;
    private Long lngDurationValue;

    transient boolean movedToDay;  // prevents dups.

    private static DateTimeFormatter dtf;


    private static transient boolean blnSettingDuration = false;
    private transient String strDurationUnits;

    public EventNoteData() {
        super();
        movedToDay = false;
        clearEventNoteData();
    } // end constructor


    // The copy constructor (clone)
    public EventNoteData(EventNoteData endCopy) {
        super();
        blnRetainNote = endCopy.blnRetainNote;
        eventStartDateString = endCopy.eventStartDateString;
        eventEndDateString = endCopy.eventEndDateString;
        eventStartTimeString = endCopy.eventStartTimeString;
        eventEndTimeString = endCopy.eventEndTimeString;
        extendedNoteHeightInt = endCopy.extendedNoteHeightInt;
        extendedNoteString = endCopy.extendedNoteString;
        extendedNoteWidthInt = endCopy.extendedNoteWidthInt;
        iconFileString = endCopy.iconFileString;
        lngDurationValue = endCopy.lngDurationValue;
        noteString = endCopy.noteString;
        showIconOnMonthBoolean = endCopy.showIconOnMonthBoolean;
        strDurationUnits = endCopy.strDurationUnits;
        locationString = endCopy.locationString;
        recurrenceString = endCopy.recurrenceString;
        subjectString = endCopy.subjectString;
    } // end constructor


    // Construct an EventNoteData from a NoteData.
    // This is used when taking Notes from NoteData interfaces.
    public EventNoteData(NoteData nd) {
        super(nd);
        clearEventNoteData();
    } // end constructor

    public void clear() {
        clearEventNoteData();
        super.clear();
    } // end clear

    // This is (or may be?) needed as a separate method because we don't always want to
    //  call the super.clear().
    private void clearEventNoteData() {
        eventStartDateString = null;
        eventEndDateString = null;
        eventStartTimeString = null;
        eventEndTimeString = null;
        locationString = "";
        recurrenceString = "";
        blnRetainNote = true;
    } // end clearEventNoteData


    @Override
    protected NoteData copy() {
        return new EventNoteData(this);
    }

    Long getDurationValue() {
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


    String getDurationUnits() {
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

    LocalDate getEndDate() {
        if (eventEndDateString == null) return null;
        return LocalDate.parse(eventEndDateString);
    }

    LocalTime getEndTime() {
        if(eventEndTimeString == null) return null;
        return LocalTime.parse(eventEndTimeString);
    }

    // Get the Event end, as the combination of the
    // end date and end time (with caveats).
    LocalDateTime getEventEndDateTime() {
        LocalDate theEndDate = getEndDate();
        if(theEndDate == null) return null;
        LocalTime theEndTime = getEndTime();
        if(theEndTime != null) {
            return theEndDate.atTime(theEndTime);
        } else {
            return theEndDate.atTime(23,59,59);
        }
    }

    // Get the Event start, as the combination of the
    // start date and start time (with caveats).
    LocalDateTime getEventStartDateTime() {
        LocalDate theStartDate = getStartDate();
        if(theStartDate == null) return null;
        LocalTime theStartTime = getStartTime();
        if(theStartTime != null) {
            return theStartDate.atTime(theStartTime);
        } else {
            return theStartDate.atStartOfDay();
        }
    }

    String getLocationString() {
        return locationString;
    }

    String getRecurrenceString() {
        return recurrenceString;
    }

    @JsonIgnore
    public boolean getRetainNote() {
        return blnRetainNote;
    }

    static String getRecurrenceSummary(@NotNull String strSetting) {
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
                switch (strDescription) {
                    case "Su":
                        strRecurSummary += "Repeats on Sundays at ";
                        break;
                    case "Mo":
                        strRecurSummary += "Repeats on Mondays at ";
                        break;
                    case "Tu":
                        strRecurSummary += "Repeats on Tuesdays at ";
                        break;
                    case "We":
                        strRecurSummary += "Repeats on Wednesdays at ";
                        break;
                    case "Th":
                        strRecurSummary += "Repeats on Thursdays at ";
                        break;
                    case "Fr":
                        strRecurSummary += "Repeats on Fridays at ";
                        break;
                    case "Sa":
                        strRecurSummary += "Repeats on Saturdays at ";
                        break;
                }
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
                dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
                try {
//                    Date d = sdf.parse(strRecurEnd);
                    LocalDate d = LocalDate.parse(strRecurEnd, dtf); // check this!
                    dtf = DateTimeFormatter.ofPattern("EEEE MMM dd, yyyy");
//                    sdf.applyPattern("EEEE MMM dd, yyyy");
                    strRecurSummary += " and stops by ";
//                    strRecurSummary += sdf.format(d);
                    strRecurSummary += dtf.format(d);
                } catch (Exception ignored) {
                }
            }
        } // end if there is an end recurrence range

        return strRecurSummary;
    } // end getRecurrenceSummary


    // Return the Date only (no time component)
    LocalDate getStartDate() {
        if (eventStartDateString == null) return null;
        return LocalDate.parse(eventStartDateString);
    }

    LocalTime getStartTime() {
        if (eventStartTimeString == null) return null;
        return LocalTime.parse(eventStartTimeString);
    }

    //---------------------------------------------------
    // Method Name: getSummary
    //
    // Returns a textual description of the event, in
    //   an HTML-formatted string.
    //---------------------------------------------------
    @JsonIgnore
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
        LocalDate theStartDate = getStartDate();
        LocalTime theStartTime = getStartTime();
        if (theStartDate != null) {
            strTheSummary += " starts on ";
            dtf = DateTimeFormatter.ofPattern("EEEE MMM dd, yyyy");
            strTheSummary += dtf.format(theStartDate);
            if (theStartTime != null) {
                strTheSummary += " at ";
                dtf = DateTimeFormatter.ofPattern("hh:mm a");
                strTheSummary += dtf.format(theStartTime);
            }
        } else if (theStartTime != null) {
            strTheSummary += " starts on an unknown date at ";
            dtf = DateTimeFormatter.ofPattern("hh:mm a");
            strTheSummary += dtf.format(theStartTime);
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
        if (!locationString.trim().equals("")) {

            strTheSummary += "Location: ";

            // The length of this abbreviation was chosen from the known
            //   constraints of the editor interface that provides the
            //   information - ie, font, viewable length of the text in
            //   the edit control.  There may be a better way...
            if (locationString.length() > 70) {
                strTmp = locationString.substring(0, 70);
                strTmp += "...";
            } else {
                strTmp = locationString;
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
        dtf = DateTimeFormatter.ofPattern("EEEE MMMM dd, yyyy");
        LocalDate theEndDate = getEndDate();
        if (theEndDate != null) {
            if (!isEndSameDay()) {
//                strTheSummary += "It ends on " + sdf.format(dateEventEnd);
                strTheSummary += "It ends on " + dtf.format(theEndDate);
                strTheSummary += ".&nbsp; &nbsp;";
            }
        }

        if (!recurrenceString.trim().equals("")) {
            strTheSummary += getRecurrenceSummary(recurrenceString);
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
    boolean goForward() {
        if (recurrenceString.equals("")) return false;

        // Unlikely that this method was even called in this
        //   case, but test it to be sure since we're using
        //   it later and expecting it to be non-null.
        LocalDate theStartDate = getStartDate();
        if (theStartDate == null) return false;

        int intTheInterval;
        String strDescription;

        // Preserve duration so we can restore, if needed.
        getDurationValue();
        Long lngTheDuration;
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

        int intUnderscore1 = recurrenceString.indexOf('_');
        int intUnderscore2 = recurrenceString.lastIndexOf('_');

        // Calculate the proposed new start date
//        calTmp.setTime(dateEventStart);
        LocalDate futureDate = theStartDate;
        if (recurrenceString.startsWith("D")) {
            intTheInterval = Integer.parseInt(recurrenceString.substring(1, intUnderscore1));
//            calTmp.add(Calendar.DATE, intTheInterval);
            futureDate = futureDate.plusDays(intTheInterval);
        } else if (recurrenceString.startsWith("W")) {
            strDescription = recurrenceString.substring(intUnderscore1 + 1, intUnderscore2);
            intTheInterval = Integer.parseInt(recurrenceString.substring(1, intUnderscore1));

            int intTmp;
            while (true) {
                // If we are at the end of the week, jump the
                //   interval before we add another day.
                // ok, doing that, but why?  need better comment here.
//                intTmp = calTmp.get(Calendar.DAY_OF_WEEK);
                intTmp = AppUtil.getDayOfWeekInt(futureDate);
                if (intTmp == SATURDAY) {
//                    if (intTheInterval > 1) calTmp.add(Calendar.DATE, 7 * (intTheInterval - 1));
                    if (intTheInterval > 1) futureDate = futureDate.plusDays(7 * (intTheInterval - 1));
                } // end if

//                calTmp.add(Calendar.DATE, 1); // Add one day.
                futureDate = futureDate.plusDays(1);

                // Now check to see if it 'counts'.
//                intTmp = calTmp.get(Calendar.DAY_OF_WEEK);
                intTmp = AppUtil.getDayOfWeekInt(futureDate);
                if (intTmp == SUNDAY) {
                    if (strDescription.contains("Su")) break;
                } else if (intTmp == MONDAY) {
                    if (strDescription.contains("Mo")) break;
                } else if (intTmp == TUESDAY) {
                    if (strDescription.contains("Tu")) break;
                } else if (intTmp == WEDNESDAY) {
                    if (strDescription.contains("We")) break;
                } else if (intTmp == THURSDAY) {
                    if (strDescription.contains("Th")) break;
                } else if (intTmp == FRIDAY) {
                    if (strDescription.contains("Fr")) break;
                } else if (intTmp == SATURDAY) {
                    if (strDescription.contains("Sa")) break;
                } // end testing to see if the day matters
            } // end while
        } else if (recurrenceString.startsWith("M")) {
            strDescription = recurrenceString.substring(intUnderscore1 + 1, intUnderscore2);
            intTheInterval = Integer.parseInt(recurrenceString.substring(1, intUnderscore1));
//            calTmp.setTime(goForwardMonths(intTheInterval, strDescription));
            futureDate = goForwardMonths(intTheInterval, strDescription); // IF startTime is not null.  ..
        } else {  // Year
            strDescription = recurrenceString.substring(intUnderscore1 + 1, intUnderscore2);
//            calTmp.setTime(goForwardMonths(12, strDescription));
            futureDate = goForwardMonths(12, strDescription); // IF startTime is not null.  ..
        } // end if

        // Examine our Recurrence Range End
        if (!recurrenceString.endsWith("_")) {
            String strRecurEnd;
            strRecurEnd = recurrenceString.substring(intUnderscore2 + 1);
            if (strRecurEnd.length() < 4) {
                // Adjust the 'Stop After' value
                int intAfter = Integer.parseInt(strRecurEnd);
                intAfter--; // Decrease the 'Stop After' by one
                if (intAfter > 1) {
                    recurrenceString = recurrenceString.substring(0, intUnderscore2 + 1);
                    recurrenceString += String.valueOf(intAfter);
                } else {
                    // Now we are down to the last one so remove the recurrence
                    //   for next time but we still goForward.
                    recurrenceString = "";
                } // end if there will be more
            } else {                       // Stop By
//                sdf.applyPattern("yyyyMMdd");
                dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
                try {
//                    Date d = sdf.parse(strRecurEnd);
                    LocalDate d = LocalDate.parse(strRecurEnd, dtf);
//                    if (d.before(calTmp.getTime())) return false;
                    if (d.isBefore(futureDate)) return false;
                } catch (Exception ignored) {
                }
            } // end if/else - Stop    After or By
        } // end if there is an end recurrence range

        // Preserve the value from calTmp from unintended changes
        //   that the other 'set' methods will make.
//        Date dateTheNewStart = calTmp.getTime();
        // ^^^ not needed; it is immutable.

        String strKeepRecurrence = recurrenceString;
        // System.out.println("  Adjusting start date to: " + dateTheNewStart);
        setEndDate(null);
//        setStartDate(dateTheNewStart);
        setStartDate(futureDate);
        if (lngTheDuration != null) setDuration(lngTheDuration);
        recurrenceString = strKeepRecurrence;

        return true;
    } // end goForward


    //------------------------------------------------------------
    // Method Name: goForwardMonths
    //
    // Calculates and returns the end date that occurs after
    //   the specified number of input months, adjusted to
    //   fit the pattern.
    //------------------------------------------------------------
    private LocalDate goForwardMonths(int months, String strMonthPattern) {
//        Date dateTheEndDate;  // Where we end up, after going forward.
        LocalDate futureDate; // Where we end up, after going forward.
//        calTmp.setTime(dateEventStart);  // eventStartDateTime

        // Get our start day, for multiple uses below.
        String strWhichOne = "first";
//        int intDayOfWeek = calTmp.get(Calendar.DAY_OF_WEEK);
        int intDayOfWeek = AppUtil.getDayOfWeekInt(getStartDate());

        // Keep the last known 'good' date, as we scan forward.
//        Date dateGood;
        LocalDate ldGood;

        // This calculation works for a simple numeric date and
        // does not consider the Monthly pattern.
//        calTmp.add(Calendar.MONTH, months);
//        dateTheEndDate = calTmp.getTime();
        futureDate = getStartDate().plusMonths(months);

        // Preserve the month value.
//        int intMonth = calTmp.get(Calendar.MONTH);
        int intMonth = futureDate.getMonthValue() - 1;

        // Examine the user-selected recurrence pattern.
        if (!Character.isDigit(strMonthPattern.charAt(4))) {
            // If the pattern is the simple numeric, then we are done.  Otherwise -
            if (strMonthPattern.toLowerCase().contains("weekend")) {
                // System.out.println("generalized - weekend");
                // Now set the calendar to the first one in this month -
//                calTmp.set(Calendar.DAY_OF_MONTH, 1);
                LocalDate tmpFutureDate = futureDate.withDayOfMonth(1);
//                while (RecurrencePanel.isWeekday(calTmp)) calTmp.add(Calendar.DATE, 1);
                while (RecurrencePanel.isWeekday(tmpFutureDate)) futureDate = futureDate.plusDays(1);
//                dateGood = calTmp.getTime();
                ldGood = tmpFutureDate;
                // System.out.println("Adjusted to correct day: " + calTmp.getTime());

                while (!strMonthPattern.toLowerCase().contains(strWhichOne)) {
                    switch (strWhichOne) {
                        case "first":
                            strWhichOne = "second";
                            break;
                        case "second":
                            strWhichOne = "third";
                            break;
                        case "third":
                            strWhichOne = "fourth";
                            break;
                        default:
                            strWhichOne = "keep going...";
                            break;
                    }

//                    calTmp.add(Calendar.DATE, 1); // add a day
                    tmpFutureDate = tmpFutureDate.plusDays(1);

                    // and keep going, if we need to,
                    // to get to the next weekend day.
//                    while (RecurrencePanel.isWeekday(calTmp)) calTmp.add(Calendar.DATE, 1);
                    while (RecurrencePanel.isWeekday(tmpFutureDate)) futureDate = futureDate.plusDays(1);

                    // System.out.println(strWhichOne + " " + calTmp.getTime());
//                    if (calTmp.get(Calendar.MONTH) != intMonth) {
                    if (futureDate.getMonthValue() - 1 != intMonth) {
                        // System.out.println("Shot past - resetting.");
//                        calTmp.setTime(dateGood);
                        tmpFutureDate = ldGood;
                        break;
                    } else {
                        //if (!RecurrencePanel.isWeekday(calTmp)) dateGood = calTmp.getTime();
                        if (!RecurrencePanel.isWeekday(tmpFutureDate)) ldGood = tmpFutureDate;
                    } // end if/else
                } // end while
//                dateTheEndDate = calTmp.getTime();
                futureDate = tmpFutureDate;
            } else if (strMonthPattern.toLowerCase().contains("weekday")) {
                // System.out.println("generalized - weekday");
                // Now set the calendar to the first one in this month -
//                calTmp.set(Calendar.DAY_OF_MONTH, 1);
                LocalDate tmpFutureDate = futureDate.withDayOfMonth(1);

//                while (!RecurrencePanel.isWeekday(calTmp)) calTmp.add(Calendar.DATE, 1);
                while (!RecurrencePanel.isWeekday(tmpFutureDate)) tmpFutureDate = tmpFutureDate.plusDays(1);
//                dateGood = calTmp.getTime();
                ldGood = tmpFutureDate;
                // System.out.println("Adjusted to correct day: " + calTmp.getTime());

                while (!strMonthPattern.contains(strWhichOne)) {
                    switch (strWhichOne) {
                        case "first":
                            strWhichOne = "second";
                            break;
                        case "second":
                            strWhichOne = "third";
                            break;
                        case "third":
                            strWhichOne = "fourth";
                            break;
                        default:
                            strWhichOne = "keep going...";
                            break;
                    }

//                    calTmp.add(Calendar.DATE, 1); // add a day
                    tmpFutureDate = tmpFutureDate.plusDays(1);


                    // and keep going, if we need to,
                    // to get to the next weekday.
//                    while (!RecurrencePanel.isWeekday(calTmp)) calTmp.add(Calendar.DATE, 1);
                    while (!RecurrencePanel.isWeekday(tmpFutureDate)) tmpFutureDate = tmpFutureDate.plusDays(1);

                    // System.out.println(strWhichOne + " " + calTmp.getTime());
//                    if (calTmp.get(Calendar.MONTH) != intMonth) {
                    if (tmpFutureDate.getMonthValue() - 1 != intMonth) {
                        // System.out.println("Shot past - resetting.");
//                        calTmp.setTime(dateGood);
                        tmpFutureDate = ldGood;
                        break;
                    } else {
//                        if (RecurrencePanel.isWeekday(calTmp)) dateGood = calTmp.getTime();
                        if (RecurrencePanel.isWeekday(tmpFutureDate)) ldGood = tmpFutureDate;
                    } // end if/else
                } // end while
//                dateTheEndDate = calTmp.getTime();
                futureDate = tmpFutureDate;
            } else if (strMonthPattern.toLowerCase().contains("last day")) {
                // System.out.println("last day");
                LocalDate tmpFutureDate = futureDate;
                while (true) {
//                    dateGood = calTmp.getTime();
                    ldGood = tmpFutureDate;
//                    calTmp.add(Calendar.DATE, 1); // add a day
                    tmpFutureDate = tmpFutureDate.plusDays(1);

//                    if (calTmp.get(Calendar.MONTH) != intMonth) {
                    if (tmpFutureDate.getMonthValue() - 1 != intMonth) {
                        // System.out.println("Shot past - resetting.");
//                        calTmp.setTime(dateGood);
                        tmpFutureDate = ldGood;
                        break;
                    } // end if
                } // end while
//                dateTheEndDate = calTmp.getTime();
                futureDate = tmpFutureDate;
            } else {
                // System.out.println("specific day");
                // Now set the calendar to the first one in this month -
//                calTmp.set(Calendar.DAY_OF_MONTH, 1);
                LocalDate tmpFutureDate = futureDate.withDayOfMonth(1);
//                while (calTmp.get(Calendar.DAY_OF_WEEK) != intDayOfWeek) {
//                    calTmp.add(Calendar.DATE, 1);
//                } // end while
                while (AppUtil.getDayOfWeekInt(tmpFutureDate) != intDayOfWeek) {
                    tmpFutureDate = tmpFutureDate.plusDays(1);
                }
                // System.out.println("Adjusted to correct day: " + calTmp.getTime());

                while (!strMonthPattern.contains(strWhichOne)) {
                    switch (strWhichOne) {
                        case "first":
                            strWhichOne = "second";
                            break;
                        case "second":
                            strWhichOne = "third";
                            break;
                        case "third":
                            strWhichOne = "fourth";
                            break;
                        default:
                            strWhichOne = "keep going...";
                            break;
                    }

//                    dateGood = calTmp.getTime();
                    ldGood = tmpFutureDate;
//                    calTmp.add(Calendar.DATE, 7); // add a week
                    tmpFutureDate = tmpFutureDate.plusWeeks(1);

                    // System.out.println(strWhichOne + " " + calTmp.getTime());
//                    if (calTmp.get(Calendar.MONTH) != intMonth) {
//                        // System.out.println("Shot past - resetting.");
//                        calTmp.setTime(dateGood);
//                        break;
//                    } // end if
                    if (tmpFutureDate.getMonthValue() - 1 != intMonth) {
                        // System.out.println("Shot past - resetting.");
                        tmpFutureDate = ldGood;
                        break;
                    } // end if
                } // end while
//                dateTheEndDate = calTmp.getTime();
                futureDate = tmpFutureDate;
            } // end if/else - general or specific or last
        }


//        return dateTheEndDate;
        return futureDate;
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
    @JsonIgnore
    public DayNoteData getDayNoteData() {
        DayNoteData dnd = new DayNoteData(this);
//        Instant instant = Instant.ofEpochMilli(this.getStartTime().getTime()); // This COULD be supposed to be END time.
        // TODO - use the 'movedToDay' flag here.
//        LocalTime ansr = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime();
        LocalTime ansr = this.getStartTime();
        dnd.setTimeOfDayString(ansr.toString());
        dnd.setSubjectString("Event");

        // Insert the Event category (if there is one) into
        //   the extended note.
        String s = this.extendedNoteString;
        if (this.subjectString != null) {
            s = this.subjectString + "\n" + s;
        } // end if

        // And append the location, if there is one.
        if (!this.locationString.trim().equals("")) {
            s += "\nLocation: " + this.locationString;
        } // end if
        dnd.setExtendedNoteString(s);

        movedToDay = true;
        return dnd;
    } // end getDayNoteData


    //---------------------------------------------------------
    // Method Name: hasStarted
    //
    // Compares the known Event start to the current time.
    //---------------------------------------------------------
    boolean hasStarted() {
        if (getStartDate() == null) return false;
        LocalDateTime rightNow = LocalDateTime.now();

        if (getStartTime() != null) return rightNow.isAfter(getStartDate().atTime(getStartTime()));
        else return rightNow.isAfter(getStartDate().atStartOfDay());
    } // end hasStarted


    //---------------------------------------------------
    // Method Name: isEndSameDay
    //
    // Answers the question - does the event
    //   end on the same day that it started?
    //---------------------------------------------------
    @JsonIgnore
    private boolean isEndSameDay() {
        LocalDate theStartDate = getStartDate();
        LocalDate theEndDate = getEndDate();
        if (theStartDate == null) return false; // Can't determine == no
        if (theEndDate == null) return false;

        int intTheYear = theStartDate.getYear();
        int intTheDay = theStartDate.getDayOfYear();
        if (theEndDate.getYear() != intTheYear) return false;
        return theEndDate.getDayOfYear() == intTheDay;
    } // end isEndSameDay


    //---------------------------------------------------------
    // Method Name: isTimesKnown
    //
    // Returns true if both the Start Time and the End Time
    //   are known; otherwise returns false.
    //---------------------------------------------------------
    @JsonIgnore
    private boolean isTimesKnown() {
        LocalTime startTime = getStartTime();
        if(startTime == null) return false;
        return getEndTime() != null;
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
    private void recalcDuration() {
        LocalDate theStartDate = getStartDate();
        LocalDate theEndDate = getEndDate();
        LocalTime theStartTime = getStartTime();
        LocalTime theEndTime = getEndTime();

        boolean blnDatePair = isTimesKnown();
        boolean blnTimePair = ((theStartTime != null) && (theEndTime != null));

        // Defaults
        lngDurationValue = null;
        strDurationUnits = "unknown";

        // First, cover the 11 cases where we know we cannot calculate duration.
        //----------------------------------------------------------------------
        if (theStartDate == null) return;
        if (theEndDate == null) return;

        // Two more cases here -
        // Start (Date known, Time not), End (Date not, Time known)
        // Start (Date not, Time known), End (Date known, Time not)
        if ((!blnDatePair) && (!blnTimePair)) return;

        // Already covered -
        // Two cases where we know both times but only one or the other date
        //----------------------------------------------------------------------

        // Now, presumably, we have enough to work with.
        strDurationUnits = "minute";

        // Perform the (same) calculation for each of the remaining 6 cases.
        // If a 'placeholder' value was set in order to hold a duration
        //   value then this calculation will include it.
        //lngDurationValue = dateEventEnd.getTime() - dateEventStart.getTime();

        // Convert to Seconds (from milliseconds)
//        lngDurationValue /= 1000;  // Up to .999 second loss of precision.

        // Convert to Minutes (from seconds)
//        lngDurationValue /= 60;  // Up to 59 seconds loss of precision.
        assert theStartTime != null;
        lngDurationValue = ChronoUnit.MINUTES.between(theStartTime, theEndTime);

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
                        return;
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
        if (!isTimesKnown()) {
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
            if (theEndTime == null) { // (start time already proven to be known, at this point)
                    strDurationUnits = "days";
            }
        } // end if duration is zero

        if (lngDurationValue > 1) strDurationUnits += "s";

    } // end recalcDuration


    public void setDuration(long theDuration) {
        // just preventing compilation errors, for now.
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
//    public void setDuration(long lngDuration) {
//        long tmpLong;
//        blnSettingDuration = true;
//
//        // Convert the input to milliseconds.
//        lngDuration *= (60 * 1000);
//
//        // The actions taken will depend on which values
//        //   are already known.  There are 16 unique cases.
//        //   The line numbers in the comments refer to the
//        //   related table in the documentation.
//        // We DO NOT simply set our Event dates to the calculation
//        //   results and then call the 'set' methods with those
//        //   values, because the method calls will change them.
//        // So, we call the 'set' methods with a new Date each time
//        //   and only directly manipulate the Event dates when
//        //   assigning a placeholder value, AFTERwards, because
//        //   those methods are written to drop out such values.
//        //--------------------------------------------------------
//        if (intDatKnown == 0) {
//            // Line 1 - p(SD ST ED ET)
//            // Nothing already known; the two internal dates
//            //   must contain 'placeholder' values, and they
//            //   should be set without affecting the 'known'
//            //   status tracking value (intDatKnown).
//            dateEventStart = new Date();
//            tmpLong = dateEventStart.getTime();
//            dateEventEnd = new Date(tmpLong + lngDuration);
//        } else if (intDatKnown == END_TIME_KNOWN) {
//            // Line 2 - p(SD) & ST
//            tmpLong = dateEventEnd.getTime();
//            setStartTime(new Date(tmpLong - lngDuration));    // ST
//            dateEventStart = new Date(tmpLong - lngDuration); // p(SD)
//        } else if (intDatKnown == END_DATE_KNOWN) {
//            // Line 3 - SD & p(ST)
//            tmpLong = dateEventEnd.getTime();
//            setStartDate(new Date(tmpLong - lngDuration));    // SD
//            dateEventStart = new Date(tmpLong - lngDuration); // p(ST)
//        } else if (intDatKnown == (END_DATE_KNOWN + END_TIME_KNOWN)) {
//            // Line 4 - SD & ST
//            tmpLong = dateEventEnd.getTime();
//            setStartDate(new Date(tmpLong - lngDuration));  // SD
//            setStartTime(new Date(tmpLong - lngDuration));  // ST
//        } else if (intDatKnown == START_TIME_KNOWN) {
//            // Line 5 - p(ED) & ET
//            tmpLong = dateEventStart.getTime();
//            setEndTime(new Date(tmpLong + lngDuration));     // ET
//            dateEventEnd = new Date(tmpLong + lngDuration);  // p(ED)
//        } else if (intDatKnown == (START_TIME_KNOWN + END_TIME_KNOWN)) {
//            // Line 6 - rET & p(ED)
//            tmpLong = dateEventStart.getTime();
//            setEndTime(new Date(tmpLong + lngDuration));     // rET
//            dateEventEnd = new Date(tmpLong + lngDuration);  // p(ED)
//        } else if (intDatKnown == (START_TIME_KNOWN + END_DATE_KNOWN)) {
//            // Line 7 - SD & ET
//            // Less straightforward on how to proceed; here is the logic:
//            // Add the duration to the start time so that we can have
//            // a known end time.  Then use the composite end to get the
//            // correct start date.
//            tmpLong = dateEventStart.getTime();
//            setEndTime(new Date(tmpLong + lngDuration));    // ET
//            tmpLong = dateEventEnd.getTime();
//            setStartDate(new Date(tmpLong - lngDuration));  // SD
//        } else if (intDatKnown == (START_TIME_KNOWN + END_KNOWN)) {
//            // Line 8 - SD & rST
//            tmpLong = dateEventEnd.getTime();
//            setStartDate(new Date(tmpLong - lngDuration));  // SD
//            setStartTime(new Date(tmpLong - lngDuration));  // rST
//        } else if (intDatKnown == START_DATE_KNOWN) {
//            // Line 9 - ED & p(ET)
//            tmpLong = dateEventStart.getTime();
//            setEndDate(new Date(tmpLong + lngDuration));    // ED
//            dateEventEnd = new Date(tmpLong + lngDuration); // p(ET)
//        } else if (intDatKnown == (START_DATE_KNOWN + END_TIME_KNOWN)) {
//            // Line 10 - ST & ED
//            // Less straightforward on how to proceed; here is the logic:
//            // Subtract the duration from the end time so that we can have
//            // a known start time.  Then use the composite start to get the
//            // correct end date.
//            tmpLong = dateEventEnd.getTime();
//            setStartTime(new Date(tmpLong - lngDuration));  // ST
//            tmpLong = dateEventStart.getTime();
//            setEndDate(new Date(tmpLong + lngDuration));    // ED
//        } else if (intDatKnown == (START_DATE_KNOWN + END_DATE_KNOWN)) {
//            // Line 11 - rED & p(ET)
//            tmpLong = dateEventStart.getTime();
//            setEndDate(new Date(tmpLong + lngDuration));     // rED
//            dateEventEnd = new Date(tmpLong + lngDuration);  // p(ET)
//        } else if (intDatKnown == (START_DATE_KNOWN + END_KNOWN)) {
//            // Line 12 - rSD & ST
//            tmpLong = dateEventEnd.getTime();
//            setStartDate(new Date(tmpLong - lngDuration));  // rSD
//            setStartTime(new Date(tmpLong - lngDuration));  // ST
//        } else if (intDatKnown == (START_KNOWN)) {
//            // Line 13 - ED & ET
//            tmpLong = dateEventStart.getTime();
//            setEndDate(new Date(tmpLong + lngDuration)); // ED
//            setEndTime(new Date(tmpLong + lngDuration)); // ET
//        } else if (intDatKnown == (START_KNOWN + END_TIME_KNOWN)) {
//            // Line 14 - ED & rET
//            tmpLong = dateEventStart.getTime();
//            setEndDate(new Date(tmpLong + lngDuration)); // ED
//            setEndTime(new Date(tmpLong + lngDuration)); // rET
//        } else if (intDatKnown == (START_KNOWN + END_DATE_KNOWN)) {
//            // Line 15 - rED & ET
//            tmpLong = dateEventStart.getTime();
//            setEndDate(new Date(tmpLong + lngDuration)); // rED
//            setEndTime(new Date(tmpLong + lngDuration)); // ET
//        } else if (intDatKnown == (START_KNOWN + END_KNOWN)) {
//            // Line 16 - rED & rET
//            tmpLong = dateEventStart.getTime();
//            setEndDate(new Date(tmpLong + lngDuration)); // rED
//            setEndTime(new Date(tmpLong + lngDuration)); // rET
//        } // end if/else - all 16 cases
//
//        recalcDuration();
//        blnSettingDuration = false;
//    } // end setDuration


    // NOTES:
    //
    // When setting a date or time it is true that the fields may
    //   have previously contained a duration.  However, (per
    //   design requirements) ANY such setting must invalidate a
    //   duration because it should then be recalculated.
    //--------------------------------------------------------------


    //------------------------------------------------------------
    // Method Name: setEndDate
    //
    // Tests the proposed new composite End against the already
    //   established composite Start.  If it would be earlier
    //   then no change is made and the return value is false.
    //   Otherwise the setting is accepted and the return value
    //   is true.
    //------------------------------------------------------------
    public boolean setEndDate(LocalDate newEndDate) {
        if (newEndDate == null) { // The user is 'un-setting' the date.
            eventEndDateString = null;
            recalcDuration();
            return true;
        } // end if UNsetting a date

        // Get a temporary composite Event End, for possible comparison to the start.
        LocalDateTime newEventEnd;
        LocalTime endTime = getEndTime();
        if(endTime != null) {
            newEventEnd = newEndDate.atTime(endTime);
        } else {
            newEventEnd = newEndDate.atTime(23,59,59); // Assume end-of-day.
        }

        // Continue setting up for the comparison -
        LocalDate theStartDate = getStartDate();
        if(theStartDate != null) { // Comparison not possible without a start date.
            LocalDateTime eventStart;  // Make a temporary composite Event Start.
            LocalTime startTime = getStartTime();
            if(startTime != null) {
                eventStart = theStartDate.atTime(startTime);
            } else { // Assume it runs to the end of the day.
                eventStart = theStartDate.atStartOfDay();
            }
            // The comparison -
            // Ensure that the proposed new Event End is not before the current Event Start.
            if (newEventEnd.isBefore(eventStart)) return false;
        }

        // Accept the proposed new End date and recalculate duration.
        eventEndDateString = newEndDate.toString();
        recalcDuration();
        return true;
    } // end setEndDate


    //------------------------------------------------------------
    // Method Name: setEndTime
    //
    // Tests the proposed new End time for validity and if okay
    // then the setting is accepted and the return value is true.
    //------------------------------------------------------------
    boolean setEndTime(LocalTime newEndTime) {
        if (newEndTime == null) { // The user is 'un-setting' the time.
            // We can just accept the setting; removal of an end time
            // cannot result in the event starting after the event end.
            eventEndTimeString = null;
            recalcDuration();
            return true;
        } // end if UNsetting the end time

        // Get a temporary composite Event End, for possible comparison to the start.
        LocalDateTime newEventEnd;
        LocalDate theEndDate = getEndDate();
        if (theEndDate != null) { // otherwise no comparison possible.
            newEventEnd = theEndDate.atTime(newEndTime);
            LocalDateTime eventStart;
            LocalDate theStartDate = getStartDate();
            if(theStartDate != null) { // otherwise no comparison possible.
                LocalTime theStartTime = getStartTime();
                if(theStartTime != null) { // otherwise it defaults to start-of-day and the composite start could
                    // not possibly be after the end, even if start and end occur on the same day.
                    // So this is the only logical branch where a change of the end time might be rejected.
                    eventStart = theStartDate.atTime(theStartTime);
                    // The comparison - Event Start must be before Event End.
                    if(newEventEnd.isBefore(eventStart)) return false;
                } // end if (no 'else' needed)
            } // end if - if we have a start date
        } // end if

        // Accept the proposed new End Time and recalculate duration.
        eventEndTimeString = newEndTime.toString();
        recalcDuration();
        return true;
    } // end setEndTime

    public void setLocation(String s) {
        locationString = s;
    }

    public void setRecurrence(String s) {
        recurrenceString = s;
    }

    void setRetainNote(boolean b) {
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
//    public boolean setStartDate(Date d) {
//        if (d == null) { // The user is 'un-setting' the date.
//            if (isStartDateKnown()) intDatKnown -= START_DATE_KNOWN;
//            // If we also now have no time component as well, then
//            //   we can set the entire EventStart to null.  If it had
//            //   been in use as a Duration placeholder then this is
//            //   still OK since without either Start component, the
//            //   duration calculation cannot be made.
//            if (!isStartTimeKnown()) {
//                dateEventStart = null;
//            } else { // Need to prevent phantom durations
//                // This will re-initialize the 'unknown' Date component
//                //   so that no unwanted duration remains.
//                setStartTime(dateEventStart);
//            } // end if
//            strRecurrence = "";
//            recalcDuration();
//            return true;
//        } // end if UNsetting a date
//
//        // Put the input Date into the temporary calendar.
//        calTmp.setTime(d);
//
//        // Add the time component, if any.
//        //----------------------------------------------
//        if (!isStartTimeKnown()) {
//            // There is no Time component - zero it out.
//            calTmp.set(Calendar.HOUR_OF_DAY, 0);
//            calTmp.set(Calendar.MINUTE, 0);
//            calTmp.set(Calendar.SECOND, 0);
//            calTmp.set(Calendar.MILLISECOND, 0);
//        } else {
//            // There IS a time component.  Extract the input Date components
//            // and then use the previous dateEventStart (with its valid time
//            // component) as the temporary calendar's base.
//            int intYear = calTmp.get(Calendar.YEAR);
//            int intMonth = calTmp.get(Calendar.MONTH);
//            int intDay = calTmp.get(Calendar.DATE);
//            calTmp.setTime(dateEventStart);
//
//            // Set the temporary calendar to the input Date
//            //   (leaving time unmodified)
//            calTmp.set(intYear, intMonth, intDay);
//        }
//
//        // Get the temporary composite Start, for comparison.
//        Date dateTmpEventStart = calTmp.getTime();
//
//        // Ensure that the proposed composite Start is not after the EventEnd.
//        if (isEndDateKnown() && dateTmpEventStart.after(dateEventEnd)) return false;
//
//        // Get the final Date from the temporary Calendar
//        dateEventStart = dateTmpEventStart;
//
//        // Set the 'known' indicator
//        intDatKnown |= START_DATE_KNOWN;
//
//        strRecurrence = "";
//        recalcDuration();
//        return true;
//    } // end setStartDate

    public boolean setStartDate(LocalDate newStartDate) {
        if (newStartDate == null) { // The user is 'un-setting' the date.
            recurrenceString = ""; // Recurrence not possible without a start date.
            eventStartDateString = null;
            recalcDuration();
            return true;
        } // end if UNsetting a date

        // Get a temporary composite Event Start, for possible comparison to the end.
        LocalDateTime newEventStart;
        LocalTime startTime = getStartTime();
        if(startTime != null) {
            newEventStart = newStartDate.atTime(startTime);
        } else {
            newEventStart = newStartDate.atStartOfDay();
        }

        // Continue setting up for the comparison -
        LocalDate theEndDate = getEndDate();
        if(theEndDate != null) { // Comparison not possible without an end date.
            LocalDateTime eventEnd;  // Make a temporary composite Event End.
            LocalTime endTime = getEndTime();
            if(endTime != null) {
                eventEnd = theEndDate.atTime(endTime);
            } else { // Assume it runs to the end of the day.
                eventEnd = theEndDate.atTime(23,59,59);
            }
            // The comparison -
            // Ensure that the proposed new Event Start is not after the current Event End.
            if (newEventStart.isAfter(eventEnd)) return false;
        }

        // Accept the proposed new Start date, reset recurrence, and recalculate duration.
        eventStartDateString = newStartDate.toString();
        recurrenceString = "";
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
    boolean setStartTime(LocalTime newStartTime) {
        if (newStartTime == null) { // The user is 'un-setting' the time.
            // We can just accept the setting; removal of a start time
            // cannot result in the event ending after the event start.
            eventStartTimeString = null;
            recalcDuration();
            return true;
        } // end if UNsetting the start time

        // Get a temporary composite Event Start, for possible comparison to the end.
        LocalDateTime newEventStart;
        LocalDate theStartDate = getStartDate();
        if (theStartDate != null) { // otherwise no comparison possible.
            newEventStart = theStartDate.atTime(newStartTime);
            LocalDateTime eventEnd;
            LocalDate theEndDate = getEndDate();
            if(theEndDate != null) { // otherwise no comparison possible.
                LocalTime theEndTime = getEndTime();
                if(theEndTime != null) { // otherwise it defaults to end-of-day and the composite end could
                    // not possibly be ahead of the start, even if start and end occur on the same day.
                    // So this is the only logical branch where a change of the start time might be rejected.
                    eventEnd = theEndDate.atTime(theEndTime);
                    // The comparison - Event End must be after Event Start.
                    if(newEventStart.isAfter(eventEnd)) return false;
                } // end if (no 'else' needed)
            } // end if - if we have a start date
        } // end if

        // Accept the proposed new Start Time and recalculate duration.
        eventStartTimeString = newStartTime.toString();
        recalcDuration(); // Any new setting of the four main components invalidates duration.
        return true;
    } // end setStartTime

} // end class EventNoteData
