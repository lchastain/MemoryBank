public class NodeChange {
    public static final int SELECTED = 83;
    public static final int DESELECTED = 84;
    public static final int RENAMED = 85;
    public static final int REMOVED = 86;
    public static final int MARKED = 87;
    public static final int UNMARKED = 88;
    public static final int MOVED = 89;

    public String nodeName;
    public String renamedTo;
    public int changeType;

    public NodeChange(String s, int t) {
        nodeName = s;
        changeType = t;
    }

    public NodeChange(String s, String t) {
        this(s, RENAMED);
        renamedTo = t;
    }

    @Override
    public String toString() {
        String retVal = nodeName;
        switch (changeType) {
            case NodeChange.SELECTED:
                retVal += " was SELECTED";
                break;
            case NodeChange.REMOVED:
                retVal += " was REMOVED";
                break;
            case NodeChange.MARKED:
                retVal += " was MARKED";
                break;
            case NodeChange.UNMARKED:
                retVal += " was UNMARKED";
                break;
            case NodeChange.MOVED:
                retVal += " was MOVED";
                break;
            case NodeChange.DESELECTED:
                retVal += " was DESELECTED";
                break;
            case NodeChange.RENAMED:
                retVal += " was RENAMED to " + renamedTo;
                break;
            default:
                retVal += " was changed in some strange manner unbeknownst to me.";
        }
        return retVal;
    }

}
