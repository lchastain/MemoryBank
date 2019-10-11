//
// This class is a User Interface for setting the format of
//   a time.  To use, instantiate once and then call setup to
//   initialize prior to each display of the UI. 
//
import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.text.DateFormatSymbols;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

public class TimeFormatBar extends Container implements ClingSource {
    private static final long serialVersionUID = 1L;

    private static HeaderButton hb1;   // Hours
    private static HeaderButton hb2;   // Minutes
    private static HeaderButton hb3;   // Seconds
    private static HeaderButton hb4;   // AM/PM
    private static HeaderButton hb5;   // TZ (TimeZone)

    private static FieldLabel fb1;    // Hours
    private static FieldLabel fb2;    // Minutes
    private static FieldLabel fb3;    // Seconds
    private static FieldLabel fb4;    // AM/PM
    private static FieldLabel fb5;    // TZ (TimeZone)

    private ZonedDateTime theDateTime;
    private JLabel theDateLabel;
    private String initialFormat;
    private JPanel header;
    private JPanel fields;
    private static DfbHeaderPopup pop1;
    private static DfbHeaderPopup pop2;
    private static DfbHeaderPopup pop3;
    private static DfbHeaderPopup pop4;
    private static DfbHeaderPopup pop5;
    private static popHandler al;
    private static DateTimeFormatter dtf;
    static DateFormatSymbols dfs;

    // Known maximum widths needed for columns
    private static final int c1width = 28; // Hours
    private static final int c2width = 28; // Minutes
    private static final int c3width = 28; // Seconds
    private static final int c4width = 56; // AM/PM
    private static final int c5width = 150; // TZ

    private boolean beenMoved;

    static {
        String[] choices;
        al = new popHandler();

        choices = new String[]{
                "Hours",
                "Hour in AM/PM (0-11)",
                "Hour in AM/PM (1-12)",
                "Hour in Day (1-24)",
                "Hour in Day (0-23)"};
        pop1 = new DfbHeaderPopup(choices);

        choices = new String[]{"Minutes"};
        pop2 = new DfbHeaderPopup(choices);
        pop2.remove(2); // an extra separator

        choices = new String[]{"Seconds"};
        pop3 = new DfbHeaderPopup(choices);
        pop3.remove(2); // an extra separator

        choices = new String[]{"AM/PM"};
        pop4 = new DfbHeaderPopup(choices);
        pop4.remove(2); // an extra separator

        choices = new String[]{
                "TimeZone",
                "Short Form",
                "Long Form"};
        pop5 = new DfbHeaderPopup(choices);
    } // end static

    public TimeFormatBar() {
        super();
        setLayout(new GridLayout(0, 1, 0, 0));

        al.setDfb(this);

        beenMoved = false;
        initialFormat = "";

        DndLayout headerLayout = new DndLayout();
        headerLayout.setMoveable(true);
        headerLayout.setClingSource(this);
        header = new JPanel(headerLayout) {
            private static final long serialVersionUID = -1163528668614895962L;

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
        hb2 = new HeaderButton(pop2);
        hb3 = new HeaderButton(pop3);
        hb4 = new HeaderButton(pop4);
        hb5 = new HeaderButton(pop5);

        header.add(hb1, "1");
        header.add(hb2, "2");
        header.add(hb3, "3");
        header.add(hb4, "4");
        header.add(hb5, "5");
        add(header);

        fb1 = new FieldLabel(" ");
        fb2 = new FieldLabel(" ");
        fb3 = new FieldLabel(" ");
        fb4 = new FieldLabel(" ");
        fb5 = new FieldLabel(" ");

        fields.add(fb1, "1");
        fields.add(fb2, "2");
        fields.add(fb3, "3");
        fields.add(fb4, "4");
        fields.add(fb5, "5");
        add(fields);

        add(new JLabel("   ")); // spacer

        theDateLabel = new JLabel(" ", JLabel.CENTER);
        theDateLabel.setFont(theDateLabel.getFont().deriveFont(Font.BOLD));
        add(theDateLabel);
    } // end constructor

    public static String getDefault() {
        return "124!h':'!mm' '!a' '";
    } // end getDefault

    // There are five possible columnns, in any order.
    //   This function returns a String that describes
    //   the order and visibility of columns in the header.
    public String getColumnOrder() {
        HeaderButton hb;
        StringBuilder returnString = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            hb = (HeaderButton) header.getComponent(i);
            if (!hb.isVisible()) continue;
            if (hb.getText().equals(hb1.getText())) returnString.append("1");
            if (hb.getText().equals(hb2.getText())) returnString.append("2");
            if (hb.getText().equals(hb3.getText())) returnString.append("3");
            if (hb.getText().equals(hb4.getText())) returnString.append("4");
            if (hb.getText().equals(hb5.getText())) returnString.append("5");
        } // end for i
        if (returnString.toString().equals("")) returnString = new StringBuilder("0");

        return returnString.toString();
    } // end getColumnOrder

