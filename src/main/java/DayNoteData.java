import java.time.LocalTime;

class DayNoteData extends IconNoteData {
    private String timeOfDayString;

    DayNoteData() {
        timeOfDayString = LocalTime.now().toString();
    } // end constructor


    // Alternate constructor, for starting with an EventNoteData,
    //   used when aging events, prior to saving to a Day.
    DayNoteData(EventNoteData eventNoteData) {
        super(eventNoteData);  // takes care of IconNoteData & NoteData members.

        // but fix the icon, if it is 'default' - we want the Event default icon, not the Daynote default icon.

        // The Event may have a Category (ie, Subject), but for this
        // usage we will override it if there is one, to 'Event'.
        setSubjectString("Event");

        // Now, if the Event does have its own Subject/Category,
        // preserve it into the extended note of the DayNoteData.
        String s = eventNoteData.extendedNoteString;
        if (eventNoteData.subjectString != null) {
            s = eventNoteData.subjectString + "\n" + s;
        } // end if

        // And append the location, if there is one.
        String theLocation = eventNoteData.getLocationString();
        if(theLocation != null && !theLocation.trim().equals("")) {
            s += "\nLocation: " + theLocation;
        } // end if
        setExtendedNoteString(s);

        // And we still have to set our timeOfDayString -
        // Using the Event Start Time, if any -
        LocalTime lt = eventNoteData.getStartTime();
        if(lt == null) {
            timeOfDayString = ""; // null is no longer being saved as a value, but do not want the default (current time).
        } else {
            timeOfDayString = lt.toString();
        }
    } // end constructor

    // Alternate constructor, for starting with a TodoNoteData,
    //   used when moving Todo notes to a specific day.
    DayNoteData(TodoNoteData todoNoteData) {
        super(todoNoteData);  // takes care of IconNoteData & NoteData members.

        //----------------------------------------------------------------
        // Create the DayNote Extended Text from the TodoNote by prefixing
        //   Priorty and Status info that would otherwise be lost in the move.
        //----------------------------------------------------------------
        // First New Line in the Extended Text:  Priority
        String newExtText;
        if (todoNoteData.getPriority() == 0) newExtText = "Priority: Not Set";
        else newExtText = "Priority: " + todoNoteData.getPriority();
        newExtText += "\n";

        // Second New Line:  Status
        String theStatus = "Status: ";
        if (todoNoteData.getStatus() == 0) theStatus += "Not specified.";
        else theStatus += todoNoteData.getStatusString();
        newExtText += theStatus + "\n";

        // Append the original extended note, if any.
        newExtText += todoNoteData.extendedNoteString;
        //----------------------------------------------------------------

        // Make the remaining DayNoteData assignments
        extendedNoteString = newExtText;
        iconFileString = TodoNoteComponent.getIconFilename(todoNoteData.getStatus());
        showIconOnMonthBoolean = false;
        timeOfDayString = ""; // null is no longer being saved as a value, but do not want the default (current time).
    } // end constructor


    // The copy constructor (clone)
    DayNoteData(DayNoteData dnd) {
        super(dnd); // IconNoteData --> NoteData
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


    // This is provided as an alternative way to call the copy constructor,
    // for when the exact type is not known but the class is either this
    // one or one of its descendants.
    @Override
    protected NoteData copy() {
        return new DayNoteData(this);
    }

    String getTimeOfDayString() {
        return timeOfDayString;
    }

    void setTimeOfDayString(String value) {
        timeOfDayString = value;
        touchLastMod();
    }

} // end class DayNoteData
