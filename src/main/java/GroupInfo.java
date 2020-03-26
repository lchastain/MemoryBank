import com.fasterxml.jackson.core.type.TypeReference;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.Vector;

// This class is a bit bloated, currently.  Both the definition of a single GroupInfo is here, and
// then it has a static loader for a list of them.  Look for a better way, but this is workable for now
// because the one list itself is also static.

// We extend from BaseData not for the inherited instanceId (which is not needed or used) but for the
// tracking of the changes to the name of the Group, via the inherited Last Mod member.

public class GroupInfo extends BaseData {
    String fullName; // The group's filename without path, but has prefix and ending.

    // This member pertains to the list of groups, not this group; it might move out.
    static String fileName; // The absolute path

    static {
        fileName = MemoryBank.userDataHome + File.separatorChar + "GroupNames.json";
    }

    // Used by the Jackson code during load / conversion of the list to a Vector.
    GroupInfo() {
        super();
    }

    // theName is expected to be the filename (without the path)
    GroupInfo(String theName) {
        super();
        fullName = theName;
    }

    // There is a potential problem here - group name uniqueness is only constrained because
    // they translate to colocated file names but persistence will eventually change to a
    // database, at which time we could begin to return the wrong group in cases with
    // duplicate group names, if the users begin to do that and we haven't disallowed.
    // Since the groupName includes a prefix, for now we still have the uniqueness across group
    // categories that is needed to be able to find and return the correct (unique) value.
    static GroupInfo getGroupInfo(String groupName) {
        // groupName should have a prefix with underscore and end in '.json' (for now).
        for(Object object: MemoryBank.groupNames) {
            GroupInfo groupInfo = (GroupInfo) object;
            if(groupInfo.fullName.equals(groupName)) {
                return groupInfo;
            }
        }
        return null;
    }

    static GroupInfo getGroupInfo(UUID theId) {
        for(Object object: MemoryBank.groupNames) {
            GroupInfo groupInfo = (GroupInfo) object;
            if(groupInfo.instanceId == theId) {
                return groupInfo;
            }
        }
        return null;
    }

    String getGroupName() {
        int underscore = fullName.indexOf('_');
        int theJson = fullName.lastIndexOf(".json");
        if(theJson > 0) return fullName.substring(underscore+1, theJson);
        return fullName.substring(underscore+1);
    }

    String getGroupType() {
        String theType;
        if(fullName.startsWith("event_")) theType = "Event";
        else if(fullName.startsWith("goal_")) theType =  "Goal";
        else if(fullName.startsWith("todo_")) theType =  "To Do List";
        else theType = "Unknown";  //  Any others - formats are: Dmmdd_ or Mmm_ or Y_
        // SearchResultGroup names are not addressed since they cannot be linked.
        return theType;
    }

    static void load() {
        File theFile = new File(fileName);

        if(theFile.exists()) {
            Object[] theGroup = FileGroup.loadFileData(GroupInfo.fileName);
            MemoryBank.groupNames = AppUtil.mapper.convertValue(theGroup, new TypeReference<Vector<GroupInfo>>() {
            });
        } else {
            MemoryBank.groupNames  = new Vector<>();
        }
    }

    // We have too many 'save' methods that look like this one, or close enough.
    //  Need to consolidate.     Subjects, Defaults, Locations, NoteGroups, etc.
    static void save() {
        MemoryBank.debug("Saving Group Names in " + fileName);

        try (FileWriter writer = new FileWriter(fileName);
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(AppUtil.toJsonString(MemoryBank.groupNames));
            bw.flush();
        } catch (IOException ioe) {
            String ems = ioe.getMessage();
            ems = ems + "\nGroup Names save operation aborted.";
            MemoryBank.debug(ems);
        } // end try/catch
    }

}
