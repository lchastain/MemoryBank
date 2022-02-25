/*  Manage notes on upcoming events.

 */

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Vector;

@SuppressWarnings({"unchecked", "rawtypes"})
public class EventNoteGroupPanel extends NoteGroupPanel implements IconKeeper, DateSelection {
    private static final Logger log = LoggerFactory.getLogger(EventNoteGroupPanel.class);

    // Notes on the implemented interfaces:
    //---------------------------------------------------------------------
    // DateSelection - method is dateSelected, to respond to TMC clicks.
    // IconKeeper - method is setDefaultIcon.

    // Because the parent NoteGroup class is where all NoteComponents get
    //   made and that constructor runs before the one here, the defaultIcon
    //   (as seen in our EventNoteComponents) MUST be present BEFORE the
    //   NoteGroup constructor is called.  This is why we need to
    //   assign it from the static section of this class.  The defaultIcon
    //   filename comes from the eventNoteDefaults.
    //------------------------------------------------------------------
    private static final EventNoteDefaults eventNoteDefaults;
    private static ImageIcon defaultIcon;

    private final ThreeMonthColumn tmc;
    private final EventHeader theHeader;
    private EventNoteComponent eventNoteComponent;

    static {
        eventNoteDefaults = EventNoteDefaults.load();

        if (eventNoteDefaults.defaultIconFileName.equals("")) {
            MemoryBank.debug("Default EventNoteComponent Icon: <blank>");
            defaultIcon = new ImageIcon();
        } else {
            MemoryBank.debug("Default EventNoteComponent Icon: " + eventNoteDefaults.defaultIconFileName);
            defaultIcon = new ImageIcon(eventNoteDefaults.defaultIconFileName);
           IconInfo.scaleIcon(defaultIcon);
        } // end if/else
    } // end static section


    // Only Archived event groups come directly here.  Normal access comes via the other constructor,
    //   which will age and sort.
    public EventNoteGroupPanel(GroupInfo groupInfo) {
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty GoalGroup.
        myNoteGroup.myNoteGroupPanel = this;
        loadNotesPanel();

        tmc = new ThreeMonthColumn();
        tmc.setSubscriber(this);

        theHeader = new EventHeader(this);
        add(theHeader, BorderLayout.NORTH);

        // Wrapped tmc in a FlowLayout panel, to prevent stretching.
        JPanel pnl1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnl1.add(tmc);
        add(pnl1, BorderLayout.EAST);

        theNotePager.reset(1);

        if(groupInfo.archiveName != null) setEditable(false); // Archived groups are non-editable
    }


    // This constructor will load the group if it exists in 'current' data, otherwise
    // you get a new one with that group name.
    EventNoteGroupPanel(String groupName) {
        this(new GroupInfo(groupName, GroupType.EVENTS));

        // Call 'ageEvents'
        if (ageEvents()) { // This indicates that one or more items was date-adjusted and/or
            // removed.  We show that by saving the altered data and then reloading it.
            preClosePanel();    // Save the new states of 'aged' events.
            updateGroup(); // Reload the group (visually removes aged-off items, if any)
        } // end if
        doSort(); // needed whether events were aged off, or not.
    }// end constructor


