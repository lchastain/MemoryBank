import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.LocalDate;

//@Disabled("Looking for a rogue 'working' dialog")
class ThreeMonthColumnTest {
    ThreeMonthColumn tmc;

    @BeforeAll
    static void beforeAll() throws IOException {
        MemoryBank.debug = true;
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
    }

    @BeforeEach
    void setUp() {
        tmc = new ThreeMonthColumn();
    }

    @Test
    void testSetChoice() {
        LocalDate ld = LocalDate.now().minusMonths(3).minusDays(3);
        tmc.setChoice(ld);
        LocalDate theChoice = tmc.getChoice();
        Assertions.assertEquals(ld, theChoice);
    }
}