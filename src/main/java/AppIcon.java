import net.sf.image4j.codec.bmp.BMPDecoder;
import net.sf.image4j.codec.ico.ICODecoder;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class AppIcon extends ImageIcon {
    private static final long serialVersionUID = 1L;

    // Pass-thru constructor to ImageIcon.
    AppIcon() {
        super();
        setDescription(""); // empty is ok, null is not.
    }

    AppIcon(String a_filename) {
        // Note: Do not construct with null - handle that case prior to coming here.
        Image myImage = null;
        String filename = a_filename.toLowerCase(); // So the 'endsWith' condition has less to consider.

        // Convert file separator characters, if needed.  This makes for file system
        // compatibility (even though we only expect to serve from one type of OS).
        char sep = File.separatorChar;
        if (sep != '/') filename = filename.replace(sep, '/');
        //debug("  Full icon filename: " + filename);

        if (filename.endsWith(".ico")) {
            try {
                List<BufferedImage> images = ICODecoder.read(new File(filename));
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
        } else { // This handles .png, .jpg, .gif
            myImage = Toolkit.getDefaultToolkit().getImage(filename);
        } // end if

        // The 'description' IS used when saving - this is tricky; it may appear that this is not needed but the
        // description is picked up by the iconNoteComponent when the rest of the icon appears to come thru as null.
        // With the filename hiding in the place of the description, we can restore it as needed.
        // See also:  iconNoteComponent.mouseClicked and setIcon.
        setDescription(filename);

        // Consider just ending at this point; could move the 'load and set' to the calling context.
        if (myImage == null) return;
        loadImage(myImage);  // Order matters..
        setImage(myImage);
    } // end constructor


    public static void scaleIcon(AppIcon theIcon, int theWidth, int theHeight) {
        Image tmpImg = theIcon.getImage();
        tmpImg = tmpImg.getScaledInstance(theWidth, theHeight, Image.SCALE_SMOOTH);
        theIcon.setImage(tmpImg);
    }


    public static void scaleIcon(AppIcon theIcon) {
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

    public static void scaleIcon0(AppIcon theIcon) {
        int theHeight, theWidth;

        boolean scaleIt = false;
        Image tmpImg = theIcon.getImage();

        theHeight = theIcon.getIconHeight();
        if (theHeight > 36) {
            theHeight = 36;
            scaleIt = true;
        } // end if

        theWidth = theIcon.getIconWidth();
        if (theWidth > 36) {
            theWidth = 36;
            scaleIt = true;
        } // end if

        if (scaleIt) tmpImg = tmpImg.
                getScaledInstance(theWidth, theHeight, Image.SCALE_SMOOTH);

        theIcon.setImage(tmpImg);
    } // end scaleIcon

} // end class AppIcon
