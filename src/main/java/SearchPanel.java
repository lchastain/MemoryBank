import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SearchPanel extends JPanel implements DocumentListener {
    private static final long serialVersionUID = 1L;


    // 'My' Variables declaration section
    //-------------------------------------------------------------
    private static final int NOPARENS = 0;
    private static final int PARENS1 = 1;
    private static final int PARENS2 = 2;

    public static final int AFTER = 3;
    public static final int BEFORE = 4;
    private static final int BETWEEN = 5;

    private final int intPreferredWidth;    // See note in constructor.
    private final int intPreferredHeight;   // See note in constructor.
    private final DateTimeFormatter dtf;
    private LocalDate noteWhen1;
    private LocalDate noteWhen2;
    private LocalDate dateLastMod1;
    private LocalDate dateLastMod2;
    private ButtonGroup bgWhen;
    private ButtonGroup bgMod;
    private ButtonGroup andOr1;
    private ButtonGroup andOr2;

    YearView yvDateChooser;
    private boolean blnDialogClosed; // Has an input dialog been properly dismissed?
    JDialog dialogWindow;
    SearchPanelSettings searchPanelSettings;
    boolean editable;
    //-------------------------------------------------------------

    private JCheckBox chkboxGoals;
    private JCheckBox chkboxDayNotes;
    private JCheckBox chkboxMonthNotes;
    private JCheckBox chkboxYearNotes;
    private JCheckBox chkboxOtherNotes;
    private JCheckBox chkboxPastEvents;
    private JCheckBox chkboxFutureEvents;
    private JCheckBox chkboxTodoLists;
    private JLabel lblOpenParen1;
    private JCheckBox chkboxNot1;
    private JTextField searchText1;
    //-----
    private JLabel lblOpenParen2;
    private JLabel lblCloseParen1;
    private JRadioButton rbtnAnd1;
    private JRadioButton rbtnOr1;
    private JCheckBox chkboxNot2;
    private JTextField searchText2;
    //-----
    private JLabel lblCloseParen2;
    private JRadioButton rbtnAnd2;
    private JRadioButton rbtnOr2;
    private JCheckBox chkboxNot3;
    private JTextField searchText3;
    private JLabel parensOnLabel;
    private JLabel parensOffLabel;
    //-----
    private JLabel lblWhenSelected;
    //-----
    private JRadioButton rbtnModBefore;
    private JRadioButton rbtnModBetween;
    JRadioButton rbtnModAfter;
    //-----
    private JRadioButton rbtnWhenAfter;
    private JRadioButton rbtnWhenBefore;
    private JRadioButton rbtnWhenBetween;
    //-----
    private JLabel lblModSelected;
    //-----

    KeyAdapter typingListener;
    // End of variables declaration


    public SearchPanel() {
        this(null);
    }


    SearchPanel(SearchPanelSettings sps) {
        // Prepare our date formatter.
        dtf = DateTimeFormatter.ofPattern("EEE  d MMM yyyy");

        yvDateChooser = new YearView(LocalDate.now());

        if(sps != null) {
            searchPanelSettings = sps;
            editable = false;
        } else {
            searchPanelSettings = new SearchPanelSettings();
            editable = true;
        }

        buildPanel(); // Build the panel

        // Sizes captured here once, for consistency when used by the 'get<some>Size' methods, below.
        intPreferredWidth = getSize().width;
        intPreferredHeight = getSize().height;
        blnDialogClosed = false;

        if(sps != null) {
            loadTheSettings();
        } else {
            resetKeywordPanel(); // default settings and enablement
        }
    }


    /**
     * Add Component Without a Layout Manager (Absolute Positioning)
     */
    private void addComponent(Container container, Component c, int x, int y, int width, int height) {
        c.setBounds(x, y, width, height);
        container.add(c);
    }

    //------------------------------------------------------------
    // Method Name: allConditionsFalse
    //
    // This method is a simple check to see if all three of the
    //   individual conditions evaluates to false (keeping in mind
    //   that a null is also treated as a false).  If so then the
    //   truth table for the totality of conditions will certainly also evaluate to false.
    // The return value will be true if this situation is found;
    //   the presence or lack of parens is not a factor.
    //------------------------------------------------------------
    private boolean allConditionsFalse(Boolean blnC1, Boolean blnC2, Boolean blnC3) {

        if (blnC1 != null) if (blnC1) return false;
        if (blnC2 != null) if (blnC2) return false;
        if (blnC3 != null) if (blnC3) return false;

        if (blnC1 == null)
            if (blnC2 == null)
                return blnC3 == null;

        return true;
    } // end allConditionsFalse


    private boolean allConditionsNull(Boolean blnC1, Boolean blnC2, Boolean blnC3) {
        return (blnC1 == null && blnC2 == null && blnC3 == null);
    } // end allConditionsNull


    //------------------------------------------------------------
    // Method Name: checkFalseCombo
    //
    // This method is a simple check to see if there is either a
    //   (false AND x) or (x AND false) situation specified.  If so
    //   then that portion of the statement will evaluate to false.
    // The return value from this method will be true if this situation
    //   is found.  Note that this check alone is not used to rule out
    //   a 'hit', because prior checks and parentheses are also a factor.
    //------------------------------------------------------------
    private boolean checkFalseCombo(Boolean blnC1, Boolean blnC2, Boolean blnC3) {
        boolean rv = false;
        // At first glance it would appear that the logic here could be simplified, but
        // the extra wrinkle of having C2 possibly be null while still considering the
        // interactions between C1 and C3 due to rbtnAnd2 being selected is why the
        // extra conditional wrappers are needed.  This constitutes a 'bending over backward'
        // assist to the user, to allow maximum versatility of the SearchPanel keyword
        // combinations, but it does seem overly complex.

        // Tests should show whether we can even get to all the conditions here.

        if ((blnC1 != null) && (!blnC1)) { // if C1 is false
            if ((rbtnAnd1.isSelected() && blnC2 != null)) return true; // C1 && C2
            if (blnC2 == null) {
                if ((rbtnAnd2.isSelected() && blnC3 != null)) return true; // C1 && C3
            } // end if C2 is null
        } // end if C1 is false

        if ((blnC2 != null) && (!blnC2)) { // if C2 is false
            if ((rbtnAnd1.isSelected() && blnC1 != null)) return true; // C2 && C1  (C1 can only be null or true by now)
            if ((rbtnAnd2.isSelected() && blnC3 != null)) return true; // C2 && C3
        } // end if C2 is false

        if ((blnC3 != null) && (!blnC3)) { // if C3 is false
            if (blnC2 == null) {
                if ((rbtnAnd2.isSelected() && blnC1 != null)) return true; // C1 && C3
            } // end if C2 is null
            if ((rbtnAnd2.isSelected() && blnC2 != null)) return true;   // C2 && C3
        } // end if C3 is false

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
                if (dateInQuestion != null) {
                    if(dateInQuestion.isBefore(dateLastMod1)) rv = false;
                }
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
        //   then the return value will be false.  If search text is not provided then
        //   that is not a basis by which to reject an item, so it may pass.  If the
        //   SearchPanel settings are all left at their defaults (with no search text
        //   specified), then ALL notes will be 'found'.

        // Test the 'Last Mod' condition
        if (filterLastMod(nd.getLastModDate().toLocalDate())) return false;

        // Test the 'Items with Dates' condition (but not really; just putting it here for completeness).
        // Filename-level date filtering for DayNotes, MonthNotes, and YearNotes will have already been
        //   done prior to this point, and there is no date associated with a base NoteData.
        //   No further filtering needed.

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

        String strW2 = getWord2();
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

        // The rest of this method is concerned with keyword searching.  If no keywords have been supplied in
        // the first place then by this point we have already done all the filtering that is possible, and
        // the NoteData under consideration has been 'found'.
        if(allConditionsNull(blnC1, blnC2, blnC3)) return true;

        // Now we have between one and three conditions (based on search keywords) to use in the evaluations below.
        // The possible variations of these three conditions, along with
        //   their possible combinations of AND/OR and in some cases
        //   grouped with parentheses, results in a truth table that has
        //   a total of 48 entries.  Of those, we are only interested in
        //   the 24 that evaluate to false, so that we can return early.
        // The complete truth table is contained in the documentation:
        //   SupportingData.xlsx

        // This first check applies to 6 of the 24 false cases,
        //   regardless of whether or not there are parens.
        if (allConditionsFalse(blnC1, blnC2, blnC3)) return false; // Af  (All false)

        // Now we consider the presence or lack of the parentheses -
        if (getParens() == NOPARENS) {
            // There are 6 remaining cases without parentheses where one false is ANDed with at
            // least one true, so that the overall result will also be false (the case where
            // a false is ANDed with other falses - was already covered by the 'Af' check).
            return !checkFalseCombo(blnC1, blnC2, blnC3); // Nfat (No parens, false ANDed with true)
        } else { // 12 cases left, that can fully evaluate to false.  6 in each parentheses grouping.
            // If we have parens then no one condition should have remained null.  But just in case
            // we got here thru some invisible back door, and to make IJ happy:
            assert blnC1 != null;
            assert blnC2 != null;
            assert blnC3 != null;

            // Note to self:  Beware of the differences between boolean logic, boolean arithmetic, truth
            // tables, and coding logic to implement a truth table; they don't always look like they are
            // tracking together, and reviewing this code you may think you see flaws but I have been over it
            // repeatedly for the past 3+ days working on the logic here and inching the tests past each 'if'
            // block while debugging to be sure that the right lines and conditions are hit.  They are. So
            // regardless of how it may look to you on some later session, before you start to trash it, I
            // highly recommend setting aside a pristene copy, to be kept until you are sure you no longer
            // need it.  That includes 'fixing' the comments.

            boolean p1, p2; // The composite booleans results, depending on location of parentheses.

            if (getParens() == PARENS1) {  // C1 grouped with C2
                if (rbtnOr1.isSelected()) { // This means they are OR'd
                    p1 = blnC1 || blnC2; // C1 OR C2 - evaluates to true if either one or both are true
                    // which means that the collective P1 is AND'd with C3
                    // So if C3 is false, then the P1 result didn't matter.
                    if (!blnC3) return false; // P1taC3f - 3 total cases
                    // But if it was true then we fall thru to here and now P1 does matter -
                    if (!p1) return false;   // P1faC3t - line 39 of the truth table.
                } else { // This means that C1 is AND'd with C2
                    p1 = blnC1 && blnC2; // C1 AND C2 - evaluated to true only if both are true
                    if (!p1 && !blnC3) return false;  // P1foC3f - lines 23,35 two cases
                }
            } // end if PARENS1

            if (getParens() == PARENS2) { // PARENS2 -  C2 grouped with C3
                if (rbtnOr2.isSelected()) { // This means they are OR'd
                    p2 = blnC2 || blnC3; // C2 OR C3 - evaluates to true if either one or both are true
                    // which means that the collective P2 is AND'd with C1
                    // So if C1 false, then the P2 result didn't matter.
                    if (!blnC1) return false;  // C1faP2t - 3 total cases
                    // But if it was true then we fall thru to here and now P2 does matter -
                    return p2;   // C1taP2f - line 24 of the truth table.
                } else { // This means that C2 is AND'd with C3
                    p2 = blnC2 && blnC3; // C2 AND C3 - evaluated to true only if both are true
                    return p2 || blnC1;  // C1foP2f - lines 34,40 two cases
                }
            } // end if PARENS2
        } // end else there are PARENS.
        return true;
    } // end foundIt

    private boolean getAnd1() {
        return rbtnAnd1.isSelected();
    }

    private boolean getAnd2() {
        return rbtnAnd2.isSelected();
    }

    private LocalDate getDateLastMod1() {
        return dateLastMod1;
    }

    private LocalDate getDateLastMod2() {
        return dateLastMod2;
    }

    private LocalDate getnoteWhen1() {
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


    @Override
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
    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }


    private int getParens() {
        if (searchPanelSettings.paren1) return PARENS1;
        if (searchPanelSettings.paren2) return PARENS2;
        return NOPARENS;
    } // end getParens

    // Set the values of our settings from the current states of our components
    // But not covered here: paren1 & paren2
    SearchPanelSettings getSettings() {
        searchPanelSettings.not1 = getNot1();
        searchPanelSettings.not2 = getNot2();
        searchPanelSettings.not3 = getNot3();

        searchPanelSettings.word1 = getWord1();
        searchPanelSettings.word2 = getWord2();
        searchPanelSettings.word3 = getWord3();

        searchPanelSettings.and1 = getAnd1();
        searchPanelSettings.and2 = getAnd2();
        searchPanelSettings.or1 = getOr1();
        searchPanelSettings.or2 = getOr2();

        // Parentheses are set directly into SearchPanelSettings, by resetParentheses()

        searchPanelSettings.typeGoal = chkboxGoals.isSelected();
        searchPanelSettings.typeDay = chkboxDayNotes.isSelected();
        searchPanelSettings.typeMonth = chkboxMonthNotes.isSelected();
        searchPanelSettings.typeYear = chkboxYearNotes.isSelected();
        searchPanelSettings.typeOtherNote = chkboxOtherNotes.isSelected();
        searchPanelSettings.typePastEvent = chkboxPastEvents.isSelected();
        searchPanelSettings.typeFutureEvent = chkboxFutureEvents.isSelected();
        searchPanelSettings.typeTask = chkboxTodoLists.isSelected();

        LocalDate localDate; // re-used several places below.

        searchPanelSettings.whenChoice = getWhenSetting();
        localDate = getnoteWhen1();
        if (localDate == null) {
            searchPanelSettings.noteDateWhen1String = null;
        } else {
            searchPanelSettings.noteDateWhen1String = localDate.toString();
        }
        localDate = getnoteWhen2();
        if (localDate == null) {
            searchPanelSettings.noteDateWhen2String = null;
        } else {
            searchPanelSettings.noteDateWhen2String = localDate.toString();
        }

        searchPanelSettings.modChoice = getLastModSetting();
        localDate = getDateLastMod1();
        if (localDate == null) {
            searchPanelSettings.dateLastMod1String = null;
        } else {
            searchPanelSettings.dateLastMod1String = localDate.toString();
        }
        localDate = getDateLastMod2();
        if (localDate == null) {
            searchPanelSettings.dateLastMod2String = null;
        } else {
            searchPanelSettings.dateLastMod2String = localDate.toString();
        }

        return searchPanelSettings;
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
            case 1:  // Only Word1
                if (sps.not1) strNot = "NOT ";
                else strNot = "";
                s = "Search Text was: " + strNot + sps.word1;
                break;
            case 2:  // Only Word2
                if (sps.not2) strNot = "NOT ";
                else strNot = "";
                s = "Search Text was: " + strNot + sps.word2;
                break;
            case 3:  // Two words: Word 1, Word 2
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
            case 4:  // Only Word3
                if (sps.not3) strNot = "NOT ";
                else strNot = "";
                s = "Search Text was: " + strNot + sps.word3;
                break;
            case 5:  // Two words: Word 1, Word 3
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
            case 6:  // Two words: Word 2, Word 3
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
            case 7: // All 3 words
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


    int getWhenSetting() {
        if (rbtnWhenAfter.isSelected()) return AFTER;
        if (rbtnWhenBefore.isSelected()) return BEFORE;
        if (rbtnWhenBetween.isSelected()) return BETWEEN;
        return -1; // error condition
    } // end getWhenSetting

    private String getWord1() {
        String s = searchText1.getText();
        if (s.trim().isEmpty()) return null;
        return s; // Leading or trailing spaces - ok; we don't trim here.
    } // end getWord1


    private String getWord2() {
        String s = searchText2.getText();
        if (s.trim().isEmpty()) return null;
        return s; // Leading or trailing spaces - ok; we don't trim here.
    } // end getWord2


    private String getWord3() {
        String s = searchText3.getText();
        if (s.trim().isEmpty()) return null;
        return s; // Leading or trailing spaces - ok; we don't trim here.
    } // end getWord3


    //---------------------------------------------------------
    // DocumentListener methods
    //---------------------------------------------------------
    public void insertUpdate(DocumentEvent e) {
        // System.out.println("insertUpdate: " + e.toString());
        resetKeywordPanel();
    } // end insertUpdate

    public void removeUpdate(DocumentEvent e) {
        System.out.println("removeUpdate: " + e.toString());
        resetKeywordPanel();
    } // end removeUpdate


    private void buildPanel() {
        // The 'where to search' panel and its checkboxes
        JPanel pnlWhere = new JPanel();
        chkboxGoals = new JCheckBox("Goals", true);
        chkboxDayNotes = new JCheckBox("Day Notes", true);
        chkboxMonthNotes = new JCheckBox("Month Notes", true);
        chkboxYearNotes = new JCheckBox("Year Notes", true);
        chkboxOtherNotes = new JCheckBox("Other Notes", true);
        chkboxPastEvents = new JCheckBox("Past Events", true);
        chkboxFutureEvents = new JCheckBox("Future Events", true);
        chkboxTodoLists = new JCheckBox("To Do Lists", true);
        //-----
        // Visibility and enablement of these (not their text) is controlled from other contexts.
        lblOpenParen1 = new JLabel("(");
        lblOpenParen2 = new JLabel("(");
        lblCloseParen1 = new JLabel(")");
        lblCloseParen2 = new JLabel(")");
        chkboxNot1 = new JCheckBox("NOT");
        chkboxNot2 = new JCheckBox("NOT");
        chkboxNot3 = new JCheckBox("NOT");
        rbtnAnd1 = new JRadioButton("AND");
        rbtnOr1 = new JRadioButton("OR");
        rbtnAnd2 = new JRadioButton("AND");
        rbtnOr2 = new JRadioButton("OR");

        searchText1 = new JTextField();
        searchText2 = new JTextField();
        searchText3 = new JTextField();

        JPanel keywordPanel = new JPanel(); // Holds 3 'rows'
        JPanel pnlKeyword1 = new JPanel();
        JPanel pnlKeyword2 = new JPanel();
        JPanel pnlKeyword3 = new JPanel();
        //-----
        JPanel pnlWhen = new JPanel();
        JPanel pnlLastMod = new JPanel();
        //-----
        lblWhenSelected = new JLabel();
        JPanel jPanel10 = new JPanel();
        //-----
        rbtnModBefore = new JRadioButton("Before");
        rbtnModBetween = new JRadioButton("Between");
        rbtnModAfter = new JRadioButton("After");
        JPanel jPanel12 = new JPanel();
        //-----
        rbtnWhenBefore = new JRadioButton("Before");
        rbtnWhenBetween = new JRadioButton("Between");
        rbtnWhenAfter = new JRadioButton("After");
        JPanel jPanel15 = new JPanel();
        //-----
        lblModSelected = new JLabel();
        JPanel jPanel17 = new JPanel();
        //-----

        // The base panel for this class -
        setLayout(null);
        addComponent(this, keywordPanel, 9, 10, 421, 135);
        addComponent(this, pnlWhen, 9, 150, 272, 100);
        addComponent(this, pnlLastMod, 9, 257, 272, 102);
        addComponent(this, pnlWhere, 293, 150, 138, 209);

        chkboxGoals.setEnabled(editable);
        chkboxDayNotes.setEnabled(editable);
        chkboxMonthNotes.setEnabled(editable);
        chkboxYearNotes.setEnabled(editable);
        chkboxOtherNotes.setEnabled(editable);
        chkboxPastEvents.setEnabled(editable);
        chkboxFutureEvents.setEnabled(editable);
        chkboxTodoLists.setEnabled(editable);

        // pnlKeywords
        parensOnLabel = new JLabel("   Group keywords when AND+OR.  Same AND/OR again to move the parentheses.");
        parensOffLabel = new JLabel("   Keyword searches are case-insensitive.");
        keywordPanel.setLayout(new BoxLayout(keywordPanel, BoxLayout.Y_AXIS));
        keywordPanel.add(pnlKeyword1, 0);
        keywordPanel.add(pnlKeyword2, 1);
        keywordPanel.add(pnlKeyword3, 2);
        keywordPanel.add(parensOnLabel, 3);
        keywordPanel.add(parensOffLabel, 4);
        keywordPanel.setBorder(new TitledBorder("Title"));

        // pnlWhere
        pnlWhere.setLayout(new BoxLayout(pnlWhere, BoxLayout.Y_AXIS));
        pnlWhere.add(chkboxGoals, 0);
        pnlWhere.add(chkboxDayNotes, 1);
        pnlWhere.add(chkboxMonthNotes, 2);
        pnlWhere.add(chkboxYearNotes, 3);
        pnlWhere.add(chkboxOtherNotes);
        pnlWhere.add(chkboxPastEvents);
        pnlWhere.add(chkboxFutureEvents);
        pnlWhere.add(chkboxTodoLists);
        pnlWhere.setBorder(new TitledBorder("Title"));

        // Panel for the first keyword
        searchText1.setEditable(editable);
        chkboxNot1.setEnabled(editable);
        if(editable) searchText1.getDocument().addDocumentListener(this);
        pnlKeyword1.setLayout(null);
        addComponent(pnlKeyword1, new JLabel("Keyword(s)"), 20, 5, 60, 18);
        addComponent(pnlKeyword1, lblOpenParen1, 111, 6, 15, 18);
        addComponent(pnlKeyword1, chkboxNot1, 120, 5, 47, 23);
        addComponent(pnlKeyword1, searchText1, 170, 6, 210, 21);

        // Panel for the second keyword
        searchText2.setEditable(editable);
        chkboxNot2.setEnabled(editable);
        if(editable) searchText2.getDocument().addDocumentListener(this);
        pnlKeyword2.setLayout(null);
        addComponent(pnlKeyword2, lblOpenParen2, 111, 6, 60, 18);
        addComponent(pnlKeyword2, lblCloseParen1, 389, 5, 60, 18);
        addComponent(pnlKeyword2, rbtnAnd1, 2, 5, 47, 23);
        addComponent(pnlKeyword2, rbtnOr1, 56, 5, 41, 23);
        addComponent(pnlKeyword2, chkboxNot2, 120, 5, 47, 23);
        addComponent(pnlKeyword2, searchText2, 170, 6, 210, 21);

        // Panel for the third keyword
        searchText3.setEditable(editable);
        chkboxNot3.setEnabled(editable);
        if(editable) searchText3.getDocument().addDocumentListener(this);
        pnlKeyword3.setLayout(null);
        addComponent(pnlKeyword3, lblCloseParen2, 389, 5, 60, 18);
        addComponent(pnlKeyword3, rbtnAnd2, 2, 5, 47, 23);
        addComponent(pnlKeyword3, rbtnOr2, 56, 5, 41, 23);
        addComponent(pnlKeyword3, chkboxNot3, 120, 5, 47, 23);
        addComponent(pnlKeyword3, searchText3, 170, 6, 210, 21);
        //
        // pnlWhen
        pnlWhen.setLayout(new BoxLayout(pnlWhen, BoxLayout.X_AXIS));
        pnlWhen.add(jPanel15, 0);
        pnlWhen.add(jPanel10, 1);
        pnlWhen.setBorder(new TitledBorder("Title"));
        //
        // pnlLastMod
        pnlLastMod.setLayout(new BoxLayout(pnlLastMod, BoxLayout.X_AXIS));
        pnlLastMod.add(jPanel12, 0);
        pnlLastMod.add(jPanel17, 1);
        pnlLastMod.setBorder(new TitledBorder("Title"));
        //
        // lblWhenSelected
        lblWhenSelected.setHorizontalAlignment(SwingConstants.CENTER);
//        lblWhenSelected.setText("Selected Date");
        //
        jPanel10.setLayout(null);
        addComponent(jPanel10, lblWhenSelected, 12, 7, 166, 57);
        //
        jPanel12.setLayout(new BoxLayout(jPanel12, BoxLayout.Y_AXIS));
        jPanel12.add(rbtnModBefore, 0);
        jPanel12.add(rbtnModBetween, 1);
        jPanel12.add(rbtnModAfter, 2);
        //
        jPanel15.setLayout(new BoxLayout(jPanel15, BoxLayout.Y_AXIS));
        jPanel15.add(rbtnWhenBefore, 0);
        jPanel15.add(rbtnWhenBetween, 1);
        jPanel15.add(rbtnWhenAfter, 2);
        //
        // lblModSelected
        lblModSelected.setHorizontalAlignment(SwingConstants.CENTER);
//        lblModSelected.setText("Selected Date");
        //
        jPanel17.setLayout(null);
        addComponent(jPanel17, lblModSelected, 12, 7, 166, 57);
        //
        // SearchPanel
        this.setLocation(new Point(16, 0));
        this.setSize(new Dimension(443, 357));

        // Below here was originally rebuildPanel()
        //===================================================================================================
        // Fill in the border titles -
        //-----------------------------------------------
        TitledBorder tb; // Using the same reference for all borders.

        tb = (TitledBorder) pnlWhere.getBorder();
        tb.setTitle("Where to search");
        tb.setTitleFont(Font.decode("Dialog-bold-14"));

        tb = (TitledBorder) keywordPanel.getBorder();
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
        andOr1 = new ButtonGroup();
        andOr1.add(rbtnAnd1);
        andOr1.add(rbtnOr1);
        rbtnAnd1.setEnabled(editable);
        rbtnOr1.setEnabled(editable);

        andOr2 = new ButtonGroup();
        andOr2.add(rbtnAnd2);
        andOr2.add(rbtnOr2);
        rbtnAnd2.setEnabled(editable);
        rbtnOr2.setEnabled(editable);

        bgWhen = new ButtonGroup();
        bgWhen.add(rbtnWhenBefore);
        bgWhen.add(rbtnWhenBetween);
        bgWhen.add(rbtnWhenAfter);
        rbtnWhenBefore.setEnabled(editable);
        rbtnWhenBetween.setEnabled(editable);
        rbtnWhenAfter.setEnabled(editable);

        bgMod = new ButtonGroup();
        bgMod.add(rbtnModBefore);
        bgMod.add(rbtnModBetween);
        bgMod.add(rbtnModAfter);
        rbtnModBefore.setEnabled(editable);
        rbtnModBetween.setEnabled(editable);
        rbtnModAfter.setEnabled(editable);
        //-----------------------------------------------

        // Increase the size and boldness of the parens
        lblOpenParen1.setFont(Font.decode("Dialog-bold-14"));
        lblOpenParen2.setFont(Font.decode("Dialog-bold-14"));
        lblCloseParen1.setFont(Font.decode("Dialog-bold-14"));
        lblCloseParen2.setFont(Font.decode("Dialog-bold-14"));

        // Define and assign a handler for parentheses positioning.
        // This also implements a 'toggle' functionality.
        //--------------------------------------------------------
        MouseAdapter ma1 = new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                getSettings(); // Update all SearchPanelSettings data based on curent state of Panel controls.

                if(searchPanelSettings.paren1) {
                    searchPanelSettings.paren1 = false;
                    searchPanelSettings.paren2 = true;
                } else if(searchPanelSettings.paren2) {
                    searchPanelSettings.paren2 = false;
                    searchPanelSettings.paren1 = true;
                } else {
                    // Initial showing of parens.  We always start with paren1.
                    if(rbtnAnd1.isSelected() && rbtnOr2.isSelected()) searchPanelSettings.paren1 = true;
                    if(rbtnAnd2.isSelected() && rbtnOr1.isSelected()) searchPanelSettings.paren1 = true;
                }
                resetParentheses();
            }
        }; // end redefined MouseAdapter

        if(editable) {
            rbtnAnd1.addMouseListener(ma1);
            rbtnAnd2.addMouseListener(ma1);
            rbtnOr1.addMouseListener(ma1);
            rbtnOr2.addMouseListener(ma1);
        }
        //--------------------------------------------------------

        // Date Initialization
        noteWhen1 = null;
        noteWhen2 = null;
        dateLastMod1 = null;
        dateLastMod2 = null;
        resetDateDisplays(); // Set the initial date prompts

        // Define and assign a handler for date specifications
        //--------------------------------------------------------
        MouseAdapter ma2 = new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                boolean rightClick = me.getButton() == MouseEvent.BUTTON3;
                //int m = me.getModifiersEx();
                //if ((m & InputEvent.BUTTON3_DOWN_MASK) != 0) rightClick = true;

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

        if(editable) {
            rbtnWhenBefore.addMouseListener(ma2);
            rbtnWhenBetween.addMouseListener(ma2);
            rbtnWhenAfter.addMouseListener(ma2);
            rbtnModBefore.addMouseListener(ma2);
            rbtnModBetween.addMouseListener(ma2);
            rbtnModAfter.addMouseListener(ma2);
        }

    } // end buildPanel


    public void changedUpdate(DocumentEvent e) {
        System.out.println("changedUpdate: " + e.toString());
        resetKeywordPanel();
    } // end changedUpdate


    // Called when an AND or an OR radio button is clicked.
    // Set the visibility and text of components based on the current SearchPanelSettings.
    private void resetParentheses() {
        boolean blnNeedParens = false;

        lblOpenParen1.setVisible(false);
        lblOpenParen2.setVisible(false);
        lblCloseParen1.setVisible(false);
        lblCloseParen2.setVisible(false);
        parensOnLabel.setVisible(false);
        parensOffLabel.setVisible(true);

        rbtnAnd1.setToolTipText(null);
        rbtnAnd2.setToolTipText(null);
        rbtnOr1.setToolTipText(null);
        rbtnOr2.setToolTipText(null);

        String s = "Click again to move parentheses";
        if(searchPanelSettings.and1 && searchPanelSettings.or2) {
            blnNeedParens = true;
            rbtnAnd1.setToolTipText(s);
            rbtnOr2.setToolTipText(s);
        } // end if

        if(searchPanelSettings.or1 && searchPanelSettings.and2) {
            blnNeedParens = true;
            rbtnOr1.setToolTipText(s);
            rbtnAnd2.setToolTipText(s);
        } // end if

        if (blnNeedParens) {
            if(editable) {
                // We only want to give the paren-move help text when the panel is editable.
                parensOnLabel.setVisible(true);
                parensOffLabel.setVisible(false);
            }
            if (searchPanelSettings.paren2) {
                lblOpenParen2.setVisible(true);
                lblCloseParen2.setVisible(true);
            } else {
                lblOpenParen1.setVisible(true);
                lblCloseParen1.setVisible(true);
            }
        } // end if

    } // end resetParentheses

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

        resetDateDisplays();
    } // end handleDateSpecChanged


    //----------------------------------------------------------
    // Method Name: hasWhere
    //
    // Returns true if the user has specified at least
    //   one place to search; otherwise false.
    //----------------------------------------------------------
    boolean hasWhere() {
        boolean retVal = chkboxGoals.isSelected();
        if (chkboxDayNotes.isSelected()) retVal = true;
        if (chkboxMonthNotes.isSelected()) retVal = true;
        if (chkboxYearNotes.isSelected()) retVal = true;
        if (chkboxOtherNotes.isSelected()) retVal = true;
        if (chkboxPastEvents.isSelected()) retVal = true;
        if (chkboxFutureEvents.isSelected()) retVal = true;
        if (chkboxTodoLists.isSelected()) retVal = true;
        return retVal;
    } // end hasWhere


    // Use the searchPanelSettings to configure the panel's controls.  This is for
    //   search criteria review only, not to be user-altered or used in a new search.
    void loadTheSettings() {
        // Keyword Panel
        chkboxNot1.setSelected(searchPanelSettings.not1);
        searchText1.setText(searchPanelSettings.word1);
        rbtnAnd1.setSelected(searchPanelSettings.and1);
        rbtnOr1.setSelected(searchPanelSettings.or1);
        chkboxNot2.setSelected(searchPanelSettings.not2);
        searchText2.setText(searchPanelSettings.word2);
        rbtnAnd2.setSelected(searchPanelSettings.and2);
        rbtnOr2.setSelected(searchPanelSettings.or2);
        chkboxNot3.setSelected(searchPanelSettings.not3);
        searchText3.setText(searchPanelSettings.word3);
        resetParentheses();
//        if(searchPanelSettings.paren1) resetParentheses(false);
//        if(searchPanelSettings.paren2) resetParentheses(true);

        // These need to be done AFTER the text fields have been set, to avoid the Document listener.
        // We re-enable these for the coloration, only.  No mouse listeners.
        if(searchPanelSettings.and1) rbtnAnd1.setEnabled(true);
        if(searchPanelSettings.or1) rbtnOr1.setEnabled(true);
        if(searchPanelSettings.and2) rbtnAnd2.setEnabled(true);
        if(searchPanelSettings.or2) rbtnOr2.setEnabled(true);

        // Group Type Panel - pnlWhere
        chkboxGoals.setSelected(searchPanelSettings.typeGoal);
        chkboxDayNotes.setSelected(searchPanelSettings.typeDay);
        chkboxMonthNotes.setSelected(searchPanelSettings.typeMonth);
        chkboxYearNotes.setSelected(searchPanelSettings.typeYear);
        chkboxOtherNotes.setSelected(searchPanelSettings.typeOtherNote);
        chkboxPastEvents.setSelected(searchPanelSettings.typePastEvent);
        chkboxFutureEvents.setSelected(searchPanelSettings.typeFutureEvent);
        chkboxTodoLists.setSelected(searchPanelSettings.typeTask);

        // Note Date Panel - pnlWhen (not all note types have a date)
        rbtnWhenBefore.setSelected(false);
        rbtnWhenBetween.setSelected(false);
        rbtnWhenAfter.setSelected(false);
        switch(searchPanelSettings.whenChoice) {
            case BEFORE:
                rbtnWhenBefore.setSelected(true);
                break;
            case BETWEEN:
                rbtnWhenBetween.setSelected(true);
                break;
            case AFTER:
                rbtnWhenAfter.setSelected(true);
                break;
        }

        // Note Last Modified Panel - pnlLastMod
        rbtnModBefore.setSelected(false);
        rbtnModBetween.setSelected(false);
        rbtnModAfter.setSelected(false);
        switch(searchPanelSettings.modChoice) {
            case BEFORE:
                rbtnModBefore.setSelected(true);
                break;
            case BETWEEN:
                rbtnModBetween.setSelected(true);
                break;
            case AFTER:
                rbtnModAfter.setSelected(true);
                break;
        }

        // Format and show the Dates
        if(searchPanelSettings.noteDateWhen1String != null)
            noteWhen1 = LocalDate.parse(searchPanelSettings.noteDateWhen1String);
        if(searchPanelSettings.noteDateWhen2String != null)
            noteWhen2 = LocalDate.parse(searchPanelSettings.noteDateWhen2String);
        if(searchPanelSettings.dateLastMod1String != null)
            dateLastMod1 = LocalDate.parse(searchPanelSettings.dateLastMod1String);
        if(searchPanelSettings.dateLastMod2String != null)
            dateLastMod2 = LocalDate.parse(searchPanelSettings.dateLastMod2String);
        resetDateDisplays();
    }

    private void resetDateDisplays() {
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
    } // end resetDateDisplays


    // Enable/disable keyword checkboxes and and/or buttons based on presence of keywords.
    // This is called with every entry into any keyword field, every time.  (from insertUpdate)
    // Some parts of the keyword panel have variable visibility.  Others can be
    // enabled or disabled, depending on combinations of the states of other controls.  These
    // settings and combinations are examined here, and settings made accordingly.
    // The 'editable' flag is not considered, since this method is not called when the panel is non-editable.
    void resetKeywordPanel() {
        // Initialize all And/Or controls to disabled -
        rbtnAnd1.setEnabled(false);
        rbtnOr1.setEnabled(false);
        rbtnAnd2.setEnabled(false);
        rbtnOr2.setEnabled(false);

        if (getWord1() != null) {
            chkboxNot1.setEnabled(true);
            if(getWord2() != null) {
                rbtnAnd1.setEnabled(true);
                rbtnOr1.setEnabled(true);
            }
        } else {
            andOr1.clearSelection();
            chkboxNot1.setSelected(false);
            chkboxNot1.setEnabled(false);
            searchPanelSettings.paren1 = false;
            searchPanelSettings.paren2 = false;
            searchPanelSettings.and1 = false;
            searchPanelSettings.or1 = false;
            if(getWord2() == null) {
                // We already didn't have a word 1; now if there is also
                //  no word 2 then the and/or for word 3 can be cleared.
                andOr2.clearSelection();
                searchPanelSettings.and2 = false;
                searchPanelSettings.or2 = false;
            }
        }

        if (getWord2() != null) {
            chkboxNot2.setEnabled(true);
            if (getWord1() != null) {
                rbtnAnd1.setEnabled(true);
                rbtnOr1.setEnabled(true);

                // We might have arrived here with .and1 being true, if words 1&2 were not null on a recent previous keystroke.
                if(searchPanelSettings.and1) {
                    rbtnAnd1.setSelected(true); // The 'and's are never the default.
                }
                else {
                    rbtnOr1.setSelected(true);
                    searchPanelSettings.or1 = true; // Either it already was, or we need to make it so now, as the default.
                }
            }
        } else {
            andOr1.clearSelection();
            chkboxNot2.setSelected(false);
            chkboxNot2.setEnabled(false);
            searchPanelSettings.paren1 = false;
            searchPanelSettings.paren2 = false;
            searchPanelSettings.and1 = false;
            searchPanelSettings.or1 = false;
        }

        if (getWord3() != null) {
            chkboxNot3.setEnabled(true);
            if (getWord1() != null || getWord2() != null) {
                rbtnAnd2.setEnabled(true);
                rbtnOr2.setEnabled(true);

                // We might have arrived here with .and2 being true, if word 3 was not null on a recent previous keystroke.
                if(searchPanelSettings.and2) {
                    rbtnAnd2.setSelected(true); // The 'and's are never the default.
                }
                else {
                    rbtnOr2.setSelected(true);
                    searchPanelSettings.or2 = true; // Either it already was, or we need to make it so now, as the default.
                }
            }
        } else {
            andOr2.clearSelection();
            chkboxNot3.setSelected(false);
            chkboxNot3.setEnabled(false);
            searchPanelSettings.paren1 = false;
            searchPanelSettings.paren2 = false;
            searchPanelSettings.and2 = false;
            searchPanelSettings.or2 = false;
        }

        if(getWord1() == null || getWord2() == null || getWord3() == null) {
            searchPanelSettings.paren1 = false;
            searchPanelSettings.paren2 = false;
        }
        resetParentheses();
    } // end resetKeywordPanel

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
        return chkboxFutureEvents.isSelected();
    }

    boolean searchLists() {
        return chkboxTodoLists.isSelected();
    }

    // Used only by tests.
    // Refactor them to use the new constructor and 'loadTheSettings'.
    void setTheSettings(SearchPanelSettings theSettings) {
        // All SearchPanelSettings default to null, which is NOT the same defaults
        // in every setting on the SearchPanel.

        if (theSettings.word1 != null) searchText1.setText(theSettings.word1);
        if (theSettings.word2 != null) searchText2.setText(theSettings.word2);
        if (theSettings.word3 != null) searchText3.setText(theSettings.word3);
        rbtnAnd1.setSelected(theSettings.and1);
        rbtnOr1.setSelected(theSettings.or1);
        rbtnAnd2.setSelected(theSettings.and2);
        rbtnOr2.setSelected(theSettings.or2);
        lblOpenParen1.setVisible(theSettings.paren1); // 'closed' is in sync with 'open' -
        lblOpenParen2.setVisible(theSettings.paren2); // we're depending on it.
    }

    void showDateDialog(String s, int numSelections) {
        // Make a dialog window to choose a date from a Year.
        Frame f = JOptionPane.getFrameForComponent(this);
        dialogWindow = new JDialog(f, true);
        blnDialogClosed = false;

        dialogWindow.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                //System.out.println("Window closing event: " + we.toString());
                blnDialogClosed = true;
            }
        });

        dialogWindow.getContentPane().add(yvDateChooser, BorderLayout.CENTER);
        dialogWindow.setTitle(s);
        dialogWindow.setSize(yvDateChooser.getPreferredSize());
        dialogWindow.setResizable(false);
        yvDateChooser.setDialog(dialogWindow, numSelections);

        // Center the dialog relative to the main frame.
        dialogWindow.setLocationRelativeTo(f);

        // Go modal -
        dialogWindow.setVisible(true);
    } // end showDateDialog


} // end class SearchPanel


