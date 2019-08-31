import java.io.File;
import java.io.Serializable;
import java.util.Date;

public class TodoNoteData extends NoteData implements Serializable {
    static final long serialVersionUID = 8617084685157848161L;

    static final int TODO_STARTED = 0;
    static final int TODO_COMPLETED = 1;
    static final int TODO_INPROG = 2;
    static final int TODO_WAITING = 3;
    static final int TODO_QUERY = 4;
    static final int TODO_OBE = 5;

    private Date dateTodoItem;
    private int intPriority;
    private int intStatus;
    private String strLinkage;


    public TodoNoteData() {
        super();
        clearTodoNoteData(); // sets default values.
    } // end constructor


    // The copy constructor (clone)
    public TodoNoteData(TodoNoteData tndCopy) {
        super(tndCopy);

        // These may not be default values.
        intPriority = tndCopy.intPriority;
        intStatus = tndCopy.intStatus;
        strLinkage = tndCopy.strLinkage;
        dateTodoItem = tndCopy.dateTodoItem;
    } // end constructor

    // Construct a TodoNoteData from a NoteData.
    // This is used when taking Notes from NoteData interfaces.
    public TodoNoteData(NoteData nd) {
        super(nd);
        clearTodoNoteData(); // sets default values.
    } // end constructor

    @Override
    protected void clear() {
        super.clear();
        clearTodoNoteData();
    } // end clear


    @Override
    protected NoteData copy( ) {
        return new TodoNoteData(this);
    }

    private void clearTodoNoteData() {
        intPriority = 0;
        intStatus = TODO_STARTED;
        strLinkage = null;
        dateTodoItem = null;
    } // end clearTodoNoteData


    //-------------------------------------------------------------
    // Method Name: getDayNoteData
    //
    // Returns a version of itself that has been packed into
    //   a DayNoteData, for moving to a specific date.
    // Note that although a Day does not usually hold its correct calendar date
    //   in the 'time' field, in this case it must, in order for
    //   NoteGroup.addNote to place it into the correct file.
    //-------------------------------------------------------------
    public DayNoteData getDayNoteData(boolean useDate) {
        DayNoteData dnd = new DayNoteData();
        String newExtText;

        // Adjust the height of the extended text, if needed.
        int newHite = extendedNoteHeightInt;
        boolean thereIsExtText = extendedNoteString.trim().length() != 0;
        if (thereIsExtText) newHite += 28 + (21 * 2); // 2 new lines.
        // If the item had extended text, in 'porting' it to a
        //  DayNoteComponent we have to account for the 'subject' combobox
        //  plus any lines that we add, to adjust to same visibility.
        // If it had none to start, then the new data we're adding will
        //  fit inside the default text area without the need to expand.
        // As for width, we're not addressing it.

        // Create the DayNote Extended Text from the TodoNote by
        //   adding info that would otherwise be lost in the move.
        //----------------------------------------------------------------
        // First New Line:  Priority
        if (intPriority == 0) newExtText = "Priority: Not Set";
        else newExtText = "Priority: " + intPriority;
        newExtText += "\n";

        // Second New Line:  Status
        String theStatus = "Status: ";
        if (intStatus == 0) theStatus += "Not specified.";
        else theStatus += getStatusString();
        newExtText += theStatus + "\n";

        newExtText += extendedNoteString;
        //----------------------------------------------------------------

        // Set the timestamp for this Note according to which menu
        //   selection the user specified.
        Date newTimeDate;
        if (useDate) {
            newTimeDate = dateTodoItem;

            // The user should NOT have been able to select
            // 'Move to Selected Date' if the date selection
            // was null, but we cover that case here anyway.
            if (newTimeDate == null) newTimeDate = new Date();
        } else { // This 'else' goes with the higher 'if', not directly above.
            newTimeDate = new Date();
        }

        // Choose an initial icon based on status, if any.
        String iconFileString = null;
        if (intStatus > 0) {
            iconFileString = TodoNoteComponent.getIconFilename(intStatus);
            // Now change the 'images' reference to 'icons', and make sure that
            // this image is present in the user's data.
            File src = new File(iconFileString);
            MemoryBank.debug("  Source image is: " + src.getPath());
            int imagesIndex = iconFileString.indexOf("images");
            String destFileName = "icons/" + iconFileString.substring(imagesIndex + 7);
            destFileName = MemoryBank.userDataHome + "/" + destFileName;
            File dest = new File(destFileName);
            String theParentDir = dest.getParent();
            File f = new File(theParentDir);
            if (!f.exists()) //noinspection ResultOfMethodCallIgnored
                f.mkdirs();
            if (dest.exists()) {
                MemoryBank.debug("  Destination image is: " + src.getPath());
            } else {
                MemoryBank.debug("  Copying to: " + destFileName);
                AppUtil.copy(src, dest);
            } // end if
            iconFileString = dest.getPath();
        } // end if status has been set


        // Make all assignments
        dnd.setExtendedNoteHeightInt(newHite);
        dnd.setExtendedNoteString(newExtText);
        dnd.setExtendedNoteWidthInt(extendedNoteWidthInt);
        dnd.setIconFileString(iconFileString);
        dnd.setNoteString(noteString);
        dnd.setShowIconOnMonthBoolean(false);
        dnd.setSubjectString(subjectString);
        dnd.setTimeOfDayDate(newTimeDate);

        return dnd;
    } // end getDayNoteData


    public String getLinkage() {
        return strLinkage;
    }

    protected Date getNoteDate() {
        return dateTodoItem;
    }

    public int getPriority() {
        return intPriority;
    }

    public int getStatus() {
        return intStatus;
    }

    String getStatusString() {
        String s;
        switch (intStatus) {
            case TODO_STARTED:
                s = "No status at this time - click to change.";
                break;
            case TODO_COMPLETED:
                s = "This item has been completed.";
                break;
            case TODO_INPROG:
                s = "This item is in progress.";
                break;
            case TODO_WAITING:
                s = "Waiting until a specified date/time.";
                break;
            case TODO_QUERY:
                s = "Waiting for a response or event (beyond your control).";
                break;
            case TODO_OBE:
                s = "This item will not be done after all.";
                break;
            default:
                s = "Undefined";
        } // end switch
        return s;
    } // end getStatusString


    public void setLinkage(String val) {
        strLinkage = val;
    }

    public void setPriority(int val) {
        intPriority = val;
    }

    public void setStatus(int val) {
        intStatus = val;
    }

    void setTodoDate(Date value) {
        dateTodoItem = value;
    }

} // end class TodoNoteData
