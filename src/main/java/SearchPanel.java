/* ***************************************************************/
/*                      SearchPanel	                            */
/*                                                              */

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Summary description for SearchPanel
 */
public class SearchPanel extends JPanel {
    private static final long serialVersionUID = -7578180659628641668L;

    // 'My' Variables declaration section
    //-------------------------------------------------------------

    private static final int NOPARENS = 0;
    private static final int PARENS1 = 1;
    private static final int PARENS2 = 2;

    public static final int AFTER = 3;
    public static final int BEFORE = 4;
    private static final int BETWEEN = 5;

    private int intPreferredWidth;    // See note in constructor.
    private int intPreferredHeight;   // See note in constructor.
    private DateTimeFormatter dtf;
    private Date dateWhen1;
    private Date dateWhen2;
    private LocalDate noteWhen1;
    private LocalDate noteWhen2;
    private Date dateMod1;
    private Date dateMod2;
    private LocalDate dateLastMod1;
    private LocalDate dateLastMod2;
    private ButtonGroup bgWhen;
    private ButtonGroup bgMod;
    private YearView yvDateChooser;
    private boolean blnDialogClosed; // For use by inner classes
    //-------------------------------------------------------------


    //-----
    private JCheckBox chkboxGoals;
    private JCheckBox chkboxDayNotes;
    private JCheckBox chkboxMonthNotes;
    private JCheckBox chkboxYearNotes;
    private JCheckBox chkboxEvents;
    private JCheckBox chkboxTodoLists;
    private JPanel pnlWhere;
    //-----
    private JPanel pnlKeywords;
    private JLabel lblOpenParen1;
    private JCheckBox chkboxNot1;
    private JComboBox<String> comboxSearchText1;
    //-----
    private JLabel lblOpenParen2;
    private JLabel lblCloseParen1;
    private JRadioButton rbtnAnd1;
    private JRadioButton rbtnOr1;
    private JCheckBox chkboxNot2;
    private JComboBox<String> comboxSearchText2;
    //-----
    private JLabel lblCloseParen2;
    private JRadioButton rbtnAnd2;
    private JRadioButton rbtnOr2;
    private JCheckBox chkboxNot3;
    private JComboBox<String> comboxSearchText3;
    //-----
    private JPanel pnlWhen;
    //-----
    private JPanel pnlLastMod;
    //-----
    private JLabel lblWhenSelected;
    //-----
    private JRadioButton rbtnModBefore;
    private JRadioButton rbtnModBetween;
    private JRadioButton rbtnModAfter;
    //-----
    private JRadioButton rbtnWhenAfter;
    private JRadioButton rbtnWhenBefore;
    private JRadioButton rbtnWhenBetween;
    //-----
    private JLabel lblModSelected;
    //-----
    // End of variables declaration


    public SearchPanel() {
        super();
        initializeComponent();

        // Although size was set explicitly in initializeComponent, each call to getSize()
        //   reported a smaller amount by (6,25) after each time the dialog was closed.
        //   This method of capturing the initial size is the workaround.  Now the
        //   EventNoteGroup calls getMinimumSize (defined below) to get the size value.
        intPreferredWidth = getSize().width;
        intPreferredHeight = getSize().height;
        blnDialogClosed = false;

        reinitializeComponent();
    } // end constructor


    /**
     * Add Component Without a Layout Manager (Absolute Positioning)
     */
    private void addComponent(Container container, Component c, int x, int y, int width, int height) {
        c.setBounds(x, y, width, height);
        container.add(c);
    }

    //------------------------------------------------------------
    // Method Name: checkAllFalse
    //
    // This method is a simple check to see if the only specified
    //   conditions evaluated to false.  If so
    //   then the truth table will certainly also evaluate to false.
    // The return value will be true if this situation is found,
    //   and the presence or lack of parens is not a factor.
    //------------------------------------------------------------
    private boolean checkAllFalse(Boolean blnC1, Boolean blnC2, Boolean blnC3) {

        if (blnC1 != null) if (blnC1) return false;
        if (blnC2 != null) if (blnC2) return false;
        if (blnC3 != null) if (blnC3) return false;

        if (blnC1 == null)
            if (blnC2 == null)
                return blnC3 != null;

        return true;
    } // end checkAllFalse


    //------------------------------------------------------------
    // Method Name: checkFalseCombo
    //
    // This method is a simple check to see if there is either a
    //   (false AND x) or (x AND false) situation specified.  If so
    //   then the truth table will certainly also evaluate to false.
    // The return value will be true if this situation is found,
    //   but it will only be valid in the absence of parens.
    //------------------------------------------------------------
    private boolean checkFalseCombo(Boolean blnC1, Boolean blnC2, Boolean blnC3) {
        boolean rv = false;

        if ((blnC1 != null) && (!blnC1)) {
            if ((rbtnAnd1.isSelected() && blnC2 != null)) return true;   // C1 && C2
            if (blnC2 == null) {
                if ((rbtnAnd2.isSelected() && blnC3 != null)) return true; // C1 && C3
            } // end if C2 is null
        } // end if C1 is false

        if ((blnC2 != null) && (!blnC2)) {
            if ((rbtnAnd1.isSelected() && blnC1 != null)) return true; // C1 && C2
            if ((rbtnAnd2.isSelected() && blnC3 != null)) return true; // C2 && C3
        } // end if C1 is false

        if ((blnC3 != null) && (!blnC3)) {
            if (blnC2 == null) {
                if ((rbtnAnd2.isSelected() && blnC1 != null)) return true; // C1 && C3
            } // end if C2 is null
            if ((rbtnAnd2.isSelected() && blnC2 != null)) return true;   // C2 && C3
        } // end if C1 is false

        return rv;
    } // end checkFalseCombo


