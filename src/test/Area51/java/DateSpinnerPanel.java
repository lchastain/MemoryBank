import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// T&E code, looking at JSpinner variants.
// Morphed from a tutorial that came from Java 8 but still uses java.util.Date and Calendar.

public class DateSpinnerPanel extends JPanel {
    JSpinner spinner;
    SpinnerListModel monthModel;

    public DateSpinnerPanel(String theLabelText, ChronoUnit theDateType) {
//        super(new FlowLayout());
        if (theLabelText != null) add(new JLabel(theLabelText));

        String[] labels = {"Month: ", "Year: ", "Another Date: "};
        JFormattedTextField ftf;

        if (theDateType.equals(ChronoUnit.MONTHS)) {
            //Add the first label-spinner pair.
            String[] monthStrings = getMonthStrings(); //get month names
            monthModel = new CyclingSpinnerListModel(monthStrings);

            spinner = new JSpinner(monthModel);
            spinner.setFont(new Font("Tahoma", Font.PLAIN, 16));
            add(spinner);


            //Tweak the spinner's formatted text field.
            ftf = getTextField(spinner);
            if (ftf != null) {
                ftf.setColumns(7);
                ftf.setHorizontalAlignment(JTextField.CENTER);
            }
        }

        if(theDateType == ChronoUnit.YEARS) {
            //Add second label-spinner pair.
            int currentYear = LocalDate.now().getYear();
            SpinnerModel yearModel = new SpinnerNumberModel(currentYear, //initial value
                    currentYear - 100, //min
                    currentYear + 100, //max
                    1);                //step
            //If we're cycling, hook this model up to the month model.
            if (monthModel instanceof CyclingSpinnerListModel) {
                ((CyclingSpinnerListModel) monthModel).setLinkedModel(yearModel);
            }
            spinner = addLabeledSpinner(this, labels[1], yearModel);
            //Make the year be formatted without a thousands separator.
            spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));
        }

    }


    /**
     * Return the formatted text field used by the editor, or
     * null if the editor doesn't descend from JSpinner.DefaultEditor.
     */
    public JFormattedTextField getTextField(JSpinner spinner) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            return ((JSpinner.DefaultEditor) editor).getTextField();
        } else {
            System.err.println("Unexpected editor type: "
                    + spinner.getEditor().getClass()
                    + " isn't a descendant of DefaultEditor");
            return null;
        }
    }

    /**
     * DateFormatSymbols returns an extra, empty value at the
     * end of the array of months.  Remove it.
     */
    static protected String[] getMonthStrings() {
        String[] months = new java.text.DateFormatSymbols().getMonths();
        int lastIndex = months.length - 1;

        if (months[lastIndex] == null
                || months[lastIndex].length() <= 0) { //last item empty
            String[] monthStrings = new String[lastIndex];
            System.arraycopy(months, 0,
                    monthStrings, 0, lastIndex);
            return monthStrings;
        } else { //last item not empty
            return months;
        }
    }

    static protected JSpinner addLabeledSpinner(Container c,
                                                String label,
                                                SpinnerModel model) {
        JLabel l = new JLabel(label);
        c.add(l);

        JSpinner spinner = new JSpinner(model);
        l.setLabelFor(spinner);
        c.add(spinner);

        return spinner;
    }

    static class CyclingSpinnerListModel extends SpinnerListModel {
        Object firstValue, lastValue;
        SpinnerModel linkedModel = null;

        public CyclingSpinnerListModel(Object[] values) {
            super(values);
            firstValue = values[0];
            lastValue = values[values.length - 1];
        }

        public void setLinkedModel(SpinnerModel linkedModel) {
            this.linkedModel = linkedModel;
        }

        public Object getNextValue() {
            Object value = super.getNextValue();
            if (value == null) {
                value = firstValue;
                if (linkedModel != null) {
                    linkedModel.setValue(linkedModel.getNextValue());
                }
            }
            return value;
        }

        public Object getPreviousValue() {
            Object value = super.getPreviousValue();
            if (value == null) {
                value = lastValue;
                if (linkedModel != null) {
                    linkedModel.setValue(linkedModel.getPreviousValue());
                }
            }
            return value;
        }
    }

    public static void main(String[] args) {
//        UIManager.put("swing.boldMetal", Boolean.FALSE);

        //Create and set up the window.
        JFrame testFrame = new JFrame("DateSpinner Driver");
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Add content to the window.
        Box theBox = new Box(BoxLayout.Y_AXIS);
        theBox.add(new DateSpinnerPanel("Month:", ChronoUnit.MONTHS));
        theBox.add(new DateSpinnerPanel("Month:", ChronoUnit.YEARS));

        testFrame.getContentPane().add(theBox, "Center");

        // Needed to override the 'metal' L&F for Swing components.
        String laf = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(laf);
        } catch (Exception ignored) {
        }    // end try/catch
        SwingUtilities.updateComponentTreeUI(testFrame);

        //Display the window.
        testFrame.pack();
        testFrame.setSize(new Dimension(300, 250));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }
}