    public long getDate() {
        return theDateTime.toInstant().getEpochSecond();
    } // end getDate

    public static String getDateString(long theDate, String theFormat) {
        ZonedDateTime d = Instant.ofEpochMilli(theDate).atZone(ZoneId.systemDefault());
        String s = getRealFormat(theFormat);
        dtf = DateTimeFormatter.ofPattern(s).withZone(ZoneId.systemDefault());
        return dtf.format(d);
    } // end getDateString


    // Here is where the format is interpreted.
    // why do we have one here and another in FormatUtil, that calls this one?  This needs consolidation.
    public static String getRealFormat(String theFormat) {
        // System.out.println("Format parsing: [" + theFormat + "]");
        if (theFormat.equals("")) return "";  // never been set
        if (theFormat.equals("0")) return ""; // explicitly set to ""

        String s = theFormat;
        s = s.substring(s.indexOf("!") + 1); // Drop off order prefix
        s = s.replace("!", "");    // Remove delimiters

        s = s.replace("#SQUOTE#", "''");
        s = s.replace("#DQUOTE#", "\"");
        s = s.replace("#EPOINT#", "!");
        s = s.replace("#VBAR#", "|");
        // System.out.println("The REAL format specifier is: " + s);
        return s;
    } // end getRealFormat

    // Given the known configuration of the initialFormat string,
    //   parse out and return the requested element.
    private String getFieldFromFormat(int i) {
        String s = getOrderFromFormat();  // String may contain "12345"
        int numFields = s.length();
        String theField = "";
        int pos = s.indexOf(String.valueOf(i));
        if (pos == -1) return theField; // this field not in the format.
        // System.out.println("Looking for field " + i + " in " + initialFormat);

        // Get past the order-specifying prefix of the format string
        String suffix = initialFormat.substring(s.length() + 1);
        // System.out.println("suffix: " + suffix);

        for (int j = 0; j < numFields; j++) {
            if (j == numFields - 1) theField = suffix;  // last field
            else theField = suffix.substring(0, suffix.indexOf("!"));
            if (j == pos) break;
            suffix = suffix.substring(theField.length() + 1);
        } // end for j

        // Now cut off the separator string, if any.
        int sspos = theField.indexOf("'");
        if (sspos == -1) return theField;
        return theField.substring(0, sspos);
    } // end getFieldFromFormat

    // Given the known configuration of the initialFormat string,
    //   parse out and return the requested element.
    private String getSeparatorFromFormat(int i) {
        String s = getOrderFromFormat();  // String may contain "12345"
        int numFields = s.length();
        String theField = "";
        int pos = s.indexOf(String.valueOf(i));
        if (pos == -1) return " "; // The default; this field not in the format.

        // Get past the order-specifying prefix of the format string
        String suffix = initialFormat.substring(s.length() + 1);

        for (int j = 0; j < numFields; j++) {
            if (j == numFields - 1) theField = suffix;  // last field
            else theField = suffix.substring(0, suffix.indexOf("!"));
            if (j == pos) break;
            suffix = suffix.substring(theField.length() + 1);
        } // end for j

        // Got the Field; now process the separator string, if any.
        int sspos = theField.indexOf("'");
        if (sspos == -1) return ""; // Field yes, separator no.

        // Trim of the single quotes.
        String theSeparator = theField.substring(sspos + 1);
        theSeparator = theSeparator.substring(0, theSeparator.length() - 1);
        return theSeparator;
    } // end getSeparatorFromFormat

