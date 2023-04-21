import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class SearchResultGroupPanel extends NoteGroupPanel {
    private static final Logger log = LoggerFactory.getLogger(SearchResultGroupPanel.class);
    private JLabel resultsPageOf;

    SearchResultHeader listHeader;
    static final int INORDER = 123;

    public SearchResultGroupPanel(GroupInfo groupInfo) {
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty GoalGroup.
        myNoteGroup.myNoteGroupPanel = this;
        setEditable(false); // Search Results are non-editable
        setAppendable(false); // The user cannot directly add notes to this group.

        // The lines below may only be needed for older data.  May be able to remove, eventually.
        // ('older' == before the user was given the ability to review search panel settings)
        //----------------------------------------------------------------------------------------------------------
        SearchResultGroupProperties srgProperties = (SearchResultGroupProperties) myNoteGroup.getGroupProperties();
        srgProperties.setGroupName(groupInfo.getGroupName()); // Override whatever name was loaded ("No Name Yet").
        srgProperties.groupType = GroupType.SEARCH_RESULTS; // Wasn't there, before.
        // The 'or' controls originally defaulted to 'true'.  The lines below help with that.
        boolean word1, word2, word3;
        word1 = srgProperties.searchPanelSettings.word1 != null;
        word2 = srgProperties.searchPanelSettings.word2 != null;
        word3 = srgProperties.searchPanelSettings.word3 != null;
        if(!word1 || !word2) srgProperties.searchPanelSettings.or1 = false;
        if(!word1 && !word2) srgProperties.searchPanelSettings.or2 = false;
        if(!word3) srgProperties.searchPanelSettings.or2 = false;
        // Group types to search previously defaulted to all true in the dialog, but no values were stored.  So if they
        // are not in the file it can only mean that the default was used since if they were ALL false in reality
        // then the search would not have been conducted in the first place.  The missing settings have been 'fixed' by
        // setting the default 'true' values in the SearchPanelSettings class definition which is used when the file is
        // read in.

        //----------------------------------------------------------------------------------------------------------

        loadNotesPanel();

        int theOrder = INORDER;
        if (myNoteGroup.getGroupProperties() != null) {
            theOrder = ((SearchResultGroupProperties) myNoteGroup.getGroupProperties()).columnOrder;
        } // end if
        if (theOrder != INORDER) resetColumnOrder();

        theNotePager.reset(1);
        buildPanelContent(); // Content other than the groupDataVector
    } // end constructor

    SearchResultGroupPanel(String groupName) {
        this(new GroupInfo(groupName, GroupType.SEARCH_RESULTS));
    } // end constructor


    // This interface allows for a group rename.
    void doReview() {
        JPanel nameAndSearchPanel = new JPanel(new BorderLayout());
        String originalName = getGroupName();

        // Get the group's properties.  One of the data members is the original search settings.
        SearchResultGroupProperties myProperties = (SearchResultGroupProperties) myNoteGroup.myProperties;

        // Invoking a new SearchPanel with previously established settings
        //   means that it will be non-editable; used for criteria review only.
        SearchPanel searchPanel = new SearchPanel(myProperties.searchPanelSettings);

        // Make a Title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5, 0));
        JTextField titleField = new JTextField(26);
        JLabel titleLabel = new JLabel("  Search Title: ");
        titleLabel.setFont(Font.decode("Dialog-bold-14"));
        titlePanel.add(titleLabel);
        titlePanel.add(titleField);
        titleField.setText(originalName);
        titleField.setFont(Font.decode("Dialog-bold-14"));
        nameAndSearchPanel.add(titlePanel, BorderLayout.NORTH);
        nameAndSearchPanel.add(searchPanel, BorderLayout.CENTER);

        int doit = JOptionPane.showConfirmDialog(
                JOptionPane.getFrameForComponent(theBasePanel),
                nameAndSearchPanel,
                "Search criteria for:    " + getGroupName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (doit == -1) return; // The X on the dialog
        if (doit == JOptionPane.CANCEL_OPTION) return;

        // The non-editable display of the search criteria does still allow for the Search name to be changed.
        // Get the current name, determine if there was a change (other than whitespace & case) -
        String currentName = titleField.getText();
        if(currentName == null || currentName.isBlank()) currentName = originalName;
        else currentName = currentName.trim();
        if(!originalName.equalsIgnoreCase(currentName)) {
            JTree theTree = AppTreePanel.theInstance.getTree();
            NoteGroupPanelKeeper theKeeper = AppTreePanel.theInstance.theSearchResultsKeeper;
            BranchHelper searchBranchHelper = new BranchHelper(theTree, theKeeper, DataArea.SEARCH_RESULTS);
            DefaultMutableTreeNode myBranch = BranchHelperInterface.getNodeByName(searchBranchHelper.theRoot, searchBranchHelper.theAreaNodeName);
            ArrayList<NodeChange> changeList = new ArrayList<>(); // Array of NodeChange
            changeList.add(new NodeChange(originalName, currentName));

            // Unlike with the TreeBranchEditor, we have no alternate tree branch to swap with.
            // But this is a single operation, so we do it more surgically - rename the one affected node.
            DefaultMutableTreeNode changedNode = BranchHelperInterface.getNodeByName(myBranch, originalName);
            changedNode.setUserObject(currentName); // This is the tree-flavored equivalent of node.setText().

            // Now - let the BranchHelper do all the required operations.
            boolean didIt = searchBranchHelper.doApply(myBranch, changeList);

            if(!didIt) {
                System.out.println("The rename operation failed; resetting node name to original value");
                changedNode.setUserObject(originalName);
            } else {
                System.out.println("Renamed SearchResult FROM: " + originalName + "   TO: " + currentName);
            }
        }
    }


    private void buildPanelContent() {
        // The 2-row Header for the SearchResultGroup -
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        // The First Header Row -   (Title & paging control)
        JPanel headingRow1 = new JPanel(new BorderLayout());
        headingRow1.setBackground(Color.blue);

        // Title
        JLabel titleLabel = new JLabel();
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setForeground(Color.white);
        titleLabel.setFont(Font.decode("Serif-bold-20"));
        titleLabel.setText(getGroupName());
        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                doReview();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                setStatusMessage("Click here to see the Search criteria");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                setStatusMessage(" ");
            }
        });

        // Set the pager's background to the same color as this row,
        //   since other items on this row make it slightly 'higher'
        //   than the pager control.
        theNotePager.setBackground(headingRow1.getBackground());

        headingRow1.add(titleLabel, "Center");
        headingRow1.add(theNotePager, "East");

        // The Second Header Row -     (record count and search text)
        JPanel headingRow2 = new JPanel(new BorderLayout());
        headingRow2.setBackground(Color.blue);

        // Show the results count (needs to be after updateGroup)
        resultsPageOf = new JLabel();
        resultsPageOf.setHorizontalAlignment(JLabel.CENTER);
        resultsPageOf.setForeground(Color.white);
        resultsPageOf.setFont(Font.decode("Serif-bold-14"));
        resultsPageOf.setText(theNotePager.getSummary());
        headingRow2.add(resultsPageOf, "West");

        // Show the search summary
        JLabel searchSummary = new JLabel();
        searchSummary.setHorizontalAlignment(JLabel.CENTER);
        searchSummary.setForeground(Color.white);
        searchSummary.setFont(Font.decode("Serif-bold-14"));
        searchSummary.setText(SearchPanel.getSummary(((SearchResultGroupProperties) myNoteGroup.myProperties).searchPanelSettings));
        headingRow2.add(searchSummary, "Center");

        heading.add(headingRow1);
        heading.add(headingRow2);
        add(heading, BorderLayout.NORTH);

        listHeader = new SearchResultHeader(this);
        setGroupHeader(listHeader);
    }

    //-------------------------------------------------------------------
    // Method Name: checkColumnOrder
    //
    // Re-order the columns.
    // This may be needed if the list had been saved with a different
    //   column order than the default.  In that case, this method is
    //   called from the constructor after the file load.
    // It is also needed after paging away from a 'short' page where
    //   a 'sort' was done, even though no reordering occurred.
    // We do it for ALL notes, visible or not, so that
    //   newly activated notes will appear properly.
    //-------------------------------------------------------------------
    private void checkColumnOrder() {
        SearchResultComponent tempNote;

        for (int i = 0; i <= getHighestNoteComponentIndex(); i++) {
            tempNote = (SearchResultComponent) groupNotesListPanel.getComponent(i);
            tempNote.resetColumnOrder(((SearchResultGroupProperties) myNoteGroup.myProperties).columnOrder);
        } // end for
    } // end checkColumnOrder



    //--------------------------------------------------------
    // Method Name: getNoteComponent
    //
    // Returns a SearchResultComponent that can be used to manipulate
    // component state as well as set/get underlying data.
    //--------------------------------------------------------
    @Override
    public SearchResultComponent getNoteComponent(int i) {
        return (SearchResultComponent) groupNotesListPanel.getComponent(i);
    } // end getNoteComponent


    //------------------------------------------------------------------
    // Method Name: pageNumberChanged
    //
    // Overrides the base class no-op method, to set the correct
    //   information in the header and ensure the group columns are
    //   displayed in the correct order.
    //------------------------------------------------------------------
    protected void pageNumberChanged() {
        resultsPageOf.setText(theNotePager.getSummary());

        // The column order must be reset because we may be coming from
        //   a page that had fewer items than this one, where a sort had
        //   occurred.  In the dndLayoutManager, non-visible rows appear
        //   to have a negative width in the text column, which causes
        //   them to be swapped on the 'short' page, only to show up
        //   (now in the wrong order) when paging to a 'full' page.
        // You cannot fix this by disallowing a swap for non-showing rows,
        //   because an intentional swap will not affect them and then
        //   the problem would appear when adding new notes.
        // Will not make this call conditional on a short-page-sort,
        //   because the gain in performance against the overhead needed
        //   to track that condition, is questionable.
        checkColumnOrder();
    } // end pageNumberChanged


    //-------------------------------------------------------------------
    // Method Name: makeNewNoteComponent
    //
    // Called by the NoteGroup (base class) constructor.  This
    //   is just the visual component; no actual data yet.
    //-------------------------------------------------------------------
    @Override
    protected JComponent makeNewNoteComponent(int i) {
        SearchResultComponent src = new SearchResultComponent(this, i);
        src.setEditable(false);
        src.setVisible(false);
        return src;
    } // end makeNewNoteComponent


    // Disabled this 9/03/2019 so that it does not pull down code coverage for tests.
    // But it appears to have never been used - there was no menu item leading here.
    // See the comment in TodoNoteGroup.printList for additional context but this one is
    // not exactly the same in that there is even LESS reason to use this one; I suspect
    // that the only reason it was here was that it was a leftover from when the
    // SearchResultGroup was first cloned from a TodoNoteGroup.  I would remove it
    // right now except that it might help to keep as a reference if we do go ahead
    // with any kind of remediation for the TodoNoteGroup print capability, and this
    // one ALSO has the same problem of two consecutive dialogs popping up so it might
    // be useful in tracking down that issue.
    // This function can be seen / tested via the (currently-commented-out) test for it.
