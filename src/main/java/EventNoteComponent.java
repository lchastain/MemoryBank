/**  Representation of a single Event Note.  It 'contains' the associated
 EventNoteData.
 */

public class EventNoteComponent extends IconNoteComponent {
    private static final long serialVersionUID = 1L;

    // The Member
    private EventNoteData myEventNoteData;

    EventNoteComponent(EventNoteGroup eng, int i) {
        super(eng, i);

        MemoryBank.init();
    } // end constructor


    //-----------------------------------------------------------------
    // Method Name: getNoteData
    //
    // This method is called to update information into the local
    //   EventNoteData prior to accessing it.
    //-----------------------------------------------------------------
    public NoteData getNoteData() {
        if (!initialized) return null;
        // Extended note data was already updated, since it was collected
        //   from a modal dialog.

        // The icon string also comes from a modal selection, directly into
        //   the data.
        return myEventNoteData;
    } // end getNoteData


    protected void makeDataObject() {
        myEventNoteData = new EventNoteData();
    } // end makeDataObject


    protected void resetMouseMessage(int textStatus) {
        String s = " ";

        switch (textStatus) {
            case NEEDS_TEXT:
                s = "Click here to enter text for this Event.";
                break;
            case HAS_BASE_TEXT:
                s = "Double-click here to add details about this Event.";
                break;
            case HAS_EXT_TEXT:
                // This gives away the 'hidden' text, if
                //   there is no primary (blue) text.
                s = "Double-click here to see/edit";
                s += " the additional details for this Event.";
        } // end switch
        myNoteGroup.setMessage(s);
    } // end resetMouseMessage


    @Override
    public void setNoteData(NoteData newNoteData) {
        if (newNoteData instanceof EventNoteData) {  // same type, but cast is still needed
            setEventNoteData((EventNoteData) newNoteData);
        } else { // Not 'my' type, but we can make it so.
            setEventNoteData(new EventNoteData(newNoteData));
        }
    } // end setNoteData


    void setEventNoteData(EventNoteData newNoteData) {
        myEventNoteData = newNoteData;

        // update visual components...
        initialized = true;  // without updating the 'lastModDate'
        resetComponent();
        setNoteChanged();
    } // end setNoteData


    public void swap(NoteComponent enc) {
        // Get a reference to the two data objects
        EventNoteData end1 = (EventNoteData) this.getNoteData();
        EventNoteData end2 = (EventNoteData) enc.getNoteData();

        // Note: getNoteData and setNoteData are working with references
        //   to data objects.  If you 'get' data into a local variable
        //   and then later clear the component, you have also just
        //   cleared the data in your local variable because you never had
        //   a separatate copy of the data object, just the reference to it.

        // So - copy the data objects.
        if (end1 != null) end1 = new EventNoteData(end1);
        if (end2 != null) end2 = new EventNoteData(end2);

        if (end1 == null) enc.clear();
        else enc.setNoteData(end1);

        if (end2 == null) this.clear();
        else this.setEventNoteData(end2);

        myNoteGroup.setGroupChanged();
    } // end swap

} // end class EventNoteComponent



