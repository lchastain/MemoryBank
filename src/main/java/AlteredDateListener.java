import java.time.LocalDate;

//Interface for operations that should occur when a date has changed.
interface AlteredDateListener {
    void dateChanged(LocalDate fromDate, LocalDate theNewDate);
} // end DateSelection
