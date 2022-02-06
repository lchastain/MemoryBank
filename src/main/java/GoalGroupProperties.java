public class GoalGroupProperties extends GroupProperties {
    // From BaseData this class gets its ID and Last Mod Date.
    String longTitle;  // A single line of text, may be more descriptive of the goal than its title alone.
    String goalPlan;   // Full textual description of the Goal

    enum CurrentStatus {
        UNDERWAY("Underway"),
        STALLED("Stalled");

        private final String display;

        CurrentStatus(String s) {
            display = s;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    // currentStatus - the immediate status; what is happening on this goal right now.
    //      Underway - working on it, or at least not stalled
    //      Stalled  - might be waiting, maybe a (temporary?) loss of drive
    //
    CurrentStatus currentStatus;


    enum OverallStatus {
        OFF_COURSE("Off Course"),
        ON_TRACK("On Track"),
        AHEAD_OF_PLAN("Ahead of Plan");

        private final String display;

        OverallStatus(String s) {
            display = s;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    // overallStatus - related to progress (you could be stalled but still be on track)
    //      Off Course
    //      On Track
    //      Ahead of Plan
    OverallStatus overallStatus;


    static float percentageComplete; // Did some research to consider float vs double.
    // And the results were inconclusive.  For a percentage with two decimal points we
    // don't need the extra precision that a double would give, but some argue that all
    // modern processors are 64-bit so that using only 32 bits would actually take longer.
    // But then there are others who claim that the 'modern' processors are optimized to
    // do 32-bit operations when appropriate, so that they are faster.  Decided that I
    // like that answer but speed isn't the issue here anyway, so the smaller data type
    // wins out due to being the 'best fit' for the computational memory requirements.
    // BUT - currently there is no usage of this member; progress, if kept, will be a
    // user-determined qualitative value, not calculated by this app.


    static {
        MemoryBank.trace();
    } // end static

    public GoalGroupProperties() {} // Needed / used by Jackson.

    public GoalGroupProperties(String groupName) {
        super(groupName, GroupType.GOALS);
        currentStatus = CurrentStatus.UNDERWAY;
        overallStatus = OverallStatus.ON_TRACK;
    }

}
