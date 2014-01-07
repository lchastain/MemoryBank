/* ***************************************************************************
 *
 * File:  SearchResultComponent.java
 *
 * Author:  D. Lee Chastain
 *
 ****************************************************************************/
/**  Representation of a single Day Note.
 */

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JComponent;

public class SearchResultComponent extends NoteComponent {
    private static final long serialVersionUID = 1L;

    // The Members
    private SearchResultData mySearchResultData;
    private FoundInButton fibTheFoundInButton;
    private LastModLabel lmlTheLastModLabel;
    private SearchResultGroup myNoteGroup; // status msgs & change notification

    SearchResultComponent(SearchResultGroup ng, int i) {
        super(ng, i);
        setLayout(new DndLayout());

        index = i;
        myNoteGroup = ng;

        setEditable(false);
        fibTheFoundInButton = new FoundInButton();
        lmlTheLastModLabel = new LastModLabel();

        //----------------------------------------------------------
        // Graphical elements
        //----------------------------------------------------------
        // Note: The dndLayout does not care about any name other
        //   than 'Stretch', but something must be provided.  Only
        //   one component can be the one to be stretched.
        add(fibTheFoundInButton, "fib");
        add(noteTextField, "Stretch"); // will resize along with container
        add(lmlTheLastModLabel, "sb");

        MemoryBank.init();
    } // end constructor


    //-----------------------------------------------------------------
    // Method Name: clear
    //
    // Clears both the Graphical elements and the underlying data.
    //-----------------------------------------------------------------
    public void clear() {
        super.clear();
        fibTheFoundInButton.clear();
        lmlTheLastModLabel.clear();
    } // end clear


    //-----------------------------------------------------------------
    // Method Name: getNoteData
    //
    //-----------------------------------------------------------------
    public NoteData getNoteData() {
        if (!initialized) return null;
        return mySearchResultData;
    } // end getNoteData

    public JComponent getFoundInButton() {
        return fibTheFoundInButton;
    }

    public JComponent getLastModLabel() {
        return lmlTheLastModLabel;
    }

    public void initialize() {
        super.initialize();
    } // end initialize


    //----------------------------------------------------------------
    // Method Name: resetColumnOrder
    //
    // Do not call this method if the columns are already in order;
    //   it just wastes cpu cycles.  Test for that condition in the
    //   calling context and only make the call if needed.
    //----------------------------------------------------------------
    public void resetColumnOrder(int theOrder) {
        String pos = String.valueOf(theOrder);
        // System.out.println("SearchResultComponent resetColumnOrder to " + pos);

        //   Note that now we do not provide the 'name' and so we will
        //   be going through the base layout class 'add' method.
        add(fibTheFoundInButton, pos.indexOf("1"));
        add(noteTextField, pos.indexOf("2"));
        add(lmlTheLastModLabel, pos.indexOf("3"));

        // This was needed after paging was implemented.
        noteTextField.transferFocusUpCycle();  // new 3/19/2008
    } // end resetColumnOrder


    //----------------------------------------------------------
    // Method Name: resetComponent
    //
    // Called after a change to the encapsulated data, to show
    //   the visual effects of the change.
    //----------------------------------------------------------
    protected void resetComponent() {
        super.resetComponent(); // the note text

        fibTheFoundInButton.setText(mySearchResultData.getFoundIn());

        // Get the Last Mod date
        Date d = mySearchResultData.getLastModDate();

        String strModDate;
        if (d == null) { // May happen for older (mine only) data -
            strModDate = "";
        } else {
            // Make a date formatter.
            SimpleDateFormat sdf = new SimpleDateFormat();
            sdf.applyPattern("MM/dd/yyyy");

            // Format the Last Mod date.
            strModDate = sdf.format(d);
        } // end if

        lmlTheLastModLabel.setText(strModDate);
    } // end resetComponent


    protected void resetMouseMessage(int textStatus) {
        String s;

        s = "Search result text is non-editable.  Go to the ";
        s += "original source if a change is needed.";
        myNoteGroup.setMessage(s);
    } // end resetMouseMessage


    //----------------------------------------------------------
    // Method Name: setNoteData
    //
    // Overrides the base class
    //----------------------------------------------------------
    public void setNoteData(NoteData newNoteData) {
        setNoteData((SearchResultData) newNoteData);
    } // end setNoteData


