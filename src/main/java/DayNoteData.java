import java.time.ZonedDateTime;
import java.util.Date;

class DayNoteData extends IconNoteData {
    private static final long serialVersionUID = 1L;
    private Date timeOfDayDate;
    private String noteDateTimeString;

    DayNoteData() {
        super();
        timeOfDayDate = new Date();
        noteDateTimeString = ZonedDateTime.now().toString();
    } // end constructor


    // Alternate constructor, for starting
    //   with common base class data.
    DayNoteData(IconNoteData ind) {
        super(ind);
        timeOfDayDate = new Date();
        noteDateTimeString = ZonedDateTime.now().toString();
    } // end constructor


    // The copy constructor (clone)
    DayNoteData(DayNoteData dnd) {
        super(dnd);
        timeOfDayDate = dnd.timeOfDayDate;
        noteDateTimeString = dnd.noteDateTimeString;
    } // end constructor

    // Construct a DayNoteData from a NoteData.
    // This is used when taking Notes from NoteData interfaces.
    // The result will get a new time and the default icon.
    DayNoteData(NoteData nd) {
        super(nd);
        timeOfDayDate = new Date();
        noteDateTimeString = ZonedDateTime.now().toString();
    }


    protected void clear() {
        super.clear();
        timeOfDayDate = null;
        noteDateTimeString = null;
    } // end clear


    @Override
    protected NoteData copy( ) {
        return new DayNoteData(this);
    }

    Date getTimeOfDayDate() {
        return timeOfDayDate;
    }

    ZonedDateTime getNoteDateTime() {
        return ZonedDateTime.parse(noteDateTimeString);
    }

    void setTimeOfDayDate(Date value) {
        timeOfDayDate = value;
    }

    void setNoteDateTime(ZonedDateTime zdt) {
        if(zdt == null) {
            noteDateTimeString = null;
        } else {
            noteDateTimeString = zdt.toString();
        }
    }

} // end class DayNoteData
