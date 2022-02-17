import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class AppImageMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("g01@doughmain.net");

        AppImage li = new AppImage();

        // Construct a list of five images (although one is null) -
        String basePath = MemoryBank.logHome + File.separatorChar;
        Image[] images = new Image[]{
                new ImageIcon(basePath + "icons" + File.separatorChar + "icon_not.gif").getImage(),
                new ImageIcon(basePath + "images" + File.separatorChar + "ABOUT.gif").getImage(),
                null,
                new ImageIcon(basePath + "icons" + File.separatorChar + "acro.ico").getImage(),
                new ImageIcon(basePath + "icons" + File.separatorChar + "new8.gif").getImage()
        };

        JFrame testFrame = new JFrame("AppImage Driver");
        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        testFrame.getContentPane().add(li, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(300, 300));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
        int i = 0;
        int theWait= 5000; // milliseconds between images.
        boolean neverFalse = true;

        // Go into an endless loop, showing the defined images.
        while (neverFalse) {
            if (images[i] == null) theWait = 2000; // Unless image is null, then two secs.
            li.setImage(images[i++]);

            try {
                Thread.sleep(theWait);
            } catch (Exception e) {
                neverFalse = false;
            }

            if (i == images.length) i = 0;
        } // end while
    }
}
