import java.io.Serializable;

public class TodoListProperties implements Serializable {
    static final long serialVersionUID = 2340086274436821238L;

    // Priority
    public boolean showPriority;
    public int maxPriority;

    // Deadline
    public boolean showDeadline;
    public String defaultDeadlineFormat;
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

    // Fonts
    public String itemFont;
    public String deadFont;

//    public Dimension frameSize;  // Size of the Frame
    public int scrollerPos;      // Position the Frame is vertically scrolled to.
//    public Point todoPos;        // Position of the Frame on the full screen
    public String listTitle;     // Title of the to do list
    public int numberOfItems;    // How many items in the list
    public String column1Label;
    public String column2Label;
    public String column3Label;
    public String column4Label;
    public int columnOrder;

    public TodoListProperties() { // Constructor with defaults
        showDeadline = false;
        showPriority = true;
        maxPriority = 20;
        deadWidth = 170;
        defaultDeadlineFormat = "1346|EEEE', '|MMMM' '|d' '|yyyy"; // 1346
        itemFont = "";  // Placeholder
        deadFont = "";  // Placeholder
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

        // This only matters to the stand-alone 'todo'.
//        frameSize = new Dimension(700, 480);

        scrollerPos = 0;
//        todoPos = new Point(100, 50);
        listTitle = "To Do List";
        numberOfItems = 15;
        column1Label = "Priority";
        column2Label = "To Do Text";
        column3Label = "Status";
        column4Label = "Deadline";
        columnOrder = TodoNoteGroup.INORDER;
    } // end constructor

} // end TodoListProperties
