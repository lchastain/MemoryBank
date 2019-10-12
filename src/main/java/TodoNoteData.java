import java.io.File;
import java.time.LocalDate;

public class TodoNoteData extends NoteData {
    static final int TODO_STARTED = 0;
    static final int TODO_COMPLETED = 1;
    static final int TODO_INPROG = 2;
    static final int TODO_WAITING = 3;
    static final int TODO_QUERY = 4;
    static final int TODO_OBE = 5;

    private String todoDateString; // This can be a deadline or a 'do after', or ...   Set/chosen on the TMC
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
        todoDateString = tndCopy.todoDateString;
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
        todoDateString = null;
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
    DayNoteData getDayNoteData() {
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

        // Choose an initial icon based on status, if any.
        String iconFileString = null;
        if (intStatus > 0) {
            iconFileString = TodoNoteComponent.getIconFilename(intStatus);
            // Now change the 'images' reference to 'icons', and make sure that
            // this image is present in the user's data.
            File src = new File(iconFileString);
            MemoryBank.debug("  Source image is: " + src.getPath());
            int imagesIndex = iconFileString.indexOf("images");
            String destFileName = "icons" + File.separatorChar + iconFileString.substring(imagesIndex + 7);
            destFileName = MemoryBank.userDataHome + File.separatorChar + destFileName;
            File dest = new File(destFileName);
            String theParentDir = dest.getParent();
            File f = new File(theParentDir);
            if (!f.exists()) //noinspection ResultOfMethodCallIgnored
                f.mkdirs();
            if (dest.exists()) {
                MemoryBank.debug("  Destination image is: " + dest.getPath());
            } else {
                MemoryBank.debug("  Copying to: " + destFileName);
                AppUtil.copy(src, dest);
            } // end if
            iconFileString = dest.getPath();
        } // end if status has been set

        // Make all assignments
        // TODO - the time of day should be blank, for a moved todo note.
        dnd.setExtendedNoteHeightInt(newHite);
        dnd.setExtendedNoteString(newExtText);
        dnd.setExtendedNoteWidthInt(extendedNoteWidthInt);
        dnd.setIconFileString(iconFileString);
        dnd.setNoteString(noteString);
        dnd.setShowIconOnMonthBoolean(false);
        dnd.setSubjectString(subjectString);

        return dnd;
    } // end getDayNoteData


    public String getLinkage() {
        return strLinkage;
    }

    LocalDate getTodoDate() {
        if(todoDateString == null) return null;
        return LocalDate.parse(todoDateString);
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

    void setTodoDate(LocalDate value) {
        if(value == null) {
            todoDateString = null;
        } else {
            todoDateString = value.toString();
        }
    }

} // end class TodoNoteData
