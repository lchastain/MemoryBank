/* ***************************************************************************
 *
 * File:  $Id: log.java,v 1.8 2006/07/22 02:06:46 lee Exp $
 *
 * Author:  D. Lee Chastain
 *
 * $Log: log.java,v $
 * Revision 1.8  2006/07/22 02:06:46  lee
 * Changes in support of the new data/component Note hierarchy.
 *
 * Revision 1.7  2006/02/20 00:52:47  lee
 * Converted from JTabbedPane to a JTree.
 *
 * Revision 1.6  2005/10/15 14:40:36  lee
 * Startup changes in debug printing and JFrame initialization.
 *
 * Revision 1.5  2005/09/05 23:08:08  lee
 * Moved screenSize from log to MonthView; now only used in MonthView.
 *
 * Revision 1.4  2005/09/05 15:23:58  lee
 * Changes to support the addition and use of a splash screen.
 *
 * Revision 1.3  2005/08/07 15:33:59  lee
 * Moved the code for 'setDataLocations' out of main and into its own method,
 * for access by other class test drivers.
 *
 * Revision 1.2  2005/07/31 17:54:41  lee
 * Changes in support of new architecture with NoteGroup as a base class
 * rather than an interface.  To significantly reduce code duplication and
 * prepare for additional 'NoteGroup' classes.
 *
 ****************************************************************************/
/** Maintains a searchable database of date/time-related notes,
    events, and activities.  Can be used as a planning tool
    and a chronological reference.
 */

import javax.swing.JFrame;
import java.awt.*;
import java.io.*;      // File, InputStream, OutputStream, ...
import java.text.*;    // SimpleDateFormat, DateFormatSymbols
import java.util.*;    // Vector, Date
import javax.swing.*;

public class MemoryBank {
  //-------------------------------------------------
  // Static members that are developed prior
  //   to running 'main':
  //-------------------------------------------------
  public static final int SAME_DAY = 0;
  public static final int NEXT_DAY = 1;
  public static final int PREV_DAY = 2;

  public static Color amColor;
  public static Color pmColor;
  public static boolean archive;
  public static boolean military;
  public static GregorianCalendar tempCalendar;
  public static SimpleDateFormat sdf;
  public static boolean debug;
  public static boolean event;
  public static boolean init;
  public static boolean trace;
  public static boolean timing;
  public static String userDataDirPathName; // User data top-level directory 'MemoryBank'
  public static String logHome;  // For finding icons & images

  //----------------------------------------------------------
  // Members used in more than one method but only by 'MemoryBank':
  //----------------------------------------------------------
  private static JFrame logFrame;
  private static LogSplash splash;
  private static boolean logApplicationShowing;
  private static int percs[] = { 20, 25, 45, 
      50, 60, 90, 100 };
  private static int updateNum = 0;

  // This section is needed for test drivers that
  //   will try to instantiate objects that need
  //   items developed here (such as the tempCalendar)
  //   without going thru the MemoryBank.main.
  static {
    amColor = Color.blue;
    pmColor = Color.black;
    archive = false;
    military = false; // 12 or 24 hour time display

    // These can be 'defined' in the startup command.  Ex:
    //   java -Ddebug MemoryBank
    debug  = (System.getProperty("debug")  != null);
    event  = (System.getProperty("event")  != null);
    init   = (System.getProperty("init")   != null);
    timing = (System.getProperty("timing") != null);
    trace  = (System.getProperty("trace")  != null);

    if(debug) System.out.println("Debugging printouts on.");
    if(event) System.out.println("Event tracing printouts on.");
    if(init) System.out.println("Initialization trace printouts on.");
    if(timing) System.out.println("Timing printouts on.");
    if(trace) System.out.println("Method trace printouts on.");

    sdf = new SimpleDateFormat();
    sdf.setDateFormatSymbols(new DateFormatSymbols());

    tempCalendar = (GregorianCalendar) Calendar.getInstance();
    // Note: getInstance at this time returns a Calendar that
    //   is actually a GregorianCalendar, but since the return
    //   type is Calendar, it must be cast in order to assign.

    tempCalendar.setGregorianChange(new GregorianCalendar(1752,
        Calendar.SEPTEMBER, 14).getTime());

    //--------------------------------------
    // Establish the locations for data.
    //--------------------------------------
    setDataLocations();
  } // end static


// Change this name - MemoryBank is not an applet.
  //-----------------------------------------------------------------
  // Method Name: init
  //
  // Description:  Prints the class name of the calling context and
  //               specifies whether called from the static section
  //               or the constructor.
  // 
  // Recommended Usage: Call this method as the last line of the
  //                    constructor or static section.  Gives an
  //                    indication that the relevant section has
  //                    executed, and also can help in performance
  //                    tuning.
  //-----------------------------------------------------------------
  public static void init() {
    if(!init) return;

    String mname;
    String cname;

    mname = Thread.currentThread().getStackTrace()[3].getMethodName();
    cname = Thread.currentThread().getStackTrace()[3].getClassName();
    if(mname.equals("<init>")) {
      System.out.println(cname + " constructor  " + new Date().toString());
    } else if(mname.equals("<clinit>")) {
      System.out.println(cname + " static section  " + new Date().toString());
    } else {
      System.out.println("Improper use of constructor trace method!");
      System.out.println(Thread.currentThread().getStackTrace()[3].toString());
    } // end if

  } // end init


