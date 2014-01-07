/* ***************************************************************************
 *
 * File:  $Id: LogSplash.java,v 1.2 2006/02/20 00:55:56 lee Exp $
 *
 * Author:  D. Lee Chastain
 *
 * $Log: LogSplash.java,v $
 * Revision 1.2  2006/02/20 00:55:56  lee
 * Added serialVersionUID, for -Xlint.
 *
 * Revision 1.1  2005/12/04 15:54:08  lee
 * Initial Version
 *
 ****************************************************************************/
/** This class provides the 'splash' window for the Log application.
*/

import javax.swing.*;
import java.awt.*;

public class LogSplash extends JWindow {
  private static final long serialVersionUID = 1841135910245380844L;

  private JProgressBar progressBar;

  public LogSplash(ImageIcon imageIcon) {
    progressBar = new JProgressBar(0, 100);
    progressBar.setStringPainted(true);
    progressBar.setForeground(Color.blue);
    JLabel imageLabel = new JLabel(imageIcon);
    getContentPane().setLayout(new BorderLayout());
    JPanel southPanel = new JPanel(new GridLayout(1,1,25,5));
    getContentPane().add(imageLabel, BorderLayout.CENTER);
    getContentPane().add(southPanel, BorderLayout.SOUTH);
    southPanel.add(progressBar);
    setAlwaysOnTop(true);
    pack();
    setLocationRelativeTo(null);
  } // end constructor


  public void setProgress(String message, int progress) {
    final int theProgress = progress;
    final String theMessage = message;
    progressBar.setValue(theProgress);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        progressBar.setValue(theProgress);
        progressBar.setString(theMessage);
      }
    });
  } // end setProgress

} // end class LogSplash


