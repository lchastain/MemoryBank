import java.io.Serializable;
import java.util.Date;

class DayNoteData extends IconNoteData implements Serializable {
    private static final long serialVersionUID = -2202469274687602102L;
    protected Date timeOfDayDate;

    public DayNoteData() {
        super();
    } // end constructor


    // Alternate constructor, for starting
    //   with common base class data.
    public DayNoteData(IconNoteData ind) {
        super(ind);
        // In this case, the invoking context is responsible for
        //   making an additional call to setTimeOfDayDate().
    } // end constructor


    // The copy constructor (clone)
    public DayNoteData(DayNoteData dnd) {
        super(dnd);

        timeOfDayDate = dnd.timeOfDayDate;
    } // end constructor


    protected void clear() {
        super.clear();
        timeOfDayDate = null;
    } // end clear


    public Date getTimeOfDayDate() {
        return timeOfDayDate;
    }

    public void setTimeOfDayDate(Date value) {
        timeOfDayDate = value;
    }

} // end class DayNoteData
