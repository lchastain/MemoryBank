import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;

// Much T&E was done here, to determine how to persist the various parts of a StyledDocument in a JSON string.
// Needed because due to infinite recursion or bounds checking, the jackson library is unable to serialize these
//   classes:  DefaultStyledDocument, Element, Style, AttributeSet, Attribute, Enumeration of AttributeNames.

// My own understanding and findings:
// A Document is a hierarchy of Elements.  An Element can have children that are Elements.  The relevant
//   Element names for this application are 'paragraph' and 'content'; apparently auto-assigned; I didn't provide
//   these names.  Names are not unique; there can be more than one with the same name.  The name also seems to
//   track along with the isLeaf() method, where a paragraph has children and is not a leaf, and content is a
//   leaf and does not have children.
// A Style is effectively just an AttributeSet with a name.
// A StyledDocument has a list of associated Styles (but you cannot retrieve that list; you can only 'get' a Style
//   individually, by its name).  The DefaultStyledDocument provides a method whereby you can get an Enumeration of the
//   list of style names as defined in the document.  The 'names' in this list are Objects (not String).
// The examples I was working from were adding all their styles to the document upfront during their construction, and
//   then later using the name known to the doc to apply to the text.  But I had the intent that I would be allowing
//   all available Colors and Fonts, meaning that doing it that way would be impractical.  So I mistakenly thought that
//   if I dynamically made a new Style 'from scratch' that it could be done outside of the document and immediately
//   applied directly to the text, without having to grow the document's list of Styles.  But I had a long and
//   frustrating sidetrack trying to do that, because there is no constructor for a Style; it is an interface, not a
//   class.  Looking around, I only found one other swing-appropriate class that implements Style. Internal to
//   StyleContext is NamedStyle.  It took a lot of T&E but I finally was able to use it to create a 'standalone' Style
//   but then I saw that its 'default' parent Style was not necessarily the same as the one from the TextPane's
//   document where it was to be applied, and finally came to realize that getting the Style by adding it to the
//   document really was the best way and it didn't have to happen until the new Style was needed.
// A sidetrack to that sidetrack was that according to the docs for the Style.getName() method, the name may be null.
//   But if the name is null then how would you 'get' it?  And how do you even have a Style with no name in the first
//   place?  The only way to create a Style in a document is to give it a name.  The Style that you can get from a
//   StyleContext appears to be able to make an internal private Style with no name, but I haven't found a way to get
//   it out into a discrete Style-implementing instance variable where using it to call getName() would return null.
//   Eventually dropped that line of inquiry as being too far off-center from the overall goals.
// The root Element of the document starts with a paragraph.  A linefeed in the text appears to create a new paragraph.
// Elements describe a portion of the doc and have an associated AttributeSet which can be retrieved but is not
//   referenced as a discrete Style although its grouped Attributes may match the ones in a defined Style.
// Content attributes can override paragraph attributes.
// Although an Element's attribute set may contain a StyleConstants.NameAttribute, this Attribute cannot be relied
//   upon to accurately track back to one of the Styles in the Document's Style list, because for one thing there
//   is no requirement for different Styles to have unique names; Styles are added to documents with no name checking.
//   On top of that, when more than one named Style is additively applied to the same text, the attribute set for that
//   Element will only have the NameAttribute of the last Style that was applied, overwriting the names of any that
//   came earlier.  However, the rest of the attributes in the Element's AttributeSet will still be individually
//   complete and accurate to properly decorate the indicated text fragment.  Therefore the StyleConstants.NameAttribute
//   should not be used for identification of the Element's AttributeSet; at best it is just the name of the last Style
//   that was applied but if there was more than one Style applied to the indicated text fragment then this 'name'
//   could lead you down the wrong path.  Better to just ignore it, in favor of attribute-by-attribute examination of
//   the contents of the AttributeSet.
// The new Styles that I might create during a run of the program do not 'pile up' in the list of Styles after all,
//   precisely because of the findings listed above and the fact that when I add my own style, I reuse the same name
//   so that a new one just replaces the last one to use that name.  Of course this means that the name should not
//   be used to retrieve a Style, other than immediately after its creation, as is done in the button handler.
//
// This research app has been developed on top of excerpts -
// from: https://docs.oracle.com/javase/tutorial/displayCode.html?code=https://docs.oracle.com/javase/tutorial/uiswing/examples/components/TextSamplerDemoProject/src/components/TextSamplerDemo.java
// and:  https://www.demo2s.com/java/java-swing-jtextpane-rich-text-editor-component.html
public class StyledDocumentResearch {
    ObjectMapper mapper;
    JPanel theMainPanel;
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

