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
        String filename = a_filename.toLowerCase();

        // If this icon is being constructed for the iconChooser (or
        //   as a result of an iconChooser 'choice'), then 'filename'
        //   will be the full path to the icon file in Program Data.

        // Get the position of 'icons' in filename.
        int iconsIndex = filename.indexOf("icons");

        // If this icon is being reconstructed from saved
        //   user data then 'filename' will be a relative
        //   (short) path, so we need to prefix it with the
        //   path to the user's data.
        if (iconsIndex == 0) {
            filename = MemoryBank.userDataHome + "/" + filename;
        } // end if

        // Convert file separator characters, if needed.
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

        // Consider just ending at this point; move the 'load and set' to the calling context, or add
        // another level of objects, for 'myImage'
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
