import java.time.ZonedDateTime;
import java.util.UUID;

public class BaseData {
    static boolean loading = false;
    protected String zdtLastModString;
    protected UUID instanceId;

    BaseData() {
        instanceId = UUID.randomUUID();
        if(!loading) {
            zdtLastModString = ZonedDateTime.now().toString();
        }
    }

    @Override
    public boolean equals(Object theOtherOne) {
        if (getClass() != theOtherOne.getClass()) return false;
        return instanceId.toString().equals(((BaseData) theOtherOne).instanceId.toString());
    }
}
