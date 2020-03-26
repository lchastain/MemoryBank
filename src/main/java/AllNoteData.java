// This class is used by the Search mechanism to collect all note types into a single type-vector that
//   can be searched.  It allows AppTreePanel to load and search the data from the various NoteGroups,
//   without having to actually instantiate the full group with all the rest of its overhead such
//   as panel building and all the associated utility methods.  It makes searches much faster.
// This data is not persisted, displayed, or manipulated; just searched and possibly copied
//   into a SearchResultData instance.  IntelliJ may 'think' that it is all unused - not so.

import java.io.File;

// This class is used in data loading, where the data coming in could be any generation of NoteData but
// if extraneous data members are not accounted for then the load and type conversions would fail.  So
// the solution is to have this class, which encompasses all possible data members that might be in the
// data.  It is not used except in data searches and LinkData storage and retrieval.

public class AllNoteData extends NoteData {
    // In addition to the fields that we get from a standard NoteData, we also include the
    // persisted data members from EVERY OTHER child class of NoteData.  It is not necessary
    // to @JsonIgnore any of them; if a member is not in the data file it just deals with
    // what is there.  The problem, if there is one, would come from 'seeing' data that
    // it does not recognize, so these all need to be defined.

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
    // From SearchResultData:  ------------------------ (not used in searches, but is used in DataFixes)
    private File fileFoundIn;


    // The JSON mapper uses this one during a load; IntelliJ doesn't find a usage.
    public AllNoteData() {
        super();
    } // end default constructor

    // This constructor allows for a kind of 'sidecast' - taking in a different child of the
    // base NoteData class and returning an instance of this class that is a sibling of the
    // input parameter instance.  Of course if it is just the base class coming in and not a
    // child, then this would be called a 'downcast'.  Either way, that's not something you
    // get automatically, so this constructor is needed.
    // Of note is the setting of the instanceId, below - we do not intend that this instance
    // should have its own unique value, because the goal here is to have an AllNoteData-flavored
    // version of the input parameter, so it gets the same Id as the input so that the original
    // note can be referenced when needed.
    public AllNoteData(NoteData noteData) {
        this();
        this.instanceId = noteData.instanceId;
        this.zdtLastModString = noteData.zdtLastModString;

        this.extendedNoteString = noteData.extendedNoteString;
        this.noteString = noteData.noteString;
        this.subjectString = noteData.subjectString;
    }

} // end class AllNoteData
