import org.junit.jupiter.api.*;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.time.LocalDate;

class DayNoteGroupPanelTest {
    private DayNoteGroupPanel dng;

    @BeforeAll
    static void ssetup() {
        MemoryBank.debug = true;
    }

    @BeforeEach
    void setUp() {
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
        dng = new DayNoteGroupPanel();
    }

    @AfterEach
    void tearDown() {
        dng = null;
    }

    @Test
    void testSetChoice() {
        dng.setDate(LocalDate.now());
    }

    @Test
    void testDefaultIcon() {
        ImageIcon ai = dng.getDefaultIcon();
        Assertions.assertNotNull(ai);
        // We're just doing coverage here, for now.
        dng.setDefaultIcon(ai);
    }


    @Test
    void testGetNoteComponent() {
        // Currently all DayNoteGroups have PAGE_SIZE components.
        // But using all of them is quite uncommon, so we
        // expect that the last one will not be visible,
        // regardless of the day setting.
        DayNoteComponent dnc = dng.getNoteComponent(NoteGroupPanel.PAGE_SIZE-1); // zero-indexed
        Assertions.assertNotNull(dnc);
        Assertions.assertFalse(dnc.initialized);
    }

    @Test
    void testMouseListener() {
        // We're just doing coverage here, for now.
        LabelButton jb = new LabelButton("+");
        MouseEvent me = new MouseEvent(jb, 0, 0, 0, 100, 100, 1, false);
        dng.mouseEntered(me);
        dng.mouseClicked(me);
        jb.setText("-");
        dng.mouseEntered(me);
        dng.mouseClicked(me);
        jb.setText("12");
        dng.mouseEntered(me);
        dng.mouseClicked(me);
        jb.setText("24");
        dng.mouseEntered(me);
        dng.mouseClicked(me);
        dng.mouseExited(me);
        dng.mousePressed(me);
        dng.mouseReleased(me);
    }


    @Test
    void testShifting() {
        // We're just doing coverage here, for now.
        dng.shiftUp(1);
        dng.shiftDown(0);
    }
}