    // Construct a format string based on the current configuration
    //   of the interface.
    public String getFormat() {
        StringBuilder s;
        String order = getColumnOrder();
        s = new StringBuilder(order);
        if (order.equals("0")) return s.toString();
        int numcols = order.length();
        for (int i = 0; i < numcols; i++) {
            if (order.substring(i, i + 1).equals("1")) {
                s.append("!").append(fb1.getFormat());
                if (hb1.separatorString.length() != 0)
                    s.append("'").append(hb1.getSeparator()).append("'");
            } // end if

            if (order.substring(i, i + 1).equals("2")) {
                s.append("!").append(fb2.getFormat());
                if (hb2.separatorString.length() != 0)
                    s.append("'").append(hb2.getSeparator()).append("'");
            } // end if

            if (order.substring(i, i + 1).equals("3")) {
                s.append("!").append(fb3.getFormat());
                if (hb3.separatorString.length() != 0)
                    s.append("'").append(hb3.getSeparator()).append("'");
            } // end if

            if (order.substring(i, i + 1).equals("4")) {
                s.append("!").append(fb4.getFormat());
                if (hb4.separatorString.length() != 0)
                    s.append("'").append(hb4.getSeparator()).append("'");
            } // end if

            if (order.substring(i, i + 1).equals("5")) {
                s.append("!").append(fb5.getFormat());
                if (hb5.separatorString.length() != 0)
                    s.append("'").append(hb5.getSeparator()).append("'");
            } // end if

        } // end for i

        // System.out.println("The INTERNAL format specifier is: " + s);
        initialFormat = s.toString();
        return s.toString();
    } // end getFormat

