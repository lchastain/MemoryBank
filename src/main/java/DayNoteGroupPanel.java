/*  This class displays a group of DayNoteComponent.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class DayNoteGroupPanel extends CalendarNoteGroupPanel
        implements IconKeeper, MouseListener {

    private static AppIcon defaultIcon;
    static DayNoteDefaults dayNoteDefaults; // Also accessed by MonthView

    static {
        // Because the parent NoteGroup class is where all NoteComponents get
        //   made and that constructor runs before the one here, the defaultIcon
        //   (seen in a DayNoteComponent) MUST be present BEFORE that
        //   constructor is called.  This is why we need to
        //   assign it during the static section of this class.
        //------------------------------------------------------------------
        dayNoteDefaults = DayNoteDefaults.load(); // This may provide a different default icon.
        if(dayNoteDefaults.defaultIconFileName == null) {
            // Something wrong; just make a new one; it will not be null.
            dayNoteDefaults = new DayNoteDefaults();
        } else if (dayNoteDefaults.defaultIconFileName.equals("")) {
            // It IS possible that the user wants no default icon.
            MemoryBank.debug("Default DayNoteComponent Icon: <blank>");
            defaultIcon = new AppIcon();
        } else {
            MemoryBank.debug("Default DayNoteComponent Icon: " + dayNoteDefaults.defaultIconFileName);
            defaultIcon = new AppIcon(dayNoteDefaults.defaultIconFileName);
            AppIcon.scaleIcon(defaultIcon);
        } // end if/else

        MemoryBank.trace();
    } // end of the static section


    DayNoteGroupPanel() {
        super(GroupInfo.GroupType.DAY_NOTES);
        buildMyPanel();
    } // end constructor

    private void buildMyPanel() {
        LabelButton timeFormatButton = new LabelButton("24");
        timeFormatButton.addMouseListener(this);
        timeFormatButton.setPreferredSize(new Dimension(28, 28));
        timeFormatButton.setFont(Font.decode("Dialog-bold-14"));

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
        heading.add(panelTitleLabel, "Center");

        if (dayNoteDefaults.military) timeFormatButton.setText("12");
        heading.add(timeFormatButton, "East");  // spacer 56

        add(heading, BorderLayout.NORTH);

        updateHeader();
        MemoryBank.trace();
    }


    public AppIcon getDefaultIcon() {
        return defaultIcon;
    }

    // Returns a DayNoteComponent that can be used to manipulate
    // component state as well as set/get underlying data.
    public DayNoteComponent getNoteComponent(int i) {
        return (DayNoteComponent) groupNotesListPanel.getComponent(i);
    } // end getNoteComponent


    @Override
    JComponent makeNewNote(int i) {
        DayNoteComponent dnc = new DayNoteComponent(this, i);
        dnc.setVisible(false);
        return dnc;
    } // end makeNewNote

    //<editor-fold desc="MouseListener methods">
    public void mouseClicked(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        source.requestFocus(); // Remove selection highlighting
        String s = source.getText();

        switch (s) {
            case "+":
                setOneForward();
                break;
            case "-":
                setOneBack();
                break;
            case "12":
                toggleMilitary();
                source.setText("24");
                return;
            case "24":
                toggleMilitary();
                source.setText("12");
                return;
        }

        updateGroup();
        updateHeader();
    } // end mouseClicked

    public void mouseEntered(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getText();
        switch (s) {
            case "+":
                s = "Click here to see next day";
                break;
            case "-":
                s = "Click here to see previous day";
                break;
            case "12":
                s = "Click here to see time in 12 hour format";
                break;
            case "24":
                s = "Click here to see time in 24 hour format";
                break;
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

    @Override
    protected void preClosePanel() {
        dayNoteDefaults.save();
        super.preClosePanel();
    }


    // This is called from AppTreePanel prior to display of this panel, needed when
    // the date choice has changed via controls in some other panel.
//    @Override
//    public void setDate(LocalDate theNewChoice) {
//        // If the new day is the same as the current one - return.
////        if (getTitle().equals(dtf.format(theNewChoice))) return;   // Don't know why this was (for a while) preferred over the one below.
//        if (dtf.format(theChoice).equals(dtf.format(theNewChoice))) return;
//
//        super.setDate(theNewChoice); // setDate is in CalendarNoteGroup.
//        updateHeader();
//    } // end setDate


    //----------------------------------------------------
    // Method Name: setDefaultIcon
    //
    // Called by the DayNoteComponent's
    //   popup menu handler for 'Set As Default'.
    //----------------------------------------------------
    public void setDefaultIcon(AppIcon li) {
        defaultIcon = li;
        dayNoteDefaults.defaultIconFileName = li.getDescription();
        setGroupChanged(true);
        preClosePanel();
        updateGroup();
    } // end setDefaultIcon


    public void shiftDown(int index) {
        if(!editable) return;
        if (index >= (lastVisibleNoteIndex - 1)) return;
        MemoryBank.debug("Day Shifting note down");
        DayNoteComponent dnc1, dnc2;
        dnc1 = (DayNoteComponent) groupNotesListPanel.getComponent(index);
        dnc2 = (DayNoteComponent) groupNotesListPanel.getComponent(index + 1);

        dnc1.swap(dnc2);
        dnc2.setActive();
    } // end shiftDown


    public void shiftUp(int index) {
        if(!editable) return;
        if (index == 0 || index == lastVisibleNoteIndex) return;
        System.out.println("Day Shifting note up");
        DayNoteComponent dnc1, dnc2;
        dnc1 = (DayNoteComponent) groupNotesListPanel.getComponent(index);
        dnc2 = (DayNoteComponent) groupNotesListPanel.getComponent(index - 1);

        dnc1.swap(dnc2);
        dnc2.setActive();
    } // end shiftUp


    //--------------------------------------------------------------
    // Method Name: toggleMilitary
    //
    // This is called from the 12/24 button
    //--------------------------------------------------------------
    private void toggleMilitary() {
        dayNoteDefaults.military = !dayNoteDefaults.military;
        // Need to reprint all time labels -
        for (int i = 0; i <= lastVisibleNoteIndex; i++) {
            getNoteComponent(i).resetTimeLabel();
        } // end for i
        setGroupChanged(true);
    } // end toggleMilitary

} // end class DayNoteGroupPanel


