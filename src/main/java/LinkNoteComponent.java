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
// A label (linkTypeLabel) giving the target type and group name
// A read-only text field that contains the target notestring, if one was chosen

public class LinkNoteComponent extends NoteComponent {
    static final long serialVersionUID = 1L;
    NoteComponentManager myManager;
    LinkedEntityData myLinkedEntityData;
    JComboBox<String> linkTypeDropdown;
    JCheckBox deleteCheckBox;
    JLabel linkTitleLabel;

    @SuppressWarnings({"rawtypes", "unchecked"})
    LinkNoteComponent(NoteComponentManager noteComponentManager, LinkedEntityData linkedEntityData, int i) {
        super(noteComponentManager, i);
        myManager = noteComponentManager;
        myLinkedEntityData = linkedEntityData;

        removeAll();   // We will redo the base layout.
        setLayout(new BorderLayout());

        // Make a good NoteData that we can show, from the link sent in to the constructor.
        NoteInfo noteInfo = myLinkedEntityData.getTargetNoteInfo();
        if (noteInfo == null) {
            noteInfo = new NoteInfo();
            noteInfo.noteString = myLinkedEntityData.getTargetGroupInfo().getGroupName();
            noteInfo.myNoteGroupPanel = myLinkedEntityData.getTargetGroupInfo().myNoteGroupPanel;
            // Here we COULD set an extended note, but decided against it, at this time.
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
        // I did look for better places to make this call, but this component is the
        //   only one who needs the linkTitle, so why not here?  The linkTitle String
        //   is a transient, so making it at linkedEntityData construction does not work
        //   because the base constructor is used for loading the other data from file
        //   and although that other data is needed to make the title, it is not available
        //   to be used for that purpose at that point in the construction.  Other
        //   constructors could work for new links vs the persisted ones, but then the
        //   solution would be piecemeal, in at least two places, whereas making the call
        //   from this one location works for both persisted and new linkedEntityData.
        if (myLinkedEntityData.linkTitle == null) myLinkedEntityData.makeLinkTitle();
        // TODO - now that I've articulated such a verbose excuse - it seems like this
        // class is the best place to do this, and MOVE the variable to here, take it out
        // of the persisted class.  Do that, then clear out all the words here.

        linkTitleLabel.setText(myLinkedEntityData.linkTitle);
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
        linkTitleLabel.setText(myLinkedEntityData.linkTitle);
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
            theNoteComponent.setNoteData(nd2);
        }

        if (data2 == null) this.clear();
        else {
            this.setLinkTarget(data2);
            this.setNoteData(nd1);
        }

        myManager.setGroupChanged(true);
    } // end swap


}
