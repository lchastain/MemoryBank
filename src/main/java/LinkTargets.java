import java.util.UUID;
import java.util.Vector;

public class LinkTargets extends Vector<LinkedEntityData> {
    private static final long serialVersionUID = 1L;

    LinkTargets() {
        super(0, 1);
    }

    boolean removeLink(UUID theId) {
        for(LinkedEntityData linkedEntityData: this) {
            if(linkedEntityData.instanceId.toString().equals(theId.toString())) {
                return remove(linkedEntityData);
            }
        }
        return false;
    }
}
