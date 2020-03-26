import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// A LinkNoteComponent extends a NoteComponent BUT it encapsulates a LinkTarget, unlike the base and its
//   other children that encapsulate a NoteData.  So here, any 'super' methods that are overridden will
//   translate the operation so that the effect is seen on the Target NoteData portion of the LinkTarget.

// Visually, a LinkNoteComponent is a horizontal line composed of:
// A link type dropdown control
// A 'delete' checkbox
// A label (linkTypeLabel) giving the target type and group name
// A read-only text field that contains the target notestring, if one was chosen

@SuppressWarnings("rawtypes")
public class LinkNoteComponent extends NoteComponent {
    static final long serialVersionUID = 1L;
    NoteComponentManager myManager;
    private LinkTarget myLinkTargetData;
    JComboBox<String> linkTypeDropdown;
    JCheckBox deleteCheckBox;
    JLabel linkTitleLabel;

    @SuppressWarnings({"unchecked"})
    LinkNoteComponent(NoteComponentManager noteComponentManager, LinkTarget linkTarget, int i) {
        super(noteComponentManager, i);
        myManager = noteComponentManager;
        myLinkTargetData = linkTarget;

        removeAll();   // We will redo the base layout.
        setLayout(new BorderLayout());

        setNoteData(myLinkTargetData.getTargetNoteData()); // The part of the link that needs to be shown.

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                myLinkTargetData.deleteMe = !myLinkTargetData.deleteMe; // toggle
                deleteCheckBox.setSelected(myLinkTargetData.deleteMe);
            }
        };

        // westPanel will hold the Link type dropdown, the Delete checkbox, and the linkTitleLabel.
        JPanel westPanel = new JPanel();  // Default layout here is Flow

        // centerPanel will hold the text field for identifying the specific target note
        JPanel centerPanel = new JPanel(new DndLayout());

        //-------------------------------------------------------------------------------------
        // The link type dropdown
        linkTypeDropdown = new JComboBox<>();
        linkTypeDropdown.setModel(new DefaultComboBoxModel(LinkTarget.LinkType.values()));
        linkTypeDropdown.setSelectedItem(linkTarget.theType);
        linkTypeDropdown.setFocusable(false);
        westPanel.add(linkTypeDropdown);

        // The 'delete me' checkbox
        deleteCheckBox = new JCheckBox();
        deleteCheckBox.setFocusable(false);
        deleteCheckBox.setSelected(myLinkTargetData.deleteMe);
        deleteCheckBox.addMouseListener(mouseAdapter);
        westPanel.add(deleteCheckBox);

        // The Group Type - Group Name label
        GroupInfo groupInfo = GroupInfo.getGroupInfo(linkTarget.getTargetGroupId());
        if (groupInfo != null) {
            myLinkTargetData.linkTitle = groupInfo.getGroupType() + ": " + groupInfo.getGroupName();
        } else {
            myLinkTargetData.linkTitle = "Group Type - Group Name: ";
        }
        linkTitleLabel = new JLabel(myLinkTargetData.linkTitle) {
            static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() {
                // This helps to standardize the apparent lengths of the text in the West panel.
                return new Dimension(180, super.getPreferredSize().height);
            }
        };
        linkTitleLabel.addMouseListener(mouseAdapter);
        westPanel.add(linkTitleLabel);
        add(westPanel, BorderLayout.WEST);

        //-------------------------------------------------------------------------------------
        noteTextField.setText(linkTarget.getTargetNoteData().noteString);
        noteTextField.setEditable(false);
        centerPanel.add(noteTextField, "Stretch");
        add(centerPanel, BorderLayout.CENTER);
    }

    //-----------------------------------------------------------------
    // Method Name: clear
    //
    // Clears both the Graphical elements and the underlying data.
    //-----------------------------------------------------------------
    @Override
    protected void clear() {
        // We need to clear out our own members and components before clearing the base component.
        myLinkTargetData.deleteMe = false;
        deleteCheckBox.setSelected(myLinkTargetData.deleteMe);
        myLinkTargetData.linkTitle = "";
        linkTitleLabel.setText(myLinkTargetData.linkTitle);

        super.clear(); // This also sets the component 'initialized' to false.
    } // end clear


    public LinkTarget getLinkTarget() {
        myLinkTargetData.theType = (LinkTarget.LinkType) linkTypeDropdown.getSelectedItem();
        myLinkTargetData.deleteMe = deleteCheckBox.isSelected();
        return myLinkTargetData;
    }


    @Override
    public NoteData getNoteData() {
        return myLinkTargetData.getTargetNoteData();
    }

    //----------------------------------------------------------
    // Method Name: resetComponent
    //
    // Called after a change to the encapsulated data, to show
    //   the visual effects of the change.
    //----------------------------------------------------------
    @Override
    protected void resetComponent() {
        linkTypeDropdown.setSelectedItem(myLinkTargetData.theType);
        deleteCheckBox.setSelected(myLinkTargetData.deleteMe);
        linkTitleLabel.setText(myLinkTargetData.linkTitle);

        super.resetComponent(); // the note text
    } // end resetComponent


    void setLinkTarget(LinkTarget newLinkData) {
        myLinkTargetData = newLinkData;

        // update visual components...
        initialized = true;  // without updating the 'lastModDate'
        resetComponent();
        setNoteChanged();
    } // end setLinkData

    // The base class uses this; we redirect any 'NoteData' operations to our Target NoteData.
    @Override
    public void setNoteData(NoteData newNoteData) {
        myLinkTargetData.setLinkTargetNoteData(newNoteData);

        // update visual components...
        initialized = true;  // without updating the 'lastModDate'

        // Just do the text and its color, for now.
        super.resetComponent();
        setNoteChanged();
    }

    // Exchange data content between this component and the input
    // parameter, that is an instance of the same NoteComponent child-class.
    @Override
    public void swap(NoteComponent theNoteComponent) {
        // Get a reference to the two data objects
        LinkTarget data1 = this.getLinkTarget();
        LinkTarget data2 = ((LinkNoteComponent) theNoteComponent).getLinkTarget();

        // Note: getLinkData and setLinkData are working with references
        //   to data objects.  If you 'get' data from the NoteComponent
        //   into a local variable and then later clear the component, you have
        //   also just cleared the data in your local variable because you never
        //   had a separatate copy of the data object, just the reference to it.

        // So - copy the data objects.
        if (data1 != null) data1 = new LinkTarget(data1);
        if (data2 != null) data2 = new LinkTarget(data2);

        if (data1 == null) theNoteComponent.clear();
        else ((LinkNoteComponent) theNoteComponent).setLinkTarget(data1);

        if (data2 == null) this.clear();
        else this.setLinkTarget(data2);

        myManager.setGroupChanged(true);
    } // end swap


}
