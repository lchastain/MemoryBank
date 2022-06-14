import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.Vector;

public class StyledDocumentData {
    Vector<ParagraphData> paragraphData;

    StyledDocumentData() {
        paragraphData = new Vector<>(0, 1);
    }
//    StyledDocumentData(String s) {
//        this();
//    }

    void addParagraph(ParagraphData pd) {
        paragraphData.add(pd);
    }

    // Returns an ImageIcon, or null if the path was invalid.
    static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = StyledDocumentData.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, path);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    // A calling context must provide a StyledDocument (a new/empty doc is typical).
    // This method then fills that document (starting at the 'end') with the data
    //   that this class holds.
    void fillStyledDocument(DefaultStyledDocument theDocument) {
        Style theDefault = theDocument.getStyle(StyleContext.DEFAULT_STYLE);
        Style parentStyle;
        Style paragraphStyle;
        for (ParagraphData pd : paragraphData) {
            int paragraphStart = theDocument.getLength();
            for (ContentData cd : pd.contentData) {
                // Make a Style (AttributeSet) from the contentAttributeData Vector in the ContentData.

                // Initialize 'contentStyle' to a 'real' style that will never be used, to silence compiler
                //   complaints in the loop below about how maybe it hadn't ever been initialized.
                // We expect this to work because every ContentData in this app will have a name, and that name will
                //   be the first attribute in its set, at which time the placeholder variable that we define here
                //   will be replaced with the correct Style that will be used with all subsequent attributes.
                Style contentStyle = theDocument.addStyle("contentStyle", null);

                for(AttributeData ad: cd.contentAttributeData) {
                    // Note that not all attributes require a Style adjustment.  For those that do not, we just ignore.
                    switch (ad.type) {
                        case "name":
                            contentStyle = theDocument.addStyle(ad.value, theDefault);
                            break;
                        case "resolver":
                            parentStyle = theDocument.getStyle(ad.value);
                            //s.addAttributes(parentStyle);
                            contentStyle.setResolveParent(parentStyle);
                            break;
                        case "family":
                            StyleConstants.setFontFamily(contentStyle, ad.value);
                            break;
                        case "foreground":
                            Color cf = new Color(Integer.parseInt(ad.value));
                            StyleConstants.setForeground(contentStyle, cf);
                            break;
                        case "background":
                            Color cb = new Color(Integer.parseInt(ad.value));
                            StyleConstants.setBackground(contentStyle, cb);
                            break;
                        case "size":
                            int theSize = Integer.parseInt(ad.value);
                            StyleConstants.setFontSize(contentStyle, theSize);
                            break;
                        case "icon":
                            StyleConstants.setIcon(contentStyle, createImageIcon(ad.value));
                            break;
                        case "italic":
                            StyleConstants.setItalic(contentStyle, Boolean.parseBoolean(ad.value));
                            break;
                        case "bold":
                            StyleConstants.setBold(contentStyle, Boolean.parseBoolean(ad.value));
                            break;
                        case "underline":
                            StyleConstants.setUnderline(contentStyle, Boolean.parseBoolean(ad.value));
                            break;
                        case "superscript":
                            StyleConstants.setSuperscript(contentStyle, Boolean.parseBoolean(ad.value));
                            break;
                        case "subscript":
                            StyleConstants.setSubscript(contentStyle, Boolean.parseBoolean(ad.value));
                            break;
                    } // end switch on attribute type (ie, its name)
                } // end for each AttributeData

                try {
                    theDocument.insertString(theDocument.getLength(), cd.contentTextFragment, contentStyle);
                } catch (BadLocationException ble) {
                    System.err.println("Couldn't insert text into text pane.");
                }
            } // end for contentData

            // Although this does not quite match the 'order' implied by the json-data hierarchy, it seems that
            //   the content and its attributes needs to be added before we apply the paragraph attributes.
            for(AttributeData ad: pd.paragraphAttributeData) {
                parentStyle = theDocument.getLogicalStyle(paragraphStart);
                paragraphStyle = theDocument.addStyle("paragraphStyle", parentStyle);
                if ("Alignment".equals(ad.type)) {
                    StyleConstants.setAlignment(paragraphStyle, Integer.parseInt(ad.value));
                    theDocument.setParagraphAttributes(paragraphStart, pd.paragraphTextFragment.length(),
                            paragraphStyle, true);
                } else if("resolver".equals(ad.type)) {
                    parentStyle = theDocument.getStyle(ad.value);
                    paragraphStyle.setResolveParent(parentStyle);
                }
                // Not necessary to add any other attributes to the paragraph; if there are any,
                // they are automatically applied along with the content text and attributes.
            }
        }
        // Document style cleanup.
        theDocument.removeStyle("paragraphStyle");
        theDocument.removeStyle("contentStyle");
    } // end fileStyledDocument
}

