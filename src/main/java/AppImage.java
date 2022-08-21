/*
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.Serial;

public final class AppImage extends JPanel {
    @Serial
    private static final long serialVersionUID = 1L;

    private Image theImage;
    private Image theScaledImage;
    private int containerWidth;
    private int containerHeight;
    private int imageWidth;
    private int imageHeight;
    private boolean doScale;

    public AppImage() {
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
                // MemoryBank.debug("AppImage - componentResized, Component size: " + d);

                if (containerWidth == 0) return;

                scaleImage();
            } // end componentResized

            public void componentShown(ComponentEvent e) {
                MemoryBank.debug("AppImage shown");
            } // end componentShown
        });
    } // end constructor


    public AppImage(String imageFile) {
        this();
        doScale = false;
        java.net.URL imgURL = AppImage.class.getResource(imageFile);
        setImage(getToolkit().getImage(imgURL));
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
        // MemoryBank.debug("AppImage - scaleImage factor = " + theFactor);

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
        // MemoryBank.dbg("AppImage - setImage  width: " + imageWidth);
        // MemoryBank.debug("\theight: " + imageHeight);

        if (doScale) scaleImage();
        else {
            theScaledImage = theImage;
            repaint();
        } // end if

    } // end setImage

} // end class AppImage
