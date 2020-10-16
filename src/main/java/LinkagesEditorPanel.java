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

    LinkagesEditorPanel(GroupProperties sourceGroupProperties) {
        this(); // build the panel, make the action listener.

        deleteCheckedLinks = true;
        sourceNoteData = null;
        this.sourceGroupProperties = sourceGroupProperties;

        // Isolate the source entity from changes the user makes here until
        // and unless they are accepted when the dialog is dismissed.
        linkTargets = (LinkTargets) sourceGroupProperties.linkTargets.clone();

        editorNoteData = new NoteData();
        editorNoteData.noteString = sourceGroupProperties.getCategory() + ": " + sourceGroupProperties.getGroupName();

        filterLinkages(); // Here is where we pre-groom the linkTargets list.
        rebuildDialog();
    }

    LinkagesEditorPanel(GroupProperties sourceGroupProperties, NoteData sourceNoteData) {
        this(); // build the panel, make the action listener.

        deleteCheckedLinks = true;
        this.sourceNoteData = sourceNoteData;
        this.sourceGroupProperties = sourceGroupProperties;

        // Isolate the source entity from changes the user makes here until
        // and unless they are accepted when the dialog is dismissed.
        linkTargets = (LinkTargets) sourceNoteData.linkTargets.clone();

        editorNoteData = new NoteData();
        editorNoteData.noteString = sourceGroupProperties.getCategory() + ": " + sourceGroupProperties.getGroupName();
        editorNoteData.noteString += " - " + sourceNoteData.noteString;

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

    // Create a reverse link from this 'forward' one, then attach it to the current target, and persist.
    // This method is called in the same event sequence in which the forward link was just created, so we are
    // confident that the two transient members of the link are populated correctly and that the indicated
    // group or group+note is available.
    private void addReverseLink(LinkedEntityData linkedEntityData) {
        GroupInfo otherEndGroupInfo = linkedEntityData.getTargetGroupInfo();
        NoteInfo otherEndNoteInfo = linkedEntityData.getTargetNoteInfo();

        // If somehow our above-noted confidence was misplaced - complain loudly.
        assert otherEndGroupInfo != null;

        // Use the target GroupProperties to get a reference to the group's data accessor
        NoteGroupDataAccessor groupToSave = otherEndGroupInfo.getNoteGroupDataAccessor();
        // The Group to save will not be null - because in order for the target to have been selected, its Group
        // will have first been displayed in a Panel, during this very same session.

        // But again, if somehow our above-noted confidence is misplaced - complain loudly.
        assert groupToSave != null;
        // And not to worry - if this group is active in the app and had some unsaved changes, those changes
        // will have been preserved by the LinkTargetSelectionPanel's call to refresh() prior to presenting the group
        // as the potential link target.

        // Create the reverse link
        LinkedEntityData reverseLinkedEntityData = createReverseLink(linkedEntityData);

        // Attach the reversed link to the right place, and re-persist the Group.
        if (otherEndNoteInfo != null) {
            groupToSave.getLinkTargets(otherEndNoteInfo).add(reverseLinkedEntityData);
//            ((NoteGroupPanel) groupToSave).setGroupChanged(true);
//            ((NoteGroupPanel) groupToSave).preClosePanel(); // Saving this way will update the group's internal data first.
        } else {
            groupToSave.getGroupProperties().linkTargets.add(reverseLinkedEntityData);
        }

        // Use the data accessor method to persist the Group with the new reverse link.
        groupToSave.saveNoteGroupData(); // This saves the updated Group.
    } // and addReverseLink


    // Cycle through the list of linkages to find the 'new' ones,
    //   and add a reverse link for each one.
    void addReverseLinks(LinkTargets linkages) {
        for (LinkedEntityData linkedEntityData : linkages) {
            // Since we are looking through ALL links to see if we need to handle new ones, we will also see the
            // ones (if any) that were already there.   In those cases we don't want to add a reverse link for them
            // because that already happened when they first appeared, and we don't want them to pile up; only one
            // reverse link per forward link is allowed.  We can know that a forward link pre-existed based
            // on whether or not we are allowed to change its type, because only new links are allowed to be
            // re-'typed'.  And since no reverse link is allowed to be re-typed, the same logic prevents a reverse
            // link from being created for a link that is itself already a reverse link, regardless of whether or
            // not it is new.
            if (!linkedEntityData.retypeMe) continue;

            addReverseLink(linkedEntityData);
        }

    }

    // Make a 'reverse' link from a 'forward' one.
    // Our source entity will now be the target.
    // (I thought I had a second usage for this method, while auto-deleting reverse links; now - not so sure about that).
    LinkedEntityData createReverseLink(LinkedEntityData linkedEntityData) {
        // First, just a standard 'forward' link, where our source entity is now the target.
        LinkedEntityData reverseLinkedEntityData;
        if(sourceNoteData != null) {
            reverseLinkedEntityData = new LinkedEntityData(sourceGroupProperties, sourceNoteData);
        } else {
            reverseLinkedEntityData = new LinkedEntityData(sourceGroupProperties);
        }

        // But now - give it the same ID as the forward one.  This will help with any
        // subsequent operations where the two will need to be 'in sync'.
        reverseLinkedEntityData.instanceId = linkedEntityData.instanceId;

        // Then give it a type that is the reverse of the forward link's type -
        reverseLinkedEntityData.linkType = linkedEntityData.reverseLinkType(linkedEntityData.linkType);

        // and then raise the 'reversed' flag.
        reverseLinkedEntityData.reversed = true;

        return reverseLinkedEntityData;
    }


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
                    if (linkTargetSelectionPanel.selectedTargetGroup == null) return;

                    // Disallow a same-group link targetselection.  The LinkTargetSelectionPanel will have
                    // already stopped this for notes within CalendarNoteGroups and all other group types,
                    // but not for a CalendarNoteGroup-only selection.
                    String groupId = linkTargetSelectionPanel.selectedTargetGroup.getGroupProperties().instanceId.toString();
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
                    GroupProperties selectedGroupProperties = linkTargetSelectionPanel.selectedTargetGroup.getGroupProperties();
                    NoteData selectedNoteData = linkTargetSelectionPanel.selectedNoteData;

                    LinkedEntityData linkedEntityData;
                    if (selectedNoteData == null) {
                        linkedEntityData = new LinkedEntityData(selectedGroupProperties);
                    } else {
                        linkedEntityData = new LinkedEntityData(selectedGroupProperties, selectedNoteData);
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
    //  had been deleted.
    private void removeReverseLink(LinkedEntityData linkedEntityData) {
        // The link will either be directly on a group, or on a note within a group.
        // The first step here will be to obtain the GroupInfo for that target group.
        GroupInfo otherEndGroupInfo = linkedEntityData.getTargetGroupInfo();

        // The difference between the linkedEntityData that was sent to the addReverseLink method and the one that is
        //  sent here is that the one sent here does not immediately follow the selection of the link target.

        //  with correctly populated
        //  transient member instances; it most commonly comes from deserialized data that has no handle to a
        //  panel that holds the data for this group.
        // A less common operational variant is that the transient members ARE already populated, because the forward
        //  link was created and persisted in the current session, but then was recalled and is now being deleted.
        //  An unusual situation but certainly not impossible.  But unlike the sequencing when the reverse link
        //  was added, here we cannot guarantee that further user changes to the target group did not occur between
        //  the time the link was created and now when it is being removed, and if there were changes, using the
        //  reference at this point would give us the group without getting its data updated from the associated panel.
        // So, we need to use the GroupInfo to get an accessor for the data because whether the data needs updating or
        //  not, this way we are sure we are getting a group with the most current data.  There is even more verbose
        //  commenting on this in the getNoteGroupDataAccessor method, looking at the GroupInfo from the perspective
        //  of the GroupInfo class itself.

        // Use the target GroupInfo to get a reference to the group's data accessor
        NoteGroupDataAccessor groupToSave = otherEndGroupInfo.getNoteGroupDataAccessor();
        if(groupToSave == null) return; // if that didn't work then we're done here; removal not possible.

        // Remove the link from its 'holder'.
        LinkTargets linkTargets;
        NoteInfo otherEndNoteInfo = linkedEntityData.getTargetNoteInfo();
        if (otherEndNoteInfo != null) {
            linkTargets = groupToSave.getLinkTargets(otherEndNoteInfo);
        } else {
            linkTargets = groupToSave.getGroupProperties().linkTargets;
        }

        // This removal will pass through our local reference and be felt in the group.
        boolean removedLink;
        removedLink = linkTargets.remove(linkedEntityData); // Probably need a new 'remove' method in LinkTargets class.
        // but may not, if you make an 'equals' method for LinkedEntityData ??

        // re-persist the group, now without the one removed link.
        if(removedLink) groupToSave.saveNoteGroupData();

        // If it came from a keeper, refresh.
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
//                    removeReverseLink(linkedEntityData);
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


