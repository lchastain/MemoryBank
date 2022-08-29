import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class RecurrencePanelMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.userEmail = "g01@doughmain.net";

        JFrame testFrame = new JFrame("RecurrencePanel Driver");

        RecurrencePanel theRecurrencePanel = new RecurrencePanel();

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        testFrame.getContentPane().add(theRecurrencePanel, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(340, 450));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
