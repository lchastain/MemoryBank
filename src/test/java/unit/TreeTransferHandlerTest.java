import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

class TreeTransferHandlerTest {
    private TreeTransferHandler treeTransferHandler;
    private JTree theTree;

    @BeforeEach
    void setUp() {
        treeTransferHandler = new TreeTransferHandler(true);
        DefaultMutableTreeNode dmtn;
        dmtn = new DefaultMutableTreeNode("An Example Branch");
        dmtn.add(new DefaultMutableTreeNode("that"));
        dmtn.add(new DefaultMutableTreeNode("these"));
        DefaultMutableTreeNode aNode = new DefaultMutableTreeNode("when");
        dmtn.add(new DefaultMutableTreeNode(aNode));

        TreeModel treeModel = new DefaultTreeModel(dmtn);
        theTree = new JTree(treeModel);
        theTree.setDragEnabled(true);
        theTree.setDropMode(DropMode.ON_OR_INSERT);
        theTree.setTransferHandler(treeTransferHandler);

//        theTree.getSelectionModel().setSelectionMode
//                (TreeSelectionModel.SINGLE_TREE_SELECTION);

//        theTree.setSelectionPath(new TreePath(dmtn.getTreePath()));
    }

    @Test
    void testCreateTransferable() {
        treeTransferHandler.createTransferable(theTree);
//        assert transferable != null;
//        DataFlavor[] theFlavors = transferable.getTransferDataFlavors();
//        assert theFlavors != null;
    }

//    @Test
//    void testExportDone() {
//    }
//
//    @Test
//    void testGetSourceActions() {
//    }

//    @Test
//    void testImportData() {
//
//        Transferable transferable = treeTransferHandler.createTransferable(theTree);
//        assert transferable != null;
//        transferSupport = new TransferHandler.TransferSupport(theTree, transferable);
//        b = treeTransferHandler.importData(transferSupport);
//        Assertions.assertFalse(b);
//
//        DropTargetEvent dropTargetEvent = new DropTargetEvent(theTree.getDropTarget().getDropTargetContext());
//        DropTargetDropEvent dropTargetDropEvent = new DropTargetDropEvent(theTree.getDropTarget().getDropTargetContext(), new Point(55,42), DnDConstants.ACTION_MOVE, 3);
        //aNode.processEvent()
//        javafx.event.fireEvent();
//        dropTargetEvent.fireEvent()
//        aNode.fireEvent();

//        b = treeTransferHandler.importData(transferSupport);
//        b = treeTransferHandler.importData(TransferHandler.TransferSupport);
//    }

    @Test
    void testTestToString() {
        System.out.println("Test of the toString method: " + treeTransferHandler.toString());
    }

}

