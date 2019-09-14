import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;


public abstract class NoteGroup extends JPanel {
    static final long serialVersionUID = 1L;

    // Status report codes for Load / Save
    private static final int INITIAL = 500;
    static final int ONGOING = 501;
    private static final int DELETEOLDFILEFAILED = 502;
    private static final int DIRECTORYINMYPLACE = 503;
    private static final int CANNOTMAKEAPATH = 504;
    private static final int FILEINMYDIRPATH = 505;
    private static final int OTHERFAILURE = 506;
    private static final int SUCCEEDED = 600;

    // Directions for Sort operations
    static final int ASCENDING = 0;
    static final int DESCENDING = 1;

    //=============================================================
    // Members that child classes may access directly
    //=============================================================
    protected static String ems;     // Error Message String
    protected ExtendedNoteComponent enc;
    boolean addNoteAllowed;
    private int borderWidth = 2;
    int lastVisibleNoteIndex = 0;
    int pageSize;

    // Child classes that check this variable should test for a
    //   value >= SUCCEEDED; if true, the count of records written
    //   may be obtained by: (intSaveGroupStatus - SUCCEEDED)
    int intSaveGroupStatus = INITIAL;

    // Container for graphical members - limited to pageSize
    protected JPanel groupNotesListPanel;

    // Container for a paging control
    NotePager npThePager;

    // Container for the (complete collection of) Group data objects
    protected Vector<NoteData> vectGroupData;

    // The properties for the group - cast to proper class
    Object objGroupProperties;   // REMOVE this; now the child classes set their properties directly
    //=============================================================

    // Private members
    //-------------------------------------------------------------
    private static final int PAGE_SIZE = 40;

    private int intHighestNoteComponentIndex;

    private JScrollPane jsp;

    // Flag used to determine if saving might be necessary.
    private boolean groupChanged;

    // The Information/Status panel of the frame.
    private JLabel lblStatusMessage;

    // Access with getGroupFilename() & setGroupFilename()
    private String strGroupFilename;
    //-------------------------------------------------------------

    NoteGroup(String defaultSubject) {
        this(defaultSubject, PAGE_SIZE);
    } // end constructor 1

