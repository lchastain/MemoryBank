/*  Implements a header for the LogEvents.
 */

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class EventHeader extends JPanel implements MouseListener {
    private static final long serialVersionUID = 1L;

    private final JLabel lblEventSummary;
    private final JPanel pnlCenter;

    // Custom Types
    private final LabelButton btnShowHide;
    private final EventNoteGroupPanel eventNoteGroupPanel;

    EventHeader(EventNoteGroupPanel eventNoteGroupPanel) {
        super(new BorderLayout());
        this.eventNoteGroupPanel = eventNoteGroupPanel;
        setBackground(Color.blue);

        // Create the window title
        // Swing Types
        JLabel lblTheTitle = new JLabel(eventNoteGroupPanel.getGroupName());
        lblTheTitle.setHorizontalAlignment(JLabel.CENTER);
        lblTheTitle.setForeground(Color.white);
        lblTheTitle.setFont(Font.decode("Serif-bold-20"));
        lblTheTitle.setBackground(getBackground());
        lblTheTitle.setOpaque(true);

        // Controls
        btnShowHide = new LabelButton("Show Summary ");

        lblEventSummary = new JLabel("Select an Event to display.") {
            private static final long serialVersionUID = 1L; // Yes, needed.
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                if (d.height < 50) d.height = 50;
                return new Dimension(200, d.height);
            } // end getPreferredSize
        };

        // Font settings
        btnShowHide.setFont(Font.decode("Dialog-bold-12"));
        lblEventSummary.setFont(Font.decode("Dialog-bold-11"));

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
        pnlNorth.add(eventNoteGroupPanel.theNotePager, BorderLayout.EAST);
        eventNoteGroupPanel.theNotePager.setBackground(getBackground());

        // Center panel
        pnlCenter = new JPanel(new BorderLayout());
        pnlCenter.add(lblEventSummary, BorderLayout.CENTER);
        pnlCenter.setVisible(false);

        add(pnlNorth, BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);

        // (Swing) Event Handling
        btnShowHide.addMouseListener(this);
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
            case "Show Summary ":
                pnlCenter.setVisible(true);
                btnShowHide.setText("Show Calendar ");
                eventNoteGroupPanel.getThreeMonthColumn().setVisible(false);
                break;
            case "Show Calendar ":
                pnlCenter.setVisible(false);
                btnShowHide.setText("Show Summary ");
                eventNoteGroupPanel.getThreeMonthColumn().setVisible(true);
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
            case "Show Summary ":
                s = "Click here to show the summary info for each Event";
                break;
            case "Show Calendar ":
                s = "Click here to hide the summary info for each Event";
                break;
        }
        eventNoteGroupPanel.setStatusMessage(s);
    } // end mouseEntered

    public void mouseExited(MouseEvent e) {
        eventNoteGroupPanel.setStatusMessage(" ");
    } // end mouseExited

    public void mousePressed(MouseEvent e) {
    } // end mousePressed

    public void mouseReleased(MouseEvent e) {
    } // end mouseReleased
    //---------------------------------------------------------

} // end class EventHeader
