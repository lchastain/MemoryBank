/**  Implements a header for the LogEvents.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.border.*;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class EventHeader extends JPanel implements MouseListener {
    private static final long serialVersionUID = 685914511761384442L;

    // Swing Types
    private JLabel lblTheTitle;
    private JLabel lblEventSummary;
    private JPanel pnlCenter;

    // Custom Types
    private LabelButton btnShowHide;
    private LabelButton btnUpdate;  // No longer used; moved to menu as 'refresh'
    private EventNoteGroup smTheHeaderContainer;

    EventHeader(EventNoteGroup sm) {
        super(new BorderLayout());
        smTheHeaderContainer = sm;
        setBackground(Color.blue);

        // Create the window title
        lblTheTitle = new JLabel("Upcoming Events");
        lblTheTitle.setHorizontalAlignment(JLabel.CENTER);
        lblTheTitle.setForeground(Color.white);
        lblTheTitle.setFont(Font.decode("Serif-bold-20"));
        lblTheTitle.setBackground(getBackground());
        lblTheTitle.setOpaque(true);

        // Controls
        btnShowHide = new LabelButton("Show");
        btnUpdate = new LabelButton("Update");
        lblEventSummary = new JLabel("Select an Event to display.") {
            static final long serialVersionUID = 1512781130847802324L;

            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                if (d.height < 50) d.height = 50;
                return new Dimension(200, d.height);
            } // end getPreferredSize
        };

        // Font settings
        btnShowHide.setFont(Font.decode("Dialog-bold-12"));
        btnUpdate.setFont(Font.decode("Dialog-bold-12"));
        lblEventSummary.setFont(Font.decode("Dialog-bold-11"));

        // Sizing
        Dimension d = btnShowHide.getPreferredSize();
        btnShowHide.setPreferredSize(new Dimension(40, d.height));
        btnUpdate.setPreferredSize(new Dimension(50, d.height));

        // Without the width restriction, the label length
        //   can dictate the width of the entire panel, and
        //   if a fixed height is not specified
        //   then the display is too jumpy when going from
        //   one two-line summary to the next, since the
        //   momentary display between them is a one-liner.
        lblEventSummary.setBorder(new EmptyBorder(1, 1, 1, 1));

        // North panel
        JPanel pnlNorth = new JPanel(new BorderLayout());
        pnlNorth.add(btnShowHide, BorderLayout.WEST);
        pnlNorth.add(lblTheTitle, BorderLayout.CENTER);
        pnlNorth.add(smTheHeaderContainer.npThePager, BorderLayout.EAST);
        smTheHeaderContainer.npThePager.setBackground(getBackground());

        // Center panel
        pnlCenter = new JPanel(new BorderLayout());
        pnlCenter.add(lblEventSummary, BorderLayout.CENTER);
        pnlCenter.setVisible(false);

        add(pnlNorth, BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);

        // (Swing) Event Handling
        btnShowHide.addMouseListener(this);
        btnUpdate.addMouseListener(this);

    } // end constructor


    // The input parameter should be properly formatted HTML
    public void setEventSummary(String s) {
        lblEventSummary.setText(s);
    } // end setEventSummary


    //---------------------------------------------------------
    // MouseListener methods
    //---------------------------------------------------------
    public void mouseClicked(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getText();

        if (s.equals("Show")) {
            pnlCenter.setVisible(true);
            btnShowHide.setText("Hide");
        } else if (s.equals("Hide")) {
            pnlCenter.setVisible(false);
            btnShowHide.setText("Show");
        } else if (s.equals("Update")) {
            smTheHeaderContainer.refresh();
            // System.out.println("Update " + (new Date()).toString());
        } else {
            (new Exception("Unhandled action!")).printStackTrace();
            System.exit(1);
        } // end if
    } // end mouseClicked

    public void mouseEntered(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getText();
        if (s.equals("Show")) {
            s = "Click here to show the summary info for each Event";
        } else if (s.equals("Hide")) {
            s = "Click here to hide the summary info for each Event";
        } else if (s.equals("Update")) {
            s = "Click here to update the Events to current date/time";
        } // end if
        smTheHeaderContainer.setMessage(s);
    } // end mouseEntered

    public void mouseExited(MouseEvent e) {
        smTheHeaderContainer.setMessage(" ");
    } // end mouseExited

    public void mousePressed(MouseEvent e) {
    } // end mousePressed

    public void mouseReleased(MouseEvent e) {
    } // end mouseReleased
    //---------------------------------------------------------

} // end class EventHeader
