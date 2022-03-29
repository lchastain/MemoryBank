/*  An intermediate class, extended by both Day Notes and Events.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public abstract class IconNoteComponent extends NoteComponent {
    private static final long serialVersionUID = 1L;
    static final int ICONNOTEHEIGHT = 38; // 24 is too small for icons.

    // The Graphical Member
    JLabel theIconLabel;

    // The Listeners
    ActionListener actionListener;
    MouseListener mouseListener;

    // Private static values that are accessed from multiple contexts.
    private static final IconFileChooser iconChooser;
    private static final JCheckBoxMenuItem siombMi;
    static JMenuItem sadMi;
    private static final JMenuItem resetMi;
    static JMenuItem blankMi;
    private static final JPopupMenu iconPopup;

    // A reference to the container that holds this component
    private final IconKeeper myIconKeeper;


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
        iconChooser = new IconFileChooser(MemoryBank.mbHome + File.separatorChar + "icons");

    } // end static section


    IconNoteComponent(NoteComponentManager ng, int i) {
        super(ng, i);
        index = i;

        defineActionListener(); // Not needed until the popup menu is displayed.
        defineMouseListener();  // Added here in this constructor.

        if(ng instanceof IconKeeper) {
            myIconKeeper = (IconKeeper) ng;
        } else {
            myIconKeeper = null;
        }

        //------------------
        // Graphical elements
        //------------------
        noteTextField.setFont(Font.decode("DialogInput-bold-20"));

        theIconLabel = new JLabel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(ICONNOTEHEIGHT + 4, ICONNOTEHEIGHT);
            }
        };
        theIconLabel.setBorder(highBorder);
        theIconLabel.addMouseListener(mouseListener);

        add(theIconLabel, "East");
        //------------------

        MemoryBank.trace();
    } // end constructor


    //-----------------------------------------------------------------
    // Method Name: clear
    //
    // Clears both the Graphical elements and the underlying data.
    //-----------------------------------------------------------------
    protected void clear() {
        theIconLabel.setIcon(null); // this setIcon method is in JLabel
        super.clear();
    } // end clear

    void defineActionListener() {
        actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JMenuItem jm = (JMenuItem) e.getSource();
                String s = jm.getText();
                IconNoteData myIconNoteData = ((IconNoteData) getNoteData());
                switch (s) {
                    case "Reset Icon":
                        myIconNoteData.setIconFileString(null);
                        myIconNoteData.setShowIconOnMonthBoolean(false);
                        setIcon(myIconKeeper.getDefaultIcon());
                        break;
                    case "Blank Icon":
                        myIconNoteData.setIconFileString("");
                        theIconLabel.setIcon(null);
                        myIconNoteData.setShowIconOnMonthBoolean(false);
                        break;
                    case "Set As Default":
                        // Get a reference to the icon.
                        ImageIcon tmpIcon;
                        tmpIcon = (ImageIcon) theIconLabel.getIcon();

                        // Set the new default icon and tell the container to update,
                        //   which will reload all visual components.
                        myIconKeeper.setDefaultIcon(tmpIcon);

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
        };
    } // end defineActionListener

    void defineMouseListener() {
        mouseListener = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                MemoryBank.event();
                // For all clicks - including Single left
                IconNoteComponent.this.setActive();
                if (!initialized) return;

                int m = e.getModifiersEx();
                //if ((m & InputEvent.BUTTON3_DOWN_MASK) != 0) { // Click of right mouse button.
                if(e.getButton() == MouseEvent.BUTTON3) { // Click of right mouse button.
                    if (e.getClickCount() >= 2) return; // Single right click only, not a double.
                    showIconPopup(e);  // Show the popup menu
                } else if (e.getClickCount() == 2) {  // Double left button click
                    System.out.print(""); // Don't care.
                } else { // Single Left Mouse Button
                    // Some of the method calls below use the full
                    //   scope - without it, the method would only apply to
                    //   this inner component and not the overall one.
                    IconNoteComponent.this.setBorder(redBorder);
                    IconNoteComponent.this.repaint();

                    int returnVal = iconChooser.showDialog(theIconLabel, "Set Icon");
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        String iconFileName = iconChooser.getSelectedFile().getPath();
                        MemoryBank.debug("Chosen icon file: " + iconFileName);
                        IconNoteComponent.this.setIcon(new ImageIcon(iconFileName.toLowerCase()));

                        // Since an explicit Icon was chosen, default to showing on Month.
                        ((IconNoteData) getNoteData()).setShowIconOnMonthBoolean(true);
                        setNoteChanged();
                    } // end if

                    IconNoteComponent.this.setBorder(null);
                } // end if/else if
            } // end mouseClicked

            @Override
            public void mousePressed(MouseEvent e) {
                if (!initialized) return;
                setBorder(lowBorder);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!initialized) return;
                setBorder(highBorder);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!initialized) return;
                String s;
                s = "Left click here to set a new Icon";
                myManager.setStatusMessage(s);
            } // end mouseEntered

            @Override
            public void mouseExited(MouseEvent e) {
                myManager.setStatusMessage(" ");
            }
        };
    } // end defineMouseListener


    // Do not let it grow to fill the available space in the container.
    public Dimension getMaximumSize() {
        Dimension d = super.getMaximumSize();
        return new Dimension(d.width, ICONNOTEHEIGHT);
    } // end getMaximumSize


    // Need to keep the height constant.
    public Dimension getPreferredSize() {
        int minWidth = 100; // For the Text Field
        minWidth += theIconLabel.getPreferredSize().width;
        return new Dimension(minWidth, ICONNOTEHEIGHT);
    } // end getPreferredSize


    protected void initialize() {
        if(myIconKeeper != null) {
            setIcon(myIconKeeper.getDefaultIcon());
        }
        super.initialize();
    } // end initialize


    @Override
    void setEditable(boolean b) {
        super.setEditable(b);

        if(b) { // This limits us to only one mouseListener corresponding to 'mouseListener'.
            // The limitation is needed because this method is called whenever a page is (re-)loaded.
            MouseListener[] mouseListeners = getMouseListeners();
            boolean alreadyEditable = false;
            for (MouseListener ml : mouseListeners) {
                if(ml == mouseListener) {
                    alreadyEditable = true;
                    break;
                }
            }
            if (!alreadyEditable) {
                addMouseListener(mouseListener); // On its own line, for debug clarity.
            }
        }
        // Luckily, if mouseListener is not currently a MouseListener then the next line is a silent no-op,
        // and otherwise it does what we've asked.
        else removeMouseListener(mouseListener);



    }

    // This is the IconNoteComponent (vs NoteIcon/JLabel) method.
    public void setIcon(ImageIcon theIcon) {
        // The default icon should have already been scaled by the container for which it is the default.
        // Otherwise, scale the icon.
        if ((theIcon != myIconKeeper.getDefaultIcon()) && (theIcon != null)) {
            String s = theIcon.getDescription();  // Description was set in getImageIcon()
            // Make an IconInfo here?
            ((IconNoteData) getNoteData()).setIconFileString(s);
            IconInfo.scaleIcon(theIcon);
        } // end if

        theIconLabel.setIcon(theIcon);
    } // end setIcon


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
        sadMi.addActionListener(actionListener);

        ala = siombMi.getActionListeners();
        for (ActionListener al : ala) siombMi.removeActionListener(al);
        siombMi.addActionListener(actionListener);

        ala = resetMi.getActionListeners();
        for (ActionListener al : ala) resetMi.removeActionListener(al);
        resetMi.addActionListener(actionListener);

        ala = blankMi.getActionListeners();
        for (ActionListener al : ala) blankMi.removeActionListener(al);
        blankMi.addActionListener(actionListener);

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


    // Called after a change to the encapsulated data, to show the visual effects of the change.
    protected void resetComponent() {
        super.resetComponent();

        IconNoteData iconNoteData = (IconNoteData) getNoteData();

        String infs = ((IconNoteData) getNoteData()).getIconFileString();
        // The NoteData.iconFileString may be null if it was never
        // before set.  This is what allows it to go to the 'default'
        // icon, and if the default icon is later changed, theIconLabel
        // will automatically change to the new appearance.
        // But if it was explicitly cleared, it will be "" and will not
        // be affected by changes to the default.
        if (infs == null) {
           // MemoryBank.debug("IconNoteComponent resetComponent:  Icon string null - using default");
            if(myIconKeeper != null) {
                setIcon(myIconKeeper.getDefaultIcon());
            }
        } else {
            if (infs.trim().equals("")) {
                MemoryBank.debug("IconNoteComponent resetComponent:  Icon string empty - showing blank icon");
                return;
            } // end if

            MemoryBank.debug("Setting icon to: " + infs);
            setIcon(new ImageIcon(infs));
//            setIcon(iconNoteData.iconInfo.getImageIcon());

        } // end if
    } // end resetComponent

} // end class IconNoteComponent
