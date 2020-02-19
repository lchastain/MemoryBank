
public class Goal extends NoteData {
//    private String theGroupFilename;

    // Specific to a Goal -
    //-----------------------------------------------------------------------------------
    // NoteData.noteString will be used as the Goal statement / title.
    // NoteData.extendedNoteString will hold the high-level Plan.
    // NoteData.noteId will be the ID of the goal
    // NoteData.linkages will be the list of linkages from other entities to this Goal.

    int goalStatus;

    static float percentageComplete; // Did some research to consider float vs double.
    // And the results were inconclusive.  For a percentage with two decimal points we
    // don't need the extra precision that a double would give, but some argue that all
    // modern processors are 64-bit so that using only 32 bits would actually take longer.
    // But then there are others who claim that the 'modern' processors are optimized to
    // do 32-bit operations when appropriate, so that they are faster.  Decided that I
    // like that answer but speed isn't the issue here anyway, so the smaller data type
    // wins out due to being the 'best fit' for the computational need.
    //-----------------------------------------------------------------------------------


    static {
        MemoryBank.trace();
    } // end static

    public Goal(String fname) {
        super();


    }



}
