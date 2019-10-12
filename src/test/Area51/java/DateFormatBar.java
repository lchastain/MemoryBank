//
// This class is a User Interface for setting the format of
//   a date.  To use, instantiate once and then call setup to
//   initialize prior to each display of the UI. 
//
//
import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.Vector;

public class DateFormatBar extends Container implements ClingSource {
    private static final long serialVersionUID = 1L;

    private HeaderButton hb1;   // Day
    private HeaderButton hb3;   // Month
    private HeaderButton hb4;   // Date
    private HeaderButton hb5;   // Time
    private HeaderButton hb6;   // Year
    private HeaderButton hb7;   // Era

    private FieldLabel fb1;    // Day
    private FieldLabel fb3;    // Month
    private FieldLabel fb4;    // Date
    private FieldLabel fb5;    // Time
    private FieldLabel fb6;    // Year
    private FieldLabel fb7;    // Era

    private String initialFormat;

    private ZonedDateTime theDate;
    private JLabel theDateLabel;
    private JPanel header;
    private JPanel fields;
    private TimeFormatPanel tfp;

    private DfbHeaderPopup pop1;
    private DfbHeaderPopup pop3;
    private DfbHeaderPopup pop4;
    private DfbHeaderPopup pop5;
    private DfbHeaderPopup pop6;
    private DfbHeaderPopup pop7;
    private popHandler al;

    // Known maximum widths needed for columns
    private static final int c1width = 73; // Day
    private static final int c3width = 67; // Month
    private static final int c4width = 35; // Date
    private static final int c5width = 180; // Time
    private static final int c6width = 37; // Year
    private static final int c7width = 28; // Era

    private boolean beenMoved;

    DateFormatBar() {
        super();

        al = new popHandler();

        pop1 = new DfbHeaderPopup(new String[]{
                "Day",
                "3 letters (Ddd)",
                "Full Day Name"});

        pop3 = new DfbHeaderPopup(new String[]{
                "Month",
                "1-12",
                "01-12",
                "3 letters (Mmm)",
                "Full Month Name"});

        pop4 = new DfbHeaderPopup(new String[]{
                "Date",
                "1-31",
                "01-31"});

        pop5 = new DfbHeaderPopup(new String[]{"Time"});
        // Note: pop5 is now not used except as a necessary
        //   parameter to the 'Time' HeaderButton.

        pop6 = new DfbHeaderPopup(new String[]{
                "Year",
                "2 digits",
                "4 digits"});

        pop7 = new DfbHeaderPopup(new String[]{"Era"});
        pop7.remove(2); // an extra separator


        setLayout(new GridLayout(0, 1, 0, 0));

        al.setDfb(this);

        beenMoved = false;
        initialFormat = "";

        DndLayout headerLayout = new DndLayout();
        headerLayout.setMoveable(true);
        headerLayout.setClingSource(this);
        header = new JPanel(headerLayout) {
            private static final long serialVersionUID = 6355422801441353411L;

            public void doLayout() {
                super.doLayout();
                if (beenMoved) {
                    resetDateLabel();
                    beenMoved = false;
                } // end if
            } // end doLayout
        };//end header def

        DndLayout fieldLayout = new DndLayout();
        fields = new JPanel(fieldLayout);

        // Column Header Buttons with Menus -
        hb1 = new HeaderButton(pop1);
        hb3 = new HeaderButton(pop3);
        hb4 = new HeaderButton(pop4);
        hb5 = new HeaderButton(pop5); // Time
        hb6 = new HeaderButton(pop6);
        hb7 = new HeaderButton(pop7);

        hb5.setSeparator(""); // Otherwise defaults to a space.
//    hb5.removeMouseListener(hb5);
        hb5.addMouseListener(new TimeFormatListener());

        // The numeric strings below -
        //  do not seem to matter or to be used.
        // put them out of order to try to see the effect.
        header.add(hb1, "3");
        header.add(hb3, "3");
        header.add(hb4, "3");
        header.add(hb5, "1");
        header.add(hb6, "1");
        header.add(hb7, "7");
        add(header);

        fb1 = new FieldLabel(" ");
        fb3 = new FieldLabel(" ");
        fb4 = new FieldLabel(" ");
        fb5 = new FieldLabel(" ");
        fb6 = new FieldLabel(" ");
        fb7 = new FieldLabel(" ");

        fields.add(fb1, "1");
        fields.add(fb3, "3");
        fields.add(fb4, "4");
        fields.add(fb5, "5");
        fields.add(fb6, "6");
        fields.add(fb7, "7");
        add(fields);

        add(new JLabel("   ")); // spacer

        theDateLabel = new JLabel(" ", JLabel.CENTER);
        theDateLabel.setFont(theDateLabel.getFont().deriveFont(Font.BOLD));
        add(theDateLabel);
        tfp = new TimeFormatPanel();
    } // end constructor

