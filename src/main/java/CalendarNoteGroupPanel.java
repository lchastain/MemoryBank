/*  Displays a calendar-based group of NoteComponent.  The
    calendar is set to one of DAY, MONTH, YEAR.

 */

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public abstract class CalendarNoteGroupPanel extends NoteGroupPanel {
    LocalDate theChoice;   // Holds the date of the group that the Panel is currently displaying.
    DateTimeFormatter dtf; // Child classes display the date in different formats.

    private AlteredDateListener alteredDateListener = null;
    private ChronoUnit dateType;


    CalendarNoteGroupPanel(GroupInfo.GroupType groupType) {
        super();  // This builds the notes panel

        // Unlike other group types, we do not start off knowing our exact name.
        // But we do know that it will be some format of 'today'.
        switch(groupType) { // This Panel should not be constructed with any other types.
            case YEAR_NOTES:
                super.setDefaultSubject("Year Note");
                dateType = ChronoUnit.YEARS;
                dtf = DateTimeFormatter.ofPattern("yyyy");
                break;
            case MONTH_NOTES:
                super.setDefaultSubject("Month Note");
                dateType = ChronoUnit.MONTHS;
                dtf = DateTimeFormatter.ofPattern("MMMM yyyy");
                break;
            case DAY_NOTES:
                super.setDefaultSubject("Day Note");
                dateType = ChronoUnit.DAYS;
                dtf = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
                break;
        }
        theChoice = LocalDate.now();
        GroupInfo groupInfo = new GroupInfo(getTitle(), groupType);
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.
        myNoteGroup.myNoteGroupPanel = this;

        loadNotesPanel(); // previously was done via updateGroup; remove this comment when stable.
    }

    // original (mostly) -
    CalendarNoteGroupPanel(String defaultSubject) {
        super();
        super.setDefaultSubject(defaultSubject);

        myNoteGroup.setGroupProperties(null); // We get a different Properties with every choice.
        theChoice = LocalDate.now();
//        setGroupFilename(getGroupFilename());  not needed?  happens via the call to updateGroup, below.

        switch (defaultSubject) {
            case "Day Note":
                dateType = ChronoUnit.DAYS;
                break;
            case "Month Note":
                dateType = ChronoUnit.MONTHS;
                break;
            case "Year Note":
                dateType = ChronoUnit.YEARS;
                break;
        }

        updateGroup(); // Load the data and properties, if there are any.
        myNoteGroup.myNoteGroupPanel = this;
    } // end constructor


    // A NoteGroupPanel does not have a 'choice'; a CalendarNoteGroupPanel does.
    public LocalDate getChoice() {
        return theChoice;
    }

// This logic has been ported to NoteGroupFile.  But unproven.  Keep this as a reference until proven.
    // Check this just prior to loading the group rather than earlier, because the group content may
    //   have changed so that the file to load now is not the same as it was at group construction;
    //   the filename for the group depends on the base date shown in the panel.
//    @Override
//    protected String getGroupFilename() {
//        String s;
//
//        if (saveIsOngoing) {
//            // In this case we need a new filename; need to make one because (due to timestamping) it
//            // almost certainly does not already exist.
//            if (dateType == ChronoUnit.DAYS) s = NoteGroupFile.makeFullFilename(theChoice, "D");
//            else if (dateType == ChronoUnit.MONTHS) s = NoteGroupFile.makeFullFilename(theChoice, "M");
//            else s = NoteGroupFile.makeFullFilename(theChoice, "Y");
//            return s;
//        } else {  // Results of a findFilename may be "".
//            if (dateType == ChronoUnit.DAYS) s = NoteGroupFile.foundFilename(theChoice, "D");
//            else if (dateType == ChronoUnit.MONTHS) s = NoteGroupFile.foundFilename(theChoice, "M");
//            else s = NoteGroupFile.foundFilename(theChoice, "Y");
//            return s;
//        } // end if saving else not saving
//    } // end getGroupFilename

    @Override
    protected void getPanelData() {
        // Needed when the date has been altered.  We don't know that it HAS been changed; this is just a catch-all.
        myNoteGroup.setGroupProperties(myNoteGroup.getGroupProperties());

        super.getPanelData();
    }

    // The title of CalendarNoteGroups is just the date that the panel is set to,
    // formatted to the granularity of the Panel (day, month, year).
    String getTitle() {
        return dtf.format(getChoice());
    }



    // A calling context should only make this call if it is
    //   needed, because it causes a reload of the group.
    public void setDate(LocalDate theNewChoice) {
        preClosePanel();
        theChoice = theNewChoice;
        updateGroup();
    } // end setDate


    public void setOneBack() {
        preClosePanel(); // Save the current one first, if needed.
        myNoteGroup.setGroupProperties(null); // There may be no file to load, so this is needed here.
        theChoice = theChoice.minus(1, dateType);
        myNoteGroup.myGroupInfo.setGroupName(getTitle());
        if(alteredDateListener != null) alteredDateListener.dateDecremented(theChoice, dateType);
    } // end setOneBack


    public void setOneForward() {
        preClosePanel(); // Save the current one first, if needed.
        myNoteGroup.setGroupProperties(null); // There may be no file to load, so this is needed here.
        theChoice = theChoice.plus(1, dateType);
        myNoteGroup.myGroupInfo.setGroupName(getTitle());
        if(alteredDateListener != null) alteredDateListener.dateIncremented(theChoice, dateType);
    } // end setOneForward


    void setAlteredDateListener(AlteredDateListener adl) {
        alteredDateListener = adl;
    }

} // end class CalendarNoteGroup
