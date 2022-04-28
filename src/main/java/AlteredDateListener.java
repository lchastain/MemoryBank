import java.time.LocalDate;

// Interface for operations that should occur when a date has changed.
// The primary implementer of this interface is the AppTreePanel, that uses it to keep date-related
//   Panels in sync.  But sometimes all that is needed is a simple notification that there has been
//   a change, with minimal processing after that.  So this interface gives the implementers a
//   more efficient way to separate out their intentions, as opposed to coding all the variants into
//   a single method in AppTreePanel.
interface AlteredDateListener {
    void dateChanged(DateRelatedDisplayType whoChangedIt, LocalDate theNewDate);
} // end DateSelection
