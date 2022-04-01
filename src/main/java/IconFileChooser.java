import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;

public class IconFileChooser extends JFileChooser {
    private static final long serialVersionUID = 1L;
    static private final FileFilter filter;

    /* A note about the support for the bmp format - while a decoder for it has been added to the FileDataAccessor,
    it seems that the two I tried were still unsupported (16-bit, compressed) and for
    the one that I found that was supported, scaling was required and after that it was
    not visible in the chooser, and upon selection it threw an exception with a long
    stack trace that did not even trail back to Memory Bank source.  So bmp icons, if used at all,
    will need to be added / managed carefully.  For now, that type is disabled at the level of
    the IconFileView even though at this level it appears to be available.
     */
    static {
        filter = new FileFilter() {
            public boolean accept(File f) {
                if (f != null) {
                    if (f.isDirectory()) return true;
                    String filename = f.getName().toLowerCase();
                    int i = filename.lastIndexOf('.');
                    if (i > 0 && i < filename.length() - 1) {
                        String extension = filename.substring(i + 1);
                        return extension.equals("tiff") ||
                                extension.equals("tif") ||
                                extension.equals("gif") ||
                                extension.equals("bmp") ||
                                extension.equals("jpeg") ||
                                extension.equals("jpg") ||
                                extension.equals("ico") ||
                                extension.equals("png");
                    } // end if
                } // end if
                return false;
            } // end accept

            public String getDescription() {
                return "supported icon images";
            } // end getDescription
        };
    }

    // References:
    // https://docs.oracle.com/javase/tutorial/uiswing/components/filechooser.html
    // https://docs.oracle.com/javase/tutorial/uiswing/examples/components/index.html#FileChooserDemo2
    // Note that the implementation in this app does not use 'setAccessory'; if it did, upon selection of an icon
    //   it would display it (again) in a separate panel to the right.  But since that panel is redundant in all
    //   cases except for animated gifs, and because that panel would always be present and using up valuable
    //   screen real-estate, we do not use it.
    //
    // BUT - the setAccessory method could be used when the path includes 'animated'.  Call it upon
    //  change of navigation...
    public IconFileChooser(String thePath) {
        super(thePath);
        setAcceptAllFileFilterUsed(false);
        addChoosableFileFilter(filter);
        setFileView(new IconFileView());
        // setAccessory(new ImagePreview(this));  // The ImagePreview is available at the oracle site.
    }

} // end class IconFileChooser
