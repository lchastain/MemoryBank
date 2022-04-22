import java.time.LocalDate;

public interface TreePanel {

//    void setSelectedDate(LocalDate theSelection);
    LocalDate getViewedDate();

    void setViewedDate(int theYear);

    void setViewedDate(LocalDate theViewedDate);

    void showDay();

    void showFoundIn(SearchResultData searchResultData);

    void showWeek(LocalDate theWeekToShow);

    void showMonthView();

}
