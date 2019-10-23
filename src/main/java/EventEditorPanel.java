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
import java.util.Vector;

// An interface to edit the members of an EventNoteData.
// It inherits a subject-management capability.

public class EventEditorPanel extends ExtendedNoteComponent {
    private static final long serialVersionUID = 1L;

    private EventNoteData editedEventNoteData;

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
    private int intPreferredWidth;    // See note in constructor.
    private int intPreferredHeight;   // See note in constructor.
    private DateTimeFormatter dtf;
    private String strLocFilename;   // Locations file name
    private Vector<String> locations;
    private boolean locationsChanged;


    // For Events
    private MouseAdapter maDateTimeHandler;
    private ItemListener ilDurationUnits;
    private KeyAdapter userTyping;

    // Ancillary dialogs -
    private YearView yvDateChooser;
    private TimeChooser tcTimeChooser;
    private RecurrencePanel rpRepeatSetting;

    // Note:  This editor should be initialized with an EventNoteData
    // (by a call to 'showTheData').  Any changes that the user makes
    // to the interface are immediately sent into the initial data
    // object.  May want to disassociate, to allow for a 'cancel'.

    private String strRecurrenceSetting;  // Recurrence

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

        userTyping = new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!((c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)
                        || (c == KeyEvent.VK_ENTER) || (c == KeyEvent.VK_TAB)
                        || (Character.isDigit(c)))) {
                    e.consume();
                }
            }
        };

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

    // This method is only here to 'fool' JFramebuilder-generated code.
    public JPanel getContentPane() {
        return this;
    }


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
    //   reason, so there is no need to check for one ...)
    //-----------------------------------------------------------
    private void handleDateTimeClicked(MouseEvent e) {
        int m = e.getModifiers();
        Component source = (Component) e.getSource();
        String s = source.getName();
        boolean rightClick = false;
        String strTmp;

        if ((m & InputEvent.BUTTON3_MASK) != 0) rightClick = true;

        // Get the initial values of the four main vars -
        LocalDate startDate = editedEventNoteData.getStartDate();
        LocalTime startTime = editedEventNoteData.getStartTime();
        LocalDate endDate = editedEventNoteData.getEndDate();
        LocalTime endTime = editedEventNoteData.getEndTime();

        switch (s) {
            case "Start Date":
                if (rightClick) { // This is a directive to clear the field.
                    if (startDate == null) break; // it was already cleared.
                    editedEventNoteData.setStartDate(null); // clear it.
                    editedEventNoteData.setRecurrence(null); // and any recurrence.
                    startDate = null;
                 } else {  // A request to set it
                    // Initialize  a date-chooser
                    if (startDate != null) yvDateChooser.setChoice(startDate);
                    else yvDateChooser.setChoice(null);

                    showDateDialog("Select a Start Date for the Event");
                    LocalDate newStartDate = yvDateChooser.getChoice();
                    if (newStartDate == startDate) break; // They chose the Date it was already set to.
                    if (editedEventNoteData.setStartDate(newStartDate)) {
                        startDate = newStartDate;
                        strRecurrenceSetting = "";
                    } else { // There is only one reason why a setting might be refused.
                        strTmp = "<html>The Event cannot start after it ends.<br>";
                        strTmp += "Your selection of ";
                        strTmp += dtf.format(newStartDate); // A null value would not have been refused.
                        strTmp += " has been discarded.</html>";

                        JLabel lblTmp = new JLabel(strTmp);
                        lblTmp.setFont(Font.decode("Dialog-bold-12"));
                        JOptionPane.showMessageDialog(this, lblTmp,
                                "Start Date Selection Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                resetStartPanel(startDate, startTime); // Not needed in the 'rejected' case, but doesn't hurt.
                break;
            case "End Date":
                if (rightClick) { // This is a directive to clear the field.
                    if (endDate == null) break; // it was already cleared.
                    editedEventNoteData.setEndDate(null); // clear it.
                    endDate = null; // and clear the value we had previously captured.
                } else {
                    if (endDate != null) yvDateChooser.setChoice(endDate);
                    else yvDateChooser.setChoice(null);

                    showDateDialog("Select an End Date for the Event");
                    LocalDate newEndDate = yvDateChooser.getChoice();
                    if (newEndDate == endDate) break; // They chose the Date it was already set to.
                    if (editedEventNoteData.setEndDate(newEndDate)) {
                        endDate = newEndDate; // update the previously captured value.
                    } else { // There is only one reason why a setting might be refused.
                        strTmp = "<html>The Event cannot end before it starts.<br>";
                        strTmp += "Your selection of ";
                        strTmp += dtf.format(newEndDate); // A null value would not have been refused.
                        strTmp += " has been discarded.</html>";

                        JLabel lblTmp = new JLabel(strTmp);
                        lblTmp.setFont(Font.decode("Dialog-bold-12"));
                        JOptionPane.showMessageDialog(this, lblTmp,
                                "End Date Selection Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                resetEndPanel(endDate, endTime); // Not needed in the 'rejected' case, but doesn't hurt.
                break;
            case "Start Time":
                if (rightClick) { // This is a directive to clear the field.
                    if (startTime == null) break; // it was already cleared.
                    editedEventNoteData.setStartTime(null);
                    startTime = null;
                } else {
                    // If it has already been set once then display it as the initial setting.
                    if (startTime != null) tcTimeChooser = new TimeChooser(startTime);
                        // Otherwise use the current time as the initial setting.
                    else tcTimeChooser = new TimeChooser();
                    showTimeDialog("Select a Start Time");
                    LocalTime theTime = tcTimeChooser.getChoice();

                    if (editedEventNoteData.setStartTime(theTime)) {
                        startTime = theTime;
                    } else {
                        strTmp = "<html>The Event cannot start after it ends.<br>";
                        strTmp += "Your selection of ";
                        strTmp += theTime;
                        strTmp += " has been discarded.</html>";

                        JLabel lblTmp = new JLabel(strTmp);
                        lblTmp.setFont(Font.decode("Dialog-bold-12"));
                        JOptionPane.showMessageDialog(this, lblTmp,
                                "Start Time Selection Error", JOptionPane.ERROR_MESSAGE);
                    }
                } // end if
                resetStartPanel(startDate, startTime); // Not needed in the 'rejected' case, but doesn't hurt.
                break;
            case "End Time":
                if (rightClick) { // This is a directive to clear the field.
                    if (endTime == null) return; // it was already cleared.
                    editedEventNoteData.setEndTime(null);
                    endTime = null;
                } else {
                    if (endTime != null) tcTimeChooser = new TimeChooser(endTime);
                    else tcTimeChooser = new TimeChooser();
                    showTimeDialog("Select an End Time");
                    LocalTime theTime = tcTimeChooser.getChoice();

                    if (editedEventNoteData.setEndTime(theTime)) {
                        endTime = theTime;
                    } else {
                        strTmp = "<html>The Event cannot end before it starts.<br>";
                        strTmp += "Your selection of ";
                        strTmp += theTime;
                        strTmp += " has been discarded.</html>";

                        JLabel lblTmp = new JLabel(strTmp);
                        lblTmp.setFont(Font.decode("Dialog-bold-12"));
                        JOptionPane.showMessageDialog(this, lblTmp,
                                "End Time Selection Error", JOptionPane.ERROR_MESSAGE);
                    }
                } // end if
                resetEndPanel(endDate, endTime); // Not needed in the 'rejected' case, but doesn't hurt.
                break;
        } // end switch

        // Update the Duration fields to keep up with changes to Date/Time that were just made.
        String strUnits = editedEventNoteData.getDurationUnits();
        if (strUnits == null) {
            txtfDurationValue.setText("");
            strUnits = "Unknown";
        } else {
            txtfDurationValue.setText(String.valueOf(editedEventNoteData.getDurationValue()));
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
                if (loc == null) break; // Added this line to avoid an IJ complaint about 'while'
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

        txtfDurationValue.addKeyListener(userTyping);

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

        comboxLocation.addKeyListener(userTyping);
        ItemListener ilLocation = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                // Don't care about deselections; only selections (which happen immediately afterwards)
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setLocation();
                }
            }
        };
        comboxLocation.addItemListener(ilLocation);

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

    // This method sets the content of the End Panel without checks;
    // it just fills in the info it has and does the color-coding, no
    // adjustments to the source info.
    private void resetEndPanel(LocalDate endDate, LocalTime endTime) {

        // Set the Date button text
        if (endDate == null) {
            btnEndDate.setText("Unknown");
            btnEndDate.setToolTipText("Click here to select a End date");
        } else {
            btnEndDate.setText(dtf.format(endDate));
            btnEndDate.setToolTipText("Right click here to clear the End date");
        } // end if

        // Set the Time button text
        if (endTime == null) {
            btnEndTime.setText(" ");
        } else {
            btnEndTime.setText(MemoryBank.makeTimeString(endTime));

            if (endTime.getHour() < 12) { // 0-11 ?
                btnEndTime.setForeground(MemoryBank.amColor);
            } else {     // Calendar.PM
                btnEndTime.setForeground(MemoryBank.pmColor);
            } // end if
        } // end if

        // Check the date against current Date/Time, for color setting.
        boolean datePast = false;
        if (endDate != null) {
            LocalDateTime ldtTemp;
            if (endTime != null) {
                ldtTemp = LocalDateTime.of(endDate, endTime);
            } else {
                ldtTemp = LocalDateTime.of(endDate, LocalTime.MIDNIGHT.minusMinutes(1));
                // Minutes is the finest granularity that we use in this interface.
            }
            if (LocalDateTime.now().isAfter(ldtTemp)) {
                datePast = true;
            } // end if
        }

        // Color the Panel
        if (endDate == null) {
            pnlEnd.setBackground(backColor);
        } else {
            if (datePast) {
                pnlEnd.setBackground(pastColor);
            } else {
                pnlEnd.setBackground(futureColor);
            } // end if
        } // end if
    } // end resetEndPanel

    // This method sets the content of the Start Panel without checks;
    // it just fills in the info it has and does the color-coding, no
    // adjustments to the source info.
    private void resetStartPanel(LocalDate startDate, LocalTime startTime) {

        // Set the Date button text
        if (startDate == null) {
            btnStartDate.setText("Unknown");
            btnStartDate.setToolTipText("Click here to select a Start date");
        } else {
            btnStartDate.setText(dtf.format(startDate));
            btnStartDate.setToolTipText("Right click here to clear the Start date");
        } // end if

        // Set the Time button text
        if (startTime == null) {
            btnStartTime.setText(" ");
        } else {
            btnStartTime.setText(MemoryBank.makeTimeString(startTime));

            if (startTime.getHour() < 12) { // 0-11 ?
                btnStartTime.setForeground(MemoryBank.amColor);
            } else {     // PM
                btnStartTime.setForeground(MemoryBank.pmColor);
            } // end if
        } // end if

        // Check the date against current Date/Time, for color setting.
        boolean datePast = false;
        if (startDate != null) {
            LocalDateTime ldtTemp;
            if (startTime != null) {
                ldtTemp = LocalDateTime.of(startDate, startTime);
            } else {
                ldtTemp = LocalDateTime.of(startDate, LocalTime.MIDNIGHT);
            }
            if (LocalDateTime.now().isAfter(ldtTemp)) {
                datePast = true;
            } // end if
        }

        // Color the Panel
        if (startDate == null) {
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
        strTmp = EventNoteData.getRecurrenceSummary(strRecurrenceSetting);

        // We may have a multi line summary
        strTmp = "<html>" + strTmp + "</html>";

        lblRecurrenceSetting.setText(strTmp);

        if (startDate == null) {
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
    // This method is called for an ActionPerformed or
    // a FocusLost in the Value field, or for an
    // ItemStateChanged in the Units field.
    // The two settings are more tightly coupled here
    // in this interface than in the data class, because
    // here, the Start/End panels need to be reset for
    // either change but in the data the duration value
    // can be set without kicking off a Date/Time
    // calculation.
    //-----------------------------------------------
    private void setDuration() {
        Integer theValue;
        String theValueString = txtfDurationValue.getText();
        if(theValueString.equals("0") || theValueString.isEmpty()) theValue = null;
        else theValue = new Integer(theValueString);
        Object theUnitsSelection = comboxDurationUnits.getSelectedItem();
        String theUnits;
        if(theUnitsSelection != null) theUnits = theUnitsSelection.toString();
        else theUnits = null;

        // These may be getting un-set, as well as possibly getting set to good values.
        editedEventNoteData.setDurationUnits(theUnits);
        editedEventNoteData.setDurationValue(theValue);

        // Update our interface based on the effect of the duration setting that was just done.
        resetStartPanel(editedEventNoteData.getStartDate(), editedEventNoteData.getStartTime());
        resetEndPanel(editedEventNoteData.getEndDate(), editedEventNoteData.getEndTime());
    } // end setDuration

    private void setLocation() {
        // Get the text from the Location combo box, whether it
        //   was placed there via a selection or typing.
        Object objTmpLoc = comboxLocation.getEditor().getItem();
        if (objTmpLoc != null) {
            String strTmpLoc = objTmpLoc.toString().trim();
                if (!strTmpLoc.equals(editedEventNoteData.getLocationString().trim())) {
                    // Only if this represents a change.
                    editedEventNoteData.setLocation(strTmpLoc);
                    if(!strTmpLoc.equals("")) addLocation(strTmpLoc);
                }
        }
        saveLocations();

    } // end setLocation


    //----------------------------------------------------------
    // Method Name: setDurationUnits
    //
    // This method provides a way to programmatically set the
    //   Duration Units without firing off the selection
    //   handler.
    //----------------------------------------------------------
    private void setDurationUnits(String s) {
        if (s == null) return;

        // The input string comes directly from the data class.
        // We need to match it to one of our combobox selections.
        comboxDurationUnits.removeItemListener(ilDurationUnits);
        comboxDurationUnits.setSelectedItem(s);
        comboxDurationUnits.addItemListener(ilDurationUnits);
    } // end setDurationUnits


    // Set the interface fields per the input data object.
    public void showTheData(@NotNull EventNoteData end) {
        editedEventNoteData = new EventNoteData(end);

        // Load the interface with the correct data
        //------------------------------------------------
        // These two methods are in the base class.
        setExtText(end.getExtendedNoteString());
        setSubject(end.getSubjectString());

        chkboxRetainNote.setSelected(end.getRetainNote());

        // Grab the Recurrence
        strRecurrenceSetting = end.getRecurrenceString();

        // Set the text and coloring for Date/Time buttons
        resetStartPanel(end.getStartDate(), end.getStartTime());
        resetEndPanel(end.getEndDate(), end.getEndTime());

        // Show the Duration -
        // Two different possible situations here -
        // First time the Event is being shown in the current session, the calculated fields
        //   will be null because no calc has yet been done.  Any values returned by the
        //   'get' methods will be the explicit user-entered settings.
        // Subsequent showings of the Event could show either set of vars, depending on
        //   the latest user actions.
        // So if we query both settings and get nulls for them both, then we should do
        //   a recalc at this time.
        String strUnits = end.getDurationUnits();
        Integer intVal = end.getDurationValue();
        if(null == intVal && null == strUnits) { // If the stored values are both null
            end.recalcDuration(); // then we should recalculate
            strUnits = end.getDurationUnits(); // and see if we get anything.
            if (strUnits == null) {  // If not then clear the interface fields.
                // For calculated fields, units and values exist (or don't exist) in tandem.
                setDurationUnits("Unknown");
                txtfDurationValue.setText("");
            } else { // Get the new duration value, show both updated/calculated fields.
                setDurationUnits(strUnits);
                intVal = end.getDurationValue();
                txtfDurationValue.setText(String.valueOf(intVal));
            }
        } else { // We didn't get both fields as null, but one of them may be.
            if(null == strUnits) setDurationUnits("Unknown");
            else setDurationUnits(strUnits);
            if (intVal != null) txtfDurationValue.setText(String.valueOf(intVal));
            else txtfDurationValue.setText("");
        }

        // Show the Location
        comboxLocation.setSelectedItem(end.getLocationString());

        // Will not do a Date Format after all, for now.
        btnDateFormat.setVisible(false); // No handler for this.
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
    } // end showTimeDialog


    //-------------------------------------------------------------
    // Method Name: showRecurrenceDialog
    //
    // The RecurrencePanel itself has already been constructed
    //   (once only) - this method simply loads it with the
    //   correct data and displays as a modal dialog.
    //-------------------------------------------------------------
    private void showRecurrenceDialog() {
        LocalDate startDate = editedEventNoteData.getStartDate();

        // This allows the button to stay 'live' and therefore
        //   reflect a different tool tip that the user can read,
        //   to understand why the Recurrence button does not
        //   appear to be working for them.
        if (startDate == null) return;

        // Send the current data to the Recurrence Editor dialog.
        rpRepeatSetting.showTheData(strRecurrenceSetting, startDate);

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

        resetStartPanel(editedEventNoteData.getStartDate(), editedEventNoteData.getStartTime()); // This formats it with HTML.
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

    // We cannot use a 'get' method here, because the hierarchy that led us here is not set
    // up to work that way.  And we cannot just swap out the parameter for our own, because
    // that violates the rule about not reassigning a reference from a method that it was
    // sent to.  So - a painful tedious process of copying out every member that needs to
    // be preserved, and jumping thru the hoops needed to clear the 'set' validations.
    void assimilateTheData(EventNoteData end) {
        // The noteString is not affected by this editor.

        // But the extended note is.
        updateSubject();
        end.setExtendedNoteString(getExtText());

        // For size of the Extended text - non-adjustable by the user
        //   but it is larger here than the default for a Note,
        //   and if this event gets aged off and retained as a
        //   note then this size will be used.
        end.setExtendedNoteWidthInt(body.getWidth() + 10);
        end.setExtendedNoteHeightInt(spaneNotes.getHeight() + 40);

        end.setSubjectString(getSubject());
        // We need to be able to save a blank subject, and recall it,
        //   which is different than if you never set one in the
        //   first place, in which case it would be null and you
        //   should get the default subject.  So -
        //   we allow the assignment without checking its content.

        // Get the current state of the 'Retain Note' checkbox.
        end.setRetainNote(chkboxRetainNote.isSelected());

        // If duration has been entered by the user then we don't want
        // to lose that info when setting the Dates/Times, below.
        Integer preservedDurationValue = editedEventNoteData.getDurationValue();
        String preservedDurationUnits = editedEventNoteData.getDurationUnits();

        // The Start is not allowed to be later than the End.  But what if, during the course
        //   of this interface being used, the End date were first
        //   extended and then the Start date was also increased,
        //   to a value that exceeds the original End date?
        //   Within the interface, this passes validation but now we need
        //   to put the results back into the 'accepted' data and
        //   if we start with the new Start Date, it would not be
        //   legal when compared to the original Event End that would not
        //   yet have been replaced by the data here.  Likewise/conversely for
        //   the other direction, which means that there is no one 'right'
        //   order in which to set them.  So - the only way to be sure
        //   that the updates will be accepted is to first clear
        //   the existing data.
        end.setStartDate(null);
        end.setStartTime(null);
        end.setEndDate(null);
        end.setEndTime(null);

        // Now we can use the values from this interface without further validation.
        end.setStartDate(editedEventNoteData.getStartDate());
        end.setStartTime(editedEventNoteData.getStartTime());
        end.setEndDate(editedEventNoteData.getEndDate());
        end.setEndTime(editedEventNoteData.getEndTime());

        // Do this after setting the start date, since
        //   that action clears recurrence.
        end.setRecurrence(strRecurrenceSetting);

        // But setting Dates/Times has seriously messed with the Duration we might have had,
        // and the preservation we did above still doesn't tell us whether it was
        // user-entered, or had been calculated.  We only want to preserve it if it
        // had been entered.  We can figure that out by checking the current values
        // (which should be the calculated ones) and if they do not match what we
        // preserved then what was preserved must have been entered.  If it does
        // match then any recalculation will reproduce it, so we do nothing.
        String theUnits = end.getDurationUnits();
        Integer theValue = end.getDurationValue();

        boolean sameUnits = (preservedDurationUnits != null) && preservedDurationUnits.equals(theUnits);
        boolean sameValue = (preservedDurationValue != null) && preservedDurationValue.equals(theValue);
        if(!sameUnits) {
            end.setDurationUnits(preservedDurationUnits);
        }
        if(!sameValue) {
            end.setDurationValue(preservedDurationValue);
        }

        // Location
        end.setLocation(editedEventNoteData.getLocationString());

    } // end assimilateTheData
} // end class EventEditorPanel
