import javax.swing.*;
import java.awt.*;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Vector;

// Provides an editable view of the Notes in the noteGroupDataVector of a NoteGroup.

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class NoteGroupPanel implements NoteComponentManager {
    static final int PAGE_SIZE = 40; // A default; see the pageSize member for actual size.

    // Directions for Sort operations
    static final int ASCENDING = 0;
    static final int DESCENDING = 1;

    //=============================================================
    // Members that child classes may access directly
    //=============================================================
    static Notifier optionPane;
    static String ems;     // Error Message String
    NoteGroup myNoteGroup; // Group onto which this Panel provides a window.
    JPanel theBasePanel;   // We could 'absorb' this member, if this class would extend a JPanel.
    ExtendedNoteComponent extendedNoteComponent;
    JMenu myListMenu; // Child classes each have their own menu

    String defaultSubject;
    private boolean editable;
    int lastVisibleNoteIndex = 0;
    int pageSize;

    // Container for graphical members - limited to pageSize
    protected JPanel groupNotesListPanel;

    // Container for a paging control
    NotePager theNotePager;

    // Private members
    private int intHighestNoteComponentIndex;
    private JScrollPane jsp;
    private JLabel lblStatusMessage; // The Information/Status panel of the frame.


    // A NoteGroupPanel is just the encapsulation of the central container panel and the methods that work on it,
    //   plus a bottom 'status' area for operational assistance messages to the user.
    // As an abstract it cannot be instantiated.  The child classes have titles and other panels, and can set a
    //   header on the central panel.
    NoteGroupPanel() {
        this(PAGE_SIZE);
    } // end constructor 1


    NoteGroupPanel(int intPageSize) {
        pageSize = intPageSize;
        BaseData.loading = true;   // attempted fix..
        buildNotesPanel();
        BaseData.loading = false;
    } // end constructor 2

    // You can add a note to either a plain NoteGroup or to a Panel (which adds it to its NoteGroup).
    // If you have a group that you got from a Panel, adding the note to it via the panel is needed so that it
    // gets picked up when the group saves, since the Panel save operation updates the Vector from its content
    // prior to saving the data.
    void addNote(NoteData noteData) {
        myNoteGroup.addNote(noteData); // This adds the note to the data Vector.
        theNotePager.reset(1); // Recalculate the number of pages; the addition may have caused a page rollover.
        loadPage(theNotePager.getHighestPage()); // Page to the end, to show the new note.
    }


    // This method will set the next note visible, unless:
    //   the requested index is already visible   OR
    //   there are no more hidden notes to show, in which case it will
    //   create a new page.
    // It is called either from NoteComponent.initialize() or loadPage().
    public void activateNextNote(int noteIndex) {
        if ((noteIndex >= 0) && (noteIndex < lastVisibleNoteIndex)) return;  // already showing.

        // noteIndex is -1 when we have come here from loadPage, where the displayed page is empty.
        if (noteIndex >= 0) {
            // Get the component for the indicated noteIndex (the note we're 'coming from').
            NoteComponent thisNote = (NoteComponent) groupNotesListPanel.getComponent(noteIndex);

            // If the note we're coming from has not been initialized, we shouldn't activate the next one.
            // How could this happen, you ask?  Well, it happens when a note has been added to a
            // page that we've paged away from and now we've come back to that page and
            // the loadPage method is trying to correctly set the lastVisibleNoteIndex.
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
                    // see that the total page count should be increased by one.
                    if (myNoteGroup.groupChanged) unloadNotesPanel(tmpPage);

                    theNotePager.reset(tmpPage);
                } // end if
            } // end if

        } // end if
    } // end activateNextNote


    // This is a convenience method to allow a NoteGroupPanel to act like a JPanel.
    // The signature and name matches the JPanel method, and this method is just a
    // pass-thru to our encapsulated JPanel so that it works even though this class
    // does not extend that one.
    void add(JComponent component, Object object) {
        theBasePanel.add(component, object);
    }


    void buildNotesPanel() {
        theBasePanel = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            // This preference is necessary to set a limit of a maximum height value.
            //   After we add content to the scrollable area of this panel, if the
            //   height of that content exceeds the limit that we set here then
            //   the vertical scrollbar will kick in.
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, 400);
            } // end getPreferredSize
        };
        editable = true;
        intHighestNoteComponentIndex = pageSize - 1;

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
        optionPane = new Notifier() {
        }; // Uses all default methods.
    }


    // Clear all notes (which may span more than one page) and the interface.
    // This still leaves the GroupProperties.
    void clearAllNotes() {
        theBasePanel.transferFocusUpCycle(); // Otherwise can get unwanted focus events.
        clearPage();
        myNoteGroup.noteGroupDataVector.clear();
        showGroupData(myNoteGroup.noteGroupDataVector);
        setGroupChanged(true);
        theNotePager.reset(1);
    } // end clearAllNotes


    // Clear data from all NoteComponents that are showing in the interface,
    //  as well as their encapsulated NoteData instances.  It does not
    //  remove components from the panel; just makes them look new.
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


    // Provides an interface for the modification of two members of the NoteData.
    //   Returns true if there was a change to either one; false otherwise.
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

        // Present the ExtendedNoteComponent in a modal dialog
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

    boolean getEditable() { return editable; }

    // The (simple) name of the group (as seen as a node in the tree except for CalendarNoteGroup types)
    String getGroupName() {
        return myNoteGroup.getGroupProperties().getGroupName();
    }


    // Returns a NoteComponent that can be used to manipulate
    // component state as well as set/get underlying data.
    public NoteComponent getNoteComponent(int i) {
        return (NoteComponent) groupNotesListPanel.getComponent(i);
    } // end getNoteComponent


    // Called by preClosePanel only if the group has changed, prior to
    // persisting the group data.  It updates group content without
    // affecting NoteGroupPanel attributes such as the menus.
    protected void getPanelData() {

        // This can be needed by CalendarNoteGroups where the date has been altered.  Otherwise it's a no-op.
// that line is now in the CalendarNoteGroupPanel override of this method.
// Leaving the comments here for now because we are not 100% sure that it really is not otherwise needed.
        // maybe needed for link target changes?
// can remove after all tests pass and reverse link auto-removal is working.
//        myNoteGroup.setGroupProperties(myNoteGroup.getGroupProperties());

        unloadNotesPanel(theNotePager.getCurrentPage());
        myNoteGroup.setNotes(getCondensedInfo());
    }


    // View and Edit the linkages associated with this Group.
    void groupLinkages() {
        LinkagesEditorPanel linkagesEditorPanel = new LinkagesEditorPanel(myNoteGroup.getGroupProperties(), null);
        // In the above call, need to get properties via the accessor, because they could be null for CalendarNoteGroups.

        int choice = JOptionPane.showConfirmDialog(
                this.theBasePanel,
                linkagesEditorPanel,
                "Linkages Editor",
                JOptionPane.OK_CANCEL_OPTION, // Option type
                JOptionPane.PLAIN_MESSAGE);    // Message type

        if (choice == JOptionPane.OK_OPTION) {
            // Replace the original linkTargets with the linkTargets from the edited note.
            linkagesEditorPanel.updateLinkagesFromEditor();
            myNoteGroup.getGroupProperties().linkTargets = linkagesEditorPanel.linkTargets;

            // Save this NoteGroup, to preserve the new link(s) so that the reverse links that we
            // are about to create from it/them will have proper corresponding forward link(s).
            // Sorry but this will happen now even if 'Ok' was clicked but nothing had changed.
            setGroupChanged(true);
            preClosePanel();

            myNoteGroup.addReverseLinks(myNoteGroup.getGroupProperties().linkTargets);

            // These lines can be useful during development.
            //System.out.println("Serializing the new group properties:");
            //System.out.println(AppUtil.toJsonString(myProperties));
        }

        // If this NoteComponent's group has a keeper and was also viewed via the linkTargetSelectionPanel
        // during the link view/edit operation then it was pulled out of the AppTreePanel in order to be
        // shown and now the viewing pane of the main app will be empty but it will still appear to hold
        // the group.  You can see this by resizing the app; it will repaint as empty.
        // So the fix is to clear the tree selection and then reselect the current group.
        // This happens just from showing the group for target selection; the ultimate selection of a link
        // (or not) does not matter.
        int selectionRow = AppTreePanel.theInstance.getTree().getMaxSelectionRow();
        AppTreePanel.theInstance.getTree().clearSelection();
        AppTreePanel.theInstance.getTree().setSelectionRow(selectionRow);
    }

    // This method enables or disables every NoteComponent in the Panel.
    void setEditable(boolean b) {
        if(editable != b) {
            editable = b;
            loadPage(theNotePager.getCurrentPage());
        }
    }

    // Called by all contexts that make a change to the data, each time a change is made.
    //   Child classes can override if they need to intercept a data change, but in that case
    //   they should still call THIS super method so that menu items are managed correctly.
    //
    // Note that this method is called with a 'false' after a NoteGroup save, regardless of whether or not
    // the save attempt succeeded.  This disables the 'save' menu item, thereby preventing a second+ attempt to save.
    // The disabled menu item tells the user that they cannot keep trying and that
    // there is nothing more they can do.  This might give them a false sense
    // of having had a successful save, but given that we are considering a hypothetical situation along a
    // path where we already have an unanticipated error, that particular potential downside is entirely
    // acceptable, at least until it begins cropping up repeatedly.
    @Override // A NoteComponentManager interface implementation
    public void setGroupChanged(boolean b) {
        adjustMenuItems(b);
        myNoteGroup.setGroupChanged(b); // Calling the 'real' one, in the NoteGroup.
    } // end setGroupChanged


    // Provides a way to set the displayed data, vs loading it from a file.
    void showGroupData(Vector<NoteData> newGroupData) {
        myNoteGroup.noteGroupDataVector = newGroupData;
        loadPage(1);
    }

    // The base group data will be reloaded whenever this method is called.
    void loadNotesPanel() {
        lastVisibleNoteIndex = 0;

        Exception e = null;
        try { // Shouldn't only be for page 1 - see if there can be a better call-sequence.
            loadPage(1); // Always load page 1
        } catch (Exception cce) {
            // The most likely/common exception will be a ClassCastException, but it gets no
            // different handling so we only need the one catch-all 'catch'.
            e = cce;
        } // end try/catch

        if (e != null) {
            ems = "NoteGroupPanel loadNotesPanel: Error in loading the interface for " + getGroupName() + " !\n";
            ems = ems + e.toString();  // e.getMessage() just says 'null'; not good enough.
            ems = ems + "\nLoad Notes Panel operation aborted.";
            System.out.println("ems = " + ems);
            optionPane.showMessageDialog(JOptionPane.getFrameForComponent(theBasePanel),
                    ems, "Error", JOptionPane.ERROR_MESSAGE);
            //e.printStackTrace();
        } // end if
    } // end loadNotesPanel

    // This method transfers the data vector items to the onscreen
    //   components.  A 'page' specifier is used to determine which
    //   portion of the vector to display, if there are more notes
    //   than will fit on one page.  Child classes are responsible for
    //   displaying the pager control and sending page numbers higher
    //   than one when indicated.  If they do not, then only
    //   the first page of data will be available.
    void loadPage(int intPageNum) {
        //AppUtil.localDebug(true);
        boolean currentChangedState = myNoteGroup.groupChanged; // Preserve this value now, restore it after.

        // Set the indexes into the data vector -
        int maxDataIndex = myNoteGroup.noteGroupDataVector.size() - 1;
        int dataIndex = (intPageNum - 1) * pageSize;
        MemoryBank.debug("NoteGroupPanel.loadPage starting at vector data index " + dataIndex);

        lastVisibleNoteIndex = -1;
        NoteComponent tempNoteComponent;
        BaseData.loading = true; // We don't want to update the LMDs just for showing the info.
        for (int panelIndex = 0; panelIndex < pageSize; panelIndex++) {
            // The next line casts to NoteComponent.  Since the component is actually
            //   a child of NoteComponent, the 'setNoteData' method that is called
            //   later will be the child class method that is an override of the one
            //   in NoteComponent.  That behavior is critical to this operation.
            tempNoteComponent = (NoteComponent) groupNotesListPanel.getComponent(panelIndex);

            // This method may have been called directly after a Group load but can also be called
            // as a result of a change to the panel's editability.
            tempNoteComponent.setEditable(editable);

            if (dataIndex <= maxDataIndex) { // Put vector data into the interface.
                //MemoryBank.debug("  loading panel index " + panelIndex + " with data element " + dataIndex);
                tempNoteComponent.setNoteData((NoteData) myNoteGroup.noteGroupDataVector.elementAt(dataIndex)); // sets initialized to true
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
                //MemoryBank.debug("  clearing panel index " + panelIndex);

                // These three lines are an effective 'clear' of the component, without using the
                // reference that could point back to 'good' data that is elswhere in the vector.
                tempNoteComponent.makeDataObject();
                tempNoteComponent.resetComponent();
                tempNoteComponent.initialized = false;

                // And now make it invisible.
                tempNoteComponent.setVisible(false);
            }
        } // end for

        if (editable) activateNextNote(lastVisibleNoteIndex);
        MemoryBank.debug("lastVisibleNoteIndex is " + lastVisibleNoteIndex);

        // Each of the 'setNoteData' calls above would have set this to true, but this method may
        // have been called simply to handle a paging event; no real change to the data.  So -
        // having preserved the original value, we now set it back to that.
        setGroupChanged(currentChangedState);

        BaseData.loading = false;

        //AppUtil.localDebug(false);
    } // end loadPage


    // This is called from the constructor; should be overridden by
    //   child classes and those children should NOT call this one.
    JComponent makeNewNote(int i) {
        NoteComponent nc = new NoteComponent(this, i);
        nc.setVisible(false);
        return nc;
    } // end makeNewNote


    // This method is provided as a means for the pager to notify a
    //   group that the page has changed.  If this notification is
    //   needed, the child will override this no-op base method and
    //   take some action.
    protected void pageNumberChanged() {
    } // end pageNumberChanged


    // Called by the pager control
    void pageFrom(int pageFrom) {
        MemoryBank.debug("Paging away from Page: " + pageFrom);
        unloadNotesPanel(pageFrom);
    } // end pageFrom

    // Called by the pager control
    void pageTo(int pageTo) {
        MemoryBank.debug("Paging To Page: " + pageTo);
        loadPage(pageTo);
    } // end pageTo


    // This should be called prior to closing, but there are a few other cases.
    void preClosePanel() {
        if (null != extendedNoteComponent && null != defaultSubject) {
            extendedNoteComponent.saveSubjects();
        }

        // Without this condition, the existing unchanged data might get written out to the data store
        //   and that might overwrite changes that had been made and were already persisted outside of a Panel.
        if(myNoteGroup.groupChanged) {
            getPanelData(); // update the data, condense.
            myNoteGroup.saveNoteGroup();
        }

    } // end preClosePanel


    // Use this method to remove 'gaps' in the panel data.
    Vector<NoteData> getCondensedInfo() {
        Vector<NoteData> trimmedList = new Vector<>();

        // Xfer the 'good' data over to a new, temporary Vector.
        for (Object object : myNoteGroup.noteGroupDataVector) {

            // Don't retain this note if there is no significant primary text.
            NoteData tempNoteData = (NoteData) object;
            if (tempNoteData.getNoteString().trim().isEmpty()) continue;

            // Add each 'good' note to the 'keeper' list.
            trimmedList.add(tempNoteData);
        }
        return trimmedList;
    }


    void setGroupHeader(Container c) {
        jsp.setColumnHeaderView(c);
    } // end setGroupHeader

    // Some Panels have a default subject; others do not.
    // If this method is not called, defaultSubject remains null.
    // Usage convention is that this method is to be used for all value writes, but defaultSubject is
    //   package-accessible so it may be 'read' directly.
    void setDefaultSubject(String defaultSubject) {
        this.defaultSubject = defaultSubject;
    }

    @Override
    public void setStatusMessage(String s) {
        if(editable) {
            lblStatusMessage.setText("  " + s);
            lblStatusMessage.invalidate();
            theBasePanel.validate();
        }
    } // end setStatusMessage


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
//        if(!editable) return;
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
//        if(!editable) return;
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


    void sortLastMod(int direction) {

        // Preserve current interface changes before sorting.
        unloadNotesPanel(theNotePager.getCurrentPage());

        // Do the sort
        LastModComparator lmc = new LastModComparator(direction);
        myNoteGroup.noteGroupDataVector.sort(lmc);

        // Display the same page, now with possibly different contents.
        loadPage(theNotePager.getCurrentPage());
    } // end sortLastMod


    void sortNoteString(int direction) {

        // Preserve current interface changes before sorting.
        unloadNotesPanel(theNotePager.getCurrentPage());

        // Do the sort
        NoteStringComparator nsc = new NoteStringComparator(direction);
        myNoteGroup.noteGroupDataVector.sort(nsc);

        // Display the same page, now with possibly different contents.
        loadPage(theNotePager.getCurrentPage());
    } // end sortNoteString


    // This method transfers the visible notes from the page to their
    //   correct places in the data vector, and adds vector elements
    //   when needed to match the location in the interface to the data
    //   Vector.  It is needed during various events and not solely for
    //   preparation to save the group data.  For that reason
    //   we allow possible 'gaps' in data from cleared items.
    //   Gaps will be removed by the getCondensedInfo() method that is
    //   called during the 'save' process.
    void unloadNotesPanel(int currentPage) {

        // Set the indexes into the data vector -
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = startIndex + lastVisibleNoteIndex;  // last visible may or may not be initialized.
        MemoryBank.debug("NoteGroupPanel.unloadNotesPanel into vector index " + startIndex + " to " + endIndex);

        // When unloading the currently displayed interface, we need to know where the groupDataVector ends and new
        // data begins.  groupDataVector size-1 will almost always be less than endIndex
        // (because on a less-than-full page, the last visible note hasn't been typed into yet, for one thing).
        // The maxDataIndex will tell us whether a note should be replaced or just added to the end of groupDataVector.
        int maxDataIndex = myNoteGroup.noteGroupDataVector.size() - 1;

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
                myNoteGroup.noteGroupDataVector.setElementAt(tempNoteData, dataIndex);
            } else {  // New, user-entered data is in the interface.  Get it.
                if (tempNoteComponent.initialized) {  // This could be false on the last note on the page.
                    System.out.println("NoteGroupPanel.unloadNotesPanel: Adding new element!");
                    myNoteGroup.noteGroupDataVector.addElement(tempNoteData);
                }
            } // end if
        } // end for i
    } // end unloadNotesPanel


    //----------------------------------------------------
    // Method Name: updateGroup
    //
    // Repaints the display by clearing the data, then reload.
    // Called by various actions.  To preserve changes,
    //   the calling context should first call 'preClosePanel'.
    //----------------------------------------------------
    public void updateGroup() {
        BaseData.loading = true; // This needs to happen before we 'clear'.
        clearPage(); // Clears the data from the interface Components.
        BaseData.loading = false;

        // The page reset below is needed BEFORE loadGroup, in case we came here
        //   when the page number was higher than 1; a condition
        //   that may be in effect during a 'refresh' (group reload) which would
        //   cause the higher numbered page to be loaded with page
        //   one data.  So - we make sure we are on page one.
        theNotePager.reset(1);

        BaseData.loading = true;
        myNoteGroup.loadNoteGroup(); // Now we have 'new' data.
        BaseData.loading = false;

        loadNotesPanel();      // Loads the data array and interface.  This calls loadPage, which toggles BaseData.loading.
        BaseData.loading = true;
        setGroupChanged(false);
        BaseData.loading = false;

        myNoteGroup.myNoteGroupPanel = this; // The 'load' cleared this value; needs a reset.

        // Also needed AFTER loadGroup, not to set the page number but to set the total number of
        //   pages, which will be shown in the pager control and may have changed due to the load.
        theNotePager.reset(1);

    } // end updateGroup


    // Called by AppTreePanel in response to user selection of the menu item to save the group.
    protected void refresh() {
        preClosePanel();     // Save any in-progress changes
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


    // Used to enable or disable the 'undo' and 'save' menu items.  Called once when the
    // list menu is initially set and then later called repeatedly for every 'setGroupChanged'.
    // Although the boolean currently exactly matches the 'groupChanged' variable, taking it as an input
    // parameter allows it to be based on other criteria (at some future point.  I know, yagni).
    protected void adjustMenuItems(boolean b) {
        if(myListMenu == null) return; // Too soon.  Come back later.
        MemoryBank.debug("NoteGroupPanel.adjustMenuItems <" + b + ">");

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
        MemoryBank.debug("NoteGroupPanel.setListMenu: " + listMenu.getText());
        myListMenu = listMenu;
        adjustMenuItems(myNoteGroup.groupChanged); // set enabled state for 'undo' and 'save'.
    }


} // end class NoteGroupPanel
