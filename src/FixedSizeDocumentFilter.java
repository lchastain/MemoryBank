/* ***********************************************************************
 * File:    FixedSizeDocumentFilter.java
 * Author:  D. Lee Chastain
 *
 ************************************************************************/

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class FixedSizeDocumentFilter extends DocumentFilter {
    private int maxSize;

    // limit is the maximum number of characters allowed.
    public FixedSizeDocumentFilter(int limit) {
        maxSize = limit;
    }

    // This method is called when characters are inserted into the document
    public void insertString(DocumentFilter.FilterBypass fb, int offset, String str,
                             AttributeSet attr) throws BadLocationException {
        replace(fb, offset, 0, str, attr);
    }

    // This method is called when characters in the document are replace with other characters
    public void replace(DocumentFilter.FilterBypass fb, int offset, int length,
                        String str, AttributeSet attrs) throws BadLocationException {
        int newLength = fb.getDocument().getLength() - length + str.length();
        if (newLength <= maxSize) {
            fb.replace(offset, length, str, attrs);
        } else {
            throw new BadLocationException("New characters exceeds max size of document", offset);
        }
    }
} // end class FixedSizeDocumentFilter
