
/*
 A pairing of a TextArea that contains a 'note', with
 a ComboBox that shows the note's 'Subject'.  Subjects are loaded
 and managed at this level, although they can be set from a calling
 context, to any value including one not in the list.
*/

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Vector;

public class ExtendedNoteComponent extends JPanel {
    private static final long serialVersionUID = 2901865483988763694L;

    private static final int maxSubjects = 20;

    // Laid out differently in extended classes.
    protected JComboBox<String> subjectChooser;
    protected JTextArea body;

    private Vector<String> subjects;
    private String initialSubject;
    private String mySubject;
    private String theDefaultSubject;
    private String FileName;

    // This flag is reset to false when subjects are saved.
    private boolean subjectsChanged = false;

    public ExtendedNoteComponent(String defaultSubject) {
        super(new BorderLayout());
        subjects = new Vector<>(6, 1);
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

        // Develop the file name of the Subjects from the default
        //   subject that was the input parameter, by adding the
        //   word 'Subjects' after the first space, if any.
        int space = defaultSubject.indexOf(" ");
        String s;
        if (space > -1) s = defaultSubject.substring(0, space);
        else s = defaultSubject;
        s += "Subjects";
        FileName = MemoryBank.userDataHome + File.separatorChar + s;

        loadSubjects(); // There may or may not be any.
        subjectChooser = new JComboBox<>(subjects);
        subjectChooser.setEditable(true);
        subjectChooser.setFont(Font.decode("Serif-bold-12"));
        // Note: too large of font here causes display problems.

        subjectChooser.setToolTipText("Type or Select the Subject for this note");
        subjectChooser.setMaximumRowCount(maxSubjects);

        theDefaultSubject = defaultSubject;
        add(subjectChooser, "North");
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


    // This is needed to acquire typed text.
    // Called from a higher context because there are no locally
    //   handled events that are relevant.
    void checkSubject() {
        String theSubject = (String) subjectChooser.getEditor().getItem();
        // Note: Cannot use '.getSelectedItem()' above, because it may have
        //   just been typed in and not be in the list, therefore it is not
        //   the selected item.

        MemoryBank.debug("checkSubject " + theSubject);
        if (theSubject == null) return;
        else theSubject = theSubject.trim();

        mySubject = theSubject;

        addSubject(mySubject);  // Move to the top of the list.
    } // end checkSubject


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


    //-------------------------------------------------------------
    // Method Name: loadSubjects
    //
    //-------------------------------------------------------------
    private void loadSubjects() {
        Exception e = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        String subj;

        MemoryBank.debug("Loading subjects file: " + FileName);

        try {
            fis = new FileInputStream(FileName);
            ois = new ObjectInputStream(fis);

            while (true) {  // The expected exit is via EOFException
                subj = (String) ois.readObject();
                if(subj == null) break; // Added this line to avoid an IJ complaint about 'while'
                subjects.addElement(subj);
                // MemoryBank.debug("  loaded subject: " + subj);
            } // end while
        } catch (FileNotFoundException fnfe) {
            // not a problem; expected (very) first time for each user.
        } catch (EOFException eofe) {
            // System.out.println("End of file reached!");
            try {
                if (null != ois) ois.close();
                fis.close();
            } catch (IOException ioe) {   // This one's a throw-away.
                ioe.printStackTrace(); // not handled but not (entirely) ignored...
            } // end try/catch
        } catch (ClassCastException | ClassNotFoundException | IOException eee) {
            e = eee;
        } // end try/catch

        if (e != null) {
            String ems;
            ems = "Error in loading " + FileName + " !\n";
            ems = ems + e.toString();
            ems = ems + "\n'Subjects' load operation aborted.";
            JOptionPane.showMessageDialog(new Frame(),
                    ems, "Error", JOptionPane.ERROR_MESSAGE);
        } // end if
    } // end loadSubjects


    // This needs to be called from a higher context
    void saveSubjects() {
        MemoryBank.debug("Saving subjects: " + subjectsChanged);
        if (!subjectsChanged) return;

        MemoryBank.debug("Saving subject data in " + FileName);
        try {
            FileOutputStream fos = new FileOutputStream(FileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            for (String subj : subjects) {
                oos.writeObject(subj);
            } // end for (my first usage of this new 'for loop' syntax!)

            oos.flush();
            oos.close();
            fos.close();
            subjectsChanged = false;
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        } // end try/catch
    } // end saveSubjects

} // end class ExtendedNoteComponent