  //-----------------------------------------------------------------
  // Method Name: dbg
  //
  // Description:  Prints the input parameter, without a linefeed.
  //-----------------------------------------------------------------
  public static void dbg(String s) {
    if(debug) System.out.print(s);
  } // end dbg


  //-----------------------------------------------------------------
  // Method Name: debug
  //
  // Description:  Prints the input parameter.
  //   Does a 'flush' so that statements are not printed out
  //   of order, when mixed with exceptions. 
  //-----------------------------------------------------------------
  public static void debug(String s) {
    if(debug) { 
      System.out.println(s);
      System.out.flush();
    } // end if
  } // end debug


  //------------------------------------------------------------------
  // Method Name: errorOut
  //
  /** This method is called for fatal io errors.  It will display
      an error dialog with the string parameter as its message,
      then exit the application when the user presses OK.
  */
  //------------------------------------------------------------------
  public static void errorOut(String s) {
    s += "\nThe MemoryBank program will terminate now.";
    JOptionPane.showMessageDialog(logFrame, s, "Error!",
        JOptionPane.ERROR_MESSAGE);
    System.exit(1);  // Abby Normal exit.
  } // end errorOut


  //-----------------------------------------------
  // Method Name: event
  //
  // Description:  Prints the calling context
  //-----------------------------------------------
  public static void event() {
    if(!MemoryBank.event) return;
    System.out.println(Thread.currentThread().getStackTrace()[3].toString());
  } // end event

/*
// Put this in a 'view' base class....
  public static boolean[][] findDataDays(int year) {
    boolean hasDataArray[][] = new boolean [12][31];
    // Will need to normalize the month integers from 0-11 to 1-12 
    //   and the day integers from 0-30 to 1-31

    // System.out.println("Searching for data in the year: " + year);
    String FileName = location + File.separatorChar + year;
    // System.out.println("Looking in " + FileName);

    String foundFiles[] = null;

    File f = new File(FileName);
    if(f.exists()) {
      if(f.isDirectory()) {
        foundFiles = f.list(new logFileFilter("D"));
        f = null; // free the file
      } // end if directory
    } // end if exists

    if(foundFiles == null) return hasDataArray;
    int month, day;

    // System.out.println("Found:");
    for(int i=0; i<foundFiles.length; i++) {
      month = Integer.parseInt(foundFiles[i].substring(1,3));
      day = Integer.parseInt(foundFiles[i].substring(3,5));
      // System.out.println("  " + foundFiles[i]);
      // System.out.println("\tMonth: " + month + "\tDay: " + day);
      hasDataArray[month-1][day-1] = true;
    } // end for i

    return hasDataArray;
  } // end findDataDays
*/
  
