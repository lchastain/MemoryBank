import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

//Interface for operations that should occur when a date has changed.
interface AlteredDateListener {
    void dateDecremented(LocalDate theNewDate, ChronoUnit theGranularity);
    void dateIncremented(LocalDate theNewDate, ChronoUnit theGranularity);
} // end DateSelection
