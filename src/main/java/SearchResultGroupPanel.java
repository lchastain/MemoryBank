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
    boolean fixedDataWhileLoading;

    SearchResultGroupPanel(String groupName) {
        // super(10);  // test, for paging
        BaseData.loading = true;
        setDefaultSubject("Search Info"); // Used in Search Criteria review, only.

        GroupInfo groupInfo = new GroupInfo(groupName, GroupType.SEARCH_RESULTS);
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads and sets the data, if any.

        // These lines may only be needed for older data ("No Name Yet").  May be able to remove, eventually.
        myNoteGroup.getGroupProperties().setGroupName(groupName); // Override whatever name was loaded.
        myNoteGroup.getGroupProperties().groupType = GroupType.SEARCH_RESULTS;

        myNoteGroup.myNoteGroupPanel = this;
        editable = false;
        loadNotesPanel();
        editable = true;

        // Unlike with a ToDo list, this is not conditional; we just do it whether needed or not.
        checkColumnOrder();

        if(fixedDataWhileLoading) setGroupChanged(true); // This can go away when all is fixed.
        // The component might have set this to true, if it found a non-null 'fileFoundIn' value.

        theNotePager.reset(1);
        buildPanelContent();
        BaseData.loading = false;
    } // end constructor


    private void buildPanelContent() {
        // The 2-row Header for the SearchResultGroup -
        //-----------------------------------------------------
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        // The First Header Row -   (Title & paging control)
        //----------------------------------------------------------
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
                SearchResultGroupProperties myProperties = (SearchResultGroupProperties) myNoteGroup.myProperties;

                // Invoking a new SearchPanel with previously established settings -
                //   means that it will be non-editable, for criteria review only.
                SearchPanel searchPanel = new SearchPanel(myProperties.searchPanelSettings);

                // Make our own ExtendedNoteComponent, vs the default one provided by our base class when the editor
                //  is invoked.  This allows further customization, below.
                extendedNoteComponent = new ExtendedNoteComponent(defaultSubject);

                // Provide an alternative center panel for the ExtendedNoteComponent -
                //   the SearchPanel replaces the JScrollPane that holds a JTextArea.
                extendedNoteComponent.setCenterPanel(searchPanel);

                // Configure the title of the editor, and the initial subject (in this case 'subject' == search name).
                NoteData titleNoteData = new NoteData("Search criteria for:    " + getGroupName());
                titleNoteData.setSubjectString(getGroupName());
                // Since we are not using the default JTextArea, there is no need to set an extendedNoteString.

                // We will use the base class 'edit' method for displaying the search criteria.
                // It takes the subject from the provided note, IF this NoteGroup has a default subject.
                boolean nameChanged = editExtendedNoteComponent(titleNoteData);

                // The non-editable display of the search criteria does still allow for the Search name to be changed.
                if(nameChanged) {
                    JTree theTree = AppTreePanel.theInstance.getTree();
                    NoteGroupPanelKeeper theKeeper = AppTreePanel.theInstance.theSearchResultsKeeper;
                    BranchHelper searchBranchHelper = new BranchHelper(theTree, theKeeper, BranchHelper.AreaName.SEARCH);
                    DefaultMutableTreeNode myBranch = BranchHelperInterface.getNodeByName(searchBranchHelper.theRoot, searchBranchHelper.theAreaNodeName);
                    ArrayList<NodeChange> changeList = new ArrayList<>(); // Array of NodeChange
                    String renamedFrom = getGroupName();
                    String renamedTo = extendedNoteComponent.getSubject();
                    changeList.add(new NodeChange(renamedFrom, renamedTo));

                    // Unlike with the TreeBranchEditor, we have no alternate tree branch to swap with.
                    // But this is a single operation, so we do it more surgically - rename the one affected node.
                    DefaultMutableTreeNode changedNode = BranchHelperInterface.getNodeByName(myBranch, renamedFrom);
                    changedNode.setUserObject(renamedTo); // This is the tree-flavored equivalent of node.setText().

                    // Now - do all required operations.
                    boolean didIt = searchBranchHelper.doApply(myBranch, changeList);

                    if(!didIt) {
                        System.out.println("The renamed operation failed; resetting node name to original value");
                        changedNode.setUserObject(renamedFrom);
                    } else {
                        System.out.println("Renamed SearchResult FROM: " + renamedFrom + "   TO: " + renamedTo);
                    }
                }
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
        //----------------------------------------------------------

        // The Second Header Row -     (record count and search text)
        //----------------------------------------------------------
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
        //----------------------------------------------------------

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
    // Method Name: makeNewNote
    //
    // Called by the NoteGroup (base class) constructor.  This
    //   is just the visual component; no actual data yet.
    //-------------------------------------------------------------------
    @Override
    protected JComponent makeNewNote(int i) {
        SearchResultComponent src = new SearchResultComponent(this, i);
        src.setEditable(false);
        src.setVisible(false);
        return src;
    } // end makeNewNote


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


    private void saveProperties() {
        // Update the header text of the columns.
        ((SearchResultGroupProperties) myNoteGroup.myProperties).column1Label = listHeader.getColumnHeader(1);
        ((SearchResultGroupProperties) myNoteGroup.myProperties).column2Label = listHeader.getColumnHeader(2);
        ((SearchResultGroupProperties) myNoteGroup.myProperties).column3Label = listHeader.getColumnHeader(3);
        ((SearchResultGroupProperties) myNoteGroup.myProperties).columnOrder = listHeader.getColumnOrder();
    } // end saveProperties


    public void shiftDown(int index) {
//        if(!editable) return;  // Seems like a user SHOULD be able to sort their results.
        if (index >= lastVisibleNoteIndex) return;
        System.out.println("SRG Shifting note down");
        SearchResultComponent src1, src2;
        src1 = (SearchResultComponent) groupNotesListPanel.getComponent(index);
        src2 = (SearchResultComponent) groupNotesListPanel.getComponent(index + 1);

        src1.swap(src2);
        src2.setActive();
    } // end shiftDown


    public void shiftUp(int index) {
//        if(!editable) return;
        if (index == 0) return;
        System.out.println("SRG Shifting note up");
        SearchResultComponent src1, src2;
        src1 = (SearchResultComponent) groupNotesListPanel.getComponent(index);
        src2 = (SearchResultComponent) groupNotesListPanel.getComponent(index - 1);

        src1.swap(src2);
        src2.setActive();
    } // end shiftUp

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


