/* ***************************************************************************
 *
 * File:  $Id: DayNoteComponent.java,v 1.1 2006/07/22 02:04:29 lee Exp $
 *
 * Author:  D. Lee Chastain
 *
 * $Log: DayNoteComponent.java,v $
 * Revision 1.1  2006/07/22 02:04:29  lee
 * New file in support of a data/component Note hierarchy.
 *
 ****************************************************************************/
/**  Representation of a single Day Note.
 */

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

public class DayNoteComponent extends IconNoteComponent {
  private static final long serialVersionUID = 1L;

  public static final int DAYNOTEHEIGHT = ICONNOTEHEIGHT;
  
  // The Members
  private DayNoteData myDayNoteData;
  protected NoteTimeLabel noteTimeLabel;
  protected int myTimeAmPm = Calendar.AM_PM;
  private Date myTime;

  // Private static values that are accessed from multiple contexts.
  protected static JMenuItem clearTimeMi;
  protected static JMenuItem setTimeMi;
  protected static JPopupMenu timePopup;

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
    super.clear();
    noteTimeLabel.clear();  // Clear the Time Label
  } // end clear


  //-----------------------------------------------------------------
  // Method Name: getNoteData
  //
  // Returns the data object that this component encapsulates
  //   and manages.  Used primarily in operations at 
  //   the group level such as load, save, shift, sort, etc.
  //-----------------------------------------------------------------
  public NoteData getNoteData() {
    if(!initialized) return null;

    // DO NOT do this here....  6/17/2007
    myDayNoteData.setTimeOfDayDate(myTime);

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
    myTime = new Date();
    resetTimeLabel();
  } // end initialize

  
  protected void makeDataObject() {
    myDayNoteData = new DayNoteData();
  } // end makeDataObject
  
  
  //-------------------------------------------------------------------
  // Method Name: noteActivated
  //
  // Overrides the (no-op) base class behavior.
  //-------------------------------------------------------------------
  protected void noteActivated(boolean noteIsActive) {
    super.noteActivated(noteIsActive);
    if( !noteIsActive ) {
      noteTimeLabel.setInactive();
    } // end if
  } // end noteActivated
  

  //----------------------------------------------------------
  // Method Name: resetComponent
  //
  // Called after a change to the encapsulated data, to show
  //   the visual effects of the change.
  //----------------------------------------------------------
  protected void resetComponent() {
    super.resetComponent(); // the Icon (from there, the note text)
    resetTimeLabel();
  } // end resetComponent


  // This method is called in response to a 'military' toggle 
  //   as well as when initializing or updating the time.
  public void resetTimeLabel() {
    if(myTime == null) {
      // The following statement could be needed if a DayNoteComponent had 
      //   had its time cleared, and then it was being shifted up or down.
      noteTimeLabel.setText("     ");  // enough room for 'HH:MM'
      // Otherwise, if it had been cleared and this method is called by
      //   a time format toggle, it is not needed but no harm done.
      return;
    } // end if
    MemoryBank.tempCalendar.setTime(myTime);
    noteTimeLabel.setText(MemoryBank.makeTimeString());

    // Check AM / PM
    int which = MemoryBank.tempCalendar.get(Calendar.AM_PM);
    if( which != myTimeAmPm ) {
      if( which == Calendar.AM ) {
        noteTimeLabel.setForeground(MemoryBank.amColor);
      } else {     // Calendar.PM
        noteTimeLabel.setForeground(MemoryBank.pmColor);
      } // end if

      myTimeAmPm = which;
    } // end if
  } // end resetTimeLabel


  //----------------------------------------------------------
  // Method Name: setNoteData
  //
  // Overrides the base class
  //----------------------------------------------------------
  public void setNoteData(NoteData newNoteData) {
    setNoteData((DayNoteData) newNoteData);
  } // end setNoteData


  //----------------------------------------------------------
  // Method Name: setNoteData
  //
  // An overload of the method in the base class.
  // Called directly by a NoteGroup during a load, and
  //   indirectly (via swap) for a shift up/down.
  // Do not send a null; if you want to 'un' set the note
  //   data then call 'clear' instead.
  //----------------------------------------------------------
  public void setNoteData(DayNoteData newNoteData) {
    
    // LogUtil.localDebug(true);
    myDayNoteData = newNoteData;
    initialized = true;

    myTime = newNoteData.getTimeOfDayDate();
    MemoryBank.debug("My time: " + myTime);

    // update visual components without updating the 'lastModDate'
    resetComponent();
    // LogUtil.localDebug(false);
    
    setNoteChanged(); 
  } // end setNoteData


  protected void shiftDown() {
    if(noteTimeLabel.isActive) {
      // add one minute
      int dayCheck = MemoryBank.modMinute(myTime, -1);
      if( dayCheck == MemoryBank.SAME_DAY ) {
        myTime = MemoryBank.tempCalendar.getTime();
        resetTimeLabel();
        DayNoteComponent.this.setNoteChanged(); 
      } // end if
    } else {
      myNoteGroup.shiftDown(index);
    } // end if
  } // end shiftDown

  protected void shiftUp() {
    if(noteTimeLabel.isActive) {
      // subtract one minute
      int dayCheck = MemoryBank.modMinute(myTime, 1);
      if( dayCheck == MemoryBank.SAME_DAY ) {
        myTime = MemoryBank.tempCalendar.getTime();
        resetTimeLabel();
        DayNoteComponent.this.setNoteChanged(); 
      } // end if
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
    //   a separatate copy of the data object, just the reference to it.

    // So - copy the data objects.
    if(dnd1 != null) dnd1 = new DayNoteData(dnd1);
    if(dnd2 != null) dnd2 = new DayNoteData(dnd2);
    
    if(dnd1 == null) dnc.clear();
    else dnc.setNoteData(dnd1);
    
    if(dnd2 == null) this.clear();
    else this.setNoteData(dnd2);
    
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
      myTime = null;
    } // end clear

    public Dimension getPreferredSize() {
      return new Dimension(timeWidth, DAYNOTEHEIGHT);
    } // end getPreferredSize


    protected void setInactive() {
      setBorder(highBorder);
      isActive = false;
    } // end setInactive

    
    private void showTimeChooser() {
      DayNoteComponent.this.setBorder(redBorder);

      Frame theFrame = JOptionPane.getFrameForComponent(this);

      TimeChooser tc = new TimeChooser(myTime);
      int choice = JOptionPane.showConfirmDialog(
          theFrame,                     // for modality
          tc,                           // UI Object
          "Set the time for this note", // pane title bar
          JOptionPane.OK_CANCEL_OPTION, // Option type
          JOptionPane.QUESTION_MESSAGE, // Message type
          null );                       // icon

      DayNoteComponent.this.setBorder(null);
      if(choice != JOptionPane.OK_OPTION) return;

      if(tc.getClearBoolean()) clear();
      else {
        myTime = MemoryBank.tempCalendar.getTime();
        
        System.out.println("The date is: " + myTime);
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
    public void showTimePopup(MouseEvent me) {
      ActionListener[] ala;

      ala = clearTimeMi.getActionListeners();
      for( ActionListener al: ala ) clearTimeMi.removeActionListener( al );
      clearTimeMi.addActionListener(this);

      ala = setTimeMi.getActionListeners();
      for( ActionListener al: ala ) setTimeMi.removeActionListener( al );
      setTimeMi.addActionListener(this);

      timePopup.show(me.getComponent(), me.getX(), me.getY());
    } // end showTimePopup


    //---------------------------------------------------------
    // Menu Item action handler for NoteTimeLabel
    //---------------------------------------------------------
    public void actionPerformed(ActionEvent e) {
      JMenuItem jm = (JMenuItem) e.getSource();
      String s = jm.getText();
      if(s.equals("Clear Line")) {
        clear();
      } else if(s.equals("Clear Time")) {
        // Do not set myTime to null; just clear the visual indicator.
        //  This leaves the note still initialized; critical to decisions
        //  made at load time.
        noteTimeLabel.clear();
      } else if(s.equals("Set Time")) {
        noteTimeLabel.showTimeChooser();
      } else {  // Nothing else expected so print it out -
        System.out.println(s);
      } // end if/else if

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
      if(!initialized) return;

      int m = e.getModifiers();
      // Single click, right mouse button.
      if((m & InputEvent.BUTTON3_MASK) != 0) {
        if(e.getClickCount() >= 2) return;
        // Show the popup menu
        showTimePopup(e);

        // Double click, left mouse button.
      } else if(e.getClickCount() == 2) {
        showTimeChooser(); // bring up a mouse-controlled time interface

      } else { // Single Left Mouse Button
        if(isActive) {  // This implements a 'toggle'
          setBorder(highBorder);
          isActive = false;
        } else {
          setBorder(lowBorder);
          isActive = true;
          if( getText().trim().equals("") ) {
            // This can happen if a previously initialized
            //   note has had its time cleared.
            myTime = new Date();
            DayNoteComponent.this.resetTimeLabel();
            DayNoteComponent.this.setNoteChanged(); 
          } // end if
        } // end if
      } // end if/else if
    } // end mouseClicked

    public void mouseEntered(MouseEvent e) {
      if(!initialized) return;
      myNoteGroup.setMessage("Left click mouse to activate, then " +
         "shift up/down arrows to adjust time");
    } // end mouseEntered

    public void mouseExited(MouseEvent e) { myNoteGroup.setMessage(" "); }
    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
    //---------------------------------------------------------
  } // end class NoteTimeLabel

} // end class DayNoteComponent


// Embedded Data class
//-------------------------------------------------------------------
//
class DayNoteData extends IconNoteData implements Serializable {
  private static final long serialVersionUID = -2202469274687602102L;
  protected Date    timeOfDayDate;

  public DayNoteData() {
    super();
  } // end constructor


  // Alternate constructor, for starting 
  //   with common base class data.
  public DayNoteData(IconNoteData ind) {
    super(ind);
    // In this case, the invoking context is responsible for
    //   making an additional call to setTimeOfDayDate().
  } // end constructor


  // Alternate constructor, for starting 
  //   with common base class data.
  public DayNoteData(NoteData nd) {
    this((IconNoteData)nd);
    // In this case, the invoking context is responsible for
    //   making additional calls to 'set' methods.
  } // end constructor


  // The copy constructor (clone)
  public DayNoteData(DayNoteData dnd) {
    super(dnd);
    
    timeOfDayDate = dnd.timeOfDayDate;
  } // end constructor


  protected void clear() {
    super.clear();
    timeOfDayDate = null;
  } // end clear


  public Date getTimeOfDayDate() { return timeOfDayDate; }
  
  public void setTimeOfDayDate(Date value) { timeOfDayDate = value; }

} // end class DayNoteData



