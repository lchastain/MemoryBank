/*  User interface to manage notes associated with a Month.
    This class extends from NoteGroupPanel and uses the inherited
    NoteComponent; there is no need for a MonthNoteComponent.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class MonthNoteGroupPanel extends CalendarNoteGroupPanel implements MouseListener, IconKeeper {
    private static final long serialVersionUID = 1L;

    static ImageIcon defaultIcon;

    static {
        // Because the parent NoteGroup class is where all NoteComponents get
        //   made and that constructor runs before the one here, the defaultIcon
        //   (seen in an IconNoteComponent) MUST be present BEFORE that
        //   constructor is called.  This is why we need to
        //   assign it during the static section of this class.
        if(MemoryBank.appOpts.defaultMonthNoteIconInfo == null) {
            IconNoteData ind = new IconNoteData();
            ind.setIconFileString(MemoryBank.appOpts.defaultMonthNoteIconDescription);
            defaultIcon = ind.getImageIcon();
        } else {
            defaultIcon = MemoryBank.appOpts.defaultMonthNoteIconInfo.getImageIcon();
        }

        MemoryBank.trace();
    } // end of the static section


    MonthNoteGroupPanel() {
        super(GroupType.MONTH_NOTES);
        myDateType = DateRelatedDisplayType.MONTH_NOTES;
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
        todayButton.setToolTipText("This Month");
        p0.add(next);
        p0.add(yearPlus);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setBackground(Color.blue);
        heading.add(p0, "West");
        heading.add(panelTitleLabel, "Center");

        add(heading, BorderLayout.NORTH);

        updateHeader();
    } // end constructor

    @Override
    public ImageIcon getDefaultIcon() {
        return defaultIcon;
    }

    // Remove this method to get lines that are about half the height and have no icons.
    @Override
    JComponent makeNewNote(int i) {
        IconNoteComponent inc = new IconNoteComponent(this, i);
        inc.setVisible(false);
        return inc;
    } // end makeNewNote

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
            case "Y-" -> setOneBack(ChronoUnit.YEARS);
            case "-" -> setOneBack(ChronoUnit.MONTHS);
            case "T" -> {
                if (archiveDate != null) setDate(archiveDate);
                else setDate(LocalDate.now());
                if (alteredDateListener != null) alteredDateListener.dateChanged(myDateType, theDate);
            }
            case "+" -> setOneForward(ChronoUnit.MONTHS);
            case "Y+" -> setOneForward(ChronoUnit.YEARS);
        }
        updateGroup();
    } // end mouseClicked

    public void mouseEntered(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getName();
        if ("Y-".equals(s)) {
            s = "Click here to go back one year";
        } else if ("-".equals(s)) {
            s = "Click here to see previous month";
        } else if ("T".equals(s)) {
            s = "Click here to see notes for this month.";
        } else if ("+".equals(s)) {
            s = "Click here to see next month";
        } else if ("Y+".equals(s)) {
            s = "Click here to go forward one year";
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

    // Called by the IconNoteComponent's
    //   popup menu handler for 'Set As Default'.
    @Override
    public void setDefaultIcon(ImageIcon li) {
        defaultIcon = li;
        MemoryBank.appOpts.defaultMonthNoteIconInfo = null;
        MemoryBank.appOpts.defaultMonthNoteIconDescription = li.getDescription();
        setGroupChanged(true);
        preClosePanel();
        updateGroup();
    } // end setDefaultIcon

} // end class MonthNoteGroupPanel


