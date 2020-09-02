/*  Representation of a single Search Result.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SearchResultComponent extends NoteComponent {
    private static final long serialVersionUID = 1L;

    // The Members
    private SearchResultData mySearchResultData;
    private FoundInButton fibTheFoundInButton;
    private LastModLabel lastModLabel;
    private SearchResultGroupPanel myNoteGroup; // status msgs & change notification

    SearchResultComponent(SearchResultGroupPanel ng, int i) {
        super(ng, i);
        setLayout(new DndLayout());

        index = i;
        myNoteGroup = ng;
        fibTheFoundInButton = new FoundInButton();
        lastModLabel = new LastModLabel();

        // Since a SearchResult should never be modified we can do this once here,
        // unlike the Consolidated View for Events which needs to set the static
        // boolean both before and after construction because all other instances
        // of an Event List except that one should remain editable.
        noteTextField.setEditable(false);

        //----------------------------------------------------------
        // Graphical elements
        //----------------------------------------------------------
        // Note: The dndLayout does not care about any component name other
        //   than 'Stretch', but something must be provided for each one.
        //   Only one component can be the one to be stretched.
        add(fibTheFoundInButton, "foundIn");
        add(noteTextField, "Stretch"); // will resize along with container
        add(lastModLabel, "lastMod");

        MemoryBank.trace();
    } // end constructor


    //-----------------------------------------------------------------
    // Method Name: clear
    //
    // Clears both the Graphical elements and the underlying data.
    //-----------------------------------------------------------------
    public void clear() {
        super.clear();
        fibTheFoundInButton.clear();
        lastModLabel.clear();
    } // end clear


    @Override
    public NoteData getNoteData() {
//        if (!initialized) return null;
        return mySearchResultData;
    } // end getNoteData

    JComponent getFoundInButton() {
        return fibTheFoundInButton;
    }

    JComponent getLastModLabel() {
        return lastModLabel;
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
        add(lastModLabel, pos.indexOf("3"));

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

        // This happens during loadInterface, a lot.
        // None of the non-visible notes on the page ever got a data object.
        if(mySearchResultData == null) {
            return;
        }

        fibTheFoundInButton.setText(mySearchResultData.getFoundIn());

        // Get the Last Mod date
        ZonedDateTime zdtLastModDate = mySearchResultData.getLastModDate();

        // Make a date formatter.
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        // Format the Last Mod date and set the label text.
        assert zdtLastModDate != null;
        String strModDate = dtf.format(zdtLastModDate);
        lastModLabel.setText(strModDate);
    } // end resetComponent

    @Override
    protected void resetNoteStatusMessage(int textStatus) {
        String s;

        s = "Search result text is non-editable.  Go to the ";
        s += "original source if a change is needed.";
        myNoteGroup.setStatusMessage(s);
    } // end resetNoteStatusMessage


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

        myNoteGroup.setGroupChanged(true);
    } // end swap

    //---------------------------------------------------------
    // End of NoteComponent specific methods
    //---------------------------------------------------------

    //---------------------------------------------------------
    // Inner Classes -
    //---------------------------------------------------------
    protected class FoundInButton extends LabelButton implements MouseListener {
        private static final long serialVersionUID = 1L;

        static final int intWidth = 120;

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
            AppUtil.localDebug(true);
            AppTreePanel.theInstance.showFoundIn(mySearchResultData);
            AppUtil.localDebug(false);
        } // end mouseClicked

        public void mouseEntered(MouseEvent e) {
            // System.out.println(e);
            if (!mySearchResultData.hasText()) return;

            String s;
            s = "Click here to go to the editable original source";
            myNoteGroup.setStatusMessage(s);
        }

        public void mouseExited(MouseEvent e) {
            // System.out.println(e);
            rightClicked = false;
            leftClicked = false;
            myNoteGroup.setStatusMessage(" ");
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
            } else {
                rightClicked = false;
            } // end if

        } // end mouseReleased
        //---------------------------------------------------------

    } // end class FoundInButton

    protected class LastModLabel extends LabelButton {
        private static final long serialVersionUID = 1L;

        static final int intWidth = 80;

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
                    myNoteGroup.setStatusMessage(s);
                }

                public void mouseExited(MouseEvent e) {
                    myNoteGroup.setStatusMessage(" ");
                } // end mouseExited

            });
        } // end LastModLabel constructor


        public void clear() {
            setText("");
        } // end clear


        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();

            d.width = intWidth;
            return d;
        } // end getPreferredSize

    } // end class LastModLabel

} // end class SearchResultComponent