    // There are seven possible columnns, in any order.
    //   This function returns a String that describes
    //   the order and visibility of columns in the header.
    private String getColumnOrder() {
        HeaderButton hb;
        StringBuilder returnString = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            hb = (HeaderButton) header.getComponent(i);
            if (!hb.isVisible()) continue;
            if (hb.getText().equals(hb1.getText())) returnString.append("1");
            if (hb.getText().equals(hb3.getText())) returnString.append("3");
            if (hb.getText().equals(hb4.getText())) returnString.append("4");
            if (hb.getText().equals(hb5.getText())) returnString.append("5");
            if (hb.getText().equals(hb6.getText())) returnString.append("6");
            if (hb.getText().equals(hb7.getText())) returnString.append("7");
        } // end for i
        if (returnString.toString().equals("")) returnString = new StringBuilder("0");

        return returnString.toString();
    } // end getColumnOrder

    long getDate() {
        // System.out.println("theDate = " + theDate);
        return theDate.toInstant().getEpochSecond();
    } // end getDate

/*
  public String getDateString(long theDate, String theFormat) {
    Date d = new Date(theDate);
    // System.out.println("DateFormatBar - date, format " + d + " " + theFormat);
    String s = getRealFormat(theFormat);
    Memory Bank.sdf.applyPattern(s);
    return Memory Bank.sdf.format(d);
  } // end getDateString

  // Here is where the format is interpreted.
  public String getRealFormat(String theFormat) {
    // System.out.println("Format parsing: [" + theFormat + "]");
    if(theFormat.equals("")) return "";  // never been set
    if(theFormat.equals("0")) return ""; // explicitly set to ""
    initialFormat = theFormat;

    String s = "";
    int which;
    String theField;

    int end = theFormat.indexOf("|");
    if(end == -1) return "";
    String order = theFormat.substring(0, end);

    for(int i=0; i<order.length(); i++) {
      which = Integer.parseInt(order.substring(i, i+1));
      theField = Memory Bank.getFieldFromFormat(which, initialFormat);
      if(which == 5) {
        s += TimeFormatBar.getRealFormat(theField);
      } else {
        s += theField;
        s += getSeparatorFromFormat(which, false);
      } // end if
    } // end for i

    s = Memory Bank.convert(s, "#SQUOTE#", "''");
    s = Memory Bank.convert(s, "#DQUOTE#", "\"");
    s = Memory Bank.convert(s, "#VBAR#", "|");
    // System.out.println("The REAL format specifier is: " + s);
    return s;
  } // end getRealFormat
*/

    private String getDefault() {
        return "1346|EEEE', '|MMMM' '|d' '|yyyy"; // 1346
    } // end getDefault

    // Given the known configuration of the initialFormat string,
    //   parse out and return the requested element.
    private String getSeparatorFromFormat(int i) {
        String s = getOrderFromFormat();  // String may contain "134567"
        int numFields = s.length();
        String theField = "";
        int pos = s.indexOf(String.valueOf(i));
        if (pos == -1) return " "; // this field not in the format.

        // Get past the order-specifying prefix of the format string
        String suffix = initialFormat.substring(s.length() + 1);
        // System.out.println("suffix: " + suffix);

        for (int j = 0; j < numFields; j++) {
            if (j == numFields - 1) theField = suffix;  // last field
            else theField = suffix.substring(0, suffix.indexOf("|"));
            if (j == pos) break;
            suffix = suffix.substring(theField.length() + 1);
        } // end for j

        // Got the Field; now process the separator string, if any.
        int sspos = theField.indexOf("'");
        if (sspos == -1) return "";

        String theSeparator = theField.substring(sspos);

        // Trim of the single quotes.
        theSeparator = theSeparator.substring(1);
        theSeparator = theSeparator.substring(0, theSeparator.length() - 1);
        return theSeparator;
    } // end getSeparatorFromFormat

    // Construct a format string based on the current configuration
    //   of the interface.
    String getFormat() {
        StringBuilder s;
        String order = getColumnOrder();
        s = new StringBuilder(order);
        if (order.equals("0")) return s.toString();
        int numcols = order.length();
        for (int i = 0; i < numcols; i++) {
            if (order.substring(i, i + 1).equals("1")) {
                s.append("|").append(fb1.getFormat());
                if (hb1.separatorString.length() != 0)
                    s.append("'").append(hb1.separatorString).append("'");
            } // end if

            if (order.substring(i, i + 1).equals("3")) {
                s.append("|").append(fb3.getFormat());
                if (hb3.separatorString.length() != 0)
                    s.append("'").append(hb3.separatorString).append("'");
            } // end if

            if (order.substring(i, i + 1).equals("4")) {
                s.append("|").append(fb4.getFormat());
                if (hb4.separatorString.length() != 0)
                    s.append("'").append(hb4.separatorString).append("'");
            } // end if

            // Time is a special (embedded) case.
            if (order.substring(i, i + 1).equals("5")) {
                s.append("|").append(tfp.getFormat());
            } // end if

            if (order.substring(i, i + 1).equals("6")) {
                s.append("|").append(fb6.getFormat());
                if (hb6.separatorString.length() != 0)
                    s.append("'").append(hb6.separatorString).append("'");
            } // end if

            if (order.substring(i, i + 1).equals("7")) {
                s.append("|").append(fb7.getFormat());
                if (hb7.separatorString.length() != 0)
                    s.append("'").append(hb7.separatorString).append("'");
            } // end if

        } // end for i

        // System.out.println("The INTERNAL format specifier is: " + s);
        initialFormat = s.toString();
        return s.toString();
    } // end getFormat

    private String getOrderFromFormat() {
        int end = initialFormat.indexOf("|");
        if (end == -1) return "";

        return initialFormat.substring(0, end);
    } // end getOrderFromFormat

    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width = 0;
        d.width += hb1.getPreferredSize().width;
        d.width += hb3.getPreferredSize().width;
        d.width += hb4.getPreferredSize().width;
        d.width += hb5.getPreferredSize().width;
        d.width += hb6.getPreferredSize().width;
        d.width += hb7.getPreferredSize().width;
        return d;
    } // end getPreferredSize

    int getVisibilityFromFormat() {
        int visibility = 0;

        String posString = getOrderFromFormat();
        if (posString.equals("")) return visibility; // visibility remains 0
        if (posString.contains("1")) visibility += 1;
        // Not working here atm, but just saw this and shouldn't there be a case for a '2' value?
        // It looks like there was one at one point but it got removed, referencing a
        // HeaderButton number two (hb2).  I understand the reluctance to renumber all the remaining
        // hb items, but numerically here the next progression would be += 2, so the real question
        // is - Is this working correctly, and if so - how/why?
        // TODO - add tests.  And eliminate this bothersome gap or explain why not.
        if (posString.contains("3")) visibility += 4;
        if (posString.contains("4")) visibility += 8;
        if (posString.contains("5")) visibility += 16;
        if (posString.contains("6")) visibility += 32;
        if (posString.contains("7")) visibility += 64;
        return visibility;
    } // end getVisibilityFromFormat

    // The date label needs to be re-calculated due to UI changes.
    void resetDateLabel() {
        theDateLabel.setText(FormatUtil.getDateString(getDate(), getFormat()));
    } // end resetDateLabel

    // Given the initialFormat, pre-select choices in the header menus
    //   and set Labels on the corresponding fields.
    private void setHeadersAndFields() {
        String s;
        JRadioButtonMenuItem jrbmi;

        s = FormatUtil.getFieldFromFormat(1, initialFormat); // Day format
        if (s.equals("")) s = "EEEE";  // default setting
        jrbmi = (JRadioButtonMenuItem) pop1.getComponent(3);
        if (s.equals("E")) jrbmi = (JRadioButtonMenuItem) pop1.getComponent(2);
        jrbmi.setSelected(true);
        hb1.setSeparator(getSeparatorFromFormat(1));
        fb1.setFormat(s);

        s = FormatUtil.getFieldFromFormat(3, initialFormat); // Month format
        if (s.equals("")) s = "MMMM";
        jrbmi = (JRadioButtonMenuItem) pop3.getComponent(5);
        if (s.equals("M")) jrbmi = (JRadioButtonMenuItem) pop3.getComponent(2);
        if (s.equals("MM")) jrbmi = (JRadioButtonMenuItem) pop3.getComponent(3);
        if (s.equals("MMM")) jrbmi = (JRadioButtonMenuItem) pop3.getComponent(4);
        jrbmi.setSelected(true);
        hb3.setSeparator(getSeparatorFromFormat(3));
        fb3.setFormat(s);

        s = FormatUtil.getFieldFromFormat(4, initialFormat); // Date format
        if (s.equals("")) s = "d";
        jrbmi = (JRadioButtonMenuItem) pop4.getComponent(2);
        if (s.equals("dd")) jrbmi = (JRadioButtonMenuItem) pop4.getComponent(3);
        jrbmi.setSelected(true);
        hb4.setSeparator(getSeparatorFromFormat(4));
        fb4.setFormat(s);

        s = FormatUtil.getFieldFromFormat(5, initialFormat); // Time format
        if (s.equals("")) s = tfp.getRealFormat(tfp.getFormat());
        else s = tfp.getRealFormat(s);
        fb5.setFormat(s);

        s = FormatUtil.getFieldFromFormat(6, initialFormat); // Year format
        if (s.equals("")) s = "yyyy";
        jrbmi = (JRadioButtonMenuItem) pop6.getComponent(3);
        if (s.equals("yy")) jrbmi = (JRadioButtonMenuItem) pop6.getComponent(2);
        jrbmi.setSelected(true);
        hb6.setSeparator(getSeparatorFromFormat(6));
        fb6.setFormat(s);

        s = FormatUtil.getFieldFromFormat(7, initialFormat); // Era format
        if (s.equals("")) s = "G";
        hb7.setSeparator(getSeparatorFromFormat(7));
        fb7.setFormat(s);
    } // end setHeadersAndFields

    private void setOrder(String s) {
        if (s.equals("")) return;
        // System.out.println("Setting order: " + s);
        int numFields = s.length();
        String ch;
        for (int i = 0; i < numFields; i++) {
            ch = s.substring(i, i + 1);

            // The value of the digit tells us which which hb/fb to add
            switch (Integer.parseInt(ch)) {
                case 1:
                    header.add(hb1, i);
                    fields.add(fb1, i);
                    break;
                case 3:
                    header.add(hb3, i);
                    fields.add(fb3, i);
                    break;
                case 4:
                    header.add(hb4, i);
                    fields.add(fb4, i);
                    break;
                case 5:
                    header.add(hb5, i);
                    fields.add(fb5, i);
                    break;
                case 6:
                    header.add(hb6, i);
                    fields.add(fb6, i);
                    break;
                case 7:
                    header.add(hb7, i);
                    fields.add(fb7, i);
            } // end switch
        } // end for i
        // System.out.println("Resultant order: " + getColumnOrder());
    } // end setOrder

    // The initialFormat string (set by the parameter idf),
    //   is used to configure the various elements in the interface as
    //   presented to the user.  When the user has completed their
    //   manipulation of these elements, the final state of the UI is
    //   queried to produce a new format.  Given that the final date
    //   string is comprised of several distinct substrings rather than
    //   a simple string, and given that the user has control over
    //   substring visibility and order as well as format, this format
    //   string is not used to directly format a date; it is stored
    //   as distinct segments and therefore must
    //   first be parsed.  Here, then, is the makeup of the format:
    //     <prefix>|<format1>|<format2> ....    where the format of the
    //   prefix is described in the header of the 'getColumnOrder' method,
    //   and the subsequent zero or more formats will match the known
    //   set of Java DateFormatSymbols.  Trailing separator strings for
    //   each element are also allowed if enclosed in single quotes.
    public void setup(long idt, String idf) {

        long initialDate;
        if (idf.equals("")) { // Set defaults, if necessary.
            if (initialFormat.equals("")) { // Was never set previously.
                initialFormat = getDefault();
            } // end if
            // Keep previous format but get current date.
//            initialDate = new Date().getTime();
            initialDate = LocalDate.now().toEpochDay();
        } else {
            initialDate = idt;
            initialFormat = idf;
        } // end if

        tfp.setup(initialDate, FormatUtil.getFieldFromFormat(5, initialFormat));

//        theDate = new Date(initialDate);
        theDate = Instant.ofEpochMilli(initialDate).atZone(TimeZone.getDefault().toZoneId());
//        theDate = LocalDate.ofEpochDay(initialDate);
        setVisibility(getVisibilityFromFormat());
        setOrder(getOrderFromFormat());
        setHeadersAndFields();
        resetDateLabel();

    } // end setup

    void setVisibility(int v) {
        // System.out.println("setVisibility: " + v);
        hb1.setVisible((v & 1) != 0);  // Day
        hb3.setVisible((v & 4) != 0);  // Month
        hb4.setVisible((v & 8) != 0);  // Date
        hb5.setVisible((v & 16) != 0);  // Time
        hb6.setVisible((v & 32) != 0);  // Year
        hb7.setVisible((v & 64) != 0);  // Era
        fb1.setVisible((v & 1) != 0);
        fb3.setVisible((v & 4) != 0);
        fb4.setVisible((v & 8) != 0);
        fb5.setVisible((v & 16) != 0);
        fb6.setVisible((v & 32) != 0);
        fb7.setVisible((v & 64) != 0);
    } // end setVisibility

    // Inner class
    //----------------------------------------------------------------
    class HeaderButton extends LabelButton {
        private static final long serialVersionUID = 117091180010309053L;

        DfbHeaderPopup pop;
        private String separatorString;

        HeaderButton(DfbHeaderPopup jpm) {
            super();
            String s = ((JLabel) jpm.getComponent(0)).getText();
            setText(s.trim());
            pop = jpm;
            separatorString = " ";

            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent me) {
                    pop.setVisible(false);
                    beenMoved = true;
                } // end mouseDragged
            });
        } // end constructor

        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            if (this == hb1) d.width = c1width; // Day
            if (this == hb3) d.width = c3width; // Month
            if (this == hb4) d.width = c4width; // Date
            if (this == hb5) d.width = c5width; // Time
            if (this == hb6) d.width = c6width; // Year
            if (this == hb7) d.width = c7width; // Era

            return d;
        } // end getPreferredSize

        public String getSeparator() {
            return separatorString;
        } // end getSeparator

        String getViewableSeparator() {
            String newSep = separatorString;
            newSep = newSep.replace("#SQUOTE#", "'");
            newSep = newSep.replace("#DQUOTE#", "\"");
            newSep = newSep.replace("#VBAR#", "|");
            return newSep;
        } // end getViewableSeparator

        void setSeparator(String s) {
            // See explanation in same method of TimeFormatBar$HeaderButton.
            String newSep = s;

            newSep = newSep.replace("'", "#SQUOTE#");
            newSep = newSep.replace("\"", "#DQUOTE#");
            newSep = newSep.replace("|", "#VBAR#");

            separatorString = newSep;
        } // end setSeparator

    } // end class HeaderButton

    class FieldLabel extends JLabel {
        private static final long serialVersionUID = 8671002847300078573L;

        String format;

        FieldLabel(String s) {
            super(s, JLabel.CENTER);

            format = "";
            setBorder(new MatteBorder(3, 3, 3, 3, Color.blue));
            setOpaque(true);
            setFont(Font.decode("Dialog-12"));
        } // end constructor

        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();

            if (this == fb1) d.width = c1width;
            if (this == fb3) d.width = c3width;
            if (this == fb4) d.width = c4width;
            if (this == fb5) d.width = c5width;
            if (this == fb6) d.width = c6width;
            if (this == fb7) d.width = c7width;
            return d;
        } // end getPreferredSize

        String getFormat() {
            return format;
        }

        void setFormat(String s) {
            format = s;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format).withZone(TimeZone.getDefault().toZoneId());
            setText(dtf.format(theDate));
        } // end setFormat

        public void setText(String s) {
            super.setText(s);
            Container c = getParent();
            if (c != null) c.doLayout();
        } // end setText
    } // end class FieldLabel

    public Vector<JComponent> getClingons(Component comp) {
        Vector<JComponent> ClingOns = new Vector<>(1, 1);
        if (comp == hb1) ClingOns.addElement(fb1);
        if (comp == hb3) ClingOns.addElement(fb3);
        if (comp == hb4) ClingOns.addElement(fb4);
        if (comp == hb5) ClingOns.addElement(fb5);
        if (comp == hb6) ClingOns.addElement(fb6);
        if (comp == hb7) ClingOns.addElement(fb7);
        ((DndLayout) fb1.getParent().getLayout()).Dragging = true;
        return ClingOns;
    } // end getClingons

    class popHandler implements ActionListener {
        DateFormatBar dfb;

        public void actionPerformed(ActionEvent e) {
            JMenuItem jm = (JMenuItem) e.getSource();
            String s = jm.getText();
            // System.out.println(s);

            if (s.equals("Set Trailing Characters")) {
                DfbHeaderPopup jpm = (DfbHeaderPopup) jm.getParent();
                HeaderButton hb = null;
                if (jpm == pop1) hb = hb1;
                if (jpm == pop3) hb = hb3;
                if (jpm == pop4) hb = hb4;
                if (jpm == pop5) hb = hb5;
                if (jpm == pop6) hb = hb6;
                if (jpm == pop7) hb = hb7;

                assert hb != null;
                String ss = hb.getViewableSeparator();
                String separatorString = ss;
                String title = "Separator Text";

                ss = (String) JOptionPane.showInputDialog(
                        hb,                           // parent component - for modality
                        "Enter up to 3 trailing characters",// prompt
                        title,                        // pane title bar
                        JOptionPane.QUESTION_MESSAGE, // type of pane
                        null,                         // icon
                        null,                         // list of choices
                        ss);                         // initial value

                if (ss == null) return;      // No user entry
                if (ss.equals(separatorString)) return; // No difference

                if (ss.length() > 3) ss = ss.substring(0, 3);
                // System.out.println("Setting new separator: '" + ss + "'");

                hb.setSeparator(ss);
                dfb.resetDateLabel();
                return;
            } // end if

            String nf = "";
            FieldLabel fb = null;
            if (jm.getParent() == pop1) fb = fb1;
            if (jm.getParent() == pop3) fb = fb3;
            if (jm.getParent() == pop4) fb = fb4;
            if (jm.getParent() == pop5) fb = fb5;
            if (jm.getParent() == pop6) fb = fb6;

            if (s.equals("3 letters (Ddd)")) nf = "E";
            if (s.equals("Full Day Name")) nf = "EEEE";
            if (s.equals("Day of week in Month (1-5)")) nf = "F";
            if (s.equals("Day in Year (1-366)")) nf = "D";
            if (s.equals("Week in Month (1-6)")) nf = "W";
            if (s.equals("Week in Year (1-54)")) nf = "w";
            if (s.equals("1-12")) nf = "M";
            if (s.equals("01-12")) nf = "MM";
            if (s.equals("3 letters (Mmm)")) nf = "MMM";
            if (s.equals("Full Month Name")) nf = "MMMM";
            if (s.equals("1-31")) nf = "d";
            if (s.equals("01-31")) nf = "dd";
            if (s.equals("2 digits")) nf = "yy";
            if (s.equals("4 digits")) nf = "yyyy";

            assert fb != null;
            fb.setFormat(nf);
            dfb.resetDateLabel();
        } // end actionPerformed

        // This handler needs to be added to items that are initialized in a
        //   'static' section, but the 'resetDateLabel' method of the outer
        //   class must be called from here and it is necessarily not static.
        //   Normal syntax would be to simply call that method since an inner
        //   class would have access, or possibly :
        //   'DateFormatBar.this.resetDateLabel', but neither is possible due
        //   to the static vs instantiated constraint.  Instead, the call can
        //   be accomplished by de-referencing the outer context via the 'dfb'
        //   variable, but setting it properly from the DateFormatBar
        //   constructor when it is actually instantiated.
        void setDfb(DateFormatBar value) {
            dfb = value;
        }
    } // end class popHandler

    // Made this class vs using JPopupMenu directly, solely to collect
    //    repetitive operations into the constructor, to shorten code.
    class DfbHeaderPopup extends JPopupMenu {
        private static final long serialVersionUID = -7409823293157418822L;

        DfbHeaderPopup(String[] s) {
            super();
            JMenuItem mi;
            String choice;
            ButtonGroup bg = null;

            String spaces = "                    "; // 20
            JLabel jl = new JLabel(spaces + s[0]);
            jl.setFont(jl.getFont().deriveFont(Font.BOLD));

            add(jl);
            add(new JSeparator());

            for (int i = 1; i < s.length; i++) {
                if (bg == null) bg = new ButtonGroup();
                choice = s[i];

                mi = new JRadioButtonMenuItem(choice);
                mi.addActionListener(al);
                bg.add(mi);
                add(mi);
            } // end for i

            add(new JSeparator());
            mi = new JMenuItem("Set Trailing Characters");
            mi.addActionListener(al);
            add(mi);
        } // end constructor
    } // end class DfbHeaderPopup

    class TimeFormatListener implements MouseListener {
        //---------------------------------------------------------
        // MouseListener methods
        //---------------------------------------------------------
        public void mouseClicked(MouseEvent e) {
            HeaderButton hb = (HeaderButton) e.getSource();

//            long dl = theDate.toEpochDay();
            long dl = theDate.toInstant().getEpochSecond();
            String df = FormatUtil.getFieldFromFormat(5, initialFormat);
            tfp.setup(dl, df);

            int choice = JOptionPane.showConfirmDialog(
                    hb,                           // parent component - for modality
                    tfp,                          // UI Object
                    "Specify a time format",      // pane title bar
                    JOptionPane.OK_CANCEL_OPTION, // Option type
                    JOptionPane.QUESTION_MESSAGE, // Message type
                    null);                       // icon

            if (choice != JOptionPane.OK_OPTION) return;
//            df = tfp.getFormat(); // see TimeFormatBar for format expl.
//            dl = tfp.getDate();
            // String s = TimeFormatBar.getDateString(dl, df);
            // System.out.println("End result: " + s);
            // System.out.println("Time format: " + tfp.getFormat());

            // Note: we 'know' that fb5 is the only fb that will go here.
            fb5.setFormat(tfp.getRealFormat(tfp.getFormat()));
            DateFormatBar.this.resetDateLabel();

        } // end mouseClicked

        public void mouseEntered(MouseEvent e) {
        } // end mouseEntered

        public void mouseExited(MouseEvent e) {
        } // end mouseExited

        public void mousePressed(MouseEvent e) {
        } // end mousePressed

        public void mouseReleased(MouseEvent e) {
        } // end mouseReleased
        //---------------------------------------------------------
    } // end class TimeFormatListener
} // end class DateFormatBar

