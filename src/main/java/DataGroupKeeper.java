import java.util.Vector;

/*
 Basically a wrapper for a vector of DataGroups.  The idea is
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

public class DataGroupKeeper {
    private Vector<DataGroup> theNoteGroups;

    DataGroupKeeper() {
        theNoteGroups = new Vector<>();
    }

    public void add(DataGroup tng) { theNoteGroups.add(tng); }

    public DataGroup get(String aListName) {
        // Search the Vector for the list.
        for (DataGroup noteGroup : theNoteGroups) {
            String tngName = noteGroup.prettyName();
            if (aListName.equals(tngName)) {
                return noteGroup;
            } // end if
        } // end for
        return null;
    }

    // Scan the vector looking for the indicated group and if found, remove.
    //----------------------------------------------------------------
    public void remove(String aListName) {
        DataGroup theGroup = null; // Keep a temporary reference

        // Search the Vector for the group.
        for (DataGroup noteGroup : theNoteGroups) {
            String tngName = noteGroup.prettyName();
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
        } else {
            MemoryBank.debug("  Unable to remove " + aListName + "; it was not found in the NoteGroupKeeper");
        } // end if
    } // end remove

    void saveAll() {
        for(DataGroup aNoteGroup: theNoteGroups) {
            aNoteGroup.preClose();
        }
    }

    int size() {
        return theNoteGroups.size();
    }

} // end class NoteGroupKeeper