    //----------------------------------------------------------
    // Method Name: setNoteData
    //
    // Called by a NoteGroup during a load or a shift up/down.
    //----------------------------------------------------------
    public void setNoteData(SearchResultData newNoteData) {
        mySearchResultData = newNoteData;
        initialized = true;

        // update visual components....
        resetComponent();

        setNoteChanged();
    } // end setNoteData


    //------------------------------------------------------------------
    // Method Name: swap
    //
    //------------------------------------------------------------------
    public void swap(SearchResultComponent src) {
        // Get a reference to the two data objects
        SearchResultData srd1 = (SearchResultData) this.getNoteData();
        SearchResultData srd2 = (SearchResultData) src.getNoteData();

        // Note: getNoteData and setNoteData are working with references
        //   to data objects.  If you 'get' data into a local variable
        //   and then later clear the component, you have also just
        //   cleared the data in your local variable because you never had
        //   a separatate copy of the data object, just the reference to it.

        // So - copy the data objects.
        if (srd1 != null) srd1 = new SearchResultData(srd1);
        if (srd2 != null) srd2 = new SearchResultData(srd2);

        if (srd1 == null) src.clear();
        else src.setNoteData(srd1);

        if (srd2 == null) this.clear();
        else this.setNoteData(srd2);

        System.out.println("SearchResultComponent.swap");

        myNoteGroup.setGroupChanged();
    } // end swap

    //---------------------------------------------------------
    // End of NoteComponent specific methods
    //---------------------------------------------------------

    //---------------------------------------------------------
    // Inner Classes -
    //---------------------------------------------------------
    protected class FoundInButton extends LabelButton implements MouseListener {
        private static final long serialVersionUID = 1L;

        public static final int intWidth = 120;

        // Note: These two variables are used to obtain a finer granularity
        //   of mouse events than reported by the JVM - mouseReleased should
        //   not have an effect if there was a mouseExited.  getClickCount still
        //   reports a '1' in that case, so we use these vars to distinguish
        //   between a precise click (priority +/-) and a sliding click (ignored).
        boolean leftClicked = false;
        boolean rightClicked = false;

        public FoundInButton() {
            super("  ");

            setFont(Font.decode("Monospaced-bold-12"));
            addMouseListener(this);
        } // end FoundInButton constructor

        public void clear() {
        }

        //----------------------------------------------
        // Overridden AWT methods
        //----------------------------------------------
        public boolean isFocusPainted() {
            return false;
        }

        public boolean isFocusable() {
            return false;
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getPreferredSize() {
            if (!isVisible()) return new Dimension(0, 0);
            Dimension d = super.getPreferredSize();

            d.width = intWidth;
            return d;
        } // end getPreferredSize

        //---------------------------------------------------------
        // MouseListener methods
        //---------------------------------------------------------
        public void mouseClicked(MouseEvent e) {
            LogUtil.localDebug(true); // off in LogTree.selectFoundIn
            LogTree.ltTheTree.showFoundIn(mySearchResultData);
        } // end mouseClicked

        public void mouseEntered(MouseEvent e) {
            // System.out.println(e);
            if (!mySearchResultData.hasText()) return;

            String s;
            s = "Click here to go to the editable original source";
            myNoteGroup.setMessage(s);
        }

        public void mouseExited(MouseEvent e) {
            // System.out.println(e);
            rightClicked = false;
            leftClicked = false;
            myNoteGroup.setMessage(" ");
        } // end mouseExited

        public void mousePressed(MouseEvent e) {
            setActive();

            // System.out.println(e);

            if (e.isMetaDown()) {
                rightClicked = true;
                leftClicked = false;
            } else {
                leftClicked = true;
                rightClicked = false;
            } // end if
        } // end mousePressed

        public void mouseReleased(MouseEvent e) {
            // System.out.println(e);
            if (!(leftClicked || rightClicked)) return;

            if (leftClicked) {
                leftClicked = false;
            } else if (rightClicked) {
                rightClicked = false;
            } // end if

        } // end mouseReleased
        //---------------------------------------------------------

    } // end class FoundInButton


    protected class LastModLabel extends LabelButton {
        private static final long serialVersionUID = 1L;

        public static final int intWidth = 80;

        public LastModLabel() {
            super();

            // May resemble one, but not a Java method -
            this.removeMouseListener(); // Removes the 'depress' functionality

            this.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    setActive();
                }