    //--------------------------------------------------------------------
    // Method Name: filterLastMod
    //
    // Tests the input Date against the 'Last Mod' date settings and
    //   returns true if the date passes thru the filter; otherwise
    //   returns a false.
    //
    // Relies on the constraints of the operation of the interface to
    //   ensure that if a setting has been chosen then the appropriate
    //   date fields are also set.  If no setting has been made then
    //   we return the default - true.
    //--------------------------------------------------------------------
    boolean filterLastMod(LocalDate dateInQuestion) {
        boolean rv = true;
        switch (getLastModSetting()) {
            case AFTER:
                if (dateInQuestion.isBefore(dateLastMod1)) rv = false;
                break;
            case BEFORE:
                if (dateInQuestion.isAfter(dateLastMod1)) rv = false;
                break;
            case BETWEEN:
                if (dateInQuestion.isBefore(dateLastMod1)) rv = false;
                if (dateInQuestion.isAfter(dateLastMod2)) rv = false;
                break;
        } // end switch

        return !rv;
    } // end filterLastMod


    //--------------------------------------------------------------------
    // Method Name: filterWhen
    //
    // Tests the input Date against the 'When' date settings and
    //   returns true if the date passes the filter; otherwise returns
    //   a false.
    //
    // Relies on the constraints of the operation of the interface to
    //   ensure that if a setting has been chosen then the appropriate
    //   date fields are also set.  If no setting has been made then
    //   we return the default - true.
    //--------------------------------------------------------------------
    boolean filterWhen(LocalDate d) {
        boolean rv = true;
        if (d == null) return false;

        switch (getWhenSetting()) {
            case AFTER:
                if (d.isBefore(noteWhen1)) rv = false;
                break;
            case BEFORE:
                if (d.isAfter(noteWhen1)) rv = false;
                break;
            case BETWEEN:
                if (d.isBefore(noteWhen1)) rv = false;
                if (d.isAfter(noteWhen2)) rv = false;
                break;
        } // end switch

        return !rv;
    } // end filterWhen


    //-------------------------------------------------------------------
    // Method Name: foundIt
    //
    // Returns true if the input NoteData passes the various
    //   filters of the interface; otherwise false.
    //
    // When it comes to text searching, the search will be done as
    //   case insensitive.  The user currently does not have the
    //   option to change this.
    //-------------------------------------------------------------------
    boolean foundIt(NoteData nd) {
        // Remember that the Panel is for filtering OUT results; therefore
        //   the default is true (found), but if it fails even one test
        //   then the return value will be false.

        // Test the 'Last Mod' condition
        //if (filterLastMod(Date.from(nd.getLastModDate().toInstant()))) return false;
        if (filterLastMod(nd.getLastModDate().toLocalDate())) return false;

        // Test the 'Items with Dates' condition
        // File-level filtering for DayNotes, MonthNotes, etc, should
        //   have already been done prior to this point; here, it is
        //   a Note-level test.  If not relevant then the NoteDate will
        //   be null, which passes thru the filter.
//        if (filterWhen(nd.getNoteDate())) return false; - no longer possible, didn't make sense anyway.

        // Construct a string to search, from the base NoteData elements.
        String s = nd.getNoteString();
        s += nd.getSubjectString();
        s += nd.getExtendedNoteString();
        s = s.toLowerCase(); // For case-insensitivity

        // These text conditions include the 'NOT' toggle, so that if
        //   NOT is checked and the word is not found, it is true.  We
        //   are using the object vs the primitive type, so that we can
        //   also make use of a third possible value - null.
        Boolean blnC1;  // 'C' is short for Condition
        Boolean blnC2;
        Boolean blnC3;

        // First, we evaluate the three individual conditions.
        //------------------------------------------------------
        String strW1 = getWord1();
        if (strW1 != null) {
            // A non-specified word is a default 'found'.
            if (chkboxNot1.isSelected()) {
                blnC1 = !s.contains(strW1.toLowerCase());
            } else {
                blnC1 = s.contains(strW1.toLowerCase());
            } // end if NOT, else
        } else {
            // If the word was not even filled in, it should not affect
            //   the outcome of the search.  We need this as a flag
            //   that tells us - do not consider this condition.
            blnC1 = null;
        } // end if

        String strW2 = getWord1();
        if (strW2 != null) {
            // A non-specified word is a default 'found'.
            if (chkboxNot2.isSelected()) {
                blnC2 = !s.contains(strW2.toLowerCase());
            } else {
                blnC2 = s.contains(strW2.toLowerCase());
            } // end if NOT, else
        } else {
            blnC2 = null;
        } // end if

        String strW3 = getWord3();
        if (strW3 != null) {
            // A non-specified word is a default 'found'.
            if (chkboxNot3.isSelected()) {
                blnC3 = !s.contains(strW3.toLowerCase());
            } else {
                blnC3 = s.contains(strW3.toLowerCase());
            } // end if NOT, else
        } else {
            blnC3 = null;
        } // end if

        // Now we know whether each condition evaluates to T, F, or null.
        // The possible variations of these three conditions, along with
        //   their possible combinations of AND/OR and in some cases
        //   grouped with parentheses, results in a truth table that has
        //   a total of 79 entries.  Of those, we are only interested in
        //   the 39 that evaluate to false, so that we can return early.
        // The complete truth table is contained in the documentation;
        //   the mapping to statements is also there.

        // This first test applies to 15 of the 39 cases,
        //   regardless of whether or not there are parens.
        if (checkAllFalse(blnC1, blnC2, blnC3)) return false; // Aaf

        // Now we consider the presence or lack of the parentheses -
        if (getParens() == NOPARENS) {
            return !checkFalseCombo(blnC1, blnC2, blnC3); // N1fc
            // This test covered the remaining 12 cases without parens.
        } else { // 12 more (with parens) to go -
            if (getParens() == PARENS1) { // 3 cases
                if (rbtnAnd2.isSelected() && (blnC3 != null && !blnC3)) return false; // P1aC3f
            } // end if

            if (getParens() == PARENS2) { // 3 cases
                if ((blnC1 != null && !blnC1) && rbtnAnd1.isSelected()) return false; // P2C1fa
            } // end if

            // The remaining 6 cases apply whether they are P1 or P2
            // So now we break them out according to AND/OR -
            // (When we test for AND, we know the other is an OR, or else
            //   we wouldn't have the parens in the first place).
            assert blnC1 != null;
            assert blnC2 != null;
            assert blnC3 != null;
            if (rbtnAnd1.isSelected()) { // 3 cases
                if (blnC1 && !blnC2 && !blnC3) return false; // Ptafof (2)
                if (!blnC1 && blnC2 && !blnC3) return false; // Pfatof (1)
            } // end if

            if (rbtnAnd2.isSelected()) { // 3 cases
                if (!blnC1 && !blnC2 && blnC3) return false; // Pfofat (2)
                return blnC1 || !blnC2 || blnC3; // Pfotaf (1)
            } // end if
        } // end if/else PARENS
        return true;
    } // end foundIt

