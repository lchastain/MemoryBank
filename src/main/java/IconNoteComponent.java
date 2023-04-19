/*  An intermediate class, extended by both Day Notes and Events.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serial;

public class IconNoteComponent extends NoteComponent {
    @Serial
    private static final long serialVersionUID = 1L;
    static final int ICONNOTEHEIGHT = 38; // 24 is too small for icons.

    // The Graphical Member
    JLabel theIconLabel;

    // The Listeners
    ActionListener actionListener;
    MouseListener mouseListener;

    // Private static values that are accessed from multiple contexts.
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
    } // end static section


    IconNoteComponent(NoteComponentManager ng, int i) {
        super(ng, i);
        componentHeight = ICONNOTEHEIGHT;
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
        // Since the text field gets higher to accomodate the icon, it is less unattractive
        //   to make the text larger, to fill the vertical space.  Multilines are smaller.
        noteTextField.setFont(Font.decode("DialogInput-bold-18"));

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
        // end actionPerformed
        actionListener = e -> {
            JMenuItem jm = (JMenuItem) e.getSource();
            String s = jm.getText();
            IconNoteData myIconNoteData = ((IconNoteData) getNoteData());
            switch (s) {
                case "Reset Icon" -> {
                    myIconNoteData.setIconFileString(null);
                    myIconNoteData.setShowIconOnMonthBoolean(false);
                    assert myIconKeeper != null;
                    setIcon(myIconKeeper.getDefaultIcon());
                }
                case "Blank Icon" -> {
                    myIconNoteData.setIconFileString("");
                    theIconLabel.setIcon(null);
                    myIconNoteData.setShowIconOnMonthBoolean(false);
                }
                case "Set As Default" -> {
                    // Get a reference to the icon.
                    ImageIcon tmpIcon;
                    tmpIcon = (ImageIcon) theIconLabel.getIcon();

                    // Set the new default icon and tell the container to update,
                    //   which will reload all visual components.
                    assert myIconKeeper != null;
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
                }
                case "Show on Month" -> myIconNoteData.setShowIconOnMonthBoolean(siombMi.getState());
                default -> {  // ignore anything else
                    return;
                }
            }
            setNoteChanged();
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

                if(e.getButton() == MouseEvent.BUTTON3) { // Click of right mouse button.
                    if (e.getClickCount() >= 2) return; // Handle a single right click only, not a double.
                    showIconPopup(e);  // Show the popup menu
                    setBorder(null);  // The popup puts an unseen border onto the label, which cuts into the icon.
                } else if (e.getClickCount() == 2) {  // Double left button click
                    System.out.print(""); // Don't care.
                } else { // Single Left Mouse Button
                    setBorder(redBorder);
                    repaint();

                    String theIconDescription = MemoryBank.dataAccessor.chooseIcon();

                    if(theIconDescription != null) {
                        IconNoteData myIconNoteData = ((IconNoteData) getNoteData());
                        myIconNoteData.setIconFileString(theIconDescription);
                        myIconNoteData.setShowIconOnMonthBoolean(true);
                        setIcon(myIconNoteData.getImageIcon());
                        setNoteChanged();
                    }
                    setBorder(null);  // This is the highlight of the full component, not the icon.
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

    @Override
    public int getComponentHeight() {
        return ICONNOTEHEIGHT;
    }

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

    // This is the IconNoteComponent (vs JLabel) method.
    public void setIcon(ImageIcon theIcon) {
        // The default icon should have already been scaled by the container for which it is the default.
        // Otherwise, scale the icon.
        if ((theIcon != myIconKeeper.getDefaultIcon()) && (theIcon != null)) {
            String s = theIcon.getDescription();  // Description was set by IconInfo.getImageIcon()
            // Make an IconInfo here?
            ((IconNoteData) getNoteData()).setIconFileString(s);
            IconInfo.scaleIcon(theIcon);
        } // end if

        theIconLabel.setIcon(theIcon);
    } // end setIcon


    //----------------------------------------------------------
    // Method Name: setNoteData
    //
    // Called by the overridden setNoteData and the 'swap' method.
    //----------------------------------------------------------
    private void setIconNoteData(IconNoteData newNoteData) {
        myNoteData = newNoteData;

        // update visual components...
        initialized = true;  // without updating the 'lastModDate'
        resetText();
        resetComponent();
        setNoteChanged();
    } // end setIconNoteData

    @Override
    public void setNoteData(NoteData newNoteData) {
        if (newNoteData instanceof IconNoteData) {  // same type, but cast is still needed
            setIconNoteData((IconNoteData) newNoteData);
        } else {
            setIconNoteData(new IconNoteData(newNoteData));
        }
    } // end setNoteData

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

        // Enable/Disable the items based on rules.

        // The icon can only be set as the default, if it isn't already.
        String s = ((IconNoteData) getNoteData()).getIconFileString();
        if (s == null) {
            sadMi.setEnabled(false);
            resetMi.setEnabled(false);
        } else {
            sadMi.setEnabled(true);
            resetMi.setEnabled(true);
            if (s.equals("")) blankMi.setEnabled(false);
        } // end if

        // Set the Menu Item checkbox for 'Show on Month'
        siombMi.setState(((IconNoteData) getNoteData()).getShowIconOnMonthBoolean());
        // But regardless of the boolean, only Day icons can show on the MonthView.
        siombMi.setVisible(this instanceof DayNoteComponent);

        // Blank is always enabled.
//        blankMi.setEnabled(true);

        iconPopup.show(me.getComponent(), me.getX(), me.getY());
    } // end showIconPopup


    @Override
    protected void makeDataObject() {
        myNoteData = new IconNoteData();
    } // end makeDataObject


    // Called after a change to the encapsulated data, to show the visual effects of the change.
    protected void resetComponent() {
        super.resetComponent();
        IconNoteData iconNoteData = (IconNoteData) getNoteData();
        ImageIcon theIcon = null;

        String infs = iconNoteData.getIconFileString(); // This (now) drops off the leading path!
        // The NoteData.iconFileString may be null if it was never
        // before set.  This is what allows it to go to the 'default'
        // icon, and if the default icon is later changed, noteIcon
        // will automatically change to the new appearance.
        // If it was explicitly cleared, it means that the user wants to see no
        // icon at all.  In that case it will be empty ("") and will not
        // be affected by subsequent changes to the default icon.
        if (infs == null) {
            // MemoryBank.debug("IconNoteComponent resetComponent:  Icon string null - using default");
            if(myIconKeeper != null) {
                theIcon = myIconKeeper.getDefaultIcon();
            }
        } else {
            if (infs.trim().equals("")) {
                MemoryBank.debug("IconNoteComponent resetComponent:  Icon string empty - showing blank icon");
            } else {
                MemoryBank.debug("Setting icon to: " + infs);
                theIcon = iconNoteData.getImageIcon();
            } // end if
        } // end if

        if(theIcon == null) return;
        setIcon(theIcon);
    } // end resetComponent

    @Override
    public void swap(NoteComponent inc) {
        // Get a reference to the two data objects
        IconNoteData ind1 = (IconNoteData) this.getNoteData();
        IconNoteData ind2 = (IconNoteData) inc.getNoteData();

        // Note: getNoteData and setNoteData are working with references
        //   to data objects.  If you 'get' data into a local variable
        //   and then later clear the component, you have also just
        //   cleared the data in your local variable because you never had
        //   a separate copy of the data object, just the reference to it.

        // So - copy the data objects.
        if (ind1 != null) ind1 = new IconNoteData(ind1);
        if (ind2 != null) ind2 = new IconNoteData(ind2);

        if (ind1 == null) inc.clear();
        else inc.setNoteData(ind1);

        if (ind2 == null) this.clear();
        else this.setNoteData(ind2);

        myManager.setGroupChanged(true);
    } // end swap


} // end class IconNoteComponent
