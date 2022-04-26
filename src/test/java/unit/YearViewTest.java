import org.junit.jupiter.api.Test;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

class YearViewTest {

    @Test
    void testMouseAdapter() throws InterruptedException {
        YearView yearView = new YearView();

        long mouseWhen  = LocalDateTime.now().toEpochSecond(OffsetDateTime.now().getOffset());
        int mouseX = 16;
        int mouseY = 10;

        // Get a handle to the mouse listener that will handle the Mouse events on the 'prev' button.
        // (We only want the second one, accessed at index 1).
        MouseListener[] mouseListeners = yearView.prev.getMouseListeners();
        MouseListener mouseListener = mouseListeners[1];

        MouseEvent prevExited = new MouseEvent(yearView.prev, MouseEvent.MOUSE_EXITED,
                mouseWhen, 0, mouseX, mouseY, 0, false);
        mouseListener.mouseExited(prevExited);

        MouseEvent prevPressed = new MouseEvent(yearView.prev, MouseEvent.MOUSE_PRESSED,
                mouseWhen, 0, mouseX, mouseY, 0, false);
        mouseListener.mousePressed(prevPressed);
        Thread.sleep(500); // Let it stay depressed for a bit.

        MouseEvent prevReleased = new MouseEvent(yearView.prev, MouseEvent.MOUSE_RELEASED,
                mouseWhen, 0, mouseX, mouseY, 0, false);
        mouseListener.mouseReleased(prevReleased);
    }


    @Test
    // Just runs the constructors, for coverage.
    void runit() {
        MemoryBank.debug = true;
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
        YearView yv = new YearView();
    }


}