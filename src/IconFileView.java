// File name: IconFileView.java   by D. Lee Chastain  8/09/03

// Description:  Modification of example code (free download) from
//   SUN that shows how to make a customized JFileChooser.
//   The modification mainly involves the addition of support for
//   Windows .ico files.

import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;

public class IconFileView extends FileView {

  //let the L&F FileView figure these out...
  public String getName(File f) { return null; }
  public String getDescription(File f) { return null; }
  public Boolean isTraversable(File f) { return null; }

  public String getTypeDescription(File f) {
    String extension = getExtension(f);
    String type = null;

    if (extension != null) {
        if (extension.equals("jpeg") ||
            extension.equals("jpg")) {
            type = "JPEG Image";
        } else if (extension.equals("gif")){
            type = "GIF Image";
        } else if (extension.equals("tiff") ||
                   extension.equals("tif")) {
            type = "TIFF Image";
        } else if (extension.equals("ico")){
            type = "ICO Image";
        } else if (extension.equals("png")){
            type = "PNG Image";
        } // end if extension is various
    } // end if extension not null
    return type;
  } // end getTypeDescription

    public Icon getIcon(File f) {
        String extension = getExtension(f);
        Icon icon = null;

        // System.out.println("Path is: " + f.getPath());
        if (extension != null) {
          if (extension.equals("jpeg") ||
                extension.equals("jpg")  ||
                extension.equals("gif")  ||
                extension.equals("tiff") ||
                extension.equals("tif")  ||
                extension.equals("ico")  ||
                extension.equals("png"))
              icon = new LogIcon(f.getPath());
        } // end if extension not null
        return icon;
    } // end getIcon

    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
          ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    } // end getExtension
}
