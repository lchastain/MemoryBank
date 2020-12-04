/*  Displays a calendar-based group of NoteComponent.  The
    calendar is set to one of DAY, MONTH, YEAR.

 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public abstract class CalendarNoteGroupPanel extends NoteGroupPanel {
    LocalDate theChoice;   // Holds the date of the group that the Panel is currently displaying.
    DateTimeFormatter dtf; // Child classes display the date in different formats.

    private AlteredDateListener alteredDateListener = null;
    private ChronoUnit dateType;
    JLabel panelTitleLabel;
    LabelButton todayButton = makeAlterButton("T", null);

    CalendarNoteGroupPanel(GroupType groupType) {
        // The implicit call to the base class constructor is what builds the notes panel

        // At this point we do not yet know our exact name.
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



    // Too many AlterButtons all need the same formatting; may as well do it in one place -
    LabelButton makeAlterButton(String theText, MouseListener theListener) {
        LabelButton theButton = new LabelButton(theText);
        if(theListener != null) theButton.addMouseListener(theListener);
        theButton.setPreferredSize(new Dimension(28, 28));
        theButton.setFont(Font.decode("Dialog-bold-14"));
        return theButton;
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

        // This new date will be used to generate a new title for the Panel, which also happens to be the Group Name.
        theChoice = theNewChoice;

        // This reset of the GroupInfo groupName is needed because the name it currently has is still the 'old' date,
        //   and the GroupInfo is what is used to identify the Group that needs to be loaded.  After the load, the
        //   existing GroupProperties, if any, are cleared out so that the new ones, if any, can be deserialized
        //   into them from the data that was loaded.  If none were loaded, new ones are created when
        //   getGroupProperties() is called, and the name used at that time will be the one we set here and now.
        myNoteGroup.myGroupInfo.setGroupName(getTitle()); // Fix the GroupInfo.groupName prior to data load

        updateGroup();  // Be aware that this clears the panel, which clears the source data.
        // In operational use cases that works just fine; tests, however, might not be happy about it.
    } // end setDate


    void setDateType(ChronoUnit theType) {
        dateType = theType;
    }


    // After this, the Group is reloaded according to the current choice.
    // It does not go through 'setDate'; instead the calling context calls 'updateGroup'.
    public void setOneBack() {
        preClosePanel(); // Save the current one first, if needed.
        myNoteGroup.setGroupProperties(null); // There may be no file to load, so this is needed here.
        theChoice = theChoice.minus(1, dateType);
        myNoteGroup.myGroupInfo.setGroupName(getTitle()); // Fix the GroupInfo.groupName prior to data load
        if(alteredDateListener != null) alteredDateListener.dateDecremented(theChoice, dateType);
    } // end setOneBack


    public void setOneForward() {
        preClosePanel(); // Save the current one first, if needed.
        myNoteGroup.setGroupProperties(null); // There may be no file to load, so this is needed here.
        theChoice = theChoice.plus(1, dateType);
        myNoteGroup.myGroupInfo.setGroupName(getTitle()); // Fix the GroupInfo.groupName prior to data load
        if(alteredDateListener != null) alteredDateListener.dateIncremented(theChoice, dateType);
    } // end setOneForward


    @Override
    public void updateGroup() {
        super.updateGroup();

        String today = dtf.format(LocalDate.now());
        todayButton.setEnabled(!getTitle().equals(today));

        updateHeader();
    }

    // This one-liner is broken out as a separate method to simplify the coding
    //   from the calling contexts, and also to help them be more readable.
    void updateHeader() {
        // Generate a new title from current choice.
        panelTitleLabel.setText(dtf.format(getChoice()));
    } // end updateHeader

} // end class CalendarNoteGroup