    private NoteGroup(String defaultSubject, int intPageSize) {
        super(new BorderLayout());
        pageSize = intPageSize;
        enc = new ExtendedNoteComponent(defaultSubject);
        addNoteAllowed = true;
        intHighestNoteComponentIndex = pageSize - 1;

        vectGroupData = new Vector<>();
        jsp = new JScrollPane();
        JScrollBar jsb = new JScrollBar();

        // This is necessary because otherwise, once the bar appears,
        //  the tab and up/down keys can transfer focus over to here,
        //  and the up/down keys cannot get it back out.
        jsb.setFocusable(false);

        jsb.setUnitIncrement(NoteComponent.NOTEHEIGHT);
        jsb.setBlockIncrement(NoteComponent.NOTEHEIGHT);
        jsp.setVerticalScrollBar(jsb);

        strGroupFilename = "";
        groupChanged = false;

        setBorder(BorderFactory.createLineBorder(Color.black, borderWidth));

        groupNotesListPanel = new JPanel();
        groupNotesListPanel.setLayout(
                new BoxLayout(groupNotesListPanel, BoxLayout.Y_AXIS));

        // Make the paging control.  It is up to the various child classes
        //   to display it to the user by adding it to their interface.
        npThePager = new NotePager(this);

        // The 'makeNewNote' methodology works for ALL child groups regardless
        //   of the Type of new note that they make, because we do not
        //   actually put the result into a typed variable; it goes directly
        //   into the container.  Every Group method that accesses the
        //   container contents will either be overridden and cast the
        //   note to the correect type, or it will call a method common
        //   to all of them, such as the 'setVisible' in the next section.
        for (int i = 0; i <= intHighestNoteComponentIndex; i++) {
            groupNotesListPanel.add(makeNewNote(i));
        } // end for

        // The first note should not be invisible.
        // This is for SearchResultGroup, since he specifies a page size
        //   that is the same as the number of records read in, since
        //   we don't yet have paging.  At some point, this test needs
        //   to go away or move or change logic/readability.
        if (pageSize > 0) {
            groupNotesListPanel.getComponent(0).setVisible(true);
        } // end if there is at least one Note

        add(jsp, BorderLayout.CENTER);
        jsp.setViewportView(groupNotesListPanel);

        String s = "Use the mouse pointer for context-sensitive help";
        lblStatusMessage = new JLabel(s);
        add(lblStatusMessage, BorderLayout.SOUTH);

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
    //----------------------------------------------------------------------
    void activateNextNote(int noteIndex) {
        if (noteIndex < lastVisibleNoteIndex) return;  // already showing.

        if (lastVisibleNoteIndex < intHighestNoteComponentIndex) {
            lastVisibleNoteIndex++;
            NoteComponent nc;
            nc = (NoteComponent) groupNotesListPanel.getComponent(lastVisibleNoteIndex);
            nc.setVisible(true);
        } else {
            // Implement a page rollover.
            int tmpPage = npThePager.getCurrentPage();
            if (tmpPage > 0) {   // < to disable, > normal ops
                // Ensure this only happens after the first pager reset.

                if (tmpPage == npThePager.getHighestPage()) {
                    unloadInterface(tmpPage);  // Add new notes to the vector.
                    npThePager.reset(tmpPage);
                } // end if
            } // end if

        } // end if
    } // end activateNextNote


    //----------------------------------------------------------------
    // Method Name: clearGroup
    //
    // Clear all data (which may span more than one page) and the interface.
    //----------------------------------------------------------------
    void clearGroup() {
        transferFocusUpCycle(); // Otherwise can get unwanted focus events.
        clearPage();
        vectGroupData.clear();
        groupChanged = true;
        saveGroup();   // Save the empty group (which will remove the file, if one exists).
        updateGroup(); // The display will be cleared when it tries to load a data file.
        groupNotesListPanel.invalidate(); // sometimes anomalous graphics remain.
        groupNotesListPanel.validate();   // sometimes anomalous graphics remain.
    } // end clearGroup


    //----------------------------------------------------------------
    // Method Name: clearPage
    //
    // Clear data from all Notes that are showing in the interface.
    // This resets any components that are displayed with info,
    //  as well as their underlying data objects.  It does not
    //  remove components or the data object itself; just clears them.
    //  (although - once a component is not initialized, a request for
    //  its data will return a null, whether it's really there or not)
    //----------------------------------------------------------------
    private void clearPage() {
        if (intHighestNoteComponentIndex < 0) return; // an 'empty' group

        for (int i = 0; i <= lastVisibleNoteIndex; i++) {
            NoteComponent tempNote = (NoteComponent) groupNotesListPanel.getComponent(i);

            // Only initialized components (appear to) have a data object.
            //      (I think; 8/29/2019)
            // The 'clear' method that is called below is overridden by child classes
            // so that they can first clear their own components.  After that, they still
            // call super.clear() which will clear the parent component and then call the
            // data-clearing method (also overridden, also calls its super).
//    if (tempNote.initialized) tempNote.clear();  // 8/29/2019 remove if no further problem with new TodoLists.
            tempNote.clear(); // Clear does not un-initialize.
            tempNote.initialized = false; // Because we are clearing the 'page'.

        } // end for
    } // end clearPage


    private boolean deleteFile(File f) {
        // There are a couple of cases where we could try to delete a file that is not there
        // in the first place.  So - we verify that the file is really there and only if it is
        // do we try to delete it.  Then we check for existence again because the deletion can
        // occasionally return a false negative (because of gc/timing issues?).
        // But File.delete does not ever return a false positive (that I've seen).
        // The user is only notified of a problem if the file still exists.
        if (f.exists() && !f.delete() && f.exists()) {
            (new Exception("File removal exception!")).printStackTrace();
            ems = "Error - unable to remove: " + f.getName();
            JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this),
                    ems, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            // System.out.println(f.getName() + " was removed");
            strGroupFilename = "";
            return true;
        } // end if
    } // end deleteFile


