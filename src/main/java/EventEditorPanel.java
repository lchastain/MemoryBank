import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Vector;

/*
 * Summary description for EventEditorPanel
 * It inherits a subject-management capability.
 */
public class EventEditorPanel extends ExtendedNoteComponent {
    private static final long serialVersionUID = 1L;

    // Used both here and by RecurrencePanel
    public static Color backColor;
    public static Color pastColor;
    public static Color futureColor;


    // 'My' Variables declaration section
    //-------------------------------------------------------------
    private static final int maxLocations = 20;

    // New 'buttons' to replace JButtons
    private LabelButton btnStartDate;
    private LabelButton btnStartTime;
    private LabelButton btnEndDate;
    private LabelButton btnEndTime;

    // Simple data types
    private LocalTime timeSetting;    // Holds the TimeChooser result.
    private int intPreferredWidth;    // See note in constructor.
    private int intPreferredHeight;   // See note in constructor.
    private DateTimeFormatter dtf;
    private String strLocFilename;   // Locations file name
    private Vector<String> locations;
    private boolean locationsChanged;


    // For Events
    private MouseAdapter maDateTimeHandler;
    private ItemListener ilDurationUnits;

    // Ancillary dialogs -
    private YearView yvDateChooser;
    private TimeChooser tcTimeChooser;
    private RecurrencePanel rpRepeatSetting;

    // Note:  This editor can be initialized with an EventNoteData
    // (in 'showTheData') but any changes that the user makes to
    // the interface are not passed back thru to the initial data
    // object.  Instead, the calling context can make a call to
    // the 'collectTheData' method, to get any/all updates.

    //  We keep all user edits either in the user-controlled components
    //  that you can see on screen, or in variables that stay under the
    //  hood.  From this one interface a user may invoke up to six
    //  additional dialogs (four of them unique) from which the results
    //  must be stored here until the 'collect' call is made.  The
    //  variables below are used for this purpose.
    private LocalDate eventStartDate;  // Holds only the Date.
    private LocalTime eventStartTime;  // Holds only the Time.
    private LocalDate eventEndDate;    // Holds only the Date.
    private LocalTime eventEndTime;    // Holds only the Time.

    private LocalDateTime eventStartDateTime;
    private LocalDateTime eventEndDateTime;


    private String strRecurrenceSetting;  // Recurrence
    // strDateFormatSetting

    // The reason for NOT immediately updating the data (as
    //   touched on in the note above) is to support a
    //   possible future 'Cancel' option.
    //-------------------------------------------------------------

    // JFrameBuilder Variables declaration section
    //-------------------------------------------------------------
    private JLabel lblDuration;
    private JLabel lblCategory;
    private JLabel lblLocation;
    private JLabel lblRecurrenceSetting;
    private JLabel lblNotes;
    private JCheckBox chkboxRetainNote;
    private JTextField txtfDurationValue;
    private JComboBox<String> comboxLocation;
    private JComboBox<String> jComboBox2;  // placeholder; to be replaced
    private JComboBox<String> comboxDurationUnits;
    private JScrollPane spaneNotes;
    private JButton btnRecurrence;
    private JButton btnDateFormat;
    private JPanel contentPane;
    private JLabel lblStartDate;
    private JLabel lblStartTime;
    private JButton jButton5;               // replaced
    private JButton jButton8;               // replaced
    private JPanel pnlStart;
    private JLabel lblEndDate;
    private JLabel lblEndTime;
    private JButton jButton3;               // replaced
    private JButton jButton4;               // replaced
    private JPanel pnlEnd;
    //-----
    // End of variables declaration


