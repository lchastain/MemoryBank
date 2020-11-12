// This class is for describing the target of a connection to another entity.

import java.util.Random;

public class LinkedEntityData extends BaseData {
    transient boolean deleteMe;  // Only for keeping state of the checkbox while dialog is active.
    transient boolean showMe;    // Do not show the link if its group is not active.
    transient boolean retypeMe;  // Only 'new' link types will be changeable.
    static Random random = new Random();

    // Each of these values needs to have an inverse that is also in the list.  This
    // is because we only define and view a link as though it is the source because
    // of where it will be stored/found; making a link go 'both ways' doesn't
    // help if you don't know where to look for it, so instead we make two
    // separate links, each pointing to the other, and each stored on the
    // note or group that has the 'source' perspective of the link.
    enum LinkType {
        RELATED("related to"),              // inverse here is the same.
        DEPENDING_ON("depending on"),
        DEPENDED_ON_BY("depended on by"),
        BEFORE("before"),
        DURING("same time or during"),     // inverse here is the same.
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

    // Persisted Class members
    private GroupInfo targetGroupInfo;
    private NoteInfo targetNoteInfo;
    LinkType linkType;     // Says what kind of connection this is.  Values defined above.
    boolean reversed;

    // This default constructor is used by Jackson when loading the predefined class from persisted data.
    // The other constructors here do use this one, but they continue on to fully define the data
    //      entity when making a new one.
    // Otherwise, this constructor should not be called and so it is scoped as 'private'.
    private LinkedEntityData() {
        super();
        linkType = LinkedEntityData.LinkType.RELATED;
        showMe = true;
        touchLastMod(); // Preserve the time of link creation.
    }

    // This is the true, 'meaty' constructor, used to make a new data entity vs a copy or loaded from file.
    public LinkedEntityData(GroupInfo groupInfo, NoteInfo noteInfo) {
        this();
        targetGroupInfo = groupInfo;
        targetNoteInfo = noteInfo;
        reversed = false;
        retypeMe = true;
    }

    // The copy constructor (clone) - used by 'swap' code and reverse link creation.
    public LinkedEntityData(LinkedEntityData theCopy) {
        this();

        // These two are only used in displays; not modified, so references are ok to be reused.
        targetGroupInfo = theCopy.targetGroupInfo;
        targetNoteInfo = theCopy.targetNoteInfo;

        // These primitive types are the true copies
        linkType = theCopy.linkType;
        deleteMe = theCopy.deleteMe;
        retypeMe = theCopy.retypeMe;
        reversed = theCopy.reversed; // This will be overridden if not being called during a swap.
    } // end constructor


    public GroupInfo getTargetGroupInfo() {
        return targetGroupInfo;
    }


    public NoteInfo getTargetNoteInfo() {
        return targetNoteInfo;
    }

    LinkType reverseLinkType(LinkType linkType) {
        LinkType theReverseType;
        switch(linkType) {
            case DEPENDING_ON:
                theReverseType = LinkType.DEPENDED_ON_BY;
                break;
            case DEPENDED_ON_BY:
                theReverseType = LinkType.DEPENDING_ON;
                break;
            case AFTER:
                theReverseType = LinkType.BEFORE;
                break;
            case BEFORE:
                theReverseType = LinkType.AFTER;
                break;
            default:  // Related, During
                theReverseType = linkType;
        }
        return theReverseType;
    }

}
