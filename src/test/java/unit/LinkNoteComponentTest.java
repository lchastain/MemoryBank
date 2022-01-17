import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// LinkNoteComponent is already tested for most of its coverage via the functional test, but we still
// need to ensure that it correctly displays its content.
// Four tests of this so far (of 12), but then might want to look at reversals as well, IF we can get them created via
// the editor panel code and not directly, as we do here.

class LinkNoteComponentTest extends LinkHelper implements NoteComponentManager {
    public void shiftDown(int index)  {}
    public void shiftUp(int index) {}

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

    @Test
    void testSwap() {
        LinkNoteComponent linkNoteComponent1 = new LinkNoteComponent(this, makeEventNoteLink(), 0);
        linkNoteComponent1.makeLinkTitle();

        LinkNoteComponent linkNoteComponent2 = new LinkNoteComponent(this, makeGoalNoteLink(), 0);
        linkNoteComponent2.makeLinkTitle();

        //System.out.println("Event Note link before swap with link to Goal Note: " + AppUtil.toJsonString(linkNoteComponent1.getLinkTarget()));
        System.out.println("  linkTitleString: " + linkNoteComponent1.linkTitleString);
        //System.out.println("Goal Note link before swap with link to Event Note: " + AppUtil.toJsonString(linkNoteComponent2.getLinkTarget()));

        linkNoteComponent1.swap(linkNoteComponent2);
        linkNoteComponent1.makeLinkTitle();
        System.out.println("  linkTitleString: " + linkNoteComponent1.linkTitleString);
        linkNoteComponent2.makeLinkTitle();
        Assertions.assertEquals(GroupType.GOALS, linkNoteComponent1.getLinkTarget().getTargetGroupInfo().groupType);
        Assertions.assertEquals("important milestone", linkNoteComponent1.getLinkTarget().getTargetNoteInfo().noteString);
        Assertions.assertEquals("Goal: Test Goal", linkNoteComponent1.linkTitleString);
        Assertions.assertEquals(LinkedEntityData.LinkType.DEPENDING_ON, linkNoteComponent1.linkTypeDropdown.getSelectedItem());

        //System.out.println("Event Note link after swap with link to Goal Note: " + AppUtil.toJsonString(linkNoteComponent1.getLinkTarget()));
        //System.out.println("Goal Note link after swap with link to Event Note: " + AppUtil.toJsonString(linkNoteComponent2.getLinkTarget()));

    }

}