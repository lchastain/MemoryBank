import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.text.WordUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.DAYS;

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

    // Duration should be recalculated every time one of the Date/Time fields is set.
    // But if Duration was entered by the user and as a result one or more of the Date/Time
    // fields was calculated and set, then we don't want to also do a duration
    // recalculation.  This var is used to stop that from happening.
    static transient boolean settingDuration = false;

    // These two are set by the recalcDuration method via the Date/Time setters, and
    // they share their 'get' methods with durationValue and durationUnits.
    private transient Long calculatedDurationValue;
    private transient String calculatedDurationUnits;

    // These are only set if the user explicitly entered them; calculations are not persisted.
    // Both are required; otherwise neither are kept.
    private Integer durationValue;  // User entries do not need to be Long.
    private String durationUnits;

    private static DateTimeFormatter dtf;

    public EventNoteData() {
        super();
        clearEventNoteData();
    } // end constructor


    // The copy constructor (clone)
    public EventNoteData(EventNoteData endCopy) {
        super(endCopy);
        blnRetainNote = endCopy.blnRetainNote;
        durationUnits = endCopy.durationUnits;
        durationValue = endCopy.durationValue;
        eventStartDateString = endCopy.eventStartDateString;
        eventEndDateString = endCopy.eventEndDateString;
        eventStartTimeString = endCopy.eventStartTimeString;
        eventEndTimeString = endCopy.eventEndTimeString;
        extendedNoteString = endCopy.extendedNoteString;
        iconFileString = endCopy.iconFileString;
        calculatedDurationValue = endCopy.calculatedDurationValue;
        noteString = endCopy.noteString;
        showIconOnMonthBoolean = endCopy.showIconOnMonthBoolean;
        calculatedDurationUnits = endCopy.calculatedDurationUnits;
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

    // This method is used to set user-specified duration units, only.
    // After a user-specified Date/Time setting causes new Duration Units to be calculated, that
    // value goes into a different, transient variable; this method is not used in that case.
    void setDurationUnits(String newValue) {
        calculatedDurationValue = null; // Any user-directed setting will invalidate a calculated one.
        calculatedDurationUnits = null; // Any user-directed setting will invalidate a calculated one.
        touchLastMod();

        // An un-setting variant; 'unknown' also works and is
        // handled the same way but with more commentary, below.
        if (newValue == null) {
            durationUnits = null;
            return;
        }

        // Groom the input -
        String theValue = WordUtils.capitalizeFully(newValue.trim());

        // Constrain the input to a 'legal' value.
        String[] validValues = {"Unknown", "Minutes", "Hours", "Days", "Weeks"};
        List<String> list = Arrays.asList(validValues);
        if (!list.contains(theValue)) return;

        // We don't want to cause recalculations when there wasn't really a change.
        if (durationUnits != null && durationUnits.equals(theValue)) return;

        // Accept the input value.
        durationUnits = theValue;

        // But if units went to Unknown we don't want to do any recalculations
        // and we don't want to remove any user-supplied durationValue (yet);
        // just leave it alone until the user takes some further action such
        // as making a different unit selection, or leaving the interface.
        if (theValue.equals("Unknown")) {
            durationUnits = null;  // We don't actually keep the 'Unknown' value; it just means units should be null.
            return;
        }

        // Duration may have been set while units were still unknown and in
        // that case no other calculations would have been attempted.  Now
        // we are either setting units for the first time, or changing to
        // a different unit.  In either case, a duration that was previously
        // set (if any) will need to be re-set, to do those calcs again with
        // different results.
        if (durationValue != null) setDurationValue(durationValue);
    }

    String getDurationUnits() {
        if (durationUnits != null) { // User-entered
            return durationUnits;
        } else {  // Calculated
            if (calculatedDurationUnits != null) {
                return calculatedDurationUnits;
            }
        }
        return null;
    } // end getDurationUnits

    Integer getDurationValue() {
        if (durationValue != null) { // User-entered
            return durationValue;
        } else {  // Calculated
            if (calculatedDurationValue != null) {
                return Math.toIntExact(calculatedDurationValue);
            }
        }
        return null;
    } // end getDurationValue


    LocalDate getEndDate() {
        if (eventEndDateString == null) return null;
        return LocalDate.parse(eventEndDateString);
    }

    LocalTime getEndTime() {
        if (eventEndTimeString == null) return null;
        return LocalTime.parse(eventEndTimeString);
    }

    // Get the Event end, as the combination of the
    // end date and end time (with caveats).
    private LocalDateTime getEventEndDateTime() {
        LocalDate theEndDate = getEndDate();
        if (theEndDate == null) return null;
        LocalTime theEndTime = getEndTime();
        if (theEndTime != null) {
            return theEndDate.atTime(theEndTime);
        } else {
            return theEndDate.atTime(LocalTime.MIDNIGHT.minusMinutes(1));
        }
    }

    // Get the Event start, as the combination of the
    // start date and start time (with caveats).
    LocalDateTime getEventStartDateTime() {
        LocalDate theStartDate = getStartDate();
        if (theStartDate == null) return null;
        LocalTime theStartTime = getStartTime();
        if (theStartTime != null) {
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

    static String getRecurrenceSummary(String strSetting) {
        if(null == strSetting || strSetting.equals("")) return "None";

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
            strTheSummary += String.valueOf(calculatedDurationValue);
            strTheSummary += " " + calculatedDurationUnits;
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
                strTheSummary += "It ends on " + dtf.format(theEndDate);
                strTheSummary += ".&nbsp; &nbsp;";
            }
        }

        if (recurrenceString != null && !recurrenceString.trim().equals("")) {
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
    //   case it will return a false.
    //
    // In considering the 'Stop After' count, we do not need
    //   to refuse to goForward if the count is down to one
    //   because as soon as it goes below two there is no
    //   more recurrence at all.
    //------------------------------------------------------
    boolean goForward() {
        if (recurrenceString == null) return false;
        if (recurrenceString.trim().equals("")) return false;

        // Unlikely that this method was even called in this
        //   case, but test it to be sure since we're using
        //   it later and expecting it to be non-null.
        LocalDate theStartDate = getStartDate();
        if (theStartDate == null) return false;

        int intTheInterval;
        String strDescription;

        int intUnderscore1 = recurrenceString.indexOf('_');
        int intUnderscore2 = recurrenceString.lastIndexOf('_');

        // Calculate the proposed new start date
//        calTmp.setTime(dateEventStart);
        LocalDate futureDate = theStartDate;
        if (recurrenceString.startsWith("D")) {
            intTheInterval = Integer.parseInt(recurrenceString.substring(1, intUnderscore1));
//            calTmp.setNotes(Calendar.DATE, intTheInterval);
            futureDate = futureDate.plusDays(intTheInterval);
        } else if (recurrenceString.startsWith("W")) {
            strDescription = recurrenceString.substring(intUnderscore1 + 1, intUnderscore2);
            intTheInterval = Integer.parseInt(recurrenceString.substring(1, intUnderscore1));

            int intTmp;
            while (true) {
                // If we are at the end of the week, jump the
                //   interval before we setNotes another day.
                // ok, doing that, but why?  need better comment here.
                intTmp = AppUtil.getDayOfWeekInt(futureDate);
                if (intTmp == SATURDAY) {
                    if (intTheInterval > 1) futureDate = futureDate.plusDays(7 * (intTheInterval - 1));
                } // end if

//                calTmp.setNotes(Calendar.DATE, 1); // Add one day.
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
        if (!recurrenceString.endsWith("_")) { // recurrence has an end; does not go on indefinitely.
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
            } else {              // Stop By
                dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
                try {
                    LocalDate d = LocalDate.parse(strRecurEnd, dtf);
                    if (d.isBefore(futureDate)) return false;
                } catch (Exception ignored) {
                }
            } // end if/else - Stop    After or By
        } // end if there is an end recurrence range

        // Various sections above have calculated the appropriate new
        // date to which to set the Start.  Now - use it.

        // Preserve the recurrence from being wiped by setStartDate().
        String strKeepRecurrence = recurrenceString;

        // Adjust to the new (later) Start Date.
        MemoryBank.debug("  Adjusting start date to: " + futureDate);
        // If there is an End Date, it will need to move forward too, by the same amount.
        LocalDate theEndDate = getEndDate();
        if(theEndDate != null) {
            long theAdjustment = DAYS.between(theStartDate, futureDate);
            setEndDate(theEndDate.plusDays(theAdjustment));
        }
        setStartDate(futureDate);
//        May need to also re-set duration here, if we have user-entered (vs calculated) settings.

        // Restore the recurrence string.
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
//        calTmp.setNotes(Calendar.MONTH, months);
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
//                while (RecurrencePanel.isWeekday(calTmp)) calTmp.setNotes(Calendar.DATE, 1);
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

//                    calTmp.setNotes(Calendar.DATE, 1); // setNotes a day
                    tmpFutureDate = tmpFutureDate.plusDays(1);

                    // and keep going, if we need to,
                    // to get to the next weekend day.
//                    while (RecurrencePanel.isWeekday(calTmp)) calTmp.setNotes(Calendar.DATE, 1);
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

//                while (!RecurrencePanel.isWeekday(calTmp)) calTmp.setNotes(Calendar.DATE, 1);
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

//                    calTmp.setNotes(Calendar.DATE, 1); // setNotes a day
                    tmpFutureDate = tmpFutureDate.plusDays(1);


                    // and keep going, if we need to,
                    // to get to the next weekday.
//                    while (!RecurrencePanel.isWeekday(calTmp)) calTmp.setNotes(Calendar.DATE, 1);
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
//                    calTmp.setNotes(Calendar.DATE, 1); // setNotes a day
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
//                    calTmp.setNotes(Calendar.DATE, 1);
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
//                    calTmp.setNotes(Calendar.DATE, 7); // setNotes a week
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

        return futureDate;
    } // end goForwardMonths


    // This is used by a Set during uniqueness checking.  This method effectively disables
    // the 'hashcode' part of the check, so that the only remaining uniqueness criteria
    // is the result of the .equals() method.
    @Override
    public int hashCode() {
        return 1;
    }

    //---------------------------------------------------------
    // Method Name: hasStarted
    //
    // Returns true if the Event start is known and earlier than the current time.
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


    //-----------------------------------------------------------
    // Method Name: recalcDuration
    //
    // There are 16 distinct cases to cover, as detailed in the
    // SupportingData.xlsx spreadsheet table.  A duration
    //   determination is only possible for 5 of them.
    //
    // If the calculation cannot be made, return with no action taken.
    // If the calculation can be made, the duration units start in
    // minutes and is only increased if there is no loss of precision.
    //-----------------------------------------------------------
    void recalcDuration() {
        if (settingDuration) {
            return;
        } else {
            durationUnits = null;
            durationValue = null;
        }
        LocalDate theStartDate = getStartDate();
        LocalDate theEndDate = getEndDate();
        LocalTime theStartTime = getStartTime();
        LocalTime theEndTime = getEndTime();

        boolean blnDatePair = ((theStartDate != null) && (theEndDate != null));
        boolean blnTimePair = ((theStartTime != null) && (theEndTime != null));

        // Defaults
        calculatedDurationValue = null;
        calculatedDurationUnits = null;

        // First, cover the 11 cases where we know we cannot calculate duration.
        //----------------------------------------------------------------------
        if (theStartDate == null && theStartTime == null) return; // Lines 0-3 (4 cases) of the table.
        if (theEndDate == null && theEndTime == null) return; // Lines 0,4,8,c  one overlap here, so 7 cases so far..

        // Two more cases here -
        // Start Date not, Start Time known, End Date known, End Time not
        // Start Date known, Start Time not, End Date not, End Time known
        if ((!blnDatePair) && (!blnTimePair)) return; // Lines 6,9

        // Two cases where we know both times but only one or the other date
        if (blnTimePair) {
            if (theStartDate == null && theEndDate != null) return;  // Line 7
            if (theStartDate != null && theEndDate == null) return;  // Line d
        }
        //----------------------------------------------------------------------

        // Now, presumably, we have enough to work with.

        // The next condition could be more readable to match our cases, but by
        // now we have already thrown out all cases where we have a time pair
        // and the End Date is known, so if blnTimePair is true then
        // theEndDate will always be null.  So - the condition is simplified.
        //   if(blnTimePair && (null == theStartDate) && (null == theEndDate))
        if (blnTimePair && null == theStartDate) { // TIME_PAIR_BUT_NO_DATES - Line 5
            calculatedDurationValue = ChronoUnit.MINUTES.between(theStartTime, theEndTime);
            if (calculatedDurationValue < 0) calculatedDurationValue += 1440; // Normalize to a positive duration.
            calculatedDurationUnits = "Minutes";
        }

        if (blnDatePair && !blnTimePair) { // Lines a,b,e
            calculatedDurationValue = DAYS.between(theStartDate, theEndDate);
            calculatedDurationUnits = "Days";
        }

        if (blnDatePair && blnTimePair) { // Line f
            calculatedDurationValue = ChronoUnit.MINUTES.between(getEventStartDateTime(), getEventEndDateTime());
            calculatedDurationUnits = "Minutes";
        }


        // Now compress the units, if reasonable to do so.
        //--------------------------------------------------------
        // We know that the Event editor duration value field can show a maximum
        //   of five digits (ie, 99999), so first we should check to
        //   see if we have an 'overflow' condition, and prevent it.
        if (calculatedDurationValue > 99999 && calculatedDurationUnits.equals("Minutes")) {
            // Convert Minutes to Hours
            calculatedDurationValue /= 60;
            calculatedDurationUnits = "Hours";

            if (calculatedDurationValue > 99999) {
                // Convert Hours to Days
                calculatedDurationValue /= 24;
                calculatedDurationUnits = "Days";

                if (calculatedDurationValue > 99999) {
                    // Convert Days to Weeks
                    calculatedDurationValue /= 7;
                    calculatedDurationUnits = "Weeks";

                    if (calculatedDurationValue > 99999) { // Just too big to show.
                        calculatedDurationValue = null;
                        calculatedDurationUnits = null;
                    }
                } // end if Days is too small a unit
            } // end if Hours is too small a unit
            return; // (we only go in the other direction, below).
        } // end if Minutes is too small a unit

        if (calculatedDurationValue > 99999) {  // Units starts in Days, at this point
            // Convert Days to Weeks
            calculatedDurationValue /= 7;
            calculatedDurationUnits = "Weeks";

            if (calculatedDurationValue > 99999) { // Just too big to show.
                calculatedDurationValue = null;
                calculatedDurationUnits = null;
            }
            return; // (we only go in the other direction, below).
        } // end if Days is too small a unit
        //--------------------------------------------------------

        // Now there are a few other cases where units should be compressed, as long as there is no loss of precision.
        if(calculatedDurationUnits.equals("Minutes")) {
            if(calculatedDurationValue % 60 == 0) {
                calculatedDurationValue /= 60;
                calculatedDurationUnits = "Hours";
            }
            if(calculatedDurationValue % 24 == 0 && calculatedDurationUnits.equals("Hours")) {
                calculatedDurationValue /= 24;
                calculatedDurationUnits = "Days";
            }
        }

        if(calculatedDurationUnits.equals("Days")) {
            if(calculatedDurationValue % 7 == 0) {
                calculatedDurationValue /= 7;
                calculatedDurationUnits = "Weeks";
            }
        }

        // A duration of zero should be in units of either minutes
        //   or Days - Minutes if the user has specified both time
        //   components; otherwise days.
        if (calculatedDurationValue == 0) {
            if(blnTimePair) calculatedDurationUnits = "Minutes";
            else calculatedDurationUnits = "Days";
        } // end if duration is zero
    } // end recalcDuration

    // This method is used to set a user-specified Duration, only.
    // After a user-specified Date/Time setting causes a new Duration to be calculated, that
    // value goes into a different, transient variable; this method is not used in that case.
    void setDurationValue(Integer newDuration) {
        calculatedDurationValue = null;
        calculatedDurationUnits = null;
        durationValue = newDuration; // Accept the input (even if null)
        touchLastMod();
        if (durationValue == null) return;  // The user cleared the duration value or units?
        // You might think that with a durationValue of zero, there would be nothing to do here,
        // but the calcs below can still work with that, to set unknowns to their known equivalents.

        String theUnits = getDurationUnits();
        if ((theUnits == null) || (theUnits.equals("Unknown"))) return; // We need units, to continue.
        // A duration without units is meaningless.
        // If units are set first, no problem and we go on from here.  If units are set later,
        // then setDurationUnits() will call this one again, if a duration value has been set.
        // Duration value can be set without having units yet; we just don't go any further with
        // it until units are known.

        // Give meaningful names to unique values that can be used by a bitmapped indicator variable.
        // Enumerated values would also work but bitmapping is more stylish.
        final int ALL_UNKNOWN = 0;
        final int START_DATE_KNOWN = 1;
        final int START_TIME_KNOWN = 2;
        final int END_DATE_KNOWN = 4;
        final int END_TIME_KNOWN = 8;
        final int START_KNOWN = START_DATE_KNOWN + START_TIME_KNOWN;
        final int END_KNOWN = END_DATE_KNOWN + END_TIME_KNOWN;
        final int ALL_KNOWN = START_KNOWN + END_KNOWN;

        // Now set a bitmapped indicator variable using the named values.
        int knownIndicator = ALL_UNKNOWN;
        if (getStartDate() != null) knownIndicator += START_DATE_KNOWN;
        if (getStartTime() != null) knownIndicator += START_TIME_KNOWN;
        if (getEndDate() != null) knownIndicator += END_DATE_KNOWN;
        if (getEndTime() != null) knownIndicator += END_TIME_KNOWN;

        // Variables needed in more than one case, below -
        int theDays;
        int theMinutes;
        LocalDateTime theStart;  // Combined Start Date & Start Time
        LocalDateTime theEnd;    // Combined End Date & End Time

        // Preserve the original value of this boolean, while setting it to True for this method.
        boolean originalSettingDuration = settingDuration;
        settingDuration = true;

        // Cover the 16 different conditions as listed in the supporting data spreadsheet truth table.
        // Thanks to the bitmapped 'known' indicator, each case below is mutually exclusive of the others.
        switch (knownIndicator) {
            case ALL_UNKNOWN:  // Just accept the new setting; nothing else in place to interact with.
                break;
            case END_TIME_KNOWN: // We can set the unknown Start Time
                switch (theUnits) {
                    case "Minutes":
                        setStartTime(getEndTime().minusMinutes(durationValue));
                        break;
                    case "Hours":
                        setStartTime(getEndTime().minusHours(durationValue));
                        break;
                    default:  // Working backwards in units of Days or Weeks, the time of day would be the same.
                        setStartTime(getEndTime());
                        break;
                }
                break;
            case END_DATE_KNOWN: // We can set the unknown Start Date (maybe)
                switch (theUnits) {
                    case "Days":
                        setStartDate(getEndDate().minusDays(durationValue));
                        break;
                    case "Weeks":
                        setStartDate(getEndDate().minusWeeks(durationValue));
                        break;
                    // Notes about Hours and Minutes
                    // Yes, the units are smaller than Days but the value may still
                    // be large enough that a number of days are involved.  So we can
                    // convert to Days and can use the whole number in the new setting,
                    // but what about the remainder?  What if the 'real' end time was
                    // only at 2am on the End Date?  a duration of 27 hours would put
                    // the start date back by two days, not just one.  On the other hand
                    // the 'real' end time might be end of day on the End Date, so that
                    // the calculated start date should be only one earlier.  We have
                    // already committed to honoring the user's explicit settings and
                    // making no assumptions on their behalf, but
                    // we need to also honor their explicit non-settings and since the
                    // exact Start Date cannot be determined unequivocally then we need
                    // to abandon the attempt altogether.  So - if in the conversion of
                    // hours or minutes to Days, there is NO remainder, then and only
                    // then can we use it to calculate the Start Date, otherwise just
                    // do not set a Start Date at all.
                    case "Hours":
                        theDays = durationValue / 24;
                        if (theDays > 0) {
                            if (durationValue % 24 == 0) setStartDate(getEndDate().minusDays(theDays));
                        }
                        break;
                    case "Minutes":
                        theDays = durationValue / (24 * 60);
                        if (theDays > 0) {
                            if (durationValue % (24 * 60) == 0) setStartDate(getEndDate().minusDays(theDays));
                        }
                        break;
                }
                break;
            case (END_DATE_KNOWN + END_TIME_KNOWN): // We can set both unknown Start fields
            case (END_DATE_KNOWN + END_TIME_KNOWN + START_TIME_KNOWN): // We will override the Start Time
            case (END_DATE_KNOWN + END_TIME_KNOWN + START_DATE_KNOWN): // We will override the Start Date
                theEnd = getEventEndDateTime();
                assert theEnd != null;  // We already know this; it's just to make IJ happy.
                switch (theUnits) {
                    case "Days":
                        setStartDate(getEndDate().minusDays(durationValue));
                        setStartTime(getEndTime());
                        break;
                    case "Weeks":
                        setStartDate(getEndDate().minusWeeks(durationValue));
                        setStartTime(getEndTime());
                        break;
                    case "Hours":
                        theStart = theEnd.minusHours(durationValue);
                        setStartDate(theStart.toLocalDate());
                        setStartTime(theStart.toLocalTime());
                        break;
                    case "Minutes":
                        theStart = theEnd.minusMinutes(durationValue);
                        setStartDate(theStart.toLocalDate());
                        setStartTime(theStart.toLocalTime());
                        break;
                }
                break;
            case START_TIME_KNOWN: // We can set the End Time
            case (START_TIME_KNOWN + END_TIME_KNOWN): // We will override the End Time.
                // Here, we already know both the Start and End times (and so a proposed
                // duration was already calculated and displayed to the user) but the
                // user has now entered their own value (presumably because they know
                // that the 'real' duration is more than one day but less than one day is the
                // default duration when only times are known.  For a forward-thinking
                // person, it makes more sense to 'trust'  the Start Time over the End
                // Time.  But if the user has only increased the number of days in the
                // duration then the end time will actually just stay the same anyway.
                switch (theUnits) {
                    case "Days":
                    case "Weeks":
                        setEndTime(getStartTime());
                        break;
                    case "Hours":
                        setEndTime(getStartTime().plusHours(durationValue));
                        break;
                    case "Minutes":
                        setEndTime(getStartTime().plusMinutes(durationValue));
                        break;
                }
                break;
            case (START_TIME_KNOWN + END_DATE_KNOWN):  // We can set the remaining two values
                // Less straightforward on how to proceed; here is the logic:
                // Convert the duration to minutes and setNotes it the start time so that we can have
                // a known end time to go along with the known end date.  Then use the complete
                // end minus the duration to get back to the correct start date.
                theMinutes = durationToMinutes(durationValue, theUnits);
                setEndTime(getStartTime().plusMinutes(theMinutes));
                theStart = Objects.requireNonNull(getEventEndDateTime()).minusMinutes(theMinutes);
                setStartDate(theStart.toLocalDate());
                break;
            case START_DATE_KNOWN: // We can set the unknown End Date (maybe)
            case (START_DATE_KNOWN + END_DATE_KNOWN):  // We may override a known End Date.
                // Note about overriding the End Date - although the user has previously
                // set it (either explicitly or via an earlier duration calculation), the
                // current duration setting will take precedence since it is  the most
                // recent indication of user intent.  And when the duration units are less
                // than days and the value does not convert to whole days, when the End Date
                // is unknown we just leave it that way.  But if there is a previously
                // set End Date yet we don't have whole days to work with, what to do with
                // it?  Set to null?  Leave alone?  If we leave it alone it might be a value
                // that is wildly illogical given the known start and current duration setting.
                // The possibly imprecise calculation from Hours and Minutes units (below)
                // still gets us to within one day of accuracy.  So if there is a known End
                // Date that is within that one-day window we can leave it alone but otherwise
                // we will have to null it out.
                switch (theUnits) {
                    case "Days":
                        setEndDate(getStartDate().plusDays(durationValue));
                        break;
                    case "Weeks":
                        setEndDate(getStartDate().plusWeeks(durationValue));
                        break;
                    // Notes about Hours and Minutes
                    // Yes, the units are smaller than Days but the value may still
                    // be large enough that a number of days are involved.  So we can
                    // convert to Days and can use the whole number in the new setting,
                    // but what about the remainder?  What if the 'real' start time was
                    // at 11pm on the Start Date?  a duration of 27 hours would put
                    // the end date ahead by two days, not just one.  On the other hand
                    // the 'real' start time might be start of day on the Start Date, so that
                    // the calculated end date should be only one later.  We have
                    // already committed to honoring the user's explicit settings and
                    // making no assumptions on their behalf, but
                    // we need to also honor their explicit non-settings and since the
                    // exact End Date cannot be determined unequivocally then we need
                    // to abandon the attempt altogether.  So - if in the conversion of
                    // hours or minutes to Days, there is NO remainder, then and only
                    // then can we use it to calculate the End Date, otherwise leave it
                    // unknown, unless one had previously been set and it would be illogical
                    // to leave it that way given the new duration and known start.
                    case "Hours":
                        theDays = durationValue / 24;
                            if ((durationValue % 24 == 0) && (theDays > 0))  { // Set (or override) the End Date
                                setEndDate(getStartDate().plusDays(theDays));
                            } else { // Has the End Date previously been set?
                                LocalDate theEndDate = getEndDate();
                                if (theEndDate != null) { // We will not use the imprecise duration to set a new End
                                    // Date but we can check the existing End Date to see if it fits within the
                                    // duration 'window'.  If so then we can just leave it alone.  Otherwise we need
                                    // to null it out.
                                    boolean nullit = true;
                                    if (getStartDate().plusDays(theDays).equals(getEndDate())) nullit = false;
                                    if (getStartDate().plusDays(theDays + 1).equals(getEndDate())) nullit = false;
                                    if (nullit) setEndDate(null);
                                }
                            }
                        break;
                    case "Minutes":
                        theDays = durationValue / (24 * 60);
                            if ((durationValue % (24 * 60) == 0) && (theDays > 0)){ // Set (or override) the End Date
                                setEndDate(getStartDate().plusDays(theDays));
                            } else { // Has the End Date previously been set?
                                LocalDate theEndDate = getEndDate();
                                if (theEndDate != null) { // We will not use the imprecise duration to set a new End
                                    // Date but we can check the existing End Date to see if it fits within the
                                    // duration 'window'.  If so then we can just leave it alone.  Otherwise we need
                                    // to null it out.
                                    boolean nullit = true;
                                    if (getStartDate().plusDays(theDays).equals(getEndDate())) nullit = false;
                                    if (getStartDate().plusDays(theDays + 1).equals(getEndDate())) nullit = false;
                                    if (nullit) setEndDate(null);
                                }
                            }
                        break;
                }
                break;
            case (START_DATE_KNOWN + END_TIME_KNOWN):
                // Calculate the unknowns - Start Time and End Date.
                // Less straightforward on how to proceed; here is the logic:
                // Convert the duration to minutes and apply it the end time so that we can have
                // a known start time to go along with the known start date.  Then use the complete
                // start plus the duration to get to the correct end date.
                theMinutes = durationToMinutes(durationValue, theUnits);
                setStartTime(getEndTime().minusMinutes(theMinutes));
                theEnd = getEventStartDateTime().plusMinutes(theMinutes);
                setEndDate(theEnd.toLocalDate());
                break;
            case (START_DATE_KNOWN + START_TIME_KNOWN):
            case (START_DATE_KNOWN + START_TIME_KNOWN + END_TIME_KNOWN): // We will override the End Time
            case (START_DATE_KNOWN + START_TIME_KNOWN + END_DATE_KNOWN): // We will override the End Date
            case ALL_KNOWN: // We will override the End Date and End Time
                theStart = getEventStartDateTime();
                switch (theUnits) {
                    case "Days":
                        setEndDate(getStartDate().plusDays(durationValue));
                        setEndTime(getStartTime());
                        break;
                    case "Weeks":
                        setEndDate(getStartDate().plusWeeks(durationValue));
                        setEndTime(getStartTime());
                        break;
                    case "Hours":
                        theStart = theStart.plusHours(durationValue);
                        setEndDate(theStart.toLocalDate());
                        setEndTime(theStart.toLocalTime());
                        break;
                    case "Minutes":
                        theStart = theStart.plusMinutes(durationValue);
                        setEndDate(theStart.toLocalDate());
                        setEndTime(theStart.toLocalTime());
                        break;
                }
                break;
        }
        settingDuration = originalSettingDuration; // Restore the boolean.
    }

    private int durationToMinutes(int durationValue, String theUnits) {
        int theMinutes = 0;

        switch (theUnits) {
            case "Minutes":
                theMinutes = durationValue;
                break;
            case "Hours":
                theMinutes = durationValue * 60;
                break;
            case "Days":
                theMinutes = durationValue * 60 * 24;
                break;
            case "Weeks":
                theMinutes = durationValue * 60 * 24 * 7;
                break;
        }
        return theMinutes;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) return true; // self check
        if (otherObject == null) return false; // null check
        if (getClass() != otherObject.getClass()) return false; // type check

        EventNoteData otherEvent = (EventNoteData) otherObject;

        if(!noteString.equals(otherEvent.noteString)) return false;
        if(!extendedNoteString.equals(otherEvent.extendedNoteString)) return false;
        if(eventStartDateString == null && otherEvent.eventStartDateString != null) return false;
        if(eventStartDateString != null) {
            if (!eventStartDateString.equals(otherEvent.eventStartDateString)) return false;
        }
        if(eventEndDateString == null && otherEvent.eventEndDateString != null) return false;
        if(eventEndDateString != null) {
            if(!eventEndDateString.equals(otherEvent.eventEndDateString)) return false;
        }

        return null != recurrenceString || otherEvent.recurrenceString == null;

//        if(!eventStartTimeString.equals(otherEvent.eventStartTimeString)) return false;
//        if(!eventEndTimeString.equals(otherEvent.eventEndTimeString)) return false;
//        if(!durationUnits.equals(otherEvent.durationUnits)) return false;
//        if(!durationValue.equals(otherEvent.durationValue)) return false;
    }

    // NOTES:
    //
    // When setting a date or time it is true that the Event may
    //   have previously contained a duration.  However, (per
    //   design requirements) ANY such setting must invalidate a
    //   duration because it should then be recalculated.  The
    //   previous value is NOT considered when taking in new
    //   user-specified Date/Time settings.
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
        touchLastMod();
        if (newEndDate == null) { // The user is 'un-setting' the date.
            eventEndDateString = null;
            recalcDuration();
            return true;
        } // end if UNsetting a date

        // Get a temporary composite Event End, for possible comparison to the start.
        LocalDateTime newEventEnd;
        LocalTime endTime = getEndTime();
        if (endTime != null) {
            newEventEnd = newEndDate.atTime(endTime);
        } else {
            newEventEnd = newEndDate.atTime(23, 59, 59); // Assume end-of-day.
        }

        // Continue setting up for the comparison -
        LocalDate theStartDate = getStartDate();
        if (theStartDate != null) { // Comparison not possible without a start date.
            LocalDateTime eventStart;  // Make a temporary composite Event Start.
            LocalTime startTime = getStartTime();
            if (startTime != null) {
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
        touchLastMod();
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
            if (theStartDate != null) { // otherwise no comparison possible.
                LocalTime theStartTime = getStartTime();
                if (theStartTime != null) { // otherwise it defaults to start-of-day and the composite start could
                    // not possibly be after the end, even if start and end occur on the same day.
                    // So this is the only logical branch where a change of the end time might be rejected.
                    eventStart = theStartDate.atTime(theStartTime);
                    // The comparison - Event Start must be before Event End.
                    if (newEventEnd.isBefore(eventStart)) return false;
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
        touchLastMod();
    }

    public void setRecurrence(String s) {
        recurrenceString = s;
        touchLastMod();
    }

    void setRetainNote(boolean b) {
        blnRetainNote = b;
        // Keeping the note (or not) should not affect the Last Mod date.
    }

    public boolean setStartDate(LocalDate newStartDate) {
        touchLastMod();
        recurrenceString = ""; // Setting (or unsetting) the Start date invalidates recurrence.

        if (newStartDate == null) { // The user is 'un-setting' the date.
            eventStartDateString = null;
            recalcDuration();
            return true;
        } // end if UNsetting a date

        // Get a temporary composite Event Start, for possible comparison to the end.
        LocalDateTime newEventStart;
        LocalTime startTime = getStartTime();
        if (startTime != null) {
            newEventStart = newStartDate.atTime(startTime);
        } else {
            newEventStart = newStartDate.atStartOfDay();
        }

        // Continue setting up for the comparison -
        LocalDate theEndDate = getEndDate();
        if (theEndDate != null) { // Comparison not possible without an end date.
            LocalDateTime eventEnd;  // Make a temporary composite Event End.
            LocalTime endTime = getEndTime();
            if (endTime != null) {
                eventEnd = theEndDate.atTime(endTime);
            } else { // Assume it runs to the end of the day.
                eventEnd = theEndDate.atTime(23, 59, 59);
            }
            // The comparison -
            // Ensure that the proposed new Event Start is not after the current Event End.
            if (newEventStart.isAfter(eventEnd)) return false;
        }

        // Accept the proposed new Start date and recalculate duration.
        eventStartDateString = newStartDate.toString();
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
        touchLastMod();
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
            if (theEndDate != null) { // otherwise no comparison possible.
                LocalTime theEndTime = getEndTime();
                if (theEndTime != null) { // otherwise it defaults to end-of-day and the composite end could
                    // not possibly be ahead of the start, even if start and end occur on the same day.
                    // So this is the only logical branch where a change of the start time might be rejected.
                    eventEnd = theEndDate.atTime(theEndTime);
                    // The comparison - Event End must be after Event Start.
                    if (newEventStart.isAfter(eventEnd)) return false;
                } // end if (no 'else' needed)
            } // end if - if we have a start date
        } // end if

        // Accept the proposed new Start Time and recalculate duration.
        eventStartTimeString = newStartTime.toString();

        // Any new setting of the four main components invalidates duration,
        // unless it happens WHILE we're setting duration.
        recalcDuration();

        return true;
    } // end setStartTime

} // end class EventNoteData
