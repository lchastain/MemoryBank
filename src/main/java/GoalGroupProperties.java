
public class GoalGroupProperties extends NoteData{
    // Usage of NoteData members:
    //-----------------------------------------------------------------------------------
    // NoteData.noteString will be used as the Goal statement / title.
    // NoteData.extendedNoteString will hold the high-level Plan.
    // NoteData.noteId will be the ID of the goal
    // NoteData.linkages will remain null - some thought was given to using it in place
    //   of the Group DataVector, but the Vector needs to contain GoalNoteData, not
    //   Linkages, and doing it this way allows the GoalGroup to behave just like all the
    //   others in terms of interface management, new data creation, loading, and saving.
    //   Another alternative would be to hold another copy of the all the various
    //   linkages that are defined in the source notes, but there seems to be no need to
    //   do that at this time, given that the Vector will restate anything that needs to
    //   be seen by the user, and changes to source notes will be reflected into the
    //   relevant Goal vectors as they occur; no need to 'point back', although a
    //   GoalNoteComponent may hold the Linkage from which it is created, and that would
    //   provide another path to the complete list of linkages although not as direct.

    int goalStatus;
    // Unscheduled - no particular timeline set
    // Scheduled - by a certain date
        // On track, ahead, behind

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

    public GoalGroupProperties(String fname) {
        super();
        goalStatus = 0;


    }



}
