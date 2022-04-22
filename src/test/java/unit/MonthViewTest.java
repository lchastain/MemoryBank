import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class MonthViewTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    // Just runs the constructors, for coverage.
    void runit() {
        MonthView mv = new MonthView(LocalDate.now());
    }
}