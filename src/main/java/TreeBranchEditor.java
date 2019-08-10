import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreeBranchEditor extends JPanel
        implements ItemListener, ActionListener, TreeModelListener {
    static final long serialVersionUID = -1L;
    private static Logger log = LoggerFactory.getLogger(TreeBranchEditor.class);

    private JScrollPane leftScroller;
    private JScrollPane rightScroller;

    private DefaultMutableTreeNode origBranch;
    private DefaultMutableTreeNode myBranch;
    private TreeBranchHelper myHelper;
    private ArrayList<String> theChoices;
    private ArrayList<String> removals = new ArrayList<String>();
    BranchEditorModel bem;

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

    public TreeBranchEditor(TreeNode tn, TreeBranchHelper tbh) {
        super(new BorderLayout());
        origBranch = (DefaultMutableTreeNode) tn;
        myBranch = deepClone(origBranch);
        myHelper = tbh;
        changeList = new ArrayList<NodeChange>();

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

        centerPanel.setLeftComponent(leftScroller);
        centerPanel.setRightComponent(rightScroller);

        add(centerPanel, "Center");
        add(southPanel, "South");
    } // end constructor

    // If the helper's getChoices() has not been implemented,
    // then make our own, from the provided branch.
    @SuppressWarnings("rawtypes") // Adding a type then causes 'unchecked' problem.
    private ArrayList<String> getChoices() {
        theChoices = myHelper.getChoices();
        if(theChoices != null) return theChoices;

        // Create list of choices from the existing branch.
        theChoices = new ArrayList<String>();
        String theRoot = origBranch.toString();
        Enumeration dfe = origBranch.depthFirstEnumeration();
        while(dfe.hasMoreElements()) {
            DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)dfe.nextElement();
            if(!dmtn.toString().equals(theRoot))
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

    @SuppressWarnings("rawtypes") // Adding a type then causes 'unchecked' problem.
    private void showChoices() {
        // Create lists of pre-selected items from the existing branch.
        Enumeration dfe = myBranch.depthFirstEnumeration();
        ArrayList<String> leaves = new ArrayList<String>();
        ArrayList<String> branches = new ArrayList<String>();
        while(dfe.hasMoreElements()) {
            DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)dfe.nextElement();
            String name = dmtn.toString();
            if(dmtn.isLeaf()) leaves.add(name);
            else branches.add(name);
        }

        JPanel jpr = new JPanel();
        jpr.setLayout(new BoxLayout(jpr, BoxLayout.Y_AXIS));

        // To the right-side panel, add the selection choices.
        for(String s: theChoices)  {
            JCheckBox jcb = new JCheckBox(s);
            if(leaves.contains(s)) {
                jcb.setSelected(true);
            } else if(branches.contains(s)) {
                jcb.setSelected(true);
                jcb.setEnabled(false);
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
            if(myHelper.deletesAllowed()) {
                JPanel oneLine = new JPanel(new BorderLayout());
                oneLine.add(jcb, "West");
                String deleteCommand = getDeleteCommand();
                JButton jb =new JButton(deleteCommand);
                jb.setActionCommand(s);
                jb.addActionListener(this);
                if(!branches.contains(s)) oneLine.add(jb, "East");
                if(removals.contains(s)) {
                    jcb.setEnabled(false);
                    JLabel removalLabel = new JLabel("Marked for REMOVAL");
                    removalLabel.setForeground(Color.red);
                    removalLabel.setHorizontalAlignment(JLabel.CENTER);
                    oneLine.add(removalLabel, "Center");
                }
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

    private String getDeleteCommand() {
        String deleteCommand = myHelper.getDeleteCommand();
        if(deleteCommand == null) return "X"; // a default.
        return deleteCommand;
    }

    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        String theText = ((JCheckBox)source).getText();

        if (e.getStateChange() == ItemEvent.SELECTED) {
            //log.debug(theText + " selected");
            DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(theText);
            myBranch.add(dmtn);
            changeList.add(new NodeChange(theText, NodeChange.SELECTED));
        } else { // if (e.getStateChange() == ItemEvent.DESELECTED) {
            //log.debug(theText + " deselected");
            remove(theText);
            changeList.add(new NodeChange(theText, NodeChange.DESELECTED));
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
        //log.debug("Removing: " + theLeafText);
        DefaultMutableTreeNode theParent = (DefaultMutableTreeNode) tmpLeaf.getParent();
        theParent.remove(tmpLeaf);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        String theAction = actionEvent.getActionCommand();
        String theText = ((JButton) actionEvent.getSource()).getText();
        //log.debug(theAction);

        if(theAction.equals("Cancel")) {
            myBranch = deepClone(origBranch);
            theChoices = getChoices();
            changeList = new ArrayList<NodeChange>();
            removals = new ArrayList<String>();
            showTree();
            showChoices();
        }   else if(theAction.equals("Apply")) {
            for(String s: removals) {
                changeList.add(new NodeChange(s, NodeChange.REMOVED));
            }
            myHelper.doApply(myBranch, changeList);
            // At this point there is not much use in leaving the editor displayed and
            // accessible to the user; in fact they may wreak havoc with another click
            // of any of the buttons that we handle here.  But for some reason, it seems
            // like a bad idea to try and idiot-proof it from here; that will be up to
            // the consumer of this tool.  Good luck and happy tree-trimming!
        }  else if(theText.equals(getDeleteCommand())) {
            // The action was a 'delete' button click, which is a toggle.
            if(removals.contains(theAction)) {
                removals.remove(theAction);
                changeList.add(new NodeChange(theAction, NodeChange.UNMARKED));
            }   else {
                removals.add(theAction);
                remove(theAction);  // Remove from the tree.
                changeList.add(new NodeChange(theAction, NodeChange.MARKED));
            }
            showTree();
            showChoices();
        }
    }

    @SuppressWarnings("rawtypes") // Adding a type then causes 'unchecked' problem.
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
        DefaultMutableTreeNode node;
        node = (DefaultMutableTreeNode)
                (treeModelEvent.getTreePath().getLastPathComponent());

         // If the event lists children then the changed node is the child of that one.
         // Otherwise, the changed node is the one we already have.
        try {
            int index = treeModelEvent.getChildIndices()[0];
            node = (DefaultMutableTreeNode)
                    (node.getChildAt(index));
        } catch (NullPointerException exc) { System.out.print(""); }

        String renamedTo = node.toString();
        String renamedFrom = bem.getOriginalName();
        //log.debug(renamedFrom + " was renamed to " + renamedTo);
        doRename(renamedFrom, renamedTo);
        changeList.add(new NodeChange(renamedFrom, renamedTo));
    }

    // This method is called after a rename has been done on the JTree UI,
    // in order to keep the selections in line with the new text.
    private void doRename(String renamedFrom, String renamedTo) {
        for(int i=0; i<theChoices.size(); i++) {
            String s = theChoices.get(i);
            if(s.equals(renamedFrom)) {
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
        } catch (NullPointerException exc) { System.out.print(""); }

        changeList.add(new NodeChange(node.toString(), NodeChange.MOVED));
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
    // The TreeBranchHelper may impose restrictions on certain 'from' or 'to' names, via the
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

        public BranchEditorModel(TreeNode treeNode, boolean b) {
            super(treeNode, b);
        }

        // Handle rename actions
        public void valueForPathChanged(TreePath path, Object newValue)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
            //log.debug("value changed: " + node.toString());

            // No user feedback (here) for these cases; if desired then do
            // it in the helper methods, where a reason may also be provided.
            if(!myHelper.allowRenameFrom(node.toString())) return;
            if(!myHelper.allowRenameTo(String.valueOf(newValue))) return;

            // Now consider the original list of choices, if such a list was provided by the helper.
            ArrayList<String> helperChoices = myHelper.getChoices();
            boolean foundInHelperChoices = false;
            if(helperChoices != null) {
                foundInHelperChoices = helperChoices.contains(String.valueOf(newValue));
            }

            // Now consider the choices as they currently appear.
            boolean foundInCurrentChoices = theChoices.contains(String.valueOf(newValue));

            // If a refusal to rename happens due to our own 'foundIn' reasons, currently
            // we just silently ignore the attempt.  If the helper has a 'need to know'
            // then we can always add a notification method to the interface.
            if(!foundInHelperChoices && !foundInCurrentChoices) {
                originalName = node.toString();
                super.valueForPathChanged(path, newValue);
            }
        }

        public String getOriginalName() { return originalName; }
    } // end inner class BranchEditorModel

} // end class TreeBranchEditor

