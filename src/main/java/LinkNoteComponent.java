import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// A LinkNoteComponent extends a NoteComponent BUT it encapsulates a LinkTarget, unlike the base and its
//   other children that encapsulate a NoteData.  So here, any 'super' methods that are overridden will
//   translate the operation so that the effect is seen on the encapsulated NoteData portion of the LinkTarget.

// Visually, a LinkNoteComponent is a horizontal line composed of:
// A link type dropdown control
// A 'delete' checkbox
// A label (linkTypeLabel) giving the target type and group name (LinkedEntityData.linkTitle)
// A read-only text field that contains the target notestring, if one was chosen

public class LinkNoteComponent extends NoteComponent {
    static final long serialVersionUID = 1L;
    NoteComponentManager myManager;
    LinkedEntityData myLinkedEntityData;
    JComboBox<String> linkTypeDropdown;
    JCheckBox deleteCheckBox;
    GroupInfo targetGroupInfo;
    NoteInfo noteInfo;   // The info for this component; not necessarily the info of the link target note.
    String linkTitleString;
    JLabel linkTitleLabel;

    @SuppressWarnings({"rawtypes", "unchecked"})
    LinkNoteComponent(NoteComponentManager noteComponentManager, LinkedEntityData linkedEntityData, int i) {
        super(noteComponentManager, i);
        myManager = noteComponentManager;
        myLinkedEntityData = linkedEntityData;
        targetGroupInfo = myLinkedEntityData.getTargetGroupInfo();
        makeLinkTitle();

        removeAll();   // We will redo the base layout.
        setLayout(new BorderLayout());

        // Construct our component info from the link sent in to the constructor.
        noteInfo = myLinkedEntityData.getTargetNoteInfo();
        if (noteInfo == null) {
            // If none then we make our own, for usage only in the LinkagesEditorPanel, probably.
            noteInfo = new NoteInfo();
            noteInfo.noteString = myLinkedEntityData.getTargetGroupInfo().getGroupName();
//            noteInfo.myNoteGroupPanel = myLinkedEntityData.getTargetGroupInfo().myNoteGroupPanel;
            // Rather than trying to keep this around, replace it with a 'get' method that is only called when needed.
            // Its usage from within a link note component - I don't remember; maybe not needed at all, or else
            // we'll be back here tracking down the problem.
            // If we need this, maybe assign it to a yet-to-be defined LinkagesEditorPanel.theInstance member.

            // Here we COULD set an extended note for a popup tooltip, but decided against it, at this time.
            //noteInfo.extendedNoteString = "All notes in this group";
        }
        myNoteData = new NoteData(noteInfo); // This isolates our 'component' NoteData from the source entity.

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                myLinkedEntityData.deleteMe = !myLinkedEntityData.deleteMe; // toggle
                deleteCheckBox.setSelected(myLinkedEntityData.deleteMe);
            }
        };

        // westPanel will hold the Link type dropdown, the Delete checkbox, and the linkTitleLabel.
        JPanel westPanel = new JPanel();  // Default layout here is Flow

        // centerPanel will hold the text field for identifying the specific target note
        JPanel centerPanel = new JPanel(new DndLayout());

        //-------------------------------------------------------------------------------------
        // The link type dropdown
        linkTypeDropdown = new JComboBox<>();
        linkTypeDropdown.setModel(new DefaultComboBoxModel(LinkedEntityData.LinkType.values()));
        linkTypeDropdown.setSelectedItem(linkedEntityData.linkType);
        linkTypeDropdown.setFocusable(false);
        linkTypeDropdown.setEnabled(linkedEntityData.retypeMe);
        westPanel.add(linkTypeDropdown);

        // The 'delete me' checkbox
        deleteCheckBox = new JCheckBox();
        deleteCheckBox.setFocusable(false);
        deleteCheckBox.setSelected(myLinkedEntityData.deleteMe);
        deleteCheckBox.addMouseListener(mouseAdapter);
        westPanel.add(deleteCheckBox);

        // The linkTitleLabel
        linkTitleLabel = new JLabel() {
            static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                // This helps to standardize the apparent lengths of the text in the West panel.
                return new Dimension(180, super.getPreferredSize().height);
            }
        };

        linkTitleLabel.setText(linkTitleString);
        linkTitleLabel.setBackground(Color.ORANGE);
        linkTitleLabel.setOpaque(myLinkedEntityData.reversed);
        linkTitleLabel.addMouseListener(mouseAdapter);
        westPanel.add(linkTitleLabel);
        add(westPanel, BorderLayout.WEST);

        //-------------------------------------------------------------------------------------
        noteTextField.getDocument().removeDocumentListener(noteTextField);
        noteTextField.setText(myNoteData.noteString);
        noteTextField.setEditable(false);
        centerPanel.add(noteTextField, "Stretch");
        add(centerPanel, BorderLayout.CENTER);
    }// end constructor

    // Clears both the Graphical elements and the underlying data.
    @Override
    protected void clear() {
        // We need to clear out our own members and components before clearing the base component.
        myLinkedEntityData.deleteMe = false;
        deleteCheckBox.setSelected(myLinkedEntityData.deleteMe);
        linkTitleLabel.setText("");
        linkTitleLabel.setOpaque(false);
        myNoteData.clear();

        super.clear(); // This also sets the component 'initialized' to false.
    } // end clear


    // Update the data with info from the UI controls, then return the updated data.
    public LinkedEntityData getLinkTarget() {
        myLinkedEntityData.linkType = (LinkedEntityData.LinkType) linkTypeDropdown.getSelectedItem();
        myLinkedEntityData.deleteMe = deleteCheckBox.isSelected();
        return myLinkedEntityData;
    }

    void makeLinkTitle() {
        String theTitleString;

        String groupType = targetGroupInfo.groupType.toString();
        String groupName = targetGroupInfo.getGroupName(); // User-provided at group creation, except for Notes.

        if(myLinkedEntityData.getTargetNoteInfo() == null) { // The link is to a full group
            // Do not change this condition to look at the noteInfo of THIS class; we will not allow that one to stay null.
            theTitleString = groupType;
        } else { // The link is to a specific Note within a Group
            if(groupType.equals("Day Note")) {
                // Drop the leading day name and comma-space
                String shorterName = groupName.substring(groupName.indexOf(",") + 1);
                theTitleString = groupType + ": " + shorterName;
            } else {
                theTitleString = groupType + ": " + groupName;
            }
        }

        linkTitleString = theTitleString;
    }

    //----------------------------------------------------------
    // Method Name: resetComponent
    //
    // Called after a change to the encapsulated data, to show
    //   the visual effects of the change.
    //----------------------------------------------------------
    @Override
    protected void resetComponent() {
        super.resetComponent(); // the note text
        linkTypeDropdown.setSelectedItem(myLinkedEntityData.linkType);
        linkTypeDropdown.setEnabled(myLinkedEntityData.retypeMe);
        deleteCheckBox.setSelected(myLinkedEntityData.deleteMe);
        linkTitleLabel.setText(linkTitleString);
        //System.out.println("Title: " + myLinkedEntityData.linkTitle + "   reversed: " + myLinkedEntityData.reversed);
        linkTitleLabel.setOpaque(myLinkedEntityData.reversed); // Color shows, or not.

        noteTextField.getDocument().removeDocumentListener(noteTextField);
    } // end resetComponent


    void setLinkTarget(LinkedEntityData newLinkData) {
        myLinkedEntityData = newLinkData;

        // update visual components...
        initialized = true;  // without updating the 'lastModDate'
        resetComponent();
        setNoteChanged();
    } // end setLinkData


    // Exchange data content between this component and the input
    // parameter, that is an instance of the same NoteComponent child-class.
    @Override
    public void swap(NoteComponent theNoteComponent) {
        // Get a reference to the two data objects
        LinkedEntityData data1 = this.getLinkTarget();
        LinkedEntityData data2 = ((LinkNoteComponent) theNoteComponent).getLinkTarget();

        // Note: getLinkData and setLinkData are working with references
        //   to data objects.  If you 'get' data from the NoteComponent
        //   into a local variable and then later clear the component you have
        //   also just cleared the data in both the local variable and your original
        //   data object because you never had a separatate copy of it, just its reference.

        // So - copy the data objects.
        if (data1 != null) data1 = new LinkedEntityData(data1);
        if (data2 != null) data2 = new LinkedEntityData(data2);
        NoteData nd1 = new NoteData(theNoteComponent.myNoteData);
        NoteData nd2 = new NoteData(this.myNoteData);

        if (data1 == null) theNoteComponent.clear();
        else {
            ((LinkNoteComponent) theNoteComponent).setLinkTarget(data1);
            ((LinkNoteComponent) theNoteComponent).makeLinkTitle();
            theNoteComponent.setNoteData(nd2); // this calls resetComponent.
        }

        if (data2 == null) this.clear();
        else {
            this.setLinkTarget(data2);
            this.makeLinkTitle();
            this.setNoteData(nd1); // this calls resetComponent.
        }

        myManager.setGroupChanged(true);
    } // end swap


}
