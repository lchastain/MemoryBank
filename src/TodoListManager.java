/* ***************************************************************************
 *
 * File:  $Id: TodoListManager.java,v 1.1 2006/02/20 01:11:11 lee Exp $
 *
 * Author:  D. Lee Chastain
 *
 * $Log: TodoListManager.java,v $
 * Revision 1.1  2006/02/20 01:11:11  lee
 * New file in support of the JTree version of the log application.
 *
 ****************************************************************************/
/**
 This custom component provides an alternative to a FileChooser.  It
 does not allow navigating, shows current selections, and provides
 a mechanism to switch between add, select/deselect, rename and
 delete operations.
 */

/* HERE - What needs work:

The centerPanel should scroll if contents get too large to view.
   Currently, preferred size of the entire component gets larger, and if it
   is contained in a scrollPane then THAT one gets the scrollbar which is
   not right since you lose the view of the north and/or south panels.

   possibly need to set a preferred size for the centerpanel.
   need calculations at resize about number of columns.

*/

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

public final class TodoListManager extends JPanel {
    static final long serialVersionUID = 3055425722408255576L;

    public static final int SELECT_MODE = 6;
    public static final int ADD_MODE = 7;
    public static final int RENAME_MODE = 8;
    public static final int DELETE_MODE = 9;

    private static final int MAX_FILENAME_LENGTH = 32;
    private static String ems;  // Error Message String

    private int containerWidth;
    private JScrollPane centerPanel;
    private Vector<String> selections;
    private Vector<String> results;
    private int theMode;
    private JPanel northPanel;
    private JTextField newNameField;
    private JButton doit;
    private JLabel sayit;

    public TodoListManager(Vector<String> s, final ActionListener aa) {
        this(s, aa, SELECT_MODE);
    } // end of the 2-parameter constructor


