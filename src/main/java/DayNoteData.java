import java.time.LocalTime;

class DayNoteData extends IconNoteData {
    private String timeOfDayString;

    DayNoteData() {
        super();
        timeOfDayString = LocalTime.now().toString();
    } // end constructor


    // Alternate constructor, for starting with an EventNoteData,
    //   used when aging events, prior to saving to a Day.
    DayNoteData(EventNoteData end) {
        super(end);  // takes care of IconNoteData & NoteData members.

        // but fix the icon, if it is 'default' - we want the Event default icon, not the Daynote default icon.

        // The Event may have a Category (ie, Subject), but for this
        // usage we will override it if there is one, to 'Event'.
        setSubjectString("Event");

        // Now, if the Event does have its own Subject/Category,
        // preserve it into the extended note of the DayNoteData.
        String s = end.extendedNoteString;
        if (end.subjectString != null) {
            s = end.subjectString + "\n" + s;
        } // end if

        // And append the location, if there is one.
        String theLocation = end.getLocationString();
        if(theLocation != null && !theLocation.trim().equals("")) {
            s += "\nLocation: " + theLocation;
        } // end if
        setExtendedNoteString(s);

        // And we still have to set our timeOfDayString -
        // Using the Event Start Time, if any -
        LocalTime lt = end.getStartTime();
        if(lt == null) {
            timeOfDayString = null;
        } else {
            timeOfDayString = lt.toString();
        }

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
