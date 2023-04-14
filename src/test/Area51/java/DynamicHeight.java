import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.Serial;

public class DynamicHeight {
    protected static int NUM_COMPONENTS = 35;
    JPanel groupNotesListPanel;
    private JScrollPane jsp;
    MouseAdapter ma;

    // Static values that are accessed from multiple contexts.
    static final Border offBorder;
    static Border redBorder;
    static Border highBorder;
    static Border lowBorder;

    static {
        //-----------------------------------
        // Create the borders.
        //-----------------------------------
        offBorder = LineBorder.createGrayLineBorder();
        redBorder = BorderFactory.createLineBorder(Color.red, 2);
        highBorder = new SoftBevelBorder(BevelBorder.RAISED);
        lowBorder = new SoftBevelBorder(BevelBorder.LOWERED);
    }

    public DynamicHeight() {
        ma = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JComponent jc = (JComponent) e.getSource();
                Container aContainer = jc.getParent();
                while(aContainer != null) {
                    System.out.println("container: " + aContainer.getName() + " " + aContainer);
                    System.out.println("  layout: " + aContainer.getLayout());
                    aContainer = aContainer.getParent();
                }
            }
        };
    }

    NoteTextArea makeNoteTextArea(String name) {
        NoteTextArea nta = new NoteTextArea();
        nta.setName(name);
        return nta;
    }

    NoteTextField makeNoteTextField(String name) {
        NoteTextField ntf = new NoteTextField();
        ntf.setName(name);
        return ntf;
    }

    public void populateContentPane(Container contentPane) {
        contentPane.setName("contentPane");
        groupNotesListPanel = new JPanel();
        groupNotesListPanel.setLayout(new BoxLayout(groupNotesListPanel, BoxLayout.PAGE_AXIS));
        groupNotesListPanel.setName("groupNotesListPanel");

        jsp = new JScrollPane() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, 400);
            } // end getPreferredSize
        };
        jsp.setName("jsp");


        //Create the initial components.
        for (int i = 0; i < NUM_COMPONENTS; i++) {
            if(i == 25) {
                NoteTextArea nta = makeNoteTextArea(String.valueOf(i));
                nta.setText("i ama NoteTextArea.");
                groupNotesListPanel.add(nta);
            } else {
                NoteTextField ntf = new NoteTextField();
                ntf.setName(String.valueOf(i));
                ntf.setText("I ma textfiled!");
                groupNotesListPanel.add(ntf);
            }

        }
        groupNotesListPanel.add(Box.createVerticalGlue());

        groupNotesListPanel.setBorder(BorderFactory.createLineBorder(Color.red));

        jsp.setViewportView(groupNotesListPanel);


        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(new JLabel("A Tree"));
        splitPane.setName("splitPane");

        splitPane.setRightComponent(jsp);


        contentPane.add(splitPane, BorderLayout.CENTER);

    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Dynamic Height Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        DynamicHeight demo = new DynamicHeight();
        demo.populateContentPane(frame.getContentPane());

        //Display the window.
        frame.pack();
        frame.setSize(400, 450);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(DynamicHeight::createAndShowGUI);
    }

    // This class implements a text field with a red border that
    //   appears when the focus is gained.
    protected class NoteTextArea extends JTextArea implements
            FocusListener, MouseListener {
        @Serial

        private static final long serialVersionUID = 1L;

        public NoteTextArea() {
            // By calling the super contructor with no rows or columns, we get the smallest possible TextArea.
            // But the component to which this inner class belongs is putting us into a stretchable Panel that
            //   (given the chosen font) will initially make enough room for text that is five rows in height.
            //   The number of columns will vary depending on the size of the container that holds the
            //   outer component.
            super();

            // This is needed so that the KeyListener will hear a TAB.
            setFocusTraversalKeysEnabled(false);

            setBorder(offBorder);
            addMouseListener(ma);
            addMouseListener(this);
            setFont(Font.decode("Dialog-bold-14"));
            addFocusListener(this);
        } // end constructor

        @Override
        public Dimension getMaximumSize() {
            Dimension d = super.getMaximumSize();

            // System.out.println("NoteTextField preferred size: " + d);
            d.height = 32;
            return d;
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension d = super.getPreferredSize();

            // System.out.println("NoteTextField preferred size: " + d);
            d.height = 120;
            return d;
        } // end getMinimumSize

        @Override
        public Dimension getPreferredSize() {
            return getMinimumSize();
        }

        @Override
        public void setText(String s) {
            super.setText(s);
            setCaretPosition(0); // Do not leave notes scrolled horizontally.
        }

        //=====================================================================
        // EVENT HANDLERS
        //=====================================================================

        //<editor-fold desc="FocusListener methods for the TextArea">
        // Note: The order of focusGained / focusLost along with
        //  the visual indicators (borders, highlighting) and
        //  how they can be invoked by either up/down arrows,
        //  mouse clicks, or the tab key - is critical!
        //  The key to it all is to disallow other components from
        //  getting the focus when they appear (most notably, the
        //  PopupMenu and the vertical scrollbar).  Then, do
        //  everything based on Focus here being gained or lost.
        public void focusGained(FocusEvent e) {
            // System.out.println("focusGained for index " + index);
            setBorder(redBorder);

            // We occasionally get a null pointer exception at startup.
            if (getCaret() == null) return;
// trying to disable the pre-highlighted text seen in todolists.  Need to consistently reproduce, first.
//            setSelectionStart(getSelectionEnd());
            getCaret().setVisible(true);
        } // end focusGained

        public void focusLost(FocusEvent e) {
            // System.out.println("focusLost for index " + index);
            setBorder(offBorder);
            getCaret().setVisible(false);
            // We do not de-select at this point because any selection would be lost
            // when the user clicks 'ok', for instance.
            // Instead, selections are cleared prior to presenting new choices.

        } // end focusLost
        //</editor-fold>

        //<editor-fold desc="MouseListener methods for the TextArea">
        public void mouseClicked(MouseEvent e) {
            if (!this.hasFocus()) {
                // The rmb click does not change focus, so we help it out.
                requestFocusInWindow();
            }
            JComponent jc = (JComponent) e.getSource();
            String theName = jc.getName();
            int theIndex = Integer.parseInt(theName);
            System.out.println("My name is: " + theName);
            NoteTextField ntf = makeNoteTextField(theName);
            ntf.setText("Brand New NTF!");
            groupNotesListPanel.remove(theIndex);
            groupNotesListPanel.add(ntf, theIndex);
            groupNotesListPanel.revalidate();
            ntf.requestFocusInWindow();
        } // end mouseClicked


        public void mouseEntered(MouseEvent e) {
        } // end mouseEntered

        public void mouseExited(MouseEvent e) {

        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }
        //</editor-fold>
    } // end class NoteTextArea

    // This class implements a text field with a red border that
    //   appears when the focus is gained.
    protected class NoteTextField extends JTextField implements
            FocusListener, MouseListener {
        @Serial
        private static final long serialVersionUID = 1L;
        public static final int minWidth = 80;

        public NoteTextField() {
            // This is needed so that the KeyListener will hear a TAB.
            setFocusTraversalKeysEnabled(false);

            setBorder(offBorder);
            addMouseListener(ma);
            addMouseListener(this);
            setFont(Font.decode("Dialog-bold-14"));
            addFocusListener(this);
        } // end constructor

        private void clear() {
            // Clear the text field
            setText(null);
            setForeground(Color.black);
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension d = super.getMaximumSize();

            // System.out.println("NoteTextField preferred size: " + d);
            d.height = 32;
            return d;
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension d = super.getPreferredSize();

            // System.out.println("NoteTextField preferred size: " + d);
            d.width = minWidth;
            d.height = 24;
            return d;
        } // end getMinimumSize

        @Override
        public Dimension getPreferredSize() {
            return getMinimumSize();
        }

        @Override
        public void setText(String s) {
            super.setText(s);
            setCaretPosition(0); // Do not leave notes scrolled horizontally.
        }

        //=====================================================================
        // EVENT HANDLERS
        //=====================================================================

        //<editor-fold desc="FocusListener methods for the TextField">
        // Note: The order of focusGained / focusLost along with
        //  the visual indicators (borders, highlighting) and
        //  how they can be invoked by either up/down arrows,
        //  mouse clicks, or the tab key - is critical!
        //  The key to it all is to disallow other components from
        //  getting the focus when they appear (most notably, the
        //  PopupMenu and the vertical scrollbar).  Then, do
        //  everything based on Focus here being gained or lost.
        public void focusGained(FocusEvent e) {
            // System.out.println("focusGained for index " + index);
            setBorder(redBorder);

            // We occasionally get a null pointer exception at startup.
            if (getCaret() == null) return;
            getCaret().setVisible(true);

        } // end focusGained

        public void focusLost(FocusEvent e) {
            // System.out.println("focusLost for index " + index);
            setBorder(offBorder);
            getCaret().setVisible(false);
            // We do not de-select at this point because any selection would be lost
            // when the user clicks 'ok', for instance.
            // Instead, selections are cleared prior to presenting new choices.

        } // end focusLost
        //</editor-fold>


        //<editor-fold desc="MouseListener methods for the TextField">
        public void mouseClicked(MouseEvent e) {
            if (!this.hasFocus()) {
                // The rmb click does not change focus, so we help it out.
                requestFocusInWindow();
            }
            JComponent jc = (JComponent) e.getSource();
            String theName = jc.getName();
            int theIndex = Integer.parseInt(theName);
            System.out.println("My name is: " + theName);
            NoteTextArea nta = makeNoteTextArea(theName);
            nta.setText("Brand New NTA!");
            groupNotesListPanel.remove(theIndex);
            groupNotesListPanel.add(nta, theIndex);
            groupNotesListPanel.revalidate();
            nta.requestFocusInWindow();
        } // end mouseClicked

        public void mouseEntered(MouseEvent e) {
        } // end mouseEntered

        public void mouseExited(MouseEvent e) {

        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }
        //</editor-fold>

    } // end class NoteTextField

}
