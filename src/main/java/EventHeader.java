/*  Implements a header for the LogEvents.
 */

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class EventHeader extends JPanel implements MouseListener {
    private static final long serialVersionUID = 1L;

    private JLabel lblEventSummary;
    private JPanel pnlCenter;

    // Custom Types
    private LabelButton btnShowHide;
    private EventNoteGroup smTheHeaderContainer;

    EventHeader(EventNoteGroup eventNoteGroup) {
        super(new BorderLayout());
        smTheHeaderContainer = eventNoteGroup;
        setBackground(Color.blue);

        // Create the window title
        // Swing Types
        JLabel lblTheTitle = new JLabel(eventNoteGroup.getName());
        lblTheTitle.setHorizontalAlignment(JLabel.CENTER);
        lblTheTitle.setForeground(Color.white);
        lblTheTitle.setFont(Font.decode("Serif-bold-20"));
        lblTheTitle.setBackground(getBackground());
        lblTheTitle.setOpaque(true);

        // Controls
        btnShowHide = new LabelButton("Show");

        // No longer used; moved to menu as 'refresh'
        LabelButton btnUpdate = new LabelButton("Update");

        lblEventSummary = new JLabel("Select an Event to display.") {
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
    void setEventSummary(String s) {
        lblEventSummary.setText(s);
    } // end setEventSummary


    //---------------------------------------------------------
    // MouseListener methods
    //---------------------------------------------------------
    public void mouseClicked(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getText();

        switch (s) {
            case "Show":
                pnlCenter.setVisible(true);
                btnShowHide.setText("Hide");
                break;
            case "Hide":
                pnlCenter.setVisible(false);
                btnShowHide.setText("Show");
                break;
            case "Update":
                smTheHeaderContainer.refresh();
                // System.out.println("Update " + (new Date()).toString());
                break;
            default:
                (new Exception("Unhandled action!")).printStackTrace();
                System.exit(1);
        }
    } // end mouseClicked

    public void mouseEntered(MouseEvent e) {
        LabelButton source = (LabelButton) e.getSource();
        String s = source.getText();
        switch (s) {
            case "Show":
                s = "Click here to show the summary info for each Event";
                break;
            case "Hide":
                s = "Click here to hide the summary info for each Event";
                break;
            case "Update":
                s = "Click here to update the Events to current date/time";
                break;
        }
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
