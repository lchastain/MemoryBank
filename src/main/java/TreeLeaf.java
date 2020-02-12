import javax.swing.*;
import java.awt.*;
import java.io.File;

public abstract class TreeLeaf extends JPanel {
    static final long serialVersionUID = 1L; // JPanel wants this but we will not serialize.

    protected boolean leafChanged; // Flag used to determine if saving might be necessary.
    JMenu myListMenu; // Child classes each have their own menu

    public TreeLeaf() {
        super(new BorderLayout());
    }

    //-----------------------------------------------------------------
    // Method Name:  prettyName
    //
    // A formatter for a filename specifier.  Note
    //   that this method name was chosen so as to not conflict with
    //   the 'getName' of the Component ancestor of this class.
    // Usage is intended for non-Calendar notegroups.
    //-----------------------------------------------------------------
    static String prettyName(String theLongName) {
        // Trim any leading/trailing whitespace.
        String thePrettyName = theLongName.trim();

        // Cut off the leading path specifier characters, if present.
        int i = thePrettyName.lastIndexOf(File.separatorChar);
        if (i >= 0) { // if it has the File separator character
            // then we only want the part after that
            thePrettyName = theLongName.substring(i + 1);
        }

        // Drop the JSON file extension
        i = thePrettyName.lastIndexOf(".json");
        if (i > 0) thePrettyName = thePrettyName.substring(0, i);

        // Cut off the leading group type (event_, todo_, search_, etc)
        i = thePrettyName.indexOf("_");
        if (i >= 0) thePrettyName = thePrettyName.substring(i + 1);

        return thePrettyName;
    } // end prettyName


    boolean getLeafChanged() {
        return leafChanged;
    } // end getGroupChanged


    // Used to enable or disable the 'undo' and 'save' menu items.  Called once when the
    // list menu is initially set and then later called repeatedly for every 'setGroupChanged'
    protected void adjustMenuItems(boolean b) {
        if (myListMenu == null) return; // Too soon.  Come back later.
        MemoryBank.debug("NoteGroup.adjustMenuItems <" + b + ">");

        // And now we adjust the Menu -
        JMenuItem theUndo = AppUtil.getMenuItem(myListMenu, "Undo All");
        if (theUndo != null) theUndo.setEnabled(b);
        JMenuItem theSave = AppUtil.getMenuItem(myListMenu, "Save");
        if (theSave != null) theSave.setEnabled(b);
    }

    // Returns the full path + name of the file containing the
    //   data for this group of notes.
    public abstract String getLeafFilename();

    static String basePath(String areaName) {
        return MemoryBank.userDataHome + File.separatorChar + areaName + File.separatorChar;
    }

    //----------------------------------------------------------------------
    // Method Name: preClose
    //
    // This should be called prior to closing.
    //----------------------------------------------------------------------
    void preClose() {
    } // end preClose


    //----------------------------------------------------------------
    // Method Name: setLeafChanged
    //
    // Called by all contexts that make a change to the data, each
    //   time a change is made.  Child classes can override if they
    //   need to intercept a state change, but in that case they
    //   should still call this super method so that group saving
    //   is done when needed and menu items are managed correctly.
    //----------------------------------------------------------------
    void setLeafChanged(boolean b) {
        leafChanged = b;
        adjustMenuItems(b);
    } // end setLeafChanged


    // Not all NoteGroups need to manage enablement of items in their menu but those
    // that do, all do the same things.  If this ever branches out into different
    // actions and/or menu items then they can override this and/or adjustMenuItems.
    void setListMenu(JMenu listMenu) {
        MemoryBank.debug("TreeLeaf.setListMenu: " + listMenu.getText());
        myListMenu = listMenu;
        adjustMenuItems(leafChanged); // set enabled state for 'undo' and 'save'.
    }
}