                public void mouseEntered(MouseEvent e) {
                    // System.out.println(e);
                    if (!mySearchResultData.hasText()) return;

                    String s;
                    s = "This is the last date (prior to this search) ";
                    s += "that a change was made to this note.";
                    myNoteGroup.setMessage(s);
                }

                public void mouseExited(MouseEvent e) {
                    myNoteGroup.setMessage(" ");
                } // end mouseExited

            });
        } // end LastModLabel constructor


        public void clear() {
//      setIcon(null);
            setText("");
        } // end clear


        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();

            d.width = intWidth;
            return d;
        } // end getPreferredSize

    } // end class LastModLabel

} // end class SearchResultComponent


//Embedded Data class
//-------------------------------------------------------------------

class SearchResultData extends NoteData implements Serializable {
    static final long serialVersionUID = -1257203205705118689L;

    // The 'dateNoteWhen' member is used to hold the date 'choice' when
    //   handling a click on a button in the 'Found In' column, prior
    //   to changing to a calendar-based view.  Of course, not all
    //   sources are calendar-based, and those that are not, do not
    //   need this member to handle the click.  However, it is also
    //   used as the sort key when sorting by 'Found In', since the
    //   textual dates and file names that are displayed would not
    //   necessarily sort in the expected order.
    //   For Events, the 'dateEventStart' (if available) is kept here.
    //   For Todo items, the 'dateTodoItem' (if available) is kept.
    //   When sorting on 'Found In', if this date is still null then
    //   the sort treats it as a 'no key' and places it ????
    private Date dateNoteWhen;

    private File fileFoundIn;

    // Called during a search - the other members will be set
    //   explicitly, in subsequent method calls.
    public SearchResultData(NoteData nd) {
        super(nd);
    } // end constructor


    // called by swap - need to set all data members now.
    public SearchResultData(SearchResultData srd) {
        super(srd);
        dateNoteWhen = srd.getNoteDate();
        fileFoundIn = srd.getFileFoundIn();
    } // end constructor


    public File getFileFoundIn() {
        return fileFoundIn;
    }

    protected Date getNoteDate() {
        return dateNoteWhen;
    }

    //-----------------------------------------------------
    // Method Name: getFoundIn
    //
    // Returns a short, easily readable string indicating which file
    //   this result originally comes from.  For calendar-based
    //   sources, decided to use alpha months rather than numeric
    //   because it "reads" best and does not affect sorting
    //   which uses the 'dateNoteWhen'.
    //-----------------------------------------------------
    public String getFoundIn() {
        String retstr; // RETurn STRing

        String fname = fileFoundIn.getName();
        String fpath = fileFoundIn.getParent();

        retstr = fname; // as a default; will probably change, below.

        if (fname.endsWith(".todolist")) {
            retstr = fname.substring(0, fname.lastIndexOf('.'));
        } else if (fname.equals("UpcomingEvents")) {
            retstr = "Upcoming";
        } else if (!fpath.endsWith("MemoryBank")) {
            // If the path does not end at the top level data
            //   directory, then (at least at this writing) it
            //   means that we are down one of the calendar-
            //   based 'Year' paths.
            String strYear = fpath.substring(fpath.lastIndexOf(File.separatorChar) + 1);

            if (fname.startsWith("Y")) {
                retstr = strYear;
            } else {
                // We get the numeric Month from character
                //   positions 1-2 in the filename.
                String strMonthInt = fname.substring(1, 3);
                int intMonth = Integer.parseInt(strMonthInt);

                String[] monthNames = new String[]{"Jan", "Feb",
                        "Mar", "Apr", "May", "Jun", "Jul",
                        "Aug", "Sep", "Oct", "Nov", "Dec"};

                String strMonth = monthNames[intMonth - 1];

                if (fname.startsWith("M")) {
                    retstr = strMonth + " " + strYear;
                } else if (fname.startsWith("D")) {
                    // We get the numeric Day from character
                    //   positions 3-4 in the filename.
                    String strDay = fname.substring(3, 5);
                    retstr = strDay + " " + strMonth + " ";
                    retstr += strYear;
                } // end if
            } // end if
        } // end if

        return retstr;
    } // end getFoundIn

    public void setFileFoundIn(File f) {
        fileFoundIn = f;
    }

    public void setNoteDate(Date value) {
        dateNoteWhen = value;
    }

} // end class SearchResultData



