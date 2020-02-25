import com.fasterxml.jackson.annotation.JsonIgnore;

public class TodoListGroupProperties {

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
    int numberOfItems;    // How many items in the list

    public String column1Label;
    public String column2Label;
    public String column3Label;
    public String column4Label;
    public int columnOrder;

    public TodoListGroupProperties() { // Constructor with defaults
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
        whenNoKey = TodoNoteGroup.BOTTOM;

//        numberOfItems = 15;
        column1Label = "Priority";
        column2Label = "To Do Text";
        column3Label = "Status";
        column4Label = "Deadline";
        columnOrder = TodoNoteGroup.INORDER;
    } // end constructor

} // end TodoListProperties
