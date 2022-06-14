import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Vector;

import static javax.swing.JOptionPane.PLAIN_MESSAGE;

public class RichNoteDataEditor {
    private static final int maxSubjects = 20;

    JPanel theMainPanel;
    JComboBox<String> subjectChooser;
    JTextPane textPane;
    StyledDocumentData sdd;

    JButton normalBtn = new JButton("Plain");
    JButton boldBtn = new JButton("Bold");
    JButton italicBtn = new JButton("Italic");
    JButton underlineBtn = new JButton("Underline");
    JButton leftBtn = new JButton("Left Justification");
    JButton centerBtn = new JButton("Center Justification");
    JButton rightBtn = new JButton("Right Justification");
    JButton fullBtn = new JButton("Full Justification");
    JButton foregroundBtn = new JButton("Foreground Color");
    JButton backgroundBtn = new JButton("Background Color");
    JButton superscriptBtn = new JButton("Superscript");
    JButton subscriptBtn = new JButton("Subscript");
    JButton iconBtn = new JButton("Icon");
    JButton clearBtn = new JButton("Clear");

    JButton saveBtn = new JButton("Save");
    JButton cancelBtn = new JButton("Cancel");
    JButton plainEditorBtn = new JButton("Plain Text Editor");

    JComponent subjectComponent; // Edit the Subject with either a JTextField or a JComboBox
    String phantomText;  // Leave this null, if not being used.

    private Vector<String> subjects;
    private String mySubject;
    private final String theDefaultSubject;

