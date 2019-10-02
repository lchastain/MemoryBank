/*  An intermediate class, extended by both Day Notes and Events.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public abstract class IconNoteComponent extends NoteComponent {
    private static final long serialVersionUID = 1L;

    static final int ICONNOTEHEIGHT = 38; // 24 is too small for icons.

    // The Members
    NoteIcon noteIcon;

    // Private static values that are accessed from multiple contexts.
    private static IconFileChooser iconChooser;
    private static JCheckBoxMenuItem siombMi;
    private static JMenuItem sadMi;
    private static JMenuItem resetMi;
    private static JMenuItem blankMi;
    private static JPopupMenu iconPopup;

    // A reference to the container that holds this component
    private iconKeeper myContainer;


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

        // Initialize the Icon file chooser
        iconChooser = new IconFileChooser(MemoryBank.logHome + File.separatorChar + "icons");

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
        noteIcon.clear();       // Clear the Icon
        super.clear();
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
        setIcon(myContainer.getDefaultIcon());
        super.initialize();
    } // end initialize


    //-----------------------------------------------------
    // Method Name: setIcon
    //
    // This is the NoteComponent (vs NoteIcon/JLabel) method.
    //-----------------------------------------------------
    public void setIcon(AppIcon theIcon) {
        // We don't want to save the name of the default icon, and it should have
        //   already been scaled by the container for which it is the default.
        // Otherwise, use the description as the icon's file name, and scale the icon.
        if ((theIcon != myContainer.getDefaultIcon()) && (theIcon != null)) {
            String s = theIcon.getDescription();
            ((IconNoteData) getNoteData()).setIconFileString(s);
            noteIcon.theIconFile = s;
            AppIcon.scaleIcon(theIcon);
        } // end if

        noteIcon.setIcon(theIcon);
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
            setIcon(new AppIcon(infs));
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
        void showIconPopup(MouseEvent me) {
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
            switch (s) {
                case "Reset Icon":
                    myIconNoteData.setIconFileString(null);
                    myIconNoteData.setShowIconOnMonthBoolean(false);
                    setIcon(myContainer.getDefaultIcon());
                    break;
                case "Blank Icon":
                    myIconNoteData.setIconFileString("");
                    noteIcon.setIcon(null);
                    noteIcon.theIconFile = "";
                    myIconNoteData.setShowIconOnMonthBoolean(false);
                    break;
                case "Set As Default":

                    // Get a reference to the icon.
                    AppIcon tmpIcon;
                    tmpIcon = (AppIcon) noteIcon.getIcon();

                    // Set the description.
                    MemoryBank.dbg("The new default icon's description is: ");
                    if (tmpIcon == null) {
                        // The user is setting a 'blank' to be default.
                        tmpIcon = new AppIcon();
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

                    break;
                case "Show on Month":
                    myIconNoteData.setShowIconOnMonthBoolean(siombMi.getState());
                    break;
                default:  // ignore anything else
                    return;
            }

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
                System.out.print(""); // Don't care.
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
                    iconFileName = iconFileName.toLowerCase();

                    // Now copy the chosen file from 'Program Data' to user data.
                    // (if needed).  Once an icon has been 'chosen', it should never
                    // change, even if the source icon file does.  So - we preserve it
                    // in its original form, in the user's data location.  Of course,
                    // if a 'new' icon comes into system data, replacing one that used to have that
                    // name, this 'solution' will cause the app to always display the old
                    // one.  This obviously still needs work - perhaps need to also save
                    // the image..
                    File src = new File(iconFileName);
                    int iconsIndex = iconFileName.indexOf("icons");
                    String destFileName;
                    if(iconsIndex >= 0) {
                        destFileName = iconFileName.substring(iconsIndex);
                        destFileName = MemoryBank.userDataHome + File.separatorChar + destFileName;
                    } else {
                        // need to drop off the drive, convert filesep chars?
                        destFileName = MemoryBank.userDataHome + File.separatorChar + iconFileName;
                    }
                    System.out.println("destFileName = " + destFileName);
                    File dest = new File(destFileName);
                    String theParentDir = dest.getParent();
                    File f = new File(theParentDir);
                    if (!f.exists()) {
                        if (!f.mkdirs()) {
                            System.out.println("Error - Could not create directories: " + f.getAbsolutePath());
                        } // end if
                    }
                    if (!dest.exists()) {
                        MemoryBank.debug("  copying to " + destFileName);
                        AppUtil.copy(src, dest);
                    } // end if

                    IconNoteComponent.this.setIcon(new AppIcon(iconFileName));

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
