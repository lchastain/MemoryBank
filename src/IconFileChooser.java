import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;

public class IconFileChooser extends JFileChooser {
    static private FileFilter filter;

    static {
        filter = new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f != null) {
                    if (f.isDirectory()) return true;
                    String filename = f.getName().toLowerCase();
                    int i = filename.lastIndexOf('.');
                    if (i > 0 && i < filename.length() - 1) {
                        String extension = filename.substring(i + 1);
                        if (extension.equals("tiff") ||
                                extension.equals("tif") ||
                                extension.equals("gif") ||
                                extension.equals("jpeg") ||
                                extension.equals("jpg") ||
                                extension.equals("ico") ||
                                extension.equals("bmp") ||
                                extension.equals("png"))
                            return true;

                    } // end if
                } // end if
                return false;
            } // end accept

            public String getDescription() {
                return "icon images";
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
