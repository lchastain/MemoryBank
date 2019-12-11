/*  User interface to manage notes associated with a Month.
    This class extends from NoteGroup and uses the inherited
    NoteComponent; there is no need for a MonthNoteComponent.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MonthNoteGroup extends CalendarNoteGroup implements MouseListener {
    private static final long serialVersionUID = 1L;

    private static JLabel monthTitle;

    static {
        // Create the window title
        monthTitle = new JLabel();
        monthTitle.setHorizontalAlignment(JLabel.CENTER);
        monthTitle.setForeground(Color.white);
        monthTitle.setFont(Font.decode("Serif-bold-20"));

        MemoryBank.trace();
    } // end static

    //=============================================================

    MonthNoteGroup() {
        super("Month Note");
        dtf = DateTimeFormatter.ofPattern("MMMM yyyy");

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

        MemoryBank.debug("MonthNoteGroup recalc - " + dtf.format(getChoice()));
    } // end recalc


    // This is called from AppTreePanel.
    @Override
    public void setDate(LocalDate theNewChoice) {

        // If the new date puts us in the same month as the current one - return.
        if (dtf.format(getChoice()).equals(dtf.format(theNewChoice))) return;

        super.setDate(theNewChoice);
        updateHeader();
    } // end setDate


    //--------------------------------------------------------------
    // Method Name: updateHeader
    //
    // This header contains only a formatted date string.
    //--------------------------------------------------------------
    private void updateHeader() {
        // Generate new title from current choice.
        monthTitle.setText(dtf.format(getChoice()));
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

} // end class MonthNoteGroup


