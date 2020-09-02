/*  NotePager provides a control for altering the page number of the
 NoteGroup.  It is created by the base NoteGroup class but relies
 on the child NoteGroup to 'add' it to their container.  All NoteGroups
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
    private int intPageFrom;
    private NoteGroupPanel myNoteGroupPanel;
    private LabelButton lbCurrentPage;
    private JTextField jtfThePageNum;

    NotePager(NoteGroupPanel ng) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        myNoteGroupPanel = ng;

        // Make a path to the images for the Alter Buttons
        char c = java.io.File.separatorChar;
        String iString = MemoryBank.logHome + c + "images" + c;

        LabelButton leftAb = new LabelButton();
        leftAb.setName("leftAb");
        leftAb.addMouseListener(this);
        leftAb.setIcon(new ImageIcon(iString + "left.gif"));
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
        rightAb.setIcon(new ImageIcon(iString + "right.gif"));
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
        return intMaxPages;
    }

    // This method was previously called from saveGroup, as a way to get the page number to use with unloadInterface.
    // Now, saveGroup does not do an unload; it happens one step earlier, in preClosePanel, and saving the data is NOT done
    // during a page event.  This better supports an 'undo', and it is part of breaking apart the paging event into
    // two operations (pageAway, pageTo) vs a single one (gotoPage).  Having two operations is more in line with other
    // awt/swing events like focusLost, focusGained.  Querying the current page number should NOT be done during the
    // paging events themselves, and neither should saving the group.  These are now separate activities.
    // This method and note may be removed after the current paging problems are solved, test(s) in place, and there
    // has been at least one git commit of this note.  Then, the var itself can also probably go away.
    int getPageFrom() {
        return intPageFrom;
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


    //-------------------------------------------------------------------------
    // Method Name: reset
    //
    // This method is used set the current page number and to
    //   calculate the maximum number of pages.  It is called
    //   by NoteGroup.updateGroup() where it sets the current
    //   page to 1, both before and after a group load.
    //-------------------------------------------------------------------------
    public void reset(int i) {
        currentPageNumber = i;
        intPageFrom = i;
        intPageSize = myNoteGroupPanel.pageSize;
        intGroupSize = myNoteGroupPanel.groupDataVector.size();

        // Calculate the maximum number of pages.
        if (intGroupSize == 0) intMaxPages = 1;  // No data; only one page means no pager control.
        else if ((intGroupSize % intPageSize) == 0) { // The data comes out to an even number of pages, exactly.
            intMaxPages = (intGroupSize / intPageSize);
            if (myNoteGroupPanel.editable) {
                intMaxPages = (intGroupSize / intPageSize) + 1; // Add a page for 'growth'.
            }
        } else intMaxPages = (intGroupSize / intPageSize) + 1;  // A partial last page.

        if (intMaxPages == 1) {
            // No paging control is needed if there is only one page.
            setVisible(false);
            return;
        } else { // we may be getting a reset from a page 1 overflow -
            setVisible(true);
        } // end if

        setMiddleMessage();

        MemoryBank.dbg("NotePager.reset Page " + i + "\t Size: " + myNoteGroupPanel.pageSize);
        MemoryBank.debug("\t Group size: " + myNoteGroupPanel.groupDataVector.size());
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
                myNoteGroupPanel.pageAway(currentPageNumber);
                intPageFrom = currentPageNumber;
                currentPageNumber = theNewPage;
                myNoteGroupPanel.pageTo(currentPageNumber);
                setMiddleMessage();
                myNoteGroupPanel.pageNumberChanged();
                intPageFrom = currentPageNumber;
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

        String mouseMsg;
        switch (s) {
            case "middleButton":
                mouseMsg = "Click here to go to a specific page.";
                break;
            case "rightAb":
                mouseMsg = "Click here to see the next page of results.";
                break;
            case "leftAb":
                mouseMsg = "Click here to see the previous page of results.";
                break;
            default:
                mouseMsg = "I have no idea how to handle " + s;
                break;
        }

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
//            AppTreePanel.theInstance.requestFocusInWindow();  // 1/4/2020 - this tripped up a test, so seeing if we can live without.
            myNoteGroupPanel.pageAway(currentPageNumber);
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
        intPageFrom = currentPageNumber;

        //AppUtil.localDebug(false);
    } // end mousePressed

    public void mouseReleased(MouseEvent e) {
    }
    //---------------------------------------------------------

} // end class NotePager