//    //--------------------------------------------------------------------------
//    // Method Name: printList
//    //
//    //--------------------------------------------------------------------------
//    public void printList() {
//        int t = strTheGroupFilename.lastIndexOf(".sresults");
//        String dumpFileName;    // Formatted text for printout
//        dumpFileName = strTheGroupFilename.substring(0, t) + ".dump";
//
//        PrintWriter outFile = null;
//        SearchResultComponent tnc;
//        SearchResultData tnd;
//        int i;
//        String todoText;
//        String extText;
//
//        // Open the output file.
//        try {
//            outFile = new PrintWriter(new BufferedWriter
//                    (new FileWriter(dumpFileName)), true);
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.exit(0);
//        } // end try/catch
//
//        int listSize = lastVisibleNoteIndex;
//        // System.out.println("listSize = " + listSize);
//
//        for (i = 0; i < listSize; i++) {
//            tnc = (SearchResultComponent) groupNotesListPanel.getComponent(i);
//            tnd = (SearchResultData) tnc.getNoteData();
//
//            // Do not print items with no primary text
//            todoText = tnd.getNoteString().trim();
//            if (todoText.equals("")) continue;
//
//            // To Do Text
//            outFile.println(todoText);
//
//            // Extended Text
//            extText = tnd.getExtendedNoteString();
//            if (!extText.equals("")) {
//                String indent = "    ";
//                StringBuilder rs = new StringBuilder(indent); // ReturnString
//
//                for (int j = 0; j < extText.length(); j++) {
//                    if (extText.substring(j).startsWith("\t"))
//                        rs.append("        "); // convert tabs to 8 spaces.
//                    else if (extText.substring(j).startsWith("\n"))
//                        rs.append("\n").append(indent);
//                    else
//                        rs.append(extText, j, j + 1);
//                    // The 'j+1' will not exceed string length when j=length
//                    //  because 'substring' works with one less on the 2nd int.
//                } // end for j
//
//                extText = rs.toString();
//                outFile.println(extText);
//            } // end if
//        } // end for i
//        outFile.close();
//
//        TextFilePrinter tfp = new TextFilePrinter(dumpFileName,
//                JOptionPane.getFrameForComponent(this));
//        tfp.setOptions(true, true, true);
//        tfp.setVisible(true);
//    } // end printList


    // A panel-level reordering of the columns.
    // This may be needed if the list had been saved with a different
    //   column order than the default.  In that case, this method is
    //   called from the constructor after the file load.
    // It is also needed after paging away from a 'short' page where
    //   a 'sort' was done, even though no reordering occurred.
    // We do it for ALL notes, visible or not, so that
    //   newly activated notes will appear properly.
    private void resetColumnOrder() {
        TodoNoteComponent tempNote;

        for (int i = 0; i <= getHighestNoteComponentIndex(); i++) {
            tempNote = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
            // Call the component-level column reordering method.
            tempNote.resetColumnOrder(((TodoGroupProperties) myNoteGroup.myProperties).columnOrder);
        } // end for
    } // end resetColumnOrder


    // Originally this 'sort' method was cloned from a class where it was possible that
    // the items being sorted might not actually have a value in the field they were being sorted
    // on.  So those cases needed to be handled and the question to ask was - if there is no sort
    // key, where do we put this?  There were 3 possible answers - put it at the top, put it at
    // the bottom, or just leave it where it is and sort everything else around it (stay).  So,
    // with ASCENDING and DESCENDING, that made six different ways to sort the items.

    // But here in a SearchResultGroup we never did get an option panel to allow the user to make
    // that choice themselves, so it's always been a default, and four of the other choices were
    // never used.  Sorting NoteComponents based on their base text, we thought that the STAY
    // option would work best but really we can just assign it the empty string and sort anyway,
    // so that's what's happening now and there is only one ascending and one descending way to go.

    // New thoughts 7 Nov 2020 - How can there be no value on a noteString, where a note is not allowed
    // to be saved in the first place if there is no text?  This means that for SearchResults there are
    // only the two directions and the user does have control over which one is used.  But this method
    // looks like it only sorts the visible page; if there is more than one page - there will be problems
    // when the page is 'turned'.  The sorting needs to be done on the source data vector, set the
    // groupChanged flag to true and then just reload the current page.

    public void sortText(int direction) {
        SearchResultData todoData1, todoData2;
        SearchResultComponent todoComponent1, todoComponent2;
        int i, j;
        String str1, str2;
        int items = lastVisibleNoteIndex;

        log.debug("SearchResultGroup sortText - Number of items in list: " + items);

        if(direction == ASCENDING) {
            log.debug("Sorting: Text, ASCENDING");
            for (i = 0; i < (items - 1); i++) {
                todoComponent1 = (SearchResultComponent) groupNotesListPanel.getComponent(i);
                todoData1 = (SearchResultData) todoComponent1.getNoteData();
                if (todoData1 == null) str1 = "";
                else str1 = todoData1.getNoteString().trim();
                for (j = i + 1; j < items; j++) {
                    todoComponent2 = (SearchResultComponent) groupNotesListPanel.getComponent(j);
                    todoData2 = (SearchResultData) todoComponent2.getNoteData();
                    if (todoData2 == null) str2 = "";
                    else str2 = todoData2.getNoteString().trim();
                    if (str1.compareTo(str2) > 0) {
                        str1 = str2;
                        todoComponent1.swap(todoComponent2);
                    } // end if
                } // end for j
            } // end for i
        } else {
            log.debug("Sorting: Text, DESCENDING");
            for (i = 0; i < (items - 1); i++) {
                todoComponent1 = (SearchResultComponent) groupNotesListPanel.getComponent(i);
                todoData1 = (SearchResultData) todoComponent1.getNoteData();
                if (todoData1 == null) str1 = "";
                else str1 = todoData1.getNoteString().trim();
                for (j = i + 1; j < items; j++) {
                    todoComponent2 = (SearchResultComponent) groupNotesListPanel.getComponent(j);
                    todoData2 = (SearchResultData) todoComponent2.getNoteData();
                    if (todoData2 == null) str2 = "";
                    else str2 = todoData2.getNoteString().trim();
                    if (str1.compareTo(str2) < 0) {
                        str1 = str2;
                        todoComponent1.swap(todoComponent2);
                    } // end if
                } // end for j
            } // end for i
        }
    } // end sortText

} // end class SearchResultGroup


