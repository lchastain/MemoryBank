/*  Representation of a single Day Note.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.util.Calendar;

public class DayNoteComponent extends IconNoteComponent {
    private static final long serialVersionUID = 1L;

    private static final int DAYNOTEHEIGHT = ICONNOTEHEIGHT;

    // The Members
    private DayNoteData myDayNoteData;
    private NoteTimeLabel noteTimeLabel;

    // Private static values that are accessed from multiple contexts.
    private static JMenuItem clearTimeMi;
    private static JMenuItem setTimeMi;
    private static JPopupMenu timePopup;

    static {
        //-----------------------------------
        // Create the popup menus.
        //-----------------------------------

        timePopup = new JPopupMenu();
        timePopup.setFocusable(false);

        //--------------------------------------------
        // Define the popup menu for a DayNoteComponent
        //--------------------------------------------
        clearTimeMi = new JMenuItem("Clear Time");
        timePopup.add(clearTimeMi);
        setTimeMi = new JMenuItem("Set Time");
        timePopup.add(setTimeMi);

    } // end static section


    DayNoteComponent(NoteGroup ng, int i) {
        super(ng, i);
        index = i;

        //------------------
        // Graphical elements
        //------------------
        noteTimeLabel = new NoteTimeLabel();
        add(noteTimeLabel, "West");

        MemoryBank.init();
    } // end constructor


    //-----------------------------------------------------------------
    // Method Name: clear
    //
    // Clears both the Graphical elements and the underlying data.
    //-----------------------------------------------------------------
    protected void clear() {
        noteTimeLabel.clear();  // Clear the Time Label
        super.clear();
    } // end clear


    //-----------------------------------------------------------------
    // Method Name: getNoteData
    //
    // Returns the data object that this component encapsulates
    //   and manages.
    //-----------------------------------------------------------------
    @Override
    public NoteData getNoteData() {
        return myDayNoteData;
    } // end getNoteData


    // Need to keep the height constant.
    public Dimension getPreferredSize() {
        int minWidth = 100; // For the Text Field
        minWidth += noteTimeLabel.getPreferredSize().width;
        minWidth += noteIcon.getPreferredSize().width;
        return new Dimension(minWidth, DAYNOTEHEIGHT);
    } // end getPreferredSize


    protected void initialize() {
        super.initialize();

        myDayNoteData.setTimeOfDayString(LocalTime.now().toString());
        resetTimeLabel();
    } // end initialize


    protected void makeDataObject() {
        myDayNoteData = new DayNoteData();
    } // end makeDataObject


    //-------------------------------------------------------------------
    // Method Name: noteActivated
    //
    //-------------------------------------------------------------------
    @Override
    protected void noteActivated(boolean noteIsActive) {
        if (!noteIsActive) {
            noteTimeLabel.setInactive();
        } // end if
        super.noteActivated(noteIsActive);
    } // end noteActivated


    //----------------------------------------------------------
    // Method Name: resetComponent
    //
    // Called after a change to the encapsulated data, to show
    //   the visual effects of the change.
    //----------------------------------------------------------
    @Override
    protected void resetComponent() {
        resetTimeLabel();
        super.resetComponent(); // the Icon (from there, the note text)
    } // end resetComponent


    // This method is called in response to a 'military' toggle
    //   as well as when initializing or updating the time.
    void resetTimeLabel() {
        if(!initialized) return;

        String timeOfDayString = myDayNoteData.getTimeOfDayString();
        if (timeOfDayString == null || timeOfDayString.isEmpty()) {
            // The following statement could be needed if a DayNoteComponent had
            //   had its time cleared, and then it was being shifted up or down.
            noteTimeLabel.setText("     ");  // enough room for 'HH:MM'
            // Otherwise, if it had been cleared and this method is called by
            //   a time format toggle, it is not needed but no harm done.
            return;
        } // end if

        // IntelliJ doesn't believe that the substring should be internal to the parseInt.
        String hoursString = timeOfDayString.substring(0, 2);
        int theHours = Integer.parseInt(hoursString);

        String minutesString = timeOfDayString.substring(3, 5);
        String theLabel;

        int meridian = Calendar.AM;
        if (theHours > 11) {
            meridian = Calendar.PM;
        }

        if (MemoryBank.military) {
            // drop out the colon and take just hours and minutes.
            theLabel = hoursString + minutesString;
        } else {  // Normalize to a 12-hour clock
            if (theHours > 12) {
                theLabel = (theHours - 12) + ":" + minutesString;
            } else {
                theLabel = hoursString + ":" + minutesString;
            }
        }
        noteTimeLabel.setText(theLabel);

        // Colorize AM / PM
        if (meridian == Calendar.AM) {
            noteTimeLabel.setForeground(MemoryBank.amColor);
        } else {     // Calendar.PM
            noteTimeLabel.setForeground(MemoryBank.pmColor);
        } // end if

    } // end resetTimeLabel


    @Override
    // Set the data for this component.  Do not send a null; if you want
    //   to unset the NoteData then call 'clear' instead.
    public void setNoteData(NoteData newNoteData) {
        if (newNoteData instanceof DayNoteData) {  // same type, but cast is still needed
            setDayNoteData((DayNoteData) newNoteData);
        } else if (newNoteData instanceof TodoNoteData) {
            setDayNoteData(((TodoNoteData) newNoteData).getDayNoteData(false));
        } else if (newNoteData instanceof EventNoteData) {
            setDayNoteData(((EventNoteData) newNoteData).getDayNoteData());
        } else {
            setDayNoteData(new DayNoteData(newNoteData));
        }
    } // end setNoteData


    //----------------------------------------------------------
    // Method Name: setNoteData
    //
    // Called by the overridden setNoteData and the 'swap' method.
    //----------------------------------------------------------
    private void setDayNoteData(DayNoteData newNoteData) {
        myDayNoteData = newNoteData;
//        myTime = newNoteData.getTimeOfDayDate();
        MemoryBank.debug("My time: " + myDayNoteData.getTimeOfDayString());

        // update visual components...
        initialized = true;  // without updating the 'lastModDate'
        resetComponent();
        setNoteChanged();
    } // end setNoteData


    protected void shiftDown() {
        if (noteTimeLabel.isActive) {
            // subtract one minute
            LocalTime lt = LocalTime.parse(myDayNoteData.getTimeOfDayString());
            lt = lt.minusMinutes(1);
            myDayNoteData.setTimeOfDayString(lt.toString());
            resetTimeLabel();
            DayNoteComponent.this.setNoteChanged();
        } else {
            myNoteGroup.shiftDown(index);
        } // end if
    } // end shiftDown

    protected void shiftUp() {
        if (noteTimeLabel.isActive) {
            // add one minute
            LocalTime lt = LocalTime.parse(myDayNoteData.getTimeOfDayString());
            lt = lt.plusMinutes(1);
            myDayNoteData.setTimeOfDayString(lt.toString());
            resetTimeLabel();
            DayNoteComponent.this.setNoteChanged();
        } else {
            myNoteGroup.shiftUp(index);
        } // end if
    } // end shiftUp


    //------------------------------------------------------------------
    // Method Name: swap
    //
    //------------------------------------------------------------------
    public void swap(DayNoteComponent dnc) {
        // Get a reference to the two data objects
        DayNoteData dnd1 = (DayNoteData) this.getNoteData();
        DayNoteData dnd2 = (DayNoteData) dnc.getNoteData();

        // Note: getNoteData and setNoteData are working with references
        //   to data objects.  If you 'get' data into a local variable
        //   and then later clear the component, you have also just
        //   cleared the data in your local variable because you never had
        //   a separate copy of the data object, just the reference to it.

        // So - copy the data objects.
        if (dnd1 != null) dnd1 = new DayNoteData(dnd1);
        if (dnd2 != null) dnd2 = new DayNoteData(dnd2);

        if (dnd1 == null) dnc.clear();
        else dnc.setDayNoteData(dnd1);

        if (dnd2 == null) this.clear();
        else this.setDayNoteData(dnd2);

        myNoteGroup.setGroupChanged();
    } // end swap


//---------------------------------------------------------
// End of NoteComponent specific methods
//---------------------------------------------------------


//---------------------------------------------------------
// Inner Classes -
//---------------------------------------------------------

    protected class NoteTimeLabel extends JLabel implements
            ActionListener, MouseListener {

        private static final long serialVersionUID = -7203985363643593551L;

        boolean isActive;
        int timeWidth = 68;

        NoteTimeLabel() {
            super();
            clear(); // initializes as well as 'clears'.
            addMouseListener(this);
            setHorizontalAlignment(JLabel.CENTER);
            setFont(Font.decode("DialogInput-bold-20"));
        } // end constructor

        private void clear() {
            setText("     ");  // enough room for 'HH:MM'
            setBorder(highBorder);
            isActive = false;
            if(myDayNoteData != null) myDayNoteData.setTimeOfDayString(null);
        } // end clear

        public Dimension getPreferredSize() {
            return new Dimension(timeWidth, DAYNOTEHEIGHT);
        } // end getPreferredSize


        void setInactive() {
            setBorder(highBorder);
            isActive = false;
        } // end setInactive


        private void showTimeChooser() {
            DayNoteComponent.this.setBorder(redBorder);

            Frame theFrame = JOptionPane.getFrameForComponent(this);
            LocalTime theTime = LocalTime.parse(myDayNoteData.getTimeOfDayString());

            TimeChooser tc = new TimeChooser(theTime);
            int result = JOptionPane.showConfirmDialog(
                    theFrame,                     // for modality
                    tc,                           // UI Object
                    "Set the time for this note", // pane title bar
                    JOptionPane.OK_CANCEL_OPTION, // Option type
                    JOptionPane.QUESTION_MESSAGE, // Message type
                    null);                       // icon

            DayNoteComponent.this.setBorder(null);
            if (result != JOptionPane.OK_OPTION) return;

            if (tc.getClearBoolean()) clear();
            else {
                myDayNoteData.setTimeOfDayString(tc.getChoice().toString());
                System.out.println("The time is: " + tc.getChoice());
                resetTimeLabel();
            } // end if
            DayNoteComponent.this.setNoteChanged();
        } // end showTimeChooser


        //------------------------------------------------------------
        // Method Name:  showTimePopup
        //
        // There is only one of each unique menu item for the entire
        //   DayNoteComponent class, no matter how many are created
        //   and inserted into a NoteGroup.  So, each time the menu
        //   is displayed, any previous component listeners must
        //   first be removed and then this one is added.
        //------------------------------------------------------------
        void showTimePopup(MouseEvent me) {
            ActionListener[] ala;

            ala = clearTimeMi.getActionListeners();
            for (ActionListener al : ala) clearTimeMi.removeActionListener(al);
            clearTimeMi.addActionListener(this);

            ala = setTimeMi.getActionListeners();
            for (ActionListener al : ala) setTimeMi.removeActionListener(al);
            setTimeMi.addActionListener(this);

            timePopup.show(me.getComponent(), me.getX(), me.getY());
        } // end showTimePopup


        //---------------------------------------------------------
        // Menu Item action handler for NoteTimeLabel
        //---------------------------------------------------------
        public void actionPerformed(ActionEvent e) {
            JMenuItem jm = (JMenuItem) e.getSource();
            String s = jm.getText();
            switch (s) {
                case "Clear Line":
                    clear();
                    break;
                case "Clear Time":
                    // Do not set myTime to null; just clear the visual indicator.
                    //  This leaves the note still initialized; critical to decisions
                    //  made at load time.
                    noteTimeLabel.clear();
                    break;
                case "Set Time":
                    noteTimeLabel.showTimeChooser();
                    break;
                default:   // Nothing else expected so print it out -
                    System.out.println(s);
                    break;
            }

            setNoteChanged();
        } // end actionPerformed


        //---------------------------------------------------------
        // MouseListener methods for the NoteTimeLabel
        //---------------------------------------------------------
        // Method Name: mouseClicked
        //
        //---------------------------------------------------------
        public void mouseClicked(MouseEvent e) {
            MemoryBank.event();
            // For all clicks - including Single left
            DayNoteComponent.this.setActive();
            if (!initialized) return;

            int m = e.getModifiers();
            // Single click, right mouse button.
            if ((m & InputEvent.BUTTON3_MASK) != 0) {
                if (e.getClickCount() >= 2) return;
                // Show the popup menu
                showTimePopup(e);

                // Double click, left mouse button.
            } else if (e.getClickCount() == 2) {
                showTimeChooser(); // bring up a mouse-controlled time interface

            } else { // Single Left Mouse Button
                if (isActive) {  // This implements a 'toggle'
                    setBorder(highBorder);
                    isActive = false;
                } else {
                    setBorder(lowBorder);
                    isActive = true;
                    if (getText().trim().equals("")) {
                        // This can happen if a previously initialized
                        //   note has had its time cleared.
//                        myTime = new Date();
                        DayNoteComponent.this.resetTimeLabel();
                        DayNoteComponent.this.setNoteChanged();
                    } // end if
                } // end if
            } // end if/else if
        } // end mouseClicked

        public void mouseEntered(MouseEvent e) {
            if (!initialized) return;
            myNoteGroup.setMessage("Left click mouse to activate, then " +
                    "shift up/down arrows to adjust time");
        } // end mouseEntered

        public void mouseExited(MouseEvent e) {
            myNoteGroup.setMessage(" ");
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }
        //---------------------------------------------------------
    } // end class NoteTimeLabel

} // end class DayNoteComponent



