
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

public class PlainNoteDataEditor extends JPanel {
    private static final long serialVersionUID = 1L;

    // This Panel can have variable content; default declarations below.
    SubjectEditor subjectEditor;
    JComboBox<String> subjectChooser;
    protected JTextArea body;  // held/shown in a JScrollPane

    String phantomText;  // Leave this null, if not being used.

    PlainNoteDataEditor(SubjectEditor subjectEditor) {
        super(new BorderLayout());
        this.subjectEditor = subjectEditor;
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
        if(subjectEditor != null) {
            add(subjectEditor, BorderLayout.NORTH);
        }

        add(scrollPane, BorderLayout.CENTER);
    } // end constructor

    String getExtText() {
        String theText = body.getText();
        if(theText.equals(phantomText)) return "";
        return theText;
    }

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

} // end class PlainNoteDataEditor

