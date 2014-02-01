/* ***************************************************************************
    A custom Editor panel for a TreeNode.

    Constructs the view from the initial branch and a TreeBranchHelper,
    and accepts user changes for leaf selection, renaming, deletion, and
    reordering (including 'stacking').

    As a component, we have a JPanel with a BorderLayout, where the Center is
    filled with a JSplitPane and the South contains the Apply/Cancel buttons.
    The JSplitPane shows the initial & proposed JTree on the left, and the
    complete list of available leaves on the right.
 ****************************************************************************/
/**/

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class TreeBranchEditor extends JPanel
        implements ItemListener, ActionListener, TreeModelListener {
    static final long serialVersionUID = -1L;
    private static Logger log = LoggerFactory.getLogger(TreeBranchEditor.class);

    private JScrollPane leftScroller;
    private JScrollPane rightScroller;

    private DefaultMutableTreeNode origBranch;
    private DefaultMutableTreeNode myBranch;
    private TreeBranchHelper myHelper;
    private ArrayList<String> theChoices;
    private boolean makeParents;

    public TreeBranchEditor(TreeNode dmtn, TreeBranchHelper tbh) {
        super(new BorderLayout());
        origBranch = (DefaultMutableTreeNode) dmtn;
        myBranch = deepClone(origBranch);
        myHelper = tbh;

        theChoices = myHelper.getChoices();

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

        centerPanel.setLeftComponent(leftScroller);
        centerPanel.setRightComponent(rightScroller);

        add(centerPanel, "Center");
        add(southPanel, "South");
    } // end constructor

    private void showTree() {
        JTree jt = new JTree(myBranch);
        jt.setShowsRootHandles(false);
        jt.setEditable(true);

        jt.setDragEnabled(true);
        jt.setDropMode(DropMode.ON_OR_INSERT);
        jt.setTransferHandler(new TreeTransferHandler(myHelper.makeParents()));
        jt.getSelectionModel().setSelectionMode(
                TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        jt.getModel().addTreeModelListener(this);
        expandTree(jt);
        leftScroller.setViewportView(jt);
    } // end showTree

    private void expandTree(JTree tree) {
        DefaultMutableTreeNode root =
                (DefaultMutableTreeNode)tree.getModel().getRoot();
        Enumeration e = root.breadthFirstEnumeration();
        while(e.hasMoreElements()) {
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode)e.nextElement();
            if(node.isLeaf()) continue;
            int row = tree.getRowForPath(new TreePath(node.getPath()));
            tree.expandRow(row);
        }
    }

    private void showChoices() {
        // Develop a list of pre-selected items.
        Vector<String> selections = new Vector<String>(1, 1);
        DefaultMutableTreeNode tmpLeaf = myBranch.getFirstLeaf();
        while (tmpLeaf != null) {
            String s = tmpLeaf.toString();
            //log.debug("Tree leaf: " + s);
            selections.addElement(s);
            tmpLeaf = tmpLeaf.getNextLeaf();
        } // end while

        JPanel jpr = new JPanel();
        jpr.setLayout(new BoxLayout(jpr, BoxLayout.Y_AXIS));

        // To the right-side panel, add the selection choices.
        for(String s: theChoices)  {
            JCheckBox jcb = new JCheckBox(s);
            if(selections.contains(s)) {
                jcb.setSelected(true);
            }
            jcb.addItemListener(this);

            // Either add a simple checkbox, or a checkbox plus a 'delete' control.
            // But now we encounter some gui resistance; the checkbox alone presents
            // no problem, but when we add a JPanel containing both, a resize of the
            // parent (BoxLayout) container wants to stretch the inner JPanels but
            // yet it does not do this to the lone JCheckbox components, so we have
            // unwanted, seemingly inconsistent behavior.
            // Complicating it all is the fact that we do want the horizontal stretch
            // behavior but don't want the vertical stretch.  So - we 'fix' this by
            // taking advantage of the fact that the BoxLayout is one of the few
            // Layouts that actually respects the minimum and maximum sizes of a component.
            if(myHelper.deleteAllowed()) {
                JPanel oneLine = new JPanel(new BorderLayout());
                oneLine.add(jcb, "West");
                JButton jb =new JButton(myHelper.getDeleteCommand());
                jb.setActionCommand(s);
                jb.addActionListener(this);
                oneLine.add(jb, "East");
                int w = oneLine.getMaximumSize().width;
                int h = oneLine.getPreferredSize().height;
                oneLine.setMaximumSize(new Dimension(w, h)); // See above note.
                jpr.add(oneLine);
            } else {
                jpr.add(jcb);
            }
        }

        rightScroller.setViewportView(jpr);
    }

    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        String theText = ((JCheckBox)source).getText();

        if (e.getStateChange() == ItemEvent.SELECTED) {
            log.debug(theText + " selected");
            DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(theText);
            myBranch.add(dmtn);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            log.debug(theText + " deselected");
            remove(theText);
        } else {
            log.debug(theText + " " + e.toString());
            return;
        }
        showTree();  // Refresh the view on the left side.
    }

    private void remove(String theLeafText) {
        DefaultMutableTreeNode tmpLeaf = myBranch.getFirstLeaf();
        while (tmpLeaf != null) {
            String s = tmpLeaf.toString();
            //log.debug("Remove - examining leaf: " + s);
            if(s.equals(theLeafText)) break;
            tmpLeaf = tmpLeaf.getNextLeaf();
        } // end while
        if(tmpLeaf == null) return; // Didn't find it.

        // We cannot just remove this leaf from myBranch; it may be deeper in if it
        // has been moved there first during the current edit session, then removed.
        log.debug("Removing: " + theLeafText);
        DefaultMutableTreeNode theParent = (DefaultMutableTreeNode) tmpLeaf.getParent();
        theParent.remove(tmpLeaf);

        // We do this here because somehow, the above removal does not kick off the
        // TreeModel treeNodesRemoved event.
        countChildren(theParent);
    }

    // We call this on a node after it has lost a child.
    private void countChildren(DefaultMutableTreeNode theParent) {
        // If the parent is the root, no need to look further 'up'.
        if(theParent.toString().equals(myBranch.toString())) return;

        // If the just-removed leaf was an only child (causing the parent to go back to
        // being just a leaf) and if the now childless parent was one of the original
        // choices and it is not currently a choice, then add it back to the list of choices.

        // * One way that it could already be a choice is - during the current edit session,
        // after it became a parent and was first removed from the choices, then some other node
        // might have been renamed to that same string value.  Messy.  But this condition
        // prevents it from getting messier. (May need to revisit this, after renaming is working.
        // maybe a better solution is to disallow a 'rename to' a 'removed' node in the same session;
        // just make them click 'Apply' after the removal, then start a new edit session to do the
        // rename to reuse the name that was removed in the previous session.
        if(theParent.getChildCount() == 0) {
            if(!theChoices.contains(theParent.toString())) {  // See the '*' note above.
                if(myHelper.getChoices().contains(theParent.toString())) {
                    theChoices.add(theParent.toString());
                    showChoices();
                }
            }
        }
    } // end countChildren

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        String theAction = actionEvent.getActionCommand();
        String theText = ((JButton) actionEvent.getSource()).getText();
        log.debug(theAction);

        if(theAction.equals("Cancel")) {
            myBranch = deepClone(origBranch);
            theChoices = myHelper.getChoices();
            showTree();
            showChoices();
        }   else if(theAction.equals("Apply")) {
            myHelper.doApply(myBranch, theChoices);
        }  else if(theText.equals(myHelper.getDeleteCommand())) {
            remove(theAction);
            theChoices.remove(theAction);
            showTree();
            showChoices();
        }
    }

    public DefaultMutableTreeNode deepClone(DefaultMutableTreeNode root){
        DefaultMutableTreeNode newRoot = (DefaultMutableTreeNode)root.clone();
        for(Enumeration childEnum = root.children(); childEnum.hasMoreElements();){
            newRoot.add(deepClone((DefaultMutableTreeNode)childEnum.nextElement()));
        }
        return newRoot;
    }

    // The rename of a node in the tree will kick off this event.
    @Override
    public void treeNodesChanged(TreeModelEvent treeModelEvent) {
        log.debug(treeModelEvent.toString());
    }

    // When one or more choices is moved to another, if the drop target is not already
    // a 'parent', it will become one at that time.  When that happens, we remove the
    // new parent from the list of choices because we no longer want to handle selection
    // or deletion events for that node.
    @Override
    public void treeNodesInserted(TreeModelEvent treeModelEvent) {
        log.debug(treeModelEvent.toString());
        Object[] thePath = treeModelEvent.getPath();
        int lastIndex = thePath.length -1;
        String droppedOn = thePath[lastIndex].toString();
        log.debug(droppedOn);
        theChoices.remove(droppedOn);
        showChoices();
    }

    // This event fires when a leaf (node) is dragged away from its branch (parent).
    // For our purposes, we want to see if this is the last child of the parent, thereby
    // turning that parent back into a leaf.  If so, we can restore that leaf back to
    // the list of choices.  Note that we also do this during deselection.
    @Override
    public void treeNodesRemoved(TreeModelEvent treeModelEvent) {
        log.debug(treeModelEvent.toString());

        DefaultMutableTreeNode node;
        node = (DefaultMutableTreeNode) (treeModelEvent.getTreePath().getLastPathComponent());
        countChildren(node);
    }

    @Override
    public void treeStructureChanged(TreeModelEvent treeModelEvent) {
        log.debug(treeModelEvent.toString());
    }
} // end class TreeBranchEditor


/**/