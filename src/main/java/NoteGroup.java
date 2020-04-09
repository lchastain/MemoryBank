import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;


@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class NoteGroup extends FileGroup implements NoteComponentManager, NoteSelection {
    //=============================================================
    // Members that child classes may access directly
    //=============================================================
    static Notifier optionPane;
    static String ems;     // Error Message String
    JPanel theBasePanel;
    ExtendedNoteComponent extendedNoteComponent;
    JMenu myListMenu; // Child classes each have their own menu

    boolean addNoteAllowed;
    boolean saveWithoutData;
    int lastVisibleNoteIndex = 0;
    int pageSize;

    // Child classes that check this variable should test for a
    //   value >= SUCCEEDED; if true, the count of records written
    //   may be obtained by: (intSaveGroupStatus - SUCCEEDED)
    int intSaveGroupStatus = INITIAL;

    // Container for graphical members - limited to pageSize
    protected JPanel groupNotesListPanel;

    // Container for a paging control
    NotePager theNotePager;

    // Container for the (complete collection of) Group data objects.
    // It may hold more than the PAGE_SIZE number of visible notes.
    Vector groupDataVector;

    // Private members
    //-------------------------------------------------------------
    private String defaultSubject;
    static final int PAGE_SIZE = 40;

    private int intHighestNoteComponentIndex;

    private JScrollPane jsp;

    // The Information/Status panel of the frame.
    private JLabel lblStatusMessage;

    //-------------------------------------------------------------

    NoteGroup() {
        this(null, GroupProperties.GroupType.UNKNOWN, PAGE_SIZE);
    } // end constructor 1

    NoteGroup(String groupName, GroupProperties.GroupType groupType, int intPageSize) {
        super(groupName, groupType);
        theBasePanel = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;
            //--------------------------------------------------------------------
            // Method Name: getPreferredSize
            //
            // This preference is necessary to set a limit of a maximum height value.
            //   After we add content to the scrollable area of this panel, if the
            //   height of that content exceeds the limit that we set here then
            //   the vertical scrollbar will kick in.
            //--------------------------------------------------------------------
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, 400);
            } // end getPreferredSize
        };
        pageSize = intPageSize;
        addNoteAllowed = true;
        saveWithoutData = false;
        intHighestNoteComponentIndex = pageSize - 1;

        groupDataVector = new Vector<>();
        jsp = new JScrollPane() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, 400);
            } // end getPreferredSize
        };
        JScrollBar jsb = new JScrollBar();

        // This is necessary because otherwise, once the bar appears
        //  the tab and up/down keys can transfer focus over to here,
        //  and the up/down keys cannot get it back.
        jsb.setFocusable(false);

        jsb.setUnitIncrement(NoteComponent.NOTEHEIGHT);
        jsb.setBlockIncrement(NoteComponent.NOTEHEIGHT);
        jsp.setVerticalScrollBar(jsb);

        int borderWidth = 2;
        theBasePanel.setBorder(BorderFactory.createLineBorder(Color.black, borderWidth));

        groupNotesListPanel = new JPanel();
        groupNotesListPanel.setLayout(
                new BoxLayout(groupNotesListPanel, BoxLayout.Y_AXIS));

        // Make the paging control.  It is up to the various child classes
        //   to display it to the user by adding it to their interface.
        theNotePager = new NotePager(this);

        // The 'makeNewNote' methodology works for ALL child groups regardless
        //   of the Type of new note that they make, because we do not
        //   actually put the result into a typed variable; it goes directly
        //   into the container.  Every Group method that accesses the
        //   container contents will either be overridden and cast the
        //   note to the correect type, or it will call a method common
        //   to all JComponents, such as 'setVisible'.
        for (int i = 0; i <= intHighestNoteComponentIndex; i++) {
            groupNotesListPanel.add(makeNewNote(i));
        } // end for

        // The first note should not be invisible.
        groupNotesListPanel.getComponent(0).setVisible(true);

        theBasePanel.add(jsp, BorderLayout.CENTER);
        jsp.setViewportView(groupNotesListPanel);

        String s = "Use the mouse pointer for context-sensitive help";
        lblStatusMessage = new JLabel(s);
        theBasePanel.add(lblStatusMessage, BorderLayout.SOUTH);

        // A variant of JOptionPane, for testing.
        optionPane = new Notifier() { }; // Uses all default methods.

        // Cannot do the updateGroup() here because child classes
        //   first need to establish their Filenames in their own
        //   constructors.
    } // end constructor 2


    //----------------------------------------------------------------------
    // Method Name: activateNextNote
    //
    // This method will set the next note visible, unless:
    //   the requested index is already visible   OR
    //   there are no more hidden notes to show, in which case it will
    //   create a new page.
    // It is called either from NoteComponent.initialize() or loadInterface().
    //----------------------------------------------------------------------
    public void activateNextNote(int noteIndex) {
        if ((noteIndex >= 0) && (noteIndex < lastVisibleNoteIndex)) return;  // already showing.

        // noteIndex is -1 when we have come here from loadInterface, where the displayed page is empty.
        if(noteIndex >= 0) {
            // Get the component for the indicated noteIndex (the note we're 'coming from').
            NoteComponent thisNote = (NoteComponent) groupNotesListPanel.getComponent(noteIndex);

            // If the note we're coming from has not been initialized, we shouldn't activate the next one.
            // How could this happen, you ask?  Well, it happens when a note has been added to a
            // page that we've paged away from and now we've come back to that page and
            // the loadInterface method is trying to correctly set the lastVisibleNoteIndex.
            // It considers the last note on the page (our new but uninitialized one) and then assumes
            // (incorrectly, in this case) that it needs to go one better.
            if (!thisNote.initialized) return;
        }

        if (lastVisibleNoteIndex < intHighestNoteComponentIndex) {
            lastVisibleNoteIndex++;
            NoteComponent nc;
            nc = (NoteComponent) groupNotesListPanel.getComponent(lastVisibleNoteIndex);
            nc.setVisible(true);
        } else {
            // Implement a page rollover.
            int tmpPage = theNotePager.getCurrentPage();
            if (tmpPage > 0) { // This should not be done before the first pager reset.
                if (tmpPage == theNotePager.getHighestPage()) {
                    // Capture changes from the current page.  This will be needed if this method is being called
                    // from initialize() after a note has been added into the last available slot on the current page.
                    // The new note(s) will need to be added to the groupDataVector now, so that the pager reset can
                    // see that the total page count should be increased (by one).
                    if(groupChanged) unloadInterface(tmpPage);

                    theNotePager.reset(tmpPage);
                } // end if
            } // end if

        } // end if
    } // end activateNextNote


    void add(JComponent component, Object object) {
        theBasePanel.add(component, object);
    }

    //----------------------------------------------------------------
    // Method Name: clearGroup
    //
    // Clear all data (which may span more than one page) and the interface.
    //----------------------------------------------------------------
    void clearGroup() {
        theBasePanel.transferFocusUpCycle(); // Otherwise can get unwanted focus events.
        clearPage();
        groupDataVector.clear();
        showGroupData(groupDataVector);
        setGroupChanged(true);
        theNotePager.reset(1);
    } // end clearGroup


    //----------------------------------------------------------------
    // Method Name: clearPage
    //
    // Clear data from all Notes that are showing in the interface.
    // This resets any components that are displayed with info,
    //  as well as their underlying data objects.  It does not
    //  remove components or the data object itself; just clears them.
    //----------------------------------------------------------------
    private void clearPage() {
        if (intHighestNoteComponentIndex < 0) return; // an 'empty' group

        for (int i = 0; i <= lastVisibleNoteIndex; i++) {
            //System.out.println("Getting component " + i);
            NoteComponent tempNote = (NoteComponent) groupNotesListPanel.getComponent(i);

            // The 'clear' method that is called below is overridden by child classes
            // so that they can first clear their own components.  After that, they still
            // call super.clear() which will clear the parent component and then call the
            // data-clearing method (also overridden, also calls its super).
            if (tempNote.initialized)
                tempNote.clear();  // The base NoteComponent clear method sets initialized to false.

        } // end for
        lastVisibleNoteIndex = -1; // This helps, when going to save (delete) an associated file.
    } // end clearPage


    //---------------------------------------------------------------
    // Method Name: editExtendedNoteComponent
    //
    // Provides an interface for the modification of data elements of
    //   the extended note component, and returns true if there
    //   was a change; false otherwise.
    //
    //---------------------------------------------------------------
    public boolean editExtendedNoteComponent(NoteData noteData) {
        // System.out.println("NoteGroup editExtendedNoteComponent");

        // Load the enc with the correct data
        if (extendedNoteComponent == null) {
            extendedNoteComponent = new ExtendedNoteComponent(defaultSubject);
        }

        extendedNoteComponent.setExtText(noteData.getExtendedNoteString());
        if (defaultSubject != null) extendedNoteComponent.setSubject(noteData.getSubjectString());

        // Preserve initial values, for later comparison to
        //   determine if there was a change.
        String origSubject = noteData.getSubjectString();
        String origExtendedString = noteData.getExtendedNoteString();

        //---------------------------------------------------------
        // Present the ExtendedNoteComponent in a dialog
        //---------------------------------------------------------
        int doit = JOptionPane.showConfirmDialog(
                JOptionPane.getFrameForComponent(theBasePanel),
                extendedNoteComponent,
                noteData.getNoteString(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (doit == -1) return false; // The X on the dialog
        if (doit == JOptionPane.CANCEL_OPTION) return false;

        // Collect results of the editing -
        //------------------------------------------------------------------

        // Get the Subject
        extendedNoteComponent.updateSubject(); // This moves the subject from the combobox into the component data
        String newSubject = extendedNoteComponent.getSubject(); // This gets the component data
        // We need to be able to save a 'None' subject (ie, ""), and recall it,
        //   which is different than if you never set one in the
        //   first place, in which case you would get the default.  So -
        //   we accept the newSubject above without checking its content.

        // Get the Extended text
        String newExtendedString = extendedNoteComponent.getExtText();

        boolean aChangeWasMade = false;
        if (newSubject != null) {
            // Cannot simplify the logic here; either new or old could be null, which is an allowed value.
            if (origSubject == null) aChangeWasMade = true;
            else if (!newSubject.equals(origSubject)) aChangeWasMade = true;
        } // end if
        if (!newExtendedString.equals(origExtendedString)) aChangeWasMade = true;

        if (aChangeWasMade) {
            noteData.setExtendedNoteString(newExtendedString);
            noteData.setSubjectString(newSubject);
        } // end if

        //------------------------------------------------------------------

        return aChangeWasMade;
    } // end editExtendedNoteComponent


    int getHighestNoteComponentIndex() {
        return intHighestNoteComponentIndex;
    }

    public int getLastVisibleNoteIndex() {
        return lastVisibleNoteIndex;
    }

    //--------------------------------------------------------
    // Method Name: getNoteComponent
    //
    // Returns a NoteComponent that can be used to manipulate
    // component state as well as set/get underlying data.
    //--------------------------------------------------------
    public NoteComponent getNoteComponent(int i) {
        return (NoteComponent) groupNotesListPanel.getComponent(i);
    } // end getNoteComponent


    //--------------------------------------------------------------------
    // Method Name: getProperties
    //
    //  Called by saveGroup - child classes may override and return
    //    an actual Object; otherwise returns null.
    //--------------------------------------------------------------------
    protected Object getProperties() {
        return null;
    } // end getProperties


    //----------------------------------------------------------------
    // Method Name: setGroupChanged
    //
    // Called by all contexts that make a change to the data, each
    //   time a change is made.  Child classes can override if they
    //   need to intercept a state change, but in that case they
    //   should still call this super method so that group saving
    //   is done when needed and menu items are managed correctly.
    //----------------------------------------------------------------
    @Override
    public void setGroupChanged(boolean b) {
        super.setGroupChanged(b);
        adjustMenuItems(b);
    } // end setGroupChanged

    // Learned how to do this (convert an ArrayList element that is a LinkedHashMap, to a Vector of NoteData),
    // from: https://stackoverflow.com/questions/15430715/casting-linkedhashmap-to-complex-object
    // Previously, I just cycled thru the LinkedHashMap by accepting the entries as Object, then converted them
    // to JSON string, then parsed the string back in to a NoteData and added it to a new Vector.  But that was
    // a several-line method; this conversion is a one-liner, and my version had the possibility of throwing an
    // Exception that needed to be caught.
    void setGroupData(Object[] theGroup) {
        BaseData.loading = true; // We don't want to affect the lastModDates!
        groupDataVector = AppUtil.mapper.convertValue(theGroup[0], new TypeReference<Vector<NoteData>>() {  });
        BaseData.loading = false; // Restore normal lastModDate updating.
    }

    // Provides a way to set the displayed data, vs loading it from a file.
    void showGroupData(Vector<NoteData> newGroupData) {
        groupDataVector = newGroupData;
        loadInterface(1);
    }

    //----------------------------------------------------------------
    // Method Name: loadGroup
    //
    // The data file will be reloaded whenever this method is called.
    //----------------------------------------------------------------

    // This method is needed to separate the load of the data
    //   from the various components that act upon it.
    //   Internally it comes in as an ArrayList of one or
    //   two Objects that identify themselves as LinkedHashMaps
    //   (although I never said that they should be).  Before
    //   we leave here, though, we call setGroupData (overridden
    //   by most children) that loads the data into a Vector
    //   of Notes and the requesting group's properties, if any.
    //--------------------------------------------------------------------


    private void loadNoteGroup() {
        groupDataVector.clear(); // Clear before loading
        lastVisibleNoteIndex = 0;

        // This string is set now, just prior to loading the group
        //   rather than earlier, because in some child classes the
        //   group will have changed so that the file to load is not the
        //   same as it was at group construction; the filename for the
        //   group may be highly variable, and this method may be getting
        //   called after each filename change, even though the panel
        //   components that hold and show the file data remain in place.
        String groupFilename = getGroupFilename();

        if (groupFilename.isEmpty()) {
            MemoryBank.debug("No filename is set for: " + this.getClass().getName());
        } else {
            MemoryBank.debug("NoteGroup file name is: " + groupFilename);
        }
        Object[] theGroup = FileGroup.loadFileData(groupFilename);
        if (theGroup != null) {
            int notesLoaded = ((List) theGroup[theGroup.length - 1]).size();
            MemoryBank.debug("Data file found; loaded " + notesLoaded + " items.");
            //System.out.println("NoteGroup data from JSON file: " + AppUtil.toJsonString(theGroup));
            setGroupData(theGroup);
        } else {
            MemoryBank.debug("No data file exists.");
            // Setting the name to 'empty' IS needed; it is examined when
            //   saving and if non-empty, the old file is deleted first.  Of
            //   course, if the file already does not exist, we don't want
            //   to let it try to do that.
//            setGroupFilename("");
        }

        Exception e = null;
        try {
            loadInterface(1); // Always load page 1
        } catch (ClassCastException cce) {
            e = cce;
        } // end try/catch

        if (e != null) {
            ems = "NoteGroup loadGroup: Error in loading " + groupFilename + " !\n";
            ems = ems + e.toString();
            ems = ems + "\nData file load operation aborted.";
            System.out.println("ems = " + ems);
            optionPane.showMessageDialog(JOptionPane.getFrameForComponent(theBasePanel),
                    ems, "Error", JOptionPane.ERROR_MESSAGE);
        } // end if
    } // end loadGroup


    //-------------------------------------------------------------------
    // Method Name: loadInterface
    //
    // This method transfers the data vector items to the onscreen
    //   components.  A 'page' specifier is used to determine which
    //   portion of the vector to display, if there are more notes
    //   than will fit on one page.  Child classes are responsible for
    //   displaying the pager control and sending page numbers higher
    //   than one.  If they do not, then only
    //   the first page of data will be available.
    //-------------------------------------------------------------------
    void loadInterface(int intPageNum) {
        //AppUtil.localDebug(true);
        boolean currentChangedState = getGroupChanged();

        // Set the indexes into the data vector -
        int maxDataIndex = groupDataVector.size() - 1;
        int dataIndex = (intPageNum - 1) * pageSize;
        MemoryBank.debug("NoteGroup.loadInterface starting at vector data index " + dataIndex);

        lastVisibleNoteIndex = -1;
        NoteComponent tempNoteComponent;
        for (int panelIndex = 0; panelIndex < pageSize; panelIndex++) {
            // The next line casts to NoteComponent.  Since the component is actually
            //   a child of NoteComponent, the 'setNoteData' method that is called
            //   later will be the child class method that is an override of the one
            //   in NoteComponent.  That behavior is critical to this operation.
            tempNoteComponent = (NoteComponent) groupNotesListPanel.getComponent(panelIndex);

            if (dataIndex <= maxDataIndex) { // Put vector data into the interface.
                MemoryBank.debug("  loading panel index " + panelIndex + " with data element " + dataIndex);
                tempNoteComponent.setNoteData((NoteData)groupDataVector.elementAt(dataIndex)); // sets initialized to true
                tempNoteComponent.setVisible(true);
                lastVisibleNoteIndex = panelIndex;
                dataIndex++;
            } else {  // This path is needed to wipe the rest of the interface clean.
                // These lines must be cleared visually, since they extend beyond the end of the data vector.

                // Clear the visual aspects for all the other remaining notes on this page.
                // We use 'makeDataObject()' below rather than clear(), because that also clears
                // the data object which is in the groupDataVector and that data object may have
                // been set by a reference from a full page three when you are now clearing the rest of a partial
                // final page four.  Instead, we first give the noteComponent a new data object,
                // then instruct it to update its appearance based on the new data and go back
                // to being 'un' initialized.
                MemoryBank.debug("  clearing panel index " + panelIndex);

                // These three lines are an effective 'clear' of the component, without using the
                // reference that could point back to 'good' data that is elswhere in the vector.
                tempNoteComponent.makeDataObject();
                tempNoteComponent.resetComponent();
                tempNoteComponent.initialized = false;
                tempNoteComponent.setVisible(false);
            }
        } // end for

        if (addNoteAllowed) activateNextNote(lastVisibleNoteIndex);
        MemoryBank.debug("lastVisibleNoteIndex is " + lastVisibleNoteIndex);

        // Each of the 'setNoteData' calls above would have set this to true, but this method may
        // have been called simply to handle a paging event; no real change to the data.  So -
        // having preserved the original value, we now set it back to that.
        setGroupChanged(currentChangedState);

        //AppUtil.localDebug(false);
    } // end loadInterface


    //-------------------------------------------------------------------
    // Method Name: makeNewNote
    //
    // This is called from the constructor; should be overridden by
    //   child classes and those children should NOT call this one.
    //-------------------------------------------------------------------
    JComponent makeNewNote(int i) {
        NoteComponent nc = new NoteComponent(this, i);
        nc.setVisible(false);
        return nc;
    } // end makeNewNote


    //------------------------------------------------------------------
    // Method Name: pageNumberChanged
    //
    // This method is provided as a means for the pager to notify a
    //   group that the page has changed.  If this notification is
    //   needed, the child will override this no-op base method and
    //   take some action.
    //------------------------------------------------------------------
    protected void pageNumberChanged() {
    } // end pageNumberChanged


    // Called by the pager control
    void pageAway(int pageFrom) {
        MemoryBank.debug("Paging away from Page: " + pageFrom);
        unloadInterface(pageFrom);
    } // end pageTo

    // Called by the pager control
    void pageTo(int pageTo) {
        MemoryBank.debug("Paging To Page: " + pageTo);
        loadInterface(pageTo);
    } // end pageTo

    //----------------------------------------------------------------------
    // Method Name: preClose
    //
    // This should be called prior to closing.
    //----------------------------------------------------------------------
    void preClose() {
        if (null != extendedNoteComponent && null != defaultSubject) {
            extendedNoteComponent.saveSubjects();
        }
        if (groupChanged) {
            unloadInterface(theNotePager.getCurrentPage());
            saveNoteGroup();
        }
    } // end preClose


    // Called by child groups that need sorting but don't have access to unloadInterface
    // (TodoNoteGroup  sortPriority, sortText (has more options than NoteGroup.sortNoteString))
    void preSort() {
        // Preserve current interface changes before sorting.
        unloadInterface(theNotePager.getCurrentPage());
    } // end preSort


    Vector<NoteData> getCondensedInfo() {
        Vector<NoteData> trimmedList = new Vector<>();

        // Xfer the 'good' data over to a new, temporary Vector.
        for (Object object : groupDataVector) {

            // This can happen with an 'empty' NoteGroup.
            if (object == null) continue;

            // Don't retain this note if there is no significant primary text.
            NoteData tempNoteData = (NoteData) object;
            if (tempNoteData.getNoteString().trim().isEmpty()) continue;

            // Add each 'good' note to the 'keeper' list.
            trimmedList.add(tempNoteData);
        }
        return trimmedList;
    }

    //--------------------------------------------------------------
    // Method Name: saveGroup
    //
    // Saving is an operation that happens automatically and often;
    //   if errors are encountered, this method can trap and print
    //   them to the screen but it will not halt execution or
    //   attempt interaction with the user.  A status variable is
    //   set at various points; child classes that 'care' about the
    //   results should check it and handle the values according to
    //   those situations.
    //--------------------------------------------------------------
    private void saveNoteGroup() {
        //AppUtil.localDebug(true);
        intSaveGroupStatus = ONGOING;
        String groupFilename = getGroupFilename();
        File f;

        // At this point, we may or may not have previously loaded a file
        //   successfully.  If we have, we will have a non-empty Filename
        //   and our first step will be to move that file out of the way
        //   before this new save operation can proceed.
        if (!groupFilename.trim().equals("")) {
            // Deleting or archiving first is necessary in case
            //   the change was to delete the information.
            MemoryBank.debug("NoteGroup.saveGroup: old filename = " + groupFilename);
            if (MemoryBank.archive) {
                MemoryBank.debug("  Archiving: " + shortName());
                // Note: need to implement archiving but for now, what happens
                // is what does not happen - we do not delete the old version.
            } else {
                f = new File(groupFilename);
                if (!deleteFile(f)) {
                    MemoryBank.debug("  Failed to delete; returning");
                    intSaveGroupStatus = DELETEOLDFILEFAILED;
                    return;
                }
            } // end if archive or delete
        } // end if

        // The name of the file to save to may not always be the same as
        //   the one we previously loaded (or attempted to load).  So, we
        //   let the child NoteGroup set the name of the file to save to.
        groupFilename = getGroupFilename();
        MemoryBank.debug("  Saving NoteGroup data in " + shortName());

        // Verify that the path is a valid one.
        f = new File(groupFilename);
        if (f.exists()) {
            // If the file already exists -
            if (f.isDirectory()) {
                System.out.println("Error - directory in place of file: " + groupFilename);
                intSaveGroupStatus = DIRECTORYINMYPLACE;
                return;
            } // end if directory
        } else {
            // The file does not already exist; check the path to it.
            String strThePath;
            strThePath = groupFilename.substring(0, groupFilename.lastIndexOf(File.separatorChar));
            f = new File(strThePath);
            if (!f.exists()) { // The directory path does not exist
                // Create the directory path down to the level you need.
                if (!f.mkdirs()) {
                    System.out.println("Error - unable to create the directory path: " + strThePath);
                    intSaveGroupStatus = CANNOTMAKEAPATH;
                    return;
                } // end if directory creation failed
            } else {
                // It does exist; make sure it is a directory and nothing else
                if (!f.isDirectory()) {
                    System.out.println("Error - file in place of directory : " + strThePath);
                    intSaveGroupStatus = FILEINMYDIRPATH;
                    return;
                } // end if not a directory
            } // end if/else the path exists
        } // end if exists

        // The logic below will allow for a file with properties but no
        // notes.  This might be a todo list with no items yet, or a
        // search that found no notes, both of which are allowed.
        int notesWritten = 0;
        Object[] theGroup;  // A new 'wrapper' for the Properties + List
        Vector<NoteData> trimmedList = getCondensedInfo();
        Object groupProperties = getProperties();
        if (groupProperties != null) {
            theGroup = new Object[2];
            theGroup[0] = groupProperties;
            theGroup[1] = trimmedList;
        } else {
            theGroup = new Object[1];
            theGroup[0] = trimmedList;
        } // end if there is a properties object

        if (saveWithoutData) {
            // We save a file, with data or not.
            notesWritten = FileGroup.saveFileData(groupFilename, theGroup);
        } else {
            // If there is data to preserve, do so now.
            if ((groupProperties != null) || (trimmedList.size() > 0)) {
                notesWritten = FileGroup.saveFileData(groupFilename, theGroup);
            } // end if

            // We didn't try to write a file if there was no data, but there are cases where
            // a file for this data might already be out there, that shouldn't be -
            // for example, if a file was previously created for writing but then the writes
            // failed, and we're here again at a later time.
            if ((notesWritten == 0) && (groupProperties == null)) deleteFile(new File(groupFilename));
        }

        if (notesWritten == trimmedList.size()) {
            intSaveGroupStatus = SUCCEEDED + notesWritten;
        } else {
            intSaveGroupStatus = OTHERFAILURE;
        } // end if note

        setGroupChanged(false); // A 'save' preserves all changes to this point, so we reset the flag.
        //AppUtil.localDebug(false);
    } // end saveGroup


    void setGroupHeader(Container c) {
        jsp.setColumnHeaderView(c);
    } // end setGroupHeader

    void setDefaultSubject(String defaultSubject) {
        this.defaultSubject = defaultSubject;
    }

    void setSelectionMonitor(NoteSelection noteSelection) {

    }

    @Override
    public void setStatusMessage(String s) {
        lblStatusMessage.setText("  " + s);
        lblStatusMessage.invalidate();
        theBasePanel.validate();
    } // end setMessage


    // The shift methods here can be made to work for ANY NoteGroup.
    //  currently, they work for groups with or without paging, that
    //  allow a note to be added.  Still need to incorporate the logic
    //  from the commented-out methods below, to support SearchResultGroup,
    //  which is currently written separately in a non-generic manner for
    //  a group that does NOT allow a note to be added.
    // Work needed in each group to use these vs their own:  Revise
    //   their NoteComponent swap methods override the base class vs
    //   the overload that they are doing now (change the parameter to
    //   a NoteComponent but leave the cast to child class).
    public void shiftDown(int index) {
        // Prevent the next-to-last note from shifting
        //   down, if it is on the last page (because the note below,
        //   that it would swap with, is not initialized).
        if (index == (lastVisibleNoteIndex - 1)) {
            if (theNotePager.getCurrentPage() == theNotePager.getHighestPage()) return;
        } // end if

        // Prevent the last note on the page from shifting
        //   down, if it is on the last page (because there is nowhere to go).
        if (index == lastVisibleNoteIndex) {
            if (theNotePager.getCurrentPage() == theNotePager.getHighestPage()) return;
            else {      // allow for paging, later
                //System.out.println("Shifting down across a page boundary - NOT.");
                return;
            } // end if/else
        } // end if - if this is the last visible note

        // System.out.println("Shifting note down");
        NoteComponent nc1, nc2;
        nc1 = (NoteComponent) groupNotesListPanel.getComponent(index);
        nc2 = (NoteComponent) groupNotesListPanel.getComponent(index + 1);

        nc1.swap(nc2);
        nc2.setActive();
    } // end shiftDown


    public void shiftUp(int index) {
        // Prevent the first note on the page from shifting up.
        if (index == 0) {
            if (theNotePager.getCurrentPage() == 1) return;
            else {      // but maybe allow for paging, later
                //System.out.println("Shifting up across a page boundary - NOT.");
                return;
            }
        } // end if

        // System.out.println("Shifting note up");
        NoteComponent nc1, nc2;
        nc1 = (NoteComponent) groupNotesListPanel.getComponent(index);
        nc2 = (NoteComponent) groupNotesListPanel.getComponent(index - 1);

        nc1.swap(nc2);
        nc2.setActive();
    } // end shiftUp

// May be possible to have ALL NoteGroup shift methods here.
//   leave the copy below until determined/done.


//  public void shiftDown(int index) {
//    if(addNoteAllowed) {
//      if(index >= (lastVisibleNoteIndex - 1)) return;
//    } else {
//      if(index > (lastVisibleNoteIndex - 1)) return;
//    } // end if
//    
//     System.out.println("Shifting note down");
//    NoteComponent nc1, nc2;
//    nc1 = (NoteComponent) groupNotesListPanel.getComponent(index);
//    nc2 = (NoteComponent) groupNotesListPanel.getComponent(index+1);
//    
//    nc1.swap(nc2);
//    nc2.setActive();
//  } // end shiftDown
//
//
//  public void shiftUp(int index) {
//    if(addNoteAllowed) {
//      if(index == 0 || index == lastVisibleNoteIndex) return;
//    } else {
//      if(index == 0) return;
//    } // end if
//    
//     System.out.println("Shifting note up");
//    NoteComponent nc1, nc2;
//    nc1 = (NoteComponent) groupNotesListPanel.getComponent(index);
//    nc2 = (NoteComponent) groupNotesListPanel.getComponent(index-1);
//    
//    nc1.swap(nc2);
//    nc2.setActive();
//  } // end shiftUp


    // Returns a short (no path) version of the groupFilename
    // but stops short of 'prettifying' it.
    private String shortName() {
        String s = getGroupFilename();
        int ix = s.lastIndexOf(File.separatorChar);
        if (ix != -1) s = s.substring(ix + 1);
        return s;
    } // end shortName


    void sortLastMod(int direction) {

        // Preserve current interface changes before sorting.
        unloadInterface(theNotePager.getCurrentPage());

        // Do the sort
        LastModComparator lmc = new LastModComparator(direction);
        groupDataVector.sort(lmc);

        // Display the same page, now with possibly different contents.
        loadInterface(theNotePager.getCurrentPage());
    } // end sortLastMod


    void sortNoteString(int direction) {

        // Preserve current interface changes before sorting.
        unloadInterface(theNotePager.getCurrentPage());

        // Do the sort
        NoteStringComparator nsc = new NoteStringComparator(direction);
        groupDataVector.sort(nsc);

        // Display the same page, now with possibly different contents.
        loadInterface(theNotePager.getCurrentPage());
    } // end sortNoteString


    //-------------------------------------------------------------------
    // Method Name: unloadInterface
    //
    // This method transfers the visible notes from the page to their
    //   correct places in the data vector, and adds vector elements
    //   when needed to match the location in the interface to the data
    //   Vector.  It may be needed during various events, not only
    //   immediately prior to redisplaying the group.  For that reason
    //   we allow possible 'gaps' in data from cleared items, for now.
    //   Gaps will be removed by the getCondensedInfo() method that is
    //   called during the 'save' process.
    //-------------------------------------------------------------------
    void unloadInterface(int currentPage) {

        // Set the indexes into the data vector -
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = startIndex + lastVisibleNoteIndex;  // last visible may or may not be initialized.
        MemoryBank.debug("NoteGroup.unloadInterface into vector index " + startIndex + " to " + endIndex);

        // When unloading the currently displayed interface, we need to know where the groupDataVector ends and new
        // data begins.  groupDataVector size-1 will almost always be less than endIndex
        // (because on a less-than-full page, the last visible note hasn't been typed into yet, for one thing).
        // The maxDataIndex will tell us whether a note should be replaced or just added to the end of groupDataVector.
        int maxDataIndex = groupDataVector.size() - 1;

        NoteComponent tempNoteComponent;
        NoteData tempNoteData;

        // Scan the interface, and adjust the groupDataVector so that it
        // matches.  Don't be tempted to just rebuild from the current interface
        // vs this more surgical method; that approach does not take into account
        // the fact that the Vector may span more than the one visible page.
        int panelIndex = 0;
        for (int dataIndex = startIndex; dataIndex <= endIndex; dataIndex++) {
            tempNoteComponent = (NoteComponent) groupNotesListPanel.getComponent(panelIndex++);
            tempNoteData = tempNoteComponent.getNoteData();
            if (dataIndex <= maxDataIndex) {
                groupDataVector.setElementAt(tempNoteData, dataIndex);
            } else {  // New, user-entered data is in the interface.  Get it.
                if(tempNoteComponent.initialized) {  // This could be false on the last note on the page.
                    System.out.println("NoteGroup.unloadInterface: Adding new element!");
                    groupDataVector.addElement(tempNoteData);
                }
            } // end if
        } // end for i
    } // end unloadInterface


    //----------------------------------------------------
    // Method Name: updateGroup
    //
    // Repaints the display by clearing the data, then reload.
    // Called by various actions.  To preserve changes,
    //   the calling context should first call 'preClose'.
    //----------------------------------------------------
    public void updateGroup() {
        clearPage(); // Clears the data (not Components) from the interface.

        // The reset is needed BEFORE loadGroup, in case we came here
        //   when the page number was higher than 1; a condition
        //   that may be in effect during a 'refresh' which would
        //   cause the higher numbered page to be loaded with page
        //   one data.  So - we make sure we are on page one.
        theNotePager.reset(1);

        loadNoteGroup();      // Loads the data array and interface.
        setGroupChanged(false);

        // Also needed AFTER loadGroup, not to set the page number but to set the
        //   total number of pages, which will be shown in the pager control.
        theNotePager.reset(1);

    } // end updateGroup


    protected void refresh() {
        preClose();     // Save any in-progress changes
        updateGroup();  // Reload the interface - this removes 'gaps'.
    }


    static class LastModComparator implements Comparator<NoteData> {
        int direction;

        LastModComparator(int d) {
            direction = d;
        } // end constructor

        public int compare(NoteData nd1, NoteData nd2) {
            ZonedDateTime zdt1 = nd1.getLastModDate();
            ZonedDateTime zdt2 = nd2.getLastModDate();

            if (direction == ASCENDING) {
                return zdt1.compareTo(zdt2);
            } else {
                return zdt2.compareTo(zdt1);
            } // end if
        } // end compare
    } // end class LastModComparator


    static class NoteStringComparator implements Comparator<NoteData> {
        int direction;

        NoteStringComparator(int d) {
            direction = d;
        } // end constructor

        public int compare(NoteData nd1, NoteData nd2) {
            String s1, s2;

            s1 = nd1.getNoteString();
            s2 = nd2.getNoteString();

            if (direction == ASCENDING) {
                return s1.compareTo(s2);
            } else {
                return s2.compareTo(s1);
            } // end if
        } // end compare
    } // end class NoteStringComparator



    boolean getGroupChanged() {
        return groupChanged;
    } // end getGroupChanged

    // Used to enable or disable the 'undo' and 'save' menu items.  Called once when the
    // list menu is initially set and then later called repeatedly for every 'setGroupChanged'
    protected void adjustMenuItems(boolean b) {
        if (myListMenu == null) return; // Too soon.  Come back later.
        MemoryBank.debug("NoteGroup.adjustMenuItems <" + b + ">");

        // And now we adjust the Menu -
        JMenuItem theUndo = AppUtil.getMenuItem(myListMenu, "Undo All");
        if (theUndo != null) theUndo.setEnabled(b);
        JMenuItem theSave = AppUtil.getMenuItem(myListMenu, "Save");
        if (theSave != null) theSave.setEnabled(b);
    }

    // Not all NoteGroups need to manage enablement of items in their menu but those
    // that do, all do the same things.  If this ever branches out into different
    // actions and/or menu items then they can override this and/or adjustMenuItems.
    void setListMenu(JMenu listMenu) {
        MemoryBank.debug("TreeLeaf.setListMenu: " + listMenu.getText());
        myListMenu = listMenu;
        adjustMenuItems(groupChanged); // set enabled state for 'undo' and 'save'.
    }


} // end class NoteGroup
