import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LinkagesEditorPanel extends JPanel implements NoteComponentManager {
    static final long serialVersionUID = 1L;
    JPanel groupNotesListPanel;
    int lastVisibleNoteIndex = -1;
    JScrollPane jsp;
    JScrollBar jsb;
    ActionListener addButtonActionListener;
    NoteData linkedNoteData;
    boolean deleteCheckedLinks;

    LinkagesEditorPanel(NoteData aLinkedNoteData) {
        super(new BorderLayout());
        linkedNoteData = aLinkedNoteData;
        deleteCheckedLinks = true;

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

        // Define the listener for the 'Add New Link' text.
        addButtonActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                // Since the NoteComponent selection monitor is a static setting, we need to
                // preserve it before changing it here, so we can set it back when done.
                // Currently this is null in most cases anyway.
                NoteSelection originalSelectionMonitor = NoteComponent.mySelectionMonitor;

                LinkTargetSelectionPanel linkTargetSelectionPanel = new LinkTargetSelectionPanel(linkedNoteData);
                NoteComponent.mySelectionMonitor = linkTargetSelectionPanel;
                int choice = JOptionPane.showConfirmDialog(
                        null,
                        linkTargetSelectionPanel,
                        "New Link Selection",
                        JOptionPane.OK_CANCEL_OPTION, // Option type
                        JOptionPane.PLAIN_MESSAGE);    // Message type

                // Restore the original (if any) Note selection monitor.
                NoteComponent.mySelectionMonitor = originalSelectionMonitor;

                if (choice == JOptionPane.OK_OPTION) {
                    // First - capture any link type or ordering changes in the existing list.
                    deleteCheckedLinks = false;
                    linkedNoteData = getEditedLinkedNote();
                    deleteCheckedLinks = true;

                    GroupProperties selectedGroupProperties = linkTargetSelectionPanel.selectedTargetGroupProperties;
                    if(selectedGroupProperties == null) return; // No Group selection - just go back.

                    if(selectedGroupProperties.groupType == GroupProperties.GroupType.NOTES) {
                        String theGroupName = selectedGroupProperties.getName();
                        CalendarNoteGroup calendarNoteGroup = linkTargetSelectionPanel.calendarNoteGroup;
                        String theNewGroupName = theGroupName + " " + calendarNoteGroup.getTitle();
                        selectedGroupProperties = new GroupProperties(calendarNoteGroup.getTitle(), GroupProperties.GroupType.NOTES);
                    }

                    NoteData selectedNoteData = linkTargetSelectionPanel.selectedNoteData;
                    LinkTargetData linkTargetData;
                    // We utilize copy constructors to send parameters to a new LinkTargetData, to convert the params
                    // to their base classes.  Of course they could just go that way without conversion and would pass
                    // through the implicit cast back to the base class, but later, upon serialization, we would get
                    // all the extra baggage that the child classes carry.  This way - it's gone, the data stored is
                    // smaller, and most importantly, loads back in without complaint.
                    if(selectedNoteData != null) {
                        linkTargetData = new LinkTargetData(new GroupProperties(selectedGroupProperties),
                                new NoteData(selectedNoteData));
                    } else {
                        linkTargetData = new LinkTargetData(new GroupProperties(selectedGroupProperties), null);
                    }

                    linkedNoteData.linkTargets.add(linkTargetData);
                    rebuildDialog(linkedNoteData);
                }
            }
        };

        rebuildDialog(linkedNoteData);
    } // end constructor

    // This method is called initially to display all currently existing links, and is then called after each
    // link is added.  Links are only added one at a time.  The display is rebuilt each time.
    private void rebuildDialog(NoteData linkedNoteData) {
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
        int linkCount = linkedNoteData.linkTargets.size();
        if(linkCount == 0) {
            theSourceNoteText = "There are no existing links.  Click on 'Add New Link' to add one.";
        } else if(linkCount == 1) {
            theUserInstructions = "Use the dropdown to change the link type, or put a check in the checkbox to mark it for deletion";
            userInstructionsLabel.setText(theUserInstructions);
            headerInfoPanel.add(userInstructionsLabel, BorderLayout.NORTH);
            theSourceNoteText = "Existing link - " + AppUtil.makeRed(linkedNoteData.noteString) + " IS:";
        } else {
            theUserInstructions = "Checked links will be deleted.  " +
                    "You can move a highlighted link by shift-up or shift-down arrow.\n";
            userInstructionsLabel.setText(theUserInstructions);
            headerInfoPanel.add(userInstructionsLabel, BorderLayout.NORTH);
            theSourceNoteText = "Existing links - " + AppUtil.makeRed(linkedNoteData.noteString) + " IS:";
        }
        headerInfoPanel.add(sourceInfoLabel, BorderLayout.SOUTH);
        sourceInfoLabel.setText(AppUtil.makeHtml(theSourceNoteText));
        add(headerInfoPanel, BorderLayout.NORTH);

        // Fill in the list of links (if any)
        groupNotesListPanel = new JPanel();
        groupNotesListPanel.setLayout(new BoxLayout(groupNotesListPanel, BoxLayout.Y_AXIS));
        int index = 0;
        for (LinkTargetData linkTargetData : linkedNoteData.linkTargets) {
            LinkNoteComponent linkNoteComponent = new LinkNoteComponent(this, linkTargetData, index++);
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

    @Override
    public void activateNextNote(int index) {
        // Not used by this Panel; no new links allowed.
        // At least, not allowed in real-time.  A new link will be
        // added to the main list and then this panel could be
        // redisplayed, showing it at that time.
    }

    // Not needed or used.
    @Override
    public boolean editExtendedNoteComponent(NoteData tmpNoteData) {
        return true;
    }

    // Cycle through the interface and reconstruct the linkages
    // for the result.
    public NoteData getEditedLinkedNote() {
        int numLinks = groupNotesListPanel.getComponentCount();
        linkedNoteData.linkTargets.clear();

        // This will fix any reordering, as well as drop out deletions.
        for(int i=0; i<numLinks; i++) {
            LinkNoteComponent linkNoteComponent = (LinkNoteComponent) groupNotesListPanel.getComponent(i);
            if(!linkNoteComponent.deleteCheckBox.isSelected() || !deleteCheckedLinks) {
                linkedNoteData.linkTargets.add(linkNoteComponent.getLinkTarget());
            }

        }
        return linkedNoteData;
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


