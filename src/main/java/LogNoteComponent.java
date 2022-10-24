/*  Representation of a single Day Note.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Serial;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LogNoteComponent extends NoteComponent {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final int LOGNOTEHEIGHT = 24;
    private final DateTimeFormatter dtf;
    private static final YearView yvDateChooser;

    static Notifier optionPane;

    // The Members
    private LogNoteData myLogNoteData;
    private final NoteDateLabel noteDateLabel;

    static {
        // optionPane (as a Notifier) is normally just a wrapper for a JOptionPane, but tests may
        //    replace this field with their own instance of a Notifier that requires no user interaction.
        optionPane = new Notifier() { };

        yvDateChooser = new YearView();
    } // end static section


    LogNoteComponent(NoteGroupPanel ng, int i) {
        super(ng, i); // ng comes into super() as a NoteComponentManager and is accessible via 'myManager'.
        index = i;
        dtf = DateTimeFormatter.ofPattern("d MMM yyyy");

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
        return myLogNoteData;
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

        myLogNoteData.setLogDate(LocalDate.now());

        resetDateLabel();
    } // end initialize


    @Override
    protected void makeDataObject() {
        myLogNoteData = new LogNoteData();
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
        LocalDate logDate = myLogNoteData.getLogDate();

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
        if (newNoteData instanceof LogNoteData) {
            // It is already the right type, but cast is still needed because of this method's polymorphic signature.
            setLogNoteData((LogNoteData) newNoteData);
        } else { // Not 'my' type, but we can make it so (coming from a 'paste' operation).
            // But this one will get a default (today) date, since the base class does not have one.
            setLogNoteData(new LogNoteData(newNoteData));
        }

    } // end setNoteData


    // Called by the overridden setNoteData (above) and the 'swap' method.
    private void setLogNoteData(LogNoteData newNoteData) {
        myLogNoteData = newNoteData;

        // update visual components...
        initialized = true;  // without updating the 'lastModDate'
        resetComponent();
        setNoteChanged();
    } // end setLogNoteData


    @Override
    protected void shiftDown() {
        if (noteDateLabel.isActive) {
            // subtract one day
            LocalDate ld = myLogNoteData.getLogDate();
            ld = ld.minusDays(1);
            myLogNoteData.setLogDate(ld);
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
            LocalDate ld = myLogNoteData.getLogDate();
            ld = ld.plusDays(1);
            myLogNoteData.setLogDate(ld);
            resetDateLabel();
            LogNoteComponent.this.setNoteChanged();
        } else {
            myManager.shiftUp(index);
        } // end if
    } // end shiftUp


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


    @Override
    public void swap(NoteComponent lnc) {
        // Get a reference to the two data objects
        LogNoteData lnd1 = (LogNoteData) this.getNoteData();
        LogNoteData lnd2 = (LogNoteData) lnc.getNoteData();

        // Note: getNoteData and setNoteData are working with references
        //   to data objects.  If you 'get' data into a local variable
        //   and then later clear the component, you have also just
        //   cleared the data in your local variable because you never had
        //   a separate copy of the data object, just the reference to it.

        // So - copy the data objects.
        if (lnd1 != null) lnd1 = new LogNoteData(lnd1);
        if (lnd2 != null) lnd2 = new LogNoteData(lnd2);

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

    class NoteDateLabel extends JLabel {
        @Serial
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
                    if(e.getButton() == MouseEvent.BUTTON3) { // Click of right mouse button.
                        if (e.getClickCount() >= 2) return;  // We don't handle a double click on this component.
                        // Show the YearView data chooser
                        yvDateChooser.setView(LocalDate.now()); // In case the current date is a null.
                        yvDateChooser.setChoice(myLogNoteData.getLogDate());
                        showDateChooser();
                        LocalDate newDate = yvDateChooser.getChoice();
                        // Reselecting the same date has same effect as 'X'ing the dialog - no date change.
                        // To clear a date:  right-click the current selection, then close the dialog.
                        System.out.println("The date retrieved from the chooser: " + newDate);
                        myLogNoteData.setLogDate(newDate);
                        myNoteGroupPanel.myNoteGroup.setGroupChanged(true);
                        resetDateLabel();
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
                                myLogNoteData.setLogDate(LocalDate.now());
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
            if(myLogNoteData != null) {
                myLogNoteData.setLogDate(null);
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

    } // end class NoteDateLabel


} // end class LogNoteComponent