    public RichNoteDataEditor(String defaultSubject) {
        if (defaultSubject != null) {
                subjects = MemoryBank.dataAccessor.loadSubjects(defaultSubject);

                subjectChooser = new JComboBox<>(subjects);
                subjectChooser.setEditable(true);
                subjectChooser.setFont(Font.decode("Serif-bold-12"));
                // Note: too large of font here causes display problems.

                subjectChooser.setToolTipText("Type or Select the Subject for this note");
                subjectChooser.setMaximumRowCount(maxSubjects);
                subjectComponent = subjectChooser;
        }
        theDefaultSubject = defaultSubject;

        textPane = new JTextPane();
        DefaultStyledDocument theDocument = (DefaultStyledDocument) textPane.getStyledDocument();
        addStylesToDocument(theDocument);
        buildPanel();

        String s1 = textPane.getText(); // This may have unwanted '\r' characters...
        String s2 = "";

        try {
            // ...and this will give us the same string without those chars, if possible.
            s2 = theDocument.getText(0, theDocument.getLength());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        //System.out.println("s1: " + s1);
        //System.out.println("s2: [" + s2 + "]");
        System.out.println("JTextPane text length: " + s1.length() + "\tStyledDocument text length: " + s2.length());
    } // end constructor

    private void buildPanel() {
        theMainPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = this.getButtonPanel();
        theMainPanel.add(buttonPanel, BorderLayout.NORTH);

        JScrollPane jsp = new JScrollPane();
        jsp.setViewportView(textPane);
        // Vertical and Horizontal scrollbars as needed but the JTextPane
        //   will wrap text, so the horizontal bar should never appear.

        theMainPanel.add(jsp, BorderLayout.CENTER);
    }

    private JPanel getButtonPanel() {
        iconifyButton(underlineBtn, "images/underline.gif");
        iconifyButton(boldBtn, "images/bold.gif");
        iconifyButton(italicBtn, "images/italic.gif");
        iconifyButton(leftBtn, "images/jleft.gif");
        iconifyButton(centerBtn, "images/jcenter.gif");
        iconifyButton(rightBtn, "images/jright.gif");
        iconifyButton(fullBtn, "images/jfull.gif");
        iconifyButton(superscriptBtn, "images/superscript.gif");
        iconifyButton(subscriptBtn, "images/subscript.gif");
        iconifyButton(foregroundBtn, "images/foreground.gif");
        iconifyButton(backgroundBtn, "images/background.gif");

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JPanel buttonRowN = new JPanel();
        JPanel buttonRowS = new JPanel();
        if(subjectChooser != null) buttonRowN.add(subjectChooser);
        buttonRowN.add(normalBtn);
        buttonRowN.add(boldBtn);
        buttonRowN.add(italicBtn);
        buttonRowN.add(underlineBtn);
        buttonRowN.add(leftBtn);
        buttonRowN.add(centerBtn);
        buttonRowN.add(rightBtn);
        buttonRowN.add(fullBtn);
        buttonRowN.add(superscriptBtn);
        buttonRowN.add(subscriptBtn);
        buttonRowN.add(foregroundBtn);
        buttonRowN.add(backgroundBtn);
        buttonRowN.add(iconBtn);
        buttonRowN.add(clearBtn);

        normalBtn.setToolTipText("Remove any extra text attributes from selection");
        iconBtn.setToolTipText("Insert an icon of your choosing");
        clearBtn.setToolTipText("Remove all content from JTextPane");
        saveBtn.setToolTipText("Doc-->Data, Reload");
        buttonRowS.add(saveBtn);
        cancelBtn.setToolTipText("Revert to the original content");
        buttonRowS.add(cancelBtn);
        plainEditorBtn.setToolTipText("Remove all text styling");
        buttonRowS.add(plainEditorBtn);

        buttonPanel.add(buttonRowN, BorderLayout.NORTH);
        buttonPanel.add(buttonRowS, BorderLayout.SOUTH);

        // Add ActionListeners to buttons
        normalBtn.addActionListener(e -> setNewStyle("plain", true));
        boldBtn.addActionListener(e -> setNewStyle("bold", true));
        italicBtn.addActionListener(e -> setNewStyle("italic", true));
        underlineBtn.addActionListener(e -> setNewStyle("underline", true));
        leftBtn.addActionListener(e -> setNewStyle("left", false));
        centerBtn.addActionListener(e -> setNewStyle("center", false));
        rightBtn.addActionListener(e -> setNewStyle("right", false));
        fullBtn.addActionListener(e -> setNewStyle("full", false));
        superscriptBtn.addActionListener(e -> setNewStyle("superscript", true));
        subscriptBtn.addActionListener(e -> setNewStyle("subscript", true));

        foregroundBtn.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(
                    textPane,
                    "Choose New Color",
                    textPane.getForeground());

            if (newColor != null) {
                // The document may have changed since initial construction; need to reacquire each time.
                DefaultStyledDocument dsd = (DefaultStyledDocument) textPane.getStyledDocument();

                System.out.println(newColor);
                Style newStyle;
                String newStyleName = "userColorStyle";

                // Create a new Style for this color selection -
                int start = textPane.getSelectionStart();
                int end = textPane.getSelectionEnd();
                int caretPosition = textPane.getCaretPosition();
                System.out.println("Selection - Start: " + start + "\tEnd: " + end + "\t\tCaret Position: " + caretPosition);
                System.out.println("-------");
                Style paragraphStyle = dsd.getLogicalStyle(caretPosition);
                System.out.println("Paragraph Style:");
                showAttributes("", "", paragraphStyle);
                System.out.println("-------");

                System.out.println("New Color Style:");
                newStyle = dsd.addStyle(newStyleName, paragraphStyle);
                StyleConstants.setForeground(newStyle, newColor);
                setNewStyle(newStyleName, true);
                showAttributes("", "", newStyle);
            }
        });

        backgroundBtn.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(
                    textPane,
                    "Choose New Color",
                    textPane.getBackground());

