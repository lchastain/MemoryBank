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

    private int intPreferredWidth;    // See note in constructor.
    private int intPreferredHeight;   // See note in constructor.
    private DateTimeFormatter dtf;
    private LocalDate noteWhen1;
    private LocalDate noteWhen2;
    private LocalDate dateLastMod1;
    private LocalDate dateLastMod2;
    private ButtonGroup bgWhen;
    private ButtonGroup bgMod;
    YearView yvDateChooser;
    private boolean blnDialogClosed; // For use by inner classes
    JDialog dialogWindow;
    boolean doSearch;
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
    private JPanel keywordPanel;
    private JLabel lblOpenParen1;
    private JCheckBox chkboxNot1;
    private JTextField comboxSearchText1;
    //-----
    private JLabel lblOpenParen2;
    private JLabel lblCloseParen1;
    private JRadioButton rbtnAnd1;
    private JRadioButton rbtnOr1;
    private JCheckBox chkboxNot2;
    private JTextField comboxSearchText2;
    //-----
    private JLabel lblCloseParen2;
    private JRadioButton rbtnAnd2;
    private JRadioButton rbtnOr2;
    private JCheckBox chkboxNot3;
    private JTextField comboxSearchText3;
    private JLabel parensOnLabel;
    private JLabel parensOffLabel;
    //-----
    private JPanel pnlWhen;
    //-----
    private JPanel pnlLastMod;
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
        super();

        doSearch = false; // A flag that can be used by tests.

        // Some parts of the keyword panel have variable visibility.  Others can be
        // enabled or disabled, depending on other factors.  These variables are
        // examined in resetKeywordPanel, and settings made accordingly.
        typingListener = new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                resetKeywordPanel();
            }
        };

        initializeComponent(); // Build (most of) the panel

        // Although size was set explicitly in initializeComponent, each call to getSize()
        //   reported a smaller amount by (6,25) after each time the dialog was closed.
        //   This method of capturing the initial size is the workaround.  Now the
        //   EventNoteGroup calls getMinimumSize (defined below) to get the size value.
        intPreferredWidth = getSize().width;
        intPreferredHeight = getSize().height;
        blnDialogClosed = false;

        reinitializeComponent(); // Finishing touches on the panel construction
        resetKeywordPanel();
    } // end constructor


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
        //   then the return value will be false.

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

        LocalDate localDate; // re-used several places below.

        sps.whenChoice = getWhenSetting();
        localDate = getnoteWhen1();
        if (localDate == null) {
            sps.noteDateWhen1String = null;
        } else {
            sps.noteDateWhen1String = localDate.toString();
        }
        localDate = getnoteWhen2();
        if (localDate == null) {
            sps.noteDateWhen2String = null;
        } else {
            sps.noteDateWhen2String = localDate.toString();
        }

        sps.modChoice = getLastModSetting();
        localDate = getDateLastMod1();
        if (localDate == null) {
            sps.dateLastMod1String = null;
        } else {
            sps.dateLastMod1String = localDate.toString();
        }
        localDate = getDateLastMod2();
        if (localDate == null) {
            sps.dateLastMod2String = null;
        } else {
            sps.dateLastMod2String = localDate.toString();
        }

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
        String s = comboxSearchText1.getText();
        if (s.trim().isEmpty()) return null;
        return s;
    } // end getWord1


    private String getWord2() {
        String s = comboxSearchText2.getText();
        if (s.trim().isEmpty()) return null;
        return s;
    } // end getWord2


    private String getWord3() {
        String s = comboxSearchText3.getText();
        if (s.trim().isEmpty()) return null;
        return s;
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

    public void changedUpdate(DocumentEvent e) {
        System.out.println("changedUpdate: " + e.toString());
        resetKeywordPanel();
    } // end changedUpdate


    // Called when an AND or an OR radio button was clicked.
    private void resetParentheses(boolean moveEm) {
        boolean toggle = false;
        boolean blnNeedParens = false;
        if (moveEm) {
            toggle = lblOpenParen1.isVisible();
        }

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

        if (!rbtnAnd1.isEnabled()) blnNeedParens = false;
        if (!rbtnAnd2.isEnabled()) blnNeedParens = false;

        if (blnNeedParens) {
            parensOnLabel.setVisible(true);
            parensOffLabel.setVisible(false);
            if (toggle) {
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
        if (chkboxDayNotes.isSelected()) retVal = true;
        if (chkboxMonthNotes.isSelected()) retVal = true;
        if (chkboxYearNotes.isSelected()) retVal = true;
        if (chkboxEvents.isSelected()) retVal = true;
        if (chkboxTodoLists.isSelected()) retVal = true;
        return retVal;
    } // end hasWhere


    private void initializeComponent() {

        JPanel contentPane = SearchPanel.this;
        //----- 
        chkboxGoals = new JCheckBox();
        chkboxDayNotes = new JCheckBox();
        chkboxMonthNotes = new JCheckBox();
        chkboxYearNotes = new JCheckBox();
        chkboxEvents = new JCheckBox();
        chkboxTodoLists = new JCheckBox();
        pnlWhere = new JPanel();
        //----- 
        keywordPanel = new JPanel();
        //----- 
        JLabel jLabel3 = new JLabel();
        lblOpenParen1 = new JLabel();
        chkboxNot1 = new JCheckBox();
        comboxSearchText1 = new JTextField();
        JPanel pnlKeyword1 = new JPanel();
        //----- 
        lblOpenParen2 = new JLabel();
        lblCloseParen1 = new JLabel();
        rbtnAnd1 = new JRadioButton();
        rbtnOr1 = new JRadioButton();
        chkboxNot2 = new JCheckBox();
        comboxSearchText2 = new JTextField();
        JPanel pnlKeyword2 = new JPanel();
        //----- 
        lblCloseParen2 = new JLabel();
        rbtnAnd2 = new JRadioButton();
        rbtnOr2 = new JRadioButton();
        chkboxNot3 = new JCheckBox();
        comboxSearchText3 = new JTextField();
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
        addComponent(contentPane, keywordPanel, 9, 10, 421, 135);
        addComponent(contentPane, pnlWhen, 9, 150, 272, 100);
        addComponent(contentPane, pnlLastMod, 9, 257, 272, 100);
        addComponent(contentPane, pnlWhere, 293, 150, 138, 207);
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
        // pnlKeywords
        //
        parensOnLabel = new JLabel("   Group keywords when AND+OR.  Same AND/OR again to move the parentheses.");
        parensOffLabel = new JLabel("   Keyword searches are case-insensitive.");
        keywordPanel.setLayout(new BoxLayout(keywordPanel, BoxLayout.Y_AXIS));
        keywordPanel.add(pnlKeyword1, 0);
        keywordPanel.add(pnlKeyword2, 1);
        keywordPanel.add(pnlKeyword3, 2);
        keywordPanel.add(parensOnLabel, 3);
        keywordPanel.add(parensOffLabel, 4);
        keywordPanel.setBorder(new TitledBorder("Title"));
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
        comboxSearchText1.getDocument().addDocumentListener(this);

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
        comboxSearchText2.getDocument().addDocumentListener(this);

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
        comboxSearchText3.getDocument().addDocumentListener(this);

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
        this.setLocation(new Point(16, 0));
        this.setSize(new Dimension(443, 357));
    } // end initializeComponent


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

        // Increase the size and boldness of the parens
        lblOpenParen1.setFont(Font.decode("Dialog-bold-14"));
        lblOpenParen2.setFont(Font.decode("Dialog-bold-14"));
        lblCloseParen1.setFont(Font.decode("Dialog-bold-14"));
        lblCloseParen2.setFont(Font.decode("Dialog-bold-14"));

        // Define and assign a handler for parentheses visibility
        //--------------------------------------------------------
        MouseAdapter ma1 = new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                resetParentheses(true);
            }
        }; // end redefined MouseAdapter

        rbtnAnd1.addMouseListener(ma1);
        rbtnAnd2.addMouseListener(ma1);
        rbtnOr1.addMouseListener(ma1);
        rbtnOr2.addMouseListener(ma1);
        //--------------------------------------------------------
        resetParentheses(false); // Initialize parens to all non-visible

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

    void resetKeywordPanel() {
        // Set the defaults
        chkboxNot1.setSelected(false);
        chkboxNot1.setEnabled(false);
        rbtnAnd1.setEnabled(false);
        rbtnOr1.setEnabled(false);
        chkboxNot2.setSelected(false);
        chkboxNot2.setEnabled(false);
        rbtnAnd2.setEnabled(false);
        rbtnOr2.setEnabled(false);
        chkboxNot2.setSelected(false);
        chkboxNot3.setEnabled(false);

        if (getWord1() != null) {
            chkboxNot1.setEnabled(true);
        }
        if (getWord2() != null) {
            chkboxNot2.setEnabled(true);
            if (getWord1() != null) {
                rbtnAnd1.setEnabled(true);
                rbtnOr1.setEnabled(true);
            }
        }
        if (getWord3() != null) {
            chkboxNot3.setEnabled(true);
            if (getWord1() != null || getWord2() != null) {
                rbtnAnd2.setEnabled(true);
                rbtnOr2.setEnabled(true);
            }
        }
        resetParentheses(false);
    }

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

    // Used only by tests (for now).
    // Future dev may support a 'recall' of a previously conducted search.
    void setTheSettings(SearchPanelSettings theSettings) {
        // All SearchPanelSettings default to null, which is NOT the same defaults
        // in every setting on the SearchPanel.

        // The SearchPanel keeps NO local variables; all values are held in the components.
        // This just is; it wasn't by design and now - don't know if it was a good idea or not.
        // It seems kind of cool but then it makes getting/setting a lot more complicated.
        // And it definitely does not keep data separate of representation which goes against
        // industry standard practices, so now that I've written it down it's looking more like
        // it probably was NOT a good idea.  Oh well - it works and I'll keep it this way for now.
        if (theSettings.word1 != null) comboxSearchText1.setText(theSettings.word1);
        if (theSettings.word2 != null) comboxSearchText2.setText(theSettings.word2);
        if (theSettings.word3 != null) comboxSearchText3.setText(theSettings.word3);
        rbtnAnd1.setSelected(theSettings.and1);
        rbtnOr1.setSelected(theSettings.or1);
        rbtnAnd2.setSelected(theSettings.and2);
        rbtnOr2.setSelected(theSettings.or2);
        lblOpenParen1.setVisible(theSettings.paren1); // Closed tracks along with Open
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


