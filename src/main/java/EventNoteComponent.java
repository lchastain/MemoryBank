/*  Representation of a single Event Note.  It 'contains' the associated
 EventNoteData.
 */

public class EventNoteComponent extends IconNoteComponent {
    private static final long serialVersionUID = 1L;
    private EventNoteGroupPanel myNoteGroup;

    // The Member
    private EventNoteData myEventNoteData;

    EventNoteComponent(EventNoteGroupPanel eng, int i) {
        super(eng, i);
        myNoteGroup = eng;
        MemoryBank.trace();
    } // end constructor


    @Override
    public NoteData getNoteData() { return myEventNoteData;  }

    protected void makeDataObject() {
        myEventNoteData = new EventNoteData();
    } // end makeDataObject

    @Override
    protected void noteActivated(boolean blnIAmOn) {
        myNoteGroup.showComponent(this, blnIAmOn);
        super.noteActivated(blnIAmOn);
    }

    @Override
    void resetNoteStatusMessage(int textStatus) {
        String s = " ";

        // Since the isEditable flag is static we cannot be checking it at
        // runtime (now).  But the result of the value that it had during
        // construction can still be considered -
        if(noteTextField.isEditable()) {
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
        } else {
            s = "Events shown in the Consolidated View are non-editable.  ";
            s += "Go to the original source if a change is needed.";
        }
        myNoteGroup.setStatusMessage(s);
    } // end resetNoteStatusMessage


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

        // update visual components without updating the 'lastModDate'
        initialized = true;
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

        myNoteGroup.setGroupChanged(true);
    } // end swap

} // end class EventNoteComponent



