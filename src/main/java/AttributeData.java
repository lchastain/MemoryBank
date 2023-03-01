import javax.swing.text.AttributeSet;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;

class AttributeData {
    String type;
    String value;

    // Used by the Jackson mapper, upon data load and conversion of json to an object of this class.
    AttributeData() { }

    AttributeData(String t, String v) {
        this();  // Not strictly needed, but this stops the IJ 'unused' complaint.
        type = t;
        value = v;
    }

    static Vector<AttributeData> getAttributeDataVector(AttributeSet attributes) {
        Vector<AttributeData> theAnswer = null;
        String attributeValue;
        if(attributes != null && attributes.getAttributeCount() > 0) {
            theAnswer = new Vector<>(1, 1);
            Enumeration<?> enumeration = attributes.getAttributeNames();
            while (enumeration.hasMoreElements()) {
                Object attributeName = enumeration.nextElement();
                Object theAttribute = attributes.getAttribute(attributeName);

                if(theAttribute instanceof StyleContext.NamedStyle) {
                    attributeValue = ((StyleContext.NamedStyle) theAttribute).getName();
                } else if(theAttribute instanceof Color) {
                    attributeValue = String.valueOf(((Color) theAttribute).getRGB());

                    // Extra info not used by the app, but may be helpful when reviewing data.
                    String s = attributeName.toString() + " color";
                    theAnswer.add(new AttributeData(s, theAttribute.toString()));
                } else {
                    attributeValue = theAttribute.toString();
                }

                AttributeData ad = new AttributeData(attributeName.toString(), attributeValue);
                theAnswer.add(ad);
            }
        }
        return theAnswer;
    }


}
