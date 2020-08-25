import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

public class LinkagesEditorPanel extends JPanel implements NoteComponentManager {
    static final long serialVersionUID = 1L;
    static Notifier optionPane;
    JPanel groupNotesListPanel;
    int lastVisibleNoteIndex = -1;
    JScrollPane jsp;
    JScrollBar jsb;
    JButton addLinkButton; // Declared here to give test access.
    ActionListener addButtonActionListener;
    NoteData sourceNoteData;
    NoteData editedNoteData; // Isolate the edits in case of a 'cancel'.
    GroupProperties sourceGroupProperties;
    GroupProperties editedGroupProperties;
    boolean deleteCheckedLinks;

    static {
        optionPane = new Notifier() { }; // Uses all default methods.
    }

    // TODO  After a way is provided to edit linkages of groups, need to handle the case where the
    //  incoming NoteData might be null.  For now, that does not happen.
    LinkagesEditorPanel(GroupProperties sourceGroupProperties, NoteData sourceNoteData) {
        this(); // build the panel, make the action listener.

        deleteCheckedLinks = true;
        this.sourceNoteData = sourceNoteData;
        this.sourceGroupProperties = sourceGroupProperties;

        // Isolate the source entity from changes the user makes here until
        // and unless they are accepted when the dialog is dismissed.
        editedNoteData = sourceNoteData.copy();
        editedGroupProperties = sourceGroupProperties.copy();

        filterLinkages(); // Here is where we pre-groom the linkages list.
        rebuildDialog();
    }

    private LinkagesEditorPanel() {
        super(new BorderLayout());

        // The center of this panel will hold this JScrollPane.
        // Here is where we set our fixed width, so that horizontal scrolling will not be needed.
        // Expand this section to see the 'real' code.
        jsp = new JScrollPane() {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(800, super.getPreferredSize().height);
            }
        };

        // Explicitly define the vertical scrollbar to allow for these additional settings:
        //  Scroll increments, and also disable focus or else once the bar appears, the tab
        //  and up/down keys can transfer focus over to it, and the up/down keys do not bring
        //  it back.  But even if they did, that's not the behavior we would want, so - no focus.
        jsb = new JScrollBar();
        jsb.setFocusable(false);
        jsb.setUnitIncrement(NoteComponent.NOTEHEIGHT);
        jsb.setBlockIncrement(NoteComponent.NOTEHEIGHT);

        // Assign both scrollbars of the scrollpane.
        jsp.setVerticalScrollBar(jsb);
        jsp.setHorizontalScrollBar(null); // We use a set width.

