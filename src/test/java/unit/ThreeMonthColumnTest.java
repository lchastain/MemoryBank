import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

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