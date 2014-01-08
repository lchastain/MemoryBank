/* ***************************************************************************
 * File:    SearchResultGroup.java
 * Author:  D. Lee Chastain
 *
 ****************************************************************************/
/**  This class displays a group of SearchResultComponent.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.*;
//import java.util.Date;

import javax.swing.*;

public class SearchResultGroup extends NoteGroup {
    private static final long serialVersionUID = 1L;

    private JLabel resultsTitle;
    private JLabel resultsPageOf;
    private JLabel searchSummary;
    public SearchResultHeader listHeader;

    private String strTheGroupFilename;

    // This is saved/loaded
    public SearchResultGroupProperties myVars; // Variables - flags and settings

    SearchResultGroup(String fname) {
        super("");
        // super("", 10);  // test, for paging

        addNoteAllowed = false;
        enc.remove(0); // Remove the subjectChooser.
        // We may want to make this operation less numeric in the future,
        //   but this works for now and no ENC changes are expected.

        strTheGroupFilename = fname;

        updateGroup(); // This is where the file gets loaded
        myVars = (SearchResultGroupProperties) objGroupProperties;
        checkColumnOrder();

        // Header for the group of SearchResultComponents
        listHeader = new SearchResultHeader(this);
        setGroupHeader(listHeader);

        // Now the 2-row Header for the SearchResultGroup -
        //-----------------------------------------------------
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        // The First Row -
        //----------------------------------------------------------
        JPanel headingRow1 = new JPanel(new BorderLayout());
        headingRow1.setBackground(Color.blue);

        // Create the window title
        resultsTitle = new JLabel();
        resultsTitle.setHorizontalAlignment(JLabel.CENTER);
        resultsTitle.setForeground(Color.white);
        resultsTitle.setFont(Font.decode("Serif-bold-20"));
        resultsTitle.setText(prettyName(fname));

        // Set the pager's background to the same color as this row,
        //   since other items on this row make it slightly 'higher'
        //   than the pager control.
        npThePager.setBackground(headingRow1.getBackground());

        headingRow1.add(resultsTitle, "Center");
        headingRow1.add(npThePager, "East");
        //----------------------------------------------------------

        // The Second Row -
        //----------------------------------------------------------
        JPanel headingRow2 = new JPanel(new BorderLayout());
        headingRow2.setBackground(Color.blue);

        // Show the results count (needs to be after updateGroup)
        resultsPageOf = new JLabel();
        resultsPageOf.setHorizontalAlignment(JLabel.CENTER);
        resultsPageOf.setForeground(Color.white);
        resultsPageOf.setFont(Font.decode("Serif-bold-14"));
        resultsPageOf.setText(npThePager.getSummary());
        headingRow2.add(resultsPageOf, "West");

        // Show the search summary
        searchSummary = new JLabel();
        searchSummary.setHorizontalAlignment(JLabel.CENTER);
        searchSummary.setForeground(Color.white);
        searchSummary.setFont(Font.decode("Serif-bold-14"));
        searchSummary.setText(SearchPanel.getSummary(myVars.sps));
        System.out.println();
        headingRow2.add(searchSummary, "Center");
        //----------------------------------------------------------

        heading.add(headingRow1);
        heading.add(headingRow2);
        add(heading, BorderLayout.NORTH);
    } // end constructor


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
            tempNote.resetColumnOrder(myVars.columnOrder);
        } // end for
    } // end checkColumnOrder


    // -------------------------------------------------------------------
    // Method Name: getGroupFilename
    //
    // This method returns the name of the file where the data for this
    //   group of notes is loaded / saved.
    // -------------------------------------------------------------------
    public String getGroupFilename() {
        return strTheGroupFilename;
    }// end getGroupFilename


    //--------------------------------------------------------
    // Method Name: getNoteComponent
    //
    // Gives containers some access.
    //--------------------------------------------------------
    public SearchResultComponent getNoteComponent(int i) {
        return (SearchResultComponent) groupNotesListPanel.getComponent(i);
    } // end getNoteComponent


    //--------------------------------------------------------------
    // Method Name: getProperties
    //
    //  Called by saveGroup.
    //  Returns an actual object, vs the overriden method
    //    in the base class that returns a null.
    //--------------------------------------------------------------
    protected Object getProperties() {
        return myVars;
    } // end getProperties


    //------------------------------------------------------------------
    // Method Name: pageNumberChanged
    //
    // Overrides the base class no-op method, to set the correct
    //   information in the header and ensure the group columns are
    //   displayed in the correct order.
    //------------------------------------------------------------------
    protected void pageNumberChanged() {
        resultsPageOf.setText(npThePager.getSummary());

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
    //   is just the visual component; no actual data yet.  The
    //   reason for providing the data component parameter is
    //   that it is required by the base class constructor.
    //-------------------------------------------------------------------
    protected JComponent makeNewNote(int i) {
        if (i == 0) myVars = new SearchResultGroupProperties();
        SearchResultComponent tnc = new SearchResultComponent(this, i);
        tnc.setVisible(false);
        return tnc;
    } // end makeNewNote


    //----------------------------------------------------------------------
    // Method Name: preClose
    //
    // Overrides the base class method in order to check if columns have
    //   been reordered.  If so, set groupChanged.
    // Then call the base class method.
    //----------------------------------------------------------------------
    protected void preClose() {
        super.preClose();
    } // end preClose


    //-----------------------------------------------------------------
    // Method Name:  prettyName
    //
    // A formatter for a filename specifier - drop off the path
    //   prefix and/or trailing '.sresults', if present.
    //-----------------------------------------------------------------
    public static String prettyName(String s) {
        int i;
        char slash = File.separatorChar;

        i = s.lastIndexOf(slash);
        if (i != -1) {
            s = s.substring(i + 1);
        } // end if

        // Even though a Windows path separator char should be a
        //   backslash, in Java a forward slash is often also accepted.
        i = s.lastIndexOf("/");
        if (i != -1) {
            s = s.substring(i + 1);
        } // end if

        // Drop the suffix
        i = s.lastIndexOf(".sresults");
        if (i == -1) return s;
        return s.substring(0, i);
    } // end prettyName


    //--------------------------------------------------------------------------
    // Method Name: printList
    //
    //--------------------------------------------------------------------------
    public void printList() {
        int t = strTheGroupFilename.lastIndexOf(".SearchResult");
        String dumpFileName;    // Formatted text for printout
        dumpFileName = strTheGroupFilename.substring(0, t) + ".dump";

        PrintWriter outFile = null;
        SearchResultComponent tnc;
        SearchResultData tnd;
        int i;
        String todoText;
        String extText;

        // Open the output file.
        try {
            outFile = new PrintWriter(new BufferedWriter
                    (new FileWriter(dumpFileName)), true);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(0);
        } // end try/catch

        int listSize = lastVisibleNoteIndex;
        // System.out.println("listSize = " + listSize);

        for (i = 0; i < listSize; i++) {
            tnc = (SearchResultComponent) groupNotesListPanel.getComponent(i);
            tnd = (SearchResultData) tnc.getNoteData();

            // Do not print items with no primary text
            todoText = tnd.getNoteString().trim();
            if (todoText.equals("")) continue;

            // To Do Text
            outFile.println(todoText);

            // Extended Text
            extText = tnd.getExtendedNoteString();
            if (!extText.equals("")) {
                String indent = "    ";
                String rs = indent; // ReturnString

                for (int j = 0; j < extText.length(); j++) {
                    if (extText.substring(j).startsWith("\t"))
                        rs = rs + "        "; // convert tabs to 8 spaces.
                    else if (extText.substring(j).startsWith("\n"))
                        rs = rs + "\n" + indent;
                    else
                        rs = rs + extText.substring(j, j + 1);
                    // The 'j+1' will not exceed string length when j=length
                    //  because 'substring' works with one less on the 2nd int.
                } // end for j

                extText = rs;
                outFile.println(extText);
            } // end if
        } // end for i
        outFile.close();

        TextFilePrinter tfp = new TextFilePrinter(dumpFileName,
                JOptionPane.getFrameForComponent(this));
        tfp.setOptions(true, true, true);
        tfp.setVisible(true);
    } // end printList

    String formatText(String s) {
        String indent = "    ";
        String rs = indent; // ReturnString

        for (int i = 0; i < s.length(); i++) {
            if (s.substring(i).startsWith("\t"))
                rs = rs + "        "; // convert tabs to 8 spaces.
            else if (s.substring(i).startsWith("\n"))
                rs = rs + "\n" + indent;
            else
                rs = rs + s.substring(i, i + 1);
            // The 'i+1' will not exceed string length when i=length
            //  because 'substring' works with one less on the 2nd int.
        } // end for i
        return rs;
    } // end formatText


    // This method is called externally, from a context that has
    //   access to a user-specified name for these search results.
    public void resetTitle(String s) {
        resultsTitle.setText(s);
    } // end resetTitle


    //--------------------------------------------------------------
    // Method Name: saveProperties
    //
    //--------------------------------------------------------------
    protected boolean saveProperties(ObjectOutputStream oos)
            throws IOException {

        // Update the header text of the columns.
        myVars.column1Label = listHeader.getColumnHeader(1);
        myVars.column2Label = listHeader.getColumnHeader(2);
        myVars.column3Label = listHeader.getColumnHeader(3);
        myVars.columnOrder = listHeader.getColumnOrder();

        // Write out the SearchResultGroupProperties
        oos.writeObject(myVars);
        return true;
    } // end saveProperties


    //-----------------------------------------------------------------
    // Method Name:  setFileName
    //
    // Provided as a way for a calling context to do a 'save as'.
    //  (By calling this first, then just calling 'save').  Any
    //  checking for validity is responsibility of calling context.
    //-----------------------------------------------------------------
    public void setFileName(String fname) {
        strTheGroupFilename = MemoryBank.userDataDirPathName + File.separatorChar;
        strTheGroupFilename += fname + ".SearchResult";

        setGroupChanged();
    } // end setFileName


    public void shiftDown(int index) {
        if (index >= lastVisibleNoteIndex) return;
        System.out.println("SRG Shifting note down");
        SearchResultComponent src1, src2;
        src1 = (SearchResultComponent) groupNotesListPanel.getComponent(index);
        src2 = (SearchResultComponent) groupNotesListPanel.getComponent(index + 1);

        src1.swap(src2);
        src2.setActive();
    } // end shiftDown


    public void shiftUp(int index) {
        if (index == 0) return;
        System.out.println("SRG Shifting note up");
        SearchResultComponent src1, src2;
        src1 = (SearchResultComponent) groupNotesListPanel.getComponent(index);
        src2 = (SearchResultComponent) groupNotesListPanel.getComponent(index - 1);

        src1.swap(src2);
        src2.setActive();
    } // end shiftUp

    public void sortDeadline(int direction) {
        SearchResultData todoData1, todoData2;
        SearchResultComponent todoComponent1, todoComponent2;
        int i, j;
        long lngDate1, lngDate2;
        int sortMethod = 0;
        int items = lastVisibleNoteIndex;
        MemoryBank.debug("SearchResultGroup sortDeadline - Number of items in list: " + items);

        // Bitmapping of the 6 possible sorting variants.
        //  Zero-values are ASCENDING, STAY (but that is not the default)
        if (direction == DESCENDING) sortMethod += 4;

        switch (sortMethod) {
            case 0:         // ASCENDING, STAY
                // System.out.println("Sorting: Deadline, ASCENDING, STAY");
                for (i = 0; i < (items - 1); i++) {
                    todoComponent1 = (SearchResultComponent) groupNotesListPanel.getComponent(i);
                    todoData1 = (SearchResultData) todoComponent1.getNoteData();
                    if (todoData1 == null) lngDate1 = 0;
                    else lngDate1 = todoData1.getNoteDate().getTime();
                    if (lngDate1 == 0) continue; // No key; skip.
                    for (j = i + 1; j < items; j++) {
                        todoComponent2 = (SearchResultComponent) groupNotesListPanel.getComponent(j);
                        todoData2 = (SearchResultData) todoComponent2.getNoteData();
                        if (todoData2 == null) lngDate2 = 0;
                        else lngDate2 = todoData2.getNoteDate().getTime();
                        if (lngDate2 == 0) continue; // No key; skip.
                        if (lngDate1 > lngDate2) {
                            lngDate1 = lngDate2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
            case 1:         // ASCENDING, TOP
                // System.out.println("Sorting: Deadline, ASCENDING, TOP");
                for (i = 0; i < (items - 1); i++) {
                    todoComponent1 = (SearchResultComponent) groupNotesListPanel.getComponent(i);
                    todoData1 = (SearchResultData) todoComponent1.getNoteData();
                    if (todoData1 == null) lngDate1 = 0;
                    else lngDate1 = todoData1.getNoteDate().getTime();
                    for (j = i + 1; j < items; j++) {
                        todoComponent2 = (SearchResultComponent) groupNotesListPanel.getComponent(j);
                        todoData2 = (SearchResultData) todoComponent2.getNoteData();
                        if (todoData2 == null) lngDate2 = 0;
                        else lngDate2 = todoData2.getNoteDate().getTime();
                        if (lngDate1 > lngDate2) {
                            lngDate1 = lngDate2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
            case 2:         // ASCENDING, BOTTOM
                // System.out.println("Sorting: Deadline, ASCENDING, BOTTOM");
                for (i = 0; i < (items - 1); i++) {
                    todoComponent1 = (SearchResultComponent) groupNotesListPanel.getComponent(i);
                    todoData1 = (SearchResultData) todoComponent1.getNoteData();
                    if (todoData1 == null) lngDate1 = 0;
                    else lngDate1 = todoData1.getNoteDate().getTime();
                    for (j = i + 1; j < items; j++) {
                        todoComponent2 = (SearchResultComponent) groupNotesListPanel.getComponent(j);
                        todoData2 = (SearchResultData) todoComponent2.getNoteData();
                        if (todoData2 == null) lngDate2 = 0;
                        else lngDate2 = todoData2.getNoteDate().getTime();
                        if (((lngDate1 > lngDate2) && (lngDate2 != 0)) || (lngDate1 == 0)) {
                            lngDate1 = lngDate2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
            case 4:         // DESCENDING, STAY
                // System.out.println("Sorting: Deadline, DESCENDING, STAY");
                for (i = 0; i < (items - 1); i++) {
                    todoComponent1 = (SearchResultComponent) groupNotesListPanel.getComponent(i);
                    todoData1 = (SearchResultData) todoComponent1.getNoteData();
                    if (todoData1 == null) lngDate1 = 0;
                    else lngDate1 = todoData1.getNoteDate().getTime();
                    if (lngDate1 == 0) continue; // No key; skip.
                    for (j = i + 1; j < items; j++) {
                        todoComponent2 = (SearchResultComponent) groupNotesListPanel.getComponent(j);
                        todoData2 = (SearchResultData) todoComponent2.getNoteData();
                        if (todoData2 == null) lngDate2 = 0;
                        else lngDate2 = todoData2.getNoteDate().getTime();
                        if (lngDate2 == 0) continue; // No key; skip.
                        if (lngDate1 < lngDate2) {
                            lngDate1 = lngDate2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
            case 5:         // DESCENDING, TOP
                // System.out.println("Sorting: Deadline, DESCENDING, TOP");
                for (i = 0; i < (items - 1); i++) {
                    todoComponent1 = (SearchResultComponent) groupNotesListPanel.getComponent(i);
                    todoData1 = (SearchResultData) todoComponent1.getNoteData();
                    if (todoData1 == null) lngDate1 = 0;
                    else lngDate1 = todoData1.getNoteDate().getTime();
                    for (j = i + 1; j < items; j++) {
                        todoComponent2 = (SearchResultComponent) groupNotesListPanel.getComponent(j);
                        todoData2 = (SearchResultData) todoComponent2.getNoteData();
                        if (todoData2 == null) lngDate2 = 0;
                        else lngDate2 = todoData2.getNoteDate().getTime();
                        if (((lngDate1 < lngDate2) && (lngDate1 != 0)) || (lngDate2 == 0)) {
                            lngDate1 = lngDate2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
            case 6:         // DESCENDING, BOTTOM
                // System.out.println("Sorting: Deadline, DESCENDING, BOTTOM");
                for (i = 0; i < (items - 1); i++) {
                    todoComponent1 = (SearchResultComponent) groupNotesListPanel.getComponent(i);
                    todoData1 = (SearchResultData) todoComponent1.getNoteData();
                    if (todoData1 == null) lngDate1 = 0;
                    else lngDate1 = todoData1.getNoteDate().getTime();
                    for (j = i + 1; j < items; j++) {
                        todoComponent2 = (SearchResultComponent) groupNotesListPanel.getComponent(j);
                        todoData2 = (SearchResultData) todoComponent2.getNoteData();
                        if (todoData2 == null) lngDate2 = 0;
                        else lngDate2 = todoData2.getNoteDate().getTime();
                        if (lngDate1 < lngDate2) {
                            lngDate1 = lngDate2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
        } // end switch sortMethod
        //thisFrame.getContentPane().validate();
    } // end sortDeadline


    public void sortText(int direction) {
        SearchResultData todoData1, todoData2;
        SearchResultComponent todoComponent1, todoComponent2;
        int i, j;
        String str1, str2;
        int sortMethod = 0;
        int items = lastVisibleNoteIndex;

        // AppUtil.localDebug(true);

        MemoryBank.debug("SearchResultGroup sortText - Number of items in list: " + items);

        // Bitmapping of the 6 possible sorting variants.
        //  Zero-values are ASCENDING, STAY
        if (direction == DESCENDING) sortMethod += 4;
        switch (sortMethod) {
            case 0:         // ASCENDING, STAY
                MemoryBank.debug("Sorting: Text, ASCENDING, STAY");
                for (i = 0; i < (items - 1); i++) {
                    todoComponent1 = (SearchResultComponent) groupNotesListPanel.getComponent(i);
                    todoData1 = (SearchResultData) todoComponent1.getNoteData();
                    if (todoData1 == null) str1 = "";
                    else str1 = todoData1.getNoteString().trim();
                    if (str1.equals("")) continue; // No key; skip.
                    for (j = i + 1; j < items; j++) {
                        todoComponent2 = (SearchResultComponent) groupNotesListPanel.getComponent(j);
                        todoData2 = (SearchResultData) todoComponent2.getNoteData();
                        if (todoData2 == null) str2 = "";
                        else str2 = todoData2.getNoteString().trim();
                        if (str2.equals("")) continue; // No key; skip.
                        if (str1.compareTo(str2) > 0) {
                            str1 = str2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
            case 1:         // ASCENDING, TOP
                MemoryBank.debug("Sorting: Text, ASCENDING, TOP");
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
                break;
            case 2:         // ASCENDING, BOTTOM
                MemoryBank.debug("Sorting: Text, ASCENDING, BOTTOM");
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
                        if (((str1.compareTo(str2) > 0) && (!str2.equals(""))) || (str1.equals(""))) {
                            str1 = str2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
            case 4:         // DESCENDING, STAY
                MemoryBank.debug("Sorting: Text, DESCENDING, STAY");
                for (i = 0; i < (items - 1); i++) {
                    todoComponent1 = (SearchResultComponent) groupNotesListPanel.getComponent(i);
                    todoData1 = (SearchResultData) todoComponent1.getNoteData();
                    if (todoData1 == null) str1 = "";
                    else str1 = todoData1.getNoteString().trim();
                    if (str1.equals("")) continue; // No key; skip.
                    for (j = i + 1; j < items; j++) {
                        todoComponent2 = (SearchResultComponent) groupNotesListPanel.getComponent(j);
                        todoData2 = (SearchResultData) todoComponent2.getNoteData();
                        if (todoData2 == null) str2 = "";
                        else str2 = todoData2.getNoteString().trim();
                        if (str2.equals("")) continue; // No key; skip.
                        if (str1.compareTo(str2) < 0) {
                            str1 = str2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
            case 5:         // DESCENDING, TOP
                MemoryBank.debug("Sorting: Text, DESCENDING, TOP");
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

                        if (((str1.compareTo(str2) < 0) && (!str1.equals(""))) || (str2.equals(""))) {
                            str1 = str2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
            case 6:         // DESCENDING, BOTTOM
                MemoryBank.debug("Sorting: Text, DESCENDING, BOTTOM");
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
                break;
        } // end switch sortMethod

        // AppUtil.localDebug(false);
    } // end sortText

} // end class SearchResultGroup


// This class holds the persistent data for the SearchResultGroup
//-----------------------------------------------------------------------
class SearchResultGroupProperties implements Serializable {
    public static final long serialVersionUID = 2412760123507069513L;

    public SearchPanelSettings sps;

    public String column1Label;
    public String column2Label;
    public String column3Label;
    public String column4Label;

    public int columnOrder;

    public SearchResultGroupProperties() {
        column1Label = "Found in";
        column2Label = "Note Text";
        column3Label = "Last Modified";
        column4Label = "";  // placeholder
        columnOrder = 123;  // 1234
    } // end constructor

    public void setSearchSettings(SearchPanelSettings s) {
        sps = s;
    } // end setSearchSettings

} // end SearchResultGroupProperties


