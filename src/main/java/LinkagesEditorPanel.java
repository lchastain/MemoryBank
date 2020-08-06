import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

public class LinkagesEditorPanel extends JPanel implements NoteComponentManager {
    static final long serialVersionUID = 1L;
    JPanel groupNotesListPanel;
    int lastVisibleNoteIndex = -1;
    JScrollPane jsp;
    JScrollBar jsb;
    ActionListener addButtonActionListener;
    NoteData sourceNoteData;
    NoteData editedNoteData; // Isolate the edits in case of a 'cancel'.
    GroupProperties sourceGroupProperties;
    GroupProperties editedGroupProperties;
    boolean deleteCheckedLinks;

    // TODO  After a way is provided to edit linkages of groups, need to handle the case where the
    //  incoming NoteData might be null.  For now, that does not happen.
    LinkagesEditorPanel(GroupProperties sourceGroupProperties, NoteData sourceNoteData) {
        this(); // build the panel, make the action listener.

        deleteCheckedLinks = true;
        this.sourceNoteData = sourceNoteData;
        this.sourceGroupProperties = sourceGroupProperties;
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
    // But this comes too soon after forward creation to have a specific type, so
    //    it cannot be reversed; type will remain the default 'related'.
            // OR - make this call later in the process.  considering that..   it would help with the 'undo' feature.
    // Then attach the link to the current target, and persist.
    public void addReverseLink(LinkedEntityData linkedEntityData) {
        NoteData otherEndNoteData = linkedEntityData.getTargetNoteData();
        GroupProperties otherEndGroupProperties = linkedEntityData.getTargetGroupProperties();
        if(otherEndGroupProperties == null) return; // Don't see how this could have happened, but this handles it.

        // Create the reverse link
        LinkedEntityData reverseLinkedEntityData = new LinkedEntityData(sourceGroupProperties, sourceNoteData);

        // Attach the link to the right place
        if(otherEndNoteData != null) {
            otherEndNoteData.linkTargets.add(reverseLinkedEntityData);
        } else {
            otherEndGroupProperties.linkTargets.add(reverseLinkedEntityData);
        }

        // Persist the new reverse link.
        NoteGroup groupToSave = otherEndGroupProperties.getGroup();
        if(groupToSave != null) groupToSave.saveNoteGroup();  // previously only done from preClose - now no longer a private method - verify this is GOOD.
    }

    // Not needed or used.
    @Override
    public boolean editExtendedNoteComponent(NoteData tmpNoteData) {
        return true;
    }

    // This method works on the editedLinkHolder to remove bad/obsolete links, update
    // text where it may have changed, and to flag those links that should not be shown.
    // Call this prior to the first showing of the dialog.
    // Operations:
    //   1.  Remove any old-style links that do not have targetGroupInfo
    //   2.  Turn off the 'showMe' flag for targets that are not currently active.
    //   3.  Remove any links that point to a non-existent target or have a non-existent source.
    //   4.  Update the link type if needed, and all target info text - both properties and notes
    //         It will be needed if this is a reverse link, and the original's type had changed.
    void filterLinkages() {
        Vector<LinkedEntityData> linkTargets = new Vector<>(0, 1);


        for(LinkedEntityData linkedEntityData: editedNoteData.linkTargets) {
            if(linkedEntityData.getTargetGroupInfo() == null) continue;  // (1)

            // There will be more conditions on this, eventually....
                linkTargets.add(linkedEntityData);
        }

        editedNoteData.linkTargets = linkTargets; // Swap out the original list for our newly-groomed one.
    }

    // Cycle through the interface and reconstruct the linkages
    // for the result.
    public NoteData getEditedLinkedNote() {
        int numLinks = groupNotesListPanel.getComponentCount();
        editedNoteData.linkTargets.clear();

        // This will fix any reordering, as well as drop out deletions.
        for(int i=0; i<numLinks; i++) {
            LinkNoteComponent linkNoteComponent = (LinkNoteComponent) groupNotesListPanel.getComponent(i);
            if(!linkNoteComponent.deleteCheckBox.isSelected() || !deleteCheckedLinks) {
                editedNoteData.linkTargets.add(linkNoteComponent.getLinkTarget());
            }

        }
        return editedNoteData;
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
                // preserve it before changing it here, so we can set it back when done.
                // Currently this is null in most cases anyway.
                NoteSelection originalSelectionMonitor = NoteComponent.mySelectionMonitor;

                LinkTargetSelectionPanel linkTargetSelectionPanel = new LinkTargetSelectionPanel(editedNoteData);
                NoteComponent.mySelectionMonitor = linkTargetSelectionPanel;
                // Show the choices of groups / notes to which to make this link.
                // The dialog that is shown will have its own OK/CANCEL, and an OK there will return us to here
                //   with a new choice having been defined.
                int choice = JOptionPane.showConfirmDialog(
                        null,
                        linkTargetSelectionPanel,
                        "New Link Selection",
                        JOptionPane.OK_CANCEL_OPTION, // Option type
                        JOptionPane.PLAIN_MESSAGE);    // Message type

                // Restore the original (if any) Note selection monitor.
                NoteComponent.mySelectionMonitor = originalSelectionMonitor;

                if (choice == JOptionPane.OK_OPTION) { // From link addition, not this editor panel

   // editedNoteData gets set during construction of this class and should not be null.  True, it may be tweaked a bit
   // before they clicked on 'addNewLink', but that would have some effect on OTHER links, not this one that we're
   // currently creating, so adding to its linkTargets is not dependent on any other linkTarget content.  And while
   // the panel may hold uncommitted changes to linkTargets, it provides no way to make other changes to the source
   // NoteData, so there should be absolutely no need to update the editedNoteData by calling 'getEditedLinkedNote'
   // at this point or before adding a reverse link.
//                    deleteCheckedLinks = false;
//                    editedNoteData = getEditedLinkedNote();  // This will update all pre-existing link types
//                    deleteCheckedLinks = true;

                    if(linkTargetSelectionPanel.selectedTargetGroup == null) return; // No Group selection - just go back.
                    GroupProperties selectedGroupProperties = linkTargetSelectionPanel.selectedTargetGroup.getGroupProperties();
                    NoteData selectedNoteData = linkTargetSelectionPanel.selectedNoteData;

                    LinkedEntityData linkedEntityData;
                    if(selectedNoteData == null) {
                        linkedEntityData = new LinkedEntityData(selectedGroupProperties);
                    } else {
                        linkedEntityData = new LinkedEntityData(selectedGroupProperties, selectedNoteData);
                    }
                    editedNoteData.linkTargets.add(linkedEntityData);
                    addReverseLink(linkedEntityData);
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
        if(downsizedNote.endsWith(".")) {
            // Take off a final '.', if there is one.
            downsizedNote = downsizedNote.substring(0, downsizedNote.length()-1);
        }
        if(downsizedNote.length() > 66) {
            // The length cutoff number chosen here is somewhat arbitrary, depends on fonts, etc.  Sorry.
            downsizedNote = downsizedNote.substring(0, 62) + "...";
        }
        int linkCount = editedNoteData.linkTargets.size();
        if(linkCount == 0) {
            theSourceNoteText = "There are no existing links for " + AppUtil.makeRed(downsizedNote);
            theSourceNoteText += ".  Click on 'Add New Link' to add one.";
        } else if(linkCount == 1) {
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
            groupNotesListPanel.add(linkNoteComponent);
            lastVisibleNoteIndex++;
        }

        jsp.setViewportView(groupNotesListPanel);
        jsb.setValue(jsb.getMaximum());  // Scroll to the bottom - any new item will be here.
        add(jsp, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new DndLayout()); // This layout keeps the content left-justified.
        JButton addLinkButton = new JButton("<html><u>Add New Link</u></html>");

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


}


