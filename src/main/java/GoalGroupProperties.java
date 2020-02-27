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


public class GoalGroupProperties extends BaseData {
    // From BaseData this class gets its ID and Last Mod Date.
    String goalTitle;  // A single line of text, descriptive of the goal
    String goalPlan;   // The plan statement at the high level, not including Todo item-like steps.

//    int goalStatus;  // This is the overall status, as opposed to status of link sources.
// Do not go 'live' until naming is more certain.  We still need the immediate status, as well as the overall.
    // Unscheduled - no particular timeline set
    // Scheduled - by a certain date
        // On track, ahead, behind
    // Could be 'calculated', but allow the user to override.


    static float percentageComplete; // Did some research to consider float vs double.
    // And the results were inconclusive.  For a percentage with two decimal points we
    // don't need the extra precision that a double would give, but some argue that all
    // modern processors are 64-bit so that using only 32 bits would actually take longer.
    // But then there are others who claim that the 'modern' processors are optimized to
    // do 32-bit operations when appropriate, so that they are faster.  Decided that I
    // like that answer but speed isn't the issue here anyway, so the smaller data type
    // wins out due to being the 'best fit' for the computational memory requirements.
    //-----------------------------------------------------------------------------------


    static {
        MemoryBank.trace();
    } // end static

    public GoalGroupProperties() {
        super();
//        goalStatus = 0;


    }



}
