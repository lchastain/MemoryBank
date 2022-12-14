//
// This class provides an interface for the printing of a text file,
//   whose filename is provided by the user when this constructor 
//   is called.
//
// There is no benefit to deriving this class from, or composing
//   it out of swing Components.
//

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Vector;

class TextFilePrinter extends Dialog implements
        ActionListener, ItemListener, TextPageListener, WindowListener {
    private static final long serialVersionUID = 1L;

    private int currentPtSize;
    private int currentPage;
    private int Pages;
    private String filename;

    private Button prevBtn = new Button("Prev");
    private Button nextBtn = new Button("Next");
    private Button printBtn = new Button("Print...");
    private Button closeBtn = new Button("Close");
    private TextField pageNumberField;
    private TextPage preview;
    private Label l1;
    private Frame parentFrame;
    private ScrollPane sp;

    private TextFilePrinter(String filename) {
        this(filename, new Frame());
    } // end constructor

    private TextFilePrinter(String filename, Frame f) {
        super(f, "Print Preview", true); // Dialog constructor
        parentFrame = f;
        currentPtSize = 12;
        currentPage = 1;
        pageNumberField = new TextField("" + currentPage);
        addWindowListener(this);
        System.out.println("TextFilePrinter: Filename =\n  " + filename);

        // Remember the filename.
        this.filename = filename;

        // Read the file into the 'lines' vector.
        Vector<String> lines = new Vector<>();
        try {
            String line;
            BufferedReader fis = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filename)));
            while ((line = fis.readLine()) != null) {
                System.out.println(line);
                lines.addElement(line);
            }
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Listen for events.
        pageNumberField.addActionListener(this);
        prevBtn.addActionListener(this);
        nextBtn.addActionListener(this);
        printBtn.addActionListener(this);
        closeBtn.addActionListener(this);

        // Layout and show components.

        // Create and prepare the preview component.
        preview = new TextPage(lines);
        preview.addTextPageListener(this);
        preview.setHeader(prettyName(filename));
        preview.setFont(new Font("Monospaced", Font.PLAIN, currentPtSize));

        // Create a ScrollPane to hold the preview
        sp = new ScrollPane();
        sp.setBackground(Color.white);
        sp.add(preview, "Center");

        resetSize();
        sp.getVAdjustable().setUnitIncrement(20);
        add(sp, BorderLayout.CENTER);

        Panel p = new Panel(new FlowLayout());
        p.add(new Label("Page:", Label.RIGHT));
        p.add(pageNumberField);
        l1 = new Label("of    x");
        l1.setAlignment(Label.LEFT);
        p.add(l1);
        p.add(prevBtn);
        prevBtn.setEnabled(false);
        p.add(nextBtn);
        p.add(new Label("    Point Size:", Label.RIGHT));

        Choice ps = new Choice();
        ps.setFont(Font.decode("DialogInput-bold-12"));
        String[] nums = {"04", "05", "06", "07", "08", "09",
                "10", "11", "12", "13", "14", "15", "16", "17",
                "18", "19", "20", "21", "22", "23", "24"};

        for (String num : nums) {
            ps.addItem(num);
        } // end for i
        ps.select("" + currentPtSize);
        ps.addItemListener(this);

        p.add(ps);
        p.add(printBtn);
        p.add(closeBtn);
        // getContentPane().setNotes(p, BorderLayout.NORTH);
        add(p, BorderLayout.NORTH);

        pack();
        setVisible(true);
    } // end constructor

    private void resetSize() {
        //--------------------------------------------------------
        // Set the ScrollPane size to the best viewable value.
        //--------------------------------------------------------
        Dimension d1 = getToolkit().getScreenSize();
        Dimension d2 = preview.getPreferredSize();
        // System.out.println("screen size: " + d1);
        // System.out.println("preview preferred size: " + d2);

        // preview preferred width is 595, less than
        //   the minimum supported 640x480 screen resolution.
        //   This means there should never be a need for a
        //   horizontal scrollbar.

        int w = d2.width;
        int h = d2.height;

        // there may not always be a vertical scrollbar,
        //   depending on screen resolution.  However, if a vertical
        //   scrollbar does appear, it will decrease the viewable
        //   width and create the need for the horizontal sb.
        //   So, to counteract that event, we add to our
        //   width setting by the amount of the width of the
        //   vertical sb.

        if (d2.height > d1.height - 130) {
            // There will be a vertical scrollbar; adjust width.
            w += 20;  // sb appears with a value of 19 or less.
            // sp.getVScrollbarWidth() does not work yet returns 0.
            // The hard-coded value is correct for the Windows L&F.

            h = d1.height - 130;
        } // end if

        //System.out.println("Setting scrollpane size to: " + w + " x " + h);
        sp.setSize(w, h);
        pack();      // to eliminate a possible horiz scrollbar.
        validate();  // to get the resize to kick in.
    } // end resetSize

    public void actionPerformed(ActionEvent evt) {
        int pagenum;

        if (evt.getSource() == pageNumberField) { // Set the page.
            try {
                pagenum = Integer.parseInt(pageNumberField.getText());
                if ((pagenum > 0) && (pagenum <= Pages)) {
                    currentPage = pagenum;
                    preview.setPage(pagenum - 1);
                } else {
                    pageNumberField.setText(String.valueOf(currentPage));
                } // end if
            } catch (NumberFormatException nfe) {
                pageNumberField.setText(String.valueOf(currentPage));
            } // end try/catch
        } else if (evt.getSource() == prevBtn) {
            if (currentPage > 1) {
                currentPage--;
                pageNumberField.setText(String.valueOf(currentPage));
                preview.setPage(currentPage - 1);
            } // end if
        } else if (evt.getSource() == nextBtn) {
            if (currentPage < Pages) {
                currentPage++;
                pageNumberField.setText(String.valueOf(currentPage));
                preview.setPage(currentPage - 1);
            } // end if
        } else if (evt.getSource() == printBtn) {
            // Print the file.
            Properties prop = new Properties();
            // System.out.println("Properties before: " + prop);
            PrintJob pj = Toolkit.getDefaultToolkit()
                    .getPrintJob(parentFrame, filename, prop);

            if (pj != null) {
                Graphics pjG = pj.getGraphics();
                // System.out.println("Properties after: " + prop); NO CHANGE.

                int i = 0;
                while (preview.setPage(i++)) {
                    if (pjG == null) pjG = pj.getGraphics();
                    pjG.setColor(Color.black);
                    pjG.setPaintMode();
                    preview.drawit(pjG);
                    // System.out.println("Sending page " + i + " to the printer");
                    //pjG.dispose(); // Sends the page to the printer.
                    pjG = null;
                } // end for i
                pj.end();  // Sends pages to the printer, does cleanup, frees resources.
            } // end if
            preview.setPage(currentPage - 1);
        } else if (evt.getSource() == closeBtn) {
            dispose();
        } // end if/else...
    } // end ActionPerformed

    public void itemStateChanged(ItemEvent ie) {
        Choice source = (Choice) ie.getSource();
        String ptsize = source.getSelectedItem();
        currentPtSize = Integer.parseInt(ptsize);
        preview.setFont(new Font("Monospaced", Font.PLAIN, currentPtSize));
    } // end itemStateChanged

    // A formatter for a filename specifier - drop off the path
    //   prefix and/or '.dump', if present.
    public String prettyName(String s) {
        int i;

        // Changes made here for the file-specific code dereferencing refactoring effort.
        // But this class is not currently in use (5 Apr 2022); more refactoring here is probably needed but
        //   not worth the effort while its future is TBD.
        // char slash = File.separatorChar;
        String slash = "\\\\";

        i = s.lastIndexOf(slash);
        if (i != -1) {
            s = s.substring(i + 1);
        } // end if

        i = s.lastIndexOf(".dump");
        if (i == -1) return s;
        return s.substring(0, i);
    } // end prettyName

    // Called by TextPage
    public void fontChanged() {
        Pages = preview.getPageCount();
        l1.setText("of " + Pages);
        if (currentPage > Pages) {
            currentPage = Pages;
            pageChanged();
            pageNumberField.setText(String.valueOf(currentPage));
            preview.setPage(currentPage - 1);
        } // end if
        resetSize();
    } // end fontChanged

    public void pageChanged() {
        prevBtn.setEnabled(currentPage != 1);
        nextBtn.setEnabled(currentPage != Pages);
    } // end pageChanged

    public void setOptions(boolean b1, boolean b2, boolean b3) {
        preview.setOptions(b1, b2, b3);
    } // end setOptions

    //---------------------------------------------------------
    // WindowListener methods
    //---------------------------------------------------------
    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        dispose();
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }
    //---------------------------------------------------------

    public static void main(String[] args) {
        if (args.length == 1) {
            new TextFilePrinter(args[0]);
        } else {
            System.err.println("Usage: java TextFilePrinter <text file>");
        } // end if/else
    } // end main
} // end class TextFilePrinter

