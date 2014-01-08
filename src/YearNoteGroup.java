/* ***************************************************************************
 * Author:  D. Lee Chastain
 ****************************************************************************/
/**  User interface to manage notes associated with a Year.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class YearNoteGroup extends CalendarNoteGroup implements MouseListener {
    private static final long serialVersionUID = 1L;

    private static JLabel yearTitle;

    static {
        // Create the window title
        yearTitle = new JLabel();
        yearTitle.setHorizontalAlignment(JLabel.CENTER);
        yearTitle.setForeground(Color.white);
        yearTitle.setFont(Font.decode("Serif-bold-20"));

        MemoryBank.init();
    } // end static

    //=============================================================

    YearNoteGroup() {
        super("Year Note");
        sdf.applyPattern("yyyy");

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
        heading.add(yearTitle, "Center");

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

        MemoryBank.debug("YearNoteGroup recalc - " + sdf.format(getChoice()));
    } // end recalc


    // This is called from AppTree.
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
    public void updateHeader() {
        // Generate new title from current choice.
        yearTitle.setText(sdf.format(getChoice()));
    } // end updateHeader


    //---------------------------------------------------------
    // MouseListener methods
    //---------------------------------------------------------
    public void mouseClicked(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getText();

        if (s.equals("XXX")) {
        } else if (s.equals("-")) {
            setOneBack();
        } else if (s.equals("+")) {
            setOneForward();
        } else {
            (new Exception("Unhandled action!")).printStackTrace();
            System.exit(1);
        } // end if

        recalc();
    } // end mouseClicked

    public void mouseEntered(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getText();
        if (s.equals("XXX")) {
        } else if (s.equals("-")) {
            s = "Click here to see previous year";
        } else if (s.equals("+")) {
            s = "Click here to see next year";
        } // end if
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
        System.out.println("main method of YearNoteGroup started.");

        MemoryBank.debug = true;
        MemoryBank.setDataLocations();

        final YearNoteGroup dn = new YearNoteGroup();

        // local variable dn is accessed from within inner class; needs
        //    to be declared final
        JFrame f = new JFrame("YearNoteGroup Test");
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

        System.out.println("main method of YearNoteGroup completed.");
    } // end main

} // end class YearNoteGroup


