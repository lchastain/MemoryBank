import org.junit.jupiter.api.*;

import java.time.ZonedDateTime;

class DateFormatPanelTest {
    private static DateFormatPanel dateFormatPanel;

    @BeforeAll
    static void meFirst() {
        dateFormatPanel = new DateFormatPanel();
        dateFormatPanel.setup(ZonedDateTime.now().toEpochSecond(), "yyyy MM dd");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testGetDate() {
        long theLong = dateFormatPanel.getDate();
        Assertions.assertTrue(theLong > 0);
    }

    @Test
    void testGetFormat() {
        String theString = dateFormatPanel.getFormat();
        Assertions.assertNotNull(theString);
    }

    @Test
    void testSetCheckBoxes() {
    }

    @Test
    void testSetup() {
    }
}