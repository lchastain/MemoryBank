
/*
 A pairing of a TextArea that contains a 'note', with a ComboBox that shows the note's 'Subject'.
 Subjects can be set to any string value, including one not in the list.
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Vector;

public class ExtendedNoteComponent extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final int maxSubjects = 20;

    // This Panel can have variable content; default declarations below.
    JComboBox<String> subjectChooser;
    protected JTextArea body;  // held/shown in a JScrollPane

    JComponent subjectComponent; // Edit the Subject with either a JTextField or a JComboBox
    String phantomText;  // Leave this null, if not being used.

    private Vector<String> subjects;
    private String mySubject;
    private final String theDefaultSubject;

    // This flag is reset to false when subjects are saved.
    private boolean subjectsChanged = false;

    public ExtendedNoteComponent(String defaultSubject) {
        super(new BorderLayout());
        body = new JTextArea(12, 80) {
            // To stop the compiler from whining -
            private static final long serialVersionUID = 1L;

            public void addNotify() {
                super.addNotify();
                requestFocusInWindow(); // wouldn't work from other places...
            } // end addNotify
        };
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if(phantomText == null) return;
                if (body.getText().trim().isEmpty()) {
                    body.setForeground(Color.GRAY);
                    body.setText(phantomText);
                }
            }
        });
        body.addKeyListener(new KeyAdapter() {
            boolean clearingDefault;

            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if(phantomText == null) return;
                char theyTyped = e.getKeyChar();
                if (body.getText().equals(phantomText)) {
                    body.setText("");
                    body.setForeground(Color.BLACK);
                    clearingDefault = true;
                }

                if (Character.isLetterOrDigit(theyTyped)) {
                    clearingDefault = false;
                }

            }

            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if(phantomText == null) return;
                if (!clearingDefault) {
                    if (body.getText().trim().isEmpty()) {
                        body.setForeground(Color.GRAY);
                        body.setText(phantomText);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(body);

        if (defaultSubject != null) {
            if (defaultSubject.equals("Goal Title")) {
                subjectComponent = new JTextField();
                subjectComponent.setFont(Font.decode("Serif-bold-12"));
                add(subjectComponent, "North");
            } else if (defaultSubject.equals("Search Info")) {
                subjectComponent = new JTextField(32);
                JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5, 0));
                JLabel titleLabel = new JLabel("    Search Title: ");
                titleLabel.setFont(Font.decode("Serif-bold-12"));
                titlePanel.add(titleLabel);
                titlePanel.add(subjectComponent);
                subjectComponent.setFont(Font.decode("Serif-bold-12"));
                add(titlePanel, "North");
            } else { // Get the candidate Subjects, possibly just a default list.
                subjects = MemoryBank.dataAccessor.loadSubjects(defaultSubject);

                subjectChooser = new JComboBox<>(subjects);
                subjectChooser.setEditable(true);
                subjectChooser.setFont(Font.decode("Serif-bold-12"));
                // Note: too large of font here causes display problems.

                subjectChooser.setToolTipText("Type or Select the Subject for this note");
                subjectChooser.setMaximumRowCount(maxSubjects);
                subjectComponent = subjectChooser;
                add(subjectChooser, "North");
            }
        }

        theDefaultSubject = defaultSubject;
        add(scrollPane, BorderLayout.CENTER);
    } // end constructor


    void addSubject(String s) {
        // Do not want to add the subject to the file in these cases.
        if (s.equals("")) return;
        if (s.equals(theDefaultSubject)) return;

        MemoryBank.debug("Adding subject: [" + s + "]");

        //------------------------------------------------------------------
        // Check to see if this subject is already first in the list -
        //------------------------------------------------------------------
        if (subjects.size() > 0) {
            if ((subjects.elementAt(0)).equals(s)) return;
        } // end if

        //------------------------------------------------------------------
        // Then, remove an occurrence of this subject lower in the list.
        //------------------------------------------------------------------
        subjects.remove(s);

        //------------------------------------------------------------------
        // Then, put this subject at the top of the list.
        //------------------------------------------------------------------
        subjects.insertElementAt(s, 0);

        //------------------------------------------------------------------
        // Then, if the list has grown too big, truncate.
        //------------------------------------------------------------------
        if (subjects.size() > maxSubjects) {
            subjects.remove(subjects.lastElement());
        } // end if too many

        subjectsChanged = true;
        // System.out.println("subjects size: " + subjects.size());
    } // end addSubject


    // This is needed to acquire text that might be typed into the Subject combobox control.
    // Needs to be called from a NoteGroup during an Edit session for the ExtendedNoteComponent,
    // because there is nothing in place to do this automatically as the result of an awt or
    // swing event.  There could be, of course, but I opted to NOT go that route because it
    // would be called for every single keypress and dropdown selection, whereas this one is
    // only needed once, at the end of the edit session.
    void updateSubject() {
        if(subjectComponent == null) return;  // No subject chooser for Todo items.

        if(subjectComponent instanceof JComboBox) {
            String theSubject = (String) subjectChooser.getEditor().getItem();
            // Note: Cannot use '.getSelectedItem()' above, because it may have
            //   just been typed in and not be in the list, therefore it is not
            //   the selected item.

            if (theSubject == null) return;
            else theSubject = theSubject.trim();
            mySubject = theSubject;
            addSubject(mySubject);  // Move to the top of the list.
        } else { // it is a JTextField, so
            mySubject = ((JTextField) subjectComponent).getText();
        }
    } // end updateSubject


    String getExtText() {
        String theText = body.getText();
        if(theText.equals(phantomText)) return "";
        return theText;
    }


    public String getSubject() {
        if (mySubject == null) return null;
        if (mySubject.equals(theDefaultSubject)) return null;
        else return mySubject;
    } // end getSubject


    int getSubjectCount() {
        return subjects.size();
    } // end getSubjectCount


    void setExtText(String s) {
        if (s != null && !s.trim().isEmpty()) {
            body.setText(s);
            body.setForeground(Color.BLACK);
        } else {
            if (phantomText != null) {
                body.setText(phantomText);
                body.setForeground(Color.GRAY);
            } else { // Just wipe the text.  Otherwise old text shows up when adding to new, should-be-empty components.
                body.setText(s);
            }
        }
    } // end setExtText

    void setCenterPanel(JPanel theCenterPanel) {
        add(theCenterPanel, BorderLayout.CENTER);
    }

    void setPhantomText(String theText) {
        phantomText = theText;
    }


    public void setSubject(String newSubject) {
        if (newSubject == null) mySubject = theDefaultSubject;
        else mySubject = newSubject.trim();
        if(subjectComponent instanceof JComboBox) {
            addSubject(mySubject);  // Makes this the first choice in the chooser.
            subjectChooser.setSelectedItem(mySubject);
        } else {
            ((JTextField) subjectComponent).setText(newSubject);
        }
    } // end setSubject


    // This is called by preClosePanel() when the panel closes.
    // There should only ever be one unsaved Panel (with the same defaultSubject) at a time.
    void saveSubjects() {
        MemoryBank.debug("Saving subjects: " + subjectsChanged);
        if(subjectsChanged) {
            boolean b = MemoryBank.dataAccessor.saveSubjects(theDefaultSubject, subjects);
            if(b) subjectsChanged = false;
        }
    } // end saveSubjects

} // end class ExtendedNoteComponent

