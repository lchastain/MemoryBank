import java.time.LocalDate;

// Various classes throughout the app will need to call methods that reside in an implementer of this interface.
// The interface is needed in order to easily switch between those implementers while having no duplication of
//   code in the classes that make the calls.  Implementers:  ArchiveTreePanel, AppTreePanel
// The classes that make these calls will all have a 'setTreePanel' method whereby the context that constructs them
//   can set the proper implementer.
public interface TreePanel {

    LocalDate getViewedDate();

    void showDay();

    void showFoundIn(SearchResultData searchResultData);

    void showWeek(LocalDate theWeekToShow);

    void showMonthView();

}
