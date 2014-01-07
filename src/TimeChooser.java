// File name: TimeChooser.java  	by D. Lee Chastain
//                                                                        
// Description:  User interface to select a time.
//                                                                       
// Modification History:
// ---------------------
//  7/10/2005 Changed all 'Global' to 'MemoryBank'.
// 10/05/2004 Rewrote isFocusTraversable calls for jdk1.5.0

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

public class TimeChooser extends JPanel implements ActionListener {
  private static final long serialVersionUID = -3452630303180246715L;

  private static Border up;
  private static Border down;
  private static TimeAlterButton hourButton;
  private static TimeAlterButton minuteButton;
  private static TimeAlterButton secondButton;
  private static Font buttonFont;

  // Variables needed by more than one method -
  private Date initialDate;      
  private int myTimeAmPm = Calendar.AM_PM;

  private boolean showSeconds;
  private boolean iAmClear;
  private JButton ampmButton;              // constructor, actionPerformed
  private JButton nowButton;               // constructor, actionPerformed
  private JButton clearButton;             // constructor, actionPerformed
  private JButton resetButton;             // constructor, actionPerformed

  static {
    up = new BevelBorder(BevelBorder.RAISED);
    down = new BevelBorder(BevelBorder.LOWERED);
    buttonFont = Font.decode("Dialog-Bold-14");
  } // end static section

  TimeChooser() {
    this(new Date());
  } // end constructor

  TimeChooser(Date initial) {
    super(new BorderLayout());
    initialDate = initial;
    showSeconds = false;    
    iAmClear = false;

    MemoryBank.tempCalendar.setTime(initialDate);
    MemoryBank.tempCalendar.set(Calendar.SECOND, 0);
    MemoryBank.tempCalendar.set(Calendar.MILLISECOND, 0);

    ampmButton = new JButton("AM") {
      private static final long serialVersionUID = -5963128943094472354L;

      public Insets getMargin() {
        Insets i = new Insets(2, 2, 2, 2);
        return i;
      } // end getMargin
      public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width = 50;
        return d;
    } // end getPreferredSize
    };
    ampmButton.setFocusable(false);
    ampmButton.setFont(Font.decode("Dialog-Bold-18"));
    ampmButton.addActionListener(TimeChooser.this);
    ampmButton.setToolTipText("Toggles AM or PM");

    nowButton = new JButton("Now");
    nowButton.setMargin(new Insets(2, 2, 2, 2));
    nowButton.setFocusable(false);
    nowButton.setFont(buttonFont);
    nowButton.addActionListener(TimeChooser.this);
    nowButton.setToolTipText("Set to current time");

    clearButton = new JButton(" ");
    clearButton.setFocusable(false);
    clearButton.setFont(buttonFont);
    clearButton.addActionListener(TimeChooser.this);
    clearButton.setToolTipText("Clear the time fields");

    resetButton = new JButton("Reset");
    resetButton.setMargin(new Insets(2, 2, 2, 2));
    resetButton.setFocusable(false);
    resetButton.setFont(buttonFont);
    resetButton.addActionListener(TimeChooser.this);
    resetButton.setToolTipText("Reset to the initial time");

    hourButton = new TimeAlterButton(Calendar.HOUR);
    minuteButton = new TimeAlterButton(Calendar.MINUTE);
    secondButton = new TimeAlterButton(Calendar.SECOND);

    JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    northPanel.add(hourButton);
    northPanel.add(minuteButton);
    northPanel.add(secondButton);
    northPanel.add(new Spacer(5,1));
    northPanel.add(ampmButton);
    southPanel.add(nowButton);
    southPanel.add(clearButton);
    southPanel.add(resetButton);
    add(northPanel, "North");
    add(southPanel, "South");

