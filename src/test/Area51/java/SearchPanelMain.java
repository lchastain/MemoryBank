import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SearchPanelMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("g01@doughmain.net");

        JFrame testFrame = new JFrame("SearchPanel Driver");

        SearchPanel theSearchPanel = new SearchPanel();

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        testFrame.getContentPane().add(theSearchPanel, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(500, 450));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
