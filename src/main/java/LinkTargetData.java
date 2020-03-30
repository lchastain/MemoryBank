// This class is for describing the target of a connection to another entity.

import java.util.Random;
import java.util.UUID;

// This class does not extend a NoteData, because it encapsulates a NoteData member and would
//   not be serializable if it did extend from NoteData, due to infinite recursion.

// It does extend BaseData, because it needs to have its own unique id separate of the one for the
//   NoteData instance that it contains, so that it can distinguish between duplicate links.

public class LinkTargetData extends BaseData {

    private UUID targetGroupId;
    private NoteData targetNoteData;

    transient boolean deleteMe;
    transient String linkTitle;
    static Random random = new Random();

    // Each of these values needs to have a converse that is also in the list.
    // This is because a link may be viewed from either perspective - source
    // or target.  Since the type is a source perspective, it will have to
    // be inverted when viewing the links of a target.
    enum LinkType {
        RELATED("related to"),
        DEPENDING_ON("depending on"),
        DEPENDED_ON_BY("depended on by"),
        BEFORE("before"),
        DURING("same time or during"),
        AFTER("after");

        // Used in dev/test
        public static LinkType getRandomType() {
            return values()[random.nextInt(values().length)];
        }

        private final String display;

        LinkType(String s) {
            display = s;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    LinkTargetData.LinkType theType;     // Says what kind of connection this is.  Values defined above.
    // status - or 'order' / priority

    public LinkTargetData() {
        super();
        theType = LinkTargetData.LinkType.RELATED;
    }

    public LinkTargetData(UUID theGroupId, NoteData theNoteData) {
        this();
        targetGroupId = theGroupId;
        targetNoteData = theNoteData;
    }

    // The copy constructor (clone) - used by 'swap' code.
    public LinkTargetData(LinkTargetData theCopy) {
        this();

        // These may not be default values.
        targetGroupId = theCopy.targetGroupId;
        targetNoteData = theCopy.targetNoteData;
        theType = theCopy.theType;
        deleteMe = theCopy.deleteMe;
        linkTitle = theCopy.linkTitle;
    } // end constructor


    public UUID getTargetGroupId() {
        return targetGroupId;
    }

    public NoteData getTargetNoteData() {
        return targetNoteData;
    }

    public void setLinkTargetGroupId(UUID newGroupId) {
        targetGroupId = newGroupId;
    }

    public void setLinkTargetNoteData(NoteData newNoteData) {
        targetNoteData = newNoteData;
    }

}
