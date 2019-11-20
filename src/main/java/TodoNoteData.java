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


    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) return true; // reference check
        if (otherObject == null) return false; // null check
        if (getClass() != otherObject.getClass()) return false; // type check

        TodoNoteData otherData = (TodoNoteData) otherObject;

        if (!noteString.equals(otherData.noteString)) return false;
        if (!extendedNoteString.equals(otherData.extendedNoteString)) return false;
        if (intPriority != otherData.getPriority()) return false;
        return intStatus == otherData.getStatus();
    }

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


    // This is used by a Set during uniqueness checking.  This method effectively disables
    // the 'hashcode' part of the check, so that the only remaining uniqueness criteria
    // is the result of the .equals() method.
    @Override
    public int hashCode() {
        return 1;
    }

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
