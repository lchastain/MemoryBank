/*  Representation of a single Day Note.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;

public class DayNoteComponent extends IconNoteComponent {
    private static final long serialVersionUID = 1L;

    private static final int DAYNOTEHEIGHT = ICONNOTEHEIGHT;
    static Notifier optionPane;

    // The Members
    private DayNoteData myDayNoteData;
    private final NoteTimeLabel noteTimeLabel;

    // Private static values that are accessed from multiple contexts.
    private static final JMenuItem clearTimeMi;
    private static final JMenuItem setTimeMi;
    private static final JPopupMenu timePopup;

    static {
        // Normally just a wrapper for a JOptionPane, but tests may replace this
        // with their own instance, that would require no user interaction.
        optionPane = new Notifier() { };

        //-----------------------------------
        // Create the popup menus.
        //-----------------------------------
        timePopup = new JPopupMenu();
        timePopup.setFocusable(false);

        //--------------------------------------------
        // Define the popup menu items for a DayNoteComponent
        //--------------------------------------------
        clearTimeMi = new JMenuItem("Clear Time");
        timePopup.add(clearTimeMi);
        setTimeMi = new JMenuItem("Set Time");
        timePopup.add(setTimeMi);
    } // end static section


    DayNoteComponent(NoteGroupPanel ng, int i) {
        super(ng, i);
        index = i;

        //------------------
        // Graphical elements
        //------------------
        noteTimeLabel = new NoteTimeLabel();
        add(noteTimeLabel, "West");

        MemoryBank.trace();
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

    // Accessed by tests
    NoteTimeLabel getNoteTimeLabel() { return noteTimeLabel; }

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

        LocalTime theTime = LocalTime.parse(timeOfDayString);
        noteTimeLabel.setText(AppUtil.makeTimeString(theTime));

        // Colorize AM / PM
        if (theTime.getHour() > 11) {
            noteTimeLabel.setForeground(MemoryBank.pmColor);
        } else {
            noteTimeLabel.setForeground(MemoryBank.amColor);
        }
    } // end resetTimeLabel


    @Override
    void setEditable(boolean b) {
        super.setEditable(b);
        noteTimeLabel.setEditable(b);
    }

    @Override
    // Set the data for this component.  Do not send a null; if you want
    //   to unset the NoteData then call 'clear' instead.
    public void setNoteData(NoteData newNoteData) {
        if (newNoteData instanceof DayNoteData) {  // same type, but cast is still needed
            setDayNoteData((DayNoteData) newNoteData);
        } else if (newNoteData instanceof TodoNoteData) {
            setDayNoteData(new DayNoteData((TodoNoteData) newNoteData));
        } else if (newNoteData instanceof EventNoteData) {
            setDayNoteData(new DayNoteData((EventNoteData) newNoteData));
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
            myManager.shiftDown(index);
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
            myManager.shiftUp(index);
        } // end if
    } // end shiftUp


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

        myManager.setGroupChanged(true);
    } // end swap


//---------------------------------------------------------
// End of NoteComponent specific methods
//---------------------------------------------------------


//---------------------------------------------------------
// Inner Classes -
//---------------------------------------------------------

    class NoteTimeLabel extends JLabel implements
            ActionListener, MouseListener {

        private static final long serialVersionUID = 1L;

        boolean isActive;
        int timeWidth = 68;

        NoteTimeLabel() {
            clear(); // initializes as well as 'clears'.
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


        void setEditable(boolean b) {
            if(b) { // This limits us to only one mouseListener.
                // The limitation was more appropriate when an initial one was added during construction, but now
                // we no longer do that.  However, this more careful approach is still valid, just more verbose.
                MouseListener[] mouseListeners = getMouseListeners();
                boolean alreadyEditable = false;
                for (MouseListener ml : mouseListeners) {
                    if(ml.getClass() == this.getClass()) {
                        alreadyEditable = true;
                        break;
                    }
                }
                if (!alreadyEditable) { // Separate line, for debug clarity.
                    addMouseListener(this);
                }
            }
            // Luckily, if 'this' is not currently a MouseListener then the next line is a silent no-op,
            // and otherwise it does what we've asked.
            else removeMouseListener(this);
        }

        void setInactive() {
            setBorder(highBorder);
            isActive = false;
        } // end setInactive


        private void showTimeChooser() {
            DayNoteComponent.this.setBorder(redBorder);

            Frame theFrame = JOptionPane.getFrameForComponent(this);
            String timeOfDayString = myDayNoteData.getTimeOfDayString();

            LocalTime theTime;
            // The current time of the NoteData may not yet be set (ie, may be null).
            // But the timechooser expects to be initialized with some value (NOT null)
            // and since the user has invoked the chooser it makes more sense to
            // initialize it with the current time rather than try to start from a 'blank'
            // time that we know the user intends to change to something else, most likely
            // the current time, so why not start with that instead?  Anyway, it solves
            // the problem of how to initialize with a null - we just don't.  On the
            // other hand, though - if they clear it (which they can do from that UI)
            // then it can remain null.  Silly user.
            if(timeOfDayString != null) theTime = LocalTime.parse(timeOfDayString);
            else theTime = LocalTime.now();

            TimeChooser tc = new TimeChooser(theTime);
            int result = optionPane.showConfirmDialog(
                    theFrame,                     // for modality
                    tc,                           // UI Object
                    "Set the time for this note", // pane title bar
                    JOptionPane.OK_CANCEL_OPTION, // Option type
                    JOptionPane.QUESTION_MESSAGE); // Message type

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
            MemoryBank.debug("DayNoteComponent.NoteTimeLabel.actionPerformed ActionEvent: " + e.toString());
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

            int m = e.getModifiersEx();
            if(e.getButton()==MouseEvent.BUTTON3) { // Click of right mouse button.
//            if ((m & InputEvent.BUTTON3_DOWN_MASK) != 0) { // This doesn't work in mouseClicked; only 'pressed'
                if (e.getClickCount() >= 2) return;
                // Show the popup menu
                showTimePopup(e);
            } else if (e.getClickCount() == 2) { // Double click, left mouse button.
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
            myManager.setStatusMessage("Left click mouse to activate, then " +
                    "shift up/down arrows to adjust time");
        } // end mouseEntered

        public void mouseExited(MouseEvent e) {
            myManager.setStatusMessage(" ");
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }
        //---------------------------------------------------------
    } // end class NoteTimeLabel

} // end class DayNoteComponent



