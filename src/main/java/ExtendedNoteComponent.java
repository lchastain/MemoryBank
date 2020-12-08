
/*
 A pairing of a TextArea that contains a 'note', with
 a ComboBox that shows the note's 'Subject'.  Subjects are loaded
 and managed at this level, although they can be set from a calling
 context, to any value including one not in the list.
*/

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
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
    private String subjectsFilename;

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
//        this.add(scrollPane);   // disabled, suspected extraneous.  Otherwise re-enable and add a comment as to why.

        if (defaultSubject != null) {
            if (defaultSubject.equals("Goal Title")) {
                subjectComponent = new JTextField();
                subjectComponent.setFont(Font.decode("Serif-bold-12"));
                add(subjectComponent, "North");
            } else if (defaultSubject.equals("Search Info")) {
                subjectComponent = new JTextField(32);
//                int height = subjectComponent.getPreferredSize().height;
//                subjectComponent.setMaximumSize(new Dimension(20, height));
//                subjectComponent.setPreferredSize(new Dimension(20, height));
                JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5, 0));
                JLabel titleLabel = new JLabel("    Search Title: ");
                titleLabel.setFont(Font.decode("Serif-bold-12"));
//                titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
                titlePanel.add(titleLabel);
                titlePanel.add(subjectComponent);
                subjectComponent.setFont(Font.decode("Serif-bold-12"));
                add(titlePanel, "North");
            } else {
                // Develop the file name of the Subjects from the default
                //   subject that was the input parameter, by adding the
                //   word 'Subjects' after the first space, if any.
                subjects = new Vector<>(6, 1);
                int space = defaultSubject.indexOf(" ");
                String s;
                if (space > -1) s = defaultSubject.substring(0, space);
                else s = defaultSubject;
                s += "Subjects.json";
                subjectsFilename = MemoryBank.userDataHome + File.separatorChar + s;

                loadSubjects(); // There may or may not be any.
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
        add(scrollPane, "Center");
    } // end constructor


    void addSubject(String s) {
        MemoryBank.debug("Adding subject: [" + s + "]");
        //------------------------------------------------------------------
        // Do not want to add the subject to the file in these cases.
        //------------------------------------------------------------------
        if (s.equals("")) return;
        if (s.equals(theDefaultSubject)) return;

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


    // This could be made static, IF you made 'subjects' into a return value.
    private void loadSubjects() {
        Exception e = null;

        try {
            String text = FileUtils.readFileToString(new File(subjectsFilename), StandardCharsets.UTF_8.name());
            Object theObject;
            theObject = AppUtil.mapper.readValue(text, Object.class);
            subjects = AppUtil.mapper.convertValue(theObject, new TypeReference<Vector<String>>() { });
            System.out.println("Subjects from JSON file: " + AppUtil.toJsonString(subjects));
        } catch (FileNotFoundException fnfe) {
            // not a problem; use defaults.
            MemoryBank.debug("Subjects not found.  Will create a new list, if needed.");
        } catch (IOException ioe) {
            e = ioe;
            e.printStackTrace();
        }

        if (e != null) {
            String ems = "Error in loading " + subjectsFilename + " !\n";
            ems = ems + e.toString();
            ems = ems + "\noperation failed; using default values.";
            MemoryBank.debug(ems);
        } // end if
    } // end loadSubjects

    // This needs to be called from a higher context
    void saveSubjects() {
        MemoryBank.debug("Saving subjects: " + subjectsChanged);
        if (!subjectsChanged) return;
        MemoryBank.debug("Saving subject data in " + subjectsFilename);

        try (FileWriter writer = new FileWriter(subjectsFilename);
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(AppUtil.toJsonString(subjects));
            bw.flush();
        } catch (IOException ioe) {
            String ems = ioe.getMessage();
            ems = ems + "\nSubjects save operation aborted.";
            MemoryBank.debug(ems);
        } // end try/catch
    } // end saveSubjects

} // end class ExtendedNoteComponent

