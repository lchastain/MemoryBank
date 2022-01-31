import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;

public class MilestoneNoteData extends IconNoteData {
    // We keep the Date as a String for more reliable persistence, because serialization of a LocalDate is problematic.
    // Then, we want to keep the date String private, without a direct setter or getter, because contexts that use
    //   this class often need to see the Date in various different formats but we don't want a non-parseable String
    //   coming back to our 'set' method, so we only allow the getter and setter to deal with a LocalDate class.
    // It will be up to the calling contexts to set only with a LocalDate, and they can format the Date that
    //   they receive from the getter however they need (not forgetting about the default '.toString' method, which
    //   is what is used here when creating the String that we ultimately persist (and is therefore guaranteed to be
    //   parseable when persisted data is reloaded).
    private String milestoneDateString; // Defaults to 'today' but can be set/chosen later.

    @JsonIgnore  // By ignoring this overloaded member from the base class, it is removed from data persistence.
    LinkTargets linkTargets;

    @JsonIgnore  // By ignoring this overloaded member from the base class, it is removed from data persistence.
    String subjectString;

    private int intIconOrder;

    public MilestoneNoteData() {
        clearTodoNoteData(); // sets default values.
    } // end constructor


    // The copy constructor (clone)
    public MilestoneNoteData(MilestoneNoteData mndCopy) {
        super(mndCopy);

        intIconOrder = mndCopy.intIconOrder;
        milestoneDateString = mndCopy.milestoneDateString;
        linkTargets = mndCopy.linkTargets;
        subjectString = mndCopy.subjectString;
    } // end constructor

    // Construct a TodoNoteData from a NoteData.
    // This is used when pasting Notes that were copied from NoteData interfaces.
    public MilestoneNoteData(NoteData nd) {
        super(nd);
        // We don't do the super.clear here; we want to keep the base data.
        clearTodoNoteData(); // sets default values; we didn't get any from the input param.
    } // end constructor

    @Override
    protected void clear() {
        super.clear();
        clearTodoNoteData();
    } // end clear


    @Override
    protected NoteData copy( ) {
        return new MilestoneNoteData(this);
    }

    private void clearTodoNoteData() {
        intIconOrder = 0;
        milestoneDateString = null;
    } // end clearTodoNoteData


    @JsonIgnore
    LocalDate getNoteDate() {
        if(milestoneDateString == null) return null;
        return LocalDate.parse(milestoneDateString);
    }

    @JsonIgnore // ignoring so that Jackson does not assume that there is an 'IconOrder'; it is 'intIconOrder'.
    public int getIconOrder() {
        return intIconOrder;
    }


    @JsonIgnore  // ignore for Jackson; there is no iconOrder member
    public void setIconOrder(int val) {
        intIconOrder = val;
        touchLastMod();
    }

    void setNoteDate(LocalDate value) {
        if(value == null) {
            milestoneDateString = null;
        } else {
            milestoneDateString = value.toString();
        }
        touchLastMod();
    }

} // end class TodoNoteData
