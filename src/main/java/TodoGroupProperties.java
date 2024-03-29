import com.fasterxml.jackson.annotation.JsonIgnore;

public class TodoGroupProperties extends GroupProperties {

    // Priority
    public boolean showPriority;
    public int maxPriority;

    public int deadWidth;

    // Print
    public boolean pHeader;
    public boolean pFooter;
    public boolean pBorder;
    public boolean pCSpace;
    public boolean pEText;
    public boolean pPriority;
    public boolean pDeadline;
    public int pCutoff;
    public int lineSpace;

    // Sort
    public int whenNoKey;

    @JsonIgnore
    int numberOfItems;    // How many items in the list.  Keep this ignored var until it is gone from ALL persisted data.

    public String column1Label;
    public String column2Label;
    public String column3Label;
    public String column4Label;
    public int columnOrder;

    public TodoGroupProperties() {} // Needed / used by Jackson.

    public TodoGroupProperties(GroupInfo groupInfo) { // Constructor with defaults
        super(groupInfo.getGroupName(), groupInfo.groupType);

        showPriority = true;
        maxPriority = 20;
        deadWidth = 170;
        pHeader = true;
        pFooter = true;
        pBorder = true;
        pCSpace = true;
        pEText = false;
        pPriority = true;
        pDeadline = false;
        pCutoff = 99;
        lineSpace = 1;
        whenNoKey = TodoNoteGroupPanel.BOTTOM;

        column1Label = "Priority";
        column2Label = "To Do Text";
        column3Label = "Status";
        column4Label = "Deadline";
        columnOrder = TodoNoteGroupPanel.INORDER;
    } // end constructor

    public TodoGroupProperties(String groupName) { // Constructor with defaults
        this(new GroupInfo(groupName, GroupType.TODO_LIST));
    } // end constructor

} // end TodoGroupProperties
