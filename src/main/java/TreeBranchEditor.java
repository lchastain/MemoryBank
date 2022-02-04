import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Enumeration;

public class TreeBranchEditor extends JPanel
        implements ItemListener, ActionListener, TreeModelListener {
    static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(TreeBranchEditor.class);

    private final JScrollPane leftScroller;
    private final JScrollPane rightScroller;

    private final DefaultMutableTreeNode origBranch;
    private DefaultMutableTreeNode myBranch;
    private final BranchHelperInterface myHelper;
    private ArrayList<String> theChoices;
    private ArrayList<String> removals = new ArrayList<>();
    private BranchEditorModel bem;
    static AppIcon theTrash;
    static AppIcon theSafe;
    int theScrollPosition;

    static {
        Image tmpImg;
        theTrash = new AppIcon("images/waste.gif");
        tmpImg = theTrash.getImage();
        tmpImg = tmpImg.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
        theTrash.setImage(tmpImg);

        theSafe = new AppIcon("images/safe1.ico");
        tmpImg = theSafe.getImage();
        tmpImg = tmpImg.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
        theSafe.setImage(tmpImg);
    }


    private ArrayList<NodeChange> changeList;
    // The changeList keeps an ongoing list of cumulative changes, that the helper receives
    // when the user ends the editing session by clicking 'Apply'.  Of course we could just
    // let the user figure out what happened by comparing the final tree with the original,
    // but that seems a bit harsh given the complexities of the logic needed to respond to
    // an editor control that allows multiple diverse and sometimes conflicting actions.
    // So - we keep a list of every single change, even when the later ones have the effect
    // of cancelling out one or more earlier ones.
    // The one exception to this is the REMOVED action, which only goes at the end of the
    // list, not to capture a user action but as an indicator that an item's final state
    // was MARKED (for removal).

    // Logical considerations for the handler of the changeList:
    // 'added' to the branch is the same as SELECTED from the list, and 'removed' from the
    // branch is the same action as DESELECTED from the list, but we don't distinguish; just
    // use SELECTED/DESELECTED because it is a better description of the action taken by the user.
    // MARKED has the same visual effect as DESELECTED from the tree, but it also affects
    // the selection list beyond just toggling the checkmark.
    // To simplify things, we rely on the restrictions built into the editor UI to
    // prevent many of the illogical cases.
    // Although we do report a MOVED node, we do not distinguish whether or not it created or
    // eliminated a 'parent' node in the process, nor do we say where it was moved to.  If you
    // need that info you will have to interpolate it, given the original and final tree along
    // with the changeList.

    public TreeBranchEditor(String theTitle, TreeNode tn, BranchHelperInterface branchHelperInterface) {
        super(new BorderLayout());
        origBranch = (DefaultMutableTreeNode) tn;
        myBranch = deepClone(origBranch);
        myHelper = branchHelperInterface;
        changeList = new ArrayList<>(); // Array of NodeChange

        theChoices = getChoices();

        log.debug("Number of preselected items: " + myBranch.getChildCount());

        JSplitPane centerPanel = new JSplitPane();
        JPanel southPanel = new JPanel(new BorderLayout());

        JButton doApply = new JButton("Apply");
        doApply.addActionListener(this);

        JButton doCancel = new JButton("Cancel");
        doCancel.addActionListener(this);

        southPanel.add(doApply, "West");
        southPanel.add(doCancel, "East");

        leftScroller = new JScrollPane();
        rightScroller = new JScrollPane();

        showTree();     // Sets the content of the leftScroller
        showChoices();  // Sets the content of the rightScroller

        // We set the preferred vertical sizes to something unrealistically low, so that when the
        // interface is loaded and it comes out larger, the vertical scroll pane kicks in.
        Dimension d1 = leftScroller.getPreferredSize();
        Dimension d2 = rightScroller.getPreferredSize();
        leftScroller.setPreferredSize(new Dimension(d1.width + 20, 100));
        leftScroller.revalidate();
        rightScroller.setPreferredSize(new Dimension(d2.width, 100));
        rightScroller.revalidate();

        // Remember that centerPanel is a split pane, in the center of a BorderLayout.
        // The left side will always contain either the same elements or fewer, than the
        //   right.  Further, the ones on the left require less height.  So - you might
        //   think that there is no need to provide a separate scroller for the left side.
        //   But that is wrong, in the case where the list of leaves is so long that when
        //   you scroll to the bottom of that list, if there was only one scroller that
        //   also scrolled the tree then you might not see ANY portion of the tree on the
        //   left side of the split pane.
        centerPanel.setLeftComponent(leftScroller);
        centerPanel.setRightComponent(rightScroller);

        setInfoPanel(theTitle);
        add(centerPanel, "Center");
        add(southPanel, "South");
    } // end constructor

    // If the helper's getChoices() has not been implemented,
    // then make our own, from the provided branch.
    @SuppressWarnings("rawtypes") // Adding a type then causes 'unchecked' problem.
    private ArrayList<String> getChoices() {
        theChoices = myHelper.getChoices();
        if (theChoices != null) return theChoices;

        // Create list of choices from the existing branch.
        theChoices = new ArrayList<>();
        String theRoot = origBranch.toString();
        Enumeration dfe = origBranch.depthFirstEnumeration();
        while (dfe.hasMoreElements()) {
            DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) dfe.nextElement();
            if (!dmtn.toString().equals(theRoot))
                theChoices.add(dmtn.toString());
        }
        return theChoices;
    }

    private void showTree() {
        bem = new BranchEditorModel(myBranch, false);
        bem.addTreeModelListener(this);
        JTree jt = new JTree(bem);
        jt.setShowsRootHandles(false);
        jt.setEditable(true);

        jt.setDragEnabled(true);
        jt.setDropMode(DropMode.ON_OR_INSERT);
        jt.setTransferHandler(new TreeTransferHandler(myHelper.makeParents()));
        jt.getSelectionModel().setSelectionMode(
                TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        expandTree(jt);
        leftScroller.setViewportView(jt);
    } // end showTree

    @SuppressWarnings("rawtypes") // Adding a type then causes 'unchecked' problem.
    private void expandTree(JTree tree) {
        DefaultMutableTreeNode root =
                (DefaultMutableTreeNode) tree.getModel().getRoot();
        Enumeration e = root.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) e.nextElement();
            if (node.isLeaf()) continue;
            int row = tree.getRowForPath(new TreePath(node.getPath()));
            tree.expandRow(row);
        }
    }

    public void setInfoPanel(String theTitle) {
        if (theTitle == null) return;
        Box myBox = new Box(BoxLayout.Y_AXIS);

        JLabel theHeading = new JLabel(("  Tree Branch Editor  "));
        theHeading.setFont(Font.decode("Serif-bold-20"));
        myBox.add(theHeading);
        myBox.add(new JLabel(" "));
        myBox.add(new JLabel(" "));
        JLabel titleLabel = new JLabel(" " + theTitle);
        titleLabel.setFont(Font.decode("Serif-bold-14"));
        myBox.add(titleLabel);
        myBox.add(new JLabel(" "));
        myBox.add(new JLabel(" On the tree:"));
        myBox.add(new JLabel(" F2 on a selected node, to rename"));
        myBox.add(new JLabel(" You can drag & drop nodes"));
        myBox.add(new JLabel(" "));
        myBox.add(new JLabel(" On the list:"));
        myBox.add(new JLabel(" Select and deselect nodes"));
//        myBox.add(new JLabel(" Recycle button to remove"));
//        myBox.add(new JLabel("  (only after 'Apply')"));
        myBox.add(new JLabel(" "));
        myBox.add(new JLabel(" Click 'Apply' to accept your changes  "));
        myBox.add(new JLabel(" Click 'Cancel' to undo your changes  "));
        add(myBox, "West");
    }


    @SuppressWarnings("rawtypes")
    private void showChoices() {
        // Create lists of pre-selected items from the existing branch.
        Enumeration dfe = myBranch.depthFirstEnumeration();
        ArrayList<String> branches = new ArrayList<>();
        ArrayList<String> leaves = new ArrayList<>();
        while (dfe.hasMoreElements()) {
            DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) dfe.nextElement();
            String name = dmtn.toString();
            if (dmtn.isLeaf()) leaves.add(name);
            else branches.add(name);
        }

        JPanel theChoicesPanel = new JPanel();
        theChoicesPanel.setLayout(new BoxLayout(theChoicesPanel, BoxLayout.Y_AXIS));

        // To the right-side panel, add the selection choices.
        for (String theChoice : theChoices) {
            JCheckBox jcb = new JCheckBox(theChoice);
            if (leaves.contains(theChoice)) {
                jcb.setSelected(true);
            } else if (branches.contains(theChoice)) {
                jcb.setSelected(true);
                jcb.setEnabled(false);
            }
            jcb.addItemListener(this);

            // Add a simple checkbox plus a 'delete' control.
            // A previous solution here would allow for 'no' delete button, which
            // necessitated the use of a BoxLayout for theChoicesPanel.  That layout may no longer be
            // required but can stay as long as it continues to work.
            JPanel oneLine = new JPanel(new BorderLayout());
            oneLine.add(jcb, "West");
            JButton deleteButton = new JButton();
            deleteButton.setIcon(theTrash);
            if (myHelper.deleteAllowed(theChoice)) {
                deleteButton.setName(theChoice);
                deleteButton.setActionCommand("Delete");
                deleteButton.addActionListener(this);
            } else {
                // When delete is not allowed, better user experience to see a
                // disabled trash icon, than no button at all.  And - it helps
                // keep the row heights constant.
                deleteButton.setEnabled(false);
            }

            // Do not allow a branch to be removed.
            if (branches.contains(theChoice)) deleteButton.setEnabled(false);

            // 4 Feb 2022 - Disabling the ability to delete from this editor; use the menubar for that.
            // Reason:  Goals now has up to 4 associated files and the delete code here does not handle that case.
            //  Do not want to add Goal-specific code to this editor because that would not be industry standard,
            //  nor do I want to move the operations into the various Panels that use this editor and then have them
            //  implement the interface with overrides (two reasons here), because that would be a lot of work for
            //  just too little value-added, and secondly because what WAS working here was file-specific and since
            //  then the rest of the app has migrated over to using a DataAccessor, to allow alternatives (like a
            //  database) to solely filesystem-based operations.
            //oneLine.add(deleteButton, "East");
            if (removals.contains(theChoice)) {
                jcb.setEnabled(false);
                deleteButton.setIcon(theSafe);
                JLabel removalLabel = new JLabel("Marked for REMOVAL  ");
                removalLabel.setForeground(Color.red);
                removalLabel.setHorizontalAlignment(JLabel.RIGHT);
                oneLine.add(removalLabel, "Center");
            }
            int w = oneLine.getMaximumSize().width;
            int h = oneLine.getPreferredSize().height;
            oneLine.setMaximumSize(new Dimension(w, h)); // See above note.
            theChoicesPanel.add(oneLine);
        }
        rightScroller.setViewportView(theChoicesPanel);
        rightScroller.getVerticalScrollBar().setValue(theScrollPosition);
    } // end showChoices


    // A Listener for the checkboxes.
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        String theText = ((JCheckBox) source).getText();

        theScrollPosition = rightScroller.getVerticalScrollBar().getValue();
        if (e.getStateChange() == ItemEvent.SELECTED) {
            log.debug(theText + " selected");
            DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(theText, false);
            myBranch.add(dmtn);
            changeList.add(new NodeChange(theText, NodeChange.SELECTED));
        } else { // if (e.getStateChange() == ItemEvent.DESELECTED) {
            log.debug(theText + " deselected");
            remove(theText);
            changeList.add(new NodeChange(theText, NodeChange.DESELECTED));
        }
        showTree();  // Refresh the view on the left side of the editor.
    }

    // Remove a choice from the tree
    private void remove(String theLeafText) {
        DefaultMutableTreeNode tmpLeaf = myBranch.getFirstLeaf(); // This one not allowed to be removed.
        while (tmpLeaf != null) { // A bit inefficient, to start with one that we know will be skipped.
            String s = tmpLeaf.toString();
            if (s.equals(theLeafText)) break;
            tmpLeaf = tmpLeaf.getNextLeaf(); // This becomes null if we get to the end of the list.
        } // end while
        if (tmpLeaf == null) return; // Didn't find it.

        // We cannot just remove this leaf from myBranch; it may have been moved to a
        // lower level first during the current edit session, then removed.  Removing
        // it from whatever parent it now has, is the proper way to do it.
        // (although 'moving lower' is not possible in this application, but then again,
        //  this editor could be used in other apps, in which case this aside-comment shouldn't
        //  even be here.  Leaving it, until the editor finds a wider audience).
        log.debug("Removing: " + theLeafText);
        DefaultMutableTreeNode theParent = (DefaultMutableTreeNode) tmpLeaf.getParent();
        theParent.remove(tmpLeaf);
        showChoices(); // Needed, to possibly return a branch to being just a leaf.
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        String theAction = actionEvent.getActionCommand();
        JButton theSource = (JButton) actionEvent.getSource();
        String theText = theSource.getName();
        //log.debug(theAction);

        switch (theAction) {
            case "Cancel":
                myBranch = deepClone(origBranch);
                theChoices = getChoices();
                changeList = new ArrayList<>();
                removals = new ArrayList<>();
                theScrollPosition = 0;
                showTree();
                showChoices();
                break;
            case "Apply": // For selections, deselections, renames and removals
                for (String s : removals) {
                    changeList.add(new NodeChange(s, NodeChange.REMOVED));
                }
                myHelper.doApply(myBranch, changeList);
                // At this point there is not much use in leaving the editor displayed and
                // accessible to the user; in fact they may wreak havoc with another click
                // of any of the buttons that we handle here.  But for some reason, it seems
                // like a bad idea to try and idiot-proof it from here; that will be up to
                // the consumer of this tool.  Good luck and happy tree-trimming!
                break;
            case "Delete":
                // The action was a 'delete' button click, which is just a flag toggle in this context.
                // The changeList will grow with each click of this button but due to the binary nature
                //   of the state change, only the last one will 'count', when the changeList is finally
                //   evaluated upon the user clicking 'Apply'.
                theScrollPosition = rightScroller.getVerticalScrollBar().getValue();
                if (removals.contains(theText)) {
                    removals.remove(theText);
                    changeList.add(new NodeChange(theAction, NodeChange.UNMARKED));
                } else {
                    removals.add(theText);
                    remove(theText);  // Remove from the tree.
                    changeList.add(new NodeChange(theText, NodeChange.MARKED));
                }
                showTree();
                showChoices();
                break;
        }
    }

    @SuppressWarnings("rawtypes") // Adding a type then causes 'unchecked' problem.
    private DefaultMutableTreeNode deepClone(DefaultMutableTreeNode root) {
        DefaultMutableTreeNode newRoot = (DefaultMutableTreeNode) root.clone();
        for (Enumeration childEnum = root.children(); childEnum.hasMoreElements(); ) {
            newRoot.add(deepClone((DefaultMutableTreeNode) childEnum.nextElement()));
        }
        return newRoot;
    }

    // The rename of a node in the tree will kick off this event.
    @Override
    public void treeNodesChanged(TreeModelEvent treeModelEvent) {
        DefaultMutableTreeNode node;
        node = (DefaultMutableTreeNode)
                (treeModelEvent.getTreePath().getLastPathComponent());

        // If the event lists children then the changed node is the child of that one.
        // Otherwise, the changed node is the one we already have.
        try {
            int index = treeModelEvent.getChildIndices()[0];
            node = (DefaultMutableTreeNode)
                    (node.getChildAt(index));
        } catch (NullPointerException ignore) {
        }

        String renamedTo = node.toString();
        String renamedFrom = bem.getOriginalName();
        //log.debug(renamedFrom + " was renamed to " + renamedTo);
        doRename(renamedFrom, renamedTo);
        changeList.add(new NodeChange(renamedFrom, renamedTo));
    } // end treeNodesChanged

    // This method is called after a rename has been done on the JTree UI,
    // in order to keep the selections in line with the new text.
    private void doRename(String renamedFrom, String renamedTo) {
        for (int i = 0; i < theChoices.size(); i++) {
            String s = theChoices.get(i);
            if (s.equals(renamedFrom)) {
                theChoices.set(i, renamedTo);
                break;
            }
        }
        showChoices();
    }

    // When one or more choices is moved to another, if the drop target is not already
    // a 'parent', it will become one at that time.  When that happens, we no longer
    // want to handle deselection or deletion events for that node.  But that logic
    // is already implemented in the showChoices method; all we need to do from here
    // is to invoke it.
    @Override
    public void treeNodesInserted(TreeModelEvent treeModelEvent) {
        //log.debug(treeModelEvent.toString());
        DefaultMutableTreeNode node;
        node = (DefaultMutableTreeNode)
                (treeModelEvent.getTreePath().getLastPathComponent());

        // If the event lists children then the changed node is the child of that one.
        // Otherwise, the changed node is the one we already have.
        try {
            int index = treeModelEvent.getChildIndices()[0];
            node = (DefaultMutableTreeNode)
                    (node.getChildAt(index));
        } catch (NullPointerException ignore) {
        }

        changeList.add(new NodeChange(node.toString(), NodeChange.MOVED));
        theScrollPosition = rightScroller.getVerticalScrollBar().getValue();
        showChoices();
    }

    // This event fires when a leaf (node) is dragged away from its branch (parent).
    // For our purposes, if this was the last child of the parent (thereby turning
    // that parent back into a leaf), we can restore the DESELECT and DELETE
    // functionalities of that leaf by simply redisplaying the list of choices.
    // Note that this event is NOT fired when a node is removed programmatically;
    // for example, in the 'remove' method.
    @Override
    public void treeNodesRemoved(TreeModelEvent treeModelEvent) {
        //log.debug(treeModelEvent.toString());
        theScrollPosition = rightScroller.getVerticalScrollBar().getValue();
        showChoices();
    }

    @Override
    public void treeStructureChanged(TreeModelEvent treeModelEvent) {
        log.debug(treeModelEvent.toString());
    }

    // Inner Class

    //------------------------------------------------------------------------------------------
    // Identical to the parent class (DefaultTreeModel) except that for a rename operation it
    // preserves the original name in a variable that can then be accessed from methods in
    // the instantiating context, such as the event handlers in the TreeModelListener.
    // The BranchHelper may impose restrictions on certain 'from' or 'to' names, via the
    // 'allowRename<To/From>' methods.
    // Additionally, to enforce the uniqueness requirement as well as to prevent the
    // various changes made during an edit session from getting cross-threaded, it will not
    // accept a 'new' name that is in the list of the original or the current choices.
    // (A string might be in the current choices but not the original ones, if a selection
    //  had already been renamed to that value during this editing session).
    // The constraint that a new name not be in the original choices does unfortunately
    // prevent the case where the user wants to restore a selection back to what it was
    // originally, before it was renamed to something else.  Such actions would indicate that the
    // first rename was an error on the part of the user and that can be easily remedied by
    // a click of the 'Cancel' button, but that is not a selective fix and will undo ALL
    // changes for the entire editing session.  Oh well.  Renaming logic is a b*tch.
    class BranchEditorModel extends DefaultTreeModel {
        static final long serialVersionUID = -1L;
        private String originalName;

        BranchEditorModel(TreeNode treeNode, boolean b) {
            super(treeNode, b);
        }

        // Handle rename actions
        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            //log.debug("value changed: " + node.toString());

            // No user feedback (here) for these cases; if desired then do
            // it in the helper methods, where a reason may also be provided.
            if (!myHelper.allowRenameFrom(node)) return;
            if (!myHelper.allowRenameTo(String.valueOf(newValue))) return;
            // Used String.valueOf() vs toString(), because newValue is an Object that might
            // be null and that cannot be changed since this method is an Override where the
            // super method is provided by the framework and not my own code.

            // Now consider the original list of choices, if such a list was provided by the helper.
            ArrayList<String> helperChoices = myHelper.getChoices();
            boolean foundInHelperChoices = false;
            if (helperChoices != null) {
                foundInHelperChoices = helperChoices.contains(String.valueOf(newValue));
            }

            // Now consider the choices as they currently appear.
            boolean foundInCurrentChoices = theChoices.contains(String.valueOf(newValue));

            if (foundInHelperChoices || foundInCurrentChoices) {
                // Notify the user if we are going to refuse the rename
                // due to our own 'foundIn' reasons,
                String ems = "That name is not available!\n";
                ems += "  Rename operation cancelled.";
                JOptionPane.showMessageDialog(null, ems,
                        "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                // Finally, no reason not to, so go ahead and allow the rename.
                originalName = node.toString();

                // But here is where the Helper might have overridden the final value -
                String renameTo = myHelper.getRenameToString();
                if (null != renameTo) super.valueForPathChanged(path, renameTo);
                else super.valueForPathChanged(path, newValue);
            }
        } // end valueForPathChanged

        String getOriginalName() {
            return originalName;
        }
    } // end inner class BranchEditorModel

} // end class TreeBranchEditor