    public TodoListManager(Vector<String> s, final ActionListener aa, int m) {
        super(new BorderLayout()); // Will use North, Center, South.
        selections = new Vector<String>(s); // Alternative to 'clone'.
        results = s;      // This is how data gets back to the mother ship.
        setOpaque(true);
        containerWidth = 0;

        MemoryBank.dbg("TodoListManager: number of preselected items =");
        MemoryBank.debug(" " + selections.size());
        for (String presel : selections) {
            MemoryBank.debug("   " + presel);
        } // end for

        // Create the three main panels for this component.
        northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        centerPanel = new JScrollPane();
        JPanel southPanel = new JPanel(new BorderLayout());

        // Create the 'New List Name' text control.
        newNameField = new JTextField(MAX_FILENAME_LENGTH);
        AbstractDocument doc = (AbstractDocument) newNameField.getDocument();
        doc.setDocumentFilter(new FixedSizeFilter(MAX_FILENAME_LENGTH));

        // Add the two 'North' components.
        northPanel.add(new JLabel("New List Name:"));
        northPanel.add(newNameField);

        doit = new JButton("Apply");
        sayit = new JLabel("x", JLabel.CENTER);
        sayit.setOpaque(true);
        JButton changeit = new JButton("Cycle Mode");
        southPanel.add(doit, "West");
        southPanel.add(sayit, "Center");
        southPanel.add(changeit, "East");

        // Note the way the 'aa' parameter is used below.  We don't
        //   actually want to add it as an actionListener because we
        //   want the ones here to always kick in first and then based
        //   on those results, the higher level handler may not even
        //   be called.

        newNameField.addActionListener(
                new Akshun() {
                    public void actionPerformed(ActionEvent e) {
                        doApply();
                        if (results.size() > 0)
                            if (aa != null) aa.actionPerformed(e);
                    }
                }
        );

        doit.addActionListener(
                new Akshun() {
                    public void actionPerformed(ActionEvent e) {
                        doApply();
                        if ((results.size() > 0) || (theMode == SELECT_MODE))
                            if (aa != null) aa.actionPerformed(e);
                    }
                }
        );

        changeit.addActionListener(
                new Akshun() {
                    public void actionPerformed(ActionEvent e) {
                        theMode++;
                        if (theMode > DELETE_MODE) theMode = SELECT_MODE;
                        if (makeFileList() == 0) theMode = ADD_MODE;
                        setModeDecor();
                    } // end actionPerformed
                }
        );

        // Add the three panels to their proper locations.
        add(northPanel, "North");
        add(centerPanel, "Center");
        add(southPanel, "South");

        // Start off in the user-specified mode.
        theMode = m;

        // Unless there are no files to manage.
        if (makeFileList() == 0) theMode = ADD_MODE;

        // Note: the call to makeFileList above is required, even if we
        //   do not use its return value.

        setModeDecor();

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                // Note - Do NOT call getPreferredSize from here;
                //   it will produce loops and undetermined results.

                // Get the size of the space this component has been allotted.
                Dimension d = getSize();
                containerWidth = d.width;
                // MemoryBank.debug("TodoListManager - componentResized to: " + d);

                if (containerWidth == 0) return;

                scaleImage();
            } // end componentResized
        });

    } // end constructor


    private void doApply() {
        String newName = newNameField.getText().trim();

        results.removeAllElements();
        // unlike clear, this also sets the vector size to 0.

        // For all modes except ADD, the toggle button selection(s),
        //   if any, are captured and made part of the results.
        if (theMode != ADD_MODE) {
            JPanel jp = (JPanel) centerPanel.getViewport().getView();
            int numLists = jp.getComponentCount();
            JToggleButton jtb;
            for (int i = 0; i < numLists; i++) {
                jtb = (JToggleButton) jp.getComponent(i);
                if (jtb.isSelected()) {
                    results.addElement(jtb.getText());
                } // end if
            } // end for i
        } // end if

        // Now mode-specific processing.

        switch (theMode) {
            case SELECT_MODE:
                // Adopt the new selections in case the user now wants to change modes
                //   and continue working with the files.
                selections = new Vector<String>(results); // Alternative to 'clone'.
                makeFileList(); // To recolor backgrounds.
                break;
            case ADD_MODE:
                if (nameCheck(newName, this)) results.addElement(newName);
                break;
            case RENAME_MODE:
                if (results.size() != 1) {
                    JOptionPane.showMessageDialog(this, "No List to rename was selected!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } // end if

                if (!nameCheck(newName, this)) {
                    // The proposed new name is not viable and the user
                    //   has already been informed via error dialog.
                    // Do not bother the controlling context.
                    results.clear();
                    return;
                } // end if

                // Add the proposed new name to the return Vector.
                results.addElement(newName);

                String oldName = results.elementAt(0);

                // Check for old == new
                if (oldName.equals(newName)) {
                    ems = "The list you have selected already has this name.\n";
                    ems += "  Rename operation cancelled.";
                    JOptionPane.showMessageDialog(this, ems,
                            "Error", JOptionPane.ERROR_MESSAGE);
                    results.clear();
                    return;
                } // end if

                // Check to see if the destination file name already exists.
                // If so then complain and refuse to do the rename.
                String newNamedFile = MemoryBank.userDataDirPathName + File.separatorChar;
                newNamedFile += newName + ".todolist";

                if ((new File(newNamedFile)).exists()) {
                    ems = "A list named " + newName + " already exists!\n";
                    ems += "  Rename operation cancelled.";
                    JOptionPane.showMessageDialog(this, ems,
                            "Error", JOptionPane.ERROR_MESSAGE);
                    results.clear();
                    return;
                } // end if

                // Now attempt the rename
                String oldNamedFile = MemoryBank.userDataDirPathName + File.separatorChar;
                oldNamedFile += oldName + ".todolist";
                File f = new File(oldNamedFile);

                ems = "";
                try {
                    if (f.renameTo(new File(newNamedFile))) {
                        // Redisplay the lists to show the new name.
                        makeFileList();
                        setModeDecor();
                    } else {
                        throw new Exception("Unable to rename " + oldName);
                    } // end if
                } catch (SecurityException se) {
                    ems += se.getMessage() + "\n";
                } catch (Exception ue) {  // User Exception
                    ems += ue.getMessage() + "\n";
                } // end try/catch/finally

                // Handle Exception(s), if any.
                if (!ems.equals("")) {
                    JOptionPane.showMessageDialog(this, ems,
                            "Error", JOptionPane.ERROR_MESSAGE);
                    results.clear(); // do not bother the controlling context.
                    return;
                } // end if

                // Look for the original name in the current selections.  If
                //   found then add a 'no' to indicate that the rename
                //   operation is (probably) not complete.  Otherwise, yes.
                for (String s : selections) {
                    if (s.equals(oldName)) {
                        results.addElement("no");
                        return;
                    } // end if
                } // end for each selection
                results.addElement("yes");

                break;
            case DELETE_MODE:
                if (results.size() == 0) break; // No selections.

                // Ask for confirmation for the entire list.
                ems = "You have requested deletion of the following To Do ";
                if (results.size() == 1) ems += "List:\n";
                else ems += "Lists:\n";
                for (String s : results) {
                    ems += "  " + s + "\n";
                } // end for each selection
                ems += "\nThis operation cannot be undone.  Are you sure?";

                int doit = JOptionPane.showConfirmDialog(this, ems,
                        "Warning", JOptionPane.YES_NO_OPTION);

                // If no then clear the results.
                if (doit != JOptionPane.YES_OPTION) {
                    results.removeAllElements();
                    break;  // By leaving now, we leave the items still selected.
                } // end if

                // Capture the user's selections as a 'wish' list rather than
                //   an accomplished fact of successful deletions.
                Vector<String> deletions = new Vector<String>(results);

                results.removeAllElements();
                // All, some, or none may be deleted, depending on permissions
                // and Exceptions.  We only populate the results if the deletion
                // succeeded.

                // Note that we CANNOT simply assign 'results' to some new Vector
                //   because it is our only pointer back to the controlling context.

                // Delete the files -
                ems = "";
                for (String s : deletions) {
                    String deleteFile = MemoryBank.userDataDirPathName + File.separatorChar + s + ".todolist";
                    MemoryBank.debug("Deleting " + deleteFile);
                    try {
                        if ((new File(deleteFile)).delete()) { // Delete the file.
                            results.addElement(s);
                        } else {
                            throw new Exception("Unable to delete " + s);
                        } // end if
                    } catch (SecurityException se) {
                        ems += se.getMessage() + "\n";
                    } catch (Exception ue) {  // User Exception
                        ems += ue.getMessage() + "\n";
                    } // end try/catch/finally

                } // end for each selection

                // Handle Exception(s), if any.
                if (!ems.equals("")) {
                    JOptionPane.showMessageDialog(this, ems,
                            "Error", JOptionPane.ERROR_MESSAGE);
                } // end if

                // Get/display a new file list, clear delete selections even if
                //   the files are still here.
                if (makeFileList() == 0) {
                    theMode = ADD_MODE;
                    setModeDecor();
                } // end if

                break;
        } // end switch
    } // end doApply


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
    //------------------------------------------------------------------
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width = 360;  // Wide enough for single-line northPanel
        return d;
    } // end getPreferredSize


    //------------------------------------------------------------------
    // Method Name:  makeFileList
    //
    //------------------------------------------------------------------
    private int makeFileList() {

        // Get a list of To Do lists in the user's data directory.
        File dataDir = new File(MemoryBank.userDataDirPathName);
        String[] theFileList = dataDir.list(
                new FilenameFilter() {
                    // Although this filter does not account for directories, it is
                    // known that the 'MemoryBank.location' will not under normal program
                    // operation contain any directory ending in '.todolist'.
                    public boolean accept(File f, String s) {
                        int max = new String(".todolist").length() + MAX_FILENAME_LENGTH;
                        return s.endsWith(".todolist") &&
                                s.length() < max;
                    }
                }
        );

        // Calculate the best layout variables.
        int cols = 3;
        if (theFileList.length < 40) cols = 2;
        if (theFileList.length < 20) cols = 1;
        JPanel theFilesPanel = new JPanel(new TopBottomGridLayout(0, cols));

        // Create the list of files to present to the user.
        int theDot;
        String theFile;
        JToggleButton toglButn;
        boolean thisFileSelected;
        MemoryBank.debug("Number of files found: " + theFileList.length);
        ButtonGroup bg = new ButtonGroup();
        for (int i = 0; i < theFileList.length; i++) {
            thisFileSelected = false;
            theDot = theFileList[i].lastIndexOf(".todolist");
            theFile = theFileList[i].substring(0, theDot);
            for (String s : selections) {
                if (s.equalsIgnoreCase(theFile)) {
                    thisFileSelected = true;
                    break;
                } // end if
            } // end for each selection

            if (theMode == RENAME_MODE) { // For Rename
                toglButn = new JRadioButton(theFile);
                if (thisFileSelected) {
                    toglButn.setBackground(Color.cyan);
                } // end if
                bg.add(toglButn);
            } else if (theMode != DELETE_MODE) { // For Add or Select/Deselect
                toglButn = new JCheckBox(theFile);
                if (theMode == ADD_MODE) toglButn.setEnabled(false);
                if (thisFileSelected) {
                    toglButn.setSelected(true);
                    if (theMode == ADD_MODE) {
                        toglButn.setBackground(Color.white);
                    } else {
                        toglButn.setBackground(Color.green);
                    } // end if
                } // end if
            } else { // For Deletions
                toglButn = new JCheckBox(theFile);
                if (thisFileSelected) {
                    toglButn.setBackground(Color.red);
                } // end if

            } // end if

            theFilesPanel.add(toglButn);
        } // end for

        centerPanel.setViewportView(theFilesPanel);
        return theFileList.length;
    } // end makeFileList


    //-------------------------------------------------------------------
    // Method Name:  nameCheck
    //
    // Check for the following 'illegal' file naming conditions:
    //   No entry, or whitespace only.
    //   The name ends in '.todolist'.
    //   The name contains more than MAX_FILENAME_LENGTH chars.
    //   The filesystem refuses to create a file with this name.
    //   User does not have required permissions.
    //
    // Return Value - false if file name should not be allowed;
    //    otherwise, true.  Also, any 'false' return will be
    //    preceeded by an informative error dialog.
    //-------------------------------------------------------------------
    public static boolean nameCheck(String theName, Component parent) {
        Exception e = null;
        ems = "";

        theName = theName.trim();

        // Check for non-entry (or evaporation).
        if (theName.equals("")) {
            ems = "No New List Name was supplied!";
        } // end if

        // Refuse unwanted help.
        if (theName.endsWith(".todolist")) {
            ems = "The name you supply cannot end in '.todolist'";
        } // end if

        // Check for legal max length.
        // The input field used in this class would not allow this one, but
        //   since this method is static and accepts input from outside
        //   sources, this check should be done.
        if (theName.length() > MAX_FILENAME_LENGTH) {
            ems = "The new name is limited to " + MAX_FILENAME_LENGTH + " characters";
        } // end if

        if (!ems.equals("")) {
            JOptionPane.showMessageDialog(parent, ems,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } // end if

        // Check to see if a file with this name already exists?
        //   No; for an 'add' the AppTree can handle that situation
        //   by simply opening it, a kind of back-door selection.

        // Note - I thought it would be a good idea to check for 'illegal'
        //   characters in the filename, but when I started testing, the
        //   Windows OS accepted %, -, $, ! and &; not sure what IS illegal.
        //   Of course there ARE illegal characters, and the ones above may
        //   also be illegal on another OS.  So, the best way to
        //   detect them is to try to create a file using the name we're
        //   checking.  Any io error and we can fail this check.

        // Now try to create the file, with a '.test' extension; this will not
        // conflict with any 'legal' existing file in this directory, so if
        // there is any problem at all then we can report a failure.

        String theFilename = MemoryBank.userDataDirPathName + File.separatorChar + theName + ".test";
        File f = new File(theFilename);
        MemoryBank.debug("Name checking new file: " + f.getAbsolutePath());
        try {
            f.delete();
            // If the file does not already exist, this simply returned a false.
            // If it did already exist then we must consider how it got there and
            // that this attempt to delete will quite probably throw a Security
            // Exception. But if it does not then the file is gone now so we go on.

            f.createNewFile();
            // We didn't test the return value here because anything short of an
            // Exception means that (thanks to the previous delete) the value is
            // 'true' and we just now created a file with the specified name.

            // The above call would have worked even without the previous delete,
            // but in the case of a preexisting file we would not have been sure
            // that we had overcome any possible security exception.  Since we
            // know that we just now created this file, we also know that it is
            // writable and do not need to check 'canWrite'.

            f.delete(); // So, delete the test file; name test passed.
        } catch (IOException ioe) {
            e = ioe;  // Identify now, handle below.
        } catch (SecurityException se) {
            e = se;  // Identify now, handle below.
        } // end try/catch

        // Handle Exceptions, if any.
        if (e != null) {
            JOptionPane.showMessageDialog(parent, e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } // end if

        return true;
    } // end nameCheck


    //Keeping this mechanism for now, in case there is a need to adjust
// number of columns.
    public void scaleImage() {
        if (containerWidth == 0) return;  // Cannot scale until we know our bounds.

        //repaint();
    } // end scaleImage


    private void setModeDecor() {
        switch (theMode) {
            case SELECT_MODE:
                sayit.setBackground(Color.green);
                sayit.setText("SELECT Mode");
                northPanel.setVisible(false);
                doit.setActionCommand("Select");
                break;
            case ADD_MODE:
                sayit.setText("ADD Mode");
                sayit.setBackground(Color.white);
                northPanel.setVisible(true);
                newNameField.setActionCommand("Add");
                newNameField.setText("");
                doit.setActionCommand("Add");
                break;
            case RENAME_MODE:
                sayit.setText("RENAME Mode");
                sayit.setBackground(Color.cyan);
                northPanel.setVisible(true);
                newNameField.setActionCommand("Rename");
                newNameField.setText("");
                doit.setActionCommand("Rename");
                break;
            case DELETE_MODE:
                sayit.setBackground(Color.red);
                sayit.setText("DELETE Mode");
                northPanel.setVisible(false);
                doit.setActionCommand("Delete");
                break;
        } // end switch
    } // end setModeDecor


    //------------------------------------------------------------------------
    // Inner classes

    //------------------------------------------------------------------------
    // Class Name:  FixedSizeFilter
    //
    // Apply this filter to a TextField to limit the number of characters
    //   the user is allowed to type in.
    //------------------------------------------------------------------------
    static class FixedSizeFilter extends DocumentFilter {
        int maxSize;

        // limit is the maximum number of characters allowed.
        public FixedSizeFilter(int limit) {
            maxSize = limit;
        }

        // This method is called when characters are inserted into the document
        public void insertString(DocumentFilter.FilterBypass fb, int offset,
                                 String str, AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, str, attr);
        }

        // This method is called when characters in the document are replaced
        //    with other characters
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length,
                            String str, AttributeSet attrs) throws BadLocationException {
            int newLength = fb.getDocument().getLength() - length + str.length();
            if (newLength <= maxSize) {
                fb.replace(offset, length, str, attrs);
            } else {
                // This does not print a stacktrace; instead it beeps.
                throw new BadLocationException(
                        "New characters exceeds max size of document", offset);
            }
        }
    } // end FixedSizeFilter


    //------------------------------------------------------------------------
    // Class Name:  TopBottomGridLayout
    //
    // A custom layout manager that fills every row in column 1 first,
    //   then goes on to fill every row in column 2, column 3, etc.
    //------------------------------------------------------------------------
    static class TopBottomGridLayout extends GridLayout {
        private static final long serialVersionUID = 5122229695909591486L;

        public TopBottomGridLayout(int rows, int cols) {
            super(rows, cols);
        }

        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets insets = parent.getInsets();
                int ncomponents = parent.getComponentCount();
                int nrows = getRows();
                int ncols = getColumns();

                if (ncomponents == 0) return;

                if (nrows > 0) {
                    ncols = (ncomponents + nrows - 1) / nrows;
                } else {
                    nrows = (ncomponents + ncols - 1) / ncols;
                }
                int w = parent.getWidth() - (insets.left + insets.right);
                int h = parent.getHeight() - (insets.top + insets.bottom);
                int hGap = getHgap();
                int vGap = getVgap();
                w = (w - (ncols - 1) * hGap) / ncols;
                h = (h - (nrows - 1) * vGap) / nrows;
                int compNum = 0;

                for (int c = 0, x = insets.left; c < ncols; c++, x += w + hGap) {
                    for (int r = 0, y = insets.top; r < nrows; r++, y += h + vGap) {
                        if (compNum < ncomponents)
                            parent.getComponent(compNum++).setBounds(x, y, w, h);
                        else break;
                    } // end for r
                } // end for c
            } // end synchronized
        } // end layoutContainer
    } // end class TopBottomGridLayout


    //-----------------------------------------------------------------
    // Test driver for the class.
    //-----------------------------------------------------------------
    public static void main(String args[]) {
        MemoryBank.debug = true;
        final Vector<String> selections = new Vector<String>(2);

        Akshun aa = new Akshun() {
            public void actionPerformed(ActionEvent e) {
                MemoryBank.debug("TodoListManager: " + e.getActionCommand());
                for (String s : selections) {
                    MemoryBank.debug("  " + s);
                }
            }
        };

        TodoListManager tlc = new TodoListManager(selections, aa);

        // Make the frame and add ourselves to it.
        JFrame testFrame = new JFrame("TodoListManager Test");
        testFrame.getContentPane().add(tlc);
        testFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        testFrame.addWindowListener(
                new WindowAdapter() {
                    public void windowClosing(WindowEvent we) {
                        System.exit(0);
                    } // end windowClosing
                } // end new WindowAdapter
        );

        // Center the Frame in the available screen area
        testFrame.pack();
        testFrame.setLocationRelativeTo(null);

        testFrame.setVisible(true);

    } // end main

} // end class TodoListManager 


// This class provided as an ActionListener Adapter.
class Akshun implements ActionListener {
    public void actionPerformed(ActionEvent e) {
    } // end actionPerformed
} // end class Akshun
