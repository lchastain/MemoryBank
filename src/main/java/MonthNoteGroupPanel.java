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
import java.time.temporal.ChronoUnit;

public class MonthNoteGroupPanel extends CalendarNoteGroupPanel implements MouseListener {
    private static final long serialVersionUID = 1L;

    static {
        MemoryBank.trace();
    } // end static


    MonthNoteGroupPanel() {
        super(GroupType.MONTH_NOTES);
        buildMyPanel();
    } // end constructor

    private void buildMyPanel() {
        dtf = DateTimeFormatter.ofPattern("MMMM yyyy");

        // Note that none of these should get tooltip text; use the mouseEntered / setStatusMessage, instead.
        LabelButton yearMinus = makeAlterButton("Y-", this);
        LabelButton prev = makeAlterButton("-", this);
        todayButton.addMouseListener(this);
        LabelButton next = makeAlterButton("+", this);
        LabelButton yearPlus = makeAlterButton("Y+", this);

        prev.setIcon(LabelButton.leftIcon);
        prev.setText(null); // We don't want both text and icon.  The original text is preserved in the 'name'.
        next.setIcon(LabelButton.rightIcon);
        next.setText(null); // We don't want both text and icon.  The original text is preserved in the 'name'.

        JPanel p0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p0.add(yearMinus);
        p0.add(prev);
        p0.add(todayButton);
        p0.add(next);
        p0.add(yearPlus);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setBackground(Color.blue);
        heading.add(p0, "West");
        heading.add(panelTitleLabel, "Center");

        add(heading, BorderLayout.NORTH);

        updateHeader();
    } // end constructor


    //---------------------------------------------------------
    // MouseListener methods
    //---------------------------------------------------------
    public void mouseClicked(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        if(!source.isEnabled()) return; // It's not really a button; we need to check this first.
        source.requestFocus(); // Remove selection highlighting
        String s = source.getName();

        // One of the mutually exclusive conditions below is expected
        // to be true but if none are then we just ignore the action.
        switch (s) {
            case "Y-":
                setDateType(ChronoUnit.YEARS);
                setOneBack();
                setDateType(ChronoUnit.MONTHS);
                break;
            case "-":
                setOneBack();
                break;
            case "T":
                // Change to 'today' without affecting theChoice.
                // This is preferred when this navigation control is used while 'Viewing FoundIn'.
                // And in that case AppTreePanel.theInstance.showToday() would not work anyway, since
                //   there would be no active selection on the Tree.  This is in alignment with the
                //   other usages of AlterButtons, vs the behavior you get from the 'Today' menu item,
                //   which DOES affect theChoice.
                setDate(LocalDate.now());
                break;
            case "+":
                setOneForward();
                break;
            case "Y+":
                setDateType(ChronoUnit.YEARS);
                setOneForward();
                setDateType(ChronoUnit.MONTHS);
                break;
        }
        updateGroup();
    } // end mouseClicked

    public void mouseEntered(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getName();
        switch (s) {
            case "Y-":
                s = "Click here to go back one year";
                break;
            case "-":
                s = "Click here to see previous month";
                break;
            case "T":
                s = "Click here to see notes for this month.";
                break;
            case "+":
                s = "Click here to see next month";
                break;
            case "Y+":
                s = "Click here to go forward one year";
                break;
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

} // end class MonthNoteGroup


