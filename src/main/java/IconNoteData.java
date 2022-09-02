import com.fasterxml.jackson.annotation.JsonIgnore;
import net.sf.image4j.codec.bmp.BMPDecoder;
import net.sf.image4j.codec.ico.ICODecoder;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

class IconNoteData extends NoteData {
    String iconFileString;  // The 'description' of the icon
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
            if (theIndex > 0) newString = iconFileString.substring(theIndex+1);
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
            if(MemoryBank.appEnvironment.equals("ide")) {
                String theFilename = iconFileString;
                if(theFilename.startsWith("icons/")) theFilename = "src/main/resources/" + theFilename;

                //MemoryBank.debug("The relative path/filename for the icon: " + theFilename);
                if (new File(theFilename).exists()) {
                    Image theImage = null;
                    if (theFilename.endsWith(".ico")) {
                        try {
                            List<BufferedImage> images = ICODecoder.read(new File(theFilename));
                            theImage = images.get(0);
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    } else if (theFilename.endsWith(".bmp")) {
                        try {
                            theImage = BMPDecoder.read(new File(theFilename));
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    } else { // This handles .png, .jpg, .gif
                        theImage = Toolkit.getDefaultToolkit().getImage(theFilename);
                    } // end if

                    if (theImage != null) {
                        theImageIcon = new ImageIcon();
                        theImageIcon.setImage(theImage);

                        //=============== IMPORTANT !!! =====================================================================
                        // ImageIcon docs will say that the description is not used or needed, BUT - it IS used by this app.
                        //   This is tricky; the description is picked up by IconNoteComponent.setIcon(ImageIcon theIcon).
                        //   With the filename hiding in the place of the description, we can update the associated
                        //   IconNoteData, and later restore the image from that.
                        theImageIcon.setDescription(theFilename);
                    }
                } // end if - if we are running via the ide and we found a file for the icon in the local filesystem.
            } else { // Otherwise, we expect to find the icon in the resources extracted from the jar we are running.
                IconInfo iconInfo = getIconInfo();
                theImageIcon = iconInfo.getImageIcon();
            }
        }
        return theImageIcon;
    } // end getImageIcon

    boolean getShowIconOnMonthBoolean() {
        return showIconOnMonthBoolean;
    }

    public void setIconFileString(String val) {
        if (val != null) {
            iconFileString = val.replaceAll("\\\\", "/");
            MemoryBank.debug("IconNoteData.setIconFileString to: " + iconFileString);
        }
    }

    void setShowIconOnMonthBoolean(boolean val) {
        // Whether or not we show this on the MonthView should not have an
        // effect on its Last Mod date, so this 'set' method does not update it.
        showIconOnMonthBoolean = val;
    }

} // end class IconNoteData


