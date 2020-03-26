/* Description:  This purpose of this custom component is to
/*   create a specific amount of space, specified in
/*   pixels, that it should take up on the screen.
*/

import javax.swing.*;
import java.awt.*;

public class Spacer extends JComponent {
    private static final long serialVersionUID = 1L;

    int myWidth;
    int myHeight;
    Color bColor;
    Color fColor;
    Color c = null;

    public Spacer() {  // constructor
        super();
        bColor = getBackground();
        fColor = getForeground();
    } // end constructor

    public Spacer(int width, int height) {  // constructor
        this();
        myWidth = width;
        myHeight = height;
    } // end constructor

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public Dimension getPreferredSize() {
        int theWidth = myWidth;
        int theHeight = myHeight;
        if(myWidth == 0) theWidth = super.getPreferredSize().width;
        if(myHeight == 0) theHeight = super.getPreferredSize().height;
        return new Dimension(theWidth, theHeight);
    } // end getPreferredSize

    public void resetColor() {
        c = null;
        setForeground(fColor);
        setBackground(bColor);
        repaint();
    } // end resetColor

    public void setColor(Color c) {
        this.c = c;
        setForeground(c);
        setBackground(c);
    } // end setColor

    public void paint(Graphics g) {
        super.paint(g);
        Dimension d = getSize();
        if (c != null) g.fillRect(0, 0, d.width, d.height);
    } // end paint
} // end class
