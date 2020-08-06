// This class is for describing the target of a connection to another entity.

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Random;

public class LinkedEntityData extends BaseData {
    // Unfortunately these two were stored in some notes, before the 'Info' classes
    // came into use.  So the ignore is needed for now but once they are all
    // gone, can replace that with a 'private transient' scoping.
    @JsonIgnore GroupProperties targetGroupProperties; // Seen in older data...
    @JsonIgnore NoteData targetNoteData; // Seen in older data...
    // When will they all BE gone, you ask?  Either as we port to a db, or as the
    // result of running a 'data fix', both of which are scheduled for: TBD.

    transient boolean deleteMe;
    transient boolean showMe;
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

    // This constructor is used by Jackson when loading the class from a file, and
    // its usage internally here is followed by the additional needed declarations.
    // Otherwise this constructor should not be used directly because we do not want
    // to allow the targetGroupInfo to ever be null.
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

    public LinkedEntityData(GroupInfo groupInfo, NoteInfo noteInfo) {
        this();
        targetGroupInfo = groupInfo;
        targetNoteInfo = noteInfo;
        //makeLinkTitle();   // Can't be done for loaded data, so instead we do this from the LinkNoteComponent that needs it.
    }

    // The copy constructor (clone) - used by 'swap' code and reverse link creation.
    public LinkedEntityData(LinkedEntityData theCopy) {
        this();

        // For these two we really do want the references to point to the same/original entity.
        targetGroupProperties = theCopy.targetGroupProperties;
        targetNoteData = theCopy.targetNoteData;

        // These two are only used in displays; not modified, so references are ok to be reused.
        targetGroupInfo = theCopy.targetGroupInfo;
        targetNoteInfo = theCopy.targetNoteInfo;

        // These primitive types are the true copies
        linkType = theCopy.linkType;
        deleteMe = theCopy.deleteMe;
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
        String groupName = targetGroupInfo.getName(); // User-provided at group creation, except for Notes, which are date-based.

        if(targetNoteInfo == null) {
            theTitleString = category;
            if(!category.equals(groupType)) theTitleString += ": " + groupType;
        } else {
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

    // Used by the test driver
    public void setLinkTargetGroupInfo(GroupInfo newGroupInfo) {
        targetGroupInfo = newGroupInfo;
    }

    public void setLinkTargetNoteInfo(NoteInfo newNoteInfo) {
        targetNoteInfo = newNoteInfo;
    }

}
