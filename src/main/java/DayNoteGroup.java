/*  This class displays a group of DayNoteComponent.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DayNoteGroup extends CalendarNoteGroup
        implements IconKeeper, MouseListener {

    private static AppIcon defaultIcon;
    private final JLabel dayTitle;  // the JLabel is final; its text is not.
    static DayNoteDefaults dayNoteDefaults; // Also accessed by MonthView
    static boolean blnNoteAdded; // Set by other NoteGroups (Event, Todo)

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

        blnNoteAdded = false;
        MemoryBank.trace();
    } // end of the static section


    DayNoteGroup() {
        super("Day Note");

        // Create the panel title
        dayTitle = new JLabel();
        dayTitle.setHorizontalAlignment(JLabel.CENTER);
        dayTitle.setForeground(Color.white);
        dayTitle.setFont(Font.decode("Serif-bold-20"));

        dtf = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");

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

        if (dayNoteDefaults.military) timeFormatButton.setText("12");
        heading.add(timeFormatButton, "East");  // spacer 56

        add(heading, BorderLayout.NORTH);

        updateHeader();
        MemoryBank.trace();
    } // end constructor

    private String getChoiceString() {
        return dtf.format(theChoice);
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
    protected void preClose() {
        dayNoteDefaults.save();
        super.preClose();
    }


    // This is called from AppTreePanel.
    public void setDate(LocalDate theNewChoice) {
        if (blnNoteAdded) {
            // This ensures that we will reload the day, even
            //   if it is already currently loaded.
            blnNoteAdded = false; // reset the flag
        } else {
            // If the new day is the same as the current one - return.
            if (dtf.format(theChoice).equals(dtf.format(theNewChoice))) return;
        } // end if

        super.setDate(theNewChoice); // setDate is in CalendarNoteGroup.
        updateHeader();
    } // end setDate


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
        preClose();
        updateGroup();
    } // end setDefaultIcon


    // Learned how to do this (convert an ArrayList element that is a LinkedHashMap, to a Vector of <my custom class>),
    // from: https://stackoverflow.com/questions/15430715/casting-linkedhashmap-to-complex-object
    // Previously, I just cycled thru the LinkedHashMap by accepting the entries as Object, then converted them
    // to JSON string, then parsed the string back in to a DayNoteData and added it to a new Vector.  But that was
    // a several-line method; this conversion is a one-liner, and my version had the possibility of throwing an
    // Exception that needed to be caught.
//    void setGroupData(Object[] theGroup)  {
//        int theLength = theGroup.length;
//        // This method should not be called if 'theGroup' is null.  Otherwise it
//        // will be an object array but the array could still be empty, somehow.
//        if(theLength == 0) return;
//
//        // Now the length can either be 1 or 2.  If 2 then it will be a group properties and
//        // the group data vector, but this is a relatively new structure where previously there
//        // were no properties for this class, so there are several years-worth of data files
//        // already out there, where the only element is the group data, and rather than attempting
//        // to fix old data, the decision is to examine the content first, then load the correct type.
//        // As a developer I know this is not the best solution but it does work and I'm lazy, so I'm
//        // going with it for now.  One possible future 'data fix' (other than writing a DataFix program)
//        // could come when/if the app is ever ported into a database.
//        BaseData.loading = true; // We don't want to affect the lastModDates!
//        if(theLength == 1) {
//            // There are two cases where there might only be one element in the object array:
//            // 1.  Old, legacy data that was originally saved without GroupProperties.
//            // 2.  New Group Properties with LinkedEntityData (linkTargets) but no group data.
//            String theClass = theGroup[0].getClass().getSimpleName();
//            System.out.println("The DayNoteData class type is: " + theClass);
//            if(theClass.equals("java.util.ArrayList")) { // old structure; this is just group data.
//                groupDataVector = AppUtil.mapper.convertValue(theGroup[0], new TypeReference<Vector<DayNoteData>>() { });
//            } else { // new structure; this is a GroupProperties.  The expected class here is java.util.LinkedHashMap
//                myProperties = AppUtil.mapper.convertValue(theGroup[0], GroupProperties.class);
//            }
//        } else { // 2 (or more, but more would mean that there has been yet another structure change)
//            myProperties = AppUtil.mapper.convertValue(theGroup[0], GroupProperties.class);
//            groupDataVector = AppUtil.mapper.convertValue(theGroup[1], new TypeReference<Vector<DayNoteData>>() { });
//        }
//        BaseData.loading = false; // Restore normal lastModDate updating.
//    }

    public void shiftDown(int index) {
        if (index >= (lastVisibleNoteIndex - 1)) return;
        MemoryBank.debug("Day Shifting note down");
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
        dayNoteDefaults.military = !dayNoteDefaults.military;
        // Need to reprint all time labels -
        for (int i = 0; i <= lastVisibleNoteIndex; i++) {
            getNoteComponent(i).resetTimeLabel();
        } // end for i
        setGroupChanged(true);
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


