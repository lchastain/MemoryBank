import javax.swing.*;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

public class LinkEditorMain {

    public static void main(String[] args) {
        boolean createFakeData = true;

        // Create an icon on the taskbar that can be used to bring the dialogs
        //   forward, when Intellij has overlaid them during debug sessions.
        //   The frame is otherwise not needed / used / seen.
        JFrame jFrame = new JFrame();
        jFrame.setLocation(-100, -100); // Offscreen
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setVisible(true);

        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("lex@doughmain.net");
        //MemoryBank.setUserDataHome("newuser@doughmain.net");
        AppOptions.loadOpts();

        // Generate up some test-only data -
        //---------------------------------------------------------------------------------------------------
        NoteData sourceNoteData = new NoteData();
        sourceNoteData.noteString = "Something interesting";
        sourceNoteData.subjectString = "This note's subject";
        sourceNoteData.extendedNoteString = "An extended note";

        // The 'createFakeData' boolean is used to determine whether we use real data, or
        // a constant set that we create here during this run.  But since that flag is
        // set at the top of this file to be either true or false on any given run and does
        // not change otherwise, IntelliJ will give an annoying 'always true or always false'
        // complaint, in our 'if' statement below.  So instead we use this fancy way to check
        // it, that avoids that complaint.
        if (new AtomicBoolean(createFakeData).get()) {
            for (int i = 0; i < 6; i++) {
                LinkTargetData linkTargetData = new LinkTargetData();
                linkTargetData.linkType = LinkTargetData.LinkType.getRandomType();  // one of the valid choices
                GroupProperties groupProperties = new GroupProperties("Group " + i, GroupProperties.GroupType.getRandomType());
                linkTargetData.setLinkTargetGroupProperties(groupProperties);

                NoteData targetNoteData = new NoteData();
                targetNoteData.setNoteString("Note string " + i);
                targetNoteData.setExtendedNoteString("Extended note.  And more, later."); // Don't care right now about a Subject
                linkTargetData.setLinkTargetNoteData(targetNoteData);
                sourceNoteData.linkTargets.add(linkTargetData);
            }
        }

        //---------------------------------------------------------------------------------------------------
        LinkagesEditorPanel linkagesEditorPanel = new LinkagesEditorPanel(sourceNoteData);

//        int choice = new TestUtil().showConfirmDialog(
        int choice = JOptionPane.showConfirmDialog(
                null,
                linkagesEditorPanel,
                "Linkages Editor", // pane title bar
                JOptionPane.OK_CANCEL_OPTION, // Option type
                JOptionPane.PLAIN_MESSAGE);    // Message type

        // We don't actually want to handle the 'Ok' button, given that this
        // is just a test driver.  But we will show the result -
        if (choice == JOptionPane.OK_OPTION) {
            NoteData updatedLinkNoteData = linkagesEditorPanel.getEditedLinkedNote();
            System.out.println("\nEditing results: \n");
            System.out.println(AppUtil.toJsonString(updatedLinkNoteData));
        } else {
            System.out.println("Linkages editing was cancelled.");
        }

        // NO - show the result - collect it and give a json printout.  or a cancel message.


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