  /*/-----------------------------------------------------------------
  // Method Name: findFilename
  //
  // Given a Calendar and a type of file to look for, this method
  //   will return the appropriate filename if it exists and is
  //   unique for the indicated timeframe.
  //-----------------------------------------------------------------
  public static String findFilename(GregorianCalendar cal, String which) {
    String foundFiles[] = null;
    String lookfor = which;
    String fileName = location + File.separatorChar;
    fileName += String.valueOf(cal.get(Calendar.YEAR));

    // System.out.println("Looking in " + fileName);

    File f = new File(fileName);
    if(f.exists()) {
      if(f.isDirectory()) {

        if(!which.equals("Y")) {
          lookfor += getMonthIntString(cal.get(Calendar.MONTH));

          if(!which.equals("M")) {
            int date = cal.get(Calendar.DATE);
            if(date < 10) lookfor += "0";
            lookfor += String.valueOf(date);
          } // end if not a Month note
        } // end if not a Year note
        lookfor += "_";

        // System.out.println("Looking for " + lookfor);
        foundFiles = f.list(new logFileFilter(lookfor));
        f = null; // free the file
      } // end if directory
    } // end if exists

    // Reset this local variable, and reuse.
    fileName = "";

    // Some valid conditions are tested for below but then
    //  have no action when they turn out to be true, so that
    //  they can be recognized and thereby stop some of the
    //   subsequent logical tests.  Ex- once we know the
    //   list is null, do not check its length -
    if(foundFiles == null) {
      // Only happens if directory is not there...
      // A valid condition; take no action.
      // System.out.println("File list was null.");
    } else if(foundFiles.length > 1) {
      String ems = "File list contained multiple entries.";
      JOptionPane.showMessageDialog(null,
          ems, "Error", JOptionPane.ERROR_MESSAGE);
    } else if(foundFiles.length == 0) {
      // A valid (very common) condition; take no action.
      // System.out.println("File list was empty.");
    } else {  // foundFiles.length == 1
      fileName = MemoryBank.location + File.separatorChar;
      fileName += String.valueOf(cal.get(Calendar.YEAR));
      fileName += File.separatorChar;
      fileName += foundFiles[0];
      // System.out.println("Found a file: \n  " + fileName);
    } // end if

    return fileName;
  } // end findFilename
*/

  //-----------------------------------------------------------------------
  // Method Name: getDateString
  //
  //-----------------------------------------------------------------------
  public static String getDateString(long theDateLong, String theFormat) {
    Date d = new Date(theDateLong);
    String s = getRealFormat(theFormat);
    sdf.applyPattern(s);
    return sdf.format(d);
  } // end getDateString


  // Given a Date/Time format that is unique to the MemoryBank program,
  //  return the requested field from that format.
  public static String getFieldFromFormat(int i, String initialFormat) {
    String s;
    int end = initialFormat.indexOf("|");
    if(end == -1) s = "";
    else s = initialFormat.substring(0, end);
    // String may contain any of: "134567"

    int numFields = s.length();
    String theField = "";
    int pos = s.indexOf(String.valueOf(i));
    if(pos == -1) return theField; // this field not in the format.
    // System.out.println("Looking for field " + i + " in " + initialFormat);

    // Get past the order-specifying prefix of the format string
    String suffix = initialFormat.substring(s.length()+1);
    // System.out.println("suffix: " + suffix);

    for(int j=0; j<numFields; j++) {
      if(j == numFields-1) theField = suffix;  // last field
      else theField = suffix.substring(0, suffix.indexOf("|"));
      if(j == pos) break;
      suffix = suffix.substring(theField.length()+1);
    } // end for j

    // The composite Time field may have multiple separators.
    if(i == 5) return theField;

    // Now cut off the separator string, if any.
    int sspos = theField.indexOf("'");
    if(sspos == -1) return theField;
    return theField.substring(0, sspos);
  } // end getFieldFromFormat


  public static int getInteger(String s) {
    int i = -1;
    try {
      return Integer.parseInt(s.trim());
    } catch(NumberFormatException nfe) {
    }
    return i;
  } // end getInteger


  // Here is where the format is interpreted.
  private static String getRealFormat(String theFormat) {
    // System.out.println("Format parsing: [" + theFormat + "]");
    if(theFormat.equals("")) return "";  // never been set
    if(theFormat.equals("0")) return ""; // explicitly set to ""
    String initialFormat = theFormat;

    String s = "";
    int which;
    String theField;

    int end = theFormat.indexOf("|");
    if(end == -1) return "";
    String order = theFormat.substring(0, end);

    for(int i=0; i<order.length(); i++) {
      which = Integer.parseInt(order.substring(i, i+1));
      theField = getFieldFromFormat(which, initialFormat);
      if(which == 5) {
        s += TimeFormatBar.getRealFormat(theField);
      } else {
        s += theField;
        s += getSeparatorFromFormat(initialFormat, which, false);
      } // end if
    } // end for i

    s = s.replace("#SQUOTE#", "''");
    s = s.replace("#DQUOTE#", "\"");
    s = s.replace("#VBAR#", "|");
    // System.out.println("The REAL format specifier is: " + s);
    return s;
  } // end getRealFormat

