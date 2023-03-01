import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class MonthViewTest {

    @Test
    // Just runs the constructors, for coverage.
    void runit() {
        MemoryBank.debug = true;
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
        new MonthView(LocalDate.now());
    }
}