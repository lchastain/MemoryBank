import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.swing.*;

class IconNoteData extends NoteData {
    String iconFileString;  // The 'description' of the icon
    boolean showIconOnMonthBoolean;

    IconNoteData() { } // end constructor

    // The copy constructor (clone)
    IconNoteData(IconNoteData ind) {
        super(ind);

        iconFileString = ind.iconFileString;
        showIconOnMonthBoolean = ind.showIconOnMonthBoolean;
    } // end constructor

    // Construct an IconNoteData from a NoteData
    IconNoteData(NoteData nd) {
        super(nd);
    } // end constructor

    protected void clear() {
        super.clear();
        iconFileString = null;
        showIconOnMonthBoolean = false;
    } // end clear


    public String getIconFileString() {
        return iconFileString;
    }

    // do not let Jackson think that there is an 'imageIcon' data member.
    @JsonIgnore
    ImageIcon getImageIcon() {
        return MemoryBank.dataAccessor.getImageIcon(this);
    }

    boolean getShowIconOnMonthBoolean() {
        return showIconOnMonthBoolean;
    }

    public void setIconFileString(String val) {
        if(val != null) {
            iconFileString = val;
            System.out.println(val);
        }
    }

    void setShowIconOnMonthBoolean(boolean val) {
        // Whether or not we show this on the MonthView should not have an
        // effect on its Last Mod date, so this 'set' method does not update it.
        showIconOnMonthBoolean = val;
    }

} // end class IconNoteData


