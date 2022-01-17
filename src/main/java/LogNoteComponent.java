/*  Representation of a single Day Note.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LogNoteComponent extends NoteComponent {
    private static final long serialVersionUID = 1L;
    private static final int LOGNOTEHEIGHT = 24;
    private final DateTimeFormatter dtf;

    static Notifier optionPane;

    // The Members
    private LogData myLogData;
    private final NoteDateLabel noteDateLabel;

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
        // Define the popup menu items for a LogNoteComponent
        //--------------------------------------------
        clearTimeMi = new JMenuItem("Clear Time");
        timePopup.add(clearTimeMi);
        setTimeMi = new JMenuItem("Set Time");
        timePopup.add(setTimeMi);
    } // end static section


    LogNoteComponent(NoteGroupPanel ng, int i) {
        super(ng, i);
        index = i;
        dtf = DateTimeFormatter.ofPattern("dd MMM yyyy");

        //------------------
        // Graphical elements
        //------------------
        noteDateLabel = new NoteDateLabel();
        add(noteDateLabel, BorderLayout.WEST);

        MemoryBank.trace();
    } // end constructor


    //-----------------------------------------------------------------
    // Method Name: clear
    //
    // Clears both the Graphical elements and the underlying data.
    //-----------------------------------------------------------------
    @Override
    protected void clear() {
        noteDateLabel.clear();  // Clear the Date Label
        super.clear();
    } // end clear


    // Returns the data object that this component encapsulates and manages.
    @Override
    public NoteData getNoteData() {
        return myLogData;
    } // end getNoteData


    // Need to keep the height constant.
//    public Dimension getPreferredSize() {
//        int minWidth = 100; // For the Text Field
//        minWidth += noteDateLabel.getPreferredSize().width;
//        return new Dimension(minWidth, LOGNOTEHEIGHT);
//    } // end getPreferredSize


    @Override
    protected void initialize() {
        super.initialize();

        myLogData.setLogDate(LocalDate.now());

//        myLogNoteData.setTimeOfDayString(LocalTime.now().toString());
        resetDateLabel();
    } // end initialize


    @Override
    protected void makeDataObject() {
        myLogData = new LogData();
    } // end makeDataObject


    @Override
    protected void noteActivated(boolean noteIsActive) {
        if (!noteIsActive) {
            noteDateLabel.setInactive();
        } // end if
        super.noteActivated(noteIsActive);
    } // end noteActivated


    // Called after a change to the encapsulated data, to show the visual effects of the change.
    @Override
    protected void resetComponent() {
        resetDateLabel();
        super.resetComponent(); // for the note text
    } // end resetComponent


    // This method is called when initializing the component or updating the date.
    void resetDateLabel() {
        if(!initialized) return;
        LocalDate logDate = myLogData.getLogDate();

        if (logDate == null) {
            // The following statement could be needed if a LogNoteComponent had
            //   had its date cleared, and then it was being shifted up or down.
            noteDateLabel.setText("           ");  // enough room for 'dd MMM yyyy'
            return;
        } // end if

        String dateString = dtf.format(logDate);
        noteDateLabel.setText(dateString);

    } // end resetDateLabel



    @Override
    void setEditable(boolean b) {
        super.setEditable(b);
        noteDateLabel.setEditable(b);
    }

    @Override
    // Set the data for this component.  Do not send a null; if you want
    //   to unset the NoteData then call 'clear' instead.
    public void setNoteData(NoteData newNoteData) {
        if (newNoteData instanceof LogData) {
            // It is already the right type, but cast is still needed because of this method's polymorphic signature.
            setLogNoteData((LogData) newNoteData);
        } else { // Not 'my' type, but we can make it so (coming from a 'paste' operation).
            // But this one will get a default (today) date, since the base class does not have one.
            setLogNoteData(new LogData(newNoteData));
        }

    } // end setNoteData


    // Called by the overridden setNoteData (above) and the 'swap' method.
    private void setLogNoteData(LogData newNoteData) {
        myLogData = newNoteData;

        // update visual components...
        initialized = true;  // without updating the 'lastModDate'
        resetComponent();
        setNoteChanged();
    } // end setLogNoteData


    @Override
    protected void shiftDown() {
        if (noteDateLabel.isActive) {
            // subtract one day
            LocalDate ld = myLogData.getLogDate();
            ld = ld.minusDays(1);
            myLogData.setLogDate(ld);
            resetDateLabel();
            LogNoteComponent.this.setNoteChanged();
        } else {
            myManager.shiftDown(index);
        } // end if
    } // end shiftDown

    @Override
    protected void shiftUp() {
        if (noteDateLabel.isActive) {
            // add one day
            LocalDate ld = myLogData.getLogDate();
            ld = ld.plusDays(1);
            myLogData.setLogDate(ld);
            resetDateLabel();
            LogNoteComponent.this.setNoteChanged();
        } else {
            myManager.shiftUp(index);
        } // end if
    } // end shiftUp


    @Override
    public void swap(NoteComponent lnc) {
        // Get a reference to the two data objects
        LogData lnd1 = (LogData) this.getNoteData();
        LogData lnd2 = (LogData) lnc.getNoteData();

        // Note: getNoteData and setNoteData are working with references
        //   to data objects.  If you 'get' data into a local variable
        //   and then later clear the component, you have also just
        //   cleared the data in your local variable because you never had
        //   a separate copy of the data object, just the reference to it.

        // So - copy the data objects.
        if (lnd1 != null) lnd1 = new LogData(lnd1);
        if (lnd2 != null) lnd2 = new LogData(lnd2);

        if (lnd1 == null) lnc.clear();
        else lnc.setNoteData(lnd1);

        if (lnd2 == null) this.clear();
        else this.setNoteData(lnd2);

        myManager.setGroupChanged(true);
    } // end swap


//---------------------------------------------------------
// End of NoteComponent specific methods
//---------------------------------------------------------


//---------------------------------------------------------
// Inner Classes -
//---------------------------------------------------------

    class NoteDateLabel extends JLabel implements ActionListener {
        private static final long serialVersionUID = 1L;

        boolean isActive;
        int dateWidth = 100;
        MouseAdapter ma;

        NoteDateLabel() {
            isActive = false;
            setBorder(highBorder);
            setHorizontalAlignment(JLabel.CENTER);
            setFont(Font.decode("Dialog-bold-12"));

            ma = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    MemoryBank.event();
                    // For all clicks - including Single left
                    LogNoteComponent.this.setActive();
                    if (!initialized) return;

                    int m = e.getModifiersEx();
                    if(e.getButton()==MouseEvent.BUTTON3) { // Click of right mouse button.
                        if (e.getClickCount() >= 2) return;
                        // Show the popup menu
//                showTimePopup(e);
                    } else if (e.getClickCount() == 2) { // Double click, left mouse button.
//                showTimeChooser(); // bring up a mouse-controlled time interface

                    } else { // Single Left Mouse Button
                        if (isActive) {  // This implements a 'toggle'
                            setBorder(highBorder);
                            isActive = false;
                        } else {
                            setBorder(lowBorder);
                            isActive = true;
                            if (getText().trim().equals("")) {
                                // This can happen if a previously initialized
                                //   note has had its date cleared.
//                        myTime = new Date();
                                LogNoteComponent.this.resetDateLabel();
                                LogNoteComponent.this.setNoteChanged();
                            } // end if
                        } // end if
                    } // end if/else if
                } // end mouseClicked

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!initialized) return;
                    myManager.setStatusMessage("Left click mouse to activate, then " +
                            "shift up/down arrows to adjust date");
                } // end mouseEntered
            };
        } // end constructor


        // Clear both the visual and data elements of this Component
        private void clear() {
            setText("           ");  // enough room for 'dd MMM yyyy'
            setBorder(highBorder);
            isActive = false;
            if(myLogData != null) {
                myLogData.setLogDate(null);
            }
        } // end clear

        public Dimension getPreferredSize() {
            return new Dimension(dateWidth, LOGNOTEHEIGHT);
        } // end getPreferredSize


        void setEditable(boolean b) {
            if(b) { // This limits us to only one mouseListener per NoteDateLabel.
                // The limitation is needed because mouse responsiveness needs to track along with editability.
                MouseListener[] mouseListeners = getMouseListeners();
                boolean alreadyEditable = false;
                for (MouseListener ml : mouseListeners) {
                    if(ml.equals(ma)) {
                        alreadyEditable = true;
                        break;
                    }
                }
                if (!alreadyEditable) { // Separate line, for debug clarity.
                    addMouseListener(ma);
                }
            }
            // If ma is not currently a MouseListener on this component then the next line is a silent no-op,
            // and otherwise it does what we've asked.
            else removeMouseListener(ma);
        }

        void setInactive() {
            setBorder(highBorder);
            isActive = false;
        } // end setInactive


     // We will need a showDateChooser
//        private void showTimeChooser() {
//            LogNoteComponent.this.setBorder(redBorder);
//
//            Frame theFrame = JOptionPane.getFrameForComponent(this);
//            String timeOfDayString = myLogNoteData.getTimeOfDayString();
//
//            LocalTime theTime;
//            // The current time of the NoteData may not yet be set (ie, may be null).
//            // But the timechooser expects to be initialized with some value (NOT null)
//            // and since the user has invoked the chooser it makes more sense to
//            // initialize it with the current time rather than try to start from a 'blank'
//            // time that we know the user intends to change to something else, most likely
//            // the current time, so why not start with that instead?  Anyway, it solves
//            // the problem of how to initialize with a null - we just don't.  On the
//            // other hand, though - if they clear it (which they can do from that UI)
//            // then it can remain null.  Silly user.
//            if(timeOfDayString != null) theTime = LocalTime.parse(timeOfDayString);
//            else theTime = LocalTime.now();
//
//            TimeChooser tc = new TimeChooser(theTime);
//            int result = optionPane.showConfirmDialog(
//                    theFrame,                     // for modality
//                    tc,                           // UI Object
//                    "Set the time for this note", // pane title bar
//                    JOptionPane.OK_CANCEL_OPTION, // Option type
//                    JOptionPane.QUESTION_MESSAGE); // Message type
//
//            LogNoteComponent.this.setBorder(null);
//            if (result != JOptionPane.OK_OPTION) return;
//
//            if (tc.getClearBoolean()) clear();
//            else {
//                myLogNoteData.setTimeOfDayString(tc.getChoice().toString());
//                System.out.println("The time is: " + tc.getChoice());
//                resetDateLabel();
//            } // end if
//            LogNoteComponent.this.setNoteChanged();
//        } // end showTimeChooser


        //---------------------------------------------------------
        // Menu Item action handler for NoteDateLabel
        //---------------------------------------------------------
        public void actionPerformed(ActionEvent e) {
            MemoryBank.debug("LogNoteComponent.NoteDateLabel.actionPerformed ActionEvent: " + e.toString());
            JMenuItem jm = (JMenuItem) e.getSource();
            String s = jm.getText();
            switch (s) {
                case "Clear Line":
                    clear();
                    break;
                case "Clear Date":
                    // Do not set logDateString to null; just clear the visual indicator.
                    //  This leaves the note still initialized; critical to decisions
                    //  made at load time.
                    noteDateLabel.clear();
                    break;
//                case "Set Time":
//                    noteTimeLabel.showTimeChooser();
//                    break;
                default:   // Nothing else expected so print it out -
                    System.out.println(s);
                    break;
            }

            setNoteChanged();
        } // end actionPerformed

    } // end class NoteDateLabel


} // end class LogNoteComponent



