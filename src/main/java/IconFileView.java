// Description:  Modification of example code (free download) from
//   SUN that shows how to make a customized JFileChooser.
//   The modification mainly involves the addition of support for
//   Windows .ico files.

import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;

public class IconFileView extends FileView {

    public String getName(File f) {
        String s = f.getName();
        String name = s;
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            name = s.substring(0, i);
        }
        return name;
    }

    // When we do not override, the System FileView for the chosen L&F will provide.
    public Boolean isTraversable(File f) {
        return null;
    }

    /* From http://www.informit.com/articles/article.aspx?p=32060 :
    As of Swing1.1 FCS, the getDescription and getTypeDescription methods are not used within
    Swing. The methods are meant for look and feels that wish to provide additional information
    about files in a file chooser.   AND:
    http://stackoverflow.com/questions/6489978/java-early-access-download-what-does-fcs-means
        FCS - First Customer Shipment
    //=======================================================================================*/
    public String getDescription(File f) {
        return null;
    }

    public String getTypeDescription(File f) {
        return null;
    }
    //=========================================================================================

    public Icon getIcon(File f) {
        String extension = getExtension(f);
        Icon icon = null;

        // System.out.println("Path is: " + f.getPath());
        if (extension != null) {
            if (extension.equals("jpeg") ||
                    extension.equals("jpg") ||
                    extension.equals("gif") ||
                    extension.equals("tiff") ||
                    extension.equals("tif") ||
                    extension.equals("ico") ||
                    extension.equals("png")) {
                AppIcon ai = new AppIcon(f.getPath());
                if(ai.getImage() != null) icon = AppIcon.scaleIcon(ai);
            }
        } // end if extension not null
        return icon;
    } // end getIcon

    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    } // end getExtension
}
