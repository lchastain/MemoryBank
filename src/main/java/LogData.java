import java.time.LocalDate;

public class LogData extends NoteData {

    // We keep the Date as a String for more reliable persistence, because serialization of a LocalDate is problematic.
    // Then, we want to keep the date String private, without a direct setter or getter, because contexts that use
    //   this class often need to see the Date in various different formats but we don't want a non-parseable String
    //   coming back to our 'set' method, so we only allow the getter and setter to deal with a LocalDate class.
    // It will be up to the calling contexts to set only with a LocalDate, and they can format the Date that
    //   they receive from the getter however they need (not forgetting about the default '.toString' method, which
    //   is what is used here when creating the String that we ultimately persist (and is therefore guaranteed to be
    //   parseable when persisted data is reloaded).
    private String logDateString; // Defaults to 'today' but can be set/chosen later.

    // A 'time' member has been considered but not adopted, because Log entries will typically be tied to unique
    //   dates.  More than one per date is of course allowed, and the user may deal with this if needed by adding
    //   a time notation to their data either in the main or extended note, or they may just handle it with proper
    //   manual sorting, but for simplicity and uniformity, the time will not be handled as a discrete data item.


    public LogData() {
        logDateString = LocalDate.now().toString();
    } // end constructor


    // The copy constructor (clone)
    public LogData(LogData lndCopy) {
        super(lndCopy);
        logDateString = lndCopy.logDateString;
    } // end constructor

    // Construct a LogData from a NoteData.
    // This is used when pasting Notes that were copied from NoteData interfaces.
    public LogData(NoteData nd) {
        super(nd);
        logDateString = LocalDate.now().toString();
    } // end constructor

    @Override
    protected void clear() {
        super.clear();
        logDateString = null;
    } // end clear


    // This is provided as an alternative to calling a specific copy constructor, for when the exact type of NoteData
    // in the calling context was not known but the inheritance hierarchy has decided that it was a LogData.
    @Override
    protected NoteData copy( ) {
        return new LogData(this);
    }



    LocalDate getLogDate() {
        if(logDateString == null) return null;
        return LocalDate.parse(logDateString);
    }


    void setLogDate(LocalDate value) {
        if(value == null) {
            logDateString = null;
        } else {
            logDateString = value.toString();
        }
        touchLastMod();
    }
} // end class TodoNoteData