    recalc();
  } // end constructor

  public void actionPerformed(ActionEvent e) {
    JButton source = (JButton) e.getSource();

    if (source.getActionCommand().equals("AM")) {
      MemoryBank.tempCalendar.add(Calendar.HOUR, 12);
    } else if(source.getActionCommand().equals("PM")) {
      MemoryBank.tempCalendar.add(Calendar.HOUR, -12);
    } else if(source.getActionCommand().equals("Now")) {
      MemoryBank.tempCalendar.setTime(new Date());
      iAmClear = false;
    } else if(source.getActionCommand().equals("Reset")) {
      MemoryBank.tempCalendar.setTime(initialDate);
      iAmClear = false;
    } else if(source.getActionCommand().equals(" ")) {
      iAmClear = true;
      hourButton.setText("  ");
      minuteButton.setText("  ");
      secondButton.setText("  ");
      ampmButton.setText("  ");
      myTimeAmPm = Calendar.AM_PM;
    } // end if
    recalc();
  } // end actionPerformed

  public boolean getClearBoolean() { return iAmClear; }

  public int getHour() {
    return MemoryBank.tempCalendar.get(Calendar.HOUR_OF_DAY);
  } // end getHour

  public int getMinute() {
    return MemoryBank.tempCalendar.get(Calendar.MINUTE);
  } // end getMinute

  public int getSecond() {
    return MemoryBank.tempCalendar.get(Calendar.SECOND);
  } // end getSecond

  public void recalc() {
    if(iAmClear) return;

    hourButton.setText(MemoryBank.hourToString(getHour()).trim());
    minuteButton.setText(MemoryBank.minuteToString(getMinute()));
    secondButton.setText(MemoryBank.minuteToString(getSecond()));

    if(showSeconds) {
      secondButton.setVisible(true);
    } else {
      secondButton.setVisible(false);
    } // end if

    // Check AM / PM
    int which = MemoryBank.tempCalendar.get(Calendar.AM_PM);
    Color c;

    if( which != myTimeAmPm ) {
      myTimeAmPm = which;
      if( which == Calendar.AM ) {
        c = MemoryBank.amColor;
        ampmButton.setText("AM");
      } else {
        c = MemoryBank.pmColor;
        ampmButton.setText("PM");
      } // end if

      hourButton.setForeground(c);
      minuteButton.setForeground(c);
      secondButton.setForeground(c);

    } // end if
  } // end recalc

  public void setShowSeconds(boolean b) { 
    showSeconds = b; 
    recalc();
  } // end setShowSeconds

  // Inner class
  //----------------------------------------------------------------
  class TimeAlterButton extends JLabel implements MouseListener {
    private static final long serialVersionUID = -4762284168787775721L;

    private boolean iAmDepressed;
    private Depressed dp;   // A Thread to keep responding
    private int timeField;

    public TimeAlterButton(int t) {
      super();
      timeField = t;
      setHorizontalAlignment(JLabel.CENTER);
      setBorder(up);
      setFont(Font.decode("Dialog-bold-38"));
      setToolTipText("Use right mouse button to decrease");

      iAmDepressed = false;
      addMouseListener(this);
    } // end constructor

    public Dimension getPreferredSize() {
      Dimension d = super.getPreferredSize();
      d.width = 60;
      return d;
    } // end getPreferredSize

    //---------------------------------------------------------
    // MouseListener methods
    //---------------------------------------------------------
    public void mouseClicked(MouseEvent e) {
    } // end mouseClicked

    public void mouseEntered(MouseEvent e) {
    } // end mouseEntered

    public void mouseExited(MouseEvent e) {
      if(iAmDepressed) dp.stopit();
      iAmDepressed = false;
      setBorder(up);
    } // end mouseExited

    public void mousePressed(MouseEvent e) {
      int direction; 

      iAmDepressed = true;
      setBorder(down);

      // Determine whether it is the left or right mouse button.
      int m = e.getModifiers();
      if((m & InputEvent.BUTTON3_MASK) != 0) direction = -1;
      else direction = 1;

      // Since a Thread will die upon return from the rum method,
      //  need to start a new one each time.  Do not want a thread
      //  running but doing nothing but waiting for a mouse click
      //  that might not come, in a program that is busy doing many
      //  other things.
      dp = new Depressed(timeField, direction);
      dp.start();
    } // end mousePressed

    public void mouseReleased(MouseEvent e) {
      if(iAmDepressed) dp.stopit();
      iAmDepressed = false;
      setBorder(up);
    } // end mouseReleased
    //---------------------------------------------------------
  } // end class TimeAlterButton

  class Depressed extends Thread {
    private int delay = 300; // milliseconds
    private boolean iAmRunning;
    private int timeField;
    private int direction;

    public Depressed(int t, int d) {
      super();
      timeField = t;
      direction = d;
      iAmRunning = true;
    } // end constructor

    // A thread 'dies' upon return from run.
    public void run() {
      while(iAmRunning) {
        MemoryBank.tempCalendar.add(timeField, direction);
        recalc();

        try {
          sleep(delay);  // milliseconds
        } catch(InterruptedException ie) {}
        if(delay > 50) delay -= 50;
        // else delay = 0;  // This lets it go tooo fast.
      } // end while
    } // end run

    public void stopit() { iAmRunning = false; }
  } // end class Depressed

  public static void main(String[] args) {
    Frame dcFrame = new Frame("TimeChooser test");
    TimeChooser dc = new TimeChooser();

    dcFrame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        System.exit(0);
      }
    });

    // Needed to override the 'metal' L&F for Swing components.
    String laf = UIManager.getSystemLookAndFeelClassName();
    try {
      UIManager.setLookAndFeel(laf);
    } catch(UnsupportedLookAndFeelException ulafe ) {
    } catch(InstantiationException iee ) {
    } catch(ClassNotFoundException cnfe ) {
    } catch(IllegalAccessException iae) {
    } // end try/catch
    SwingUtilities.updateComponentTreeUI(dc);

    dc.setShowSeconds(true);
    dcFrame.add(dc);
    dcFrame.pack();
    dcFrame.setVisible(true);
  } // end main

} // end class TimeChooser
