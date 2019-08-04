import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.*;
import java.io.*;      // File, InputStream, OutputStream, ...
import java.text.*;    // SimpleDateFormat, DateFormatSymbols
import java.util.*;    // Vector, Date
import javax.swing.*;
import javax.swing.plaf.FontUIResource;

public class MemoryBank {
    //-------------------------------------------------
    // Static members that are developed prior
    //   to running 'main':
    //-------------------------------------------------
    private static ObjectMapper mapper = new ObjectMapper();
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
    public static boolean timing;
    public static String userDataHome; // User data top-level directory 'mbankData'
    public static String logHome;  // For finding icons & images
    private static AppOptions appOpts;     // saved/loaded
    private static AppTreePanel appTreePanel;

    //----------------------------------------------------------
    // Members used in more than one method but only by 'MemoryBank':
    //----------------------------------------------------------
    private static JFrame logFrame;
    private static AppSplash splash;
    private static boolean logApplicationShowing;
    private static int percs[] = {20, 25, 45,
            50, 60, 90, 100};
    private static int updateNum = 0;

    // This section is needed for test drivers that
    //   will try to instantiate objects that need
    //   items developed here (such as the tempCalendar)
    //   without going thru the MemoryBank.main.
    static {
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        setProgramDataLocation();

        appOpts = new AppOptions(); // Start with default values.

        // Set the Look and Feel
        try {
            String thePlaf = "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel";
            System.out.println("Setting plaf to: " + thePlaf);
            UIManager.setLookAndFeel(thePlaf);
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        }

        // Global setting for tool tips
        UIManager.put("ToolTip.font", new FontUIResource("SansSerif", Font.BOLD, 12));

        amColor = Color.blue;
        pmColor = Color.black;
        archive = false;
        military = false; // 12 or 24 hour time display

        // These can be 'defined' in the startup command.  Ex:
        //   java -Ddebug MemoryBank
        debug = (System.getProperty("debug") != null);
        event = (System.getProperty("event") != null);
        init = (System.getProperty("init") != null);
        timing = (System.getProperty("timing") != null);

        if (debug) System.out.println("Debugging printouts on.");
        if (event) System.out.println("Event tracing printouts on.");
        if (init) System.out.println("Initialization trace printouts on.");
        if (timing) System.out.println("Timing printouts on.");

        sdf = new SimpleDateFormat();
        sdf.setDateFormatSymbols(new DateFormatSymbols());

        tempCalendar = (GregorianCalendar) Calendar.getInstance();
        // Note: getInstance at this time returns a Calendar that
        //   is actually a GregorianCalendar, but since the return
        //   type is Calendar, it must be cast in order to assign.

        tempCalendar.setGregorianChange(new GregorianCalendar(1752,
                Calendar.SEPTEMBER, 14).getTime());

    } // end static


    public static AppTreePanel getAppTreePanel() {
        return appTreePanel;
    }

