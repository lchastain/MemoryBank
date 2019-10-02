import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;

public class MonthViewMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;

        LocalDate ld;
        //ld = LocalDate.of(2018, 9, 2);
        ld = LocalDate.of(2010, 6, 20);
        //ld = LocalDate.of(1918, 2, 2);  // Russia
        //ld = LocalDate.of(1752, 9, 2);    // US, UK
        //ld = LocalDate.of(1582, 10, 30);  // Other European (first)
        //ld = LocalDate.of(2019, 9, 18);
        MonthView mv = new MonthView(ld);
        //mv.setChoice(LocalDate.of(2018, 9, 2));

        JFrame f = new JFrame("Month View Test");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.out.println("MonthView selection choice is: " + mv.getChoice());
                System.exit(0);
            }
        });

        f.getContentPane().add(mv, "Center");
        f.pack();
        f.setSize(new Dimension(600, 500));
        f.setVisible(true);
        f.setLocationRelativeTo(null);
    }


}
