/* ***************************************************************************
 * File:    NotePager.java
 * Author:  D. Lee Chastain
 *
 ****************************************************************************/
/**  NotePager provides a control for altering the page number of the
 NoteGroup.  It is created by the base NoteGroup class but relies
 on the child NoteGroup to 'add' it to their container.  The
 preferred location is the upper right corner of the header.  If
 this control determines that there is only one page of data to
 display, it will set its visibility to false.
 */

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class NotePager extends JPanel implements ActionListener, FocusListener, MouseListener {
    private static final long serialVersionUID = 1L;
    private int intPageSize;
    private int intGroupSize;
    private int intMaxPages;
    private int intCurrentPage;
    private int intPageFrom;
    private NoteGroup myNoteGroup;
    private LabelButton lbCurrentPage;
    private JTextField jtfThePageNum;

    NotePager(NoteGroup ng) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        myNoteGroup = ng;

        // intCurrentPage is initialized in reset().

        // Make a path to the images for the Alter Buttons
        char c = java.io.File.separatorChar;
        String iString = MemoryBank.logHome + c + "images" + c;

        LabelButton leftAb = new LabelButton();
        leftAb.setName("leftAb");
        leftAb.addMouseListener(this);
        leftAb.setIcon(new ImageIcon(iString + "left.gif"));
        leftAb.setPreferredSize(new Dimension(24, 24));

        LabelButton rightAb = new LabelButton();
        rightAb.setName("rightAb");
        rightAb.addMouseListener(this);
        rightAb.setIcon(new ImageIcon(iString + "right.gif"));
        rightAb.setPreferredSize(new Dimension(24, 24));

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

        add(leftAb);
        add(lbCurrentPage);
        add(jtfThePageNum);
        add(rightAb);

    } // end constructor

    public int getCurrentPage() {
        return intCurrentPage;
    }

    public int getHighestPage() {
        return intMaxPages;
    }

    public int getPageFrom() {
        return intPageFrom;
    }

    public String getSummary() {
        String s;
        int theFirst;
        int theLast;

        theFirst = (intCurrentPage - 1) * intPageSize + 1;
        theLast = intCurrentPage * intPageSize;

        if (theLast > intGroupSize) theLast = intGroupSize;
        if (intGroupSize == 0) theFirst = 0;

        s = "Showing " + String.valueOf(theFirst);
        s += " - " + String.valueOf(theLast);
        s += " of " + intGroupSize;

        return s;
    } // end getSummary


    //-------------------------------------------------------------------------
    // Method Name: reset
    //
    // This method is needed because this class is constructed by the
    //   NoteGroup before it has loaded any notes, so at that point
    //   the maximum number of pages cannot be determined.  Also, due
    //   to reload operations, the calculation may need to be
    //   regularly redone.  So, it is called by NoteGroup from the
    //   updateGroup method (after a group load).
    //-------------------------------------------------------------------------
    public void reset(int i) {
        intCurrentPage = i;
        intPageFrom = i;
        intPageSize = myNoteGroup.pageSize;
        intGroupSize = myNoteGroup.vectGroupData.size();

        // Calculate the maximum number of pages.
        if (intGroupSize == 0) intMaxPages = 1;
        else if ((intGroupSize % intPageSize) == 0) {
            intMaxPages = (intGroupSize / intPageSize);
            if (myNoteGroup.addNoteAllowed) intMaxPages++;
        } else intMaxPages = (intGroupSize / intPageSize) + 1;

        if (intMaxPages == 1) {
            // No paging control is needed if there is only one page.
            setVisible(false);
            return;
        } else { // we may be getting a reset from a page 1 overflow -
            setVisible(true);
        } // end if

        // Set the pager's background to the same color as its container,
        //   since other items in the container make it slightly 'higher'
        //   than the pager control.
//    if(getParent() != null)
//      setBackground(getParent().getBackground());
        //-------------------------------------------------------------

        setMiddleMessage();

        MemoryBank.dbg("NotePager.reset Page " + i + "\t Size: " + myNoteGroup.pageSize);
        MemoryBank.debug("\t Group size: " + myNoteGroup.vectGroupData.size());
    } // end reset


    //---------------------------------------------------------------------
    // Method Name: setMiddleMessage
    //
    // Resets the message on the middleButton.
    // Called by reset and any paging operation.
    //---------------------------------------------------------------------
    public void setMiddleMessage() {
        String s = String.valueOf(intCurrentPage);
        jtfThePageNum.setToolTipText(s);
        s += " of " + String.valueOf(intMaxPages);

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
            if (theNewPage <= intMaxPages) {
                intPageFrom = intCurrentPage;
                intCurrentPage = theNewPage;
                myNoteGroup.gotoPage(intCurrentPage);
                setMiddleMessage();
                myNoteGroup.pageNumberChanged();
                intPageFrom = intCurrentPage;
            } // end if
        } // end if

        jtf.transferFocus();
    } // end actionPerformed

    public void focusGained(FocusEvent e) {
        // Put the entry to the current page
        if (!jtfThePageNum.getText().trim().equals("")) {
            jtfThePageNum.setText(String.valueOf(intCurrentPage));
        } // end if
    } // end focusGained

    // This is only used for the JTextField.
    public void focusLost(FocusEvent e) {
        jtfThePageNum.setVisible(false);
        lbCurrentPage.setVisible(true);
    } // end focusLost


    //---------------------------------------------------------
    // MouseListener methods
    //---------------------------------------------------------
    public void mouseClicked(MouseEvent e) {
    } // end mouseClicked

    public void mouseEntered(MouseEvent me) {
        // System.out.println(e);

        Component source = (Component) me.getSource();
        String s = source.getName();

        String mouseMsg;
        if (s.equals("middleButton")) {
            mouseMsg = "Click here to go to a specific page.";
        } else if (s.equals("rightAb")) {
            mouseMsg = "Click here to see the next page of results.";
        } else if (s.equals("leftAb")) {
            mouseMsg = "Click here to see the previous page of results.";
        } else {
            mouseMsg = "I have no idea how to handle " + s;
        } // end if/else if

        myNoteGroup.setMessage(mouseMsg);
    } // end mouseEntered

    public void mouseExited(MouseEvent e) {
        myNoteGroup.setMessage("");
    }

    public void mousePressed(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getName();
        //LogUtil.localDebug(true);

        jtfThePageNum.setVisible(false);
        lbCurrentPage.setVisible(true);

        if (s.equals("middleButton")) {
            jtfThePageNum.setVisible(true);
            jtfThePageNum.requestFocusInWindow();
            lbCurrentPage.setVisible(false);
        } else {
//      myNoteGroup.transferFocusUpCycle();
            LogTree.ltTheTree.requestFocusInWindow();
            if (s.equals("leftAb")) {
                // System.out.println("Prev page");
                if (intCurrentPage > 1) {
                    intCurrentPage--;
                    myNoteGroup.gotoPage(intCurrentPage);
                } else {
                    intCurrentPage = intMaxPages;
                    myNoteGroup.gotoPage(intCurrentPage);
                } // end if
                setMiddleMessage();
                myNoteGroup.pageNumberChanged();
            }
            if (s.equals("rightAb")) {
                // System.out.println("Next page");
                if (intCurrentPage < intMaxPages) {
                    intCurrentPage++;
                    myNoteGroup.gotoPage(intCurrentPage);
                } else {
                    intCurrentPage = 1;
                    myNoteGroup.gotoPage(intCurrentPage);
                } // end if
                setMiddleMessage();
                myNoteGroup.pageNumberChanged();
            } // end if/else
        } // end if/else

        // The Group needs to know what page we are coming from
        //   when handling a paging event, but this value is
        //   already initialized in 'reset' and so only needs to
        //   be updated after a page change.
        intPageFrom = intCurrentPage;

        //LogUtil.localDebug(false);
    } // end mousePressed

    public void mouseReleased(MouseEvent e) {
    }
    //---------------------------------------------------------

} // end class NotePager