            if (newColor != null) {
                // The document may have changed since initial construction; need to reacquire each time.
                DefaultStyledDocument dsd = (DefaultStyledDocument) textPane.getStyledDocument();

                System.out.println(newColor);
                Style newStyle;
                String newStyleName = "userColorStyle";

                // Create a new Style for this color selection -
                int start = textPane.getSelectionStart();
                int end = textPane.getSelectionEnd();
                int caretPosition = textPane.getCaretPosition();
                System.out.println("Selection - Start: " + start + "\tEnd: " + end + "\t\tCaret Position: " + caretPosition);
                System.out.println("-------");
                Style paragraphStyle = dsd.getLogicalStyle(caretPosition);
                System.out.println("Paragraph Style:");
                showAttributes("", "", paragraphStyle);
                System.out.println("-------");

                System.out.println("New Color Style:");
                newStyle = dsd.addStyle(newStyleName, paragraphStyle);
                StyleConstants.setBackground(newStyle, newColor);
                setNewStyle(newStyleName, true);
                showAttributes("", "", newStyle);
            }
        });

        clearBtn.setForeground(Color.RED);
        clearBtn.addActionListener(e -> {
            // This clears the document of both styles and text.
            // To clear just the text, either do a document.remove() or a textpane.setText("")
            // To clear just the styles, you could get the text, clear all (as below), then setText().
            textPane.setDocument(new DefaultStyledDocument());
        });

        saveBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                collectDocData();  // null out the sdd, then fill it with data, if there is any.

                System.out.println("================================================================");
                System.out.println("JSON representation of the StyledDocumentData: ");
                System.out.println(AppUtil.toJsonString(sdd));

                DefaultStyledDocument newDoc = new DefaultStyledDocument();
                sdd.fillStyledDocument(newDoc);
                textPane.setDocument(newDoc);
                RichNoteDataEditor.this.addStylesToDocument(newDoc);
            }
        });
        cancelBtn.addActionListener(e -> { // Get back to the original content.
            DefaultStyledDocument dsd =new DefaultStyledDocument();
            addStylesToDocument(dsd);
            textPane.setDocument(dsd);
        });

        return buttonPanel;
    } // end getButtonPanel

    void collectDocData() {
        sdd = null;
        String theString = textPane.getText();
        if (theString == null || theString.trim().isEmpty()) return;
        System.out.println(theString);
        System.out.println();
        sdd = new StyledDocumentData();

        // Get the section of the document containing the text (and styling) that we need to preserve.
        Element theSection = textPane.getStyledDocument().getDefaultRootElement();
        System.out.println("The StyledDocument components starting with the DefaultRootElement - ");
        RichNoteDataEditor.this.expandElement(theSection, 1);
    }

    // Given an Element of a Document, show its content, recursively as needed.
    private void expandElement(Element theElement, int level) {
        ParagraphData newParagraphData = null;
        AttributeSet attributes;
        int attributeCount;
        int startOffset, endOffset;
        int i; // loop indexing, reused.
        String theTextFragment, printableTextFragment;
        String indent = "      ".repeat(level - 1); // When level is '1', the indent is "".

        // Get the text from the document, into a String whose indexes will match those reported by theElement.
        int docLength = textPane.getStyledDocument().getLength();
        String theString = textPane.getText(); // This may have unwanted '\r' characters.
        try { // and this will give us the same string without those chars, if possible.
            theString = textPane.getStyledDocument().getText(0, docLength);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println(toJsonString(theDoc)); // No, jackson cannot do this.

        //  Get the Element name and its other stats
        String elementName = theElement.getName();
        int elementCount = theElement.getElementCount();
        attributes = theElement.getAttributes();
        attributeCount = attributes.getAttributeCount();
        startOffset = theElement.getStartOffset();
        endOffset = theElement.getEndOffset();
        if (endOffset > docLength) endOffset = docLength;

        // Make a report of theElement at this Level -
        System.out.print(indent + "Level " + level + "\t\t\tName: " + elementName);
        System.out.print("\t\tStart: " + startOffset);
        System.out.print("\tEnd: " + endOffset);
        System.out.print("\t\tAttribute Count: " + attributeCount);
        System.out.println("\t\telementCount: " + elementCount);
        theTextFragment = theString.substring(startOffset, endOffset);
        printableTextFragment = theTextFragment.replaceAll("\\n", "\\\\n");
        System.out.println(indent + elementName + " Text Fragment: [" + printableTextFragment + "]");

        if(elementName.equals("paragraph")) {
            newParagraphData = new ParagraphData(theTextFragment);
            sdd.addParagraph(newParagraphData);
        }

        if (attributeCount > 0) {
            //System.out.println(toJsonString(attributes)); // No - infinite recursion.
            showAttributes(indent, elementName, attributes);
            assert newParagraphData != null;
            newParagraphData.paragraphAttributeData = AttributeData.getAttributeDataVector(attributes);
            System.out.println();
        }

        // If the Element itself has Elements, report on those, and recurse if not isLeaf().
        for (i = 0; i < elementCount; i++) {
            Element childElement = theElement.getElement(i);
            String childElementName = childElement.getName();
            startOffset = childElement.getStartOffset();
            endOffset = childElement.getEndOffset();
            if (endOffset > docLength) endOffset = docLength;
            attributes = childElement.getAttributes();
            attributeCount = attributes.getAttributeCount();

            if(childElement.isLeaf()) {
                System.out.print(indent + elementName + " Element " + i + " Name: " + childElementName);
                System.out.print("\tStart: " + startOffset);
                System.out.print("\tEnd: " + endOffset);
                System.out.print("\tAttribute Count: " + attributeCount);
                System.out.println("\tElementCount: " + childElement.getElementCount());

                // Determine the associated document text segment
                theTextFragment = theString.substring(startOffset, endOffset);
                printableTextFragment = theTextFragment.replaceAll("\\n", "\\\\n");
                System.out.println(indent + childElementName + " Text Fragment: [" + printableTextFragment + "]");

                ContentData newContent = new ContentData(theTextFragment);
                assert newParagraphData != null;
                newParagraphData.contentData.add(newContent);

                if (attributeCount > 0) {
                    showAttributes(indent, childElementName, attributes);
                    newContent.contentAttributeData = AttributeData.getAttributeDataVector(attributes);
                }
                System.out.println(); // separator
            } else {
                expandElement(childElement, level + 1);
            }
        } // end for elementCount
    } // end expandElement

    // Initialize some styles and add them to the text pane for later use.
    protected void addStylesToDocument(StyledDocument doc) {
        Style s; // Reused during creations, unique names will be assigned.

        // Get the default style; It is referenced by a standardized enum to be sure that we get it,
        // but it is probably 'default'.  No need to add this one to the doc but it is needed as
        // the parent of our own default/base style.
        StyleContext sc = StyleContext.getDefaultStyleContext();
        Style defaultContextStyle = sc.getStyle(StyleContext.DEFAULT_STYLE);

        // Now make and add our own default, that we will call 'plain'.
        // This will be the parent of all the other styles to follow.
        Style plain = doc.addStyle("plain", defaultContextStyle);
        StyleConstants.setFontFamily(plain, "SansSerif");
        StyleConstants.setFontSize(plain, 16);

        // Create a bold style
        s = doc.addStyle("bold", plain);
        StyleConstants.setBold(s, true);

        // Create an italic style
        s = doc.addStyle("italic", plain);
        StyleConstants.setItalic(s, true);

        // Create an underline style
        s = doc.addStyle("underline", plain);
        StyleConstants.setUnderline(s, true);

        // Create superscript and subscript styles
        s = doc.addStyle("superscript", plain);
        StyleConstants.setSuperscript(s, true);
        s = doc.addStyle("subscript", plain);
        StyleConstants.setSubscript(s, true);

        // Create paragraph alignment styles
        s = doc.addStyle("left", plain);
        StyleConstants.setAlignment(s, StyleConstants.ALIGN_LEFT);
        s = doc.addStyle("center", plain);
        StyleConstants.setAlignment(s, StyleConstants.ALIGN_CENTER);
        s = doc.addStyle("right", plain);
        StyleConstants.setAlignment(s, StyleConstants.ALIGN_RIGHT);
        s = doc.addStyle("full", plain);
        StyleConstants.setAlignment(s, StyleConstants.ALIGN_JUSTIFIED);

    } // end addStylesToDocument


    // Show the attributes of an AttributeSet.
    // In this case, the getAttribute() method takes an Object that it gets from the enumeration.
    private void showAttributes(String indent, String owner, AttributeSet attributes) {
        Enumeration<?> enumeration = attributes.getAttributeNames();
        String maybeOneSpace = owner.isEmpty() ? "" : " ";
        while (enumeration.hasMoreElements()) {
            // The Elements in this enumeration are not just String names; they are Objects that act as keys
            //   to access the Attribute.  They might be of type String, but they might be something else.
            //   Besides Strings, one common/expected type is: StyleContext.NamedStyle which can be tested
            //     for equality with StyleConstants.NameAttribute or StyleConstants.ResolveAttribute.
            Object attributeName = enumeration.nextElement();
            Object theAttribute = attributes.getAttribute(attributeName);
            //System.out.println(toJsonString(theAttribute)); // No - Index 2000 out of bounds for length 2000
            //System.out.println(indent + "   Class of the Attribute: " + theAttribute.getClass().getName());
            System.out.print(indent + owner + maybeOneSpace + "Attribute: " + attributeName);
            if(theAttribute instanceof StyleContext.NamedStyle) {
                System.out.println("\t\tParent Style: " + ((StyleContext.NamedStyle) theAttribute).getName());
            } else {
                System.out.println("\t\tValue: " + theAttribute);
            }
        }
    } // end showAttributes

    private void setNewStyle(String styleName, boolean isCharacterStyle) {
        StyledDocument document = textPane.getStyledDocument();
        Style newStyle = document.getStyle(styleName);
        int start = textPane.getSelectionStart();
        int end = textPane.getSelectionEnd();
        if (isCharacterStyle) {
            boolean replaceOld = styleName.equals("plain");
            document.setCharacterAttributes(start, end - start,
                    newStyle, replaceOld);
        } else {
            document.setParagraphAttributes(start,
                    end - start,
                    newStyle, false);
        }
    }

    private void iconifyButton(JButton theButton, String iconPath) {
        ImageIcon buttonIconImage;
        buttonIconImage = StyledDocumentData.createImageIcon(iconPath);
        if (buttonIconImage != null) {
            IconInfo.scaleIcon(buttonIconImage, 16,16);
            theButton.setIcon(buttonIconImage);
            theButton.setToolTipText(theButton.getText());
            theButton.setText(null);
            Dimension d = theButton.getPreferredSize();
            theButton.setPreferredSize(new Dimension(20, d.height));
        }
    }

    public static void main(String[] args) {
        // Needed to override the 'metal' L&F for Swing components.
        String thePlaf = "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel";
        System.out.println("Setting Pluggable Look & Feel to: " + thePlaf);
        try {
            UIManager.setLookAndFeel(thePlaf);
        } catch (Exception ignored) {
        }    // end try/catch

        NoteData nd = new NoteData();
        nd.noteString = "A test noteString";
        nd.extendedNoteString = "A test (undecorated) extendedNoteString";
        nd.subjectString = "Day Note";
        RichNoteDataEditor rte = new RichNoteDataEditor(nd.getSubjectString());
        rte.theMainPanel.setPreferredSize(new Dimension(780, 500));
        rte.textPane.setText(nd.extendedNoteString);

        String string1 = "Save";               // 0   (value of the 'doit' int; OK_OPTION)
        String string2 = "Cancel";             // 1   (CANCEL_OPTION or CLOSED_OPTION)
        String string3 = "Plain Text Editor";  // 2   (home-grown)
        Object[] options = {string1, string2, string3};
        int doit = JOptionPane.showOptionDialog(null,
                rte.theMainPanel,
                nd.getNoteString(),
                JOptionPane.YES_NO_CANCEL_OPTION,
                PLAIN_MESSAGE,
                null,     //don't use a custom Icon
                options,  //the titles of buttons
                string1); //the title of the default button

        if(doit == JOptionPane.OK_OPTION) { // Save
            rte.collectDocData();  // null out the sdd, then fill it with data, if there is any.
            if(rte.sdd != null) {

                System.out.println("================================================================");
                System.out.println("JSON representation of the StyledDocumentData: ");
                System.out.println(AppUtil.toJsonString(rte.sdd));

                DefaultStyledDocument newDoc = new DefaultStyledDocument();
                rte.sdd.fillStyledDocument(newDoc);
                rte.textPane.setDocument(newDoc);
                rte.addStylesToDocument(newDoc);
            }
        }

    } // end main

} // end class RichTextEditor