    private boolean getAnd1() {
        return rbtnAnd1.isSelected();
    }

    private boolean getAnd2() {
        return rbtnAnd2.isSelected();
    }

    // This method is only here to 'fool' JFramebuilder-generated code.
    private JPanel getContentPane() {
        return SearchPanel.this;
    }

    private LocalDate getDateLastMod1() {
        return dateLastMod1;
    }

    private LocalDate getDateLastMod2() {
        return dateLastMod2;
    }

    private  LocalDate getnoteWhen1() {
        return noteWhen1;
    }

    private LocalDate getnoteWhen2() {
        return noteWhen2;
    }

    int getLastModSetting() {
        if (rbtnModAfter.isSelected()) return AFTER;
        if (rbtnModBefore.isSelected()) return BEFORE;
        if (rbtnModBetween.isSelected()) return BETWEEN;
        return -1; // error condition
    } // end getLastModSetting


    public Dimension getMinimumSize() {
        return new Dimension(intPreferredWidth, intPreferredHeight);
    } // end getMinimumSize

    private boolean getNot1() {
        return chkboxNot1.isSelected();
    }

    private boolean getNot2() {
        return chkboxNot2.isSelected();
    }

    private boolean getNot3() {
        return chkboxNot3.isSelected();
    }

    private boolean getOr1() {
        return rbtnOr1.isSelected();
    }

    private boolean getOr2() {
        return rbtnOr2.isSelected();
    }

