/* ***************************************************************************
 * File:    IconNoteComponent.java
 * Author:  D. Lee Chastain
 *
 ****************************************************************************/
/**  An intermediate class, extended by both Day Notes and Events.
 */

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.Serializable;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public abstract class IconNoteComponent extends NoteComponent {
    private static final long serialVersionUID = 1L;

    public static final int ICONNOTEHEIGHT = 38; // 24 is too small for icons.

    // The Members
    protected NoteIcon noteIcon;

    // Private static values that are accessed from multiple contexts.
    protected static JFileChooser iconChooser;
    protected static JCheckBoxMenuItem siombMi;
    protected static JMenuItem sadMi;
    protected static JMenuItem resetMi;
    protected static JMenuItem blankMi;
    protected static JDialog tempwin;
    protected static JPopupMenu iconPopup;

    // A reference to the container that holds this component
    protected iconKeeper myContainer;


    static {
        //-----------------------------------
        // Create the popup menu.
        //-----------------------------------

        iconPopup = new JPopupMenu();
        iconPopup.setFocusable(false);

        //--------------------------------------------
        // Define the popup menu for an IconNoteComponent
        //--------------------------------------------
        sadMi = new JMenuItem("Set As Default");
        iconPopup.add(sadMi);
        siombMi = new JCheckBoxMenuItem("Show on Month");
        iconPopup.add(siombMi);
        resetMi = new JMenuItem("Reset Icon");
        iconPopup.add(resetMi);
        blankMi = new JMenuItem("Blank Icon");
        iconPopup.add(blankMi);

        //--------------------------------------------
        // Initialize the Icon file chooser
        //--------------------------------------------
        iconChooser = new JFileChooser(MemoryBank.logHome + "/icons");
        javax.swing.filechooser.FileFilter filter;
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
        iconChooser.addChoosableFileFilter(filter);
        iconChooser.setAcceptAllFileFilterUsed(false);
        iconChooser.setFileView(new IconFileView());
        //--------------------------------------------
    } // end static section


    IconNoteComponent(NoteGroup ng, int i) {
        super(ng, i);
        index = i;

        myContainer = (iconKeeper) ng;

        //------------------
        // Graphical elements
        //------------------
        noteTextField.setFont(Font.decode("DialogInput-bold-20"));

        noteIcon = new NoteIcon();
        add(noteIcon, "East");
        //------------------

        MemoryBank.init();
    } // end constructor


    //-----------------------------------------------------------------
    // Method Name: clear
    //
    // Clears both the Graphical elements and the underlying data.
    //-----------------------------------------------------------------
    protected void clear() {
        super.clear();
        noteIcon.clear();       // Clear the Icon
    } // end clear


    // Do not let it grow to fill the available space in the container.
    public Dimension getMaximumSize() {
        Dimension d = super.getMaximumSize();
        return new Dimension(d.width, ICONNOTEHEIGHT);
    } // end getMaximumSize


    // Need to keep the height constant.
    public Dimension getPreferredSize() {
        int minWidth = 100; // For the Text Field
        minWidth += noteIcon.getPreferredSize().width;
        return new Dimension(minWidth, ICONNOTEHEIGHT);
    } // end getPreferredSize


    protected void initialize() {
        super.initialize();
        setIcon(myContainer.getDefaultIcon());
    } // end initialize


    //-----------------------------------------------------
    // Method Name: setIcon
    //
    // This is the NoteComponent (vs NoteIcon/JLabel) method.
    //-----------------------------------------------------
    public void setIcon(LogIcon li) {
        if ((li != myContainer.getDefaultIcon()) && (li != null)) {
            // Do not save the default icon's filename.
            //  (and scaling should have been done in the myNoteGroup).
            String s = li.getDescription();
            ((IconNoteData) getNoteData()).setIconFileString(s);
            noteIcon.theIconFile = s;
            li = LogIcon.scaleIcon(li);
        } // end if

        noteIcon.setIcon(li);
    } // end setIcon


    //----------------------------------------------------------
    // Method Name: resetComponent
    //
    // Called after a change to the encapsulated data, to show
    //   the visual effects of the change.
    //----------------------------------------------------------
    protected void resetComponent() {
        super.resetComponent();

        String infs = ((IconNoteData) getNoteData()).getIconFileString();
        // The NoteData.iconFileString may be null if it was never
        // before set.  This is what allows it to go to the 'default'
        // icon, and if the default icon is later changed, noteIcon
        // will automatically change to the new appearance.
        // If it was explicitly cleared, it will be "" and will not
        // be affected by changes to the default.
        MemoryBank.dbg("IconNoteComponent setNoteData:  ");
        if (infs == null) {
            MemoryBank.debug("Icon string null - using default");
            setIcon(myContainer.getDefaultIcon());
        } else {
            if (infs.trim().equals("")) {
                MemoryBank.debug("Icon string empty - showing blank icon");
                return;
            } // end if

            MemoryBank.debug("Setting icon to: " + infs);
            setIcon(new LogIcon(infs));
        } // end if
    } // end resetComponent


    protected class NoteIcon extends JLabel implements
            ActionListener, MouseListener {

        private static final long serialVersionUID = -7694729396552635645L;

        private String theIconFile;

        NoteIcon() {
            super();
            addMouseListener(this);
            setBorder(highBorder);
        } // end constructor

        // Need to keep the height constant.
        public Dimension getPreferredSize() {
            return new Dimension(ICONNOTEHEIGHT + 4, ICONNOTEHEIGHT);
        }

        // Clear the NoteIcon
        private void clear() {
            setIcon(null); // this setIcon method is in JLabel
            theIconFile = null;
        } // end clear


        //------------------------------------------------------------
        // Method Name:  showIconPopup
        //
        // There is only one of each unique menu item for the entire
        //   IconNoteComponent class, no matter how many are created
        //   and inserted into a NoteGroup.  So, each time the menu
        //   is displayed, any previous component listeners must
        //   first be removed and then this one is added.
        //------------------------------------------------------------
        public void showIconPopup(MouseEvent me) {
            ActionListener[] ala;

            ala = sadMi.getActionListeners();
            for (ActionListener al : ala) sadMi.removeActionListener(al);
            sadMi.addActionListener(this);

            ala = siombMi.getActionListeners();
            for (ActionListener al : ala) siombMi.removeActionListener(al);
            siombMi.addActionListener(this);

            ala = resetMi.getActionListeners();
            for (ActionListener al : ala) resetMi.removeActionListener(al);
            resetMi.addActionListener(this);

            ala = blankMi.getActionListeners();
            for (ActionListener al : ala) blankMi.removeActionListener(al);
            blankMi.addActionListener(this);

            // Set the Menu Item checkbox for 'Show on Month'
            siombMi.setState(((IconNoteData) getNoteData()).getShowIconOnMonthBoolean());

            // Enable/Disable the items based on rules.
            blankMi.setEnabled(true);
            String s = ((IconNoteData) getNoteData()).getIconFileString();
            if (s == null) {
                sadMi.setEnabled(false);
                resetMi.setEnabled(false);
            } else {
                sadMi.setEnabled(true);
                resetMi.setEnabled(true);
                if (s.equals("")) blankMi.setEnabled(false);
            } // end if

            iconPopup.show(me.getComponent(), me.getX(), me.getY());
        } // end showIconPopup


        //---------------------------------------------------------
        // Action handler for NoteIcon popup menu items
        //---------------------------------------------------------
        public void actionPerformed(ActionEvent e) {
            JMenuItem jm = (JMenuItem) e.getSource();
            String s = jm.getText();
            IconNoteData myIconNoteData = ((IconNoteData) getNoteData());
            if (s.equals("Reset Icon")) {
                myIconNoteData.setIconFileString(null);
                myIconNoteData.setShowIconOnMonthBoolean(false);
                setIcon(myContainer.getDefaultIcon());
            } else if (s.equals("Blank Icon")) {
                myIconNoteData.setIconFileString("");
                noteIcon.setIcon(null);
                noteIcon.theIconFile = "";
                myIconNoteData.setShowIconOnMonthBoolean(false);
            } else if (s.equals("Set As Default")) {

                // Get a reference to the icon.
                LogIcon tmpIcon = null;
                tmpIcon = (LogIcon) noteIcon.getIcon();

                // Set the description.
                MemoryBank.dbg("The new default icon's description is: ");
                if (tmpIcon == null) {
                    // The user is setting a 'blank' to be default.
                    tmpIcon = new LogIcon();
                    MemoryBank.debug("<blank>");
                } else {
                    // The description did not come thru when
                    //   getting the Icon from a JLabel, above.
                    // So, we get it from the NoteIcon data.
                    tmpIcon.setDescription(noteIcon.theIconFile);
                    MemoryBank.debug(noteIcon.theIconFile);
                } // end if

                // Set the new default icon and tell the container to update,
                //   which will reload all visual components.
                myContainer.setDefaultIcon(tmpIcon);

                // Adjust underlying data.
                // Now that this icon is the default -
                // ------------------------
                // Do not show it on Month.  We don't need to unset the
                //   menu item check box, since that is set each time
                //   prior to menu display, based on the underlying data.
                myIconNoteData.setShowIconOnMonthBoolean(false);

                // Make sure the data indicates that this component
                //   should use the 'default' icon.
                myIconNoteData.setIconFileString(null);

            } else if (s.equals("Show on Month")) {
                myIconNoteData.setShowIconOnMonthBoolean(siombMi.getState());
            } else { // ignore anything else
                return;
            } // end if/else

            setNoteChanged();
        } // end actionPerformed

        //---------------------------------------------------------
        // MouseListener methods for the NoteIcon
        //---------------------------------------------------------
        public void mouseClicked(MouseEvent e) {
            MemoryBank.event();
            // For all clicks - including Single left
            IconNoteComponent.this.setActive();
            if (!initialized) return;

            int m = e.getModifiers();
            // Single click, right mouse button.
            if ((m & InputEvent.BUTTON3_MASK) != 0) {
                if (e.getClickCount() >= 2) return;

                showIconPopup(e);  // Show the popup menu

                // Double click, left mouse button.
            } else if (e.getClickCount() == 2) {
                // Don't care.
            } else { // Single Left Mouse Button
                // Some of the method calls below use the full
                //   scope - without it, the method would only apply to
                //   this inner component and not the overall one.
                IconNoteComponent.this.setBorder(redBorder);
                IconNoteComponent.this.repaint();

                int returnVal = iconChooser.showDialog(this, "Set Icon");
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String iconFileName = iconChooser.getSelectedFile().getPath();
                    MemoryBank.debug("Chosen icon file: " + iconFileName);

                    // Now copy the chosen file from 'Program Data' to user data.
                    // (if needed)
                    File src = new File(iconFileName);
                    int iconsIndex = iconFileName.indexOf("icons");
                    String destFileName = iconFileName.substring(iconsIndex);
                    destFileName = MemoryBank.userDataDirPathName + "/" + destFileName;
                    File dest = new File(destFileName);
                    String theParentDir = dest.getParent();
                    File f = new File(theParentDir);
                    if (!f.exists()) f.mkdirs();
                    if (!dest.exists()) {
                        MemoryBank.debug("  copying to " + destFileName);
                        LogUtil.copy(src, dest);
                    } // end if

                    IconNoteComponent.this.setIcon(new LogIcon(iconFileName));

                    // Since an explicit Icon was chosen, default to showing on Month.
                    ((IconNoteData) getNoteData()).setShowIconOnMonthBoolean(true);
                    setNoteChanged();
                } // end if

                IconNoteComponent.this.setBorder(null);
            } // end if/else if
        } // end mouseClicked

        public void mouseEntered(MouseEvent e) {
            if (!initialized) return;
            String s;
            s = "Left click here to set a new Icon";
            myNoteGroup.setMessage(s);
        } // end mouseEntered

        public void mouseExited(MouseEvent e) {
            myNoteGroup.setMessage(" ");
        }

        public void mousePressed(MouseEvent e) {
            if (!initialized) return;
            setBorder(lowBorder);
        }

        public void mouseReleased(MouseEvent e) {
            if (!initialized) return;
            setBorder(highBorder);
        }
    } // end class NoteIcon

} // end class IconNoteComponent


// Embedded Data class
//-------------------------------------------------------------------
//
class IconNoteData extends NoteData implements Serializable {
    private static final long serialVersionUID = -4747292791676343443L;

    protected String iconFileString;
    protected boolean showIconOnMonthBoolean;

    public IconNoteData() {
        super();
    } // end constructor


    // The copy constructor (clone)
    public IconNoteData(IconNoteData ind) {
        super(ind);

        iconFileString = ind.iconFileString;
        showIconOnMonthBoolean = ind.showIconOnMonthBoolean;
    } // end constructor


    protected void clear() {
        super.clear();
        iconFileString = null;
        showIconOnMonthBoolean = false;
    } // end clear


    public String getIconFileString() {
        return iconFileString;
    }

    public boolean getShowIconOnMonthBoolean() {
        return showIconOnMonthBoolean;
    }

    public void setIconFileString(String val) {
        iconFileString = val;
    }

    public void setShowIconOnMonthBoolean(boolean val) {
        showIconOnMonthBoolean = val;
    }

} // end class IconNoteData


