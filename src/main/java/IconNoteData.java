import java.io.Serializable;

class IconNoteData extends NoteData implements Serializable {
    private static final long serialVersionUID = -4747292791676343443L;

    protected String iconFileString;
    protected boolean showIconOnMonthBoolean;

    public IconNoteData() {
        super();
    } // end constructor


    // The copy constructor (clone)
    public IconNoteData(IconNoteData ind) {
        super(ind);

        iconFileString = ind.iconFileString;
        showIconOnMonthBoolean = ind.showIconOnMonthBoolean;
    } // end constructor


    protected void clear() {
        super.clear();
        iconFileString = null;
        showIconOnMonthBoolean = false;
    } // end clear


    public String getIconFileString() {
        return iconFileString;
    }

    public boolean getShowIconOnMonthBoolean() {
        return showIconOnMonthBoolean;
    }

    public void setIconFileString(String val) {
        iconFileString = val;
    }

    public void setShowIconOnMonthBoolean(boolean val) {
        showIconOnMonthBoolean = val;
    }

} // end class IconNoteData