    private String getOrderFromFormat() {
        int end = initialFormat.indexOf("!");
        if (end == -1) return "";

        return initialFormat.substring(0, end);
    } // end getOrderFromFormat

    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width = 0;
        d.width += hb1.getPreferredSize().width;
        d.width += hb2.getPreferredSize().width;
        d.width += hb3.getPreferredSize().width;
        d.width += hb4.getPreferredSize().width;
        d.width += hb5.getPreferredSize().width;
        return d;
    } // end getPreferredSize

    public int getVisibilityFromFormat() {
        int visibility = 0;

        String posString = getOrderFromFormat();
        if (posString.equals("")) return visibility; // visibility remains 0
        if (posString.contains("1")) visibility += 1;
        if (posString.contains("2")) visibility += 2;
        if (posString.contains("3")) visibility += 4;
        if (posString.contains("4")) visibility += 8;
        if (posString.contains("5")) visibility += 16;
        return visibility;
    } // end getVisibilityFromFormat

    // The date label needs to be re-calculated due to UI changes.
    public void resetDateLabel() {
        theDateLabel.setText(getDateString(getDate(), getFormat()));
    } // end resetDateLabel

    // Given the initialFormat, pre-select choices in the header menus
    //   and set Labels on the corresponding fields.
    private void setHeadersAndFields() {
        String s;
        JRadioButtonMenuItem jrbmi;

        s = getFieldFromFormat(1); // Hour format
        if (s.equals("")) s = "h";  // default setting
        jrbmi = (JRadioButtonMenuItem) pop1.getComponent(3);
        if (s.equals("K")) jrbmi = (JRadioButtonMenuItem) pop1.getComponent(2);
        if (s.equals("k")) jrbmi = (JRadioButtonMenuItem) pop1.getComponent(4);
        if (s.equals("H")) jrbmi = (JRadioButtonMenuItem) pop1.getComponent(5);
        jrbmi.setSelected(true);
        hb1.setSeparator(getSeparatorFromFormat(1));
        fb1.setFormat(s);

        s = getFieldFromFormat(2); // Minute format
        if (s.equals("")) s = "mm";
        hb2.setSeparator(getSeparatorFromFormat(2));
        fb2.setFormat(s);

        s = getFieldFromFormat(3); // Seconds format
        if (s.equals("")) s = "ss";
        hb3.setSeparator(getSeparatorFromFormat(3));
        fb3.setFormat(s);

        s = getFieldFromFormat(4); // AM/PM format
        if (s.equals("")) s = "a";
        hb4.setSeparator(getSeparatorFromFormat(4));
        fb4.setFormat(s);

        s = getFieldFromFormat(5); // TimeZone format
        if (s.equals("")) s = "z";
        jrbmi = (JRadioButtonMenuItem) pop5.getComponent(2);
        if (s.equals("zzzz")) jrbmi = (JRadioButtonMenuItem) pop5.getComponent(3);
        jrbmi.setSelected(true);
        hb5.setSeparator(getSeparatorFromFormat(5));
//        fb5.setFormat(s);

    } // end setHeadersAndFields

    public void setOrder(String s) {
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
                case 2:
                    header.add(hb2, i);
                    fields.add(fb2, i);
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
    //     <prefix>!<format1>!<format2> ....    where the format of the
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
            initialDate = LocalDate.now().toEpochDay();
        } else {
            initialDate = idt;
            initialFormat = idf;
        } // end if

        theDateTime =  Instant.ofEpochMilli(initialDate).atZone(ZoneId.systemDefault());
        setVisibility(getVisibilityFromFormat());
        setOrder(getOrderFromFormat());
        setHeadersAndFields();
        resetDateLabel();
    } // end setup

    public void setVisibility(int v) {
        hb1.setVisible((v & 1) != 0);
        hb2.setVisible((v & 2) != 0);
        hb3.setVisible((v & 4) != 0);
        hb4.setVisible((v & 8) != 0);
        hb5.setVisible((v & 16) != 0);
        fb1.setVisible((v & 1) != 0);
        fb2.setVisible((v & 2) != 0);
        fb3.setVisible((v & 4) != 0);
        fb4.setVisible((v & 8) != 0);
        fb5.setVisible((v & 16) != 0);
    } // end setVisibility

    // Inner class
    //----------------------------------------------------------------
    class HeaderButton extends LabelButton {
        private static final long serialVersionUID = -656293884508257610L;

        DfbHeaderPopup pop;
        private String separatorString;

        HeaderButton(DfbHeaderPopup jpm) {
            super();
            String s = ((JLabel) jpm.getComponent(0)).getText();

            s = s.trim();
            if (s.equals("Hours")) s = "HH";
            if (s.equals("Minutes")) s = "MM";
            if (s.equals("Seconds")) s = "SS";
            setText(s);

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
            if (this == hb1) d.width = c1width; // Hours
            if (this == hb2) d.width = c2width; // Minutes
            if (this == hb3) d.width = c3width; // Seconds
            if (this == hb4) d.width = c4width; // AM/PM
            if (this == hb5) d.width = c5width; // TZ

            return d;
        } // end getPreferredSize

        public String getSeparator() {
            return separatorString;
        } // end getSeparator

        String getViewableSeparator() {
            String newSep = separatorString;
            newSep = newSep.replace("#SQUOTE#", "'");
            newSep = newSep.replace("#DQUOTE#", "\"");
            newSep = newSep.replace("#EPOINT#", "!");
            newSep = newSep.replace("#VBAR#", "|");
            return newSep;
        } // end getViewableSeparator

        public void setSeparator(String s) {
            // There are four illegal characters in a separator string -
            //   the single quote, double quote, exclamation point, and
            //   the vertical bar.  These are all delimiters in the overall
            //   format but can be shown in a separator after escaping
            //   their delimiting action.  This is done via a conversion:

            // single quote is converted to: #SQUOTE#
            // double quote is converted to: #DQUOTE#
            // exclamation point is converted to: #EPOINT#
            // vertical bar is converted to: #VBAR#

            // Of course, this has the side-effect of making the above
            //   strings into a sequence of illegal characters, but
            //   those strings are extremely unlikely to be used as
            //   valid separators.  Further, the result will be a
            //   conversion to the indicated character, not a thrown
            //   exception with accompanying stack trace dump.

            String newSep = s;
            newSep = newSep.replace("'", "#SQUOTE#");
            newSep = newSep.replace("\"", "#DQUOTE#");
            newSep = newSep.replace("!", "#EPOINT#");
            newSep = newSep.replace("|", "#VBAR#");

            separatorString = newSep;
        } // end setSeparator

        //---------------------------------------------------------
        // MouseListener methods
        //---------------------------------------------------------
        public void mouseClicked(MouseEvent e) {
            HeaderButton hb = (HeaderButton) e.getSource();
            int x = TimeFormatBar.this.getX() + 20;
            int y = hb.getY() + hb.getHeight();

            SwingUtilities.updateComponentTreeUI(pop);
            pop.show(TimeFormatBar.this, x, y);
        } // end mouseClicked

        public void mousePressed() {
            if (pop.isShowing()) pop.setVisible(false);
        } // end mousePressed

        //---------------------------------------------------------
    } // end class HeaderButton

    class FieldLabel extends JLabel {
        private static final long serialVersionUID = -5764785385765106143L;

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
            if (this == fb2) d.width = c2width;
            if (this == fb3) d.width = c3width;
            if (this == fb4) d.width = c4width;
            if (this == fb5) d.width = c5width;
            return d;
        } // end getPreferredSize

        public String getFormat() {
            return format;
        }

        public void setFormat(String s) {
            if(theDateTime == null) return;
            format = s;
            dtf = DateTimeFormatter.ofPattern(format);
            setText(dtf.format(theDateTime));
        } // end setFormat

        public void setText(String s) {
            super.setText(s);
            Container c = getParent();
            if (c != null) c.doLayout();
        } // end setText

    } // end class FieldLabel

    public Vector<JComponent> getClingons(Component comp) {
        Vector<JComponent> ClingOns = new Vector<JComponent>(1, 1);
        if (comp == hb1) ClingOns.addElement(fb1);
        if (comp == hb2) ClingOns.addElement(fb2);
        if (comp == hb3) ClingOns.addElement(fb3);
        if (comp == hb4) ClingOns.addElement(fb4);
        if (comp == hb5) ClingOns.addElement(fb5);
        ((DndLayout) fb1.getParent().getLayout()).Dragging = true;
        return ClingOns;
    } // end getClingons

    static class popHandler implements ActionListener {
        TimeFormatBar dfb;

        public void actionPerformed(ActionEvent e) {
            JMenuItem jm = (JMenuItem) e.getSource();
            String s = jm.getText();
            // System.out.println(s);

            if (s.equals("Set Trailing Characters")) {
                DfbHeaderPopup jpm = (DfbHeaderPopup) jm.getParent();
                HeaderButton hb = null;
                if (jpm == pop1) hb = hb1;
                if (jpm == pop2) hb = hb2;
                if (jpm == pop3) hb = hb3;
                if (jpm == pop4) hb = hb4;
                if (jpm == pop5) hb = hb5;

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
            if (jm.getParent() == pop2) fb = fb2;
            if (jm.getParent() == pop3) fb = fb3;
            if (jm.getParent() == pop4) fb = fb4;
            if (jm.getParent() == pop5) fb = fb5;

            if (s.equals("Hour in AM/PM (0-11)")) nf = "K";
            if (s.equals("Hour in AM/PM (1-12)")) nf = "h";
            if (s.equals("Hour in Day (1-24)")) nf = "k";
            if (s.equals("Hour in Day (0-23)")) nf = "H";
            if (s.equals("Short Form")) nf = "z";
            if (s.equals("Long Form")) nf = "zzzz";

            assert fb != null;
            fb.setFormat(nf);
            dfb.resetDateLabel();
        } // end actionPerformed

        // This handler needs to be added to items that are initialized in a
        //   'static' section, but later, the 'resetDateLabel' method must be
        //   called from here and it is necessarily not static.  This can be
        //   accomplished by de-referencing the outer context via the 'dfb'
        //   variable, but setting it properly from the TimeFormatBar
        //   constructor when it is actually instantiated.
        void setDfb(TimeFormatBar value) {
            dfb = value;
        }
    } // end class popHandler

    // Made this class vs using JPopupMenu directly, solely to collect
    //    repetitive operations into the constructor, to shorten code.
    static class DfbHeaderPopup extends JPopupMenu {
        private static final long serialVersionUID = -2466875005184168161L;

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

} // end class TimeFormatBar

