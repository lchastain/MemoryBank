import java.io.Serializable;
import java.util.Date;

class DayNoteData extends IconNoteData implements Serializable {
    private static final long serialVersionUID = -2202469274687602102L;
    private Date timeOfDayDate;

    DayNoteData() {
        super();
    } // end constructor


    // Alternate constructor, for starting
    //   with common base class data.
    DayNoteData(IconNoteData ind) {
        super(ind);
        // In this case, the invoking context is responsible for
        //   making an additional call to setTimeOfDayDate().
    } // end constructor


    // The copy constructor (clone)
    DayNoteData(DayNoteData dnd) {
        super(dnd);
        timeOfDayDate = dnd.timeOfDayDate;
    } // end constructor

    // Construct a DayNoteData from a NoteData.
    // This is used when taking Notes from NoteData interfaces.
    // The result will get a new time and the default icon.
    DayNoteData(NoteData nd) {
        super(nd);
        timeOfDayDate = new Date();
    }


    protected void clear() {
        super.clear();
        timeOfDayDate = null;
    } // end clear


    @Override
    protected NoteData copy( ) {
        return new DayNoteData(this);
    }

    Date getTimeOfDayDate() {
        return timeOfDayDate;
    }

    void setTimeOfDayDate(Date value) {
        timeOfDayDate = value;
    }

} // end class DayNoteData
