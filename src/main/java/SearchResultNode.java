import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.Serializable;
import java.util.Enumeration;

//-------------------------------------------------------------------------
// Class Name:  SearchResultNode
//
// Holds the data for the 'Search Results' tree node.
// The first one (top level) will have a null filename and the
//   node name will be 'Search Results'.
//-------------------------------------------------------------------------
class SearchResultNode extends DefaultMutableTreeNode {

    public String strNodeName;
    public int intGroupSize;
    public boolean blnExpanded;
    public SearchResultGroup srg;

    public SearchResultNode(String nodename, int i) {
        intGroupSize = i;
        if ((nodename == null) || (nodename.equals(""))) {
            // This should only occur for the top level.
            strNodeName = "Search Results";
            blnExpanded = true;
        } else {
            strNodeName = nodename;
        } // end else
    } // end constructor


    public static DefaultMutableTreeNode getSearchResultNode(DefaultMutableTreeNode theRoot) {
        DefaultMutableTreeNode dmtn = null;
        Enumeration bfe = theRoot.breadthFirstEnumeration();

        while(bfe.hasMoreElements()) {
            dmtn = (DefaultMutableTreeNode) bfe.nextElement();
            if (dmtn.toString().equals("Search Results")) {
                break;
            }
        }
        return dmtn;
    }

    public String toString() {
        return strNodeName;
    }
} // end class SearchResultNode
