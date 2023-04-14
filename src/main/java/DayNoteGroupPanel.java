/*  This class displays a group of DayNoteComponent.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DayNoteGroupPanel extends CalendarNoteGroupPanel
        implements IconKeeper, MouseListener {

    static ImageIcon defaultIcon;

    static {
        // Because the parent NoteGroup class is where all NoteComponents get
        //   made and that constructor runs before the one here, the defaultIcon
        //   (seen in a DayNoteComponent) MUST be present BEFORE that
        //   constructor is called.  This is why we need to
        //   assign it during the static section of this class.
        if(MemoryBank.appOpts.defaultDayNoteIconInfo == null) {
            IconNoteData ind = new IconNoteData();
            ind.setIconFileString(MemoryBank.appOpts.defaultDayNoteIconDescription);
            defaultIcon = ind.getImageIcon();
        } else {
            defaultIcon = MemoryBank.appOpts.defaultDayNoteIconInfo.getImageIcon();
        }

        MemoryBank.trace();
    } // end of the static section


    DayNoteGroupPanel() {
        super(GroupType.DAY_NOTES);
        myDateType = DateRelatedDisplayType.DAY_NOTES;
        buildMyPanel();
    } // end constructor


    private void buildMyPanel() {
        // Note that none of these should get tooltip text; use the mouseEntered / setStatusMessage, instead.
        LabelButton yearMinus = makeAlterButton("Y-", this);
        LabelButton monthMinus = makeAlterButton("M-", this);
        LabelButton prev = makeAlterButton("-", this);
        todayButton.addMouseListener(this);
        LabelButton next = makeAlterButton("+", this);
        LabelButton monthPlus = makeAlterButton("M+", this);
        LabelButton yearPlus = makeAlterButton("Y+", this);
        LabelButton timeFormatButton = makeAlterButton("24", this);

        // Note:  Tried to use icons along with text in place of the +/- for the Year and Month buttons, but the wider
        //   end result required more width on the AlterButtons (28-->34) in order to show.  Then I expected the text
        //   to be displayed first but it put the icon first.  Final decision here: text only, for Y-,M-,M+,Y+.
        //
        //yearMinus.setIcon(LabelButton.leftIcon);
        //yearMinus.setText("Y");
        //monthMinus.setIcon(LabelButton.leftIcon);
        //monthMinus.setText("M");
        prev.setIcon(LabelButton.leftIcon);
        prev.setText(null); // We don't want both text and icon.  The original text is preserved in the 'name'.
        next.setIcon(LabelButton.rightIcon);
        next.setText(null); // We don't want both text and icon.  The original text is preserved in the 'name'.
        //monthPlus.setIcon(LabelButton.rightIcon);
        //monthPlus.setText("M");
        //yearPlus.setIcon(LabelButton.rightIcon);
        //yearPlus.setText("Y");

        JPanel p0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p0.add(yearMinus);
        p0.add(monthMinus);
        p0.add(prev);
        p0.add(todayButton);
        todayButton.setToolTipText("Today");
        p0.add(next);
        p0.add(monthPlus);
        p0.add(yearPlus);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setBackground(Color.blue);
        heading.add(p0, "West");
        heading.add(panelTitleLabel, "Center");

        if (MemoryBank.appOpts.timeFormat == AppOptions.TimeFormat.MILITARY) {
            timeFormatButton.setText("12");
            timeFormatButton.setName("12");
        }
        heading.add(timeFormatButton, "East");  // spacer 56

        add(heading, BorderLayout.NORTH);

        updateHeader();
        MemoryBank.trace();
    }// end buildMyPanel


    @Override
    public ImageIcon getDefaultIcon() {
        return defaultIcon;
    }

    // Returns a DayNoteComponent that can be used to manipulate
    // component state as well as set/get underlying data.
    public DayNoteComponent getNoteComponent(int i) {
        return (DayNoteComponent) groupNotesListPanel.getComponent(i);
    } // end getNoteComponent


    @Override
    JComponent makeNewNoteComponent(int i) {
        DayNoteComponent dnc = new DayNoteComponent(this, i);
        dnc.setName("initial DayNoteComponent " + i);
        dnc.setVisible(false);
        return dnc;
    } // end makeNewNoteComponent

    //<editor-fold desc="MouseListener methods">
    public void mouseClicked(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        if(!source.isEnabled()) return; // It's not really a button; we need to check this first.
        source.requestFocus(); // Remove selection highlighting
        String s = source.getName();

        switch (s) {
            case "Y-" -> setOneBack(ChronoUnit.YEARS);
            case "M-" -> setOneBack(ChronoUnit.MONTHS);
            case "-" -> setOneBack(ChronoUnit.DAYS);
            case "T" -> {
                if (archiveDate != null) setDate(archiveDate);
                else setDate(LocalDate.now());
                AppTreePanel.theInstance.dateChanged(DateRelatedDisplayType.DAY_NOTES, theDate);
            }
            case "+" -> setOneForward(ChronoUnit.DAYS);
            case "M+" -> setOneForward(ChronoUnit.MONTHS);
            case "Y+" -> setOneForward(ChronoUnit.YEARS);
            case "12" -> {
                toggleMilitary();
                source.setName("24");
                source.setText("24");
                mouseEntered(e); // Update the status message, without having to exit/enter.
                return;
            }
            case "24" -> {
                toggleMilitary();
                source.setName("12");
                source.setText("12");
                mouseEntered(e); // Update the status message, without having to exit/enter.
                return;
            }
            default -> System.out.println("DayNoteGroupPanel.mouseClicked unhandled: " + s);
        }
        updateGroup();
    } // end mouseClicked

    public void mouseEntered(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        if(!source.isEnabled()) return; // It's not really a button; we need to check this first.
        String s = source.getName();
        switch (s) {
            case "Y-" -> s = "Click here to go back one year";
            case "M-" -> s = "Click here to go back one month";
            case "-" -> s = "Click here to see previous day";
            case "T" -> s = "Click here to see notes for 'today'.";
            case "+" -> s = "Click here to see next day";
            case "M+" -> s = "Click here to go forward one month";
            case "Y+" -> s = "Click here to go forward one year";
            case "12" -> s = "Click here to see time in 12 hour format";
            case "24" -> s = "Click here to see time in 24 hour format";
        }
        setStatusMessage(s);
    } // end mouseEntered

    public void mouseExited(MouseEvent e) {
        setStatusMessage(" ");
    }
    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
    //---------------------------------------------------------
    //</editor-fold>

    // Called by the DayNoteComponent's
    //   popup menu handler for 'Set As Default'.
    @Override
    public void setDefaultIcon(ImageIcon li) {
        defaultIcon = li;
        MemoryBank.appOpts.defaultDayNoteIconInfo = null;
        MemoryBank.appOpts.defaultDayNoteIconDescription = li.getDescription();
        setGroupChanged(true);
        preClosePanel();
        updateGroup();
    } // end setDefaultIcon


    // This is called from the 12/24 button in the DayNoteGroupPanel header
    private void toggleMilitary() {
        if(MemoryBank.appOpts.timeFormat == AppOptions.TimeFormat.MILITARY) {
            MemoryBank.appOpts.timeFormat = AppOptions.TimeFormat.CIVILIAN;
        } else {
            MemoryBank.appOpts.timeFormat = AppOptions.TimeFormat.MILITARY;
        }
        // Need to reprint all time labels -
        for (int i = 0; i <= lastVisibleNoteIndex; i++) {
            getNoteComponent(i).resetTimeLabel();
        } // end for i
    } // end toggleMilitary

} // end class DayNoteGroupPanel


