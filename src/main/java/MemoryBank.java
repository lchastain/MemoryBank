import org.apache.commons.io.FileUtils;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class MemoryBank {
    static Color amColor;
    static Color pmColor;
    static boolean archive;
    static boolean military; // Set by DayNoteGroup.  Preserved in appOpts.
    public static DateTimeFormatter dtf;
    public static boolean debug;
    public static boolean event;
    public static boolean init;
    private static boolean timing;
    public static String userDataHome; // User data top-level directory 'mbankData'
    public static String logHome;  // For finding icons & images
    static AppOptions appOpts;     // saved/loaded
    private static AppTreePanel appTreePanel;
    static NoteData clipboardNote;
    private static JFrame logFrame;
    private static AppSplash splash;
    private static boolean logApplicationShowing;
    private static int[] percs = {20, 25, 45, 50, 60, 90, 100};
    private static int updateNum = 0;
    private static String appIconFileName;

    static {
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

        setProgramDataLocation();

        appOpts = new AppOptions(); // Start with default values.

        // Set the Look and Feel
        try {
            String thePlaf = "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel";
            debug("Setting plaf to: " + thePlaf);
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

    } // end static


    static AppTreePanel getAppTreePanel() {
        return appTreePanel;
    }

    static void loadOpts() {
        Exception e = null;
        String filename = MemoryBank.userDataHome + File.separatorChar + "appOpts.json";

        try {
            String text = FileUtils.readFileToString(new File(filename), StandardCharsets.UTF_8.name());
            appOpts = AppUtil.mapper.readValue(text, AppOptions.class);
            System.out.println("appOpts from JSON file: " + AppUtil.toJsonString(appOpts));
            military = appOpts.military; // This one is accessed by other classes.
        } catch (FileNotFoundException fnfe) {
            // not a problem; use defaults.
            debug("User tree options not found; using defaults");
        } catch (IOException ioe) {
            e = ioe;
            e.printStackTrace();
        }

        if (e != null) {
            String ems = "Error in loading " + filename + " !\n";
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
    // Recommended Usage: Call this method as the last line of a
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
            System.out.println(cname + " constructor  " + LocalDate.now().toString());
        } else if (mname.equals("<clinit>")) {
            System.out.println(cname + " static section  " + LocalDate.now().toString());
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
    private static void errorOut(String s) {
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


    private static String minuteToString(int minutes) {
        if (minutes < 10) return "0" + minutes;
        else return String.valueOf(minutes);
    } // end minuteToString


    private static String hourToString(int hour) {
        String s;
        if (military) {
            if (hour < 10) s = "0" + hour;
            else s = String.valueOf(hour);
        } else {
            if (hour > 12) s = String.valueOf(hour - 12);
            else s = String.valueOf(hour);
            if (hour == 0) s = "12";
            if (s.length() == 1) s = " " + s;
        } // end if
        return s;
    } // end hourToString


    static String makeTimeString(LocalTime lt) {
        int minute = lt.getMinute();

        // Hour portion
        String time = hourToString(lt.getHour());

        if (!military) time += ":";

        // Minutes portion
        time += minuteToString(minute);

        return time;
    } // end makeTimeString


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


    // -----------------------------------------------------------------------
    // Method Name: setProgramDataLocation
    //
    // Set the filesystem location for program data - 'logHome'.
    // Look first in the current directory.  This allows the program data to come
    // from a development location.  If not found then set it explicitly to the default
    // location but test that it is valid, by checking for the 'icons' subdirectory.
    // -----------------------------------------------------------------------
    private static void setProgramDataLocation() {
        String currentDir = System.getProperty("user.dir");
        debug("The current working directory is: " + currentDir);

        // Program data - icons, images, etc, the same for every user.
        File f = new File("icons"); // Look first in current dir.
        if (f.exists()) {  // This logic is far from infallible...
            logHome = currentDir;
            debug("MemoryBank Home = " + logHome);
        } else {
            // Explicitly setting logHome for now.
            logHome = "C:\\Program Files\\Memory Bank"; // need to use System calls here, vs hard-coded C:\
            debug("EXPLICIT MemoryBank Home = " + logHome);

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
        debug("The current working directory is: " + currentDir);
        File f = new File("appData"); // Look first in current dir.
        String loc;
        if (f.exists()) {
            loc = currentDir + File.separatorChar + "appData" + File.separatorChar + userEmail;
            appIconFileName = "notepad.gif";
        } else {
            String userHome = System.getProperty("user.home"); // Home directory.
            loc = userHome + File.separatorChar + "mbankData" + File.separatorChar + userEmail;
            appIconFileName = "icon_not.gif";
        }
        debug("Setting user data location to: " + loc);

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
    private static void timing(String s) {
        if (timing) System.out.println(LocalDate.now() + "  " + s);
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
        splash.setVisible(true);
        logApplicationShowing = false;
        new Thread(new Runnable() {
            public void run() {
                try {
                    while (!logApplicationShowing) {
                        Thread.sleep(1000);
                    } // end while
                } catch (Exception e) {
                    System.out.println("Exception: " + e.getMessage());
                }
                splash.setVisible(false);
                splash = null;
            } // end run
        }).start();

        //---------------------------------------------------------------
        // Evaluate input parameters, if any.
        //---------------------------------------------------------------
        update("Evaluating parameters");
        if (args.length > 0)
            System.out.println("Number of args: " + args.length);

        for (String startupFlag : args) { // Cycling thru them this way, position is irrelevant.

            if (startupFlag.equals("-debug")) {
                if (!debug) System.out.println("Debugging printouts on.");
                debug = true;
            } else if (startupFlag.equals("-event")) {
                if (!event) System.out.println("Event tracing printouts on.");
                event = true;
            } else if (startupFlag.equals("-init")) {
                if (!init) System.out.println("Initialization trace printouts on.");
                init = true;  // Constructors and static sections.
            } else if (startupFlag.equals("-timing")) {
                if (!timing) System.out.println("Timing printouts on.");
                timing = true;
            } else if (startupFlag.indexOf('@') > 0) {
                userEmail = startupFlag;
            } else {
                System.out.println("Parameter not handled: [" + startupFlag + "]");
            } // end if/else
        } // end for i

        setUserDataHome(userEmail);


        loadOpts();  // Load the user settings - will not exist for new user but if available, can override defaults.
        // New attributes to store and retrieve (future work):
        // size of main frame         - only if it makes sense after the future sizing work is done.
        // location of main frame     - only if it makes sense after the future sizing work is done.
        // user's preferred Log name (vs the system's user name) - needs a dialog to take in the new string.

        //--------------------------------------
        // Specify logFrame attributes
        //--------------------------------------
        update("Setting Window variables");
        logFrame.setTitle("Memory Bank for: " + userEmail);
        logFrame.getRootPane().setOpaque(false);

        // Use our own icon -
        AppIcon theAppIcon = new AppIcon(logHome + File.separatorChar + "icons" + File.separatorChar + appIconFileName);
        AppIcon.scaleIcon(theAppIcon);
        logFrame.setIconImage(theAppIcon.getImage());

        logFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
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
        Dimension d = logFrame.getSize();
        System.out.println("Default Frame Size: " + d.toString());
        logFrame.setSize(880, 600);  // explicit

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
                saveOpts(); // temp
            } // end run
        });
        Runtime.getRuntime().addShutdownHook(logPreClose);
    } // end main

    static void saveOpts() {
        String filename = MemoryBank.userDataHome + File.separatorChar + "appOpts.json";
        MemoryBank.debug("Saving application option data in " + filename);

        appOpts.military = military; // Capture the latest setting for this prior to saving.

        try (FileWriter writer = new FileWriter(filename);
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(AppUtil.toJsonString(appOpts));
            bw.flush();
        } catch (IOException ioe) {
            // Since saveOpts is to be called via a shutdown hook that is not going to
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


} // end class MemoryBank