    public EventEditorPanel(String s) {
        super(s);
        setLayout(null);
        initializeComponent();

        backColor = getBackground();
        pastColor = Color.pink;  // or new Color(250, 178, 178);
        futureColor = new Color(178, 250, 178);

        // Although size was set explicitly in initializeComponent, each call to getSize()
        //   reported a smaller amount by (6,25) after each time the dialog was closed.
        //   This method of capturing the initial size is the workaround.  Now the
        //   EventNoteGroup calls getMinimumSize (defined below) to get the size value.
        intPreferredWidth = getSize().width;
        intPreferredHeight = getSize().height;

        // These interfaces only need to be created once.
        yvDateChooser = new YearView();
        rpRepeatSetting = new RecurrencePanel();

        // Prepare our date formatter.
        dtf = DateTimeFormatter.ofPattern("EEE  d MMM yyyy");

        maDateTimeHandler = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                handleDateTimeClicked(e);
            }
        };

        btnRecurrence.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                btnRecurrence_actionPerformed();
            }
        });

        reinitializeComponent();
    } // end EventEditorPanel constructor


    /**
     * Add Component Without a Layout Manager (Absolute Positioning)
     */
    private void addComponent(Container container, Component c, int x, int y, int width, int height) {
        c.setBounds(x, y, width, height);
        container.add(c);
    }


    private void addLocation(String s) {
        MemoryBank.debug("Adding location: [" + s + "]");
        if (s.equals("")) return;

        //------------------------------------------------------------------
        // Check to see if this location is already first in the list -
        //------------------------------------------------------------------
        if (locations.size() > 0) {
            if ((locations.elementAt(0)).equals(s)) return;
        } // end if

        //------------------------------------------------------------------
        // Then, remove an occurrence of this subject lower in the list.
        //------------------------------------------------------------------
        locations.remove(s);

        //------------------------------------------------------------------
        // Then, put this subject at the top of the list.
        //------------------------------------------------------------------
        locations.insertElementAt(s, 0);

        //------------------------------------------------------------------
        // Then, if the list has grown too big, truncate.
        //------------------------------------------------------------------
        if (locations.size() > maxLocations) {
            locations.remove(locations.lastElement());
        } // end if too many

        locationsChanged = true;
    } // end addLocation


    private void btnDateFormat_actionPerformed(ActionEvent ae) {
        String s = ae.getActionCommand();
        System.out.print(this.getClass().getName());
        System.out.println(" - unhandled action: " + s);
        // Set the textual format of the Date (and optionally, time)
        //   as it will be shown in the Event summary.
    }


    private void btnRecurrence_actionPerformed() {
        showRecurrenceDialog();
    }


    // Put info from the interface into the EventNoteData object
    void collectTheData(EventNoteData end) {

        end.setExtendedNoteString(getExtText());

        // For size of the Extended text; basically non-variable
        //   but it is larger here than the default for a Note,
        //   and if this event gets aged off and retained as a
        //   note then this size may be better than the default.
        //end.setExtendedNoteHeightInt(body.getHeight() + 28);
        end.setExtendedNoteWidthInt(body.getWidth() + 10);
        end.setExtendedNoteHeightInt(spaneNotes.getHeight() + 40);

        end.setSubjectString(getSubject());
        // We need to be able to save a blank subject, and recall it,
        //   which is different than if you never set one in the
        //   first place, in which case it would be null and you
        //   should get the default subject.  So -
        //   we allow the assignment without checking its content.

        end.setRetainNote(chkboxRetainNote.isSelected());

        // A composite Start is not allowed to be later than
        //   the composite End.  But what if, during the course
        //   of this interface being used, the End date were first
        //   extended and then the Start date was also increased,
        //   to a value that exceeds the original End date?
        //   Within the interface, this was allowed but now we need
        //   to put the results back into the 'official' data and
        //   if we start with the new Start Date, it would not be
        //   legal when compared to the original Event End.  The
        //   same kind of situation could occur if we start with
        //   updating the End Date, if the interface had been used
        //   to decrease both dates.  So - the only way to be sure
        //   that the updates will be accepted is to first clear
        //   the existing data.
        end.setStartDate(null);
        end.setStartTime(null);
        end.setEndDate(null);
        end.setEndTime(null);

        // Now we can use the values from this interface and do
        //   not need to check the results.
        end.setStartDate(eventStartDate); // even if null
        end.setStartTime(eventStartTime); // even if null
        end.setEndDate(eventEndDate);     // even if null
        end.setEndTime(eventEndTime);     // even if null

        //-------------------------------------------------------------
        // Determine whether or not to try to preserve a duration.
        //-------------------------------------------------------------
        // The only time that data NEEDS to be re-set in order to
        // match the current interface duration value is when that
        // duration value exceeds the calculation that is made from
        // the 'known' values, which indicates that the 'unknown'
        // fields contain placeholder values that were set as a
        // result of an explicit user duration setting.

        // However, if we simply set the duration in every case then
        // in some cases it would cause an unknown field to be calculated
        // and marked as known.  This MUST NOT HAPPEN because this
        // duration setting is due to a programmatic (vs user) action.

        // Any duration that needs this preservation should still be
        //   showing on the interface (because any subsequent Date/Time
        //   setting would have reset it to the initial calculation value).
        Long tmpLong = getDurationSetting(); // in Minutes
        if (tmpLong != null) {
            // Now, how can we know that the user has made this specific
            //   entry and that it wasn't just a previous calculation result?
            //
            // An examination of the various cases of what remains unknown
            //   after a duration was explicitly entered (vs a calculation),
            //   shows that a 'placeholder' duration value can only be
            //   present in three unique situations:
            //   1.  Both Dates are known and both Times are not.
            //   2.  Both Times are known and both Dates are not.
            //   3.  No Times or Dates are known.
            //
            // This examination further shows that if a duration were
            //   (re-)entered in THESE cases, no 'unknown' fields
            //   would be marked as known (that thing that MUST NOT
            //   HAPPEN, would not happen).  So, we will restrict this
            //   operation to only
            //   those three cases, and it will not matter if the
            //   duration that we set was calculated or user-entered.

            boolean blnSD = end.getStartDate() != null;
            boolean blnST = end.getStartTime() != null;
            boolean blnED = end.getEndDate() != null;
            boolean blnET = end.getEndTime() != null;
            boolean doit = false;

            // Case 1
            if ((blnSD && blnED) && (!blnST && !blnET)) {
                doit = true;
            }
            // Case 2
            if ((!blnSD && !blnED) && (blnST && blnET)) {
                doit = true;
            }
            // Case 3
            if (!(blnSD || blnST || blnED || blnET)) {
                doit = true;
            }
            if (doit) end.setDuration(tmpLong);
        }
        //-------------------------------------------------------------

        // Location
        // Get the text from the Location combo box, whether it
        //   was placed there via a selection or typing.
        Object objTmpLoc = comboxLocation.getEditor().getItem();
        if (objTmpLoc != null) {
            String strTmpLoc = objTmpLoc.toString().trim();
            if (!strTmpLoc.equals("")) {
                if (!strTmpLoc.equals(end.getLocation().trim())) {
                    // Only if this represents a change.
                    end.setLocation(strTmpLoc);
                    addLocation(strTmpLoc);
                }
            }
        }
        saveLocations();

        // Do this after setting the start date, since
        //   that action clears recurrence.
        end.setRecurrence(strRecurrenceSetting);
    } // end collectTheData


    // This method is only here to 'fool' JFramebuilder-generated code.
    public JPanel getContentPane() {
        return this;
    }


    //--------------------------------------------------------------
    // Method Name: getDurationSetting
    //
    // Gets the settings from the interface and returns the long
    // value corresponding to the number of duration minutes that
    // are specified.  If the setting is incomplete it will return
    // null.
    //--------------------------------------------------------------
    private Long getDurationSetting() {
        String strValue = txtfDurationValue.getText();
        String strUnits = Objects.requireNonNull(comboxDurationUnits.getSelectedItem()).toString();
        long lngDuration;

        if (strValue.trim().equals("")) return null; // No Value
        if (strUnits.equals("Unknown")) return null; // No Units

//  System.out.println("Value: " + txtfDurationValue.getText());
//  System.out.println("Units: " + comboxDurationUnits.getSelectedItem().toString());

        // Convert string numeric to a long -
        try {
            // I don't expect trouble from this conversion because of the
            //   constraints on the entry field.  Hence, the ugly handling
            //   of the exception that I believe we'll never see.
            lngDuration = Long.parseLong(strValue);
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            lngDuration = 0;
        } // end try/catch

        // Whatever duration units were specified, convert to minutes.
        if (strUnits.equals("Hours")) lngDuration *= 60;
        if (strUnits.equals("Days")) lngDuration *= (60 * 24);
        if (strUnits.equals("Weeks")) lngDuration *= (60 * 24 * 7);

        return lngDuration;
    } // end getDurationSetting


    public Dimension getMinimumSize() {
        return new Dimension(intPreferredWidth, intPreferredHeight);
    } // end getMinimumSize


    //-----------------------------------------------------------
    // Method Name: handleDateTimeClicked
    //
    // This is the handler for the Date and Time buttons on the
    //   interface.  It is invoked directly from the associated
    //   MouseListener, which is not associated with any other
    //   Components (ie, it will not be called for an unknown
    //   reason, and so there is no need to check for one ...)
    //-----------------------------------------------------------
    private void handleDateTimeClicked(MouseEvent e) {
        int m = e.getModifiers();
        Component source = (Component) e.getSource();
        String s = source.getName();
        boolean rightClick = false;
        String strTmp;

        if ((m & InputEvent.BUTTON3_MASK) != 0) rightClick = true;

        // Make a temporary data object so that we can collect
        //   existing interface settings into it and then use
        //   it to determine the validity of the current action.
        // Note that if the interface had stored a duration-only
        //   setting, it will now be ignored (and properly so),
        //   unless the action was to clear an already cleared field.
        EventNoteData tmpNoteData = new EventNoteData();
        tmpNoteData.setStartDate(eventStartDate);
        tmpNoteData.setStartTime(eventStartTime);
        tmpNoteData.setEndDate(eventEndDate);
        tmpNoteData.setEndTime(eventEndTime);

        switch (s) {
            case "Start Date":
                if (rightClick) {
                    if (eventStartDate == null) return; // already cleared.
                    tmpNoteData.setStartDate(null);
                    eventStartDate = null;
                    strRecurrenceSetting = "";
                } else {
                    if (eventStartDate != null) yvDateChooser.setChoice(eventStartDate);
                    else yvDateChooser.setChoice(null);
                    showDateDialog("Select a Start Date for the Event");
                    if (tmpNoteData.setStartDate(yvDateChooser.getChoice())) {
                        if ((eventStartDate == null) || (tmpNoteData.getStartDate() != eventStartDate)) {
                            // The above comparison must be between time values, not objects.
                            eventStartDate = tmpNoteData.getStartDate();
                            strRecurrenceSetting = "";
                        } // end if not setting to the same date.
                    } else {
                        strTmp = "<html>The Event cannot start after it ends.<br>";
                        strTmp += "Your selection of ";
                        strTmp += dtf.format(yvDateChooser.getChoice());
                        strTmp += " has been discarded.</html>";

                        JLabel lblTmp = new JLabel(strTmp);
                        lblTmp.setFont(Font.decode("Dialog-bold-12"));
                        JOptionPane.showMessageDialog(this, lblTmp,
                                "Start Date Selection Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                resetStartPanel();
                break;
            case "End Date":
                if (rightClick) {
                    if (eventEndDate == null) return;
                    tmpNoteData.setEndDate(null);
                    eventEndDate = null;
                } else {
                    if (eventEndDate != null) yvDateChooser.setChoice(eventEndDate);
                    else yvDateChooser.setChoice(null);
                    showDateDialog("Select an End Date for the Event");
                    if (tmpNoteData.setEndDate(yvDateChooser.getChoice())) {
                        eventEndDate = tmpNoteData.getEndDate();
                    } else {
                        strTmp = "<html>The Event cannot end before it starts.<br>";
                        strTmp += "Your selection of ";
                        strTmp += dtf.format(yvDateChooser.getChoice());
                        strTmp += " has been discarded.</html>";

                        JLabel lblTmp = new JLabel(strTmp);
                        lblTmp.setFont(Font.decode("Dialog-bold-12"));
                        JOptionPane.showMessageDialog(this, lblTmp,
                                "End Date Selection Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                resetEndPanel();
                break;
            case "Start Time":
                if (rightClick) {
                    if (eventStartTime == null) return;
                    tmpNoteData.setStartTime(null);
                    eventStartTime = null;
                } else {
                    // if (dateEventStartTime != null) tcTimeChooser = new TimeChooser(dateEventStartTime);  // temp, 9/29 while working DayNotes
                    if (eventStartTime != null) tcTimeChooser = new TimeChooser(eventStartTime);
                    else tcTimeChooser = new TimeChooser();
                    showTimeDialog("Select a Start Time");

                    if (tmpNoteData.setStartTime(timeSetting)) {
                        eventStartTime = tmpNoteData.getStartTime();
                    } else {
                        strTmp = "<html>The Event cannot start after it ends.<br>";
                        strTmp += "Your selection of ";
                        strTmp += tcTimeChooser.getChoice();
                        strTmp += " has been discarded.</html>";

                        JLabel lblTmp = new JLabel(strTmp);
                        lblTmp.setFont(Font.decode("Dialog-bold-12"));
                        JOptionPane.showMessageDialog(this, lblTmp,
                                "Start Time Selection Error", JOptionPane.ERROR_MESSAGE);
                    }
                } // end if
                resetStartPanel();
                break;
            case "End Time":
                if (rightClick) {
                    if (eventEndTime == null) return;
                    tmpNoteData.setEndTime(null);
                    eventEndTime = null;
                } else {
                    // if (dateEventEndTime != null) tcTimeChooser = new TimeChooser(dateEventEndTime);  // temp, 9/29 while working DayNotes
                    if (eventEndTime != null) tcTimeChooser = new TimeChooser(LocalTime.now());
                    else tcTimeChooser = new TimeChooser();
                    showTimeDialog("Select an End Time");

                    if (tmpNoteData.setEndTime(timeSetting)) {
                        eventEndTime = tmpNoteData.getEndTime();
                    } else {
                        strTmp = "<html>The Event cannot end before it starts.<br>";
                        strTmp += "Your selection of ";
                        strTmp += tcTimeChooser.getChoice();
                        strTmp += " has been discarded.</html>";

                        JLabel lblTmp = new JLabel(strTmp);
                        lblTmp.setFont(Font.decode("Dialog-bold-12"));
                        JOptionPane.showMessageDialog(this, lblTmp,
                                "End Time Selection Error", JOptionPane.ERROR_MESSAGE);
                    }
                } // end if
                resetEndPanel();
                break;
            default:  // ... but we left this section in anyway.
                System.out.print(this.getClass().getName());
                System.out.println(" - unhandled action: " + s);
                break;
        }

//    resetDuration(tmpNoteData);
        String strUnits = tmpNoteData.getDurationUnits();
        if (strUnits.equals("unknown")) {
            txtfDurationValue.setText("");
        } else {
            txtfDurationValue.setText(String.valueOf(tmpNoteData.getDurationValue()));
        }
        setDurationUnits(strUnits);
    } // end handleDateTimeClicked


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always regenerated
     * by the Windows Form Designer. Otherwise, retrieving design might not work properly.
     * Tip: If you must revise this method, please backup this GUI file for JFrameBuilder
     * to retrieve your design properly in future, before revising this method.
     */
    private void initializeComponent() {
        lblDuration = new JLabel();
        lblCategory = new JLabel();
        lblLocation = new JLabel();
        lblRecurrenceSetting = new JLabel();
        lblNotes = new JLabel();
        chkboxRetainNote = new JCheckBox();
        txtfDurationValue = new JTextField();
        comboxLocation = new JComboBox<>();
        jComboBox2 = new JComboBox<>(); // Needed to hold this place because Subjects not yet loaded.
        comboxDurationUnits = new JComboBox<>();
        JTextArea jTextArea1 = new JTextArea();
        spaneNotes = new JScrollPane();
        btnRecurrence = new JButton();
        btnDateFormat = new JButton();
        contentPane = this.getContentPane();
        //----- 
        lblStartDate = new JLabel();
        lblStartTime = new JLabel();
        jButton5 = new JButton();
        jButton8 = new JButton();
        pnlStart = new JPanel();
        //----- 
        lblEndDate = new JLabel();
        lblEndTime = new JLabel();
        jButton3 = new JButton();
        jButton4 = new JButton();
        pnlEnd = new JPanel();
        //----- 

        // 
        // lblDuration 
        // 
        lblDuration.setText("Duration");
        // 
        // lblCategory 
        // 
        lblCategory.setText("Category");
        // 
        // lblLocation 
        // 
        lblLocation.setText("Location");
        // 
        // lblRecurrenceSetting 
        // 
        lblRecurrenceSetting.setHorizontalTextPosition(SwingConstants.LEFT);
        lblRecurrenceSetting.setText("jLabel9");
        // 
        // lblNotes 
        // 
        lblNotes.setText("Notes");
        // 
        // chkboxRetainNote 
        // 
        chkboxRetainNote.setText("Retain Day Note");
        chkboxRetainNote.setSelected(true);
        // 
        // txtfDurationValue 
        // 
        // 
        // comboxLocation 
        // 
        comboxLocation.addItem("My place");
        comboxLocation.addItem("Your place");
        comboxLocation.addItem("Home");
        comboxLocation.addItem("Work");
        comboxLocation.addItem("Office");
        comboxLocation.addItem("Sweden");
        comboxLocation.setEditable(true);
        //
        // comboxDurationUnits 
        // 
        comboxDurationUnits.addItem("Unknown");
        comboxDurationUnits.addItem("Minutes");
        comboxDurationUnits.addItem("Hours");
        comboxDurationUnits.addItem("Days");
        comboxDurationUnits.addItem("Weeks");
        // 
        // jTextArea1 
        // 
        jTextArea1.setText("jTextArea1");
        // 
        // spaneNotes 
        // 
        spaneNotes.setViewportView(jTextArea1);
        // 
        // btnRecurrence 
        // 
        btnRecurrence.setText("Recurrence");
        btnRecurrence.setToolTipText("Click here to set a repeating schedule for this Event.");
        // 
        // btnDateFormat 
        // 
        btnDateFormat.setText("Date Format");
        btnDateFormat.setToolTipText("Click here to specify a different format for the displayed Dates and/or Times");
        btnDateFormat.addActionListener(this::btnDateFormat_actionPerformed);
        // 
        // contentPane 
        // 
        contentPane.setLayout(null);
        addComponent(contentPane, lblDuration, 218, 45, 60, 18);
        addComponent(contentPane, lblCategory, 5, 8, 58, 18);
        addComponent(contentPane, lblLocation, 5, 149, 60, 18);
        addComponent(contentPane, lblRecurrenceSetting, 110, 175, 282, 41);
        addComponent(contentPane, lblNotes, 6, 223, 60, 18);
        addComponent(contentPane, chkboxRetainNote, 401, 4, 112, 24);
        addComponent(contentPane, txtfDurationValue, 213, 68, 50, 22);
        addComponent(contentPane, comboxLocation, 76, 145, 429, 22);
        addComponent(contentPane, jComboBox2, 62, 7, 331, 22);
        addComponent(contentPane, comboxDurationUnits, 210, 95, 74, 22);
        addComponent(contentPane, spaneNotes, 5, 245, 501, 210);
        addComponent(contentPane, btnRecurrence, 5, 180, 93, 28);
        addComponent(contentPane, btnDateFormat, 404, 177, 100, 28);
        addComponent(contentPane, pnlStart, 5, 40, 188, 100);
        addComponent(contentPane, pnlEnd, 318, 40, 188, 100);
        // 
        // lblStartDate 
        // 
        lblStartDate.setText("Date");
        // 
        // lblStartTime 
        // 
        lblStartTime.setText("Time");
        // 
        // jButton5 
        // 
        jButton5.setText("jButton5");
        // 
        // jButton8 
        // 
        jButton8.setText("jButton8");
        // 
        // pnlStart 
        // 
        pnlStart.setLayout(null);
        pnlStart.setBorder(new TitledBorder("Title"));
        addComponent(pnlStart, lblStartDate, 10, 30, 60, 18);
        addComponent(pnlStart, lblStartTime, 8, 66, 60, 18);
        addComponent(pnlStart, jButton5, 41, 25, 140, 28);
        addComponent(pnlStart, jButton8, 70, 63, 70, 28);
        // 
        // lblEndDate 
        // 
        lblEndDate.setText("Date");
        // 
        // lblEndTime 
        // 
        lblEndTime.setText("Time");
        // 
        // jButton3 
        // 
        jButton3.setText("jButton3");
        // 
        // jButton4 
        // 
        jButton4.setText("jButton4");
        // 
        // pnlEnd 
        // 
        pnlEnd.setLayout(null);
        pnlEnd.setBorder(new TitledBorder("Title"));
        addComponent(pnlEnd, lblEndDate, 10, 30, 60, 18);
        addComponent(pnlEnd, lblEndTime, 8, 66, 60, 18);
        addComponent(pnlEnd, jButton3, 40, 25, 140, 28);
        addComponent(pnlEnd, jButton4, 70, 63, 70, 28);
        // 
        // EventEditorPanel
        // 
        this.setLocation(new Point(0, 0));
        this.setSize(new Dimension(523, 493));
    } // end initializeComponent - JFrameBuilder generated code.

    //-------------------------------------------------------------
    // Method Name: loadLocations
    //
    //-------------------------------------------------------------
    private void loadLocations() {
        Exception e = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        String loc;

        locations = new Vector<>(6, 1);

        try {
            fis = new FileInputStream(strLocFilename);
            ois = new ObjectInputStream(fis);

            while (true) {  // The expected exit is via EOFException
                loc = (String) ois.readObject();
                if(loc == null) break; // Added this line to avoid an IJ complaint about 'while'
                locations.addElement(loc);
                MemoryBank.debug("  loaded subject: " + loc);
            } // end while
        } catch (FileNotFoundException fnfe) {
            // not a problem; expected first time for each user.
            // So - set some defaults.
            locations.add("Work");
            locations.add("Home");
            locations.add("Office");
            locations.add("1600 Pennsylvania Avenue NW, Washington, DC 20500");
        } catch (EOFException eofe) {
            // System.out.println("End of file reached!");
            try {
                if (null != ois) ois.close();
                fis.close();
            } catch (IOException ioe) {   // This one's a throw-away.
                ioe.printStackTrace(); // not handled but not (entirely) ignored...
            } // end try/catch
        } catch (Exception ex) {
            e = ex;
        } // end try/catch

        if (e != null) {
            String ems;
            ems = "Error in loading " + strLocFilename + "!\n";
            ems = ems + e.toString();
            ems = ems + "\n'Locations' load operation aborted.";
            JOptionPane.showMessageDialog(new Frame(),
                    ems, "Error", JOptionPane.ERROR_MESSAGE);
        } // end if

        locationsChanged = false;
    } // end loadLocations


    /**
     * This method is called from within the constructor.
     * It continues the work of the JFrameBuilder generated code.
     * Replaces some standard components with custom types, retaining their
     * sizes and positions.  Also additional property settings.
     */
    private void reinitializeComponent() {
        Rectangle rectTmp; // For holding original size and location.
        TitledBorder tb;   // For a shorter pointer to the panel borders.

        // Order found below follows the interface starting at top-left, L-R, T-B.

        lblCategory.setFont(Font.decode("Dialog-bold-12"));

        // Set the default Categories.
        if (getSubjectCount() == 0) {
            addSubject("Birthday");
            addSubject("Wedding");
            addSubject("Travel");
            addSubject("Vacation");
            addSubject("Holiday");
            addSubject("Anniversary");
            addSubject("Green light");
            addSubject("Deadline");
            addSubject("Meeting");
        } // end if no subjects were loaded.

        // Replace jComboBox2 with the subjectChooser
        rectTmp = jComboBox2.getBounds();
        remove(jComboBox2);
        addComponent(contentPane, subjectChooser, rectTmp.x, rectTmp.y, rectTmp.width, rectTmp.height);
        subjectChooser.setToolTipText("Type or select a Category for this Event");

        chkboxRetainNote.setFont(Font.decode("Dialog-bold-11"));

        //----------- Start Panel and contents -----------------------------------------------
        // Fix the Start panel text -
        pnlStart.setOpaque(true); // do in JFB
        tb = (TitledBorder) pnlStart.getBorder();
        tb.setTitle("Start");
        tb.setTitleFont(Font.decode("Dialog-bold-14"));

        lblStartDate.setFont(Font.decode("Dialog-bold-11"));

        // Replace jButton5 with btnStartDate.
        rectTmp = jButton5.getBounds();
        pnlStart.remove(jButton5);
        btnStartDate = new LabelButton("Unknown");
        btnStartDate.setName("Start Date");
        btnStartDate.setFont(Font.decode("Dialog-bold-12"));
        btnStartDate.addMouseListener(maDateTimeHandler);
        addComponent(pnlStart, btnStartDate, rectTmp.x, rectTmp.y, rectTmp.width, rectTmp.height);

        lblStartTime.setFont(Font.decode("Dialog-bold-11"));

        // Replace jButton8 with btnStartTime.
        rectTmp = jButton8.getBounds();
        pnlStart.remove(jButton8);
        btnStartTime = new LabelButton("Unknown");
        btnStartTime.setName("Start Time");
        btnStartTime.setToolTipText("Click here to set a Start time");
        btnStartTime.setFont(Font.decode("Dialog-bold-16"));
        btnStartTime.addMouseListener(maDateTimeHandler);
        addComponent(pnlStart, btnStartTime, rectTmp.x, rectTmp.y, rectTmp.width, rectTmp.height);

        // Duration
        //-----------------------------------------------------------
        lblDuration.setFont(Font.decode("Dialog-bold-12"));
        txtfDurationValue.setFont(Font.decode("Dialog-bold-12"));

        txtfDurationValue.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();

                if (!((c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)
                        || (c == KeyEvent.VK_ENTER) || (c == KeyEvent.VK_TAB)
                        || (Character.isDigit(c)))) {
                    e.consume();
                }
            }
        });

        txtfDurationValue.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setDuration();
            }
        });

        txtfDurationValue.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                setDuration();
            }
        });

        AbstractDocument doc = (AbstractDocument) txtfDurationValue.getDocument();
        doc.setDocumentFilter(new FixedSizeDocumentFilter(5));

        ilDurationUnits = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                // Don't care about deselections; only selections.
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setDuration();
                }
            }
        };
        comboxDurationUnits.addItemListener(ilDurationUnits);

        //----------- End Panel and contents -------------------------------------------------
        // Fix the Start panel text -
        tb = (TitledBorder) pnlEnd.getBorder();
        tb.setTitle("End");
        tb.setTitleFont(Font.decode("Dialog-bold-14"));

        lblEndDate.setFont(Font.decode("Dialog-bold-11"));

        // Replace jButton3 with btnEndDate.
        rectTmp = jButton3.getBounds();
        pnlEnd.remove(jButton3);
        btnEndDate = new LabelButton("Unknown");
        btnEndDate.setName("End Date");
        btnEndDate.setFont(Font.decode("Dialog-bold-12"));
        btnEndDate.addMouseListener(maDateTimeHandler);
        addComponent(pnlEnd, btnEndDate, rectTmp.x, rectTmp.y, rectTmp.width, rectTmp.height);

        lblEndTime.setFont(Font.decode("Dialog-bold-11"));

        // Replace jButton4 with btnEndTime.
        rectTmp = jButton4.getBounds();
        pnlEnd.remove(jButton4);
        btnEndTime = new LabelButton("Unknown");
        btnEndTime.setName("End Time");
        btnEndTime.setToolTipText("Click here to set an End time");
        btnEndTime.setFont(Font.decode("Dialog-bold-16"));
        btnEndTime.addMouseListener(maDateTimeHandler);
        addComponent(pnlEnd, btnEndTime, rectTmp.x, rectTmp.y, rectTmp.width, rectTmp.height);
        //------------------------------------------------------------------------------------

        // Location
        lblLocation.setFont(Font.decode("Dialog-bold-12"));

        strLocFilename = MemoryBank.userDataHome + File.separatorChar + "UpcomingLocations";
        rectTmp = comboxLocation.getBounds();
        remove(comboxLocation);
        loadLocations();
        comboxLocation = new JComboBox<>(locations);
        comboxLocation.setFont(Font.decode("Serif-bold-12"));
        comboxLocation.setMaximumRowCount(maxLocations);
        comboxLocation.setEditable(true);
        addComponent(this, comboxLocation, rectTmp.x, rectTmp.y, rectTmp.width, rectTmp.height);

        // Recurrence
        btnRecurrence.setFont(Font.decode("Dialog-bold-12"));
        btnRecurrence.setMargin(new Insets(1, 1, 1, 1));
        btnRecurrence.setFocusable(false);

        btnDateFormat.setFont(Font.decode("Dialog-bold-12"));
        btnDateFormat.setMargin(new Insets(1, 1, 1, 1));
        btnDateFormat.setFocusable(false);

        lblRecurrenceSetting.setText("None");
        lblRecurrenceSetting.setFont(Font.decode("Dialog-bold-11"));

        lblNotes.setFont(Font.decode("Dialog-bold-12"));

        // Replace jTextArea1 with 'body'.
        spaneNotes.setViewportView(body); // replaces jTextArea1
        spaneNotes.validate();
    } // end reinitializeComponent

    // Needed to add this after moving from a JDialog to a JOptionPane.
    public Dimension getPreferredSize() {
        return new Dimension(getMinimumSize());
    }

    private void resetEndPanel() {
        boolean datePast = false;

        // Set the Date button text
        if (eventEndDate == null) {
            btnEndDate.setText("Unknown");
            btnEndDate.setToolTipText("Click here to select a End date");
        } else {
            btnEndDate.setText(dtf.format(eventEndDate));
            btnEndDate.setToolTipText("Right click here to clear the End date");

            LocalDateTime eventEndDateTime;  // Composite, for comparison

            if (eventEndTime == null) {
                eventEndDateTime = eventEndDate.atTime(0,0);
            } else {
                // Make a temporary data object (so we can use its ability
                //   to easily combine a Date with a Time).
                EventNoteData endTmp = new EventNoteData();

                // Combine the user-specified Date and Time
                endTmp.setEndDate(eventEndDate);
                endTmp.setEndTime(eventEndTime);
                eventEndDateTime = endTmp.getEventEndDateTime();
            } // end if

            // Compare the composite value to current time.
            if (LocalDateTime.now().isAfter(eventEndDateTime)) {
                datePast = true;
            } // end if
        } // end if

        // Set the Time button text
        if (eventEndDateTime == null) {
            btnEndTime.setText(" ");
        } else {
//            MemoryBank.tempCalendar.setTime(dateEventEndTime);
            btnEndTime.setText(MemoryBank.makeTimeString(eventEndTime));

//            if (MemoryBank.tempCalendar.get(Calendar.AM_PM) == Calendar.AM) {
            if (eventEndTime.getHour() < 12) { // 0-11 ?
                btnEndTime.setForeground(MemoryBank.amColor);
            } else {     // Calendar.PM
                btnEndTime.setForeground(MemoryBank.pmColor);
            } // end if
        } // end if

        // Color the Panel
        if (eventEndDate == null) {
            pnlEnd.setBackground(backColor);
        } else {
            if (datePast) {
                pnlEnd.setBackground(pastColor);
            } else {
                pnlEnd.setBackground(futureColor);
            } // end if
        } // end if
    } // end resetEndPanel

    private void resetStartPanel() {
        boolean datePast = false;

        // Set the Date button text
        if (eventStartDate == null) {
            btnStartDate.setText("Unknown");
            btnStartDate.setToolTipText("Click here to select a Start date");
        } else {
            btnStartDate.setText(dtf.format(eventStartDate));
            btnStartDate.setToolTipText("Right click here to clear the Start date");

            LocalDateTime eventStartDateTime;  // Composite, for comparison

            if (eventStartTime == null) {
                eventStartDateTime = eventStartDate.atTime(0,0);
            } else {
                // Make a temporary data object (so we can use its ability
                //   to easily combine a Date with a Time).
                EventNoteData endTmp = new EventNoteData();

                // Combine the user-specified Date and Time
                endTmp.setStartDate(eventStartDate);
                endTmp.setStartTime(eventStartTime);
                eventStartDateTime = endTmp.getEventStartDateTime();
            } // end if

            // Compare the composite value to current time.
            if (LocalDateTime.now().isAfter(eventStartDateTime)) {
                datePast = true;
            } // end if
        } // end if

        // Set the Time button text
        if (eventStartTime == null) {
            btnStartTime.setText(" ");
        } else {
//            MemoryBank.tempCalendar.setTime(dateEventStartTime);
            btnStartTime.setText(MemoryBank.makeTimeString(eventStartTime));

//            if (MemoryBank.tempCalendar.get(Calendar.AM_PM) == Calendar.AM) {
            if (eventStartTime.getHour() < 12) { // 0-11 ?
                btnStartTime.setForeground(MemoryBank.amColor);
            } else {     // Calendar.PM
                btnStartTime.setForeground(MemoryBank.pmColor);
            } // end if
        } // end if

        // Color the Panel
        if (eventStartDate == null) {
            pnlStart.setBackground(backColor);
        } else {
            if (datePast) {
                pnlStart.setBackground(pastColor);
            } else {
                pnlStart.setBackground(futureColor);
            } // end if
        } // end if

        // Show a 'readable' interpretation of the Recurrence
        String strTmp;
        // System.out.println("Getting summary for: [" + strRecurrenceSetting + "]");
        strTmp = EventNoteData.getRecurrenceSummary(strRecurrenceSetting);

        // We may have a multi line summary
        strTmp = "<html>" + strTmp + "</html>";

        lblRecurrenceSetting.setText(strTmp);

        if (eventStartDate == null) {
            btnRecurrence.setEnabled(false);
            btnRecurrence.setToolTipText("You must set a Start Date before you can set a Recurrence schedule.");
        } else {
            btnRecurrence.setEnabled(true);
            btnRecurrence.setToolTipText("Click here to set a repeating schedule for this Event.");
        }
    } // end resetStartPanel


    private void saveLocations() {
        MemoryBank.debug("Saving locations: " + locationsChanged);
        if (!locationsChanged) return;

        MemoryBank.debug("Saving subject data in " + strLocFilename);
        try {
            FileOutputStream fos = new FileOutputStream(strLocFilename);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            for (String subj : locations) {
                oos.writeObject(subj);
            } // end for

            oos.flush();
            oos.close();
            fos.close();
            locationsChanged = false;
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        } // end try/catch
    } // end saveLocations


    //-----------------------------------------------
    // Method Name: setDuration
    //
    // Called for either an ActionPerformed or
    // a FocusLost in the Value field, or for an
    // ItemState Changed in the Units field.
    //-----------------------------------------------
    private void setDuration() {
        Long lngDuration = getDurationSetting();
        if (lngDuration == null) {
            // The user may have just entered the units and now
            // intends to enter a value (or vice versa).  We cannot
            // at this point take an inability to get the value as
            // a green-light to reset their input.
            return;
        }

        // Make a temporary data object so that we can collect
        //   existing interface settings into it and then use
        //   it in conjunction with the user's new duration setting
        //   to update our tracking variables accordingly.
        EventNoteData tmpEnd = new EventNoteData();
        tmpEnd.setStartDate(eventStartDate);
        tmpEnd.setStartTime(eventStartTime);
        tmpEnd.setEndDate(eventEndDate);
        tmpEnd.setEndTime(eventEndTime);

        // Let the Data object do the work.
        tmpEnd.setDuration(lngDuration);

        // Update our tracking variables
        eventStartDate = tmpEnd.getStartDate();
        eventStartTime = tmpEnd.getStartTime();
        eventEndDate = tmpEnd.getEndDate();
        eventEndTime = tmpEnd.getEndTime();

        // Cover all bets -
        resetStartPanel();
        resetEndPanel();

        // Except for Duration, which should stay where the user
        //   just put it, of course.
    } // end setDuration


    //----------------------------------------------------------
    // Method Name: setDurationUnits
    //
    // This method provides a way to programmatically set the
    //   Duration Units without firing off the selection
    //   handler.
    //----------------------------------------------------------
    private void setDurationUnits(String s) {

        // The input string comes directly from the data class
        //   and does not necessarily match our combo box
        //   selections.
        s = s.substring(0, 1).toUpperCase() + s.substring(1);
        if (!s.equals("Unknown")) {
            if (!s.endsWith("s")) s += "s";
        } // end if we have units

        comboxDurationUnits.removeItemListener(ilDurationUnits);
        comboxDurationUnits.setSelectedItem(s);
        comboxDurationUnits.addItemListener(ilDurationUnits);
    } // end setDurationUnits


    // Set the interface fields per the input data object.
    public void showTheData(@NotNull EventNoteData end) {

        // Load the interface with the correct data
        //------------------------------------------------
        // These two methods are in the base class.
        setExtText(end.getExtendedNoteString());
        setSubject(end.getSubjectString());

        chkboxRetainNote.setSelected(end.getRetainNote());

        // Grab the Recurrence
        strRecurrenceSetting = end.getRecurrence();

        // Note: for date and time fields, we need to set our local
        //   holding variables in addition to the interface visuals.
        //-------------------------------------------------------------
        eventStartDate = end.getStartDate();
        eventStartTime = end.getStartTime();
        resetStartPanel();

        eventEndDate = end.getEndDate();
        eventEndTime = end.getEndTime();
        resetEndPanel();
        //-------------------------------------------------------------

        // Now we can show a Duration, if possible -
        String strUnits = end.getDurationUnits();
        if (strUnits.equals("unknown")) {
            txtfDurationValue.setText("");
        } else {
            txtfDurationValue.setText(String.valueOf(end.getDurationValue()));
        }
        setDurationUnits(strUnits);

        // Show the Location
        comboxLocation.setSelectedItem(end.getLocation());

        // Will not do a Date Format after all, for now.
        btnDateFormat.setVisible(false);
    } // end showTheData

    private void showTimeDialog(String s) {
        // Make a dialog window to choose a time.
        Frame f = JOptionPane.getFrameForComponent(this);
        JDialog tempwin = new JDialog(f, true);

        tempwin.getContentPane().add(tcTimeChooser, BorderLayout.CENTER);
        tempwin.setTitle(s);
        tempwin.pack();
        tempwin.setResizable(false);

        // Center the dialog relative to the main frame.
        tempwin.setLocationRelativeTo(f);

        // Alternate dialog presentation, with OK / Cancel:

        //  int choice = JOptionPane.showConfirmDialog(
        //      f,                            // for modality
        //      tcTimeChooser,                // UI Object
        //      "Set the time for this note", // pane title bar
        //      JOptionPane.OK_CANCEL_OPTION, // Option type
        //      JOptionPane.QUESTION_MESSAGE, // Message type
        //      null );                       // icon

        // The current thought is that OK is implied and Cancel
        //   is already provided for by the 'Reset' button.

        // Go modal -
        tempwin.setVisible(true);

        if (tcTimeChooser.getClearBoolean()) timeSetting = null;
        else timeSetting = tcTimeChooser.getChoice();
    } // end showTimeDialog


    //-------------------------------------------------------------
    // Method Name: showRecurrenceDialog
    //
    // The RecurrencePanel itself has already been constructed
    //   (once only) - this method simply loads it with the
    //   correct data and displays as a modal dialog.
    //-------------------------------------------------------------
    private void showRecurrenceDialog() {
        // This allows the button to stay 'live' and therefore
        //   reflect a different tool tip that the user can read,
        //   to understand why the Recurrence button does not
        //   appear to be working for them.
        if (eventStartDate == null) return;

        // Send the current data to the Recurrence Editor dialog.
        rpRepeatSetting.showTheData(strRecurrenceSetting, eventStartDate);

        Frame theFrame = JOptionPane.getFrameForComponent(this);

        // Now display the dialog and do not take
        //   bad input for an answer.
        while (true) {
            int choice = JOptionPane.showConfirmDialog(
                    theFrame,                     // for modality
                    rpRepeatSetting,              // UI Object
                    "Set a repetition schedule for this Event", // pane title bar
                    JOptionPane.OK_CANCEL_OPTION, // Option type
                    JOptionPane.PLAIN_MESSAGE,    // Message type
                    null);                       // icon

            if (choice != JOptionPane.OK_OPTION) return;
            if (rpRepeatSetting.isRecurrenceValid()) break;
            JOptionPane.showMessageDialog(this,
                    " Settings were incomplete!\n" +
                            "Please try again, or Cancel",
                    "Recurrence Setting Error", JOptionPane.ERROR_MESSAGE);
        } // end while

        // System.out.println("Accepting user input");

        strRecurrenceSetting = rpRepeatSetting.getRecurrenceSetting();
        // System.out.println("Recurrence setting: " + strRecurrenceSetting);

        resetStartPanel(); // This formats it with HTML.
    } // end showRecurrenceDialog


    private void showDateDialog(String s) {
        // Make a dialog window to choose a date from a Year.
        Frame f = JOptionPane.getFrameForComponent(this);
        JDialog tempwin = new JDialog(f, true);

        tempwin.getContentPane().add(yvDateChooser, BorderLayout.CENTER);
        tempwin.setTitle(s);
        tempwin.setSize(yvDateChooser.getPreferredSize());
        tempwin.setResizable(false);
        yvDateChooser.setDialog(tempwin, 1);

        // Center the dialog relative to the main frame.
        tempwin.setLocationRelativeTo(f);

        // Go modal -
        tempwin.setVisible(true);
    } // end showDateDialog

} // end class EventEditorPanel