  // Given the known configuration of the initialFormat string,
  //   parse out and return the requested element.
  private static String getSeparatorFromFormat(String initialFormat, 
        int i, boolean trim) {
    String s;
    int end = initialFormat.indexOf("|");
    if(end == -1) s = "";
    else s = initialFormat.substring(0, end);

    int numFields = s.length();
    String theField = "";
    int pos = s.indexOf(String.valueOf(i));
    if(pos == -1) return " "; // this field not in the format.

    // Get past the order-specifying prefix of the format string
    String suffix = initialFormat.substring(s.length()+1);
    // System.out.println("suffix: " + suffix);

    for(int j=0; j<numFields; j++) {
      if(j == numFields-1) theField = suffix;  // last field
      else theField = suffix.substring(0, suffix.indexOf("|"));
      if(j == pos) break;
      suffix = suffix.substring(theField.length()+1);
    } // end for j

    // Got the Field; now process the separator string, if any.
    int sspos = theField.indexOf("'");
    if(sspos == -1) return "";

    String theSeparator = theField.substring(sspos);
    if(!trim) return theSeparator;

    // Trim of the single quotes.
    theSeparator = theSeparator.substring(1);
    theSeparator = theSeparator.substring(0, theSeparator.length()-1);
    return theSeparator;
  } // end getSeparatorFromFormat


  public static String minuteToString(int minutes) {
    if(minutes < 10) return "0" + String.valueOf(minutes);
    else return String.valueOf(minutes);
  } // end minuteToString


  public static String hourToString(int hour) {
    String s;
    if(military) {
      if(hour < 10) s = "0" + String.valueOf(hour);
      else s = String.valueOf(hour);
    } else {
      if(hour > 12) s = String.valueOf(hour-12);
      else s = String.valueOf(hour);
      if(hour == 0) s = "12";
      if(s.length() == 1) s = " " + s;
    } // end if
    return s;
  } // end hourToString


  public static String makeTimeString() {
    int minute = tempCalendar.get(Calendar.MINUTE);
    
    // Hour portion
    String time = hourToString(tempCalendar.get(Calendar.HOUR_OF_DAY));

    if(!military) time += ":";

    // Minutes portion
    time += minuteToString(minute);

    return time;
  } // end makeTimeString


  // Send in a Date and a signed integer, this method
  //  will set its internal Calendar to that Date
  //  adjusted in minutes by the value of the integer.
  //  Return value indicates whether or not there was 
  //  a day rollover.  Use other methods to access the
  //  resultant Calendar, if desired.
  public static int modMinute(Date d, int m) {
    tempCalendar.setTime(d);
    int minutes = tempCalendar.get(Calendar.MINUTE);
    int dayStart = tempCalendar.get(Calendar.DAY_OF_MONTH);
    minutes += m;
    tempCalendar.set(Calendar.MINUTE, minutes);
    int dayEnd = tempCalendar.get(Calendar.DAY_OF_MONTH);
    if (dayStart == dayEnd) return SAME_DAY;
    if (dayStart < dayEnd) return NEXT_DAY;
    return PREV_DAY;
  } // end modMinute


