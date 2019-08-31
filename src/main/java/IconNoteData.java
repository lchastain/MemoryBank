import java.io.Serializable;

class IconNoteData extends NoteData implements Serializable {
    private static final long serialVersionUID = -4747292791676343443L;

    protected String iconFileString;
    boolean showIconOnMonthBoolean;

    IconNoteData() {
        super();
    } // end constructor

    // The copy constructor (clone)
    IconNoteData(IconNoteData ind) {
        super(ind);

        iconFileString = ind.iconFileString;
        showIconOnMonthBoolean = ind.showIconOnMonthBoolean;
    } // end constructor

    // Construct an IconNoteData from a NoteData
    // The result will just have the default icon.
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

    boolean getShowIconOnMonthBoolean() {
        return showIconOnMonthBoolean;
    }

    public void setIconFileString(String val) {
        iconFileString = val;
    }

    void setShowIconOnMonthBoolean(boolean val) {
        showIconOnMonthBoolean = val;
    }

} // end class IconNoteData


