/*   This class is a JLabel that looks and acts like a button.  As such it
 *   has less overhead but there are other benefits:
 *   1.  This 'button' does not take focus; it is not focusable.
 *   2.  Text may be placed much closer to the edge of the component.
 *
 *   It can display either text or an arrow icon (but not both).
 *   For event handling, use a MouseListener.
 */

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LabelButton extends JLabel {
    private static final long serialVersionUID = 1L;

    public static Border upBorder = new BevelBorder(BevelBorder.RAISED);
    public static Border downBorder = new BevelBorder(BevelBorder.LOWERED);
    static final int UP = 'U';
    static final int DOWN = 'D';
    static final int LEFT = 'L';
    static final int RIGHT = 'R';
    static AppIcon upIcon;
    static AppIcon downIcon;
    static AppIcon leftIcon;
    static AppIcon rightIcon;

    private static MouseAdapter ma;

    // This member is directly accessed in 'header' management, when
    //   a LabelButton is being used as a header label that could be
    //   changed by the user.  The 'default' label will be the original,
    //   unchanged text of the label, that can be used in determination
    //   of choosing the correct sorting algorithm.  I know, very klunky....
    //   Will revisit eventually... lc 5/12/2007
    public String defaultLabel;

    static {
        upIcon = new AppIcon("images/up.gif");
        downIcon = new AppIcon("images/down.gif");
        leftIcon = new AppIcon("images/left.gif");
        rightIcon = new AppIcon("images/right.gif");
    }

    public LabelButton() {
        this("");
    } // end constructor 1

    public LabelButton(String s) {
        this(s, 0);
    } // end constructor 2

    public LabelButton(String s, int direction) {
        super();  // We don't send the text at this point; we may want icon-only.
        defaultLabel = s;
        switch(direction) {
            case UP:
                setIcon(upIcon);
                break;
            case DOWN:
                setIcon(downIcon);
                break;
            case LEFT:
                setIcon(leftIcon);
                break;
            case RIGHT:
                setIcon(rightIcon);
                break;
            default:
                setText(s);
                break;
        }
        setHorizontalAlignment(JLabel.CENTER);

        setBorder(upBorder);
        setName(s);    // Accessed later, in event handling.  When there is no icon, this is used as the text.
        setOpaque(true);
        setFont(Font.decode("Dialog-12"));
        ma = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                setBorder(downBorder);
            } // end mousePressed
            public void mouseReleased(MouseEvent e) {
                setBorder(upBorder);
            } // end mouseReleased
        };

        addMouseListener(ma);
    }

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