    //------------------------------------------------------
    // Method Name: loadOpts
    //
    // Load the last known state of the app.  This includes
    //  info about the tree as well as other settings.
    //------------------------------------------------------
    private static void loadOpts() {
        Exception e = null;
        FileInputStream fis;
        //AppOptions tmp;
        Object tmp;

        String FileName = MemoryBank.userDataHome + File.separatorChar + "app.options";

        try {
            fis = new FileInputStream(FileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            tmp = ois.readObject();
            appOpts = new AppOptions(tmp);
            ois.close();
            fis.close();
        } catch (ClassCastException cce) {
            e = cce;
        } catch (ClassNotFoundException cnfe) {
            e = cnfe;
        } catch (InvalidClassException ice) {
            e = ice;
        } catch (FileNotFoundException fnfe) {
            // not a problem; use defaults.
            debug("User tree options not found; using defaults");
        } catch (EOFException eofe) {
            e = eofe;
        } catch (IOException ioe) {
            e = ioe;
        } // end try/catch

        if (e != null) {
            String ems = "Error in loading " + FileName + " !\n";
            ems = ems + e.toString();
            ems = ems + "\nOptions load operation aborted.";
            JOptionPane.showMessageDialog(null,
                    ems, "Error", JOptionPane.ERROR_MESSAGE);
        } // end if
    } // end loadOpts


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
        if (!init) return;

        String mname;
        String cname;

        mname = Thread.currentThread().getStackTrace()[3].getMethodName();
        cname = Thread.currentThread().getStackTrace()[3].getClassName();
        if (mname.equals("<init>")) {
            System.out.println(cname + " constructor  " + new Date().toString());
        } else if (mname.equals("<clinit>")) {
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
        if (debug) System.out.print(s);
    } // end dbg


    //-----------------------------------------------------------------
    // Method Name: debug
    //
    // Description:  Prints the input parameter.
    //   Does a 'flush' so that statements are not printed out
    //   of order, when mixed with exceptions.
    //-----------------------------------------------------------------
    public static void debug(String s) {
        if (debug) {
            System.out.println(s);
            System.out.flush();
        } // end if
    } // end debug


    //------------------------------------------------------------------
    // Method Name: errorOut
    //

    /**
     * This method is called for fatal io errors.  It will display
     * an error dialog with the string parameter as its message,
     * then exit the application when the user presses OK.
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
        if (!MemoryBank.event) return;
        System.out.println(Thread.currentThread().getStackTrace()[3].toString());
    } // end event


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
        if (end == -1) s = "";
        else s = initialFormat.substring(0, end);
        // String may contain any of: "134567"

        int numFields = s.length();
        String theField = "";
        int pos = s.indexOf(String.valueOf(i));
        if (pos == -1) return theField; // this field not in the format.
        // System.out.println("Looking for field " + i + " in " + initialFormat);

        // Get past the order-specifying prefix of the format string
        String suffix = initialFormat.substring(s.length() + 1);
        // System.out.println("suffix: " + suffix);

        for (int j = 0; j < numFields; j++) {
            if (j == numFields - 1) theField = suffix;  // last field
            else theField = suffix.substring(0, suffix.indexOf("|"));
            if (j == pos) break;
            suffix = suffix.substring(theField.length() + 1);
        } // end for j

        // The composite Time field may have multiple separators.
        if (i == 5) return theField;

        // Now cut off the separator string, if any.
        int sspos = theField.indexOf("'");
        if (sspos == -1) return theField;
        return theField.substring(0, sspos);
    } // end getFieldFromFormat


    // Here is where the format is interpreted.
    private static String getRealFormat(String theFormat) {
        // System.out.println("Format parsing: [" + theFormat + "]");
        if (theFormat.equals("")) return "";  // never been set
        if (theFormat.equals("0")) return ""; // explicitly set to ""

        String s = "";
        int which;
        String theField;

        int end = theFormat.indexOf("|");
        if (end == -1) return "";
        String order = theFormat.substring(0, end);

        for (int i = 0; i < order.length(); i++) {
            which = Integer.parseInt(order.substring(i, i + 1));
            theField = getFieldFromFormat(which, theFormat);
            if (which == 5) {
                s += TimeFormatBar.getRealFormat(theField);
            } else {
                s += theField;
                s += getSeparatorFromFormat(theFormat, which, false);
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
        if (end == -1) s = "";
        else s = initialFormat.substring(0, end);

        int numFields = s.length();
        String theField = "";
        int pos = s.indexOf(String.valueOf(i));
        if (pos == -1) return " "; // this field not in the format.

        // Get past the order-specifying prefix of the format string
        String suffix = initialFormat.substring(s.length() + 1);
        // System.out.println("suffix: " + suffix);

        for (int j = 0; j < numFields; j++) {
            if (j == numFields - 1) theField = suffix;  // last field
            else theField = suffix.substring(0, suffix.indexOf("|"));
            if (j == pos) break;
            suffix = suffix.substring(theField.length() + 1);
        } // end for j

        // Got the Field; now process the separator string, if any.
        int sspos = theField.indexOf("'");
        if (sspos == -1) return "";

        String theSeparator = theField.substring(sspos);
        if (!trim) return theSeparator;

        // Trim of the single quotes.
        theSeparator = theSeparator.substring(1);
        theSeparator = theSeparator.substring(0, theSeparator.length() - 1);
        return theSeparator;
    } // end getSeparatorFromFormat


    public static String minuteToString(int minutes) {
        if (minutes < 10) return "0" + String.valueOf(minutes);
        else return String.valueOf(minutes);
    } // end minuteToString


    public static String hourToString(int hour) {
        String s;
        if (military) {
            if (hour < 10) s = "0" + String.valueOf(hour);
            else s = String.valueOf(hour);
        } else {
            if (hour > 12) s = String.valueOf(hour - 12);
            else s = String.valueOf(hour);
            if (hour == 0) s = "12";
            if (s.length() == 1) s = " " + s;
        } // end if
        return s;
    } // end hourToString


    public static String makeTimeString() {
        int minute = tempCalendar.get(Calendar.MINUTE);

        // Hour portion
        String time = hourToString(tempCalendar.get(Calendar.HOUR_OF_DAY));

        if (!military) time += ":";

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
        if (f.exists()) {
            if (!f.isDirectory()) {
                // System.out.println("Error - file in place of directory: " + s);
                answer = false;
            } // end if directory
        } else {
            // No need for a prompt for OK to create - this will only occur at
            //   startup; otherwise the user selected this dir via file nav.
            if (!f.mkdirs()) {
                // System.out.println("Error - Could not create directory: " + s);
                answer = false;
            } // end if
        } // end if exists

        if (answer) {
            userDataHome = s;
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
    } // end setUserDataDirPathName


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


    // -----------------------------------------------------------------------
    // Method Name: setProgramDataLocation
    //
    // Set the filesystem location for program data - 'logHome'.
    // Look first in the current directory.  This allows the program data to come
    // from a development location.  If not found then set it explicitly to the default
    // location but test that it is valid, by checking for the 'icons' subdirectory.
    //
    // Note:  the -debug parameter to MemoryBank is interpreted in main only
    //   after this method is run, so we do not use it here.
    // -----------------------------------------------------------------------
    public static void setProgramDataLocation() {
        String currentDir = System.getProperty("user.dir");
        System.out.println("The current working directory is: " + currentDir);

        // Program data - icons, images, etc, the same for every user.
        File f = new File("icons"); // Look first in current dir.
        if (f.exists()) {  // This logic is far from infallible...
            logHome = currentDir;
            System.out.println("MemoryBank Home = " + logHome);
        } else {
            // Explicitly setting logHome for now.
            logHome = "C:\\Program Files\\Memory Bank"; // need to use System calls here, vs hard-coded C:\
            System.out.println("EXPLICIT MemoryBank Home = " + logHome);

            // But test to see if we have icons available at that location -
            f = new File(logHome + File.separatorChar + "icons");
            if (!f.exists()) {
                errorOut("Cannot find program data!");
            } // end if
        } // end if
    } // end setProgramDataLocation


    public static void setUserDataHome(String userEmail) {
        // User data - personal notes, different for each user.
        String currentDir = System.getProperty("user.dir");
        System.out.println("The current working directory is: " + currentDir);
        File f = new File("appData"); // Look first in current dir.
        String loc;
        if (f.exists()) {
            loc = currentDir + File.separatorChar + "appData" + File.separatorChar + userEmail;
        } else {
            String userHome = System.getProperty("user.home"); // Home directory.
            loc = userHome + File.separatorChar + "mbankData" + File.separatorChar + userEmail;
        }
        System.out.println("Setting user data location to: " + loc);

        boolean answer = true;
        f = new File(loc);
        if (f.exists()) {
            if (!f.isDirectory()) {
                // System.out.println("Error - file in place of directory: " + loc);
                answer = false;
            } // end if directory
        } else {
            // No need for a prompt for OK to create - this will only occur at startup.
            if (!f.mkdirs()) {
                // System.out.println("Error - Could not create directory: " + loc);
                answer = false;
            } // end if
        } // end if exists

        if (answer) {
            userDataHome = loc;
        } else {  // Build up and display an informative error message.
            JPanel le = new JPanel(new GridLayout(5, 1, 0, 0));

            String oneline;
            oneline = "The MemoryBank program was not able to access or create:";
            JLabel el1 = new JLabel(oneline);
            JLabel el2 = new JLabel(loc, JLabel.CENTER);
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

        if (!answer) {  // Some validity testing here..
            System.exit(0);
        } // end if
    } // end setUserDataHome


    //-----------------------------------------------------------------
    // Method Name: timing
    //
    // Description:  Prints the input parameter with a timestamp.
    //-----------------------------------------------------------------
    public static void timing(String s) {
        if (timing) System.out.println(new Date() + "  " + s);
    } // end timing


    //-------------------------------------------------------------
    // Method Name: update
    //
    // Called by other classes to indicate their progress on the
    //   splash screen, during initialization.
    //-------------------------------------------------------------
    public static void update(String s) {
        int thePercentage;
        if (timing) {
            if (updateNum > 9) System.out.print(updateNum + " ");
            else System.out.print(updateNum + "  ");
            timing(s);
        } // end if

        // Test drivers will not have a splash screen.
        if (splash == null) return;

        // Each new call to update should be planned for and tested,
        //   and a new percs entry added to the list.
        // Otherwise, a subsequent call will overrun the
        //   end of the percs array.
        if (updateNum >= percs.length) {
            System.out.print("Error! Not enough percentages to cover all the");
            System.out.println(" calls to 'update'.");
            return;
        } // end if

        thePercentage = percs[updateNum++];
        splash.setProgress(s, thePercentage);
    } // end update


    public static void main(String[] args) {
        String s; // holds the startup flag(s).
        String userEmail = "default.user@elseware.com";

        // Hold our place in line, on the taskbar.
        logFrame = new JFrame("Memory Bank:");
        logFrame.setLocation(-1000, -1000);  // Offscreen; not ready to be seen, yet.
        logFrame.setVisible(true);

        //---------------------------------------------------------------
        // Splash Screen
        //---------------------------------------------------------------
        ImageIcon myImage = new ImageIcon(logHome + "/images/ABOUT.gif");
        splash = new AppSplash(myImage);
//        splash.setVisible(true);
//        logApplicationShowing = false;
//        new Thread(new Runnable() {
//            public void run() {
//                try {
//                    while (!logApplicationShowing) {
//                        Thread.sleep(1000);
//                    } // end while
//                } catch (Exception e) {
//                    System.out.println("Exception: " + e.getMessage());
//                }
//                splash.setVisible(false);
//                splash = null;
//            } // end run
//        }).start();

        //---------------------------------------------------------------
        // Evaluate input parameters, if any.
        //---------------------------------------------------------------
        update("Evaluating parameters");
        if (args.length > 0)
            System.out.println("Number of args: " + args.length);

        for (String arg : args) { // Cycling thru them this way, position is irrelevant.
            s = arg;

            if (s.equals("-debug")) {
                if (!debug) System.out.println("Debugging printouts on.");
                debug = true;
            } else if (s.equals("-event")) {
                if (!event) System.out.println("Event tracing printouts on.");
                event = true;
            } else if (s.equals("-init")) {
                if (!init) System.out.println("Initialization trace printouts on.");
                init = true;  // Constructors and static sections.
            } else if (s.equals("-timing")) {
                if (!timing) System.out.println("Timing printouts on.");
                timing = true;
            } else if (s.indexOf('@') > 0) {
                userEmail = s;
            } else {
                System.out.println("Parameter not handled: [" + s + "]");
            } // end if/else
        } // end for i

        setUserDataHome(userEmail);

        // Load the user settings
        loadOpts(); // If available, overrides defaults.

        // Temporary; this is not the change we need; need to change what is stored and retrieved,
        // then the printout will not need a conversion.
        System.out.println("JSON appOpts: " + toJsonString(appOpts));

// Change the opts to JSON data, after loading, do a debug printout of ALL, not just the pane separator.

        System.out.println("Pane separator: " + appOpts.paneSeparator);

        //--------------------------------------
        // Specify logFrame attributes
        //--------------------------------------
        update("Setting Window variables");
        String userName = System.getProperty("user.name");
        logFrame.setTitle("Memory Bank for: " + userEmail);
        logFrame.getRootPane().setOpaque(false);

// Attributes to store and retrieve:
// size of main frame
// location of main frame
// user's preferred Log name (vs the system's user name)
// MemoryBank.military (now that it was dropped from day options)
// custom icon?

        // Use our own icon -
        AppIcon theAppIcon = new AppIcon(logHome + File.separatorChar + "icons" + File.separatorChar + "icon_not.gif");
        theAppIcon = AppIcon.scaleIcon(theAppIcon);
        logFrame.setIconImage(theAppIcon.getImage());

        logFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent we) {
                System.exit(0);
            }
        });
        // This is so that our own handler can collect and save all changes.
        // Don't worry, the window still closes.
        logFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        //--------------------------------------

        update("Creating the Log Tree");
        appTreePanel = new AppTreePanel(logFrame, appOpts);
        logFrame.setContentPane(appTreePanel);

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
                //getAppTreePanel().preClose();  // Trying this out (8/4/19) - may not need 'getAppTreePanel' in this context.
                appTreePanel.preClose();
                saveOpts();
            } // end run
        });
        Runtime.getRuntime().addShutdownHook(logPreClose);
    } // end main

    private static void saveOpts() {
        String FileName = MemoryBank.userDataHome + File.separatorChar + "app.options";
        MemoryBank.debug("Saving application option data in " + FileName);

        try {
            FileOutputStream fos = new FileOutputStream(FileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(appOpts);
            oos.flush();
            oos.close();
            fos.close();
        } catch (IOException ioe) {
            // This method is only called internally from preClose.  Since
            // preClose is to be called via a shutdown hook that is not going to
            // wait around for the user to 'OK' an error dialog, any error in saving
            // will only be reported in a printout via MemoryBank.debug because
            // otherwise the entire process will hang up waiting for the user's 'OK'
            // on the dialog that will NOT be showing.

            // A normal user will not see the debug error printout but
            // they will most likely see other popups such as filesystem full, access
            // denied, etc, that a sysadmin type can resolve for them, that will
            // also fix this issue.
            String ems = ioe.getMessage();
            ems = ems + "\nMemory Bank options save operation aborted.";
            MemoryBank.debug(ems);
            // This popup caused a hangup and the vm had to be 'kill'ed.
            // JOptionPane.showMessageDialog(null,
            //    ems, "Error", JOptionPane.ERROR_MESSAGE);
            // Yes, even though the parent was null.
        } // end try/catch
    } // end saveOpts


    public static String toJsonString(Object theObject) {
        String theJson = "";
        try {
            theJson = mapper.writeValueAsString(theObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return theJson;
    }

} // end class MemoryBank