        makeActionListener();
    } // end constructor

    @Override
    public void activateNextNote(int index) {
        // Not used by this Panel; no new links allowed.
        // At least, not allowed in real-time.  A new link will be
        // added to the main list and then this panel could be
        // redisplayed, showing it at that time.
    }


    // Make a 'reverse' link from a 'forward' one.
    // Our source entity will now be the target.
    // Then attach the link to the current target, and persist.
    public void addReverseLink(LinkedEntityData linkedEntityData) {
        NoteData otherEndNoteData = linkedEntityData.getTargetNoteData();
        GroupProperties otherEndGroupProperties = linkedEntityData.getTargetGroupProperties();
        if (otherEndGroupProperties == null) return; // Don't see how this could have happened, but this handles it.

        // Create the reverse link
        //--------------------------
        // First, just a standard 'forward' link, where our source entity is now the target.
        LinkedEntityData reverseLinkedEntityData = new LinkedEntityData(sourceGroupProperties, sourceNoteData);

        // Then give it a type that is the reverse of the forward link's type -
        reverseLinkedEntityData.linkType = linkedEntityData.reverseLinkType(linkedEntityData.linkType);

        // and then raise the 'reversed' flag.
        reverseLinkedEntityData.reversed = true;

        // Attach the reversed link to the right place
        if (otherEndNoteData != null) {
            otherEndNoteData.linkTargets.add(reverseLinkedEntityData);
        } else {
            otherEndGroupProperties.linkTargets.add(reverseLinkedEntityData);
        }

        // Persist the new reverse link.
        NoteGroup groupToSave = otherEndGroupProperties.getGroup();
        // previously saveNoteGroup was only done from preClose - now for this usage it is no longer a private method
        // - verify this is GOOD.
        // And maybe consolidate the work from AppUtil, where a note can be moved or copied to Day, from Todo or Event.
        if (groupToSave != null) groupToSave.saveNoteGroup();
    }

    // Cycle through the list of linkages to find the 'new' ones,
    //   and add a reverse link for each one.
    public void addReverseLinks(Vector<LinkedEntityData> linkages) {
        for (LinkedEntityData linkedEntityData : linkages) {
            // We don't add reverse links if the forward link preexisted, and we determine that based
            // on whether or not we are allowed to change its type.  And since no reverse
            // link is allowed to be re-type'd, this also prevents a reverse link from being
            // created for a link that is itself already a reverse link.
            if (!linkedEntityData.retypeMe) continue;

            addReverseLink(linkedEntityData);
        }

    }


    // Not used, but needed for the complete NoteComponentManager implementation.
    @Override
    public boolean editExtendedNoteComponent(NoteData tmpNoteData) {
        return true;
    }

    // This method works on the <editedLinkHolder> to remove bad/obsolete links, update
    // text where it may have changed, and to flag those links that should not be shown.
    // Call this prior to the first showing of the dialog.
    // Operations:
    //   1.  Remove any old-style links that do not have targetGroupInfo.
    //   2.  Do not allow the Type of pre-existing links to be changed.
    //   3.  Turn off the 'showMe' flag for targets where the group is found, but not currently active.
    //   4.  Flag any links that point to a non-existent target (either group or note).
    //   5.  Set flags for detected changes in target info - group renamed (if we can detect that), note text changed.
    //          will probably require the (first) use of their IDs.
    void filterLinkages() {
        Vector<LinkedEntityData> linkTargets = new Vector<>(0, 1);

        for (LinkedEntityData linkedEntityData : editedNoteData.linkTargets) {
            if (linkedEntityData.getTargetGroupInfo() == null) continue;  // (1)
            linkedEntityData.retypeMe = false; // (2)

            // TODO code in (3), (4), (5)
            // for test purposes, may want a transient 'filterMe?' boolean, that will evade 3 & 4.
            linkTargets.add(linkedEntityData);
        }

        // Working on the isolated copy of the source entity,
        // swap out the original list for our newly-groomed one.
        editedNoteData.linkTargets = linkTargets;
    }

    @Override
    public int getLastVisibleNoteIndex() {
        return lastVisibleNoteIndex;
    }

    @Override
    public Dimension getPreferredSize() {
        // Width of the panel is controlled by width of the JScrollPane.
        return new Dimension(super.getPreferredSize().width, 220);
    } // end getPreferredSize


    // Define the listener for the 'Add New Link' clickable label.
    void makeActionListener() {
        addButtonActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                // Since the NoteComponent selection monitor is a static setting, we need to
                // preserve it before changing it here, so we can set it back when done
                // (but currently the only other monitor in use is the default, no-op handler).
                NoteSelection originalSelectionMonitor = NoteComponent.mySelectionMonitor;

                LinkTargetSelectionPanel linkTargetSelectionPanel = new LinkTargetSelectionPanel(editedNoteData);
                NoteComponent.mySelectionMonitor = linkTargetSelectionPanel;

                // Show the choices of groups / notes to which to make this link.
                // The dialog that is shown will have its own OK/CANCEL, and an OK there will return us to here
                //   with a new choice having been defined.
                int choice = optionPane.showConfirmDialog(
                        null,
                        linkTargetSelectionPanel,
                        "New Link Selection",
                        JOptionPane.OK_CANCEL_OPTION, // Option type
                        JOptionPane.PLAIN_MESSAGE);    // Message type

                // Restore the original Note selection monitor.
                NoteComponent.mySelectionMonitor = originalSelectionMonitor;

                if (choice == JOptionPane.OK_OPTION) {
                    // This event comes from selecting a new link; it is NOT the 'OK' for this panel.
                    // The 'OK' for this panel is handled where it was invoked.

                    // First - capture any link type or ordering changes in the existing list.
                    deleteCheckedLinks = false;
                    // This will update all pre-existing link types; they could have changed between
                    // link additions, when this dialog is used to add more than just one.
                    updateLinkagesFromEditor();
                    deleteCheckedLinks = true;

                    if (linkTargetSelectionPanel.selectedTargetGroup == null)
                        return; // No Group selection - just go back.

                    // Get the Group and Note selections
                    GroupProperties selectedGroupProperties = linkTargetSelectionPanel.selectedTargetGroup.getGroupProperties();
                    NoteData selectedNoteData = linkTargetSelectionPanel.selectedNoteData;

                    LinkedEntityData linkedEntityData;
                    if (selectedNoteData == null) {
                        linkedEntityData = new LinkedEntityData(selectedGroupProperties);
                    } else {
                        linkedEntityData = new LinkedEntityData(selectedGroupProperties, selectedNoteData);
                    }
                    editedNoteData.linkTargets.add(linkedEntityData);
                    rebuildDialog();
                }
            }
        };
    }


    // This method is called initially to display all currently existing links, and is then called after each
    // link is added.  Links are only added one at a time.  The display is rebuilt each time.

    // Works on a pre-defined NoteData called 'editedNoteData', but when we start to work on Groups as well,
    // this will break.  At that time, need to work on a variable that implemnts an interface that provides
    // the linkTargets.  Suggested interface name:  LinkHolder.  Suggested entity name:  linkHolder
    private void rebuildDialog() {
        removeAll();
        revalidate();

        // Declare the panel to hold the header information.
        // The header will contain one or two JLabels.
        JPanel headerInfoPanel = new JPanel(new BorderLayout());

        // Declare and decorate the labels that will be shown in the header.
        JLabel sourceInfoLabel = new JLabel();
        sourceInfoLabel.setFont(Font.decode("Dialog-bold-14"));
        JLabel userInstructionsLabel = new JLabel();
        userInstructionsLabel.setFont(Font.decode("Dialog-12"));

        // Variable messages and instructions, depending on how many links already exist.
        String theUserInstructions;
        String theSourceNoteText;
        String downsizedNote = editedNoteData.noteString;
        if (downsizedNote.endsWith(".")) {
            // Take off a final '.', if there is one.
            downsizedNote = downsizedNote.substring(0, downsizedNote.length() - 1);
        }
        if (downsizedNote.length() > 66) {
            // The length cutoff number chosen here is somewhat arbitrary, depends on fonts, etc.  Sorry.
            downsizedNote = downsizedNote.substring(0, 62) + "...";
        }
        int linkCount = editedNoteData.linkTargets.size();
        if (linkCount == 0) {
            theSourceNoteText = "There are no existing links for " + AppUtil.makeRed(downsizedNote);
            theSourceNoteText += ".  Click on 'Add New Link' to add one.";
        } else if (linkCount == 1) {
            theUserInstructions = "Use the dropdown to change the link type, or put a check in the checkbox to mark it for deletion";
            userInstructionsLabel.setText(theUserInstructions);
            headerInfoPanel.add(userInstructionsLabel, BorderLayout.NORTH);
            theSourceNoteText = "Existing link - " + AppUtil.makeRed(downsizedNote) + " IS:";
        } else {
            theUserInstructions = "Checked links will be deleted.  " +
                    "You can move a highlighted link by shift-up or shift-down arrow.\n";
            userInstructionsLabel.setText(theUserInstructions);
            headerInfoPanel.add(userInstructionsLabel, BorderLayout.NORTH);
            theSourceNoteText = "Existing links - " + AppUtil.makeRed(downsizedNote) + " IS:";
        }
        headerInfoPanel.add(sourceInfoLabel, BorderLayout.SOUTH);
        sourceInfoLabel.setText(AppUtil.makeHtml(theSourceNoteText));
        add(headerInfoPanel, BorderLayout.NORTH);

        // Fill in the list of links (if any)
        groupNotesListPanel = new JPanel();
        groupNotesListPanel.setLayout(new BoxLayout(groupNotesListPanel, BoxLayout.Y_AXIS));
        int index = 0;
        for (LinkedEntityData linkedEntityData : editedNoteData.linkTargets) {
            LinkNoteComponent linkNoteComponent = new LinkNoteComponent(this, linkedEntityData, index++);
            linkNoteComponent.resetComponent(); // To properly colorize text and set tooltip, if there is extended note.
            groupNotesListPanel.add(linkNoteComponent);
            lastVisibleNoteIndex++;
        }

        jsp.setViewportView(groupNotesListPanel);
        jsb.setValue(jsb.getMaximum());  // Scroll to the bottom - any new item will be here.
        add(jsp, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new DndLayout()); // This layout keeps the content left-justified.
        addLinkButton = new JButton("<html><u>Add New Link</u></html>");

        addLinkButton.addActionListener(addButtonActionListener);
        addLinkButton.setFocusable(false);
        // Changed from a 'button-style' look to more of a web-page link, because the
        // JButton was pushed right up against the bottom of the JScrollPane and it
        // was visually displeasing.  Margins didn't help because they just made a
        // bigger button, didn't move it down just a bit, like the border does, here.
        // But now the button has no border at all, so 'linkifying' it with color and
        // underline.
        addLinkButton.setBorder(new EmptyBorder(8, 10, 1, 1));
        addLinkButton.setForeground(Color.BLUE);
        addLinkButton.setFont(Font.decode("Dialog-bold-12"));

        southPanel.add(addLinkButton);
        add(southPanel, BorderLayout.SOUTH);
    }

    public void shiftDown(int index) {
        if (index >= (getLastVisibleNoteIndex())) return;

        //System.out.println("Shifting note down");
        NoteComponent nc1, nc2;
        nc1 = (NoteComponent) groupNotesListPanel.getComponent(index);
        nc2 = (NoteComponent) groupNotesListPanel.getComponent(index + 1);

        nc1.swap(nc2);
        nc2.setActive();
    } // end shiftDown


    public void shiftUp(int index) {
        // Prevent the first note on the page from shifting up.
        if (index == 0) return;

        //System.out.println("Shifting note up");
        NoteComponent nc1, nc2;
        nc1 = (NoteComponent) groupNotesListPanel.getComponent(index);
        nc2 = (NoteComponent) groupNotesListPanel.getComponent(index - 1);

        nc1.swap(nc2);
        nc2.setActive();
    } // end shiftUp


    // Cycle through the interface and rebuild the linkages.
    void updateLinkagesFromEditor() {
        int numLinks = groupNotesListPanel.getComponentCount();
        editedNoteData.linkTargets.clear();
        boolean deleteIt;

        // This will fix any reordering, update the link type (if needed) of 'new' links, and drop out deletions.
        for (int i = 0; i < numLinks; i++) {
            LinkNoteComponent linkNoteComponent = (LinkNoteComponent) groupNotesListPanel.getComponent(i);
            deleteIt = deleteCheckedLinks && linkNoteComponent.deleteCheckBox.isSelected();
            if (deleteIt) {
                // It will be deleted simply by not including it in the results, but we also want to
                // remove its reverse link, if this is a forward one.
                if (!linkNoteComponent.myLinkedEntityData.reversed) { // If this is a forward link -
                    System.out.println("NOT YET deleting a reverse link!!!");
                    // But we only need to do that if this is a pre-existing link, not needed for a 'new' one.
                }
            } else {
                // This will set the link type according to the combobox value.
                // (it also sets 'deleteMe' to false, if needed, but thanks to the other part of this
                //   conditional branch, that only matters when doing a 'swap').
                editedNoteData.linkTargets.add(linkNoteComponent.getLinkTarget());
            }

        }
    }

}


