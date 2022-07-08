import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class SubjectEditor extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int maxSubjects = 20;

    JButton setDefaultBtn; // This allows a subject to be 'un'-set.
    JComponent subjectComponent; // Will be either a JTextField or a JComboBox

    private Vector<String> subjects;
    private String mySubject;
    private final String theDefaultSubject;

    // This flag is reset to false when subjects are saved.
    private boolean subjectsChanged = false;

    SubjectEditor(@NotNull String defaultSubject) {
        super(new BorderLayout());
        if (defaultSubject.equals("Goal Title")) {
            subjectComponent = new JTextField();
            subjectComponent.setFont(Font.decode("Serif-bold-12"));
            add(subjectComponent, BorderLayout.NORTH);
        } else { // Get the candidate Subjects, possibly just a default list.
            subjects = MemoryBank.dataAccessor.loadSubjects(defaultSubject);

            subjectComponent = new JComboBox<>(subjects);
            subjectComponent.setToolTipText("Type or Select the Subject for this note");
            subjectComponent.setFont(Font.decode("Serif-bold-12")); // Note: font too large == display problems.
            ((JComboBox<?>) subjectComponent).setEditable(true);
            ((JComboBox<?>) subjectComponent).setMaximumRowCount(maxSubjects);
            setDefaultBtn = new JButton("X");
            setDefaultBtn.addActionListener(e -> {
                ((JComboBox<?>) subjectComponent).setSelectedItem(defaultSubject);
                setDefaultBtn.setVisible(false); // No need for it, after clicked once.
            });
            setDefaultBtn.setToolTipText("Revert to default");
            add(setDefaultBtn, BorderLayout.WEST);
            add(subjectComponent, BorderLayout.CENTER);
        }
        if(subjectComponent instanceof JComboBox) {
            ((JComboBox<?>) subjectComponent).addItemListener(e -> {
                if(setDefaultBtn != null) setDefaultBtn.setVisible(true);
            });
        }
        theDefaultSubject = defaultSubject;
    } // end constructor

    void addSubject(String s) {
        // We do not want to add the subject in these cases -
        if (s.equals("")) return;
        if (s.equals(theDefaultSubject)) return;

        MemoryBank.debug("Adding subject: [" + s + "]");

        // Check to see if this subject is already first in the list -
        if (subjects.size() > 0) {
            if ((subjects.elementAt(0)).equals(s)) return;
        } // end if

        subjects.remove(s); // Remove this subject (if found) from lower in the list.
        subjects.insertElementAt(s, 0); // Put this subject at the top of the list.

        // If the list has grown too big, truncate.
        if (subjects.size() > maxSubjects) {
            subjects.remove(subjects.lastElement());
        } // end if too many

        subjectsChanged = true;
        // System.out.println("subjects size: " + subjects.size());
    } // end addSubject


    public String getSubject() {
        updateSubject();
        if (mySubject == null) return null;
        if (mySubject.equals(theDefaultSubject)) return null;
        return mySubject;
    } // end getSubject


    int getSubjectCount() {
        return subjects.size();
    } // end getSubjectCount

    // This is called by preClosePanel() when the panel closes.
    // There should only ever be one unsaved Panel (with the same defaultSubject) at a time.
    void saveSubjects() {
        MemoryBank.debug("Saving subjects: " + subjectsChanged);
        if(subjectsChanged) {
            boolean b = MemoryBank.dataAccessor.saveSubjects(theDefaultSubject, subjects);
            if(b) subjectsChanged = false;
        }
    } // end saveSubjects

    public void setSubject(String newSubject) {
        // When there IS no subject set yet, this line sets it to the default.
        if (newSubject == null) mySubject = theDefaultSubject;
        else mySubject = newSubject.trim();
        if(subjectComponent instanceof JComboBox) {
            addSubject(mySubject);  // Makes this the first choice in the chooser.
            ((JComboBox<?>) subjectComponent).setSelectedItem(mySubject);
        } else {
            ((JTextField) subjectComponent).setText(newSubject);
        }
        if(setDefaultBtn != null) setDefaultBtn.setVisible(newSubject != null);

    } // end setSubject

    // This is needed to acquire text that might be typed into the Subject combobox control.
    // Needs to be called from a NoteGroup during an Edit session,
    // because there is nothing in place to do this automatically as the result of an awt or
    // swing event.  There could be, of course, but I opted to NOT go that route because it
    // would be called for every single keypress and dropdown selection, whereas this one is
    // only needed once, at the end of the edit session.
    void updateSubject() {
        if(subjectComponent == null) return;  // Not all NoteData children will allow Subject editing.

        if(subjectComponent instanceof JComboBox) {
            String theSubject = (String) ((JComboBox<?>) subjectComponent).getEditor().getItem();
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

}
