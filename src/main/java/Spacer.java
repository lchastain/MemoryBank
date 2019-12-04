/* Description:  This purpose of this custom component is to
/*   create a specific amount of space, specified in
/*   pixels, that it should take up on the screen.
*/

import javax.swing.*;
import java.awt.*;

public class Spacer extends JComponent {
    private static final long serialVersionUID = 1L;

    int Width;
    int Height;
    Color bColor;
    Color fColor;
    Color c = null;

    public Spacer() {  // constructor
        super();
        bColor = getBackground();
        fColor = getForeground();

        // This 'if' is not used/needed, except to fool IJ into going
        // 'green' because it thinks these methods are getting used.
        if (c == Color.black) {  // This does not ever happen.
            resetColor();
            setColor(c);
        }
    } // end constructor

    public Spacer(int width, int height) {  // constructor
        this();
        Width = width;
        Height = height;
    } // end constructor

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public Dimension getPreferredSize() {
        if (Width == 0 || Height == 0) return super.getPreferredSize();
        return new Dimension(Width, Height);
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
