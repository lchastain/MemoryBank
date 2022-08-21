import net.sf.image4j.codec.bmp.BMPDecoder;
import net.sf.image4j.codec.ico.ICODecoder;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.List;

// This class is a 'handle' by which icon images may be acquired by the app.  It is needed as an alternative
//  to the 'iconFileString' of IconNoteData, that does not have the filesystem path and filename but still
//  supplies all the info needed for a DataAccessor to retrieve the indicated icon.
public class IconInfo {
    // Discussion:
    // An icon can be stored / retrieved in a variety of ways - via a data stream, a file, database query, and
    // there could easily be more coming that we don't yet know about.  Currently the MB app only supports the
    // filesystem approach, but it needs to provide the file info in a way that will be compatible with future
    // data accessors.
    //
    // Below are the generic fields used by the FileDataAccessor, that it can translate into a valid File.
    // dataArea:  The top-level 'category' of the icon.  in this case, APP_ICONS, IMAGES, etc.
    // iconName:  Still somewhat of a path (starting under dataArea), with segments (directories) separated by colons.
    //   Other accessors can interpret the segments as hierarchical categories.  The last segment (potentially the
    //   only one) is the name of the icon (in this case the icon filename, without the extension).
    // iconFormat:  A String ('bmp', 'gif', etc) because I decided not to limit the available types to known enum
    //   values, given that more types can show up at any time.
    //
    // More discussion:
    // When icons are presented by this app in a selection list (to be associated with a specific note), that list is
    //   just generated from whatever icon files are found in the filesystem under the "icons" data area; there is no
    //   explicit separately moderated list of them.  This means that there is no place where additional helpful or
    //   descriptive info per icon could be stored, nor is there any way to enter that info in the first place, since
    //   we currently have no 'icon editor'.  So, the idea of the IconInfo having a field of 'iconDescription' that
    //   might have been used in an icon selection dialog - is not possible.  Besides, the IconInfo class is not used
    //   in the selection operation; it is only used in the saving and loading of the icons.  Currently the
    //   only recourse you have for seeing a description at the time of icon selection is to rename the actual
    //   files to the best wording you can fit into the relatively few characters of a filename.

    // Idea - that a possible 'icon editor' could be quickly stood up as a NoteGroup that creates an IconNoteComponent
    //   for every icon that it finds in the filesystem, to include adding a hierarchy to the app tree, if subdirectories
    //   are found.  Possibly with a BranchHelper, too.  Wild!

    DataArea dataArea;  // Is this an App icon, a User icon, or an image ?  (Currently there ARE no user icons)
    String iconName;
    String iconFormat;

    IconInfo() {
        // Data members may be set directly.
        // A null or empty dataArea can be interpreted as a 'default' area.  For now, that will be 'icons'.
    }


    IconInfo(DataArea area, String name, String format) {
        dataArea = area;
        iconName = name;
        iconFormat = format;
    }

    ImageIcon getImageIcon() {
        ImageIcon theImageIcon = null;
        if (ready()) {
            String baseIconPath; // when dataArea is null we look in the default area.

            if (dataArea == DataArea.IMAGES) {
                baseIconPath = "images/";
            } else {
                baseIconPath = "icons/";  // This is the default area.
            }

            String theIconDescription;
            if(iconName.startsWith(baseIconPath)) {
                theIconDescription = iconName + "." + iconFormat;
            } else {
                theIconDescription = baseIconPath + iconName + "." + iconFormat;
            }

            MemoryBank.debug("Full icon name: " + theIconDescription);

            java.net.URL imgURL = IconInfo.class.getResource(theIconDescription);
            if(imgURL != null) {

                Image theImage = null;
                if (iconFormat.equalsIgnoreCase("ico")) {
                    try {
                        BufferedInputStream inputStream = new BufferedInputStream(imgURL.openStream());
                        List<BufferedImage> images = ICODecoder.read(inputStream);
                        theImage = images.get(0);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                } else if (iconFormat.equalsIgnoreCase("bmp")) {
                    try {
                        BufferedInputStream inputStream = new BufferedInputStream(imgURL.openStream());
                        theImage = BMPDecoder.read(inputStream);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                } else { // This handles .png, .jpg, .gif
                    theImage = Toolkit.getDefaultToolkit().getImage(imgURL);
                } // end if

                if (theImage != null) {
                    theImageIcon = new ImageIcon();
                    theImageIcon.setImage(theImage);

                    //===================================== IMPORTANT !!! ================================================
                    // ImageIcon docs will say that the description is not used or needed, BUT - it IS used by this app.
                    //   This is tricky; the description is picked up by IconNoteComponent.setIcon(ImageIcon theIcon).
                    //   With the URL hiding in the place of the description, we can update the associated
                    //   IconNoteData, and later restore the image from that.
                    theImageIcon.setDescription(theIconDescription);
                }
            } // end if we have a non-null URL
        } // end if the IconInfo is 'ready'.
        return theImageIcon;
    }

    // If I have a non-null, non-empty name and format, then I'm 'ready'.
    boolean ready() {
        boolean theAnser = iconName != null && !iconName.isEmpty();
        if(iconFormat == null || iconFormat.isEmpty()) theAnser = false;
        return theAnser;
    }

    // Scale the icon to a specific dimension
    public static void scaleIcon(ImageIcon theIcon, int theWidth, int theHeight) {
        Image tmpImg = theIcon.getImage();
        tmpImg = tmpImg.getScaledInstance(theWidth, theHeight, Image.SCALE_SMOOTH);
        theIcon.setImage(tmpImg);
    }

    // Scale the icon to a proportional dimension where the larger of width vs length is a set limit (36).
    public static void scaleIcon(ImageIcon theIcon) {
        int theHeight, theWidth;

        boolean scaleIt = false;

        theWidth = theIcon.getIconWidth();
        theHeight = theIcon.getIconHeight();

        int theMax = Math.max(theWidth, theHeight);
        if(theMax > 36) {
            scaleIt = true;
            if(theWidth == theMax) {
                theHeight = (theHeight * 36) / theWidth;
                theWidth = 36;
            } else {
                theWidth = (theWidth * 36) / theHeight;
                theHeight = 36;
            }
        }

        Image tmpImg = theIcon.getImage();
        if (scaleIt) {
            tmpImg = tmpImg.getScaledInstance(theWidth, theHeight, Image.SCALE_SMOOTH);
        }

        theIcon.setImage(tmpImg);
    } // end scaleIcon

}
