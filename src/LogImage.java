/* ***************************************************************************
 *
 * File:  $Id: LogImage.java,v 1.3 2006/02/20 00:55:11 lee Exp $
 *
 * Author:  D. Lee Chastain
 *
 * $Log: LogImage.java,v $
 * Revision 1.3  2006/02/20 00:55:11  lee
 * Added serialVersionUID, for -Xlint.
 *
 * Revision 1.2  2005/12/11 19:08:46  lee
 * Added a constructor with a filename string parameter.  Also added an
 * option to disallow scaling, which is otherwise the default behavior.
 *
 * Revision 1.1  2005/12/10 02:59:58  lee
 * A new, lighter class that can be used by the MonthView and supports more
 * built-in scaling than the previous JLabel.
 *
 *
 ****************************************************************************/
/**
 This custom component provides an easy way to add a scaled graphic to
 a container without the overhead of the standard swing classes that
 are commonly used to display images.

 To use, either construct with an image or set one later, either
 before or after displaying the component.
 */
/*
   Cases considered:
   1.  The container is resized.
   2.  The image is subsequently changed.
*/

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

public final class LogImage extends JPanel {
    private static final long serialVersionUID = 6216448968617689802L;

    private Image theImage;
    private Image theScaledImage;
    private int containerWidth;
    private int containerHeight;
    private int imageWidth;
    private int imageHeight;
    private boolean doScale;

    public LogImage() {
        setOpaque(true);
        imageWidth = 0;
        imageHeight = 0;
        containerWidth = 0;
        containerHeight = 0;
        doScale = true;      // This is the default.

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                // Note - Do NOT call getPreferredSize from here;
                //   it will produce loops and undetermined results.

                // Get the size of the space this component has been allotted.
                Dimension d = getSize();
                containerWidth = d.width;
                containerHeight = d.height;
                // MemoryBank.debug("LogImage - componentResized, Component size: " + d);

                if (containerWidth == 0) return;

                scaleImage();
            } // end componentResized

            public void componentShown(ComponentEvent e) {
                MemoryBank.debug("LogImage shown");
            } // end componentShown
        });
    } // end constructor


    public LogImage(Image i) {
        this();
        setImage(i);
    } // end constructor


    public LogImage(String imageFile) {
        this();
        setImage(getToolkit().getImage(imageFile));
    } // end constructor


    public LogImage(String imageFile, boolean b) {
        this();
        doScale = b;
        setImage(getToolkit().getImage(imageFile));
    } // end constructor


    //------------------------------------------------------------------
    public float getAlignmentX() {
        return Component.CENTER_ALIGNMENT;
    }

    public float getAlignmentY() {
        return Component.CENTER_ALIGNMENT;
    }


    //------------------------------------------------------------------
    // Method Name:  getPreferredSize
    //
    // The default size is arbitrarily small, more than
    //   an icon but probably less than anything else.
    //
    //------------------------------------------------------------------
    public Dimension getPreferredSize() {
        if (imageWidth == 0) return new Dimension(50, 50);
        return new Dimension(imageWidth, imageHeight);
    } // end getPreferredSize


    //------------------------------------------------------------------
    // Method Name: paintComponent
    //
    //------------------------------------------------------------------
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // If we're repainting while theImage is null, it is simply to
        //   clear the graphic, which 'super' has just done.
        if (theImage == null) return;

        // Other implementations that were tried:
        // 1.  Allowing drawImage to do the scaling results in grainy images.
        // 2.  Obtaining a 'scaledInstance' at this point results in very
        //     slow drawing.

        g.drawImage(theScaledImage, 0, 0, this);
    } // end paintComponent


    public void scaleImage() {
        if (containerWidth == 0) return;  // Cannot scale until we know our bounds.
        if (theImage == null) return;     // Cannot scale a null.

        // Get the percentage that each dimension is subject to change.
        double xFactor = (double) containerWidth / (double) imageWidth;
        double yFactor = (double) containerHeight / (double) imageHeight;

        // Take the smaller of the two, so that when applied to both, the
        //  scaling will be proportional and the result will be completely
        //  viewable within the space we have, even if one dimension
        //  continues on with only 'background'.
        double theFactor = Math.min(xFactor, yFactor);

        if ((int) theFactor == 1) {
            theScaledImage = theImage;
            return;
        } // end if

        int theScalingAlgorithm = Image.SCALE_SMOOTH;
        if (theFactor > 1.0) theScalingAlgorithm = Image.SCALE_FAST;

        // A smooth scale is needed more for shrinking than enlarging.
        // Also, it can take a lot more time when enlarging.
        // MemoryBank.debug("LogImage - scaleImage factor = " + theFactor);

        theScaledImage = theImage.getScaledInstance(
                (int) ((double) imageWidth * theFactor),
                (int) ((double) imageHeight * theFactor),
                theScalingAlgorithm);

        // MemoryBank.debug("Free Memory: " + Runtime.getRuntime().freeMemory());
        repaint();
    } // end scaleImage


    public void setImage(Image i) {
        theImage = i;
        if (theImage == null) {
            repaint();
            return;
        } // end if

        imageWidth = theImage.getWidth(this);
        imageHeight = theImage.getHeight(this);
        // MemoryBank.dbg("LogImage - setImage  width: " + imageWidth);
        // MemoryBank.debug("\theight: " + imageHeight);

        if (doScale) scaleImage();
        else {
            theScaledImage = theImage;
            repaint();
        } // end if

    } // end setImage


    //-----------------------------------------------------------------
    // Test driver for the class.
    //-----------------------------------------------------------------
    public static void main(String args[]) {
        MemoryBank.debug = true;
        //-----------------------------------------------------------

        LogImage li = new LogImage();

        Image images[] = new Image[]{
                new LogIcon("icons/icon_not.gif").getImage(),
                new ImageIcon(MemoryBank.logHome + "/images/ABOUT.gif").getImage(),
                null,
                new LogIcon("icons/acro.ico").getImage(),
                new LogIcon("icons/new8.gif").getImage()
        };

        // Make the frame and add ourselves to it.
        JFrame imageFrame = new JFrame("LogImage Test");
        imageFrame.getContentPane().add(li);
        imageFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        imageFrame.addWindowListener(
                new WindowAdapter() {
                    public void windowClosing(WindowEvent we) {
                        System.exit(0);
                    } // end windowClosing
                } // end new WindowAdapter
        );

        // Center the Frame in the available screen area
        imageFrame.pack();
        imageFrame.setLocationRelativeTo(null);

        imageFrame.setVisible(true);

        int i = 0;
        int theWait;  // How many milliseconds to pause.

        while (true) {
            theWait = 10000;
            if (images[i] == null) theWait = 3000;
            li.setImage(images[i++]);

            try {
                Thread.sleep(theWait);
            } catch (Exception e) {
            }

            if (i == images.length) i = 0;
        } // end while

    } // end main
    //------------------------------------------------------------------*/

} // end class LogImage 
