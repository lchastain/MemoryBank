import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

class TimeChooserTest {
    private static TimeChooser timeChooser;
    JDialog dialogWindow;

    @BeforeAll
    static void meFirst() {
        timeChooser = new TimeChooser();
    }

    void showTimeChooserDialog() {
        // Make a dialog window.
        dialogWindow = new JDialog((Frame) null, true);

        dialogWindow.getContentPane().add(timeChooser, BorderLayout.CENTER);
        dialogWindow.setTitle("TimeChooser Test");
        dialogWindow.setSize(240, 130);
        dialogWindow.setResizable(false);

        // Center the dialog.
        dialogWindow.setLocationRelativeTo(null);

        // Go modal -
        dialogWindow.setVisible(true);
    } // end showTimeChooserDialog

    // Tests ------------------------------------------

    @Test
    void testActionPerformed() {
        ActionEvent actionEvent;

        actionEvent = new ActionEvent(timeChooser.ampmButton, ActionEvent.ACTION_PERFORMED, "AM");
        timeChooser.actionPerformed(actionEvent);

        actionEvent = new ActionEvent(timeChooser.nowButton, ActionEvent.ACTION_PERFORMED, "Now");
        timeChooser.actionPerformed(actionEvent);

        actionEvent = new ActionEvent(timeChooser.resetButton, ActionEvent.ACTION_PERFORMED, "Reset");
        timeChooser.actionPerformed(actionEvent);

        actionEvent = new ActionEvent(timeChooser.clearButton, ActionEvent.ACTION_PERFORMED, " ");
        timeChooser.actionPerformed(actionEvent);
    }




    @Test
    void testGetChoice() {
        timeChooser.getChoice();
    }

    @Test
    void testMouseActions() throws InterruptedException {
        timeChooser = new TimeChooser(); // needed to redo construction, else shows empty.

        new Thread(this::showTimeChooserDialog).start(); // Show the dialog in a thread.
        Thread.sleep(1500); // Time to bring up the chooser.

        long mouseWhen  = LocalDateTime.now().toEpochSecond(OffsetDateTime.now().getOffset());
        int mouseX = 16;
        int mouseY = 10;

        MouseEvent hb2Entered = new MouseEvent(TimeChooser.minutesButton, MouseEvent.MOUSE_ENTERED,
                mouseWhen, 0, mouseX, mouseY, 0, false);
        TimeChooser.minutesButton.mouseEntered(hb2Entered);

        MouseEvent hb2Pressed = new MouseEvent(TimeChooser.minutesButton, MouseEvent.MOUSE_PRESSED,
                mouseWhen, 0, mouseX, mouseY, 0, false);
        TimeChooser.minutesButton.mousePressed(hb2Pressed);
        Thread.sleep(500); // Let it stay depressed for a bit.

        MouseEvent hb2Released = new MouseEvent(TimeChooser.minutesButton, MouseEvent.MOUSE_RELEASED,
                mouseWhen, 0, mouseX, mouseY, 0, false);
        TimeChooser.minutesButton.mouseReleased(hb2Released);

        MouseEvent hb2Clicked = new MouseEvent(TimeChooser.minutesButton, MouseEvent.MOUSE_CLICKED,
                mouseWhen, 0, mouseX, mouseY, 0, false);
        TimeChooser.minutesButton.mouseClicked(hb2Clicked);

        MouseEvent hb2Exited = new MouseEvent(TimeChooser.minutesButton, MouseEvent.MOUSE_EXITED,
                mouseWhen, 0, mouseX, mouseY, 0, false);
        TimeChooser.minutesButton.mouseExited(hb2Exited);

        WindowEvent we = new WindowEvent(dialogWindow, WindowEvent.WINDOW_CLOSING);
        dialogWindow.dispatchEvent(we);
    }


    @Test
    void testSetShowSeconds() {
        timeChooser.setShowSeconds();
    }

}