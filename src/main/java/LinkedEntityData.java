// This class is for describing the target of a connection to another entity.

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Random;

public class LinkedEntityData extends BaseData {
    // Unfortunately these two were stored in some notes, before the 'Info' classes
    // came into use.  So the ignore is needed for now but once they are all
    // gone, can replace that with a 'private transient' scoping.
    @JsonIgnore GroupProperties targetGroupProperties; // Seen in older persisted data...
    @JsonIgnore NoteData targetNoteData; // Seen in older persisted data...
    // When will they all BE gone, you ask?  Either as we port to a db, or as the
    // result of running a 'data fix', both of which are scheduled for: TBD.
    //
    // But there just are not that many currently existing links out there -
    // possible was from late March until Aug 2020, with minimal/no creations.
    // Suggest - just go for it, and manually fix any problems that arise.

    transient boolean deleteMe;  // Only for keeping state of the checkbox while dialog is active.
    transient boolean showMe;    // Do not show the link if its group is not active.
    transient boolean retypeMe;  // Only 'new' link types will be changeable.
    transient String linkTitle;
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

    // This default constructor is used by Jackson when loading the predefined class from a
    //      file, and in that case no further grooming of the data members occurs -
    //      (even though we might want to).
    // The other constructors here do use this one, but they continue on to fully define the data
    //      entity when making a new one.
    // Otherwise, this constructor should not be called and so it is scoped as 'private'.
    //      This ensures that the targetGroupInfo will never be null.
    private LinkedEntityData() {
        super();
        linkType = LinkedEntityData.LinkType.RELATED;
        showMe = true;
    }

    // Here we make a LinkedEntityData from full-blown GroupProperties and NoteData and that's good because we
    //   snag their references, but for serialization of links we don't need or want all the extra baggage that they
    //   have, so for construction of this class we reduce them to their base classes via the use of their
    //   base class copy constructors.  This way, the data stored is smaller, and most importantly, loads back in
    //   without complaint.  Note that the base class copy constructors do an implicit cast of their parameters
    //   and this works seamlessly since the parameters are children of those base classes.
    public LinkedEntityData(GroupProperties groupProperties, NoteData noteData) {
        this(new GroupInfo(groupProperties), new NoteInfo(noteData));
        targetGroupProperties = groupProperties;
        targetNoteData = noteData;
    }

    public LinkedEntityData(GroupProperties groupProperties) {
        this(new GroupInfo(groupProperties), null);
        targetGroupProperties = groupProperties;
        targetNoteData = null;
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

        // For these two we really do want the references to point to the same/original entities;
        //   we don't need to make new entities by calling thier copy constructors.
        targetGroupProperties = theCopy.targetGroupProperties;
        targetNoteData = theCopy.targetNoteData;

        // These two are only used in displays; not modified, so references are ok to be reused.
        targetGroupInfo = theCopy.targetGroupInfo;
        targetNoteInfo = theCopy.targetNoteInfo;

        // These primitive types are the true copies
        linkType = theCopy.linkType;
        deleteMe = theCopy.deleteMe;
        retypeMe = theCopy.retypeMe;
        reversed = theCopy.reversed; // This will be overridden if not being called during a swap.

        makeLinkTitle(); // Needed for swap operation, TODO - check the result when used for reverse links.
    } // end constructor


    public GroupInfo getTargetGroupInfo() {
        return targetGroupInfo;
    }

    GroupProperties getTargetGroupProperties() {
        return targetGroupProperties;
    }

    NoteData getTargetNoteData() {
        return targetNoteData;
    }

    public NoteInfo getTargetNoteInfo() {
        return targetNoteInfo;
    }

    void makeLinkTitle() {
        String theTitleString;

        String category = targetGroupInfo.getCategory();  // Note, Goal, Event, or To Do List
        String groupType = targetGroupInfo.groupType.toString(); // Same as above except for Notes, which are more specific
        String groupName = targetGroupInfo.getSimpleName(); // User-provided at group creation, except for Notes, which are date-based.

        if(targetNoteInfo == null) { // The link is to a full group
            theTitleString = category;
            if(!category.equals(groupType)) theTitleString += ": " + groupType;
        } else { // The link is to a specific Note within a Group
            if(groupType.equals("Day Note")) {
                // Drop the leading spelled-out day name and comma-space
                String shorterName = groupName.substring(groupName.indexOf(",") + 1);
                theTitleString = groupType + ": " + shorterName;
            } else {
                theTitleString = groupType + ": " + groupName;
            }
        }

        linkTitle = theTitleString;
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
