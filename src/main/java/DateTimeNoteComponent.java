/*  Representation of a single Day Note.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DateTimeNoteComponent extends NoteComponent {
    @Serial
    private static final long serialVersionUID = 1L;

    private final DateTimeFormatter dtf;
    static Notifier optionPane;

    // The Members
    private DayNoteData myDayNoteData;
    private final NoteDateLabel noteDateLabel;
    private final NoteTimeLabel noteTimeLabel;

    // Private static values that are accessed from multiple local methods.
    private static final JMenuItem clearDateMi;
    private static final JMenuItem setDateMi;
    private static final JPopupMenu datePopup;
    private static final JMenuItem clearTimeMi;
    private static final JMenuItem setTimeMi;
    private static final JPopupMenu timePopup;

    static {
        // optionPane is normally just a wrapper for a JOptionPane, but tests may replace it
        // with their own instance, that would require no user interaction.
        optionPane = new Notifier() {
        };

        //-----------------------------------
        // Create the popup menus.
        //-----------------------------------
        datePopup = new JPopupMenu();
        datePopup.setFocusable(false);
        timePopup = new JPopupMenu();
        timePopup.setFocusable(false);

        //--------------------------------------------
        // Define the popup menu items for a DateTimeNoteComponent
        //--------------------------------------------
        clearDateMi = new JMenuItem("Clear Date");
        datePopup.add(clearDateMi);
        setDateMi = new JMenuItem("Set Date");
        datePopup.add(setDateMi);
        clearTimeMi = new JMenuItem("Clear Time");
        timePopup.add(clearTimeMi);
        setTimeMi = new JMenuItem("Set Time");
        timePopup.add(setTimeMi);
    } // end static section


    DateTimeNoteComponent(NoteGroupPanel ng, int i) {
        super(ng, i);

        dtf = DateTimeFormatter.ofPattern("d MMM yyyy");
        makeDataObject(); // Child classes of NoteComponent override this method and set their own data types.

        JPanel westPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        setLayout(new BorderLayout());

        index = i;

        //------------------
        // Graphical elements
        //------------------
        noteDateLabel = new NoteDateLabel();
        noteTimeLabel = new NoteTimeLabel();
        westPanel.add(noteDateLabel);
        westPanel.add(noteTimeLabel);

        add(westPanel, BorderLayout.WEST);

        MemoryBank.trace();
    } // end constructor


    // Clears both the Graphical elements and the underlying data.
    protected void clear() {
        noteDateLabel.clear();
        noteTimeLabel.clear();  // Clear the Time Label
        super.clear();
    } // end clear


    // Returns the data object that this component encapsulates and manages.
    @Override
    public NoteData getNoteData() {
        return myDayNoteData;
    }

    // (will/might be) Accessed by tests
    NoteTimeLabel getNoteTimeLabel() {
        return noteTimeLabel;
    }

    // Need to keep the height constant.
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(super.getMaximumSize().width, componentHeight);
    } // end getMaximumSize

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    } // end getMinimumSize

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, componentHeight);
    } // end getPreferredSize

    @Override
    String getStatusMessage(int textStatus) {
        String s = " ";

        switch (textStatus) {
            case NEEDS_TEXT -> s = "Click here to enter text for this note.";
            case HAS_BASE_TEXT -> s = "Double-click to add an extended note.";
            case HAS_EXT_TEXT -> // This gives away the 'hidden' text, if there is no primary (blue) text.
                    s = "Double-click to see/edit the extended note.";
        } // end switch
        return s;
    } // end getStatusMessage

    protected void initialize() {
        super.initialize();

        myDayNoteData.setTimeOfDayString(LocalTime.now().toString());
        resetComponent();
    } // end initialize


    @Override
    // There IS no DateTimeNoteData class, but the DayNoteData is sooo close to what is needed,
    //   that we just go ahead and use it as if it 'fits'.  And to MAKE it fit, we are
    //   hijacking the iconFileString (not needed here) and replacing it with a LocalDate string
    //   (that IS needed).  This saves us from having to have a separate data class and NoteGroup.
    protected void makeDataObject() {
        myDayNoteData = new DayNoteData();
        myDayNoteData.setIconFileString(LocalDate.now().toString());
    } // end makeDataObject


    @Override
    protected void noteActivated(boolean noteIsActive) {
        if (!initialized) return; // No need for further action, on notes where nothing was ever done.

        if (!noteIsActive) {
            super.noteActivated(noteIsActive);
            noteDateLabel.setInactive();
            noteTimeLabel.setInactive();
        } // end if
    } // end noteActivated


    //----------------------------------------------------------
    // Method Name: resetComponent
    //
    // Called after a change to the encapsulated data, to show
    //   the visual effects of the change without affecting the
    //   'lastModDate' since this method may be getting called
    //   after a data load, or a non-data change such as a swap.
    //----------------------------------------------------------
    @Override
    protected void resetComponent() {
        super.resetComponent();
        resetDateLabel();
        resetTimeLabel();
    } // end resetComponent


    // This method is called when initializing the component or updating the date.
    void resetDateLabel() {
        if (!initialized) return;
        String theDateString = myDayNoteData.getIconFileString();

        if (theDateString != null && !theDateString.isBlank()) {
            LocalDate noteDate = LocalDate.parse(theDateString);
            String dateString = dtf.format(noteDate);
            noteDateLabel.setText(dateString);
        } else {
            // The following statement could be needed if a DateTimeNoteComponent had
            //   had its date cleared, and then it was being shifted up or down.
            noteDateLabel.setText("           ");  // enough room for 'dd MMM yyyy'
        }
    } // end resetDateLabel


    // This method is called when initializing or updating the time.
    void resetTimeLabel() {
        if (!initialized) return;

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
        noteDateLabel.setEditable(b);
        noteTimeLabel.setEditable(b);
    }

    //----------------------------------------------------------
    // Method Name: setNoteData
    //
    // Called by the overridden setNoteData and the 'swap' method.
    //----------------------------------------------------------
    private void setDayNoteData(DayNoteData newNoteData) {
        myDayNoteData = newNoteData;
        MemoryBank.debug("My time: " + myDayNoteData.getTimeOfDayString());

        // update visual components...
        initialized = true;  // without updating the 'lastModDate'
        resetText();
        resetComponent();
        setNoteChanged();
    } // end setDayNoteData

    @Override
    void setInactive() {
        noteDateLabel.setInactive();
        noteTimeLabel.setInactive();
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

    @Override
    protected void shiftDown() {
        if (noteTimeLabel.isActive) { // This section dup'd from DayNoteComponent
            // subtract one minute
            LocalTime lt = LocalTime.parse(myDayNoteData.getTimeOfDayString());
            lt = lt.minusMinutes(1);
            myDayNoteData.setTimeOfDayString(lt.toString());
            resetTimeLabel();
            DateTimeNoteComponent.this.setNoteChanged();
        } else if (noteDateLabel.isActive) {
            LocalDate ld = getDateFromDayNoteData();
            if (ld != null) {
                ld = ld.minusDays(1);    // subtract one day
                myDayNoteData.setIconFileString(ld.toString());
                resetDateLabel();
                setNoteChanged();
            }
        } else {
            myManager.shiftDown(index);
        } // end if
    } // end shiftDown

    // The Date for this note is 'hidden' in the iconFileString; a 'hijacked' field that is otherwise unused.
    private LocalDate getDateFromDayNoteData() {
        if (myDayNoteData.iconFileString == null) return null;
        return LocalDate.parse(myDayNoteData.iconFileString);
    }

    @Override
    protected void shiftUp() {
        if (noteTimeLabel.isActive) { // This section dup'd from DayNoteComponent
            // add one minute
            LocalTime lt = LocalTime.parse(myDayNoteData.getTimeOfDayString());
            lt = lt.plusMinutes(1);
            myDayNoteData.setTimeOfDayString(lt.toString());
            resetTimeLabel();
            DateTimeNoteComponent.this.setNoteChanged();
        } else if (noteDateLabel.isActive) {
            LocalDate ld = getDateFromDayNoteData();
            if (ld != null) {
                ld = ld.plusDays(1);    // add one day
                myDayNoteData.setIconFileString(ld.toString());
                resetDateLabel();
                setNoteChanged();
            }
        } else {
            myManager.shiftUp(index);
        } // end if
    } // end shiftUp


    @Override
    // We did not extend a DayNoteComponent, so this code from there has been dup'd.
    public void swap(NoteComponent dnc) {
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
        else dnc.setNoteData(dnd1);

        if (dnd2 == null) this.clear();
        else this.setNoteData(dnd2);

        myManager.setGroupChanged(true);
    } // end swap

//---------------------------------------------------------
// End of NoteComponent overridden methods
//---------------------------------------------------------

    class NoteDateLabel extends JLabel implements ActionListener {
        @Serial
        private static final long serialVersionUID = 1L;
        private static final YearView yvDateChooser;

        boolean isActive;
        int dateWidth = 90;
        MouseAdapter ma;

        static {
            yvDateChooser = new YearView();
        }

        NoteDateLabel() {
            clear();
            setHorizontalAlignment(JLabel.CENTER);
            setFont(Font.decode("Dialog-bold-12"));

            ma = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    MemoryBank.event();

                    // For all clicks - including Single left
                    noteTimeLabel.setInactive();
                    setActive();
                    if (!initialized) return;

                    if (e.getButton() == MouseEvent.BUTTON3) { // Click of right mouse button.
                        if (e.getClickCount() >= 2) return;  // We don't handle a double click on this component.
                        // Show the popup menu
                        showDatePopup(e);
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
                                myDayNoteData.setIconFileString(LocalDate.now().toString());
                                resetDateLabel();
                                setNoteChanged();
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

        @Override
        public void actionPerformed(ActionEvent e) {
            MemoryBank.debug("DayNoteComponent.DateTimeLabel.actionPerformed ActionEvent: " + e.toString());
            JMenuItem jm = (JMenuItem) e.getSource();
            String s = jm.getText();
            switch (s) {
                case "Clear Date" -> noteDateLabel.clear();
                case "Set Date" -> { // Show the YearView date chooser
                    yvDateChooser.setView(LocalDate.now()); // In case the current date is a null.
                    yvDateChooser.setChoice(getDateFromDayNoteData());
                    showDateChooser();
                    LocalDate newDate = yvDateChooser.getChoice();
                    // Reselecting the same date has same effect as 'X'ing the dialog - no date change.
                    // To clear a date:  right-click the current selection, then close the dialog.
                    System.out.println("The date retrieved from the chooser: " + newDate);
                    if(newDate == null) {
                        myDayNoteData.iconFileString = null; // Cannot use the 'set' to null this out; do it directly.
                    } else {
                        myDayNoteData.setIconFileString(newDate.toString());
                    }
                    myNoteGroupPanel.myNoteGroup.setGroupChanged(true);
                    resetDateLabel();
                }
                default ->   // Nothing else expected so print it out -
                        System.out.println(s);
            }
            setNoteChanged();
        } // end actionPerformed

        // Clear both the visual and data elements of this Component
        private void clear() {
            setText("           ");  // enough room for 'dd MMM yyyy'
            //setText("date       ");
            setBorder(highBorder);
            isActive = false;
            if (myDayNoteData != null) { // If not already clear, then clear it.
                myDayNoteData.iconFileString = null;
            }
        } // end clear

        public Dimension getPreferredSize() {
            return new Dimension(dateWidth, componentHeight);
        } // end getPreferredSize


        void setEditable(boolean b) {
            if (b) { // This limits us to only one mouseListener per NoteDateLabel.
                // The limitation is needed because mouse responsiveness needs to track along with editability.
                MouseListener[] mouseListeners = getMouseListeners();
                boolean alreadyEditable = false;
                for (MouseListener ml : mouseListeners) {
                    if (ml.equals(ma)) {
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

        private void showDateChooser() {
            // Make a dialog window to choose a date from a Year.
            Frame f = JOptionPane.getFrameForComponent(this);
            JDialog tempwin = new JDialog(f, true);

            tempwin.getContentPane().add(yvDateChooser, BorderLayout.CENTER);
            tempwin.setTitle("Select a new date for this log entry.");
            tempwin.setSize(yvDateChooser.getPreferredSize());
            tempwin.setResizable(false);
            yvDateChooser.setDialog(tempwin, 1);

            // Center the dialog relative to the main frame.
            tempwin.setLocationRelativeTo(f);

            // Go modal -
            tempwin.setVisible(true);
        } // end showDateChooser

        //------------------------------------------------------------
        // Method Name:  showDatePopup
        //
        // There is only one of each unique menu item for the entire
        //   NoteDateLabel class, no matter how many are created
        //   and inserted into a NoteGroup.  So, each time the menu
        //   is displayed, any previous component listeners must
        //   first be removed and then this one is added.
        //------------------------------------------------------------
        void showDatePopup(MouseEvent me) {
            ActionListener[] ala;

            ala = clearDateMi.getActionListeners();
            for (ActionListener al : ala) clearDateMi.removeActionListener(al);
            clearDateMi.addActionListener(this);

            ala = setDateMi.getActionListeners();
            for (ActionListener al : ala) setDateMi.removeActionListener(al);
            setDateMi.addActionListener(this);

            datePopup.show(me.getComponent(), me.getX(), me.getY());
        } // end showDatePopup

    } // end class NoteDateLabel

    class NoteTimeLabel extends JLabel implements ActionListener, MouseListener {
        @Serial
        private static final long serialVersionUID = 1L;

        boolean isActive;
        int timeWidth = 56;

        NoteTimeLabel() {
            clear(); // initializes as well as 'clears'.
            setHorizontalAlignment(JLabel.CENTER);
            setFont(Font.decode("Dialog-bold-14"));
        } // end constructor

        private void clear() {
            setText("     ");  // enough room for 'HH:MM'
            //setText("time ");  // enough room for 'HH:MM'
            setBorder(highBorder);
            isActive = false;
            if (myDayNoteData != null) myDayNoteData.setTimeOfDayString("");
        } // end clear

        void setEditable(boolean b) {
            if (b) { // This limits us to only one mouseListener.
                // The limitation was more appropriate when an initial one was added during construction, but now
                // we no longer do that.  However, this more careful approach is still valid, just more verbose.
                MouseListener[] mouseListeners = getMouseListeners();
                boolean alreadyEditable = false;
                for (MouseListener ml : mouseListeners) {
                    if (ml.getClass() == this.getClass()) {
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
            DateTimeNoteComponent.this.setBorder(redBorder);

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
            if (timeOfDayString != null && !timeOfDayString.isBlank()) theTime = LocalTime.parse(timeOfDayString);
            else theTime = LocalTime.now();

            TimeChooser tc = new TimeChooser(theTime);
            int result = optionPane.showConfirmDialog(
                    theFrame,                     // for modality
                    tc,                           // UI Object
                    "Set the time for this note", // pane title bar
                    JOptionPane.OK_CANCEL_OPTION, // Option type
                    JOptionPane.QUESTION_MESSAGE); // Message type

            DateTimeNoteComponent.this.setBorder(null);
            if (result != JOptionPane.OK_OPTION) return;

            if (tc.getClearBoolean()) clear();
            else {
                myDayNoteData.setTimeOfDayString(tc.getChoice().toString());
                System.out.println("The time is: " + tc.getChoice());
                resetTimeLabel();
            } // end if
            DateTimeNoteComponent.this.setNoteChanged();
        } // end showTimeChooser


        //------------------------------------------------------------
        // Method Name:  showTimePopup
        //
        // There is only one of each unique menu item for the entire
        //   NoteTimeLabel class, no matter how many are created
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
                case "Clear Time" ->
                        // Do not set myTime to null; just clear the visual indicator.
                        //  This leaves the note still initialized; critical to decisions
                        //  made at load time.
                        noteTimeLabel.clear();
                case "Set Time" -> noteTimeLabel.showTimeChooser();
                default ->   // Nothing else expected so print it out -
                        System.out.println(s);
            }

            setNoteChanged();
        } // end actionPerformed

        public Dimension getPreferredSize() {
            return new Dimension(timeWidth, componentHeight);
        } // end getPreferredSize


        //---------------------------------------------------------
        // MouseListener methods for the NoteTimeLabel
        //---------------------------------------------------------
        public void mouseClicked(MouseEvent e) {
            MemoryBank.event();

            // For all clicks - including Single left
            noteDateLabel.setInactive();
            setActive(); // This is a NoteComponent base method - for the text
            if (!initialized) return;

            if (e.getButton() == MouseEvent.BUTTON3) { // Click of right mouse button.
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
                        //   note has had its time cleared.  But now the user has clicked
                        //   on the 'blank' time, and so we fill in the current time.
                        myDayNoteData.setTimeOfDayString(LocalTime.now().toString());
                        resetTimeLabel();
                        DateTimeNoteComponent.this.setNoteChanged();
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

    //--------------------------------------------------------------------

    public static void main(String[] args) {
        // Setup of overhead needed by this and its base classes -
        MemoryBank.debug = true;
        MemoryBank.userEmail = "lex@doughmain.net";
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
        new AppTreePanel(new JFrame(), MemoryBank.appOpts);
        GroupInfo theGroupInfo = new GroupInfo("No Panel", GroupType.NOTES);
        NoteGroupPanel theGroupPanel = new DateTimeNoteGroupPanel(theGroupInfo);

        // A Frame for this component -
        JFrame testFrame = new JFrame();
        testFrame.setTitle("Parentless DateTimeNoteComponent Driver");
        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        // This is the whole point -
        DateTimeNoteComponent dtnc = new DateTimeNoteComponent(theGroupPanel, 1);

        // Example of how to get a constant height for this component -
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.Y_AXIS));
        boxPanel.add(dtnc);
        boxPanel.add(new JLabel("Before Glue"));
        boxPanel.add(Box.createVerticalGlue());
        boxPanel.add(new JLabel("After Glue"));

        // Load up the frame, and go -
        testFrame.setContentPane(boxPanel);
        testFrame.pack();
        testFrame.setSize(new Dimension(680, 480));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

} // end class DateTimeNoteComponent