  // Called locally at startup and later at a 'change'.
  public static boolean setUserDataDirPathName(String s) {
    boolean answer = true;
    File f = new File(s);
    if(f.exists()) {
      if(!f.isDirectory()) {
        // System.out.println("Error - file in place of directory: " + s);
        answer = false;
      } // end if directory
    } else {
      // No need for a prompt for OK to create - this will only occur at 
      //   startup; otherwise the user selected this dir via file nav.
      if(!f.mkdirs()) {
        // System.out.println("Error - Could not create directory: " + s);
        answer = false;
      } // end if
    } // end if exists

    if(answer) {
      userDataDirPathName = s;
    } else {  // Build up and display an informative error message.
      JPanel le = new JPanel(new GridLayout(5, 1, 0, 0));

      String oneline;
      oneline = "The MemoryBank program was not able to access or create:";
      JLabel el1 = new JLabel(oneline);
      JLabel el2 = new JLabel(s, JLabel.CENTER);
      oneline = "This could be due to insufficient permissions ";
      oneline += "or a problem with the file system.";
      JLabel el3 = new JLabel(oneline);
      oneline = "Please try again with a different location, or see";
      oneline += " your system administrator";
      JLabel el4 = new JLabel(oneline);
      oneline = "if you believe the location is a valid one.";
      JLabel el5 = new JLabel(oneline, JLabel.CENTER);

      el1.setFont(Font.decode("Dialog-bold-14"));
      el2.setFont(Font.decode("Dialog-bold-14"));
      el3.setFont(Font.decode("Dialog-bold-14"));
      el4.setFont(Font.decode("Dialog-bold-14"));
      el5.setFont(Font.decode("Dialog-bold-14"));
      
      le.add(el1);
      le.add(el2);
      le.add(el3);
      le.add(el4);
      le.add(el5);

      JOptionPane.showMessageDialog(null, le,
          "Problem with specified location", JOptionPane.ERROR_MESSAGE);
    } // end if

    return answer;
  } // end setLocation


  //-----------------------------------------------
  // Method Name: trace
  //
  // Execution tracing -
  //-----------------------------------------------
  public static void trace(String s) {
    if(!MemoryBank.trace) return;
    String mname;
    String cname;
    String traceString;

    mname = Thread.currentThread().getStackTrace()[3].getMethodName();
    cname = Thread.currentThread().getStackTrace()[3].getClassName();
    traceString = Thread.currentThread().getStackTrace()[3].toString();

    if(mname.equals("<init>")) {
      System.out.print(cname + " constructor");
    } else {
      System.out.print(traceString);
    } // end if

    if(s != null) System.out.println("  " + s);
    else System.out.println();
  } // end trace
  //-----------------------------------------------------


  /*/----------------------------------------------------------------------
  // Inner class
  //----------------------------------------------------------------------
  static class logFileFilter implements FilenameFilter {
    String which;

    logFileFilter(String s) {
      which = s;
    } // end constructor

    public boolean accept(File dir, String name) {
      if(name.startsWith(which)) return true;
      return false;
    } // end accept
  } // end class logFileFilter
  */


  //-----------------------------------------------------------------------
  // Method Name: setDataLocations
  //
  // Called by MemoryBank main and class test drivers.
  //
  // Establish the locations for data.  
  //   User data will be in the user's home directory under a 'MemoryBank'
  //   subdirectory.  Security of user data files will be provided by the 
  //   OS, filesystem, and local security policies.
  //
  // Note:  To see the debug info in this method, use the -Ddebug runtime 
  //   option; the -debug parameter to MemoryBank is interpreted in main, only
  //   after this method is run.
  //-----------------------------------------------------------------------
  public static void setDataLocations() {
    // User data
    String userHome = System.getProperty("user.home");
    String loc = userHome + File.separatorChar + "MemoryBank";
    debug("Setting user data location to: " + loc);
    if(!setUserDataDirPathName(loc)) {  // Some validity testing here..
      System.exit(0);
    } // end if

    // Program data.
    File f = new File("icons"); // Look first in current dir.
    if(f.exists()) {
      // This means we are in the installed program data directory,
      //   running via the installed .jar file.
      logHome = System.getProperty("user.dir");
      debug("MemoryBank Home = " + logHome );
    } else { 
      // The other two cases are that we are either running the class files
      //   under src, or the .jar file one level up from src.  For both of
      //   these cases there is only one possible user/OS - me/Win2K.
      // So - explicitly setting logHome here is OK.
      logHome = "C:\\Program Files\\MemoryBank";
      debug("EXPLICIT MemoryBank Home = " + logHome );
    } // end if

    f = new File(logHome + "/icons");
    if(!f.exists()) {
      errorOut("Cannot find progam data!");
    } // end if
  } // end setDataLocations


  //-----------------------------------------------------------------
  // Method Name: timing
  //
  // Description:  Prints the input parameter with a timestamp.
  //-----------------------------------------------------------------
  public static void timing(String s) {
    if(timing) System.out.println(new Date() + "  " + s);
  } // end timing


