
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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

public class ExtendedNoteComponent extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final int maxSubjects = 20;

    // Laid out differently in extended classes.
    protected JComboBox<String> subjectChooser;
    protected JTextArea body;

    private Vector<String> subjects;
    private String initialSubject;
    private String mySubject;
    private String theDefaultSubject;
    private String subjectsFilename;

    // This flag is reset to false when subjects are saved.
    private boolean subjectsChanged = false;

    public ExtendedNoteComponent(String defaultSubject) {
        super(new BorderLayout());
        body = new JTextArea(10, 20) {
            // To stop the compiler from whining -
            private static final long serialVersionUID = 2465964677978447062L;

            public void addNotify() {
                super.addNotify();
                requestFocusInWindow(); // wouldn't work from other places...
            } // end addNotify
        };
        // Note: The size of the JTextArea for body does not matter; it is
        //  placed into the center of a dialog with a borderlayout where it is
        //  stretched to fit the frame size, which is set according to the
        //  item's enWidth and enHeight in pixels, not text columns/rows.
        body.setLineWrap(true);
        body.setWrapStyleWord(true);

        if(defaultSubject != null) {
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
            add(subjectChooser, "North");
        }

        theDefaultSubject = defaultSubject;
        add(body, "Center");
    } // end constructor


    // Called by internal methods only.
    void addSubject(String s) {
        MemoryBank.debug("Adding subject: [" + s + "]");
        //------------------------------------------------------------------
        // Do not want to add the subject to the file in these cases.
        //------------------------------------------------------------------
        if (s.equals("")) return;
        if (s.equals(initialSubject)) return;
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
        String theSubject = (String) subjectChooser.getEditor().getItem();
        // Note: Cannot use '.getSelectedItem()' above, because it may have
        //   just been typed in and not be in the list, therefore it is not
        //   the selected item.

        MemoryBank.debug("updateSubject " + theSubject);
        if (theSubject == null) return;
        else theSubject = theSubject.trim();

        mySubject = theSubject;

        addSubject(mySubject);  // Move to the top of the list.
    } // end updateSubject


    String getExtText() {
        return body.getText();
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
        body.setText(s);
    } // end setExtText


    //-------------------------------------------------------------
    // Method Name: setSubject
    //
    //-------------------------------------------------------------
    public void setSubject(String s) {
        if (s == null) s = theDefaultSubject;
        else s = s.trim();
        addSubject(s);  // Makes this the first choice in the chooser.
        subjectChooser.setSelectedItem(s);
        initialSubject = s;
    } // end setSubject


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