    JButton stylesBtn = new JButton("Styles");
    JButton reloadBtn = new JButton("Document Data");
    JButton restoreBtn = new JButton("Restore");

    public StyledDocumentResearch() {
        mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        textPane = new JTextPane();
        DefaultStyledDocument theDocument = (DefaultStyledDocument) textPane.getStyledDocument();
        addStylesToDocument(theDocument);
        fillDocument();
        insertTestString(); // Insert some default-styled text to the text pane.
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

    private void fillDocument() {
        String newline = "\n";
        String[] initString =
                {"This is an editable JTextPane, ",                     //plain
                        "another ",                                     //italic
                        "styled ",                                      //bold
                        "text ",                                        //small
                        "component, ",                                  //large
                        "which supports embedded components " + newline,//plain
                        "and embedded icons..." + newline,              //plain
                        "icon" + newline,                               //icon
                        "JTextPane is a subclass of JEditorPane that " +
                                "uses a StyledEditorKit and StyledDocument, and provides " +
                                "cover methods for interacting with those objects."
                };

        //System.out.println("Length of initial string array: " + initString.length);

        String[] initStyles =
                {"plain", "italic", "bold", "small", "large",
                        "plain", "plain", "icon",
                        "plain"
                };

        StyledDocument doc = textPane.getStyledDocument();

        try {
            for (int i = 0; i < initString.length; i++) {
                doc.insertString(doc.getLength(), initString[i],
                        doc.getStyle(initStyles[i]));
            }
        } catch (BadLocationException ble) {
            System.err.println("Couldn't insert initial text into text pane.");
        }
    } // end fillDocument

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
        stylesBtn.setToolTipText("Show the Document Styles");
        buttonRowS.add(stylesBtn);
        reloadBtn.setToolTipText("Doc-->Data, Reload");
        buttonRowS.add(reloadBtn);
        restoreBtn.setToolTipText("Set doc back to original content");
        buttonRowS.add(restoreBtn);

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

        // This button just being used to show the named styles in the (current) doc.
        stylesBtn.addActionListener(e -> {
            showStyles();
            System.out.println();
        });

        //<editor-fold desc="Show paragraph style character-by-character">
        // This ActionListener uses the 'getLogicalStyle()' method in a character-by-character loop to get the
        //   paragraph style of every individual character.  It does not get the content style for the characters.
        // Purpose was to see if using this method would be any better than the other way we see the styles in this
        //   test/utility app, by expanding the Elements.  Answer is that it does not appear so, because while the
        //   info is accurate, the requirement to specify an integer offset into the text is going to cause us to
        //   refer to the Element in order to get the right values, and in a dynamic document those offsets could be
        //   changed frequently.  It seems much better to just stick with Elements because they report their start
        //   and end offsets as well as identifying paragraph vs content.  The research was informative but now not
        //   needed other than for later review.  So - this ActionListener could be easily applied to any one button
        //   but currently is attached to none.
        ActionListener showParagraphStyles = ae -> {
            String theString = textPane.getText(); // This may have unwanted '\r' characters...
            if (theString == null || theString.trim().isEmpty()) return;
            StyledDocument document = textPane.getStyledDocument();
            int docLength = document.getLength();
            try { // and this will give us the same string without those chars, if possible.
                theString = document.getText(0, docLength);
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println(theString);

            Style s;
            for (int i = 0; i < docLength; i++) {
                s = document.getLogicalStyle(i);
                System.out.println("i: " + i + "\t" + s.getName());
                showAttributes("","", s);
            }
        };
        //</editor-fold

        reloadBtn.addActionListener(e -> {
            String theString = textPane.getText();
            if (theString == null || theString.trim().isEmpty()) return;
            System.out.println(theString);
            System.out.println();
            sdd = new StyledDocumentData();

            // Get the section of the document containing the text (and styling) that we need to preserve.
            Element theSection = textPane.getStyledDocument().getDefaultRootElement();
            System.out.println("The StyledDocument components starting with the DefaultRootElement - ");
            StyledDocumentResearch.this.expandElement(theSection, 1);

            System.out.println("================================================================");
            System.out.println("JSON representation of the StyledDocumentData: ");
            System.out.println(toJsonString(sdd));

            DefaultStyledDocument newDoc = new DefaultStyledDocument();
            sdd.fillStyledDocument(newDoc);
            textPane.setDocument(newDoc);
            addStylesToDocument(newDoc); // Most of these could be migrated to the button handlers.
        });

        restoreBtn.addActionListener(e -> { // Get back to the original content.
            DefaultStyledDocument dsd =new DefaultStyledDocument();
            addStylesToDocument(dsd);
            textPane.setDocument(dsd);
            fillDocument();
            insertTestString();
        });
        return buttonPanel;
    } // end getButtonPanel

    // Given an Element of a Document, show its content, recursively as needed.
    private void expandElement(Element theElement, int level) {
        ParagraphData newParagraphData = null;
        AttributeSet attributes;
        SimpleAttributeSet sas = new SimpleAttributeSet();
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
            //newParagraphData = new ParagraphData(theTextFragment, startOffset, endOffset);
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

        //<editor-fold desc="yagni">
// No longer so useful, but keep for ease of future development, if that happens.
//        // List out a one-liner for each child Element; they will be individually expanded further below.
//        if(elementCount > 1) { // If only 1 then this info is just immediately repeated, so don't.
//            for (i = 0; i < elementCount; i++) {
//                Element childElement = theElement.getElement(i);
//                String childElementName = childElement.getName();
//                System.out.print(indent + elementName + " Element " + i + " Name: " + childElementName);
//                boolean isLeaf = childElement.isLeaf();
//                int startOffset = childElement.getStartOffset();
//                int endOffset = childElement.getEndOffset();
//                if (endOffset > docLength) endOffset = docLength;
//                attributes = childElement.getAttributes();
//                attributeCount = attributes.getAttributeCount();
//                System.out.print("\tisLeaf: " + isLeaf);
//                System.out.print("\tStart: " + startOffset);
//                System.out.print("\tEnd: " + endOffset);
//                System.out.print("\tAttribute Count: " + attributeCount);
//                System.out.println("\tElementCount: " + childElement.getElementCount());
//            }
//            System.out.println();
//        }
//</editor-fold>

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

        // Create a blue color style
        s = doc.addStyle("blue", plain);
        StyleConstants.setForeground(s, Color.BLUE);

        // Create paragraph alignment styles
        s = doc.addStyle("left", plain);
        StyleConstants.setAlignment(s, StyleConstants.ALIGN_LEFT);
        s = doc.addStyle("center", plain);
        StyleConstants.setAlignment(s, StyleConstants.ALIGN_CENTER);
        s = doc.addStyle("right", plain);
        StyleConstants.setAlignment(s, StyleConstants.ALIGN_RIGHT);
        s = doc.addStyle("full", plain);
        StyleConstants.setAlignment(s, StyleConstants.ALIGN_JUSTIFIED);

        // Large and Small styles not used after initial load; user will use the 'size' dropdown (not yet implemented).
        s = doc.addStyle("large", plain);
        StyleConstants.setFontSize(s, 24);
        s = doc.addStyle("small", plain);
        StyleConstants.setFontSize(s, 10);
    } // end addStylesToDocument


    String toJsonString(Object theObject) {
        String theJson = "";
        try {
            theJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(theObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return theJson;
    }

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

    //<editor-fold desc= "unused method to show attributes of a StyleContext">
    // Show the attributes of a StyleContext.
    // In this case, the getStyle() method takes a String that can be made from the key in the enumeration,
    //   even when that key is not a String.  This is how a Style can have a null name and yet still have
    //   a name attribute that can be used to 'get' it.
    private void showAttributes(StyleContext styleContext, Enumeration<?> enumeration) {
        while (enumeration.hasMoreElements()) {
            // The Elements in this enumeration are not just String names; they are Objects that act as keys
            //   to access the Attribute.  They might be of type String, but they might be something else.
            //   Besides Strings, one common/expected type is: StyleContext.NamedStyle which is usually
            //     either a StyleConstants.NameAttribute or a StyleConstants.ResolveAttribute.
            String attributeName = enumeration.nextElement().toString();
            Object theAttribute = styleContext.getStyle(attributeName);
            //System.out.println(toJsonString(theAttribute)); // No - Index 2000 out of bounds for length 2000
            System.out.print("Attribute: " + attributeName);
            if(theAttribute instanceof StyleContext.NamedStyle) {
                System.out.println("\t\tParent Style: " + ((StyleContext.NamedStyle) theAttribute).getName());
            } else {
                System.out.println("\t\tValue: " + theAttribute);
            }
        }
    }
    //</editor-fold>

    // Adapted from: https://www.demo2s.com/java/java-styleconstants-resolveattribute.html
    void showStyles() {
        DefaultStyledDocument doc = (DefaultStyledDocument) textPane.getDocument();
        Enumeration<?> e1 = doc.getStyleNames();
        int styleCount = 0;
        while (e1.hasMoreElements()) {
            styleCount++;
            String styleName = (String) e1.nextElement();
            System.out.print("Style: " + styleName);
            Style style = doc.getStyle(styleName);

            // Don't count the NameAttribute; we have ensured that every style here has one, and
            // that it matches the styleName, which we have already shown by this point.
            int count = style.getAttributeCount() - 1;
            System.out.println("\t\tAttribute count: " + count);

            Enumeration<?> e = style.getAttributeNames();
            while (e.hasMoreElements()) {
                Object o = e.nextElement();
                if (o == StyleConstants.NameAttribute) {
                    // Addressing this so that it may be explicitly skipped over.
                    String nameAttribute = (String) style.getAttribute(o);
                    assert nameAttribute.equals(styleName);
                } else if (o == StyleConstants.ResolveAttribute) {
                    Style parent = (Style) style.getAttribute(o);
                    System.out.println("   Parent style: " + parent.getName());
                } else if (o instanceof String) {
                    String attrName = (String) o;
                    Object attrValue = style.getAttribute(attrName);
                    System.out.println("   Attribute Name: " + attrName + "\tValue: " + attrValue);
                } else {
                    String attrName = o.toString();
                    Object attrValue = style.getAttribute(o);
                    System.out.println("   Attribute Name: " + attrName + "\tValue: " + attrValue);
                }
            }
        }
        System.out.println("Number of styles defined in the document: " + styleCount);
    }

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

    private void insertTestString() {
        StyledDocument document = textPane.getStyledDocument();
        try {
            document.insertString(0, "Hello JTextPane\n", null); // document.getStyle("default") also works.
            //document.insertString(0, "Hello JTextPane\n", document.getStyle("default"));
            //document.insertString(0, "Hello JTextPane\n", document.getStyle("plain"));
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        StyledDocumentResearch rte = new StyledDocumentResearch();
        JFrame testFrame = new JFrame("Styled Document Research");

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        // Needed to override the 'metal' L&F for Swing components.
        String thePlaf = "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel";
        System.out.println("Setting Pluggable Look & Feel to: " + thePlaf);
        String laf = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(thePlaf);
        } catch (Exception ignored) {
        }    // end try/catch
        SwingUtilities.updateComponentTreeUI(testFrame);

        testFrame.getContentPane().add(rte.theMainPanel, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(780, 500));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);

    }

//    private void clearDocument (JTextPane pane, StyledDocument sd) {
//        // for speed, detach document from text pane before updating
//        StyledDocument doc = (StyledDocument) this.textPane.getDocument();
//        Document blank = new DefaultStyledDocument();
//        this.textPane.setDocument(blank);
//
//        try {
//            sd.remove(0, sd.getLength());
//        } catch (BadLocationException ex) {
//            ex.printStackTrace();
//        }
//    }


}
