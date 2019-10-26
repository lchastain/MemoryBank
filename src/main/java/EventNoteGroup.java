/*  Manage notes on upcoming events.

 */

import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Vector;

public class EventNoteGroup extends NoteGroup
        implements iconKeeper, DateSelection {
    private static final long serialVersionUID = 1L;

    // Notes on the implemented interfaces:
    //---------------------------------------------------------------------
    // DateSelection - method is dateSelected, to respond to TMC clicks.

    // Because the parent NoteGroup class is where all NoteComponents get
    //   made and that constructor runs before the one here, the defaultIcon
    //   (seen in an EventNoteComponent) MUST be present BEFORE that
    //   constructor is called.  This is why we need to
    //   assign it during the static section of this class.
    //------------------------------------------------------------------
    private static EventNoteDefaults eventNoteDefaults;
    private static AppIcon defaultIcon;
    private static Notifier optionPane;

    private ThreeMonthColumn tmc;
    private EventHeader theHeader;
    private EventNoteComponent eNoteComponent;

    static {
        eventNoteDefaults = new EventNoteDefaults();
        eventNoteDefaults.load();

        if (eventNoteDefaults.defaultIconFileName.equals("")) {
            MemoryBank.debug("Default EventNoteComponent Icon: <blank>");
            defaultIcon = new AppIcon();
        } else {
            MemoryBank.debug("Default EventNoteComponent Icon: " + eventNoteDefaults.defaultIconFileName);
            defaultIcon = new AppIcon(eventNoteDefaults.defaultIconFileName);
           AppIcon.scaleIcon(defaultIcon);
        } // end if/else
    } // end static section


    EventNoteGroup() {
        super();

        eNoteComponent = null;
        tmc = new ThreeMonthColumn();
        theHeader = new EventHeader(this);

        tmc.setSubscriber(this);

        add(theHeader, BorderLayout.NORTH);

        // Wrapped tmc in a FlowLayout panel, to prevent stretching.
        JPanel pnl1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnl1.add(tmc);
        add(pnl1, BorderLayout.EAST);

        optionPane = new Notifier() { }; // Uses all default methods.
        refresh();
    }// end constructor


    // -------------------------------------------------------------------
    // Method Name: ageEvents
    //
    // This method examines each event and if its start is after the
    //   current date/time, will 'age' it according to its recurrence.
    //   If there is no recurrence or the final occurrence is exceeded,
    //   the event is 'aged off'.
    // The return value will be true if one or more events were aged
    //   off.  If they were not aged at all, or only aged forward, the
    //   return value is false.
    // -------------------------------------------------------------------
    private boolean ageEvents() {
        boolean blnDropThisEvent;
        boolean blnAnEventWasAged = false;

        String s;
        EventNoteData tempNoteData;

        // AppUtil.localDebug(true);
        for (NoteData ndTmp : vectGroupData) {
            blnDropThisEvent = false;
            tempNoteData = (EventNoteData) ndTmp;

            s = tempNoteData.getNoteString();
            MemoryBank.debug("Examining: " + s + " starting " + tempNoteData.getEventStartDateTime());

            // 'Age' the event, if appropriate to do so.
            while (tempNoteData.hasStarted()) {
                if (tempNoteData.getRetainNote()) {
                    // We save this version of the event.
                    DayNoteData dnd = new DayNoteData(tempNoteData);
                    String theFilename;
                    LocalDateTime ansr = tempNoteData.getEventStartDateTime();

                    theFilename = AppUtil.findFilename(ansr.toLocalDate(), "D");
                    if (theFilename.equals("")) {
                        theFilename = AppUtil.makeFilename(ansr.toLocalDate(), "D");
                    } // end if
                    boolean success = AppUtil.addNote(theFilename, dnd);

                    // Although we test the result for success or fail, this
                    //   action could be one of potentially hundreds, and one
                    //   of the times it will occur is at load time while the
                    //   'always on top' splash screen is displayed.  It is
                    //   definitely NOT a good time to stop at each problem
                    //   and complain to the user with an error dialog that
                    //   they must review and dismiss.
                    if (success) {
                        DayNoteGroup.blnNoteAdded = true;
                        MemoryBank.debug("  Retention of Event data succeeded");
                    } else {
                        MemoryBank.debug("  Retention of Event data failed");
                    } // end if
                } // end if retain note

                // Now adjust forward in time -
                if (tempNoteData.getRecurrenceString().trim().equals("")) { // No recurrence
                    blnDropThisEvent = true;
                    break; // get out of the while loop
                } else {  // There is recurrence, but not necessarily indefinite
                    if (!tempNoteData.goForward()) {
                        // It might not have moved at all, or not enough to get past 'today'.
                        MemoryBank.debug("  The Event has started but is still ongoing; cannot age it yet.");
                        break; // get out of the while loop
                    } else {  // a new Event Start was set by 'goForward'
                        MemoryBank.debug("  Aged forward to: " + tempNoteData.getEventStartDateTime());
                        blnAnEventWasAged = true;
                    } // end if
                } // end if
            } // end while the event Start date is in the past

            if (blnDropThisEvent) {
                MemoryBank.debug("  Aging off " + tempNoteData.getNoteString());
                tempNoteData.clear();
                blnAnEventWasAged = true;
            } // end if

        } // end for - for each Event

        //  Just clearing DATA (above) does not set noteChanged (nor should it,
        //    because that data may not even be loaded into a component).
        //  So since we can't go that route to a groupChanged, just do it explicitly.
        if (blnAnEventWasAged) setGroupChanged();
        // AppUtil.localDebug(false);
        return blnAnEventWasAged;
    } // end ageEvents


    //-------------------------------------------------------------
    // Method Name:  dateSelected
    //
    // Interface to the Three Month Calendar; called by the tmc.
    //-------------------------------------------------------------
    public void dateSelected(LocalDate selectedDate) {
        // System.out.println("LogEvents - date selected on TMC = " + d);

        if (eNoteComponent == null) {
            String s;
            s = "You must select an Event before a Start date can be set!";
            setMessage(s);
            tmc.setChoice(null);
            return;
        } // end if

        EventNoteData end = (EventNoteData) (eNoteComponent.getNoteData());

        // A TMC setting will only affect the Start Date.  Before we
        //   do that, we NULL out the End Date.  Why?  Because...
        // It is possible for a new start date setting to be rejected
        //   because it conflicts with an existing end date.  In
        //   those cases where it would NOT conflict, however, there
        //   may have been a duration that is now going to calculate
        //   as (possibly much) longer.  So, either way, when a
        //   start date is set from the TMC, the best approach is to
        //   simply throw away any pre-existing end date.  (We already
        //   do that with recurrence when a start date is changed).
        end.setEndDate(null);
        end.setStartDate(selectedDate);

        // OR - grab the initial duration first, then null the end,
        //   then set the start, then set that duration ?

        // System.out.println(d);
        eNoteComponent.setEventNoteData(end);
        theHeader.setEventSummary(end.getSummary());
    } // end dateSelected


    //---------------------------------------------------------
    // Method Name: doSort
    //
    // Sorting is done ascending only, with unsortables
    //   collected at the 'bottom' of the list.  Note that
    //   preSort() is not needed; this method is only called
    //   from refresh(), where a preClose() is done, which
    //   calls the saveGroup(), which calls unloadInterface(),
    //   which is what happens during a preSort().
    //---------------------------------------------------------
    private void doSort() {
        EventNoteData ndNoteData1, ndNoteData2;
        LocalDate d1, d2;

        boolean doSwap;
        int items = vectGroupData.size();

        AppUtil.localDebug(true);

        MemoryBank.debug("EventNoteGroup.doSort - Number of items in list: " + items);
        MemoryBank.debug("  ASCENDING start dates, Events without dates at BOTTOM");

        for (int i = 0; i < (items - 1); i++) {
            ndNoteData1 = (EventNoteData) vectGroupData.elementAt(i);
            d1 = ndNoteData1.getStartDate();
            for (int j = i + 1; j < items; j++) {
                doSwap = false;
                ndNoteData2 = (EventNoteData) vectGroupData.elementAt(j);
                d2 = ndNoteData2.getStartDate();

                if ((d1 == null) || ((d2 != null) && d1.isAfter(d2))) doSwap = true;

                if (doSwap) {
                    MemoryBank.debug("  Moving Vector element " + i + " below " + j + "  (zero-based)");
                    vectGroupData.setElementAt(ndNoteData2, i);
                    vectGroupData.setElementAt(ndNoteData1, j);
                    d1 = d2;
                    ndNoteData1 = ndNoteData2;
                } // end if
            } // end for j
        } // end for i

        postSort();

        AppUtil.localDebug(false);
    } // end doSort


    @Override
    protected boolean editExtendedNoteComponent(NoteData noteData) {
        // Show the ExtendedNoteComponent (EventEditorPanel)
        if(extendedNoteComponent == null) {
            extendedNoteComponent = new EventEditorPanel("Upcoming Event");
        }

        // Cast the input parameter to its full potential.
        EventNoteData eventNoteData = (EventNoteData) noteData;

        // Send the current data to the Event Editor dialog.
        ((EventEditorPanel) extendedNoteComponent).showTheData(eventNoteData);

        int doit = optionPane.showConfirmDialog(
                JOptionPane.getFrameForComponent(this),
                extendedNoteComponent,
                noteData.getNoteString(),
                JOptionPane.OK_CANCEL_OPTION);

        if (doit == -1) return false; // The X on the dialog
        if (doit == JOptionPane.CANCEL_OPTION) return false;

        // Get the data from the Event Editor dialog.
        ((EventEditorPanel) extendedNoteComponent).assimilateTheData(eventNoteData);

        // We don't know for sure that something changed, but since the edit was not cancelled
        // we will assume that there were changes, and indicate that a save group is needed.
        setGroupChanged();

        return true;
    } // end editExtendedNoteComponent


    public AppIcon getDefaultIcon() {
        return defaultIcon;
    }


    // -------------------------------------------------------------------
    // Method Name: getGroupFilename
    //
    // This method returns the name of the file where the data for this
    //   group of notes is loaded / saved.
    // -------------------------------------------------------------------
    public String getGroupFilename() {
        return MemoryBank.userDataHome + File.separatorChar + "UpcomingEvents.json";
    }// end getGroupFilename


    // -------------------------------------------------------------------
    // Method Name: makeNewNote
    //
    // -------------------------------------------------------------------
    @Override
    JComponent makeNewNote(int i) {
        EventNoteComponent newNote = new EventNoteComponent(this, i);
        newNote.setVisible(false);
        return newNote;
    } // end makeNewNote


    //----------------------------------------------------------------------
    // Method Name: refresh
    //
    // Called during construction and also in response to a click on the
    //   'Refresh' menu item.  When an event is to be removed from the
    //   interface due to 'aging', it should be done by first saving the
    //   data and then reloading it.
    //----------------------------------------------------------------------
    @Override
    public void refresh() {
        preClose();     // Save changes
        updateGroup();

        // Call 'ageEvents'
        if (ageEvents()) {
            preClose();    // Save the new states of 'aged' events.
            updateGroup(); // Reload the group (visually removes aged-off items, if any)
        } // end if

        doSort();  // This action could change the current selection.

        // So - unselect, if not already.
        showComponent(null, false);
    } // end refresh


    // ----------------------------------------------------
    // Method Name: setDefaultIcon
    //
    // Called by the EventNoteComponent's
    // popup menu handler for 'Set As Default'.
    // ----------------------------------------------------
    public void setDefaultIcon(AppIcon li) {
        defaultIcon = li;
        eventNoteDefaults.defaultIconFileName = li.getDescription();
        eventNoteDefaults.save();
        setGroupChanged();
        preClose();
        updateGroup();
    }// end setDefaultIcon


    @Override
    void setGroupData(Object[] theGroup) {
        NoteData.loading = true; // We don't want to affect the lastModDates!
        vectGroupData = AppUtil.mapper.convertValue(theGroup[0], new TypeReference<Vector<EventNoteData>>() { });
        NoteData.loading = false; // Restore normal lastModDate updating.
    }

    // Used by test methods
    public void setNotifier(Notifier newNotifier) {
        optionPane = newNotifier;
    }

    //--------------------------------------------------------------
    // Method Name: showComponent
    //
    //  Several actions needed when a line has
    //    either gone active or inactive.
    //--------------------------------------------------------------
    void showComponent(EventNoteComponent nc, boolean b) {
        if (b) {
            eNoteComponent = nc;
            EventNoteData end = (EventNoteData) nc.getNoteData();
            if (end == null) return;

            // System.out.println("Event id: [" + end.getEventId() + "]");

            // Show the previously selected date
            if (end.getStartDate() != null) {
                tmc.setChoice(end.getStartDate());
            } // end if

            // Show the summary info.
            theHeader.setEventSummary(end.getSummary());
        } else {
            eNoteComponent = null;
            tmc.setChoice(null);
            theHeader.setEventSummary("Select an Event to display");
        } // end if
    } // end showComponent


}// end class EventNoteGroup



