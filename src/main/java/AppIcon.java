import net.sf.image4j.codec.bmp.BMPDecoder;
import net.sf.image4j.codec.ico.ICODecoder;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.List;

public class AppIcon extends ImageIcon {
    @Serial
    private static final long serialVersionUID = 1L;

    AppIcon(String a_filename) {
        // Note: Do not construct with null - handle that case prior to coming here.
        //FlashCards.debug("Constructing AppIcon: " + a_filename);
        Image myImage = null;
        String filename = a_filename;//.toLowerCase(); // Not sure why I cared about this...

        // Convert file separator characters, if needed.  This makes for file system
        // compatibility (even though we only expect to serve from one type of OS).
        char sep = File.separatorChar;
        if (sep != '/') filename = filename.replace(sep, '/');
        //FlashCards.debug("  Full icon filename: " + filename);

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
        } else {  // jpeg, jpg, png
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

    // Scale the icon as large as possible proportionally, keeping within both maximums.
    @SuppressWarnings("unused")
    public static void propScaleIcon(AppIcon theIcon, int maxWidth, int maxHeight) {
        Image tmpImg = theIcon.getImage();
        int imageWidth = tmpImg.getWidth(null);
        int imageHeight = tmpImg.getHeight(null);
        int propWidth, propHeight;

        // Take the maximum width as the new proportional width -
        //   The max may be more or less than the width of the received AppIcon's image.
        propWidth = maxWidth; // and no harm done if it was already the same value.

        // Then calculate a factor of adjustment that reflects the new assignment.
        float adj = (float) propWidth / imageWidth;

        // And then apply that factor to the image height, to get the resulting proportional height -
        propHeight = (int) (imageHeight * adj);

        // But that adjustment may have put the propHeight over its max, so -
        //   incrementally reduce the proportional width and calculate a new
        //   adjustment, until the proportional height is at or under its max.
        while(propHeight > maxHeight) {
            propWidth--;
            adj = (float) propWidth / imageWidth;
            propHeight = (int) (imageHeight * adj);
        }

        System.out.println("Received icon with dimensions: "  + imageWidth + " x " + imageHeight);
        if(propWidth == imageWidth && propHeight == imageHeight) {
            System.out.println("  No scaling needed");
        } else {
            System.out.println("  Scaled to dimensions: " + propWidth + " x " + propHeight);
            tmpImg = tmpImg.getScaledInstance(propWidth, propHeight, Image.SCALE_SMOOTH);
        }

        theIcon.setImage(tmpImg);
    } // end propScaleIcon


    public static void scaleIcon(AppIcon theIcon) {
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
