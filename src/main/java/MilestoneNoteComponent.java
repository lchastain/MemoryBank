import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class MilestoneNoteComponent extends IconNoteComponent {
    private static final long serialVersionUID = 1L;
    private final DateTimeFormatter dtf;
    private static final YearView yvDateChooser;

    static Notifier optionPane;

    // The Members
    private MilestoneNoteData myMilestoneNoteData;
    private final MilestoneNoteComponent.NoteDateLabel noteDateLabel;
    private final IconButton theIconButton;

    static ImageIcon[] theIcons;

    static {
        // Normally just a wrapper for a JOptionPane, but tests may replace this
        // with their own instance, that would require no user interaction.
        optionPane = new Notifier() { };

        yvDateChooser = new YearView(); // Used by showDateChooser()

        //---------------------------
        // Make the milestone icons
        //---------------------------
        //IconInfo iconInfo = new IconInfo(DataArea.APP_ICONS);
        //theIcons = theIcons.getIcons();
        char c = File.separatorChar;
        String iString = MemoryBank.mbHome + c + "icons" + c + "Milestones" + c;
        File milestoneIconDir = new File(iString);
        String[] theFileList = milestoneIconDir.list();
        Arrays.sort(theFileList); // So that the icon assigned to a note does not change arbitrarily.
        theIcons = new ImageIcon[theFileList.length+1];

        theIcons[0] = null; // A 'blank' icon
        int index = 1;
        for(String filename: theFileList) {
            if(filename.endsWith(".txt")) continue;
            //System.out.println(filename);
//            ImageIcon nextIcon = new IconInfo(DataArea.APP_ICONS, filename);
            ImageIcon nextIcon = new ImageIcon(iString + filename);
            IconInfo.scaleIcon(nextIcon);
            theIcons[index++] = nextIcon;
        }

    } // end static section


    MilestoneNoteComponent(MilestoneNoteGroupPanel ng, int i) {
        super(ng, i);
        index = i;
        dtf = DateTimeFormatter.ofPattern("d MMM yyyy");

        noteDateLabel = new NoteDateLabel();
        theIconButton = new IconButton();

        //----------------------------------------------------------
        // Graphical elements
        //----------------------------------------------------------
        add(noteDateLabel, BorderLayout.WEST);
        add(theIconButton, BorderLayout.EAST);

        MemoryBank.trace();
    } // end constructor


    //-----------------------------------------------------------------
    // Method Name: clear
    //
    // Clears both the Graphical elements and the underlying data.
    //-----------------------------------------------------------------
    @Override
    protected void clear() {
        // We need to clear out our own members before clearing the base component.
        noteDateLabel.clear();  // Clear the Date Label
        if (theIconButton != null) theIconButton.clear();

        super.clear(); // This also sets the component 'initialized' to false.
        // And it leaves a 'gap' but we like that so no refresh here.
    } // end clear



    @Override
    NoteData getNoteData() {
        return myMilestoneNoteData;
    } // end getNoteData

    @Override
    protected void initialize() {
        super.initialize();

        myMilestoneNoteData.setNoteDate(LocalDate.now());

        resetDateLabel();
    } // end initialize


    @Override
    protected void makeDataObject() {
        myMilestoneNoteData = new MilestoneNoteData();
    } // end


    // Move the MilestoneNoteData to a Day Note.  This happens with Events as well,
    //   but here it is done at the individual Component level whereas Events
    //   are aged as a group and more than one might be affected with different
    //   effects on the visible interface, so the group 'refresh' is needed there
    //   and it is not possible to leave a gap because the entire list gets reloaded.
    //   But here - we just leave a gap.
    private void moveToDayNote(boolean useSelectedDate) {
        // MilestoneNoteData items have a 'slot' for a Subject, but no UI to set one.  So
        // now as it goes over to a DayNote, the Subject will be the name of the list
        // from which this item is being removed.
        myMilestoneNoteData.setSubjectString(myNoteGroupPanel.myNoteGroup.myGroupInfo.getGroupName());

        // Get the Date to which we will move this item - either one has been selected AND we want to use it,
        //  or there is always 'today'.
        LocalDate moveToDate;
        if (useSelectedDate) {
            moveToDate = myMilestoneNoteData.getNoteDate();
            MemoryBank.debug("Moving MilestoneNote to specified date: " + moveToDate.toString());
        } else {
            LocalDate today = LocalDate.now();
            MemoryBank.debug("Moving MilestoneNote to Today: " + today.toString());
            myMilestoneNoteData.setNoteDate(LocalDate.now());
        }

        // Convert the item to a DayNoteData, using the MilestoneNoteData-flavored constructor.
        DayNoteData dnd = new DayNoteData(myMilestoneNoteData);

        // Use the date to get the right group, then add the note to it.
        LocalDate theNoteDate = myMilestoneNoteData.getNoteDate();
        String groupName = CalendarNoteGroup.getGroupNameForDate(theNoteDate, GroupType.DAY_NOTES);
        NoteGroup theGroup = new GroupInfo(groupName, GroupType.DAY_NOTES).getNoteGroup();
        theGroup.appendNote(dnd);

        // Save the updated DayNoteGroup and clear our note line.
        theGroup.saveNoteGroup();
        clear();  // This creates a 'gap'.
    } // end moveToDayNote


    @Override
    protected void noteActivated(boolean noteIsActive) {
        if (!noteIsActive) {
            noteDateLabel.setInactive();
        } // end if
        super.noteActivated(noteIsActive);
    } // end noteActivated

    //----------------------------------------------------------
    // Method Name: resetComponent
    //
    // Called after a change to the encapsulated data, to show
    //   the visual effects of the change.
    //----------------------------------------------------------
    @Override
    protected void resetComponent() {
        resetDateLabel();
        theIconButton.setIconChoice(myMilestoneNoteData.getIconOrder());
        super.resetComponent(); // the note text
    } // end resetComponent


    // This method is called when initializing the component or updating the date.
    void resetDateLabel() {
        if(!initialized) return;
        LocalDate noteDate = myMilestoneNoteData.getNoteDate();

        if (noteDate == null) {
            // The following statement could be needed if a MilestoneNoteComponent had
            //   had its date cleared, and then it was being shifted up or down.
            noteDateLabel.setText("           ");  // enough room for 'dd MMM yyyy'
            return;
        } // end if

        String dateString = dtf.format(noteDate);
        noteDateLabel.setText(dateString);

    } // end resetDateLabel

    @Override
    void resetPanelStatusMessage(int textStatus) {
        String s = " ";

        switch (textStatus) {
            case NEEDS_TEXT:
                s = "Click here to enter text for this task.";
                break;
            case HAS_BASE_TEXT:
                s = "Double-click here to add details about this task.";
                break;
            case HAS_EXT_TEXT:
                // This gives away the 'hidden' text, if
                //   there is no primary (blue) text.
                s = "Double-click here to see/edit";
                s += " the additional details for this task.";
        } // end switch
        myManager.setStatusMessage(s);
    } // end resetPanelStatusMessage


    @Override
    void setEditable(boolean b) {
        super.setEditable(b);
        noteDateLabel.setEditable(b);
        theIconButton.setEditable(b);
    }


    @Override
    public void setNoteData(NoteData newNoteData) {
        if (newNoteData instanceof MilestoneNoteData) {  // same type, but cast is still needed
            setMilestoneNoteData((MilestoneNoteData) newNoteData);
        } else { // Not 'my' type, but we can make it so.
            setMilestoneNoteData(new MilestoneNoteData(newNoteData));
        }
    } // end setNoteData


    void setMilestoneNoteData(MilestoneNoteData newNoteData) {
        myMilestoneNoteData = newNoteData;

        // update visual components...
        initialized = true;  // without updating the 'lastModDate'
        resetComponent();
        setNoteChanged();
    } // end setMilestoneNoteData


    @Override
    protected void shiftDown() {
        if (noteDateLabel.isActive) {
            // subtract one day
            LocalDate ld = myMilestoneNoteData.getNoteDate();
            ld = ld.minusDays(1);
            myMilestoneNoteData.setNoteDate(ld);
            resetDateLabel();
            MilestoneNoteComponent.this.setNoteChanged();
        } else {
            myManager.shiftDown(index);
        } // end if
    } // end shiftDown

    @Override
    protected void shiftUp() {
        if (noteDateLabel.isActive) {
            // add one day
            LocalDate ld = myMilestoneNoteData.getNoteDate();
            ld = ld.plusDays(1);
            myMilestoneNoteData.setNoteDate(ld);
            resetDateLabel();
            MilestoneNoteComponent.this.setNoteChanged();
        } else {
            myManager.shiftUp(index);
        } // end if
    } // end shiftUp


    private void showDateChooser() {
        // Make a dialog window to choose a date from a Year.
        Frame f = JOptionPane.getFrameForComponent(this);
        JDialog tempwin = new JDialog(f, true);

        tempwin.getContentPane().add(yvDateChooser, BorderLayout.CENTER);
        tempwin.setTitle("Select a new date for this log entry.");
        tempwin.setSize(yvDateChooser.getPreferredSize());
        tempwin.setResizable(false);
        yvDateChooser.setDialog(tempwin, 1);

        // Center the dialog relative to the main frame.
        tempwin.setLocationRelativeTo(f);

        // Go modal -
        tempwin.setVisible(true);
    } // end showDateChooser

    // Exchange data content between this component and the input parameter.
    @Override
    public void swap(NoteComponent tnc) {
        // Get a reference to the two data objects
        MilestoneNoteData tnd1 = (MilestoneNoteData) this.getNoteData();
        MilestoneNoteData tnd2 = (MilestoneNoteData) tnc.getNoteData();

        // Note: getNoteData and setNoteData are working with references
        //   to data objects.  If you 'get' data from the NoteComponent
        //   into a local variable and then later clear the component, you have
        //   also just cleared the data in your local variable because you never
        //   had a separatate copy of the data object, just the reference to it.

        // So - copy the data objects.
        if (tnd1 != null) tnd1 = new MilestoneNoteData(tnd1);
        if (tnd2 != null) tnd2 = new MilestoneNoteData(tnd2);

        if (tnd1 == null) tnc.clear();
        else tnc.setNoteData(tnd1);

        if (tnd2 == null) this.clear();
        else this.setMilestoneNoteData(tnd2);

        myManager.setGroupChanged(true);
    } // end swap

    //---------------------------------------------------------
    // End of NoteComponent specific methods
    //---------------------------------------------------------

    //---------------------------------------------------------
    // Inner Classes -
    //---------------------------------------------------------
    protected class IconButton extends LabelButton implements MouseListener {
        private static final long serialVersionUID = 1L;

        public static final int minWidth = 40;

        private int theIconOrder;
        private int theOriginalStatus;

        public IconButton() {
            setOpaque(true);
            setIcon(theIcons[theIconOrder]);
        } // end StatusButton constructor

        public void clear() {
            setIcon(null);
            setIconChoice(0); // 0
        } // end clear


        // Do not let it grow to fill the available space in the container.
        public Dimension getMaximumSize() {
            Dimension d = super.getMaximumSize();
            return new Dimension(d.width, ICONNOTEHEIGHT);
        } // end getMaximumSize


        // Need to keep the height constant.
        public Dimension getPreferredSize() {
            return new Dimension(ICONNOTEHEIGHT + 4, ICONNOTEHEIGHT);
        } // end getPreferredSize

        public int getIconOrder() {
            return theIconOrder;
        }


        void setEditable(boolean b) {
            if(b) { // This limits us to only one mouseListener corresponding to 'this'.
                // The limitation is needed because this method is called whenever a page is (re-)loaded.
                MouseListener[] mouseListeners = getMouseListeners();
                boolean alreadyEditable = false;
                for (MouseListener ml : mouseListeners) {
                    if(ml.getClass() == this.getClass()) {
                        alreadyEditable = true;
                        break;
                    }
                }
                if (!alreadyEditable) { // Separate line, for debug clarity.
                    addMouseListener(this);
                }
            }
            // Luckily, if 'this' is not currently a MouseListener then the next line is a silent no-op,
            // and otherwise it does what we've asked.
            else removeMouseListener(this);
        }

        public void setIconChoice(int i) {
            if (!initialized) return;
            if (i < 0) return;

            theIconOrder = i % theIcons.length; // handle removals, even though we said not supported.
            myMilestoneNoteData.setIconOrder(i);
            setIcon(theIcons[theIconOrder]);
        } // end setIconOrder


        //---------------------------------------------------------
        // MouseListener methods
        //---------------------------------------------------------
        public void mouseClicked(MouseEvent e) {
            setActive();

            if (!initialized) {
                String s;
                s = "An milestone must have text before an icon can be set!";
                myManager.setStatusMessage(s);
                return;
            } // end if

            int i;
            if (e.isMetaDown()) {
                i = -1;
            } else {
                i = 1;
            } // end if

            // Alter the status, wraparound if called for.
            theIconOrder = theIconOrder + i;
            if (theIconOrder > theIcons.length-1) theIconOrder = 0;
            if (theIconOrder < 0) theIconOrder = theIcons.length-1;
            myMilestoneNoteData.setIconOrder(theIconOrder);

            // Now display the correct icon.
            setIcon(theIcons[theIconOrder]);
        } // end mouseClicked


        public void mouseEntered(MouseEvent e) {
            if (!initialized) return;

            // System.out.println(e);
            if (!myMilestoneNoteData.hasText()) return;
            String theMessage = "Click here to set a new icon";
            myManager.setStatusMessage(theMessage);
            theOriginalStatus = myMilestoneNoteData.getIconOrder();
        } // end mouseEntered


        public void mouseExited(MouseEvent e) {
            // System.out.println(e);
            myManager.setStatusMessage(" ");

            if (!initialized) return;

            // Update the source data, if applicable.
            //  This allows several 'clicks' to occur but no required list save,
            //  as long as the user leaves the status where they found it.
            if (theOriginalStatus != myMilestoneNoteData.getIconOrder()) {
                myManager.setGroupChanged(true);
            } // end if
        } // end mouseExited

        public void mousePressed(MouseEvent e) {
            setActive();
        } // end mousePressed

        public void mouseReleased(MouseEvent e) {
        }
    } // end class IconButton


    class NoteDateLabel extends JLabel {
        private static final long serialVersionUID = 1L;

        boolean isActive;
        int dateWidth = 100;
        MouseAdapter ma;

        NoteDateLabel() {
            isActive = false;
            setBorder(highBorder);
            setHorizontalAlignment(JLabel.CENTER);
            setFont(Font.decode("Dialog-bold-12"));

            ma = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    MemoryBank.event();
                    // For all clicks - including Single left
                    MilestoneNoteComponent.this.setActive();
                    if (!initialized) return;

                    int m = e.getModifiersEx();
                    if(e.getButton() == MouseEvent.BUTTON3) { // Click of right mouse button.
                        if (e.getClickCount() >= 2) return;  // We don't handle a double click on this component.
                        // Show the YearView data chooser
                        yvDateChooser.setView(LocalDate.now()); // In case the current date is a null.
                        yvDateChooser.setChoice(myMilestoneNoteData.getNoteDate());
                        showDateChooser();
                        LocalDate newDate = yvDateChooser.getChoice();
                        // Reselecting the same date has same effect as 'X'ing the dialog - no date change.
                        // To clear a date:  right-click the current selection, then close the dialog.
                        System.out.println("The date retrieved from the chooser: " + newDate);
                        myMilestoneNoteData.setNoteDate(newDate);
                        myNoteGroupPanel.myNoteGroup.setGroupChanged(true);
                        resetDateLabel();
                    } else { // Single Left Mouse Button
                        if (isActive) {  // This implements a 'toggle'
                            setBorder(highBorder);
                            isActive = false;
                        } else {
                            setBorder(lowBorder);
                            isActive = true;
                            if (getText().trim().equals("")) {
                                // This can happen if a previously initialized
                                //   note has had its date cleared.
                                myMilestoneNoteData.setNoteDate(LocalDate.now());
                                MilestoneNoteComponent.this.resetDateLabel();
                                MilestoneNoteComponent.this.setNoteChanged();
                            } // end if
                        } // end if
                    } // end if/else if
                } // end mouseClicked

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!initialized) return;
                    myManager.setStatusMessage("Left click mouse to activate, then " +
                            "shift up/down arrows to adjust date");
                } // end mouseEntered
            };
        } // end constructor

        // Clear both the visual and data elements of this Component
        private void clear() {
            setText("           ");  // enough room for 'dd MMM yyyy'
            setBorder(highBorder);
            isActive = false;
            if(myMilestoneNoteData != null) {
                myMilestoneNoteData.setNoteDate(null);
            }
        } // end clear

        public Dimension getPreferredSize() {
            return new Dimension(dateWidth, NOTEHEIGHT);
        } // end getPreferredSize


        // This may eventually be needed via archiving - leave until known.
        void setEditable(boolean b) {
            if(b) { // This limits us to only one mouseListener per NoteDateLabel.
                // The limitation is needed because mouse responsiveness needs to track along with editability.
                MouseListener[] mouseListeners = getMouseListeners();
                boolean alreadyEditable = false;
                for (MouseListener ml : mouseListeners) {
                    if(ml.equals(ma)) {
                        alreadyEditable = true;
                        break;
                    }
                }
                if (!alreadyEditable) { // Separate line, for debug clarity.
                    addMouseListener(ma);
                }
            }
            // If ma is not currently a MouseListener on this component then the next line is a silent no-op,
            // and otherwise it does what we've asked.
            else removeMouseListener(ma);
        }

        void setInactive() {
            setBorder(highBorder);
            isActive = false;
        } // end setInactive

    } // end class NoteDateLabel


} // end class MilestoneNoteComponent


//Embedded Data class
//-------------------------------------------------------------------



