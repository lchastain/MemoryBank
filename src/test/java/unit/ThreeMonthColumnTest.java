import org.junit.jupiter.api.*;

import java.time.LocalDate;

//@Disabled("Looking for a rogue 'working' dialog")
class ThreeMonthColumnTest {
    ThreeMonthColumn tmc;

    @BeforeEach
    void setUp() {
        tmc = new ThreeMonthColumn();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testSetChoice() {
        LocalDate ld = LocalDate.now().minusMonths(3).minusDays(3);
        tmc.setChoice(ld);
        LocalDate theChoice = tmc.getChoice();
        Assertions.assertEquals(ld, theChoice);
    }
}