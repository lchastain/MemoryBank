import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ItemEvent;
import java.io.IOException;

class TreeBranchEditorTest {
    private TreeBranchEditor treeBranchEditor;
    DefaultMutableTreeNode dmtn;

    @BeforeAll
    static void beforeAll() throws IOException {
        MemoryBank.debug = true;
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
    }

    @BeforeEach
    void setUp() {
        // Make a 'test' Tree Branch
        dmtn = new DefaultMutableTreeNode("An Example Branch");
        dmtn.add(new DefaultMutableTreeNode("that"));
        dmtn.add(new DefaultMutableTreeNode("these"));
        dmtn.add(new DefaultMutableTreeNode("when"));
        treeBranchEditor = new TreeBranchEditor(null, dmtn, new EditorHelper());
    }

    @Test
    void testItemStateChanged() {
        JCheckBox jcb = new JCheckBox("SelectMe");
        ItemEvent ie1 = new ItemEvent(jcb, ItemEvent.SELECTED, null, 1);
        ItemEvent ie2 = new ItemEvent(jcb, ItemEvent.SELECTED, null, 0);
        treeBranchEditor.itemStateChanged(ie1);
        treeBranchEditor.itemStateChanged(ie2);
    }

//    @Test
//    void testActionPerformed() {
//    }
//
//    @Test
//    void testTreeNodesChanged() {
//    }
//
//    @Test
//    void testTreeNodesInserted() {
//    }
//
//    @Test
//    void testTreeNodesRemoved() {
//    }
//
//    @Test
//    void testTreeStructureChanged() {
//    }
}