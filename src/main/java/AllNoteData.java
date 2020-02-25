// This class is used by the Search mechanism to collect all note types into a single type-vector that
//   can be searched.  It allows AppTreePanel to load and search the data from the various NoteGroups,
//   without having to actually instantiate the full group with all the rest of its overhead such
//   as panel building and all the associated utility methods.  It makes searches much faster.
// This data is not persisted, displayed, or manipulated; just searched and possibly copied
//   into a SearchResultData instance.  IntelliJ may 'think' that it is all unused - not so.

public class AllNoteData extends NoteData {
    // In addition to the fields that we get from a standard NoteData, we also include the
    // persisted data members from EVERY OTHER child class of NoteData.  It is not necessary
    // to @JsonIgnore any of them; if a member is not in the data file it just deals with
    // what is there.  The problem, if there is one, would come from 'seeing' data that
    // it does not recognize, so these all need to be defined.

    // private File fileFoundIn; // Except this one; we don't search the SearchResultData

    // From EventNoteData:  ----------------------------
    private String locationString;
    private String eventStartDateString;
    private String eventStartTimeString;
    private String eventEndDateString;
    private String eventEndTimeString;
    private String recurrenceString;
    private boolean blnRetainNote;
    private Integer durationValue;
    private String durationUnits;
    // From TodoNoteData:  ----------------------------
    private String todoDateString;
    private int intPriority;
    private int intStatus;
    // From DayNoteData:  ----------------------------
    private String timeOfDayString;
    // From IconNoteData:  ----------------------------
    private String iconFileString;
    private boolean showIconOnMonthBoolean;


    // The JSON mapper uses this one during a load; IntelliJ doesn't find a usage.
    public AllNoteData() {
        super();
    } // end default constructor

} // end class AllNoteData
