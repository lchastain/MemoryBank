public interface LinkHolder {

    default void addReverseLink(LinkedEntityData linkedEntityData) {
        GroupInfo otherEndGroupInfo = linkedEntityData.getTargetGroupInfo();
        NoteInfo otherEndNoteInfo = linkedEntityData.getTargetNoteInfo();

        // Use the target GroupInfo to get a reference to the group.
        // For now, this will be a Panel, but as a NoteGroup child this still works.  more on that to come....
        NoteGroup groupToSave = otherEndGroupInfo.getNoteGroup();

        // We need to be sure that the link points to a valid group.
        assert groupToSave != null;

        // Create the reverse link
        LinkedEntityData reverseLinkedEntityData = createReverseLink(linkedEntityData);

        // Attach the reversed link to the original link target, and then re-persist its Group.
        if (otherEndNoteInfo != null) {
            groupToSave.getLinkTargets(otherEndNoteInfo).add(reverseLinkedEntityData);
        } else {
            groupToSave.getGroupProperties().linkTargets.add(reverseLinkedEntityData);
        }

        // Use the reference to persist the Group with the new reverse link.
        groupToSave.saveNoteGroup(); // This saves the updated Group.
    }


    // Consider this method - with the LinkTargets being sent in as a parameter, it is interesting that an implementer
    //   of this interface does not even need to keep its own link targets; they might come from/go to some other entity
    //   and in fact that is the case for the NoteGroup implementation, where its links are actually in its properties
    //   and not directly off of the NoteGroup itself.  If not for the reachout to createReverseLink(), this entire
    //   interface might have instead been just one or two static methods.
    // But being that this is a default method, implementers do not need to provide their own duplicate code for
    //   looping; this is somewhat of a 'cheat' in terms of getting around the inability of Java to support multiple
    //   inheritance; it achieves the same effect, reducing code duplication and thereby increasing code coverage.
    default void  addReverseLinks(LinkTargets linkages) {
        for (LinkedEntityData linkedEntityData : linkages) {
            // Since we are looking through ALL links to see if we need to handle new ones, we will also see the
            // ones (if any) that were already there.   In those cases we don't want to setNotes a reverse link for them
            // because that already happened when they first appeared, and we don't want them to pile up; only one
            // reverse link per forward link is allowed.  We can know that a forward link pre-existed based
            // on whether or not we are allowed to change its type, because only new links are allowed to be
            // re-'typed'.  And since no reverse link is allowed to be re-typed, the same logic prevents a reverse
            // link from being created for a link that is itself already a reverse link, regardless of whether or
            // not it is new.
            if (!linkedEntityData.retypeMe) continue;

            addReverseLink(linkedEntityData);
        }
    }

    LinkedEntityData createReverseLink(LinkedEntityData linkedEntityData);

}