    //---------------------------------------------------------------
    // Method Name: editExtendedNoteComponent
    //
    // Provides an interface for the modification of elements of
    //   the extended note component, and returns true if there
    //   was a change; false otherwise.
    //
    // Note that the 'tempwin' used here was developed long ago;
    // it is pre-JOptionPane code.
    //---------------------------------------------------------------
    protected boolean editExtendedNoteComponent(NoteData nd) {
        JDialog tempwin;
        final Dimension d = new Dimension();
        // System.out.println("NoteGroup editExtendedNoteComponent");

        // Load the enc with the correct data
        enc.setExtText(nd.getExtendedNoteString());
        enc.setSubject(nd.getSubjectString());

        //---------------------------------------------------------
        // Make a dialog window to show the ExtendedNoteComponent
        //---------------------------------------------------------
        Frame myFrame = JOptionPane.getFrameForComponent(this);
        if (myFrame == null) return false;

        tempwin = new JDialog(myFrame, true);
        tempwin.getContentPane().add(enc, BorderLayout.CENTER);

        // Preserve initial values, for later comparison to
        //   determine if there was a change.
        int origWidth = nd.getExtendedNoteWidthInt();
        int origHeight = nd.getExtendedNoteHeightInt();
        String origSubject = nd.getSubjectString();
        String origExtendedString = nd.getExtendedNoteString();

        tempwin.setSize(origWidth, origHeight);

        tempwin.setTitle(nd.getNoteString());
        tempwin.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                enc.checkSubject();
                d.setSize(we.getWindow().getSize());
                we.getWindow().dispose();
            }
        });

        // Center the ENC dialog relative to the main frame.
        tempwin.setLocationRelativeTo(myFrame);

        // Go modal -
        tempwin.setVisible(true);

        // Collect results of the editing -
        //------------------------------------------------------------------
        int newWidth = d.width;
        int newHeight = d.height;
        String newSubject = enc.getSubject();
        String newExtendedString = enc.getExtText();

        // We need to be able to save a 'None' subject, and recall it,
        //   which is different than if you never set one in the
        //   first place, in which case you should get the default.  So -
        //   we allow the newSubject above without checking its content.

        boolean aChangeWasMade = false;
        if (newWidth != origWidth) aChangeWasMade = true;
        if (newHeight != origHeight) aChangeWasMade = true;
        if (newSubject != null) {
            if (origSubject == null) aChangeWasMade = true;
            else if (!newSubject.equals(origSubject)) aChangeWasMade = true;
        } // end if
        if (!newExtendedString.equals(origExtendedString)) aChangeWasMade = true;

        if (aChangeWasMade) {
            nd.setExtendedNoteWidthInt(newWidth);
            nd.setExtendedNoteHeightInt(newHeight);
            nd.setExtendedNoteString(newExtendedString);
            nd.setSubjectString(newSubject);
        } // end if

        //------------------------------------------------------------------

        return aChangeWasMade;
    } // end editExtendedNoteComponent


    // -------------------------------------------------------------------
    // Method Name: getGroupFilename
    //
    // This method returns the name of the file where the data for this
    //   group of notes is loaded / saved.
    // -------------------------------------------------------------------
    public abstract String getGroupFilename();

    int getHighestNoteComponentIndex() {
        return intHighestNoteComponentIndex;
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
    // Method Name: getPreferredSize
    //
    // This preference is necessary to limit the default height from
    //   being the total of all content heights.  The actual size can
    //   be larger, if the NoteGroup is placed into a stretchable
    //   container / layout.  By 'preferring' less than actual contents,
    //   when the size of the content turns out to be larger than the
    //   container to hold it, the vertical scrollbar of the JScrollPane
    //     'kicks in'.
    //--------------------------------------------------------------------
    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, 400);
    } // end getPreferredSize


    //--------------------------------------------------------------------
    // Method Name: getProperties
    //
    //  Called by saveGroup - child classes may override and return
    //    an actual Object; otherwise returns null.
    //--------------------------------------------------------------------
    protected Object getProperties() {
        return null;
    } // end getProperties

    //--------------------------------------------------------------------
    // Method Name: gotoPage
    //
    // Called by the pager control
    //--------------------------------------------------------------------
    void gotoPage(int pageTo) {
        MemoryBank.debug("Paging To Page: " + pageTo);
        // We do a saveGroup here vs an unloadInterface, because:
        //   1.  We can skip it if there were no changes (fast).
        //   2.  The loadInterface will set groupChanged to false, so
        //       if we do not save now, we will not know that we need
        //       to save, later.
        if (groupChanged) saveGroup(); // unloads the 'PageFrom' page

        loadInterface(pageTo);
        resetVisibility();
    } // end gotoPage


    // Learned how to do this (convert an ArrayList element that is a LinkedHashMap, to a Vector of <my custom class>),
    // from: https://stackoverflow.com/questions/15430715/casting-linkedhashmap-to-complex-object
    // Previously, I just cycled thru the LinkedHashMap by accepting the entries as Object, then converted them to JSON
    // string, then parsed the string back in to a NoteData and added it to a new Vector.  But that was  a several-line
    // method; this conversion is a one-liner, and my version had the possibility of throwing an Exception that needed
    // to be caught.  Child classes override this, to set the Vector content type to their own internal data class.
    void setGroupData(Object[] theGroup)  {
        NoteData.loading = true; // We don't want to affect the lastModDates!
        vectGroupData = AppUtil.mapper.convertValue(theGroup[0], new TypeReference<Vector<NoteData>>() { });
        NoteData.loading = false; // Restore normal lastModDate updating.
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



    private void loadGroup() {

        // Clear before loading
        //----------------------------------------------------
        // The group may or may not have a 'Properties' object.
        objGroupProperties = null;

        vectGroupData.clear();
        lastVisibleNoteIndex = 0;
        //----------------------------------------------------

        // This string is set now, just prior to loading the group
        //   rather than earlier, because in some child classes the
        //   group will have changed so that the file to load is not the
        //   same as it was at group construction; the filename for the
        //   group may be highly variable, and this method may be getting
        //   called after each filename change, even though the group
        //   itself remains the same.
        strGroupFilename = getGroupFilename();

        MemoryBank.debug("Loading NoteGroup data from: " + strGroupFilename);
        Object[] theGroup = AppUtil.loadNoteGroupData(strGroupFilename);
        if(theGroup != null) {
            //System.out.println("NoteGroup data from JSON file: " + AppUtil.toJsonString(theGroup));
            setGroupData(theGroup);
        } else {
            // Setting the name to 'empty' IS needed; it is examined when
            //   saving and if non-empty, the old file is deleted first.  Of
            //   course, if the file already does not exist, we don't want
            //   to let it try to do that.
            strGroupFilename = "";
            return;
        }

        Exception e = null;
        try {
            loadInterface(1); // Always load page 1
        } catch (ClassCastException cce) {
            e = cce;
        } // end try/catch

        if (e != null) {
            ems = "NoteGroup loadGroup: Error in loading " + strGroupFilename + " !\n";
            ems = ems + e.toString();
            ems = ems + "\nData file load operation aborted.";
            System.out.println("ems = " + ems);
            JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this),
                    ems, "Error", JOptionPane.ERROR_MESSAGE);
        } // end if
    } // end loadGroup


    //-------------------------------------------------------------------
    // Method Name: loadInterface
    //
    // This method transfers the data vector items to the onscreen
    //   components.  A 'page' specifier is used to determine which
    //   portion of the vector to display, if there are more than
    //   will fit on one page.  Child classes are responsible for
    //   displaying the pager control.  If they do not, then only
    //   the first page of data will be available.
    //-------------------------------------------------------------------
    private void loadInterface(int intPageNum) {
        int panelIndex = 0;

        //AppUtil.localDebug(true);

        // Set the indexes into the data vector -
        int maxDataIndex = vectGroupData.size() - 1;
        int startIndex = (intPageNum - 1) * pageSize;
        int endIndex = (intPageNum * pageSize) - 1;
        if (endIndex > maxDataIndex) endIndex = maxDataIndex;

        NoteComponent tempNote;

        MemoryBank.debug("NoteGroup.loadInterface from index " + startIndex + " to " + endIndex);

        for (int i = startIndex; i <= endIndex; i++) {
            MemoryBank.debug("  loading index " + panelIndex + " with data element " + i);
            // The next line casts to NoteComponent.  If the component is actually
            //   a child of NoteComponent then it will still work and the
            //   'setNoteData' that is called will be the child class method, not
            //   the one from the base class.
            //   That behavior is critical to this app.
            tempNote = (NoteComponent) groupNotesListPanel.getComponent(panelIndex);
            tempNote.setNoteData(vectGroupData.elementAt(i));
            tempNote.setVisible(true);
            panelIndex++;
        } // end for

        if (intPageNum > 1) {
            // For page numbers higher than 1, a previous page had already
            //   been loaded into the interface.  The data is only cleared
            //   out by new data coming in and if this is a 'short' page then
            //   the remaining lines were not cleared.  When new lines are
            //   activated, instead of a blank line, the info from that same line
            //   of the previous
            //   page will be displayed and then treated as new input.  To
            //   prevent that, these lines must be cleared visually, without
            //   affecting the data in the vector.
            MemoryBank.debug("NoteGroup.loadInterface: panelIndex after load: " + panelIndex);
            MemoryBank.debug("  clearing from panelIndex to " + (pageSize - 1));

            for (int i = panelIndex; i < pageSize; i++) {
                tempNote = (NoteComponent) groupNotesListPanel.getComponent(i);
                MemoryBank.debug("    Clearing index " + i);

                if (tempNote.initialized) {
                    // Now clear the visual aspects - (see note below)
                    tempNote.makeDataObject();
                    tempNote.resetComponent();

                    tempNote.initialized = false;
                } // end if

                // Note: Cannot call clear() because that also clears the data
                //   object which is in the vectGroupData vector.  Instead, we
                //   first give the noteComponent a new data object, then
                //   instruct it to update its appearance based on that.  Only
                //   the first note in this range needs the visual clearing but
                //   all of them need to be 'un' initialized.
            } // end for
        } // end if

        lastVisibleNoteIndex = panelIndex - 1;
        MemoryBank.debug("lastVisibleNoteIndex (before activateNextNote) is " + lastVisibleNoteIndex);

        if (addNoteAllowed) activateNextNote(panelIndex);

        // Each of the 'setNoteData' calls above would have set this to true.
        groupChanged = false;

        //AppUtil.localDebug(false);
    } // end loadInterface


    //-------------------------------------------------------------------
    // Method Name: makeNewNote
    //
    // This is called from the constructor; should be overridden by
    //   child classes.
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


    void postSort() {
        // Display the same page, now with possibly different contents.
        loadInterface(npThePager.getCurrentPage());
        resetVisibility();
    } // end postSort


    //----------------------------------------------------------------------
    // Method Name: preClose
    //
    // This should be called prior to closing.
    //----------------------------------------------------------------------
    protected void preClose() {
        enc.saveSubjects();
        if (groupChanged) saveGroup();
    } // end preClose


    void preSort() {
        // Preserve current interface changes before sorting.
        unloadInterface(npThePager.getCurrentPage());
    } // end preSort


    //--------------------------------------------------------------
    // Method Name: resetVisibility
    //
    // Sets the remaining unloaded note lines (if any) to invisible.
    //--------------------------------------------------------------
    private void resetVisibility() {
        NoteComponent tempNote;

        for (int i = lastVisibleNoteIndex + 1; i <= intHighestNoteComponentIndex; i++) {
            tempNote = (NoteComponent) groupNotesListPanel.getComponent(i);
            tempNote.setVisible(false);
        } // end for i
    } // end resetVisibility


    Vector<NoteData> getCondensedInfo() {
        Vector<NoteData> trimmedList = new Vector<>();

        // Xfer the 'good' data over to a new, temporary Vector.
        for (NoteData tempNoteData : vectGroupData) {

            // This can happen with an 'empty' NoteGroup.
            if (tempNoteData == null) continue;

            // Don't save if there is no significant primary text.
            if (tempNoteData.getNoteString().trim().equals("")) continue;

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
    private void saveGroup() {
        //AppUtil.localDebug(true);
        intSaveGroupStatus = ONGOING;
        File f;

        // At this point, we may or may not have previously loaded a file
        //   successfully.  If we have, we will have a non-empty Filename
        //   and our first step will be to move that file out of the way
        //   before this new save operation can proceed.
        if (!strGroupFilename.trim().equals("")) {
            // Deleting or archiving first is necessary in case
            //   the change was to delete the information.
            MemoryBank.debug("NoteGroup.saveGroup: old filename = " + strGroupFilename);
            if (MemoryBank.archive) {
                MemoryBank.debug("  Archiving: " + shortName());
                // Note: need to implement archiving but for now, what happens
                // is what does not happen - we do not delete the old version.
            } else {
                f = new File(strGroupFilename);
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
        strGroupFilename = getGroupFilename();
        MemoryBank.debug("  Saving NoteGroup data in " + shortName());

        // Verify that the path is a valid one.
        f = new File(strGroupFilename);
        if (f.exists()) {
            // If the file already exists -
            if (f.isDirectory()) {
                System.out.println("Error - directory in place of file: " + strGroupFilename);
                intSaveGroupStatus = DIRECTORYINMYPLACE;
                return;
            } // end if directory
        } else {
            // The file does not already exist; check the path to it.
            String strThePath;
            strThePath = strGroupFilename.substring(0, strGroupFilename.lastIndexOf(File.separatorChar));
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

        // Update the vectGroupData with data from the interface.
        //----------------------------------------------------------------
        int pageToSave = npThePager.getPageFrom();
        MemoryBank.debug("  Unloading page " + pageToSave);
        unloadInterface(pageToSave);
        // Note that we unload the 'page from' rather than the current
        //   page, because this method may be called during the paging
        //   event itself but also upon closing.  'pageFrom' is updated
        //   in the pager only after the other paging actions are done.
        //----------------------------------------------------------------

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

        // If there is data to preserve, do so now.
        if ((groupProperties != null) || (trimmedList.size() > 0)) {
            notesWritten = AppUtil.saveNoteGroupData(strGroupFilename, theGroup);
        } // end if


        // We didn't try to write a file if there was no data, but there are cases where
        // a file for this data might already be out there, that shouldn't be -
        // for example, if a file was created for writing but then the writes failed.
        if ((notesWritten == 0) && (groupProperties == null)) deleteFile(new File(strGroupFilename));

        if (notesWritten == vectGroupData.size()) {
            intSaveGroupStatus = SUCCEEDED + notesWritten;
        } else {
            intSaveGroupStatus = OTHERFAILURE;
        } // end if note

        // No need to save again until after the next data change.
        groupChanged = false;
        //AppUtil.localDebug(false);
    } // end saveGroup


    void setGroupHeader(Container c) {
        jsp.setColumnHeaderView(c);
    } // end setGroupHeader


    //----------------------------------------------------------------
    // Method Name: setGroupChanged
    //
    // Called by all contexts that make a change to the data, each
    //   time a change is made.
    //----------------------------------------------------------------
    public void setGroupChanged() {
        groupChanged = true;
    } // end setGroupChanged


    public void setMessage(String s) {
        lblStatusMessage.setText("  " + s);
        lblStatusMessage.invalidate();
        validate();
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
        //   down, if it is on the last page
        if (index == (lastVisibleNoteIndex - 1)) {
            if (npThePager.getCurrentPage() == npThePager.getHighestPage()) return;
        } // end if

        // Prevent the last note on the page from shifting
        //   down, if it is on the last page.
        if (index == lastVisibleNoteIndex) {
            if (npThePager.getCurrentPage() == npThePager.getHighestPage()) return;
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
        // Prevent the first note on the page from shifting
        //   up, unless it is on page 2 or higher.
        if (index == 0) {
            if (npThePager.getCurrentPage() == 1) return;
            else {      // allow for paging, later
                //System.out.println("Shifting up across a page boundary - NOT.");
                return;
            }
        } // end if

        // Prevent the last note from shifting
        //   up, if it is on the last page
        if (index == lastVisibleNoteIndex) {
            if (npThePager.getCurrentPage() == npThePager.getHighestPage()) return;
        } // end if

        // System.out.println("Shifting note up");
        NoteComponent nc1, nc2;
        nc1 = (NoteComponent) groupNotesListPanel.getComponent(index);
        nc2 = (NoteComponent) groupNotesListPanel.getComponent(index - 1);

        nc1.swap(nc2);
        nc2.setActive();
    } // end shiftUp

// May be possible to have ALL shift methods here.
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


    // Returns a short (no path) version of the strGroupFilename
    private String shortName() {
        String s = strGroupFilename;
        int ix = s.lastIndexOf(File.separatorChar);
        if (ix != -1) s = s.substring(ix + 1);
        return s;
    } // end shortName


    void sortLastMod(int direction) {

        // Preserve current interface changes before sorting.
        unloadInterface(npThePager.getCurrentPage());

        // Do the sort
        LastModComparator lmc = new LastModComparator(direction);
        vectGroupData.sort(lmc);

        // Display the same page, now with possibly different contents.
        loadInterface(npThePager.getCurrentPage());
        resetVisibility();
    } // end sortLastMod


    void sortNoteString(int direction) {

        // Preserve current interface changes before sorting.
        unloadInterface(npThePager.getCurrentPage());

        // Do the sort
        NoteStringComparator nsc = new NoteStringComparator(direction);
        vectGroupData.sort(nsc);

        // Display the same page, now with possibly different contents.
        loadInterface(npThePager.getCurrentPage());
        resetVisibility();
    } // end sortNoteString


    //-------------------------------------------------------------------
    // Method Name: unloadInterface
    //
    // This method transfers the visible notes from the page to their
    //   correct places in the data vector.  It should be called
    //   during the 'save' process.
    //-------------------------------------------------------------------
    private void unloadInterface(int currentPage) {
        if (currentPage == 0) return; // Pager not yet reset.

        // Set the indexes into the data vector -
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = (currentPage * pageSize) - (pageSize - lastVisibleNoteIndex);

        System.out.print("NoteGroup.unloadInterface into vector index " + startIndex);
        System.out.println(" to " + endIndex);

        // vectGroupData size can be less than lastVisibleNoteIndex (during a Todo
        // List Merge, for example) and it is the upper boundary that should be
        // used to determine the highest usable index into the vectGroupData.
        // The index will tell us whether a note
        //   should be replaced, or just added to the end of the vector.
        int maxDataIndex = vectGroupData.size() - 1;

        NoteComponent tempNoteComponent;
        NoteData tempNoteData;

        // Scan the interface, and adjust the vectGroupData Vector so that it
        // matches.  Don't be tempted to just rebuild from the current interface
        // vs this more surgical approach; that does not take into account the
        // fact that the Vector may span more than the one visible page.
        int panelIndex = 0;
        for (int i = startIndex; i <= endIndex; i++) {
            tempNoteComponent = (NoteComponent) groupNotesListPanel.getComponent(panelIndex++);
            tempNoteData = tempNoteComponent.getNoteData();
            if (tempNoteData != null) {
                // The last visible (uninitialized) note has null data.
                if (i <= maxDataIndex) {
                   vectGroupData.setElementAt(tempNoteData, i);
                } else {
                    // This happens during a TodoList merge operation; one or more
                    // new items being added all at once.
                    System.out.println("NoteGroup.unloadInterface: Adding new element!");
                    vectGroupData.addElement(tempNoteData);
                } // end if
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

        // This is needed BEFORE loadGroup, in case we came here
        //   when the page number was higher than 1; a condition
        //   that may be in effect during a 'refresh' which would
        //   cause the higher numbered page to be loaded with page
        //   one data.
        npThePager.reset(1);

        loadGroup();      // Loads the data array and interface.

        // Also needed AFTER loadGroup, to examine the correct size
        //   of the vector and determine the total number of pages.
        npThePager.reset(1);

        resetVisibility();

        groupChanged = false; // (was set to true by the load).
    } // end updateGroup


    static class LastModComparator implements Comparator<NoteData> {
        int direction;

        LastModComparator(int d) {
            direction = d;
        } // end constructor

        public int compare(NoteData nd1, NoteData nd2) {
            Calendar calOween = Calendar.getInstance();
            calOween.set(1987, Calendar.OCTOBER, 30);

            Date d1, d2;

            d1 = nd1.getLastModDate();
            d2 = nd2.getLastModDate();

            if (d1 == null) d1 = calOween.getTime();
            if (d2 == null) d2 = calOween.getTime();

            if (direction == ASCENDING) {
                return d1.compareTo(d2);
            } else {
                return d2.compareTo(d1);
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


} // end class NoteGroup
