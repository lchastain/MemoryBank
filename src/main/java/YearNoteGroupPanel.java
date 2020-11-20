/*  User interface to manage notes associated with a Year.
    This class extends from NoteGroupPanel and uses the inherited
    NoteComponent; there is no need for a YearNoteComponent.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.format.DateTimeFormatter;

public class YearNoteGroupPanel extends CalendarNoteGroupPanel implements MouseListener {
    private static final long serialVersionUID = 1L;

    static {
        MemoryBank.trace();
    } // end static


    YearNoteGroupPanel() {
        super(GroupType.YEAR_NOTES);
        buildMyPanel();
    } // end constructor

    private void buildMyPanel() {
        dtf = DateTimeFormatter.ofPattern("yyyy");

        // Note that none of these should get tooltip text; use the mouseEntered / setStatusMessage, instead.
        LabelButton decadeMinus = makeAlterButton("D-", this);
        LabelButton prev = makeAlterButton("-", this);
        todayButton.addMouseListener(this);
        LabelButton next = makeAlterButton("+", this);
        LabelButton decadePlus = makeAlterButton("D+", this);

        prev.setIcon(LabelButton.leftIcon);
        prev.setText(null); // We don't want both text and icon.  The original text is preserved in the 'name'.
        next.setIcon(LabelButton.rightIcon);
        next.setText(null); // We don't want both text and icon.  The original text is preserved in the 'name'.

        JPanel p0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p0.add(decadeMinus);
        p0.add(prev);
        p0.add(todayButton);
        p0.add(next);
        p0.add(decadePlus);

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
            case "D-":
                for(int i=0; i<10; i++) {
                    setOneBack();
                }
                break;
            case "-":
                setOneBack();
                break;
            case "T":
                AppTreePanel.theInstance.showToday();
                break;
            case "+":
                setOneForward();
                break;
            case "D+":
                for(int i=0; i<10; i++) {
                    setOneForward();
                }
                break;
        }

        updateGroup();
    } // end mouseClicked

    public void mouseEntered(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getName();
        switch (s) {
            case "D-":
                s = "Click here to go back ten years";
                break;
            case "-":
                s = "Click here to see previous year";
                break;
            case "T":
                s = "Click here to see notes for this year.";
                break;
            case "+":
                s = "Click here to see next year";
                break;
            case "D+":
                s = "Click here to go forward ten years";
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

} // end class YearNoteGroup


