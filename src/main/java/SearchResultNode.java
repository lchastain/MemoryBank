import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.Serializable;

//-------------------------------------------------------------------------
// Class Name:  SearchResultNode
//
// Holds the data for the 'Search Results' tree node.
// The first one (top level) will have a null filename and the
//   node name will be 'Search Results'.
//-------------------------------------------------------------------------
class SearchResultNode extends DefaultMutableTreeNode implements Serializable {
    static final long serialVersionUID = 1955502766506973356L;

    public String strFileName;
    public String strNodeName;
    public int intGroupSize;
    public boolean blnExpanded;

    // This member is transient so that it will not be saved.  This
    //   allows the SearchResultNode to be quickly restored from
    //   saved data, even if it is a handle to a large SearchResultGroup.
    //   Upon initial construction of the node, it will be 'filled' but
    //   later, upon reload of this node, it will be null.  Then, the
    //   group will be loaded only if the node is clicked by the user.
    public transient SearchResultGroup srg;

    public SearchResultNode(String s, int i) {
        intGroupSize = i;
        if ((s == null) || (s.equals(""))) {
            strNodeName = "Search Results";
        } else {
            strFileName = s;  // will be null at the top level ONLY
            strNodeName = prettyName(s);

            // Note: an earlier version sent the full path in 's'.  Now,
            //   we expect to only have the filename.
            String strFilePath = MemoryBank.userDataHome + File.separatorChar;
//      srg = new SearchResultGroup(strFilePath + strFileName, intGroupSize);
            srg = new SearchResultGroup(strFilePath + strFileName);
        } // end else

        blnExpanded = true;
    } // end constructor


    public static String prettyName(String s) {
        int i;
        char slash = File.separatorChar;

        i = s.lastIndexOf(slash);
        if (i != -1) {
            s = s.substring(i + 1);
        } // end if

        // Even though a Windows path separator char should be a
        //   backslash, in Java a forward slash is often also accepted.
        i = s.lastIndexOf("/");
        if (i != -1) {
            s = s.substring(i + 1);
        } // end if

        // Drop the suffix
        i = s.lastIndexOf(".sresults");
        if (i == -1) return s;
        return s.substring(0, i);
    } // end prettyName


    public String toString() {
        return strNodeName;
    }
} // end class SearchResultNode
