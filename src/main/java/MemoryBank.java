import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// One important setting in IntelliJ:
// File / Settings / Editor / File Encodings - set all to UTF-8.

public class MemoryBank {
    static Color amColor;
    static Color pmColor;
    static DateTimeFormatter dtf;
    static boolean debug;
    static boolean event;
    static boolean init;
    static String userEmail;
    static AppOptions appOpts;     // saved/loaded
    static NoteData clipboardNote;
    static JFrame logFrame;
    static Notifier optionPane;
    static SubSystem system;
    static DataAccessor dataAccessor;

    static AppTreePanel appTreePanel;
    private static boolean timing;
    private static AppSplash splash;
    private static boolean logApplicationShowing;
    private static final int[] percs = {20, 25, 45, 50, 60, 90, 100};
    private static int updateNum = 0;
    static String appIconName;
    static String appIconFormat;
    static DataAccessor.AccessType dataAccessorType;

    static {
        // These can be 'defined' in the startup command.  Ex:
        //   java -Ddebug MemoryBank
        debug = (System.getProperty("debug") != null);
        event = (System.getProperty("event") != null);
        init = (System.getProperty("init") != null);
        timing = (System.getProperty("timing") != null);

        // Interface instances with default methods
        optionPane = new Notifier() { };
        system = new SubSystem() { };

        if (debug) System.out.println("Debugging printouts on.");
        if (event) System.out.println("Event tracing printouts on.");
        if (init) System.out.println("Initialization trace printouts on.");
        if (timing) System.out.println("Timing printouts on.");

        // No longer needed?
//        setProgramDataLocation();  // mbHome is set here.

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
    } // end static


    // Description:  Prints the class name of the calling context and
    //               specifies whether called from the static section
    //               or the constructor.
    //
    // Recommended Usage: Call this method as the last line of a
    //                    constructor or static section.  Gives an
    //                    indication that the relevant section has
    //                    executed, and also can help in performance
    //                    tuning.
    public static void trace() {
        if (!init) return;

        String methodName = "null";
        String className = "null";

        StackTraceElement[] theTrace = Thread.currentThread().getStackTrace();
        for(int count = 1; count < theTrace.length; count++) {
            StackTraceElement ste = theTrace[count];
            if(ste.toString().contains("init>")) {
                methodName = ste.getMethodName();
                className = ste.getClassName();
                break;
            }
        }
        if (methodName.equals("<init>")) {
            System.out.println(className + " constructor  " + LocalTime.now().toString());
        } else if (methodName.equals("<clinit>")) {
            System.out.println(className + " static section  " + LocalTime.now().toString());
        } else {
            System.out.println("Improper use of constructor trace method!");
            System.out.println(Thread.currentThread().getStackTrace()[2].toString());
        } // end if
    } // end trace


    // Description:  Prints the input parameter, without a linefeed.
    public static void dbg(String s) {
        if (debug) System.out.print(s);
    } // end dbg


    // Description:  Prints the input parameter.
    //   Does a 'flush' so that statements are not printed out
    //   of order, when mixed with exceptions.
    public static void debug(String s) {
        if (debug) {
            System.out.println(s);
            System.out.flush();
        } // end if
    } // end debug


    // Description:  Prints the calling context
    public static void event() {
        if (!MemoryBank.event) return;
        System.out.println(Thread.currentThread().getStackTrace()[3].toString());
    } // end event


    public static void main(String[] args) {
        // Set the primary user identifier.  We use the user's email address but this could be interpreted by
        // the DataAccessor implementation as a DB name, or a filesystem Directory, or something else as long as it
        // uniquely identifies the data set for the given user email.
        userEmail = "default.user@elseware.com";

        // Hold our place in line, on the taskbar.
        logFrame = new JFrame("Memory Bank:");
        logFrame.setLocation(-1000, -1000);  // Offscreen; not ready to be seen, yet.
        logFrame.setVisible(true);

        //---------------------------------------------------------------
        // Splash Screen
        //---------------------------------------------------------------
        ImageIcon myImage = new IconInfo(DataArea.IMAGES, "about", "gif").getImageIcon();
        splash = new AppSplash(myImage);
        splash.setVisible(true);
        logApplicationShowing = false;
        // end run
        new Thread(() -> {
            try {
                while (!logApplicationShowing) {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                } // end while
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
            }
            splash.setVisible(false);
            splash = null;
        }).start();

        // Tool Tip Adjustments
        ToolTipManager.sharedInstance().setInitialDelay(1500); // Wait just a bit longer to show the first tooltip.
        ToolTipManager.sharedInstance().setDismissDelay(90000); // Keep tooltip up for a long time.

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
            } else if (startupFlag.equals("-trace")) {
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

        // Set the type of Data Accessor that this app will use.
        // The value can eventually come from a configuration setting; the source of the configuration values
        //   does not dictate how the rest of the app must operate from that point on.
        // But a configuration 'file' feels like a more preferred option, to
        // allow easier access and alteration by support personnel (once we get support personnel).
        dataAccessorType = DataAccessor.AccessType.FILE;

        // The Data Accessors must have access to the user's data,
        // so this setting should be made AFTER the userEmail is set.
        dataAccessor = DataAccessor.getDataAccessor(dataAccessorType);

        appOpts = dataAccessor.getAppOptions(); // Load the user settings - if available, will override defaults.
        if(appOpts == null) appOpts = new AppOptions(); // In case of an uloadable file; not the same as not present.
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
        ImageIcon theAppIcon = new IconInfo(DataArea.APP_ICONS, appIconName, appIconFormat).getImageIcon();
        IconInfo.scaleIcon(theAppIcon);
        logFrame.setIconImage(theAppIcon.getImage());

        logFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                system.exit(0);
            }
        });
        // This is so that our own handler (the logPreClose Thread, below) can collect and save all changes.
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
        logFrame.setSize(880, 620);  // explicit

        logFrame.setLocationRelativeTo(null); // Center
        logFrame.setVisible(true);
        update("The MemoryBank Application is ready");
        logApplicationShowing = true;

        //---------------------------------------------------------------------
        // Set up a shutdown hook, to save all data before exit,
        //   whether or not it was a planned exit.
        //---------------------------------------------------------------------
        // end run
        Thread logPreClose = new Thread(() -> {
            appTreePanel.preClose(); // Preserve all changes across all open Panels.
            AppOptions.saveOpts();
        });
        Runtime.getRuntime().addShutdownHook(logPreClose);
    } // end main

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



} // end class MemoryBank