  //-------------------------------------------------------------
  // Method Name: update
  //
  // Called by other classes to indicate their progress on the
  //   splash screen, during initialization.
  //-------------------------------------------------------------
  public static void update(String s) {
    int thePercentage;
    if(timing) {
      if(updateNum > 9) System.out.print(updateNum + " ");
      else System.out.print(updateNum + "  ");
      timing(s);
    } // end if

    // Test drivers will not have a splash screen.
    if(splash == null) return;

    // Each new call to update should be planned for and tested,
    //   and a new percs entry added to the list.
    // Otherwise, a subsequent call will overrun the
    //   end of the percs array.
    if( updateNum >= percs.length ) {
      System.out.print("Error! Not enough percentages to cover all the");
      System.out.println(" calls to 'update'.");
      return;
    } // end if

    thePercentage = percs[updateNum++];
    splash.setProgress(s, thePercentage);
  } // end update


  public static void main(String[] args) {
    String s;

    // Hold our place in line, on the taskbar.
    logFrame = new JFrame("Personal Log:");
    logFrame.setLocation(-1000, -1000);
    logFrame.setVisible(true);

    //---------------------------------------------------------------
    // Splash Screen
    //---------------------------------------------------------------
    ImageIcon myImage = new ImageIcon(logHome + "/images/ABOUT.gif");
    splash = new LogSplash(myImage);
    splash.setVisible(true);
    logApplicationShowing = false;
    new Thread(new Runnable() {
      public void run() {
        try {
          while( !logApplicationShowing ) {
            Thread.sleep(1000);
          } // end while
        } catch (Exception e) { }
        splash.setVisible(false);
        splash = null;
      } // end run
    }).start();
    
    //---------------------------------------------------------------
    // Evaluate input parameters, if any.
    //---------------------------------------------------------------
    update("Evaluating parameters");
    if(args.length > 0) 
      System.out.println("Number of args: " + args.length);

    for(int i=0; i<args.length; i++) {
      s = args[i];

      if(s.equals("-debug")) {
        if(!debug) System.out.println("Debugging printouts on.");
        debug = true;
      } else if(s.equals("-event")) {
	if(!event) System.out.println("Event tracing printouts on.");
        event = true;
      } else if(s.equals("-init")) {
	if(!init) System.out.println("Initialization trace printouts on.");
        init = true;  // Constructors and static sections.
      } else if(s.equals("-timing")) {
	if(!timing) System.out.println("Timing printouts on.");
        timing = true;
      } else if(s.equals("-trace")) {
	if(!trace) System.out.println("Method trace printouts on.");
        trace = true;
      } else {
        System.out.println("Parameter not handled: [" + s + "]");
      } // end if/else
    } // end for i


    //--------------------------------------
    // Specify logFrame attributes
    //--------------------------------------
    update("Setting Window variables");
    String userName = System.getProperty("user.name");
    logFrame.setTitle("Personal Log: " + userName);
    logFrame.getRootPane().setOpaque(false);
   
// Attributes to store and retrieve:
// size of main frame
// location of main frame
// user's preferred Log name (vs the system's user name)
// MemoryBank.military (now that it was dropped from day options)
// custom icon?

    // Use our own icon -
    LogIcon theLogIcon = new LogIcon("icons/icon_not.gif");
    theLogIcon = LogIcon.scaleIcon(theLogIcon);
    logFrame.setIconImage(theLogIcon.getImage());

    logFrame.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent we) {
        System.exit(0);
      }
    });
    logFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    //--------------------------------------

    update("Creating the Log Tree");
    final LogTree logTree = new LogTree(logFrame);
    logFrame.setContentPane(logTree);


    update("Laying out graphical components");
    logFrame.pack();

    logFrame.setLocationRelativeTo(null); // Center
    logFrame.setVisible(true);
    update("The MemoryBank Application is ready");
    logApplicationShowing = true;

    //---------------------------------------------------------------------
    // Set up a shutdown hook, to save all data before exit,
    //   whether or not it was a planned exit.
    //---------------------------------------------------------------------
    Thread logPreClose = new Thread(new Runnable() {
      public void run() {
        logTree.preClose();
      } // end run
    });
    Runtime.getRuntime().addShutdownHook(logPreClose);
    
  } // end main
} // end class MemoryBank


