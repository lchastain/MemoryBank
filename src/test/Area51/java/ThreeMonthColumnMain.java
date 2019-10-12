import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ThreeMonthColumnMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("g01@doughmain.net");

        JFrame testFrame = new JFrame("ThreeMonthColumn Driver");

        ThreeMonthColumn tmc = new ThreeMonthColumn();

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.out.println("MonthNoteGroup selection choice is: " + tmc.getChoice());
                System.exit(0);
            }
        });

        testFrame.getContentPane().add(tmc, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(320, 500));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
