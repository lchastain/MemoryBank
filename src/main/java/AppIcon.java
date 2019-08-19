import net.sf.image4j.codec.bmp.BMPDecoder;
import net.sf.image4j.codec.ico.ICODecoder;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class AppIcon extends ImageIcon {
    private static final long serialVersionUID = -1747855358689601291L;

    // Pass-thru constructor to ImageIcon.
    AppIcon() {
        super();
        setDescription(""); // empty is ok, null is not.
    }

    AppIcon(String a_filename) {
        // Note: Do not construct with null - handle prior.
        MemoryBank.debug("Constructing AppIcon: " + a_filename);
        Image myImage = null;
        String filename = a_filename.toLowerCase(); // Not sure why I cared about this...

        // If this icon is being constructed for the iconChooser (or
        //   as a result of an iconChooser 'choice'), then 'filename'
        //   will be the full path to the icon file in Program Data.

        // Get the position of 'icons' in filename.  But it will not be there at all, for Test data.
        int iconsIndex = filename.indexOf("icons");

        // The filename will only start with 'icons' when it is being reconstructed
        // from saved user data.  So make the full path for it, by prefixing the
        // user's data location, where they will have their own set of icons.
        if (iconsIndex == 0) {
            filename = MemoryBank.userDataHome + File.separatorChar + filename;
            iconsIndex = filename.indexOf("icons");
        } // end if

        // Convert file separator characters, if needed.  This makes for file system
        // compatibility (even though we only expect to serve from one type of OS).
        char sep = File.separatorChar;
        if (sep != '/') filename = filename.replace(sep, '/');

        if (filename.endsWith(".ico")) {
            try {
                List<BufferedImage> images = ICODecoder.read(new File(filename));
                System.out.println("ico file image count: " + images.size());
                myImage = images.get(0);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else if (filename.endsWith(".bmp")) {
            try {
                myImage = BMPDecoder.read(new File(filename));
            } catch (IOException ioe) {
                MemoryBank.debug(ioe.getMessage());
            }
        } else {
            myImage = Toolkit.getDefaultToolkit().getImage(filename);
        } // end if

        // The 'description' IS used when saving - this is tricky; it may appear that this is not needed but the
        // description is picked up by the iconNoteComponent when the rest of the icon appears to come thru as null.
        // With the filename hiding in the place of the description, we can restore it as needed.
        // See also:  iconNoteComponent.mouseClicked and setIcon.
        if(iconsIndex > 0) {
            // Only set the description if the icon comes from a 'planned' location.
            // Otherwise we don't need a description, as in the case of Test data.
            setDescription(filename.substring(iconsIndex));
        }

        // Consider just ending at this point; could move the 'load and set' to the calling context.
        if (myImage == null) return;
        loadImage(myImage);  // Order matters..
        setImage(myImage);
        //MemoryBank.init();
    } // end constructor


    /*
    public void paintIcon(Component c, Graphics g, int x, int y) {
        super.paintIcon(c, g, x, y);
    } // end paintIcon
    */

    //-----------------------------------------------------
    // Method Name: scaleIcon
    //
    //-----------------------------------------------------
    public static AppIcon scaleIcon(AppIcon li) {
        int theHeight, theWidth;

        boolean scaleIt = false;
        Image tmpImg = li.getImage();

        theHeight = li.getIconHeight();
        if (theHeight > 36) {
            theHeight = 36;
            scaleIt = true;
        } // end if

        theWidth = li.getIconWidth();
        if (theWidth > 36) {
            theWidth = 36;
            scaleIt = true;
        } // end if

        if (scaleIt) tmpImg = tmpImg.
                getScaledInstance(theWidth, theHeight, Image.SCALE_SMOOTH);

        li.setImage(tmpImg);
        return li;
    } // end scaleIcon

} // end class AppIcon
