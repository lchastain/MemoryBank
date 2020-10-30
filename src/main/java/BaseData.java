import java.time.ZonedDateTime;
import java.util.UUID;

public class BaseData {
    static boolean loading = false;
    protected String zdtLastModString;
    protected UUID instanceId;

    BaseData() {
        instanceId = UUID.randomUUID();
        if(!loading) {  // This mechanism is definitely needed.
            // I would think that jackson would run this constructor and then overlay zdtLastModString
            // with the value that it deserialized but no, end result is that without this condition, loaded
            // data comes in as 'new'.  Don't need to fully understand it, just dealing with the way that it
            // is known to operate.
            zdtLastModString = ZonedDateTime.now().toString();

            // So what about instanceId?  Are you getting a new one of those every time data loads in?  If so
            // then this is a big problem, or at a minimum that assignment also needs to move into the
            // conditional block.
        }
    }

//    @Override
//    public boolean equals(Object theOtherOne) {
//        // Breakpoint until we find who calls / needs this - IJ gives >1000 results
//        if (getClass() != theOtherOne.getClass()) return false;
//        return instanceId.toString().equals(((BaseData) theOtherOne).instanceId.toString());
//    }

//    // This is used during uniqueness checking.  This method effectively disables
//    // the 'hashcode' part of the check, so that the only remaining uniqueness criteria
//    // is the result of the .equals() method.
//    @Override
//    public int hashCode() {
//        return 1;
//    }


}
