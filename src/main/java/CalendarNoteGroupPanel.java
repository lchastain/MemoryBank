/*  Displays a calendar-based group of NoteComponent.  The
    calendar is set to one of DAY, MONTH, YEAR.

 */

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Vector;

public abstract class CalendarNoteGroupPanel extends NoteGroupPanel {
    static String areaName;
    static String areaPath;

    LocalDate theChoice;  // Holds the 'current' date of the displayed Group.
    DateTimeFormatter dtf;

    private AlteredDateListener alteredDateListener = null;
    private ChronoUnit dateType;

    static {
        areaName = "Years"; // Directory name under user data.
        areaPath = basePath + areaName + File.separatorChar;
    }

    CalendarNoteGroupPanel(String defaultSubject) {
        super();
        super.setDefaultSubject(defaultSubject);

        myProperties = null; // We get a different Properties with every choice.
        theChoice = LocalDate.now();
//        setGroupFilename(getGroupFilename());  not needed?  happens via the call to updateGroup, below.

        switch (defaultSubject) {
            case "Day Note":
                dateType = ChronoUnit.DAYS;
                myGroupDataType = new TypeReference<Vector<DayNoteData>>() { };
                break;
            case "Month Note":
                dateType = ChronoUnit.MONTHS;
                break;
            case "Year Note":
                dateType = ChronoUnit.YEARS;
                break;
        }

        updateGroup(); // Load the data and properties, if there are any.
    } // end constructor


    // A NoteGroup does not have a 'choice'; a CalendarNoteGroup does.
    public LocalDate getChoice() {
        return theChoice;
    }


    // This method is needed for CalendarNoteGroup types because of their timestamped filenames.
    @Override
    protected String getGroupFilename() {
        String s;

        if (saveIsOngoing) {
            // In this case we need a new filename; need to make one because (due to timestamping) it
            // almost certainly does not already exist.
            if (dateType == ChronoUnit.DAYS) s = NoteGroupFile.makeFullFilename(theChoice, "D");
            else if (dateType == ChronoUnit.MONTHS) s = NoteGroupFile.makeFullFilename(theChoice, "M");
            else s = NoteGroupFile.makeFullFilename(theChoice, "Y");
            return s;
        } else {  // Results of a findFilename may be "".
            if (dateType == ChronoUnit.DAYS) s = NoteGroupFile.foundFilename(theChoice, "D");
            else if (dateType == ChronoUnit.MONTHS) s = NoteGroupFile.foundFilename(theChoice, "M");
            else s = NoteGroupFile.foundFilename(theChoice, "Y");
            return s;
        } // end if saving else not saving
    } // end getGroupFilename

    // A CalendarNoteGroup has a different GroupProperties for every choice.
    @Override
    GroupProperties getGroupProperties() {
        if(myProperties == null) {
            // If we loaded our properties member from a file then we need to use that one because it may
            // already contain linkages.  Otherwise it will be null and we can just make one right now.
            switch(dateType) {
                case DAYS:
                    myProperties = new GroupProperties(getTitle(), GroupInfo.GroupType.DAY_NOTES);
                    break;
                case MONTHS:
                    myProperties = new GroupProperties(getTitle(), GroupInfo.GroupType.MONTH_NOTES);
                    break;
                case YEARS:
                    myProperties = new GroupProperties(getTitle(), GroupInfo.GroupType.YEAR_NOTES);
                    break;
                default:
                    myProperties = new GroupProperties(getTitle(), GroupInfo.GroupType.NOTES);
            }
        }
        myProperties.myNoteGroupPanel = this;
        return myProperties;
    }

    String getTitle() {
        //return NoteGroupDataAccessor.getGroupNameForDay(getChoice());
        // The above line would work, but routing the op through the interface static method somewhat obfuscates the
        // code.  Better to just have a bit of duplication with an extra formatter defined in this class (dtf), for
        // overall readability.

        return dtf.format(getChoice());
    }



    //--------------------------------------------------------------
    // Method Name: setDate
    //
    // A calling context should only make this call if it is
    //   needed, because it causes a reload of the group.
    //--------------------------------------------------------------
    public void setDate(LocalDate theNewChoice) {
        theChoice = theNewChoice;
        updateGroup();
    } // end setDate


    public void setOneBack() {
        preClosePanel();
        myProperties = null; // There may be no file to load, so this is needed here.
        theChoice = theChoice.minus(1, dateType);
        if(alteredDateListener != null) alteredDateListener.dateDecremented(theChoice, dateType);
    } // end setOneBack


    public void setOneForward() {
        preClosePanel();
        myProperties = null; // There may be no file to load, so this is needed here.
        theChoice = theChoice.plus(1, dateType);
        if(alteredDateListener != null) alteredDateListener.dateIncremented(theChoice, dateType);
    } // end setOneForward


    void setAlteredDateListener(AlteredDateListener adl) {
        alteredDateListener = adl;
    }

} // end class CalendarNoteGroup
