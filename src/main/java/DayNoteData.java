import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalTime;

class DayNoteData extends IconNoteData {
    private static final long serialVersionUID = 1L;

    private String timeOfDayString;

    @JsonIgnore
    private String noteDateString;  // Need datafix to remove, before we can remove from here.

    DayNoteData() {
        super();
        timeOfDayString = LocalTime.now().toString();
    } // end constructor


    // Alternate constructor, for starting
    //   with common base class data.
    DayNoteData(IconNoteData ind) {
        super(ind);
        timeOfDayString = LocalTime.now().toString();
    } // end constructor


    // The copy constructor (clone)
    DayNoteData(DayNoteData dnd) {
        super(dnd);
        timeOfDayString = dnd.timeOfDayString;
    } // end constructor

    // Construct a DayNoteData from a NoteData.
    // This is used when taking in Notes from other NoteData-type interfaces.
    // The result will get a new time and the default icon.
    DayNoteData(NoteData nd) {
        super(nd);
        timeOfDayString = LocalTime.now().toString();
    }

    //=============================================================

    protected void clear() {
        super.clear();
        timeOfDayString = null;
    } // end clear


    @Override
    protected NoteData copy() {
        return new DayNoteData(this);
    }

    String getTimeOfDayString() {
        return timeOfDayString;
    }

    void setTimeOfDayString(String value) {
        timeOfDayString = value;
    }

} // end class DayNoteData
