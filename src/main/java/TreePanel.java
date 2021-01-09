import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public interface TreePanel {

    void setSelectedDate(LocalDate theSelection);

    void setViewedDate(int theYear);

    void setViewedDate(LocalDate theViewedDate, ChronoUnit theGranularity);

    void showDay();

    void showFoundIn(SearchResultData searchResultData);

    void showWeek(LocalDate theWeekToShow);

    void showMonthView();

}
