/*  This class displays a group of DayNoteComponent.
 */

import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.util.Date;
import java.util.Vector;

public class DayNoteGroup extends CalendarNoteGroup
        implements iconKeeper, MouseListener {
    private static final long serialVersionUID = 1L;

    // Because of the way that NoteGroups get their NoteComponents,
    //   the defaultIcon MUST be present BEFORE the constructor
    //   for this class is called.  The only way that is possible
    //   is to assign it during the static section of this class.
    //------------------------------------------------------------------
    static String defaultIconFileName; // Accessed by MonthView.
    private static AppIcon defaultIcon;
    private static String defaultFileName;
    //------------------------------------------------------------------

    private static JLabel dayTitle;

    // Set by other NoteGroups (Event, Todo)
    static boolean blnNoteAdded;

    static {
        // Create the window title
        dayTitle = new JLabel();
        dayTitle.setHorizontalAlignment(JLabel.CENTER);
        dayTitle.setForeground(Color.white);
        dayTitle.setFont(Font.decode("Serif-bold-20"));

        defaultIconFileName = MemoryBank.logHome + File.separatorChar + "icons" + File.separatorChar;
        defaultIconFileName += "icon_not.gif";
        defaultFileName = "DayNoteDefaults";
        blnNoteAdded = false;

        // This will override the defaults only if the load is good.
        loadDefaults(); // May provide a different default icon.

        if (defaultIconFileName.equals("")) {
            // It IS possible that the user wants no default icon, has set it
            // that way and we have just loaded in their setting.
            MemoryBank.debug("Default DayNoteComponent Icon: <blank>");
            defaultIcon = new AppIcon();
        } else {
            MemoryBank.debug("Default DayNoteComponent Icon: " + defaultIconFileName);
            defaultIcon = new AppIcon(defaultIconFileName);
            AppIcon.scaleIcon(defaultIcon);
        } // end if/else

        MemoryBank.init();
    } // end of the static section


    DayNoteGroup() {
        super("Day Note");
        sdf.applyPattern("EEEE, MMMM d, yyyy");

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
        heading.add(dayTitle, "Center");

        if (MemoryBank.military) timeFormatButton.setText("12");
        heading.add(timeFormatButton, "East");  // spacer 56

        add(heading, BorderLayout.NORTH);

        updateHeader();
    } // end constructor


    private String getChoiceString() {
        return sdf.format(choice);
    } // end getChoiceString


    public AppIcon getDefaultIcon() {
        return defaultIcon;
    }

    //--------------------------------------------------------
    // Method Name: getNoteComponent
    //
    // Returns a DayNoteComponent that can be used to manipulate
    // component state as well as set/get underlying data.
    //--------------------------------------------------------
    public DayNoteComponent getNoteComponent(int i) {
        return (DayNoteComponent) groupNotesListPanel.getComponent(i);
    } // end getNoteComponent


    // Currently this only holds the defaultIconFileString.
    // Holding the 'military' boolean in MemoryBank.appOpts because more than one
    //   context needs access to it, but this is the only place where it gets set.
    private static void loadDefaults() {
        String FileName = MemoryBank.userDataHome + File.separatorChar + defaultFileName;
        Exception e = null;
        FileInputStream fis;
        String tmp = null;

        try {
            fis = new FileInputStream(FileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            tmp = (String) ois.readObject();
            ois.close();
            fis.close();
        } catch (FileNotFoundException fnfe) {
            // not a problem; create one using program defaults.
            MemoryBank.debug(defaultFileName + " file not found; using program defaults");
            saveDefaults();
            return;
        } catch (ClassCastException | ClassNotFoundException | IOException eee) {
            e = eee;
        } // end try/catch

        if (e != null) {
            MemoryBank.debug("Error in loading " + FileName + "; using defaults");
            return;
        } // end if

        defaultIconFileName = tmp;
        MemoryBank.debug("Loaded Default icon: " + defaultIconFileName);
    } // end loadDefaults


    //-------------------------------------------------------------------
    // Method Name: makeNewNote
    //
    //-------------------------------------------------------------------
    @Override
    JComponent makeNewNote(int i) {
        DayNoteComponent dnc = new DayNoteComponent(this, i);
        dnc.setVisible(false);
        return dnc;
    } // end makeNewNote

    //<editor-fold desc="MouseListener methods">
    public void mouseClicked(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
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
        setMessage(s);
    } // end mouseEntered

    public void mouseExited(MouseEvent e) {
        setMessage(" ");
    }
    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
    //---------------------------------------------------------
    //</editor-fold>

    //--------------------------------------------------------------
    // Method Name: recalc
    //
    // Repaints the display.
    // called from AppTreePanel for an 'undo' menu item selection.
    // This can be removed after we have a real 'undo'.
    //--------------------------------------------------------------
    public void recalc() {
        updateGroup();
        updateHeader();

        MemoryBank.debug("LogDays recalc - " + getChoiceString());
    } // end recalc


    private static void saveDefaults() {
        String FileName = MemoryBank.userDataHome + File.separatorChar + defaultFileName;
        MemoryBank.debug("Saving day option data in " + defaultFileName);
        try {
            FileOutputStream fos = new FileOutputStream(FileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(defaultIconFileName);
            oos.flush();
            oos.close();
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        } // end try/catch
    } // end saveDefaults


    // This is called from AppTreePanel.
    public void setChoice(Date d) {
        if (blnNoteAdded) {
            // This ensures that we will reload the day, even
            //   if it is already currently loaded.
            blnNoteAdded = false; // reset the flag
        } else {
            // If the new day is the same as the current one - return.
            if (sdf.format(choice).equals(sdf.format(d))) return;
        } // end if

        super.setChoice(d); // setChoice is in CalendarNoteGroup.
        updateHeader();
    } // end setChoice


    //----------------------------------------------------
    // Method Name: setDefaultIcon
    //
    // Called by the DayNoteComponent's
    //   popup menu handler for 'Set As Default'.
    //----------------------------------------------------
    public void setDefaultIcon(AppIcon li) {
        defaultIcon = li;
        defaultIconFileName = li.getDescription();
        saveDefaults();
        setGroupChanged();
        preClose();
        updateGroup();
    } // end setDefaultIcon


    void setGroupData(Object[] theGroup)  {
        NoteData.loading = true; // We don't want to affect the lastModDates!
        vectGroupData = AppUtil.mapper.convertValue(theGroup[0], new TypeReference<Vector<DayNoteData>>() { });
        NoteData.loading = false; // Restore normal lastModDate updating.
    }

    public void shiftDown(int index) {
        if (index >= (lastVisibleNoteIndex - 1)) return;
        System.out.println("Day Shifting note down");
        DayNoteComponent dnc1, dnc2;
        dnc1 = (DayNoteComponent) groupNotesListPanel.getComponent(index);
        dnc2 = (DayNoteComponent) groupNotesListPanel.getComponent(index + 1);

        dnc1.swap(dnc2);
        dnc2.setActive();
    } // end shiftDown


    public void shiftUp(int index) {
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
        MemoryBank.military = !MemoryBank.military;
        // Need to reprint all time labels -
        for (int i = 0; i <= lastVisibleNoteIndex; i++) {
            getNoteComponent(i).resetTimeLabel();
        } // end for i
    } // end toggleMilitary

    //--------------------------------------------------------------
    // Method Name: updateHeader
    //
    // This one line is broken out as a separate method to simplify
    //   the coding from the calling contexts above, and also to
    //   help them be more readable.
    //--------------------------------------------------------------
    private void updateHeader() {
        // Generate new title from current choice.
        dayTitle.setText(getChoiceString());
    } // end updateHeader

} // end class DayNoteGroup


