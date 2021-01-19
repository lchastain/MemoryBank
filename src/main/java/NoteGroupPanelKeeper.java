import java.util.ArrayList;
import java.util.Vector;

/*
 Basically a wrapper for a vector of NoteGroupPanels.  The idea is
 that since loading of a list can sometimes take long enough for the user to
 notice/care, we could just load them once, and when not currently
 selected, keep them out of sight but in memory and ready to quickly redisplay.
 Using a vector vs an ArrayList, for additional speed and less overhead; we do
 not need any of the 'Collections' bells and whistles (such as sorting).
 The idea does work (fairly well, actually) but it turns out that there are
 some operations (RENAME, SAVE-AS) that after they complete, it would be better
 to remove the copy here so that upon reselection, the list will be reloaded.
 For those operations we provide the 'remove' method, and every new list action
 should consider whether or not to use it.
 */

public class NoteGroupPanelKeeper {
    private final Vector<NoteGroupPanel> theNoteGroups;

    NoteGroupPanelKeeper() {
        theNoteGroups = new Vector<>();
    }

    public void add(NoteGroupPanel tng) {
        theNoteGroups.add(tng);
        tng.myKeeper = this;
    }

    public NoteGroupPanel get(String aListName) {
        // Search the Vector for the list.
        for (NoteGroupPanel noteGroup : theNoteGroups) {
            String tngName;
            GroupProperties groupProperties = noteGroup.myNoteGroup.getGroupProperties();
            if(groupProperties != null) {
                tngName = groupProperties.getGroupName();
            } else {
                tngName = noteGroup.myNoteGroup.myGroupInfo.getGroupName();
            }
            if (aListName.equals(tngName)) {
                return noteGroup;
            } // end if
        } // end for
        return null;
    }

    ArrayList<String> getNames() {
        ArrayList<String> theList = new ArrayList<>();
        for(NoteGroupPanel noteGroupPanel: theNoteGroups) {
            theList.add(noteGroupPanel.myNoteGroup.getGroupProperties().getGroupName());
        }
        return theList;
    }

    // Scan the vector looking for the indicated group and if found, remove.
    //----------------------------------------------------------------
    public void remove(String aListName) {
        NoteGroupPanel theGroup = null; // Keep a temporary reference

        // Search the Vector for the group.
        for (NoteGroupPanel noteGroup : theNoteGroups) {
            String tngName = noteGroup.myNoteGroup.getGroupProperties().getGroupName();
            if (aListName.equals(tngName)) {
                theGroup = noteGroup;
                // Note: cannot remove from within this loop;
                // ConcurrentModificationException.
                break;
            } // end if
        } // end for

        // If found, then remove.  Otherwise no action needed.
        if (theGroup != null) {
            MemoryBank.debug("  Removing " + aListName + " from the NoteGroupKeeper");
            theNoteGroups.removeElement(theGroup);
//            theGroup.myKeeper = null; // This may be overkill, now that it's removed.  But just in case...
// NO - we could need this if a deletion is undone.
        } else {
            MemoryBank.debug("  Unable to remove " + aListName + "; it was not found in the NoteGroupKeeper");
        } // end if
    } // end remove

    void saveAll() {
        for(NoteGroupPanel aNoteGroup: theNoteGroups) {
            aNoteGroup.preClosePanel();
        }
    }

    int size() {
        return theNoteGroups.size();
    }

} // end class NoteGroupPanelKeeper
