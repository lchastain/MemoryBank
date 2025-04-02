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
import java.time.temporal.ChronoUnit;

public class YearNoteGroupPanel extends CalendarNoteGroupPanel implements MouseListener, IconKeeper {
    static ImageIcon defaultIcon;

    static {
        // Because the parent NoteGroup class is where all NoteComponents get
        //   made and that constructor runs before the one here, the defaultIcon
        //   (seen in an IconNoteComponent) MUST be present BEFORE that constructor
        //   is called.  This is why we need to assign it during the static section
        //   of this class, which is why the member must be static.  Having it be
        //   unique across different child classes of CalendarNoteGroupPanel is why
        //   it must be declared separately in each child, even though the 'get' methods
        //   are all identical.
        if(MemoryBank.appOpts.defaultYearNoteIconInfo == null) {
            IconNoteData ind = new IconNoteData();
            ind.setIconFileString(MemoryBank.appOpts.defaultYearNoteIconDescription);
            defaultIcon = ind.getImageIcon();
        } else {
            defaultIcon = MemoryBank.appOpts.defaultYearNoteIconInfo.getImageIcon();
        }

        MemoryBank.trace();
    } // end of the static section

    YearNoteGroupPanel() {
        super(GroupType.YEAR_NOTES);
        myDateType = DateRelatedDisplayType.YEAR_NOTES;
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
        todayButton.setToolTipText("This Year");
        p0.add(next);
        p0.add(decadePlus);

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
    JComponent makeNewNoteComponent(int i) {
        IconNoteComponent inc = new IconNoteComponent(this, i);
        inc.setVisible(false);
        return inc;
    } // end makeNewNoteComponent

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
        if ("D-".equals(s)) {
            setOneBack(ChronoUnit.DECADES);
        } else if ("-".equals(s)) {
            setOneBack(ChronoUnit.YEARS);
        } else if ("T".equals(s)) {
            LocalDate fromDate = theDate;

            if (archiveDate != null) setDate(archiveDate);
            else setDate(LocalDate.now());

            if (alteredDateListener != null) alteredDateListener.dateChanged(myDateType, theDate);
        } else if ("+".equals(s)) {
            setOneForward(ChronoUnit.YEARS);
        } else if ("D+".equals(s)) {
            setOneForward(ChronoUnit.DECADES);
        }

        refresh();
    } // end mouseClicked

    public void mouseEntered(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getName();
        if ("D-".equals(s)) {
            s = "Click here to go back ten years";
        } else if ("-".equals(s)) {
            s = "Click here to see previous year";
        } else if ("T".equals(s)) {
            s = "Click here to see notes for this year.";
        } else if ("+".equals(s)) {
            s = "Click here to see next year";
        } else if ("D+".equals(s)) {
            s = "Click here to go forward ten years";
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
        MemoryBank.appOpts.defaultYearNoteIconInfo = null;
        MemoryBank.appOpts.defaultYearNoteIconDescription = li.getDescription();
        setGroupChanged(true);
        preClosePanel();
        refresh();
    } // end setDefaultIcon

} // end class YearNoteGroupPanel


