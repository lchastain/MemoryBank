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
        setDescription(""); // empty is ok, null is not.
    }

    AppIcon(String a_filename) {
        // Note: Do not construct with null - handle that case prior to coming here.
        MemoryBank.debug("Constructing AppIcon: " + a_filename);
        Image myImage = null;
        String filename = a_filename.toLowerCase(); // Not sure why I cared about this...

        // If this icon is being constructed as a result of an iconChooser selection,
        // then filename will be the full path to the icon file in Program Data.

        // Planning note 11/30/2019:  A future rev of this app will allow users to manage/use their own
        // icons, in a subdir (named myIcons?) in their user data directory.  Check there first and if not found
        // then you can get it from the main loc.  If also not found there, issue a debug complaint but move on.
        // The stored filenames can be the icon filenames, only (no leading 'icons\' should be needed).  This
        // should allow a user to replace previously set library icons, IF they happen to name theirs with the same
        // name (and extension).  Possibly on the library icons, provide a way to see the correct filename.

        // The filename will only start with 'icons' when it is being reconstructed
        // from saved user data.  So make the full path for it, by prefixing the
        // program data location.
        if (filename.indexOf("icons") == 0) {
            filename = MemoryBank.logHome + File.separatorChar + filename;
        } // end if

        // Convert file separator characters, if needed.  This makes for file system
        // compatibility (even though we only expect to serve from one type of OS).
        char sep = File.separatorChar;
        if (sep != '/') filename = filename.replace(sep, '/');
        MemoryBank.debug("  Full icon filename: " + filename);

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
        } else {
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
        //MemoryBank.trace();
    } // end constructor


    /*
    public void paintIcon(Component c, Graphics g, int x, int y) {
        super.paintIcon(c, g, x, y);
    } // end paintIcon
    */


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
