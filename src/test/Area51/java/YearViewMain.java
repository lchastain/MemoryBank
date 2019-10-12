import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;

public class YearViewMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;

        Frame yvFrame = new Frame("YearView Driver");

        LocalDate ld = LocalDate.of(2018, 6, 6);
        YearView yv = new YearView(ld);

        yvFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.out.println("YearView selection choice is: " + yv.getChoice());
                System.exit(0);
            }
        });

        // Needed to override the 'metal' L&F for Swing components.
        String laf = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(laf);
        } catch (Exception ignored) {
        }    // end try/catch
        SwingUtilities.updateComponentTreeUI(yv);

        yvFrame.add(yv);
        yvFrame.pack();
        yvFrame.setVisible(true);
        yvFrame.setLocationRelativeTo(null);
    }

}