    // Used by the static JOptionPane 'show' methods.
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }


    private int getParens() {
        if (getWord1() == null) return NOPARENS;
        if (getWord2() == null) return NOPARENS;
        if (getWord3() == null) return NOPARENS;

        if (lblOpenParen1.isVisible()) return PARENS1;
        if (lblOpenParen2.isVisible()) return PARENS2;
        return NOPARENS;
    } // end getParens

    SearchPanelSettings getSettings() {
        SearchPanelSettings sps = new SearchPanelSettings();

        sps.not1 = getNot1();
        sps.not2 = getNot2();
        sps.not3 = getNot3();

        sps.word1 = getWord1();
        sps.word2 = getWord2();
        sps.word3 = getWord3();

        sps.and1 = getAnd1();
        sps.and2 = getAnd2();
        sps.or1 = getOr1();
        sps.or2 = getOr2();

        sps.paren1 = false;
        sps.paren2 = false;
        int parens = getParens();
        if (parens == SearchPanel.PARENS1) sps.paren1 = true;
        if (parens == SearchPanel.PARENS2) sps.paren2 = true;

        sps.setNoteDateWhen1(getnoteWhen1());
        sps.setNoteDateWhen2(getnoteWhen2());
        sps.whenChoice = getWhenSetting();

        sps.setDateLastMod1(getDateLastMod1());
        sps.setDateLastMod2(getDateLastMod2());
        sps.modChoice = getLastModSetting();

        return sps;
    } // end getSettings


    //--------------------------------------------------------------------
    // Returns a String that can be displayed to indicate the nature
    //   of the specified search.  Based solely on the text combinations
    //   and groupings, not dates or locations.
    //--------------------------------------------------------------------
    static String getSummary(SearchPanelSettings sps) {
        // examine words, and/or/not, and parens.

        String s;
        String strAnd, strOr;
        String strNot;

        int intMakeup = 0;
        if ((sps.word1 != null) && (!sps.word1.trim().equals(""))) intMakeup += 1;
        if ((sps.word2 != null) && (!sps.word2.trim().equals(""))) intMakeup += 2;
        if ((sps.word3 != null) && (!sps.word3.trim().equals(""))) intMakeup += 4;

        switch (intMakeup) {
            case 1:
                if (sps.not1) strNot = "NOT ";
                else strNot = "";
                s = "Search Text was: " + strNot + sps.word1;
                break;
            case 2:
                if (sps.not2) strNot = "NOT ";
                else strNot = "";
                s = "Search Text was: " + strNot + sps.word2;
                break;
            case 3:
                if (sps.and1) strAnd = " AND ";
                else strAnd = "";
                if (sps.or1) strOr = " OR ";
                else strOr = "";

                if (sps.not1) strNot = "NOT ";
                else strNot = "";
                s = "Search Text was: " + strNot + sps.word1;
                s += strAnd + strOr;

                if (sps.not2) strNot = "NOT ";
                else strNot = "";
                s += strNot + sps.word2;
                break;
            case 4:
                if (sps.not3) strNot = "NOT ";
                else strNot = "";
                s = "Search Text was: " + strNot + sps.word3;
                break;
            case 5:
                if (sps.and2) strAnd = " AND ";
                else strAnd = "";
                if (sps.or2) strOr = " OR ";
                else strOr = "";

                if (sps.not1) strNot = "NOT ";
                else strNot = "";
                s = "Search Text was: " + strNot + sps.word1;
                s += strAnd + strOr;

                if (sps.not3) strNot = "NOT ";
                else strNot = "";
                s += strNot + sps.word3;
                break;
            case 6:
                if (sps.and2) strAnd = " AND ";
                else strAnd = "";
                if (sps.or2) strOr = " OR ";
                else strOr = "";

                if (sps.not2) strNot = "NOT ";
                else strNot = "";
                s = "Search Text was: " + strNot + sps.word2;
                s += strAnd + strOr;

                if (sps.not3) strNot = "NOT ";
                else strNot = "";
                s += strNot + sps.word3;
                break;
            case 7:
                s = "Search Text was: ";
                if (sps.paren1) s += "(";
                if (sps.not1) strNot = "NOT ";
                else strNot = "";
                s += strNot + sps.word1;
                if (sps.and1) strAnd = " AND ";
                else strAnd = "";
                if (sps.or1) strOr = " OR ";
                else strOr = "";
                s += strAnd + strOr;

                if (sps.paren2) s += "(";
                if (sps.not2) strNot = "NOT ";
                else strNot = "";
                s += strNot + sps.word2;
                if (sps.paren1) s += ")";
                if (sps.and2) strAnd = " AND ";
                else strAnd = "";
                if (sps.or2) strOr = " OR ";
                else strOr = "";
                s += strAnd + strOr;

                if (sps.not3) strNot = "NOT ";
                else strNot = "";
                s += strNot + sps.word3;
                if (sps.paren2) s += ")";
                break;
            default:  // case 0, but this eliminates the complaint.
                s = "No Search Text was specified";
                break;
        } // end switch

        return s;
    } // end getSummary


    private int getWhenSetting() {
        if (rbtnWhenAfter.isSelected()) return AFTER;
        if (rbtnWhenBefore.isSelected()) return BEFORE;
        if (rbtnWhenBetween.isSelected()) return BETWEEN;
        return -1; // error condition
    } // end getWhenSetting

    private String getWord1() {
        String s = null;
        Object o = comboxSearchText1.getSelectedItem();
        if (o != null) {
            s = o.toString().trim();
            if (s.equals("")) s = null;
        } // end if
        return s;
    } // end getWord1


    private String getWord2() {
        String s = null;
        Object o = comboxSearchText2.getSelectedItem();
        if (o != null) {
            s = o.toString().trim();
            if (s.equals("")) s = null;
        } // end if
        return s;
    } // end getWord2


    private String getWord3() {
        String s = null;
        Object o = comboxSearchText3.getSelectedItem();
        if (o != null) {
            s = o.toString().trim();
            if (s.equals("")) s = null;
        } // end if
        return s;
    } // end getWord3


    // Called when an AND or an OR radio button was clicked.
    private void handleAndOrChanged() {
        boolean blnNeedParens = false;
        boolean blnToggle = lblOpenParen2.isVisible();

        lblOpenParen1.setVisible(false);
        lblOpenParen2.setVisible(false);
        lblCloseParen1.setVisible(false);
        lblCloseParen2.setVisible(false);

        rbtnAnd1.setToolTipText(null);
        rbtnAnd2.setToolTipText(null);
        rbtnOr1.setToolTipText(null);
        rbtnOr2.setToolTipText(null);

        String s = "Click again to move parentheses";
        if (rbtnAnd1.isSelected() && rbtnOr2.isSelected()) {
            blnNeedParens = true;
            rbtnAnd1.setToolTipText(s);
            rbtnOr2.setToolTipText(s);
        } // end if

        if (rbtnOr1.isSelected() && rbtnAnd2.isSelected()) {
            blnNeedParens = true;
            rbtnOr1.setToolTipText(s);
            rbtnAnd2.setToolTipText(s);
        } // end if

        if (blnNeedParens) {
            if (blnToggle) {
                lblOpenParen1.setVisible(true);
                lblCloseParen1.setVisible(true);
            } else {
                lblOpenParen2.setVisible(true);
                lblCloseParen2.setVisible(true);
            }
        } // end if

    } // end handleAndOrChanged


    private void handleDateSpecChange(JRadioButton source, boolean rc) {
        String strPrompt = "";
        int intChoiceCount = 1;
        String strWhich = source.getName();

        if (rc) { // We are deselecting -
            ButtonGroup bg;
            if (strWhich.contains("When")) {
                bg = bgWhen;
                noteWhen1 = null;
                noteWhen2 = null;
            } else {      // Mod
                bg = bgMod;
                dateLastMod1 = null;
                dateLastMod2 = null;
            } // end if
            bg.remove(source);
            source.setSelected(false);
            bg.add(source);
        } else { // We are selecting -
            // Set the Prompt string
            if (strWhich.contains("Before")) {
                strPrompt = "Select the Date that you want to search BEFORE";
                noteWhen2 = null;
                dateLastMod2 = null;
            } else if (strWhich.contains("Between")) {
                strPrompt = "Select the Dates that you want to search BETWEEN";
                intChoiceCount = 2;
            } else if (strWhich.contains("After")) {
                strPrompt = "Select the Date that you want to search AFTER";
                noteWhen2 = null;
                dateLastMod2 = null;
            } // end if

            // Show the date chooser dialog and set the corresponding date(s).
            if (strWhich.contains("When")) {
                if (noteWhen1 != null) yvDateChooser.setChoice(noteWhen1);
                else yvDateChooser.setChoice(null);

                showDateDialog(strPrompt, intChoiceCount);
                if (blnDialogClosed) {
                    if (noteWhen1 == null) handleDateSpecChange(source, true);
                    if ((intChoiceCount == 2) && (noteWhen2 == null)) handleDateSpecChange(source, true);
                } else {
                    noteWhen1 = yvDateChooser.getChoice();
                    if (intChoiceCount == 2) {
                        LocalDate dateTmp = yvDateChooser.getChoice2();
                        noteWhen2 = noteWhen1;
                        if (dateTmp.isBefore(noteWhen1)) noteWhen1 = dateTmp;
                        else noteWhen2 = dateTmp;
                    } // end if
                } // end if
            } else {  //      "Mod"
                if (dateLastMod1 != null) yvDateChooser.setChoice(dateLastMod1);
                else yvDateChooser.setChoice(null);

                showDateDialog(strPrompt, intChoiceCount);
                if (blnDialogClosed) {
                    if (dateLastMod1 == null) handleDateSpecChange(source, true);
                    if ((intChoiceCount == 2) && (dateLastMod2 == null)) handleDateSpecChange(source, true);
                } else {
                    dateLastMod1 = yvDateChooser.getChoice();
                    if (intChoiceCount == 2) {
                        LocalDate dateTmp = yvDateChooser.getChoice2();
                        dateLastMod2 = dateLastMod1;
                        if (dateTmp.isBefore(dateLastMod1)) dateLastMod1 = dateTmp;
                        else dateLastMod2 = dateTmp;
                    } // end if
                } // end if
            } // end if
        } // end if

        resetDateDisplay();
    } // end handleDateSpecChanged


    //----------------------------------------------------------
    // Method Name: hasWhere
    //
    // Returns true if the user has specified at least
    //   one place to search; otherwise false.
    //----------------------------------------------------------
    boolean hasWhere() {
        boolean retVal = false;
        if (chkboxGoals.isSelected()) retVal = true;
        if (chkboxDayNotes.isSelected()) retVal =  true;
        if (chkboxMonthNotes.isSelected()) retVal =  true;
        if (chkboxYearNotes.isSelected()) retVal =  true;
        if (chkboxEvents.isSelected()) retVal =  true;
        if (chkboxTodoLists.isSelected()) retVal = true;
        return retVal;
    } // end hasWhere


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always regenerated
     * by the Windows Form Designer. Otherwise, retrieving design might not work properly.
     * Tip: If you must revise this method, please backup this GUI file for JFrameBuilder
     * to retrieve your design properly in future, before revising this method.
     */
    private void initializeComponent() {
        JPanel contentPane = this.getContentPane();
        //----- 
        chkboxGoals = new JCheckBox();
        chkboxDayNotes = new JCheckBox();
        chkboxMonthNotes = new JCheckBox();
        chkboxYearNotes = new JCheckBox();
        chkboxEvents = new JCheckBox();
        chkboxTodoLists = new JCheckBox();
        pnlWhere = new JPanel();
        //----- 
        pnlKeywords = new JPanel();
        //----- 
        JLabel jLabel3 = new JLabel();
        lblOpenParen1 = new JLabel();
        chkboxNot1 = new JCheckBox();
        comboxSearchText1 = new JComboBox<>();
        JPanel pnlKeyword1 = new JPanel();
        //----- 
        lblOpenParen2 = new JLabel();
        lblCloseParen1 = new JLabel();
        rbtnAnd1 = new JRadioButton();
        rbtnOr1 = new JRadioButton();
        chkboxNot2 = new JCheckBox();
        comboxSearchText2 = new JComboBox<>();
        JPanel pnlKeyword2 = new JPanel();
        //----- 
        lblCloseParen2 = new JLabel();
        rbtnAnd2 = new JRadioButton();
        rbtnOr2 = new JRadioButton();
        chkboxNot3 = new JCheckBox();
        comboxSearchText3 = new JComboBox<>();
        JPanel pnlKeyword3 = new JPanel();
        //----- 
        pnlWhen = new JPanel();
        //----- 
        pnlLastMod = new JPanel();
        //----- 
        lblWhenSelected = new JLabel();
        JPanel jPanel10 = new JPanel();
        //----- 
        rbtnModBefore = new JRadioButton();
        rbtnModBetween = new JRadioButton();
        rbtnModAfter = new JRadioButton();
        JPanel jPanel12 = new JPanel();
        //----- 
        rbtnWhenAfter = new JRadioButton();
        rbtnWhenBefore = new JRadioButton();
        rbtnWhenBetween = new JRadioButton();
        JPanel jPanel15 = new JPanel();
        //----- 
        lblModSelected = new JLabel();
        JPanel jPanel17 = new JPanel();
        //----- 

        // 
        // contentPane 
        // 
        contentPane.setLayout(null);
        addComponent(contentPane, pnlWhere, 293, 130, 133, 205);
        addComponent(contentPane, pnlKeywords, 9, 10, 416, 110);
        addComponent(contentPane, pnlWhen, 9, 130, 272, 100);
        addComponent(contentPane, pnlLastMod, 9, 237, 272, 100);
        // 
        // chkboxGoals 
        // 
        chkboxGoals.setText("Goals");
        chkboxGoals.setSelected(true);
        // 
        // chkboxDayNotes 
        // 
        chkboxDayNotes.setText("Day Notes");
        chkboxDayNotes.setSelected(true);
        // 
        // chkboxMonthNotes 
        // 
        chkboxMonthNotes.setText("Month Notes");
        chkboxMonthNotes.setSelected(true);
        // 
        // chkboxYearNotes 
        // 
        chkboxYearNotes.setText("Year Notes");
        chkboxYearNotes.setSelected(true);
        // 
        // chkboxEvents 
        // 
        chkboxEvents.setText("Upcoming Events");
        chkboxEvents.setSelected(true);
        // 
        // chkboxTodoLists 
        // 
        chkboxTodoLists.setText("To Do Lists");
        chkboxTodoLists.setSelected(true);
        // 
        // pnlWhere 
        // 
        pnlWhere.setLayout(new BoxLayout(pnlWhere, BoxLayout.Y_AXIS));
        pnlWhere.add(chkboxGoals, 0);
        pnlWhere.add(chkboxDayNotes, 1);
        pnlWhere.add(chkboxMonthNotes, 2);
        pnlWhere.add(chkboxYearNotes, 3);
        pnlWhere.add(chkboxEvents, 4);
        pnlWhere.add(chkboxTodoLists, 5);
        pnlWhere.setBorder(new TitledBorder("Title"));
        // 
        // pnlKeywords 
        // 
        pnlKeywords.setLayout(new BoxLayout(pnlKeywords, BoxLayout.Y_AXIS));
        pnlKeywords.add(pnlKeyword1, 0);
        pnlKeywords.add(pnlKeyword2, 1);
        pnlKeywords.add(pnlKeyword3, 2);
        pnlKeywords.setBorder(new TitledBorder("Title"));
        // 
        // jLabel3 
        // 
        jLabel3.setText("Keyword(s)");
        // 
        // lblOpenParen1 
        // 
        lblOpenParen1.setText("(");
        // 
        // chkboxNot1 
        // 
        chkboxNot1.setText("NOT");
        // 
        // comboxSearchText1 
        // 
        comboxSearchText1.setEditable(true);
        comboxSearchText1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                comboxSearchText1_actionPerformed();
            }

        });
        // 
        // pnlKeyword1 
        // 
        pnlKeyword1.setLayout(null);
        addComponent(pnlKeyword1, jLabel3, 20, 5, 60, 18);
        addComponent(pnlKeyword1, lblOpenParen1, 111, 6, 15, 18);
        addComponent(pnlKeyword1, chkboxNot1, 120, 5, 47, 23);
        addComponent(pnlKeyword1, comboxSearchText1, 170, 6, 210, 21);
        // 
        // lblOpenParen2 
        // 
        lblOpenParen2.setText("(");
        // 
        // lblCloseParen1 
        // 
        lblCloseParen1.setText(")");
        // 
        // rbtnAnd1 
        // 
        rbtnAnd1.setText("AND");
        // 
        // rbtnOr1 
        // 
        rbtnOr1.setText("OR");
        // 
        // chkboxNot2 
        // 
        chkboxNot2.setText("NOT");
        // 
        // comboxSearchText2 
        // 
        comboxSearchText2.setEditable(true);
        comboxSearchText2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                comboxSearchText2_actionPerformed();
            }

        });
        // 
        // pnlKeyword2 
        // 
        pnlKeyword2.setLayout(null);
        addComponent(pnlKeyword2, lblOpenParen2, 111, 6, 60, 18);
        addComponent(pnlKeyword2, lblCloseParen1, 389, 5, 60, 18);
        addComponent(pnlKeyword2, rbtnAnd1, 2, 5, 47, 23);
        addComponent(pnlKeyword2, rbtnOr1, 56, 5, 41, 23);
        addComponent(pnlKeyword2, chkboxNot2, 120, 5, 47, 23);
        addComponent(pnlKeyword2, comboxSearchText2, 170, 6, 210, 21);
        // 
        // lblCloseParen2 
        // 
        lblCloseParen2.setText(")");
        // 
        // rbtnAnd2 
        // 
        rbtnAnd2.setText("AND");
        // 
        // rbtnOr2 
        // 
        rbtnOr2.setText("OR");
        // 
        // chkboxNot3 
        // 
        chkboxNot3.setText("NOT");
        // 
        // comboxSearchText3 
        // 
        comboxSearchText3.setEditable(true);
        comboxSearchText3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                comboxSearchText3_actionPerformed();
            }

        });
        // 
        // pnlKeyword3 
        // 
        pnlKeyword3.setLayout(null);
        addComponent(pnlKeyword3, lblCloseParen2, 389, 5, 60, 18);
        addComponent(pnlKeyword3, rbtnAnd2, 2, 5, 47, 23);
        addComponent(pnlKeyword3, rbtnOr2, 56, 5, 41, 23);
        addComponent(pnlKeyword3, chkboxNot3, 120, 5, 47, 23);
        addComponent(pnlKeyword3, comboxSearchText3, 170, 6, 210, 21);
        // 
        // pnlWhen 
        // 
        pnlWhen.setLayout(new BoxLayout(pnlWhen, BoxLayout.X_AXIS));
        pnlWhen.add(jPanel15, 0);
        pnlWhen.add(jPanel10, 1);
        pnlWhen.setBorder(new TitledBorder("Title"));
        // 
        // pnlLastMod 
        // 
        pnlLastMod.setLayout(new BoxLayout(pnlLastMod, BoxLayout.X_AXIS));
        pnlLastMod.add(jPanel12, 0);
        pnlLastMod.add(jPanel17, 1);
        pnlLastMod.setBorder(new TitledBorder("Title"));
        // 
        // lblWhenSelected 
        // 
        lblWhenSelected.setHorizontalAlignment(SwingConstants.CENTER);
        lblWhenSelected.setText("Selected Date");
        // 
        // jPanel10 
        // 
        jPanel10.setLayout(null);
        addComponent(jPanel10, lblWhenSelected, 12, 7, 166, 57);
        // 
        // rbtnModBefore 
        // 
        rbtnModBefore.setText("Before");
        // 
        // rbtnModBetween 
        // 
        rbtnModBetween.setText("Between");
        // 
        // rbtnModAfter 
        // 
        rbtnModAfter.setText("After");
        // 
        // jPanel12 
        // 
        jPanel12.setLayout(new BoxLayout(jPanel12, BoxLayout.Y_AXIS));
        jPanel12.add(rbtnModBefore, 0);
        jPanel12.add(rbtnModBetween, 1);
        jPanel12.add(rbtnModAfter, 2);
        // 
        // rbtnWhenAfter 
        // 
        rbtnWhenAfter.setText("After");
        // 
        // rbtnWhenBefore 
        // 
        rbtnWhenBefore.setText("Before");
        // 
        // rbtnWhenBetween 
        // 
        rbtnWhenBetween.setText("Between");
        // 
        // jPanel15 
        // 
        jPanel15.setLayout(new BoxLayout(jPanel15, BoxLayout.Y_AXIS));
        jPanel15.add(rbtnWhenBefore, 0);
        jPanel15.add(rbtnWhenBetween, 1);
        jPanel15.add(rbtnWhenAfter, 2);
        // 
        // lblModSelected 
        // 
        lblModSelected.setHorizontalAlignment(SwingConstants.CENTER);
        lblModSelected.setText("Selected Date");
        // 
        // jPanel17 
        // 
        jPanel17.setLayout(null);
        addComponent(jPanel17, lblModSelected, 12, 7, 166, 57);
        // 
        // SearchPanel
        // 
        this.setTitle();
        this.setLocation(new Point(16, 0));
        this.setSize(new Dimension(443, 337));
    } // end initializeComponent


    private void comboxSearchText1_actionPerformed() {
        System.out.println("\ncomboxSearchText1_actionPerformed(ActionEvent e) called.");

        Object o = comboxSearchText1.getSelectedItem();
        System.out.println(">>" + ((o == null) ? "null" : o.toString()) + " is selected.");
        // TODO: Add any handling code here for the particular object being selected 

    }

    private void comboxSearchText2_actionPerformed() {
        System.out.println("\ncomboxSearchText2_actionPerformed(ActionEvent e) called.");

        Object o = comboxSearchText2.getSelectedItem();
        System.out.println(">>" + ((o == null) ? "null" : o.toString()) + " is selected.");
        // TODO: Add any handling code here for the particular object being selected 

    }

    private void comboxSearchText3_actionPerformed() {
        System.out.println("\ncomboxSearchText3_actionPerformed(ActionEvent e) called.");

        Object o = comboxSearchText3.getSelectedItem();
        System.out.println(">>" + ((o == null) ? "null" : o.toString()) + " is selected.");
        // TODO: Add any handling code here for the particular object being selected 

    }


    /**
     * This method is called from within the constructor.
     * It continues the work of the JFrameBuilder generated code.
     * Replaces some standard components with custom types, retaining their
     * sizes and positions.  Also additional property settings.
     */
    private void reinitializeComponent() {
        yvDateChooser = new YearView(LocalDate.now());

        TitledBorder tb;   // For a shorter pointer to the panel borders.

        // Prepare our date formatter.
        dtf = DateTimeFormatter.ofPattern("EEE  d MMM yyyy");

        // Fill in the border titles -
        //-----------------------------------------------
        tb = (TitledBorder) pnlWhere.getBorder();
        tb.setTitle("Where to search");
        tb.setTitleFont(Font.decode("Dialog-bold-14"));

        tb = (TitledBorder) pnlKeywords.getBorder();
        tb.setTitle("What word(s) to look for");
        tb.setTitleFont(Font.decode("Dialog-bold-14"));

        tb = (TitledBorder) pnlWhen.getBorder();
        tb.setTitle("Look for items with Dates");
        tb.setTitleFont(Font.decode("Dialog-bold-14"));

        tb = (TitledBorder) pnlLastMod.getBorder();
        tb.setTitle("In a note that was Last Modified");
        tb.setTitleFont(Font.decode("Dialog-bold-14"));
        //-----------------------------------------------

        // Make button groups for radio buttons
        //-----------------------------------------------
        ButtonGroup bg1 = new ButtonGroup();
        bg1.add(rbtnAnd1);
        bg1.add(rbtnOr1);

        ButtonGroup bg2 = new ButtonGroup();
        bg2.add(rbtnAnd2);
        bg2.add(rbtnOr2);

        rbtnOr1.setSelected(true);
        rbtnOr2.setSelected(true);

        bgWhen = new ButtonGroup();
        bgWhen.add(rbtnWhenBefore);
        bgWhen.add(rbtnWhenBetween);
        bgWhen.add(rbtnWhenAfter);

        bgMod = new ButtonGroup();
        bgMod.add(rbtnModBefore);
        bgMod.add(rbtnModBetween);
        bgMod.add(rbtnModAfter);
        //-----------------------------------------------

        // Increase the size and visibility of the parens
        lblOpenParen1.setFont(Font.decode("Dialog-bold-14"));
        lblOpenParen2.setFont(Font.decode("Dialog-bold-14"));
        lblCloseParen1.setFont(Font.decode("Dialog-bold-14"));
        lblCloseParen2.setFont(Font.decode("Dialog-bold-14"));

        // Define and assign a handler for parentheses visibility
        //--------------------------------------------------------
        MouseAdapter ma1 = new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                handleAndOrChanged();
            }
        }; // end redefined MouseAdapter

        rbtnAnd1.addMouseListener(ma1);
        rbtnAnd2.addMouseListener(ma1);
        rbtnOr1.addMouseListener(ma1);
        rbtnOr2.addMouseListener(ma1);
        //--------------------------------------------------------
        handleAndOrChanged(); // Initialize to all non-visible

        // Initialize the Date Specification prompts
        noteWhen1 = null;
        noteWhen2 = null;
        dateLastMod1 = null;
        dateLastMod2 = null;
        resetDateDisplay();

        // Define and assign a handler for date specifications
        //--------------------------------------------------------
        MouseAdapter ma2 = new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                boolean rightClick = false;
                int m = me.getModifiers();
                if ((m & InputEvent.BUTTON3_MASK) != 0) rightClick = true;

                JRadioButton source = (JRadioButton) me.getSource();
                if (!source.isSelected() && rightClick) return;
                handleDateSpecChange(source, rightClick);
            }
        }; // end redefined MouseAdapter
        rbtnWhenBefore.setName("rbtnWhenBefore");
        rbtnWhenBetween.setName("rbtnWhenBetween");
        rbtnWhenAfter.setName("rbtnWhenAfter");
        rbtnModBefore.setName("rbtnModBefore");
        rbtnModBetween.setName("rbtnModBetween");
        rbtnModAfter.setName("rbtnModAfter");

        rbtnWhenBefore.addMouseListener(ma2);
        rbtnWhenBetween.addMouseListener(ma2);
        rbtnWhenAfter.addMouseListener(ma2);
        rbtnModBefore.addMouseListener(ma2);
        rbtnModBetween.addMouseListener(ma2);
        rbtnModAfter.addMouseListener(ma2);
        //--------------------------------------------------------


        // do this for searching within results
