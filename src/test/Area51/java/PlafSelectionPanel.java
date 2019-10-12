import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

// A panel that shows a list of the Pluggable Look and Feels that are currently
// installed on the system, and allows the user to make a new selection.  The
// calling context must make the change, if any.
public class PlafSelectionPanel extends JPanel implements ActionListener {
    static final long serialVersionUID = 1L;

    private String selectedPlaf;
    private ButtonGroup bg;
    private HashMap<String, String> hm;

    public PlafSelectionPanel() {
        UIManager.LookAndFeelInfo[] lafiArray = UIManager.getInstalledLookAndFeels();
        // A null or zero count array result does not seem possible.
        // Defer handling of those cases until proven otherwise.

        String currentLaf = UIManager.getLookAndFeel().getName();

        bg = new ButtonGroup();
        hm = new HashMap<>();
        setLayout(new GridLayout(0, 1));

        for (UIManager.LookAndFeelInfo lafi : lafiArray) {
            //System.out.print(lafi.getClassName() + "\t");
            //System.out.println(lafi.getName());
            String theName = lafi.getName();
            String theClassName = lafi.getClassName();
            hm.put(theName, theClassName);
            JRadioButton jrb = new JRadioButton(theName);
            jrb.addActionListener(this);
            jrb.setActionCommand(theName);
            if (theName.equals(currentLaf)) jrb.setSelected(true);
            bg.add(jrb);
            add(jrb);
        }

    }

    public String getSelectedPlaf() {
        return selectedPlaf;
    }

    public void actionPerformed(ActionEvent e) {
        ButtonModel bm = bg.getSelection();
        String t = "Not selected";
        if (bm != null) t = bm.getActionCommand();
        //System.out.println(t);
        //System.out.println(hm.get(t));
        selectedPlaf = hm.get(t);
    }

}
