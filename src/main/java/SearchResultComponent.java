/*  Representation of a single Search Result.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serial;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SearchResultComponent extends NoteComponent {
    @Serial
    private static final long serialVersionUID = 1L;

    // The Members
    private SearchResultData mySearchResultData;
    private final FoundInButton foundInButton;
    private final LastModLabel lastModLabel;
    private final SearchResultGroupPanel myNoteGroup; // status msgs & change notification

    SearchResultComponent(SearchResultGroupPanel ng, int i) {
        super(ng, i);
        setLayout(new DndLayout());

        index = i;
        myNoteGroup = ng;
        foundInButton = new FoundInButton();
        lastModLabel = new LastModLabel();

        // Since a SearchResult should never be modified we can do this here and now.
        // Also, the noteTextField has a built-in boolean for editability, unlike all the other
        //   composite NoteComponent children that add or remove a MouseListener based on the flag.
        noteTextField.setEditable(false);

        //----------------------------------------------------------
        // Graphical elements
        //----------------------------------------------------------
        // Note: The dndLayout does not care about any component name other
        //   than 'Stretch', but something must be provided for each one.
        //   Only one component can be the one to be stretched.
        add(foundInButton, "foundIn");
        add(noteTextField, "Stretch"); // will resize along with container
        add(lastModLabel, "lastMod");

        MemoryBank.trace();
    } // end constructor


    // Clears both the Graphical elements and the underlying data.
    // Used by swap
    public void clear() {
        super.clear();
        foundInButton.clear();
        lastModLabel.clear();
    } // end clear


    @Override
    public NoteData getNoteData() {
        return mySearchResultData;
    } // end getNoteData

    JComponent getFoundInButton() {
        return foundInButton;
    }

    String getFoundInButtonText(GroupInfo groupInfo) {
        if(groupInfo.groupType != GroupType.DAY_NOTES) {
            return groupInfo.getGroupName();
        }

        LocalDate theDate = CalendarNoteGroup.getDateFromGroupName(groupInfo);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d MMM yyyy");
        return dtf.format(theDate);
    }

    JComponent getLastModLabel() {
        return lastModLabel;
    }

    public void initialize() {
        super.initialize();
    } // end initialize

    public void resetColumnOrder(int theOrder) {
        String pos = String.valueOf(theOrder); // 123,132,213,231,312,321 - expecting one of these...
        System.out.println("SearchResultNoteComponent resetColumnOrder to " + pos);

        if(mySearchResultData == null) return;  // TODO ? not needed by To Do lists? Why not?
        // Without the condition, one of the tests fails but problem not seen otherwise (yet?).

        removeAll(); // With the DndLayout, it is easier to start over than try to rearrange.

        componentHeight = getComponentHeight(); // Defaulting to one line.
        if(theOrder < 200) { // 123, 132
            add(foundInButton, "1");
            if(theOrder == 123) {
                if (mySearchResultData.multiline) {
                    componentHeight = MULTI_LINE_HEIGHT;
                    add(noteScroller, "Stretch");
                } else {
                    add(noteTextField, "Stretch");
                }
                add(lastModLabel, "3");
            } else { // 132
                add(lastModLabel, "3");
                if (mySearchResultData.multiline) {
                    componentHeight = MULTI_LINE_HEIGHT;
                    add(noteScroller, "Stretch");
                } else {
                    add(noteTextField, "Stretch");
                }
            }
        } else if(theOrder < 300) { // 213, 231
            if (mySearchResultData.multiline) {
                componentHeight = MULTI_LINE_HEIGHT;
                add(noteScroller, "Stretch");
            } else {
                add(noteTextField, "Stretch");
            }
            if(theOrder == 213) {
                add(foundInButton, "1");
                add(lastModLabel, "3");
            } else { // 231
                add(lastModLabel, "3");
                add(foundInButton, "1");
            }
        } else { // 312, 321
            if(theOrder == 312) {
                add(lastModLabel, "3");
                add(foundInButton, "1");
                if (mySearchResultData.multiline) {
                    componentHeight = MULTI_LINE_HEIGHT;
                    add(noteScroller, "Stretch");
                } else {
                    add(noteTextField, "Stretch");
                }
            } else { // 321
                add(lastModLabel, "3");
                if (mySearchResultData.multiline) {
                    componentHeight = MULTI_LINE_HEIGHT;
                    add(noteScroller, "Stretch");
                } else {
                    add(noteTextField, "Stretch");
                }
                add(foundInButton, "1");
            }
        }

        // This was needed after paging was implemented.
        if (mySearchResultData.multiline) {
            noteTextArea.transferFocusUpCycle();  // new 4/18/2023
        } else {
            noteTextField.transferFocusUpCycle();  // new 3/19/2008
        }
    } // end resetColumnOrder


    // Called after a change to the encapsulated data, to show
    //   the visual effects of the change.
    @Override
    protected void resetComponent() {

        // This happens during loadPage, a lot.
        // None of the non-visible notes on the page ever got a data object.
        if(mySearchResultData == null) {
            return;
        }

        // Set the text of the 'Found In' button
        GroupInfo groupInfo = mySearchResultData.getFoundIn();
        if(groupInfo != null) {
            String foundInString = getFoundInButtonText(groupInfo);
            foundInButton.setText(foundInString);
        }

        // Get the Last Mod date
        ZonedDateTime zdtLastModDate = mySearchResultData.getLastModDate();

        // Make a date formatter.
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        // Format the Last Mod date and set the label text.
        assert zdtLastModDate != null;
        String strModDate = dtf.format(zdtLastModDate);
        lastModLabel.setText(strModDate);

        // Height adjustments and column reordering
        removeAll(); // With a DndLayout, it is easiest to just do it over.
        add(foundInButton);

        if (mySearchResultData.multiline) {
            componentHeight = MULTI_LINE_HEIGHT;
            add(noteScroller, "Stretch");
            noteTextArea.requestFocusInWindow();
        } else {
            componentHeight = getComponentHeight();
            //remove(noteScroller);
            add(noteTextField,"Stretch");
            noteTextField.requestFocusInWindow();
        }
        add(lastModLabel);

        int theOrder = ((SearchResultGroupProperties) myNoteGroupPanel.myNoteGroup.getGroupProperties()).columnOrder;
        if(theOrder != SearchResultGroupPanel.INORDER) resetColumnOrder(theOrder);

    } // end resetComponent

    @Override
    protected void resetPanelStatusMessage(int textStatus) {
        String s;

        s = "Search result text is non-editable.  Go to the ";
        s += "original source if a change is needed.";
        myNoteGroup.setStatusMessage(s);
    } // end resetPanelStatusMessage


    @Override
    public void setNoteData(NoteData newNoteData) {
        // Other NoteComponent children will handle additonal types due to the editability of their notes
        //   and group, but those kinds of changes are not allowed here so no castability check is needed.
        setSearchResultData((SearchResultData) newNoteData);
    } // end setNoteData


    private void setSearchResultData(SearchResultData newNoteData) {
        mySearchResultData = newNoteData;
        initialized = true;

        // update visual components....
        resetText();
        resetComponent();

        setNoteChanged();
    } // end setSearchResultData


    @Override
    public void swap(NoteComponent src) {
        // Get a reference to the two data objects
        SearchResultData srd1 = (SearchResultData) this.getNoteData();
        SearchResultData srd2 = (SearchResultData) src.getNoteData();

        // Note: getNoteData and setSearchResultData are working with references
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
        else this.setSearchResultData(srd2);

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
            super("Unknown");

            setFont(Font.decode("Monospaced-bold-12"));
            addMouseListener(this);
        } // end FoundInButton constructor

        public void clear() {
            setText("");
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
            if(mySearchResultData.foundIn != null) {
                if(mySearchResultData.foundIn.groupType != null) {
                    myNoteGroup.treePanel.showFoundIn(mySearchResultData);
                }
            }
            AppUtil.localDebug(false);
        } // end mouseClicked

        public void mouseEntered(MouseEvent e) {
            if (!mySearchResultData.hasText()) return;

            String s;
            s = "Click here to go to the original source";
            myNoteGroup.setStatusMessage(s);
        }

        public void mouseExited(MouseEvent e) {
            rightClicked = false;
            leftClicked = false;
            myNoteGroup.setStatusMessage(" ");
        } // end mouseExited

        public void mousePressed(MouseEvent e) {
            setActive();
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



