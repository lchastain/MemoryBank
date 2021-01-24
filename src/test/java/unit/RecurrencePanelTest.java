import org.junit.jupiter.api.*;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Method;
import java.time.LocalDate;


// RecurrencePanel methods are mostly private, called internally by the
// event handlers for the interface components.  To test them we could
// either relax the access from private to package-private, OR - as we
// do here - drive the panel via generated events.  But when testing is
// done via events it may be a lot less clear as to what is being tested.
// So - test names and comments will need to spell it out.

class RecurrencePanelTest {
    private static RecurrencePanel theRecurrencePanel;
    private static TestUtil testUtil;

    @BeforeAll
    static void meFirst() {
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

        theRecurrencePanel = new RecurrencePanel();
        testUtil = new TestUtil();

        MemoryBank.debug = true;
    }


    // Call this to see results of test setups, during test dev.  Not used in final tests.
    void showDialog() {
        JOptionPane.showConfirmDialog(
                null,            // for modality
                theRecurrencePanel,              // UI Object
                "Set a repetition schedule for this Event", // pane title bar
                JOptionPane.OK_CANCEL_OPTION, // Option type
                JOptionPane.PLAIN_MESSAGE,    // Message type
                null);                       // icon
    }

    // No Recurrence
    @Test
    void testDoNotRepeat() {
        theRecurrencePanel.showTheData("", LocalDate.now());
        //showDialog();
        String theSetting = theRecurrencePanel.getRecurrenceSetting();
        String theSummary = EventNoteData.getRecurrenceSummary(theSetting);
        Assertions.assertEquals("None", theSummary);
    }

    // Intellij reports 5 'class' methods for RecurrencePanel; 4 are shown in the Structure view for the class
    // but one is 'hiding' - it is known as a 'Synthetic' class, anonymously created by a switch statement in
    // the recalcEndByWeek method that makes a call to a date-related class that might throw an exception.  The
    // recalcEndByWeek method is tested indirectly by the method below, although it does not actually make it
    // down to the point of that switch statement.
    @Test
    void testButtonStopByMouseAdapter() throws NoSuchMethodException {
        MouseEvent me;

        // Trojan the RecurrencePanel's Notifier with the one for Tests.
        RecurrencePanel.optionPane = testUtil;

        // Configure our trojan'd Notifier to set a date on the YearView date chooser,
        // that will then be queried back out by the production code.
        Method newMethod = YearView.class.getDeclaredMethod("setChoice", LocalDate.class);
        testUtil.setTheMethod(newMethod);
        testUtil.setTheMessage(LocalDate.of(2205, 10, 20)); // Provide the needed method parameter

        // Construct a MouseEvent to emulate a mouse press on the 'Stop By' button.
        me = new MouseEvent(theRecurrencePanel.btnStopBy, MouseEvent.MOUSE_PRESSED, 0, 0, 18, 23, 0, false);

        // Get a handle to the mouse listener that will handle the Mouse events.
        // (we get them all this way, but we only want the second one, accessed at index 1).
        MouseListener[] mouseListeners = theRecurrencePanel.btnStopBy.getMouseListeners();

        mouseListeners[1].mousePressed(me);
        me = new MouseEvent(theRecurrencePanel.btnStopBy, MouseEvent.MOUSE_CLICKED, 0, 0, 18, 23, 0, false);
        mouseListeners[1].mouseClicked(me);
    }

    // Repeats every five days, goes indefinitely
    @Test
    void testRepeatDayInterval() {
        theRecurrencePanel.showTheData("D5_", LocalDate.of(2020, 2, 4));
        //showDialog();
        String theSetting = theRecurrencePanel.getRecurrenceSetting();
        String theSummary = EventNoteData.getRecurrenceSummary(theSetting);
        Assertions.assertEquals("Repeats at 5 day intervals", theSummary);
    }

    // Repeats every third Tuesday, goes indefinitely
    @Test
    void testRepeatWeekInterval() {
        theRecurrencePanel.showTheData("W3_", LocalDate.of(2020, 2, 4));
        //showDialog();
        String theSetting = theRecurrencePanel.getRecurrenceSetting();
        String theSummary = EventNoteData.getRecurrenceSummary(theSetting);
        Assertions.assertEquals("Repeats on Tuesdays at 3 week intervals", theSummary);
    }

    @Test
    void testRepeatMonthInterval() {
        theRecurrencePanel.showTheData("M3_the 4th_", LocalDate.of(2020, 2, 4));
        //showDialog();
        String theSetting = theRecurrencePanel.getRecurrenceSetting();
        String theSummary = EventNoteData.getRecurrenceSummary(theSetting);
        Assertions.assertEquals("Repeats at 3 month intervals on the 4th", theSummary);
    }

    @Test
    void testRepeatYearInterval() {
        theRecurrencePanel.showTheData("Y_the 14th of February_", LocalDate.of(2020, 2, 14));

        // Every test that makes a call to 'showTheData' is expected to have a good / valid recurrence string, so
        // a subsequent call to 'isRecurrenceValid' is not needed; after all, the setting was not made by a user.
        // The call below was used here and in other tests as a pre-req validator during test dev, and then disabled
        // after the input
        // was proven to be good.  In many cases where it initially wasn't, the validator threw exceptions rather than
        // returning a false.  In production, the validator will not have those test-only situation problems.
        //Assertions.assertTrue(theRecurrencePanel.isRecurrenceValid());
        //showDialog();

        String theSetting = theRecurrencePanel.getRecurrenceSetting();
        String theSummary = EventNoteData.getRecurrenceSummary(theSetting);
        Assertions.assertEquals("Repeats on the 14th of February of every year", theSummary);
    }

}