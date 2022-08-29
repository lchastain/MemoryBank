import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TodoNoteGroupMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.userEmail = "g01@doughmain.net";

        JFrame testFrame = new JFrame("TodoNoteGroup Driver");

        TodoNoteGroupPanel todoNoteGroup = new TodoNoteGroupPanel("Geo Wun's List");

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        testFrame.getContentPane().add(todoNoteGroup.theBasePanel, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(650, 550));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
