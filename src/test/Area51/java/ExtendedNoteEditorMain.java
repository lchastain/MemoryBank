import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ExtendedNoteEditorMain {
    ExtendedNoteComponent extendedNoteComponent;
    String defaultSubject = "a default subject";

    // See the note above the 'main0' method, below.  This method no longer used.
    boolean editExtendedNoteComponent(NoteData noteData) {
        // System.out.println("NoteGroup editExtendedNoteComponent");

        // Load the enc with the correct data
        if (extendedNoteComponent == null) {
            extendedNoteComponent = new ExtendedNoteComponent(defaultSubject);
        }

        extendedNoteComponent.setExtText(noteData.getExtendedNoteString());
        if (defaultSubject != null) extendedNoteComponent.setSubject(noteData.getSubjectString());

        // Preserve initial values, for later comparison to
        //   determine if there was a change.
        String origSubject = noteData.getSubjectString();
        String origExtendedString = noteData.getExtendedNoteString();

        //---------------------------------------------------------
        // Present the ExtendedNoteComponent in a dialog
        //---------------------------------------------------------
        int doit = JOptionPane.showConfirmDialog(
                JOptionPane.getFrameForComponent(null),
                extendedNoteComponent,
                noteData.getNoteString(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (doit == -1) return false; // The X on the dialog
        if (doit == JOptionPane.CANCEL_OPTION) return false;

        // Collect results of the editing -
        //------------------------------------------------------------------

        // Get the Subject
        extendedNoteComponent.updateSubject(); // This moves the subject from the combobox into the component data
        String newSubject = extendedNoteComponent.getSubject(); // This gets the component data
        // We need to be able to save a 'None' subject (ie, ""), and recall it,
        //   which is different than if you never set one in the
        //   first place, in which case you would get the default.  So -
        //   we accept the newSubject above without checking its content.

        // Get the Extended text
        String newExtendedString = extendedNoteComponent.getExtText();

        boolean aChangeWasMade = false;
        if (newSubject != null) {
            // Cannot simplify the logic here; either new or old could be null, which is an allowed value.
            if (origSubject == null) aChangeWasMade = true;
            else if (!newSubject.equals(origSubject)) aChangeWasMade = true;
        } // end if
        if (!newExtendedString.equals(origExtendedString)) aChangeWasMade = true;

        if (aChangeWasMade) {
            noteData.setExtendedNoteString(newExtendedString);
            noteData.setSubjectString(newSubject);
        } // end if

        //------------------------------------------------------------------

        return aChangeWasMade;
    } // end editExtendedNoteComponent

    // Not used, now.  This version utilized a copy of the edit method taken from NoteGroup, but requires
    // this class to be instantiated, which is not in line with the convention that is set for all other
    // 'main' type drivers.  The complicating factor here is that there IS no separate class that is an
    // editor for the extended note, so both NoteGroup and this class need to initialize and display the
    // component as though it was a complete, separate class instance.  Keeping this one for reference, only.
    public static void main0(String[] args) {
        ExtendedNoteEditorMain me = new ExtendedNoteEditorMain();
        NoteData noteData = new NoteData();
        noteData.extendedNoteString = "An extended note";
        me.editExtendedNoteComponent(noteData);
    }

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("g01@doughmain.net");

        String defaultSubject = "a default subject";
        NoteData noteData = new NoteData();
        noteData.subjectString = "This note's subject";
        noteData.extendedNoteString = "An extended note";
        ExtendedNoteComponent extendedNoteComponent = new ExtendedNoteComponent(defaultSubject);
        extendedNoteComponent.setExtText(noteData.getExtendedNoteString());
        extendedNoteComponent.setSubject(noteData.getSubjectString());

        JFrame testFrame = new JFrame("ExtendedNoteEditor Driver");

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        testFrame.getContentPane().add(extendedNoteComponent, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(480, 320));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }


}
