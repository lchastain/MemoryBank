// Initially this class was going to extend a NoteData, but after going to some lengths to make it workable,
//   that approach was abandoned, for two main reasons - First, I didn't want to reuse the inherited data
//   members with names that did not match their intended usages (like using noteString in place of the Goal
//   Title), and I really did not like the need to @JsonIgnore the linkages in order to get this one to
//   serialize.  Secondly, this approach is contrary to how the other Groups with properties define their
//   members, and it would motivate me to shoehorn them in as well, for consistency, when the real direction
//   I needed to go was back out, to a more appropriate class.  Turns out that the main reusables from
//   NoteData are the ID and Last Mod Data, so now those have been pulled out into the new base class
//   (BaseData) that both NoteData and group Properties extend, down separate paths.

// To elaborate on the 'linkages' problem touched on above - when this class extended from NoteData, some
//   thought was given to using linkages as the GoalGroup DataVector, but that was seen as being too
//   far outside the norm of how the other NoteGroups access their displayed data, while keeping
//   their metadata in a separate 'properties' bucket.  Another alternative would have been for it to hold
//   another copy of the linkages from the data vector, but there seems to be no need to do that;
//   it would be a wasteful redundancy.  And finally, the problem of infinite recursion
//   during serialization that was seen by LinkData would be here as well, so just like with that one,
//   it would need to be overloaded and ignored.

public class LogGroupProperties extends GroupProperties {
    // From BaseData this class gets its ID and Last Mod Date.
    GroupProperties parentGroupProperties;

    static {
        MemoryBank.trace();
    } // end static

    public LogGroupProperties() {} // Needed / used by Jackson.

    // A stand-alone Log
    public LogGroupProperties(String groupName) {
        super(groupName, GroupType.LOG);
        parentGroupProperties = null;
    }

    // A Log that is attached to some higher-level NoteGroup, such as a Goal.
    public LogGroupProperties(String groupName, GroupProperties theParentProperties) {
        super(groupName, GroupType.LOG);
        parentGroupProperties = theParentProperties;
    }


}
