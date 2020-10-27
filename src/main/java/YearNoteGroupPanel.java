/*  User interface to manage notes associated with a Year.
    This class extends from NoteGroupPanel and uses the inherited
    NoteComponent; there is no need for a YearNoteComponent.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class YearNoteGroupPanel extends CalendarNoteGroupPanel implements MouseListener {
    private static final long serialVersionUID = 1L;

    private JLabel yearTitle;

    static {
        MemoryBank.trace();
    } // end static

    //=============================================================

    YearNoteGroupPanel() {
        super(GroupInfo.GroupType.YEAR_NOTES);
        buildMyPanel();
    } // end constructor

    private void buildMyPanel() {

        // Create the window title
        yearTitle = new JLabel();
        yearTitle.setHorizontalAlignment(JLabel.CENTER);
        yearTitle.setForeground(Color.white);
        yearTitle.setFont(Font.decode("Serif-bold-20"));

        dtf = DateTimeFormatter.ofPattern("yyyy");

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


    // This is called from AppTreePanel.
    public void setDate(LocalDate theNewChoice) {

        // If the new day puts us in the same year as the current one - just return.
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
        yearTitle.setText(dtf.format(getChoice()));
    } // end updateHeader


    //---------------------------------------------------------
    // MouseListener methods
    //---------------------------------------------------------
    public void mouseClicked(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        source.requestFocus(); // Remove selection highlighting
        String s = source.getText();

        // One of the two mutually exclusive conditions below is expected
        // to be true but if neither then we just ignore the action.
        if (s.equals("-")) {
            setOneBack();
        }
        if (s.equals("+")) {
            setOneForward();
        }

        updateGroup();
        updateHeader();
    } // end mouseClicked

    public void mouseEntered(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getText();
        if (s.equals("-")) {
            s = "Click here to see previous year";
        }
        if (s.equals("+")) {
            s = "Click here to see next year";
        }
        setStatusMessage(s);
    } // end mouseEntered

    public void mouseExited(MouseEvent e) {
        setStatusMessage(" ");
    }

    public void mousePressed(MouseEvent e) {
    } // end mousePressed

    public void mouseReleased(MouseEvent e) {
    } // end mouseReleased
    //---------------------------------------------------------

} // end class YearNoteGroup