    // This method examines each event and if its start is after the
    //   current date/time, will 'age' it according to its recurrence.
    //   If there is no recurrence or the final occurrence is exceeded,
    //   the event is 'aged off'.
    // The return value will be true if one or more events were aged
    //   off.  If they were not aged at all, or only aged forward, the
    //   return value is false.
    private boolean ageEvents() {
        boolean blnDropThisEvent;
        boolean blnAnEventWasAged = false;

        String s;
        EventNoteData tempNoteData;

        // AppUtil.localDebug(true);
        for (Object ndTmp : myNoteGroup.noteGroupDataVector) {
            blnDropThisEvent = false;
            tempNoteData = (EventNoteData) ndTmp;

            s = tempNoteData.getNoteString();
            MemoryBank.debug("Examining: " + s + " starting " + tempNoteData.getEventStartDateTime());

            // 'Age' the event, if appropriate to do so.
            while (tempNoteData.hasStarted()) { // If it has started then we definitely have a start date.
                if (tempNoteData.getRetainNote()) { // We save this version of the event.
                    DayNoteData dnd = new DayNoteData(tempNoteData);
                    LocalDate eventStartDate = tempNoteData.getStartDate();
                    String dayName = CalendarNoteGroup.getGroupNameForDate(eventStartDate, GroupType.DAY_NOTES);
                    GroupInfo dayGroupInfo = new GroupInfo(dayName, GroupType.DAY_NOTES);
                    // Get the right group even if it is already open, with unsaved changes -
                    NoteGroup dayNoteGroup = dayGroupInfo.getNoteGroup();
                    dayNoteGroup.appendNote(dnd);
                    dayNoteGroup.saveNoteGroup();

                    // I don't see any Exception handling going on here - what gives?
                    // This aging event is one of potentially several in the group, and one of the times that this
                    //   could occur is at app startup while the 'always on top' splash screen is displayed.  It is
                    //   definitely NOT a good time to stop at each problem and complain to the user with an error
                    //   dialog that they must review and dismiss.  Also, there is not a lot that could go wrong.
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
        if (blnAnEventWasAged) setGroupChanged(true);
        // AppUtil.localDebug(false);
        return blnAnEventWasAged;
    } // end ageEvents


    private File chooseMergeFile() {
        ArrayList<String> groupNames = myNoteGroup.getGroupNames();
        groupNames.remove(getGroupName()); // Remove ourselves from consideration.

        // Convert to an Object array so the JOptionPane static method can present a selection list.
        Object[] theNames = new String[groupNames.size()];
        theNames = groupNames.toArray(theNames);

        String message = "Choose an Event group to merge with " + getGroupName();
        String title = "Merge Event Groups";
        // Important issue here!  The selection list is presented as an initially closed combobox dropdown with
        //   either your initial selection or the first choice selected.  BUT if the number of choices is more
        //   than 20, there is no dropdown control; you see a scrollable list instead, with no preselected choice.
        //   This is built-in Swing behavior, not my doing.
        Object theChoice = optionPane.showInputDialog(theBasePanel, message,
                title, JOptionPane.PLAIN_MESSAGE, null, theNames, null);

        System.out.println("The choice is: " + theChoice);
        if (theChoice == null) return null;
        return new File(NoteGroupFile.todoListGroupAreaPath + "todo_" + theChoice + ".json");
    } // end chooseMergeFile


    // Interface to the Three Month Calendar; called by the tmc.
    @Override // Implementation of the DateSelection interface
    public void dateSelected(LocalDate selectedDate) {
        if(getEditable()) {
            if (eventNoteComponent == null) {
                String s;
                s = "You must select an Event before a Start date can be set!";
                setStatusMessage(s);
                tmc.setChoice(null);
                return;
            } // end if

            // Ignore TMC selections for non-editable events.
            if (!eventNoteComponent.noteTextField.isEditable()) {
                tmc.setChoice(null);
                return;
            }

            EventNoteData end = (EventNoteData) (eventNoteComponent.getNoteData());

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
            eventNoteComponent.setNoteData(end);
            theHeader.setEventSummary(end.getSummary());
        }
    } // end dateSelected


    // Sorting is done ascending only, with unsortables placed at the 'bottom' of the list.
    void doSort() {
        EventNoteData ndNoteData1, ndNoteData2;
        LocalDate d1, d2;

        boolean doSwap;
        int items = myNoteGroup.noteGroupDataVector.size();

        AppUtil.localDebug(true);

        MemoryBank.debug("EventNoteGroup.doSort - Number of items in list: " + items);
        MemoryBank.debug("  ASCENDING start dates, Events without dates at BOTTOM");

        for (int i = 0; i < (items - 1); i++) {
            ndNoteData1 = (EventNoteData) myNoteGroup.noteGroupDataVector.elementAt(i);
            d1 = ndNoteData1.getStartDate();
            for (int j = i + 1; j < items; j++) {
                doSwap = false;
                ndNoteData2 = (EventNoteData) myNoteGroup.noteGroupDataVector.elementAt(j);
                d2 = ndNoteData2.getStartDate();

                if ((d1 == null) || ((d2 != null) && d1.isAfter(d2))) doSwap = true;

                if (doSwap) {
                    MemoryBank.debug("  Moving Vector element " + i + " below " + j + "  (zero-based)");
                    myNoteGroup.noteGroupDataVector.setElementAt(ndNoteData2, i);
                    myNoteGroup.noteGroupDataVector.setElementAt(ndNoteData1, j);
                    d1 = d2;
                    ndNoteData1 = ndNoteData2;
                } // end if
            } // end for j
        } // end for i

        loadPage(theNotePager.getCurrentPage());
        AppUtil.localDebug(false);
    } // end doSort


    @Override
    // Is it possible that this does not need to be overridden?  Just set tne extendedNoteComponent in the constructor
    //   and let the base class call the 'regular' edit method.  ??  Cannot research this now; later.
    public boolean editExtendedNoteComponent(NoteData noteData) {
        // Show the ExtendedNoteComponent (EventEditorPanel)
        if(extendedNoteComponent == null) {
            extendedNoteComponent = new EventEditorPanel("Upcoming Event");
        }

        // Cast the input parameter to its full potential.
        EventNoteData eventNoteData = (EventNoteData) noteData;

        // Send the current data to the Event Editor dialog.
        ((EventEditorPanel) extendedNoteComponent).showTheData(eventNoteData);

        int doit = optionPane.showConfirmDialog(
                JOptionPane.getFrameForComponent(theBasePanel),
                extendedNoteComponent,
                noteData.getNoteString(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (doit == -1) return false; // The X on the dialog
        if (doit == JOptionPane.CANCEL_OPTION) return false;

        // Get the data from the Event Editor dialog.
        ((EventEditorPanel) extendedNoteComponent).assimilateTheData(eventNoteData);

        // We don't know for sure that something changed, but since the edit was not cancelled
        // we will assume that there were changes, and indicate that a save group is needed.
        setGroupChanged(true);

        return true;
    } // end editExtendedNoteComponent


    public ImageIcon getDefaultIcon() {
        return defaultIcon;
    }


    ThreeMonthColumn getThreeMonthColumn() {
        return tmc;
    }

    @Override
    JComponent makeNewNote(int i) {
        EventNoteComponent newNote = new EventNoteComponent(this, i);
        newNote.setVisible(false);
        return newNote;
    } // end makeNewNote


    @SuppressWarnings({"unchecked"})
    public void merge() {
        File mergeFile = chooseMergeFile();
        if (mergeFile == null) return;

        // Load the file to merge in -
        Object[] theGroup = NoteGroupFile.loadFileData(mergeFile);
        //System.out.println("Merging NoteGroup data from JSON file: " + AppUtil.toJsonString(theGroup));

        Vector<EventNoteData> mergeVector = AppUtil.mapper.convertValue(theGroup[theGroup.length - 1], new TypeReference<Vector<EventNoteData>>() {  });

        // Create a 'set', to contain only unique items from both lists.
        LinkedHashSet<EventNoteData> theUniqueSet = new LinkedHashSet<EventNoteData>(myNoteGroup.noteGroupDataVector);
        theUniqueSet.addAll(mergeVector);

        // Make a new Vector from the unique set, and set our group data to the new merged data vector.
        Vector mergedVector = new Vector<>(theUniqueSet);
        showGroupData(mergedVector);
        setGroupChanged(true);
    } // end merge

    //----------------------------------------------------------------------
    // Method Name: refresh
    //
    // Called in response to a click on the 'Save' menu item.
    //----------------------------------------------------------------------
    @Override
    public void refresh() {
        preClosePanel();     // Save changes
        updateGroup();

        // Call 'ageEvents'
        if (ageEvents()) { // This indicates that one or more items was date-adjusted and/or
            // removed.  We show that by saving the altered data and then reloading it.
            preClosePanel();    // Save the new states of 'aged' events.
            updateGroup(); // Reload the group (visually removes aged-off items, if any)
        } // end if

        doSort();  // This action could change the current selection  -
        showComponent(null, false); // so unselect, if not already.
    } // end refresh


    // Called from the menu bar:  AppTreePanel.handleMenuBar() --> saveGroupAs() --> saveAs()
    // Prompts the user for a new list name, checks it for validity,
    // then if ok, saves the file with that name.
    boolean saveAs() {
        Frame theFrame = JOptionPane.getFrameForComponent(theBasePanel);

        String thePrompt = "Please enter the new Event group name";
        int q = JOptionPane.QUESTION_MESSAGE;
        String newName = optionPane.showInputDialog(theFrame, thePrompt, "Save As", q);

        // The user cancelled; return with no complaint.
        if (newName == null) return false;

        newName = newName.trim(); // eliminate outer space.

        // Test new name validity.
        String theComplaint = myNoteGroup.groupDataAccessor.getObjectionToName(newName);
        if (!theComplaint.isEmpty()) {
            optionPane.showMessageDialog(theFrame, theComplaint,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Get the current list name -
        String oldName = getGroupName();

        // If the new name equals the old name, just do the save as the user
        //   has asked and don't tell them that they are an idiot.  But no
        //   other actions in the datastore or the tree will be taken.
        if (newName.equals(oldName)) {
            preClosePanel();
            return false;
        } // end if

        // Check to see if the destination NoteGroup already exists.
        // If so then complain and refuse to do the saveAs.

        // Other applications might offer the option of overwriting the existing data.  This was considered
        // and rejected because of the possibility of overwriting data that is currently being shown in
        // another panel.  We could check for that as well, but decided not to because - why should we go to
        // heroic efforts to handle a user request where it seems like they may not understand what it is
        // that they are asking for?  This is the same approach that was taken in the 'rename' handling.
        ArrayList<String> groupNames = myNoteGroup.getGroupNames();
        if(groupNames.contains(newName)) {
            ems = "An Event NoteGroup named " + newName + " already exists!\n";
            ems += "  operation cancelled.";
            optionPane.showMessageDialog(theFrame, ems,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // After we refuse to do the operation due to a preexisting destination NoteGroup with the same name,
        // the user has several recourses, depending on what it was they really wanted to do - they could
        // delete the preexisting NoteGroup or rename it, after which a second attempt at this operation
        // would succeed, or they could realize that they had been having a senior moment and abandon the
        // effort, or they could choose a different new name and try again.

        // So we got past the pre-existence check. Now change the name, update Properties and the data accessor.
        log.debug("Saving " + oldName + " as " + newName);
        GroupProperties myGroupProperties = myNoteGroup.getGroupProperties();

        // 'setGroupName' sets the name of the group, which translates into an
        // in-place change of the name of the list held by the EventListKeeper.
        // Unfortunately, that list will still have the old title, so it still needs
        // to be removed from the keeper.  The calling context (AppTreePanel) must take care of that.
        myGroupProperties.setGroupName(newName);
        GroupInfo myGroupInfo = new GroupInfo(myGroupProperties);

        // The data accessor (constructed along with this Panel) has the old name; need to update.
        myNoteGroup.groupDataAccessor = MemoryBank.dataAccessor.getNoteGroupDataAccessor(myGroupInfo);

        // Save
        setGroupChanged(true);
        preClosePanel();

        return true;
    } // end saveAs

    // ----------------------------------------------------
    // Method Name: setDefaultIcon
    //
    // Called by the EventNoteComponent's
    // popup menu handler for 'Set As Default'.
    // ----------------------------------------------------
    public void setDefaultIcon(ImageIcon li) {
        defaultIcon = li;
        eventNoteDefaults.defaultIconFileName = li.getDescription();
        eventNoteDefaults.save();
        setGroupChanged(true);
        preClosePanel();
        updateGroup();
    }// end setDefaultIcon


    //  Several actions needed when a line has
    //    either gone active or inactive.
    void showComponent(EventNoteComponent nc, boolean b) {
        if (b) {
            eventNoteComponent = nc;
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
            eventNoteComponent = null;
            tmc.setChoice(null);
            theHeader.setEventSummary("Select an Event to display");
        } // end if
    } // end showComponent


}// end class EventNoteGroup



