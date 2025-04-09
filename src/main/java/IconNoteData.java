import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.swing.*;

class IconNoteData extends NoteData {
    String iconFileString;  // The 'description' of the icon.
    // If null, use the default icon.  If "", show a blank icon.

    boolean showIconOnMonthBoolean;

    IconNoteData() {
    } // end constructor

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
        String newString = iconFileString;

        // Drop off the leading part of a path, if there is one.
        // No longer keeping a protocol, drive, or filesystem path.
        // This is an on-the-fly data fix.
        if (iconFileString != null) {
            int theIndex = iconFileString.indexOf("/icons/");
            if (theIndex > 0) newString = iconFileString.substring(theIndex + 1);
        }
        return newString;
    }

    IconInfo getIconInfo() {
        IconInfo theIconInfo = null;
        String theIconName = getIconFileString(); // This is what drops off the leading part of a full path.
        if (theIconName != null) {
            theIconInfo = new IconInfo();
            theIconInfo.dataArea = DataArea.APP_ICONS;

            int dotIndex = theIconName.lastIndexOf(".");
            if (dotIndex > 0) {
                theIconInfo.iconName = theIconName.substring(0, dotIndex);
                theIconInfo.iconFormat = theIconName.substring(dotIndex + 1);
            }
        }
        return theIconInfo;
    }

    // do not let Jackson think that there is an 'imageIcon' data member.
    @JsonIgnore
    ImageIcon getImageIcon() {
        ImageIcon theImageIcon = null;

        iconFileString = getIconFileString(); // drop out any leading drive/path specifier.
        if (iconFileString != null) {
            IconInfo iconInfo = getIconInfo();
            theImageIcon = iconInfo.getImageIcon();
        }
        return theImageIcon;
    } // end getImageIcon

    boolean getShowIconOnMonthBoolean() {
        return showIconOnMonthBoolean;
    }

    public void setIconFileString(String val) {
        iconFileString = val; // A null value is ok; supports the 'Reset Icon' operation.
        if (val != null) {
            iconFileString = val.replaceAll("\\\\", "/");
            //MemoryBank.debug("IconNoteData.setIconFileString to: " + iconFileString);
        }
    }

    void setShowIconOnMonthBoolean(boolean val) {
        // Whether or not we show this on the MonthView should not have an
        // effect on its Last Mod date, so this 'set' method does not update it.
        showIconOnMonthBoolean = val;
    }

} // end class IconNoteData


