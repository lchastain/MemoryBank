import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;

public class IconFileChooser extends JFileChooser {
    private static final long serialVersionUID = 1L;
    static private final FileFilter filter;

    /* A note about the support for .bmp formats - while it has been added to the FileDataAccessor,
    it seems that the two I tried were still unsupported (16-bit, compressed) and for
    the one that I found that was supported, scaling was required and after that it was
    not visible in the chooser, and upon selection it threw an exception with a long
    stack trace that did not even trail back to Memory Bank source.  BMP icons (like all of them,
    actually - will need to be added / managed carefully.
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

    public IconFileChooser(String thePath) {
        super(thePath);
        setAcceptAllFileFilterUsed(false);
        addChoosableFileFilter(filter);
        setFileView(new IconFileView());
    }

} // end class IconFileChooser
