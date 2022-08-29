import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ExtendedNoteEditorMain {
    PlainNoteDataEditor plainNoteDataEditor;
    String defaultSubject = "a default subject";

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.userEmail = "g01@doughmain.net";

        String defaultSubject = "a default subject";
        NoteData noteData = new NoteData();
        noteData.subjectString = "This note's subject";
        noteData.extendedNoteString = "An extended note";
        //PlainNoteDataEditor plainNoteDataEditor = new PlainNoteDataEditor(defaultSubject);

        SubjectEditor subjectEditor = new SubjectEditor(defaultSubject);
        subjectEditor.setSubject(noteData.getSubjectString());
        PlainNoteDataEditor plainNoteDataEditor = new PlainNoteDataEditor(subjectEditor);

        plainNoteDataEditor.setExtendedNoteString(noteData.getExtendedNoteString());
        plainNoteDataEditor.subjectEditor.setSubject(noteData.getSubjectString());

        JFrame testFrame = new JFrame("ExtendedNoteEditor Driver");

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        testFrame.getContentPane().add(plainNoteDataEditor, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(480, 320));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
