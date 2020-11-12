public class LinkHelper {
    LinkHelper() {

    }

    LinkedEntityData makeGoalGroupLink() {
        GroupInfo groupInfo = new GroupInfo("Test Goal", GroupType.GOALS);
        NoteInfo noteInfo = null;
        LinkedEntityData linkedEntityData = new LinkedEntityData(groupInfo, noteInfo);
        linkedEntityData.linkType = LinkedEntityData.LinkType.RELATED;
        return linkedEntityData;
    }

    LinkedEntityData makeGoalNoteLink() {
        GroupInfo groupInfo = new GroupInfo("Test Goal", GroupType.GOALS);
        NoteInfo noteInfo = new NoteInfo();
        noteInfo.setNoteString("important milestone");
        LinkedEntityData linkedEntityData = new LinkedEntityData(groupInfo, noteInfo);
        linkedEntityData.linkType = LinkedEntityData.LinkType.DEPENDING_ON;
        return linkedEntityData;
    }

    LinkedEntityData makeEventGroupLink() {
        GroupInfo groupInfo = new GroupInfo("Conventions", GroupType.EVENTS);
        NoteInfo noteInfo = null;
        LinkedEntityData linkedEntityData = new LinkedEntityData(groupInfo, noteInfo);
        linkedEntityData.linkType = LinkedEntityData.LinkType.DEPENDED_ON_BY;
        return linkedEntityData;
    }

    LinkedEntityData makeEventNoteLink() {
        GroupInfo groupInfo = new GroupInfo("Conventions", GroupType.EVENTS);
        NoteInfo noteInfo = new NoteInfo();
        noteInfo.setNoteString("Comic-Con");
        LinkedEntityData linkedEntityData = new LinkedEntityData(groupInfo, noteInfo);
        linkedEntityData.linkType = LinkedEntityData.LinkType.BEFORE;
        return linkedEntityData;
    }
}
