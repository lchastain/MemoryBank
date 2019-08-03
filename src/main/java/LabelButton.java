/**  This class is a JLabel that looks and acts like a button.  As such it
 *   has less overhead but the main need for it is to be able to get the 
 *   displayed text much closer to the edge, thereby allowing a smaller 
 *   footprint.
 *
 *   AddActionListener (and the corresponding array of ActionListener)
 *   is currently not implemented; use the MouseClicked method
 *   of a MouseListener or MouseAdapter, instead.
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class LabelButton extends JLabel {
    private static final long serialVersionUID = 1L;

    public static Border up = new BevelBorder(BevelBorder.RAISED);
    public static Border down = new BevelBorder(BevelBorder.LOWERED);

    private static MouseAdapter ma;

    public String defaultLabel;
    // This member is directly accessed in 'header' management, when
    //   a LabelButton is being used as a header label that could be
    //   changed by the user.  The 'default' label will be the original,
    //   unchanged text of the label, that can be used in determination
    //   of choosing the correct sorting algorithm.  I know, very klunky....
    //   Will revisit eventually... lc 5/12/2007

    public LabelButton() {
        this("");
    } // end constructor 1


    public LabelButton(String s) {
        super();
        defaultLabel = s;
        setText(s);
        setHorizontalAlignment(JLabel.CENTER);

        setBorder(up);
        setName(s);    // Used later, in event handling
        setOpaque(true);
        setFont(Font.decode("Dialog-12"));
        ma = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                setBorder(down);
            } // end mousePressed

            public void mouseReleased(MouseEvent e) {
                setBorder(up);
            } // end mouseReleased
        };

        addMouseListener(ma);
    } // end constructor 2

    //==========================================================

    public Dimension getPreferredSize() {
        if (!isVisible()) return new Dimension(0, 0);
        return super.getPreferredSize();
    } // end getPreferredSize

    public void removeMouseListener() {
        removeMouseListener(ma);
    }

    public void setText(String s) {
        if (defaultLabel != null)
            if (defaultLabel.equals("")) defaultLabel = s;
        super.setText(s);
    } // end setText

} // end class LabelButton


