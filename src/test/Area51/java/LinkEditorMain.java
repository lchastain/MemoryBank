import javax.swing.*;
import java.awt.event.WindowEvent;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class LinkEditorMain {

    public static void main(String[] args) {
        boolean createFakeData = false;

        // Create an icon on the taskbar that can be used to bring the dialogs
        //   forward, when Intellij has overlaid them during debug sessions.
        //   The frame is otherwise not needed / used / seen.
        JFrame jFrame = new JFrame();
        jFrame.setLocation(-100, -100); // Offscreen
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setVisible(true);

        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("lex@doughmain.net");
        AppOptions.loadOpts();
        MemoryBank.appOpts.linkages.clear(); // Don't want to keep growing this test list.
        GroupInfo.load();

        // Generate up some test-only data -
        //---------------------------------------------------------------------------------------------------
        NoteData sourceNoteData = new NoteData();
        UUID sourceGroupId = UUID.randomUUID();
        sourceNoteData.noteString = "Something interesting";
        sourceNoteData.subjectString = "This note's subject";
        sourceNoteData.extendedNoteString = "An extended note";

        LinkedNoteData linkedNoteData = new LinkedNoteData(sourceGroupId, sourceNoteData);

          if(new AtomicBoolean(createFakeData).get()) { // Fancy way to check it, without the 'always' complaint.
            for (int i = 0; i < 7; i++) {
                LinkTargetData linkTargetData = new LinkTargetData();
                linkTargetData.theType = LinkTargetData.LinkType.getRandomType();  // one of the valid choices

                NoteData targetNoteData = new NoteData();
                targetNoteData.setNoteString("Note string " + i);
                targetNoteData.setExtendedNoteString("Extended note.  And more, later."); // Don't care right now about a Subject
                linkTargetData.setLinkTargetGroupId(((GroupInfo) MemoryBank.groupNames.elementAt(i * 2)).instanceId);
                linkTargetData.setLinkTargetNoteData(targetNoteData);
                linkedNoteData.linkTargets.add(linkTargetData);
            }
            MemoryBank.appOpts.linkages.add(linkedNoteData);
        }

        //---------------------------------------------------------------------------------------------------
        LinkagesEditorPanel linkagesEditorPanel = new LinkagesEditorPanel(linkedNoteData);

        String theTitle = "  Edit Linkages:  Checked links will be deleted.  " +
                "Click link text to highlight it.  " +
                "You can move a highlighted link by shift-up or shift-down arrow.";

        theTitle = LinkagesEditorPanel.getOptionPaneTitle(0);

        //new TestUtil().showConfirmDialog(
        int choice = JOptionPane.showConfirmDialog(
                null,
                linkagesEditorPanel,
                theTitle, // pane title bar
                JOptionPane.OK_CANCEL_OPTION, // Option type
                JOptionPane.PLAIN_MESSAGE);    // Message type

        // We don't actually want to do either of the branches below, given that this is a
        // test driver.  But - you might see a similar section in the 'live' code.
        if (choice == JOptionPane.OK_OPTION) {
            LinkedNoteData updatedLinkNoteData = linkagesEditorPanel.getEditedLinkedNote();
            MemoryBank.appOpts.linkages.add(updatedLinkNoteData);
            AppOptions.saveOpts();  // Accept the addition(s) and save.
        } else {
            //AppOptions.loadOpts();  // Cancel.
        }


// Enable the code below IF you want to have it in a resizable dialog, vs a static JOptionPane -
// (but you also would need additional/different imports, and you get no ok/cancel buttons).
//        JFrame testFrame = new JFrame("linkagesEditorPanel Driver");
//
//        testFrame.addWindowListener(new WindowAdapter() {
//            public void windowClosing(WindowEvent we) {
//                System.exit(0);
//            }
//        });
//
//        testFrame.getContentPane().add(linkagesEditorPanel, "Center");
//        testFrame.pack();
//        testFrame.setSize(new Dimension(860, 510));
//        testFrame.setVisible(true);
//        testFrame.setLocationRelativeTo(null);

        jFrame.dispatchEvent(new WindowEvent(jFrame, WindowEvent.WINDOW_CLOSING));
    }

}
