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
    LinkedNoteData linkedNoteData;

    LinkagesEditorPanel(LinkedNoteData aLinkedNoteData) {
        super(new BorderLayout());
        linkedNoteData = aLinkedNoteData;

        jsp = new JScrollPane() {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(800, super.getPreferredSize().height);
            }
        };

        // Explicitly define the vertical scrollbar to allow for these additional settings:
        //  Scroll increments, and also disable focus because once the bar appears, the tab
        //  and up/down keys can transfer focus over to it, and the up/down keys do not bring
        //  it back.  But even if they did, that's not the behavior we would want.
        jsb = new JScrollBar();
        jsb.setFocusable(false);
        jsb.setUnitIncrement(NoteComponent.NOTEHEIGHT);
        jsb.setBlockIncrement(NoteComponent.NOTEHEIGHT);

        jsp.setVerticalScrollBar(jsb);
        jsp.setHorizontalScrollBar(null);

        addButtonActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                NoteSelection originalSelectionMonitor = NoteComponent.mySelectionMonitor;
                LinkTargetPanel linkTargetPanel = new LinkTargetPanel(linkedNoteData);
                NoteComponent.mySelectionMonitor = linkTargetPanel;
                int choice = JOptionPane.showConfirmDialog(
                        null,
                        linkTargetPanel,
                        "New Link Selection",
                        JOptionPane.OK_CANCEL_OPTION, // Option type
                        JOptionPane.PLAIN_MESSAGE);    // Message type

                // Restore the original (if any) Note selection monitor.
                NoteComponent.mySelectionMonitor = originalSelectionMonitor;

                if (choice == JOptionPane.OK_OPTION) {
                    LinkTarget linkTarget = new LinkTarget(linkTargetPanel.selectedTargetGroupInfo.instanceId,
                            new NoteData(linkTargetPanel.selectedNoteData));
                    linkedNoteData.linkTargets.add(linkTarget);

                    MemoryBank.appOpts.linkages.add(linkedNoteData);
                    rebuildDialog(linkedNoteData);
                }
            }
        };

        rebuildDialog(linkedNoteData);
    } // end constructor

    // This method is called initially to display all currently existing links, and is then called after each
    // link is added.  Links are only added one at a time.  The display is rebuilt each time.
//    private void rebuildDialog(NoteData noteData)
    private void rebuildDialog(LinkedNoteData linkedNoteData) {
        removeAll();
        revalidate();

        // The global linkages list will be referenced from memory and not storage, since we
        // call this method repeatedly during link additions that might eventually be cancelled rather than accepted.
        // Cycle thru the global Linkages list scanning for the link Source entity.  Create a local list of
        // LinkNoteComponents, one for each time the source appears in a linkage.
        groupNotesListPanel = new JPanel();
        groupNotesListPanel.setLayout(new BoxLayout(groupNotesListPanel, BoxLayout.Y_AXIS));

        String theMessage = "  Checked links will be deleted.  " +
                "You can move a highlighted link by shift-up or shift-down arrow.";
        theMessage = "Existing links - " + linkedNoteData.noteString + " IS:";
        JLabel messageLabel = new JLabel(theMessage);
        messageLabel.setFont(Font.decode("Dialog-bold-12"));
        add(messageLabel, BorderLayout.NORTH);

        // pull this one out of the main list.  We will process its links in the next loop.
        MemoryBank.appOpts.linkages.remove(linkedNoteData);

        int index = 0;
        for (LinkTarget linkTarget : linkedNoteData.linkTargets) {
            LinkNoteComponent linkNoteComponent = new LinkNoteComponent(this, linkTarget, index++);
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
    public LinkedNoteData getEditedLinkedNote() {
        int numLinks = groupNotesListPanel.getComponentCount();
        linkedNoteData.linkTargets.clear();

        // This will fix any reordering, as well as drop out deletions.
        for(int i=0; i<numLinks; i++) {
            LinkNoteComponent linkNoteComponent = (LinkNoteComponent) groupNotesListPanel.getComponent(i);
            if(!linkNoteComponent.deleteCheckBox.isSelected()) {
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


