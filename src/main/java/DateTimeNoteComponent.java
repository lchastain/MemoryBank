/*  Representation of a single Day Note.
 */

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DateTimeNoteComponent extends NoteComponent {
    @Serial
    private static final long serialVersionUID = 1L;

    static final int COMPONENTHEIGHT = 104;

    private final DateTimeFormatter dtf;
    static Notifier optionPane;

    // The Members
    private DayNoteData myDayNoteData;
    private final NoteDateLabel noteDateLabel;
    private final NoteTimeLabel noteTimeLabel;
    NoteTextArea noteTextArea;

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
        remove(noteTextField);

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

        noteTextArea = new NoteTextArea();
        noteTextArea.setLineWrap(true);
        if (!editable) {
            noteTextArea.setEditable(false);
        }

        add(westPanel, BorderLayout.WEST);
        JScrollPane jsp = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setViewportView(noteTextArea);
        add(jsp, BorderLayout.CENTER);

        MemoryBank.trace();
    } // end constructor


    // Clears both the Graphical elements and the underlying data.
    protected void clear() {
        noteDateLabel.clear();
        noteTimeLabel.clear();  // Clear the Time Label
        noteTextArea.clear();
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
    public Dimension getMaximumSize() {
        return new Dimension(super.getMaximumSize().width, COMPONENTHEIGHT);
    } // end getMaximumSize

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    } // end getMinimumSize

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, COMPONENTHEIGHT);
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
        resetDateLabel();
        noteTimeLabel.resetTimeLabel();
    } // end initialize


    @Override
    // There IS no DateTimeNoteData class, but the DayNoteData is sooo close to what is needed,
    //   so we just go ahead and use it as if it 'fits'.  And to MAKE it fit,
    //   we are hijacking the iconFileString (not needed) and replacing it with a LocalDate string
    //   (that IS needed).  This saves us from having to have a separate data class and NoteGroup.
    protected void makeDataObject() {
        myDayNoteData = new DayNoteData();
        myDayNoteData.setIconFileString(LocalDate.now().toString());
    } // end makeDataObject


    @Override
    protected void noteActivated(boolean noteIsActive) {
        if (!initialized) return; // No need for further action, on notes where nothing was ever done.

        if (!noteIsActive) {
            noteDateLabel.setInactive();
            noteTimeLabel.setInactive();

            // We don't call the super() in this case, because we have no NoteTextField in this component.
            // Instead, we do for NoteTextArea, what the base class does for the NoteTextField.
            if (noteTextArea.getText().trim().equals("")) {
                NoteData nd = getNoteData();

                // Here we enforce the rule that notes must
                //  have text before they can have additional features.
                // If not, then even if they have a date and/or time, it will be cleared.
                if (nd.extendedNoteString.trim().equals("")) clear();
            } // end if
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
        resetDateLabel();
        noteTimeLabel.resetTimeLabel();

        String s;
        if (getNoteData() == null) s = "";
        else s = getNoteData().getNoteString();

        // Set the text of the component without affecting the lastModDate
        noteTextArea.getDocument().removeDocumentListener(noteTextArea);
        noteTextArea.setText(s);
        noteTextArea.getDocument().addDocumentListener(noteTextArea);

        noteTextArea.setTextColor();
        noteTextArea.resetToolTip(getNoteData());
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


    @Override
    void setEditable(boolean b) {
        editable = b; // With this one line, we don't need to call the super().
        noteDateLabel.setEditable(b);
        noteTimeLabel.setEditable(b);
        noteTextArea.setEditable(b);
    }

    @Override
    public void setActive() {
        noteTextArea.requestFocusInWindow();
    } // end NoteComponent setActive

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
        resetComponent();
        setNoteChanged();
    } // end setDayNoteData

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

    protected void shiftDown() {
        if (noteTimeLabel.isActive) { // This section dup'd from DayNoteComponent
            // subtract one minute
            LocalTime lt = LocalTime.parse(myDayNoteData.getTimeOfDayString());
            lt = lt.minusMinutes(1);
            myDayNoteData.setTimeOfDayString(lt.toString());
            noteTimeLabel.resetTimeLabel();
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

    protected void shiftUp() {
        if (noteTimeLabel.isActive) { // This section dup'd from DayNoteComponent
            // add one minute
            LocalTime lt = LocalTime.parse(myDayNoteData.getTimeOfDayString());
            lt = lt.plusMinutes(1);
            myDayNoteData.setTimeOfDayString(lt.toString());
            noteTimeLabel.resetTimeLabel();
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
// End of NoteComponent specific methods
//---------------------------------------------------------

    class NoteDateLabel extends JLabel implements ActionListener {
        @Serial
        private static final long serialVersionUID = 1L;
        private static final YearView yvDateChooser;

        boolean isActive;
        int dateWidth = 100;
        MouseAdapter ma;

        static {
            yvDateChooser = new YearView();
        }

        NoteDateLabel() {
            clear();
            setHorizontalAlignment(JLabel.CENTER);
            setFont(Font.decode("Dialog-bold-16"));

            ma = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    MemoryBank.event();

                    // For all clicks - including Single left
                    noteTimeLabel.setInactive();
                    setActive();
                    if (!initialized) return;

                    int m = e.getModifiersEx();
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
            return new Dimension(dateWidth, COMPONENTHEIGHT);
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
        int timeWidth = 68;

        NoteTimeLabel() {
            clear(); // initializes as well as 'clears'.
            setHorizontalAlignment(JLabel.CENTER);
            //setFont(Font.decode("DialogInput-bold-20"));
            setFont(Font.decode("Dialog-bold-16"));
        } // end constructor

        private void clear() {
            setText("     ");  // enough room for 'HH:MM'
            //setText("time ");  // enough room for 'HH:MM'
            setBorder(highBorder);
            isActive = false;
            if (myDayNoteData != null) myDayNoteData.setTimeOfDayString("");
        } // end clear

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
            return new Dimension(timeWidth, COMPONENTHEIGHT);
        } // end getPreferredSize


        //---------------------------------------------------------
        // MouseListener methods for the NoteTimeLabel
        //---------------------------------------------------------
        // Method Name: mouseClicked
        //
        //---------------------------------------------------------
        public void mouseClicked(MouseEvent e) {
            MemoryBank.event();

            // For all clicks - including Single left
            noteDateLabel.setInactive();
            setActive();
            if (!initialized) return;

            int m = e.getModifiersEx();
            if (e.getButton() == MouseEvent.BUTTON3) { // Click of right mouse button.
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
                        //   note has had its time cleared.  But now the user has clicked
                        //   on the 'blank' time, and so we fill in the current time.
                        myDayNoteData.setTimeOfDayString(LocalTime.now().toString());
                        noteTimeLabel.resetTimeLabel();
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

    // This class implements a text field with a red border that
    //   appears when the focus is gained.
    protected class NoteTextArea extends JTextArea implements
            DocumentListener, FocusListener, MouseListener, KeyListener {
        @Serial

        private static final long serialVersionUID = 1L;

        public NoteTextArea() {
            // By calling the super contructor with no rows or columns, we get the smallest possible TextArea.
            // But the component to which this inner class belongs is putting us into a stretchable Panel that
            //   (given the chosen font) will initially make enough room for text that is five rows in height.
            //   The number of columns will vary depending on the size of the container that holds the
            //   DateTimeNoteComponent.
            super();

            // This is needed so that the KeyListener will hear a TAB.
            setFocusTraversalKeysEnabled(false);

            setBorder(offBorder);
            addMouseListener(this);
            setFont(Font.decode("Dialog-bold-14"));
            addFocusListener(this);
            getDocument().addDocumentListener(this); // cut/paste/changed
            addKeyListener(this);
        } // end constructor

        private void clear() {
            // Remove the document listener, to avoid thread deadlocks.
            getDocument().removeDocumentListener(this);

            // Clear the text field
            setText(null);
            setForeground(Color.black);
            setToolTipText(null);

            // Restore the document listener.
            getDocument().addDocumentListener(this);
        }

        // This provides a gap between the bounds of the NoteComponent and the location of its tooltip,
        //   if it has one.  It is just low enough (by a few pixels) that we go thru 'mouseExited' if we try to
        //   move the pointer into the tooltip text area, and that causes the tooltip to go away.
        // Important:  Probably not hardened in the face of different L&Fs; optimized for Windows Classic.
        // Also: a 'fast' mouse move from within the bounds of the NoteComponent into the popped-up tooltip
        //    can evade the mouseExited event as the cursor crosses the 6-pixel gap, in which case the
        //    tooltip stays up much longer.  But we must live with that, for now.
        @Override
        public Point getToolTipLocation(MouseEvent e) {
            int offsetY = 30; // Good enough for most components.
            Object object = e.getSource();
            try {
                JComponent component = (JComponent) object;
                //Rectangle rectangle = component.getBounds();
                offsetY = component.getBounds().height + 6;
            } catch (Exception ignore) {
            }

            return new Point(10, offsetY);
        }


        // Turn off the currently displayed tooltip, if there is one.
        void hideToolTip() {
            // We preserve a single (static) last MOUSE_ENTERED event across ALL NoteTextAreas, but there is a
            // sequence of operations after program startup, whereby execution could come here while this reference
            // is still null.  So the following  block of code is conditional on it not being null.
            if (lastMouseEnteredEvent != null) {
                // Get a reference to the last entered note
                NoteTextArea theSource = (NoteTextArea) lastMouseEnteredEvent.getSource();
                // and use the reference along with the MOUSE_ENTERED event, to gen up a 'MOUSE_EXITED' event.
                MouseEvent mouseExitedEvent = new MouseEvent(theSource, MouseEvent.MOUSE_EXITED,
                        lastMouseEnteredEvent.getWhen(), lastMouseEnteredEvent.getModifiersEx(),
                        -1, -1, lastMouseEnteredEvent.getClickCount(), false);

                // Now get ALL the mouse listeners on that earlier note.
                // This is because tooltips are displayed by the JVM library code and not our own code, so if there
                //   is a tooltip showing then it was put there by a listener in that code, so that is the listener
                //   to which we need to send the event to get the tooltip to go away.
                MouseListener[] theListeners = theSource.getMouseListeners();

                // Cycle thru the listeners and call .mouseExited() on all of them.
                // (including our own, but that one will not remove the tooltip).
                for (MouseListener ml : theListeners) {
                    ml.mouseExited(mouseExitedEvent);
                }
            }
        }


        private void resetToolTip(NoteData nd) {
            if (nd == null) return;

            String subjectString = nd.getSubjectString();
            String extendedNoteString = nd.getExtendedNoteString();
            if (!extendedNoteString.isBlank()) {
                StyledDocumentData sdd = StyledDocumentData.getStyledDocumentData(extendedNoteString);
                if (sdd != null) {
                    JTextPane jtp = new JTextPane();
                    DefaultStyledDocument dsd = (DefaultStyledDocument) jtp.getStyledDocument();
                    sdd.fillStyledDocument(dsd);
                    extendedNoteString = jtp.getText();
                }
            }

            String strToolTip;

            if (subjectString != null) {
                // System.out.println("Setting the tool tip to: " + ss);
                if (subjectString.trim().equals("")) subjectString = null;
            } // end if

            // The tool tip will be a concatenation of the subject
            //   and the extended note.  If one is not present then
            //   it will just be the other.  If neither, then null.
            if ((subjectString != null) && (!extendedNoteString.isBlank())) {
                strToolTip = subjectString + System.lineSeparator() + extendedNoteString;
            } else if (subjectString != null) {
                strToolTip = subjectString;
            } else if (!extendedNoteString.isBlank()) {
                strToolTip = extendedNoteString;
            } else {
                strToolTip = null;
            } // end if / else - setting strToolTip

            if (strToolTip != null) {
                // Insert line breaks as needed and enforce
                //   an overall text length limit.
                strToolTip = AppUtil.getTooltipString(strToolTip);

                // In case we had a (too large) gap of linefeeds in the middle
                //   and the cutoff didn't make it back to real text -
                strToolTip = strToolTip.trim();

                // Convert potentially malicious characters.
                strToolTip = strToolTip.replace("&", "&amp;");
                strToolTip = strToolTip.replace("<", "&lt;");

                // Wrap in HTML and PREserve the original formatting, to hold on to indents and multi-line.
                strToolTip = "<html><pre>" + strToolTip + "</pre></html>";

            } // end if

            setToolTipText(strToolTip);
        } // end resetToolTip

        @Override
        public void setText(String s) {
            super.setText(s);
            setCaretPosition(0); // Do not leave notes scrolled horizontally.
        }

        private void setTextColor() {
            NoteData nd = getNoteData();
            if (nd == null) return;
            if (!nd.getExtendedNoteString().isBlank())
                setForeground(Color.blue);
            else
                setForeground(Color.black);
        } // end setTextColor

        //=====================================================================
        // EVENT HANDLERS
        //=====================================================================

        //<editor-fold desc="actionPerformed method">
        //   This method will be called indirectly for a mouse double-click on the JTextArea.
        //---------------------------------------------------------
        public void actionPerformed(ActionEvent ae) {
            MemoryBank.event();
            boolean extendedNoteChanged;
            if (!this.isEditable()) return;
            if (!initialized) return;
            hideToolTip(); // Turn off the currently displayed tooltip, if any.

            // Highlight this note to show it is the one being modified.
            setBorder(redBorder);

            NoteData tmpNoteData = getNoteData();
            extendedNoteChanged = myManager.editNoteData(tmpNoteData);

            if (extendedNoteChanged) {
                // Set (or clear) the tool tip.
                resetToolTip(tmpNoteData);

                setTextColor();
                setNoteChanged();
            } // end if

            // Remove the 'modification in progress' highlight
            setBorder(null);
        } // end actionPerformed
        //</editor-fold>


        //<editor-fold desc="DocumentListener methods for the TextArea">
        public void insertUpdate(DocumentEvent e) {
            // System.out.println("insertUpdate: " + e.toString());
            if (!initialized) initialize();

            getNoteData().setNoteString(getText());
            setNoteChanged();
        } // end insertUpdate

        public void removeUpdate(DocumentEvent e) {
            System.out.println("removeUpdate: " + e.toString());
            getNoteData().setNoteString(getText());
            setNoteChanged();
        } // end removeUpdate

        public void changedUpdate(DocumentEvent e) {
            System.out.println("changedUpdate: " + e.toString());
            getNoteData().setNoteString(getText());
            setNoteChanged();
        } // end changedUpdate
        //</editor-fold>

        //<editor-fold desc="FocusListener methods for the TextArea">
        // Note: The order of focusGained / focusLost along with
        //  the visual indicators (borders, highlighting) and
        //  how they can be invoked by either up/down arrows,
        //  mouse clicks, or the tab key - is critical!
        //  The key to it all is to disallow other components from
        //  getting the focus when they appear (most notably, the
        //  PopupMenu and the vertical scrollbar).  Then, do
        //  everything based on Focus here being gained or lost.
        public void focusGained(FocusEvent e) {
            // System.out.println("focusGained for index " + index);
            setBorder(redBorder);
            DateTimeNoteComponent.this.scrollRectToVisible(getBounds());  // Does this scroll text on the line, or the line in the scrollpane?
            if (mySelectionMonitor != null) mySelectionMonitor.noteSelected();

            // We occasionally get a null pointer exception at startup.
            if (getCaret() == null) return;
// trying to disable the pre-highlighted text seen in todo lists.  Need to consistently reproduce, first.
//            setSelectionStart(getSelectionEnd());
            getCaret().setVisible(true);

            if (!initialized) return;
            noteActivated(true);
        } // end focusGained

        public void focusLost(FocusEvent e) {
            // System.out.println("focusLost for index " + index);
            setBorder(offBorder);
            getCaret().setVisible(false);
            // We do not de-select at this point because any selection would be lost
            // when the user clicks 'ok', for instance.
            // Instead, selections are cleared prior to presenting new choices.

            noteActivated(false);
        } // end focusLost
        //</editor-fold>

        //<editor-fold desc="KeyListener methods for the TextArea">
        @Override
        public void keyPressed(KeyEvent ke) {
            // Turn off a previous popup, if one is showing.
            // This could happen if a menu was showing, then the user
            //   pressed a TAB, UP or DOWN key to change focus - the
            //   popup menu would still be up, but active for the
            //   previous note vs the one that appeared to be active.
            if (contextMenu.isVisible()) contextMenu.setVisible(false);

            hideToolTip(); // Turn off the currently displayed tooltip, if any.

            int kp = ke.getKeyCode();

            boolean shifted = ke.isShiftDown();
            if (!shifted) {
                // In case the user 'activated' the date or time, but then left it depressed and went over to
                //   the text area on the same line and started typing there.
                noteDateLabel.setInactive();
                noteTimeLabel.setInactive();
            }

            // Translate TAB / Shift-TAB into DOWN / UP
            if (kp == KeyEvent.VK_TAB) {
                kp = KeyEvent.VK_DOWN;
                if (shifted) {
                    shifted = false;
                    kp = KeyEvent.VK_UP;
                } // end if
            } // end if

            if ((kp != KeyEvent.VK_DOWN) && (kp != KeyEvent.VK_UP)) return;

            if (shifted) { // This means we are doing a 'swap', if allowed.
                // System.out.println();
                if (kp == KeyEvent.VK_UP) shiftUp();
                else shiftDown();
                ke.consume(); // Don't let it go on to affect the TextArea.
            } else { // Otherwise, a simple UP or DOWN arrow.
                int lineCount = getLineCount();
                int currentLine = 1;
                System.out.println("Line Count: " + lineCount);
                try {  // Get the current line (zero-based)
                    int offset = getCaretPosition();
                    int line = getLineOfOffset(offset);
                    currentLine = line + 1;
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
                System.out.println("Current line of the TextArea: " + currentLine);

                if (kp == KeyEvent.VK_DOWN) {
                    if (currentLine >= lineCount) {
                        transferFocus();
                    }
                } else {  // VK_UP
                    if (currentLine <= 1) {
                        transferFocusBackward();
                    }
                }
            } // end if
        } // end keyPressed

        @Override
        public void keyReleased(KeyEvent e) {
//            System.out.println("Selection start: " + getSelectionStart());
        }

        @Override
        public void keyTyped(KeyEvent ke) {
            if (initialized) return;

            char kc = ke.getKeyChar();
            if (kc == KeyEvent.VK_TAB) return;
            if (kc == KeyEvent.VK_ENTER) return;
            if (kc == KeyEvent.VK_BACK_SPACE) return;
            initialize(); // this will activate the next note
            noteActivated(true); // Added for SCR0091
        } // end keyTyped
        //</editor-fold>

        //<editor-fold desc="MouseListener methods for the TextArea">
        public void mouseClicked(MouseEvent e) {
            MemoryBank.event();
            noteDateLabel.setInactive();
            noteTimeLabel.setInactive();
            if (!this.hasFocus()) {
                // The rmb click does not change focus, so we help it out.
                requestFocusInWindow();
            }

            if (!this.isEditable()) return;
            int m = e.getModifiersEx();

            // Single click, right mouse button.
            if (e.getButton() == MouseEvent.BUTTON3) {
                // System.out.println("Right click on index " + index);

                // In earlier Java versions (before 1.6), The rmb click
                //   did not get focus, so -
                // requestFocusInWindow();

                // NOTE:  The above 'request' is subject to queuing and
                //  handling priorities; it will not be honored until after
                //  this method completes (because it is an event handler)
                //  AND all other focus events are handled, including the
                //  focusLost method of the note that currently has it,
                //  but is about to lose it as a result of this request.

                // Ignore double right mouse clicks.
                if (e.getClickCount() == 2) return;

                // Set the global NoteComponent.  This is the primary
                // mechanism that allows the handler to be static.
                theNoteComponent = DateTimeNoteComponent.this;

                // Child classes will override resetPopup to enable/disable, setNotes/remove
                //   menu items, based on the data content of the active NoteComponent.
                resetPopup();

                // Show the popup menu
                // System.out.println("Showing popup!");
                contextMenu.show(e.getComponent(), e.getX(), e.getY());

                // Double click, left mouse button.
            } else if (e.getClickCount() == 2) {
                actionPerformed(new ActionEvent(e.getSource(), 0, ""));
            } // end if/else if
        } // end mouseClicked

        public void mouseEntered(MouseEvent e) {
            contextMenu.setVisible(false); // This gets rid of a previous one, if any.
            lastMouseEnteredEvent = e;
//            if (!initialized) return;   // Disabled 11/4/2022 so that new notes will prompt for input.

            resetPanelStatusMessage(getTextStatus());
        } // end mouseEntered

        public void mouseExited(MouseEvent e) {
            myManager.setStatusMessage(" ");
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }
        //</editor-fold>

    } // end class NoteTextArea

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



