/*  NotePager provides a control for altering the page number of the
 NoteGroup.  It is created by the base NoteGroup class but relies
 on the child NoteGroup to 'setNotes' it to their container.  All NoteGroups
 have a 'header' panel, so the preferred location is the upper right
 corner of the header.  If this control determines that there is only
 one page of data to display, it will set its own visibility to false.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class NotePager extends JPanel implements ActionListener, FocusListener, MouseListener {
    private static final long serialVersionUID = 1L;
    private int intPageSize;
    private int intGroupSize;
    private int intMaxPages;
    private int currentPageNumber;
    private final NoteGroupPanel myNoteGroupPanel;
    private final LabelButton lbCurrentPage;
    private final JTextField jtfThePageNum;

    NotePager(NoteGroupPanel ng) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        myNoteGroupPanel = ng;
        currentPageNumber = 1;

        // Make images for the Alter Buttons
        IconInfo iconInfo = new IconInfo();
        iconInfo.dataArea = DataArea.IMAGES;
        iconInfo.iconFormat = "gif";

        LabelButton leftAb = new LabelButton();
        leftAb.setName("leftAb");
        leftAb.addMouseListener(this);
        iconInfo.iconName = "left";
        leftAb.setIcon(iconInfo.getImageIcon());
        leftAb.setPreferredSize(new Dimension(24, 24));

        lbCurrentPage = new LabelButton();
        lbCurrentPage.setName("middleButton");
        lbCurrentPage.addMouseListener(this);
        lbCurrentPage.setFont(Font.decode("Dialog-bold-10"));
        lbCurrentPage.setText("Page 0 of 0");
        lbCurrentPage.setPreferredSize(new Dimension(85, 24));

        jtfThePageNum = new JTextField();
        jtfThePageNum.setVisible(false);
        jtfThePageNum.addActionListener(this);
        jtfThePageNum.addFocusListener(this);
        jtfThePageNum.addKeyListener(new KeyAdapter() {
            // Ensure numeric entry only.
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();

                if (!((c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)
                        || (c == KeyEvent.VK_ENTER) || (c == KeyEvent.VK_TAB)
                        || (Character.isDigit(c)))) {
                    e.consume();
                }
            }
        });
        jtfThePageNum.setFont(Font.decode("Dialog-bold-14"));
        jtfThePageNum.setHorizontalAlignment(JTextField.CENTER);
        jtfThePageNum.setPreferredSize(new Dimension(85, 24));

        LabelButton rightAb = new LabelButton();
        rightAb.setName("rightAb");
        rightAb.addMouseListener(this);
        iconInfo.iconName = "right";
        rightAb.setIcon(iconInfo.getImageIcon());
        rightAb.setPreferredSize(new Dimension(24, 24));

        add(leftAb);
        add(lbCurrentPage);
        add(jtfThePageNum);
        add(rightAb);

    } // end constructor

    int getCurrentPage() {
        return currentPageNumber;
    }

    int getHighestPage() {
        intPageSize = myNoteGroupPanel.pageSize;
        intGroupSize = myNoteGroupPanel.myNoteGroup.noteGroupDataVector.size();

        // Calculate the maximum number of pages.
        if (intGroupSize == 0) intMaxPages = 1;  // No data; only one page means no pager control.
        else if ((intGroupSize % intPageSize) == 0) { // The data comes out to an even number of pages, exactly.
            intMaxPages = (intGroupSize / intPageSize);
            if (myNoteGroupPanel.getEditable()) {
                intMaxPages = (intGroupSize / intPageSize) + 1; // Add a page for 'growth'.
            }
        } else intMaxPages = (intGroupSize / intPageSize) + 1;  // A partial last page.

        return intMaxPages;
    }

    public String getSummary() {
        String s;
        int theFirst;
        int theLast;

        theFirst = (currentPageNumber - 1) * intPageSize + 1;
        theLast = currentPageNumber * intPageSize;

        if (theLast > intGroupSize) theLast = intGroupSize;
        if (intGroupSize == 0) theFirst = 0;

        s = "Showing " + theFirst;
        s += " - " + theLast;
        s += " of " + intGroupSize;

        return s;
    } // end getSummary


    // This method is used to set the current page number of the pager.  It is called
    //   by the constructors of NoteGroupPanel children where it sets the current
    //   page after a group load.  Loading occurs in a different context, not here.
    public void reset(int i) {
        currentPageNumber = i;

        getHighestPage(); // (re-)set the intMaxPages value.

        if (intMaxPages < 2) {
            // No paging control is needed if there is only one page.
            setVisible(false);
            return;
        } else { // we may be getting a reset from a page 1 overflow -
            setVisible(true);
        } // end if

        setMiddleMessage();

        MemoryBank.dbg("NotePager.reset Page " + i + "\t Size: " + myNoteGroupPanel.pageSize);
        MemoryBank.debug("\t Group size: " + myNoteGroupPanel.myNoteGroup.noteGroupDataVector.size());
    } // end reset


    //---------------------------------------------------------------------
    // Method Name: setMiddleMessage
    //
    // Resets the message on the middleButton.
    // Called by reset and any paging operation.
    //---------------------------------------------------------------------
    private void setMiddleMessage() {
        String s = String.valueOf(currentPageNumber);
        jtfThePageNum.setToolTipText(s);
        s += " of " + intMaxPages;

        if (s.length() > 10) s = "Pg " + s;
        else s = "Page " + s;

        lbCurrentPage.setText(s);
    } // end setMiddleMessage


    //========================================================================
    // Event Handlers
    //========================================================================
    public void actionPerformed(ActionEvent e) {
        JTextField jtf = (JTextField) e.getSource();
        if (!jtf.getText().trim().equals("")) {
            int theNewPage = Integer.parseInt(jtf.getText());
            if(theNewPage == currentPageNumber) { // No change?  Then no need.
                // This should not be a back-door to a 'refresh' operation.
                MemoryBank.debug("Explicit page nummber was set to current page!  Ignored.");
            } else if (theNewPage <= intMaxPages) {
                myNoteGroupPanel.pageFrom(currentPageNumber);
                currentPageNumber = theNewPage;
                myNoteGroupPanel.pageTo(currentPageNumber);
                setMiddleMessage();
                myNoteGroupPanel.pageNumberChanged();
            } // end if
        } // end if

        jtf.transferFocus();
    } // end actionPerformed

    // Focus events for the JTextField for page number entry
    //------------------------------------------------------
    public void focusGained(FocusEvent e) {
        // Put the entry to the current page
        if (!jtfThePageNum.getText().trim().equals("")) {
            jtfThePageNum.setText(String.valueOf(currentPageNumber));
        } // end if
    } // end focusGained

    public void focusLost(FocusEvent e) {
        jtfThePageNum.setVisible(false);
        lbCurrentPage.setVisible(true);
    } // end focusLost
    //------------------------------------------------------


    //---------------------------------------------------------
    // MouseListener methods
    //---------------------------------------------------------
    public void mouseClicked(MouseEvent e) {
    } // end mouseClicked

    public void mouseEntered(MouseEvent me) {
        System.out.println("MouseEntered event: " + me.toString());

        Component source = (Component) me.getSource();
        String s = source.getName();

        String mouseMsg = switch (s) {
            case "middleButton" -> "Click here to go to a specific page.";
            case "rightAb" -> "Click here to see the next page of results.";
            case "leftAb" -> "Click here to see the previous page of results.";
            default -> "I have no idea how to handle " + s;
        };

        myNoteGroupPanel.setStatusMessage(mouseMsg);
    } // end mouseEntered

    public void mouseExited(MouseEvent e) {
        myNoteGroupPanel.setStatusMessage("");
    }

    public void mousePressed(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getName();
        //AppUtil.localDebug(true);

        jtfThePageNum.setVisible(false);
        lbCurrentPage.setVisible(true);

        if (s.equals("middleButton")) {
            jtfThePageNum.setVisible(true);
            jtfThePageNum.requestFocusInWindow();
            lbCurrentPage.setVisible(false);
        } else {
            myNoteGroupPanel.pageFrom(currentPageNumber);
            if (s.equals("leftAb")) {
                // System.out.println("Prev page");
                if (currentPageNumber > 1) {
                    currentPageNumber--;
                    myNoteGroupPanel.pageTo(currentPageNumber);
                } else {
                    currentPageNumber = intMaxPages;
                    myNoteGroupPanel.pageTo(currentPageNumber);
                } // end if
                setMiddleMessage();
                myNoteGroupPanel.pageNumberChanged();
            }
            if (s.equals("rightAb")) {
                // System.out.println("Next page");
                if (currentPageNumber < intMaxPages) {
                    currentPageNumber++;
                    myNoteGroupPanel.pageTo(currentPageNumber);
                } else {
                    currentPageNumber = 1;
                    myNoteGroupPanel.pageTo(currentPageNumber);
                } // end if
                setMiddleMessage();
                myNoteGroupPanel.pageNumberChanged();
            } // end if/else
        } // end if/else

        // The Group needs to know what page we are coming from
        //   when handling a paging event, but this value is
        //   already initialized in 'reset' and so only needs to
        //   be updated after a page change.

        //AppUtil.localDebug(false);
    } // end mousePressed

    public void mouseReleased(MouseEvent e) {
    }
    //---------------------------------------------------------

} // end class NotePager
