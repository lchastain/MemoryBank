import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;

// This interface provides a wrapper for a JFileChooser with all methods defaulted to using
//   the wrapped JFileChooser as-is, to be used by the main application as though it was
//   using the standard class but the Test methods can replace targeted methods here so
//   that they can run without popping up dialogs that require user intervention.
public interface FileChooser {
    // A local JFileChooser is created below, using the default directory.
    // To change that, an instantiating context must also call setCurrentDirectory.
    // See the static section of TodoNoteGroup, for a usage example.
    JFileChooser jfilechooser = new JFileChooser();

    default int showDialog(Component parent, String approveButtonText) {
        return jfilechooser.showDialog(parent, approveButtonText);
    }

    default void addChoosableFileFilter(FileFilter filter) {
        jfilechooser.addChoosableFileFilter(filter);
    }

    default void setAcceptAllFileFilterUsed(boolean b) {
        jfilechooser.setAcceptAllFileFilterUsed(b);
    }

    default void setFileSystemView(FileSystemView fsv) {
        jfilechooser.setFileSystemView(fsv);
    }

    default File getCurrentDirectory() {
        return jfilechooser.getCurrentDirectory();
    }

    default void setCurrentDirectory(File dir) {
        jfilechooser.setCurrentDirectory(dir);
    }

    default File getSelectedFile() {
        return jfilechooser.getSelectedFile();
    }
}

