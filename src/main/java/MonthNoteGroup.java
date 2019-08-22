/**  User interface to manage notes associated with a Month.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

public class MonthNoteGroup extends CalendarNoteGroup implements MouseListener {
    private static final long serialVersionUID = 1L;

    private static JLabel monthTitle;

    static {

        // Create the window title
        monthTitle = new JLabel();
        monthTitle.setHorizontalAlignment(JLabel.CENTER);
        monthTitle.setForeground(Color.white);
        monthTitle.setFont(Font.decode("Serif-bold-20"));

        MemoryBank.init();
    } // end static

    //=============================================================

    MonthNoteGroup() {
        super("Month Note");
        sdf.applyPattern("MMMM yyyy");

        LabelButton prev = new LabelButton("-");
        prev.addMouseListener(this);
        prev.setPreferredSize(new Dimension(28, 28));
        prev.setFont(Font.decode("Dialog-bold-14"));

        LabelButton next = new LabelButton("+");
        next.addMouseListener(this);
        next.setPreferredSize(new Dimension(28, 28));
        next.setFont(Font.decode("Dialog-bold-14"));

        JPanel p0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p0.add(prev);
        p0.add(next);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setBackground(Color.blue);
        heading.add(p0, "West");
        heading.add(monthTitle, "Center");

        add(heading, BorderLayout.NORTH);

        updateHeader();
    } // end constructor


    //--------------------------------------------------------------
    // Method Name: recalc
    //
    // Repaints the display.
    //--------------------------------------------------------------
    public void recalc() {
        updateGroup();
        updateHeader();

        MemoryBank.debug("MonthNoteGroup recalc - " + sdf.format(getChoice()));
    } // end recalc


    // This is called from AppTreePanel.
    public void setChoice(Date d) {

        // If the new day is the same as the current one - return.
        if (sdf.format(getChoice()).equals(sdf.format(d))) return;

        super.setChoice(d);
        updateHeader();
    } // end setChoice


    //--------------------------------------------------------------
    // Method Name: updateHeader
    //
    // This header contains only a formatted date string.
    //--------------------------------------------------------------
    private void updateHeader() {
        // Generate new title from current choice.
        monthTitle.setText(sdf.format(getChoice()));
    } // end updateHeader


    //---------------------------------------------------------
    // MouseListener methods
    //---------------------------------------------------------
    public void mouseClicked(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getText();

        // One of the two mutually exclusive conditions below is expected
        // to be true but if neither then we just ignore the action.
        if (s.equals("-")) {
            setOneBack();
        }
        if (s.equals("+")) {
            setOneForward();
        }

        recalc();
    } // end mouseClicked

    public void mouseEntered(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getText();
        if (s.equals("-")) {
            s = "Click here to see previous month";
        }
        if (s.equals("+")) {
            s = "Click here to see next month";
        }
        setMessage(s);
    } // end mouseEntered

    public void mouseExited(MouseEvent e) {
        setMessage(" ");
    }

    public void mousePressed(MouseEvent e) {
    } // end mousePressed

    public void mouseReleased(MouseEvent e) {
    } // end mouseReleased
    //---------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("main method of MonthNoteGroup started.");

        MemoryBank.debug = true;
        MemoryBank.setProgramDataLocation();
        MemoryBank.setUserDataHome("g01@doughmain.net");

        final MonthNoteGroup dn = new MonthNoteGroup();

        // local variable dn is accessed from within inner class; needs
        //    to be declared final
        JFrame f = new JFrame("MonthNoteGroup Test");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                dn.preClose();
                System.exit(0);
            }
        });

        dn.setChoice(new Date());
        f.getContentPane().add(dn, "Center");
        f.pack();
        f.setVisible(true);

        System.out.println("main method of MonthNoteGroup completed.");
    } // end main

} // end class MonthNoteGroup


