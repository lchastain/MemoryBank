/*  Displays a calendar-based group of NoteComponent.  The
    calendar is set to one of DAY, MONTH, YEAR.

 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public abstract class CalendarNoteGroupPanel extends NoteGroupPanel {
    LocalDate theDate;   // Holds the date of the group that the Panel is currently displaying.
    DateTimeFormatter dtf; // Child classes display the date in different formats.

    AlteredDateListener alteredDateListener = null;
    DateRelatedDisplayType myDateType;
    ChronoUnit dateGranularity;
    JLabel panelTitleLabel;
    LabelButton todayButton = makeAlterButton("T", null);
    boolean reviewMode;
    LocalDate archiveDate;

    CalendarNoteGroupPanel(GroupType groupType) {
        // The implicit call to the base class constructor is what builds the notes panel

        // At this point we do not yet know our exact name.
        // But we do know that it will be some format of 'today'.
        // And the fact that it IS 'today' means that we need to disable the 'T' button,
        //   which would have been enabled when we updated the group before we had our title.
        todayButton.setEnabled(false);
        switch (groupType) { // This Panel should not be constructed with any other types.
            case YEAR_NOTES -> {
                setDefaultSubject("Year Note");
                dateGranularity = ChronoUnit.YEARS;
                dtf = DateTimeFormatter.ofPattern("yyyy");
            }
            case MONTH_NOTES -> {
                setDefaultSubject("Month Note");
                dateGranularity = ChronoUnit.MONTHS;
                dtf = DateTimeFormatter.ofPattern("MMMM yyyy");
            }
            case DAY_NOTES -> {
                setDefaultSubject("Day Note");
                dateGranularity = ChronoUnit.DAYS;
                dtf = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
            }
        }
        // Create the panel's title
        panelTitleLabel = new JLabel();
        panelTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        panelTitleLabel.setForeground(Color.white);
        panelTitleLabel.setFont(Font.decode("Serif-bold-20"));

        theDate = AppTreePanel.theInstance.getViewedDate(); // was LocalDate.now();
        GroupInfo groupInfo = new GroupInfo(getTitle(), groupType);
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.
        myNoteGroup.myNoteGroupPanel = this;
        loadNotesPanel(); // previously was done via updateGroup; remove this comment when stable.

        archiveDate = null; // unless and until 'setArchiveDate' is called.
        reviewMode = AppMenuBar.reviewMode.isSelected();
        setListMenu(AppMenuBar.getNodeMenu("Calendar Notes"));
    }


    // A NoteGroupPanel does not have a Date; a CalendarNoteGroupPanel does.
    public LocalDate getDate() {
        return theDate;
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
        return getTitle(getDate());
    }

    String getTitle(LocalDate theDate) {
        return dtf.format(theDate);
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


    void setArchiveDate(LocalDate theArchiveDate) {
        archiveDate = theArchiveDate;
        setDate(archiveDate); // Set the date of this Panel to the date of the archive
        setEditable(false);
        reviewMode = true;
    }

    // If there has been a significant enough date change to warrant a panel reload then save changes, set a new date
    //   for the panel, clear the panel, load the data for the new date.  Otherwise just accept the new date.
    public void setDate(LocalDate theNewChoice) {
        // Is the new date 'far' enough from the one it currently has, to justify a panel update?
        if (!getTitle().equals(getTitle(theNewChoice))) {
            preClosePanel(); // Save changes, if any.

            // This new date will be used to generate a new title for the Panel, which also happens to be the Group Name.
            theDate = theNewChoice;

            // This reset of the GroupInfo groupName is needed because the name it currently has is still based on the 'old'
            //   date, and the GroupInfo is what is used to identify the Group that needs to be loaded.  After the load, the
            //   existing GroupProperties, if any, are cleared out so that the new ones, if any, can be deserialized
            //   into them from the data that was loaded.  If none were loaded, new ones are created when
            //   getGroupProperties() is called, and the name used at that time will be the one we set here and now.
            myNoteGroup.myGroupInfo.setGroupName(getTitle()); // Set the groupName now in case there is no data to load.

            // Reload the data, if there is any for the new date.
            updateGroup();  // Be aware that this clears the panel, which also clears the source data.
            // In operational use cases this works just fine; test classes, however, could get tripped up if they
            //   intend to keep using that same data for the next test in the test class.
        } else { // Take the new date but decline the panel reload; we are already showing this date.
            theDate = theNewChoice;
        }

        // There is one feature of the MB app that warrants some discussion:  The ability to move a Todo Item from its
        // list to a specific date.  When this happens, the tree will be showing the To Do List, and the viewedDate
        // (that the DayNotes panel holds) may be the one to which the todo item data is being moved.  In that case the
        // DayNotes panel data is preserved prior to adding the TodoItem and then it is saved again after that, and
        // then the AppTreePanel is cleared of the DayNotes panel so that a reload is forced if Day Notes is selected
        // on the tree at some point after the move of the item.  In that case we would only be arriving here upon the
        // tree selection, and there would be no unsaved changes here to preserve, that could possibly overwrite the
        // data to which the To Do Item had been moved.
    } // end setDate


    // Reduce the date by one increment and notify the AlteredDateListener, if one is defined.
    // To load in the new data after this, calling contexts should call 'updateGroup()'.
    // They should not call setDate() after this because the only value it would provide at that point is to make that
    //   call for them, while repeating most of the other tasks that by then would have already been done here.
    public void setOneBack(ChronoUnit theMagnitude) {
        preClosePanel(); // Save the current one first, if needed.
        myNoteGroup.setGroupProperties(null); // There may be no file to load, so this is needed here.
        if(reviewMode) {
            DataAccessor dataAccessor = MemoryBank.dataAccessor;
            NoteGroupDataAccessor noteGroupDataAccessor = dataAccessor.getNoteGroupDataAccessor(myNoteGroup.myGroupInfo);
            theDate = noteGroupDataAccessor.getNextDateWithData(theDate, theMagnitude, CalendarNoteGroup.Direction.BACKWARD);
        } else {
            theDate = theDate.minus(1, theMagnitude);
        }
        myNoteGroup.myGroupInfo.setGroupName(getTitle()); // Fix the GroupInfo.groupName prior to data load
        if(alteredDateListener != null) alteredDateListener.dateChanged(myDateType, theDate);
    } // end setOneBack

    // Increase the date by one increment and notify the AlteredDateListener, if one is defined.
    // To load in the new data after this, calling contexts should call 'updateGroup()'.
    // They should not call setDate() after this because the only value it would provide at that point is to make that
    //   call for them, while repeating most of the other tasks that by then would have already been done here.
    public void setOneForward(ChronoUnit theMagnitude) {
        preClosePanel(); // Save the current one first, if needed.
        myNoteGroup.setGroupProperties(null); // There may be no file to load, so this is needed here.
        if(reviewMode) {
            boolean hardStop = false; // Flag to keep us from going beyond the archive date.
            if(archiveDate != null) {
                if (theDate.isEqual(archiveDate)) hardStop = true;
                if (theDate.plus(1, theMagnitude).isAfter(archiveDate)) hardStop = true;
            }
            DataAccessor dataAccessor = MemoryBank.dataAccessor;
            NoteGroupDataAccessor noteGroupDataAccessor = dataAccessor.getNoteGroupDataAccessor(myNoteGroup.myGroupInfo);
            if(hardStop) {
                System.out.println("Reached end of archive! \t" + archiveDate.toString());
            }
            else theDate = noteGroupDataAccessor.getNextDateWithData(theDate, theMagnitude, CalendarNoteGroup.Direction.FORWARD);
        } else {
            theDate = theDate.plus(1, theMagnitude);
        }
        myNoteGroup.myGroupInfo.setGroupName(getTitle()); // Fix the GroupInfo.groupName prior to data load
        if(alteredDateListener != null) alteredDateListener.dateChanged(myDateType, theDate);
    } // end setOneForward


    @Override
    public void updateGroup() {
        super.updateGroup();
        String today;

        today = dtf.format(Objects.requireNonNullElseGet(archiveDate, LocalDate::now));
        todayButton.setEnabled(!getTitle().equals(today));

        updateHeader();
    }

    // This one-liner is broken out as a separate method to simplify the coding
    //   from the calling contexts, and also to help them be more readable.
    void updateHeader() {
        // Generate a new title from current choice.
        panelTitleLabel.setText(dtf.format(getDate()));
    } // end updateHeader

} // end class CalendarNoteGroup
