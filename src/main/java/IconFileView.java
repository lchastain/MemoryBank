// Description:  Modification of example code (free download) from
//   SUN that shows how to make a customized JFileChooser.
//   The modification mainly involves the addition of support for
//   Windows .ico files.

// This class is used when presenting a dialog for icon selection.

import javax.swing.*;
import javax.swing.filechooser.FileView;
import java.io.File;

public class IconFileView extends FileView {

    // The getName and getIcon methods are not called directly by MemoryBank, but indirectly by
    //   the filesystem when displaying a file selection dialog for icon-type files.

    // We provide our own name specifier, to drop off the extension.
    @Override
    public String getName(File f) {
        String s = f.getName();
        String name = s;
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            name = s.substring(0, i);
        }
        return name;
    }

    // When we do not override, the System FileView for the chosen L&F will provide this
    // for us, but we just don't want it.
    @Override
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

    public String getDescription(File f) { return null; }
    public String getTypeDescription(File f) { return null; }

    // This method reads in each file and sends back an icon to be displayed in the file selector, vs the default
    //   which does not always do that for all the file types that we specify below.
    // But this overridden method is called by the system with many other files that are not icons, so the extension
    //   is used to recognize icon files vs other types, and this method will only handle those that it recognizes.
    @Override
    public Icon getIcon(File f) { // Note that the return type is an interface, not a class.
        String iconFileName = f.getName();
        String[] nameParts = iconFileName.split("\\.");

        ImageIcon icon = null;

        //System.out.println("File is: " + f.getAbsolutePath());
        if (nameParts.length >= 2) {
            String extension = nameParts[1].toLowerCase();

            switch (extension) {
                case "jpeg":
                case "jpg":
                case "tiff":
                case "tif":
                case "png":
                    icon = new ImageIcon(f.getPath());
                    if (icon.getImage() != null) IconInfo.scaleIcon(icon);
                    break;
                case "gif":  // Animated gifs appear as blanks in the IconFileChooser and the IconNoteComponent, but
                    // they can still work correctly in the MonthView.  Looking for a solution, found this:
                    // https://stackoverflow.com/questions/2935232/show-animated-gif which suggests that they would
                    // work better if loaded from a URL vs a File, but my own test does not support that; it works
                    // identically to an image that was loaded via a File.
                    // The getResource method that is used to make a URL will look for the files in same folder (or
                    // package if you are running from a jar), but that is not workable when running from an IDE that
                    // regularly rebuilds its output directory.  So - I put one animated gif into the production area
                    // (C:\Users\Lee\workspace\Memory Bank\out\production\MemoryBank)
                    // for a test, and was able to make a good URL, and the image did appear to load via the ImageIcon
                    // constructor, although it still did not display in the FileChooser or the IconNoteComponent.
                    // So for now we will stick with what has been at least partially working, which is the same as
                    // most of the others, including the non-animated gifs.
                    icon = new ImageIcon(f.getPath());
                    if (icon.getImage() != null) {
                        // Same as above but with braces, to keep IJ quiet about identical case handling.
                        // Hoping for some future change where I can have better animated gifs, in which
                        // case that code would go under this (then-different) case.
                        IconInfo.scaleIcon(icon);
                    }
                    break;
                case "bmp": // bmp still having issues; the decoder throws an exception in some (most?) cases.
                    break;
                case "ico":
                    // The app is coded so that we only arrive here when we are NOT running from a .jar file.
                    // So, the use of an IconInfo at this stage to retrieve a Resource is not appropriate.  Instead, we
                    // just go with the direct file reference, which may be pointing anywhere in the local filesystem.
                    IconNoteData ind = new IconNoteData();

                    ind.setIconFileString(f.getAbsolutePath());
                    icon = ind.getImageIcon();
                    if (icon.getImage() != null) IconInfo.scaleIcon(icon);
                    break;
            }
        } // end if extension not null
        return icon;
    } // end getIcon

    private static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    } // end getExtension
}
