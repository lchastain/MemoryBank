/*  Displays a calendar-based group of NoteComponent.  The
    calendar is set to one of DAY, MONTH, YEAR.

 */

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public abstract class CalendarNoteGroupPanel extends NoteGroupPanel {
    LocalDate theChoice;   // Holds the date of the group that the Panel is currently displaying.
    DateTimeFormatter dtf; // Child classes display the date in different formats.

    private AlteredDateListener alteredDateListener = null;
    private ChronoUnit dateType;
    JLabel panelTitleLabel;


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
//            default: // Should not ever happen, but need to cover all bases.
//                dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        }
        // Create the panel's title
        panelTitleLabel = new JLabel();
        panelTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        panelTitleLabel.setForeground(Color.white);
        panelTitleLabel.setFont(Font.decode("Serif-bold-20"));

        theChoice = LocalDate.now();
        GroupInfo groupInfo = new GroupInfo(getTitle(), groupType);
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.
        myNoteGroup.myNoteGroupPanel = this;
        loadNotesPanel(); // previously was done via updateGroup; remove this comment when stable.
    }


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


    void setAlteredDateListener(AlteredDateListener adl) {
        alteredDateListener = adl;
    }


    public void setDate(LocalDate theNewChoice) {
        // If the new date is the same as the one that is currently showing then the inclination is to just return
        // without taking any action.  However, that does not cover the case where the data for the date that is
        // showing has been changed outside of the Panel, and needs to be reloaded.  So - inclination override.
        //if (dtf.format(getChoice()).equals(dtf.format(theNewChoice))) return;

        // The proper approach for all accesses is to save a group before possibly altering it.  This doesn't mean
        // that it always happens that way but if it doesn't then there could be data-loss type problems.  So for
        // the hypothetical case where the data for this date has been externally changed, any changes we had in
        // progress in this Panel will have already been saved by the context that made the external change, before
        // it made that change, and so the call below
        // will NOT result in the old data going back to overwrite the newer info because preClose will not make
        // a call to save since by then the groupChanged flag will be false.  On the other hand, if there has been
        // no other access to the group and there ARE unsaved changes in the Panel, then the call below will capture
        // them.
        preClosePanel();

        theChoice = theNewChoice;
        myNoteGroup.myGroupInfo.setGroupName(getTitle()); // Fix the GroupInfo prior to data load
        updateGroup();  // Be aware that this clears the panel, which clears the source data.
        // In operational use cases that works just fine; tests, however, might not be happy about it.

        updateHeader();
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



    // This one-liner is broken out as a separate method to simplify the coding
    //   from the calling contexts, and also to help them be more readable.
    void updateHeader() {
        // Generate a new title from current choice.
        panelTitleLabel.setText(dtf.format(getChoice()));
    } // end updateHeader



} // end class CalendarNoteGroup
