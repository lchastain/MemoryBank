import javax.swing.*;
import java.awt.*;
import java.io.Serial;

public class AppSplash extends JWindow {
    @Serial
    private static final long serialVersionUID = 1841135910245380844L;

    private final JProgressBar progressBar;

    public AppSplash(ImageIcon imageIcon) {
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(Color.blue);
        JLabel imageLabel = new JLabel(imageIcon);
        getContentPane().setLayout(new BorderLayout());
        JPanel southPanel = new JPanel(new GridLayout(1, 1, 25, 5));
        getContentPane().add(imageLabel, BorderLayout.CENTER);
        getContentPane().add(southPanel, BorderLayout.SOUTH);
        southPanel.add(progressBar);
        setAlwaysOnTop(true);
        pack();
        setLocationRelativeTo(null);
    } // end constructor


    void setProgress(String message, int progress) {
        final int theProgress = progress;
        final String theMessage = message;
        progressBar.setValue(theProgress);
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(theProgress);
            progressBar.setString(theMessage);
        });
    } // end setProgress

} // end class AppSplash


