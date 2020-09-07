import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// LinkNoteComponent is already tested for most of its coverage via the functional test, but we still
// need to ensure that it correctly displays its content.
// Four tests of this so far (of 12), but then might want to look at reversals as well, IF we can get them created via
// the editor panel code and not directly, as we do here.

class LinkNoteComponentTest extends LinkHelper implements NoteComponentManager {

    @Test
    void testLinkGoalGroup() {
        LinkNoteComponent linkNoteComponent = new LinkNoteComponent(this, makeGoalGroupLink(), 0);

        Assertions.assertEquals(LinkedEntityData.LinkType.RELATED, linkNoteComponent.linkTypeDropdown.getSelectedItem());
        Assertions.assertEquals("Goal", linkNoteComponent.linkTitleString);
        Assertions.assertEquals("Test Goal", linkNoteComponent.noteTextField.getText());

        linkNoteComponent.clear(); // Just this once, to get coverage here.
    }

    @Test
    void testLinkGoalNote() {
        LinkNoteComponent linkNoteComponent = new LinkNoteComponent(this, makeGoalNoteLink(), 0);

        Assertions.assertEquals(LinkedEntityData.LinkType.DEPENDING_ON, linkNoteComponent.linkTypeDropdown.getSelectedItem());
        Assertions.assertEquals("Goal: Test Goal", linkNoteComponent.linkTitleString);
        Assertions.assertEquals("important milestone", linkNoteComponent.noteTextField.getText());
    }

    @Test
    void testLinkEventGroup() {
        LinkNoteComponent linkNoteComponent = new LinkNoteComponent(this, makeEventGroupLink(), 0);

        Assertions.assertEquals(LinkedEntityData.LinkType.DEPENDED_ON_BY, linkNoteComponent.linkTypeDropdown.getSelectedItem());
        Assertions.assertEquals("Event", linkNoteComponent.linkTitleString);
        Assertions.assertEquals("Conventions", linkNoteComponent.noteTextField.getText());
    }

    @Test
    void testLinkEventNote() {
        LinkNoteComponent linkNoteComponent = new LinkNoteComponent(this, makeEventNoteLink(), 0);

        Assertions.assertEquals(LinkedEntityData.LinkType.BEFORE, linkNoteComponent.linkTypeDropdown.getSelectedItem());
        Assertions.assertEquals("Event: Conventions", linkNoteComponent.linkTitleString);
        Assertions.assertEquals("Comic-Con", linkNoteComponent.noteTextField.getText());
    }

}