//  pnlWhere.setVisible(false);

    } // end reinitializeComponent


    private void resetDateDisplay() {
        String s;
        s = "<html>Click on a choice to the left to make";
        s += " your date selection(s).</html>";
        lblWhenSelected.setText(s);
        lblWhenSelected.setFont(Font.decode("Dialog-14"));

        lblModSelected.setText(s);
        lblModSelected.setFont(Font.decode("Dialog-14"));

        if (noteWhen1 != null) {
            s = "<html>" + dtf.format(noteWhen1);
            if (noteWhen2 != null) {
                s += "<br>&nbsp;&nbsp;&nbsp;&nbsp;and<br>" + dtf.format(noteWhen2);
            } // end if
            s += "</html>";
            lblWhenSelected.setText(s);
            lblWhenSelected.setFont(Font.decode("Dialog-bold-14"));
        } // end if

        if (dateLastMod1 != null) {
            s = "<html>" + dtf.format(dateLastMod1);
            if (dateLastMod2 != null) {
                s += "<br>&nbsp;&nbsp;&nbsp;&nbsp;and<br>" + dtf.format(dateLastMod2);
            } // end if
            s += "</html>";
            lblModSelected.setText(s);
            lblModSelected.setFont(Font.decode("Dialog-bold-14"));
        } // end if
    } // end resetDateDisplay

    boolean searchGoals() {
        return chkboxGoals.isSelected();
    }

    boolean searchDays() {
        return chkboxDayNotes.isSelected();
    }

    boolean searchMonths() {
        return chkboxMonthNotes.isSelected();
    }

    boolean searchYears() {
        return chkboxYearNotes.isSelected();
    }

    boolean searchEvents() {
        return chkboxEvents.isSelected();
    }

    boolean searchLists() {
        return chkboxTodoLists.isSelected();
    }

    // Just for JFrameBuilder -
    private void setTitle() {
    }

    private void showDateDialog(String s, int numSelections) {
        // Make a dialog window to choose a date from a Year.
        Frame f = JOptionPane.getFrameForComponent(this);
        JDialog tempwin = new JDialog(f, true);
        blnDialogClosed = false;

        tempwin.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                blnDialogClosed = true;
            }
        });

        tempwin.getContentPane().add(yvDateChooser, BorderLayout.CENTER);
        tempwin.setTitle(s);
        tempwin.setSize(yvDateChooser.getPreferredSize());
        tempwin.setResizable(false);
        yvDateChooser.setDialog(tempwin, numSelections);

        // Center the dialog relative to the main frame.
        tempwin.setLocationRelativeTo(f);

        // Go modal -
        tempwin.setVisible(true);
    } // end showDateDialog


} // end class SearchPanel


