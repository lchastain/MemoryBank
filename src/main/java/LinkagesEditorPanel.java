import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    NoteData editorNoteData; // Isolate the link additions and deletions in case of a 'cancel'.
    GroupProperties sourceGroupProperties;
    boolean deleteCheckedLinks;
    LinkTargets linkTargets;


    static {
        optionPane = new Notifier() { }; // Uses all default methods.
    }

    LinkagesEditorPanel(GroupProperties sourceGroupProperties, NoteData sourceNoteData) {
        this(); // build the panel, make the action listener.

        deleteCheckedLinks = true;
        this.sourceNoteData = sourceNoteData;
        this.sourceGroupProperties = sourceGroupProperties;
        editorNoteData = new NoteData();
        String theCategory = sourceGroupProperties.groupType.toString();
        if (theCategory.endsWith(" Note")) theCategory = GroupType.NOTES.toString();
        editorNoteData.noteString = theCategory + ": " + sourceGroupProperties.getGroupName();


        // Isolate the source entity from changes the user makes here until and unless they are
        // accepted, by first cloning our own copy of its linkTargets.
        if(sourceNoteData != null) {
            linkTargets = (LinkTargets) sourceNoteData.linkTargets.clone();
            editorNoteData.noteString += " - " + sourceNoteData.noteString;
        } else {
            linkTargets = (LinkTargets) sourceGroupProperties.linkTargets.clone();
        }

        filterLinkages(); // Here is where we pre-groom the linkages list.
        rebuildDialog();
    }

    private LinkagesEditorPanel() {
        super(new BorderLayout());

        // The center of this panel will hold this JScrollPane.
        // Here is where we set our fixed width, so that horizontal scrolling will not be needed.
        // If you see a little arrow below, hover on it (or click) to expand the declaration and see the full code.
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


    // This method works on the incoming linkages to remove bad/obsolete links, flag
    // text where it may have changed, and to hide those links that should not be shown.
    // Call this prior to the first showing of the dialog.
    // Operations:
    //   1.  Remove any old-style links that do not have targetGroupInfo.
    //   2.  Do not allow the Type of pre-existing links to be changed.
    //   3.  Turn off the 'showMe' flag for targets where the group is found, but not currently active.
    //   4.  Flag any links that point to a non-existent target (either group or note).
    //   5.  Set flags for detected changes in target info - group renamed (if we can detect that), note text changed.
    //          will probably require the (first) use of their IDs.
    void filterLinkages() {
        LinkTargets filteredLinkTargets = new LinkTargets();

        for(LinkedEntityData linkedEntityData : linkTargets) {
            if (linkedEntityData.getTargetGroupInfo() == null) continue;  // (1)
            linkedEntityData.retypeMe = false; // (2)

            // TODO code in (3), (4), (5)
            filteredLinkTargets.add(linkedEntityData);
        }

        // Swap out the original list for our newly-groomed one.
        linkTargets = filteredLinkTargets;
    } // end filterLinkages


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

                LinkTargetSelectionPanel linkTargetSelectionPanel = new LinkTargetSelectionPanel(sourceGroupProperties, editorNoteData.noteString);
                NoteComponent.mySelectionMonitor = linkTargetSelectionPanel;

                // Show the choices of groups / notes to which to make this link.
                // The dialog that is shown will have its own OK/CANCEL, and an OK there will return us to here
                //   with a new choice having been defined.
                int choice = optionPane.showConfirmDialog(
                        addLinkButton,
                        linkTargetSelectionPanel,
                        "New Link Selection",
                        JOptionPane.OK_CANCEL_OPTION, // Option type
                        JOptionPane.PLAIN_MESSAGE);    // Message type

                // Restore the original Note selection monitor.
                NoteComponent.mySelectionMonitor = originalSelectionMonitor;

                if (choice == JOptionPane.OK_OPTION) {
                    // This event comes from selecting a new link; it is NOT the 'OK' for this panel.
                    // The 'OK' for this panel is handled where it was invoked.

                    // If no Group selection then just go back. (but not sure how this could happen) -
                    if (linkTargetSelectionPanel.selectedTargetGroupPanel == null) return;

                    // Disallow a same-group link targetselection.  The LinkTargetSelectionPanel will have
                    // already stopped this for notes within CalendarNoteGroups and all other group types,
                    // but not for a CalendarNoteGroup-only selection.
                    String groupId = linkTargetSelectionPanel.selectedTargetGroupPanel.myNoteGroup.getGroupProperties().instanceId.toString();
                    if(groupId.equals(sourceGroupProperties.instanceId.toString())) {
                        String ems = "You cannot make a link to the same group!";
                        JOptionPane.showMessageDialog(addLinkButton,
                                ems, "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // First - capture any link type or ordering changes in the existing list.
                    deleteCheckedLinks = false;
                    // This will update all pre-existing link types; they could have changed between
                    // link additions, when this dialog is used to add more than just one.
                    updateLinkagesFromEditor();
                    deleteCheckedLinks = true;

                    // Get the Group and Note selections
//                    GroupProperties selectedGroupProperties = linkTargetSelectionPanel.selectedTargetGroup.myNoteGroup.getGroupProperties();
                    GroupInfo selectedGroupInfo = new GroupInfo(linkTargetSelectionPanel.selectedTargetGroupPanel.myNoteGroup.getGroupProperties());
                    NoteData selectedNoteData = linkTargetSelectionPanel.selectedNoteData;

                    LinkedEntityData linkedEntityData;
                    if (selectedNoteData == null) {
                        linkedEntityData = new LinkedEntityData(selectedGroupInfo, null);
                    } else {
                        NoteInfo selectedNoteInfo = new NoteInfo(selectedNoteData);
                        linkedEntityData = new LinkedEntityData(selectedGroupInfo, selectedNoteInfo);
                    }
                    linkTargets.add(linkedEntityData);
                    rebuildDialog();
                }
            }
        };
    } // end makeActionListener


    // This method is called initially to display all currently existing links, and is then called after each
    // link is added.  Links are only added one at a time.  The display is rebuilt each time.
    // Works on a pre-defined NoteData called 'editorNoteData'
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
        String downsizedNote = editorNoteData.noteString;
        if (downsizedNote.endsWith(".")) {
            // Take off a final '.', if there is one.
            downsizedNote = downsizedNote.substring(0, downsizedNote.length() - 1);
        }
        if (downsizedNote.length() > 66) {
            // The length cutoff number chosen here is somewhat arbitrary, depends on fonts, etc.  Sorry.
            downsizedNote = downsizedNote.substring(0, 62) + "...";
        }
        int linkCount = linkTargets.size();
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
        for (LinkedEntityData linkedEntityData : linkTargets) {
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

    // Given a pre-existing 'forward' link that is currently in the process of being removed by the user, we
    //  automatically remove its corresponding reverse link from the target group/note, if possible.  This is because
    //  this panel is the only creator of reverse links, so this method is just its way of cleaning up after itself.
    // As for automatically removing the corresponding forward link when the user is removing a reverse link - we don't
    //  do that because the user must clean up afer themselves.  For that they have the option to manually remove any
    //  link they choose, regardless of its directionality.
    // There are some cases below where the operation just ends because it was not possible to continue.  Given that
    //  this entire action is 'under the hood', the user will not be informed of this and it will not be treated as
    //  a problem; if removal was not possible then a strong possibility is that it just wasn't needed, such as in the
    //  case where the user had previously already removed the reverse link themselves, or the entire target group
    //  had been deleted.  If it wasn't removed because it just couldn't be found then it may still be 'out there'
    //  somewhere but its presence will be low-to-no impact and the user may remove it themselves when/if they ever
    //  find it.  As it is, we intend to allow orphaned links to exist but when shown in this panel they will have
    //  a different color so that the user can see that it has a problem of some kind.
    private void removeReverseLink(LinkedEntityData linkedEntityData) {
        // The link will either be directly on a group, or on a note within a group.
        // The first step here will be to obtain the GroupInfo for that target group.
        GroupInfo otherEndGroupInfo = linkedEntityData.getTargetGroupInfo();

        // Use the target GroupInfo to get a reference to the group that holds the reverse link.
        NoteGroup groupToSave = otherEndGroupInfo.getNoteGroup();
        if(groupToSave == null) return; // if that didn't work then we're done here; removal not possible.

        // Identify the 'holder' of the reverse link - is it the Group itself, or one of its Notes?
        LinkTargets linkTargets;
        NoteInfo otherEndNoteInfo = linkedEntityData.getTargetNoteInfo();
        if (otherEndNoteInfo != null) {
            linkTargets = groupToSave.getLinkTargets(otherEndNoteInfo);
        } else {
            linkTargets = groupToSave.getGroupProperties().linkTargets;
        }

        // Remove the link from its 'holder'.
        // This removal via our local 'linkTargets' reference will be felt in the groupToSave,
        //   whether or not it is currently being 'kept'.
        //
        // One thing we know about the reverse link is that it will have the same ID as the forward link
        //   that was sent to this method.  So in fact that is all we need to send to the 'removeLink' method.
        boolean removedLink = linkTargets.removeLink(linkedEntityData.instanceId);

        // re-persist the group, now without the one removed link.
        if(removedLink) groupToSave.saveNoteGroup();
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
        linkTargets.clear();
        boolean deleteIt;

        // This will fix any reordering, update the link type (if needed) of 'new' links, and drop out deletions.
        for (int i = 0; i < numLinks; i++) {
            LinkNoteComponent linkNoteComponent = (LinkNoteComponent) groupNotesListPanel.getComponent(i);
            deleteIt = deleteCheckedLinks && linkNoteComponent.deleteCheckBox.isSelected();
            if (deleteIt) {
                // It will be deleted simply by not including it in the final linkTargets list, but we also
                // want to remove its reverse link, if this is a forward one.
                LinkedEntityData linkedEntityData = linkNoteComponent.myLinkedEntityData;
                if (!linkedEntityData.reversed && !linkedEntityData.retypeMe) { // If this is a pre-existing forward link -
                    removeReverseLink(linkedEntityData);
                }
            } else {
                // This will set the link type according to the combobox value.
                // (it also sets 'deleteMe' to false, if needed, but thanks to the other part of this
                //   conditional branch, that only matters when doing a 'swap').
                linkTargets.add(linkNoteComponent.getLinkTarget());
            }

        }
    }

}


