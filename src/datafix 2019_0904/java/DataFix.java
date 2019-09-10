import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

// The code for scanDataDir was copied out of the AppTreePanel Search capability
// (and modified as needed for use here).  The code to load and save NoteData
// came out of NoteGroup (before it was moved to AppUtil).  Future revs of this
// fixer will need to go and acquire the latest versions of these three methods;
// the other code here was developed here, only, and can be morphed as needed
// when this module is cloned for the next fix.

public class DataFix {
    // Container for the (complete collection of) Group data objects
    protected Vector<NoteData> vectGroupData;

    private DataFix() {
    }

    private void scanDataDir(File theDir, int level) {
        MemoryBank.dbg("Scanning " + theDir.getAbsolutePath());

        File[] theFiles = theDir.listFiles();
        assert theFiles != null;
        int howmany = theFiles.length;
        MemoryBank.debug("\t\tFound " + howmany + " items");

        for (File aFile : theFiles) {
            String aFileName = aFile.getName();
            if (aFile.isDirectory()) {
                if (aFileName.equals("Archives")) continue;
                if (aFileName.equals("icons")) continue;
                scanDataDir(aFile, level + 1);
            } else {
                // The list below needs to be a complete list of any files that
                // we don't want to look at; these 'other' type data files
                // will not load properly.  There are other ways to handle this
                // situation but this is what was chosen - it works well enough.
                if (aFileName.endsWith(".json")) continue;
                if (aFileName.endsWith(".dump")) continue;
                if (aFileName.endsWith(".txt")) continue;
                if (aFileName.equals("DaySubjects")) continue;
                if (aFileName.equals("DayNoteDefaults")) continue;
                if (aFileName.equals("EventNoteDefaults")) continue;
                if (aFileName.equals("UpcomingSubjects")) continue;
                if (aFileName.equals("YearSubjects")) continue;
                if (aFileName.equals("MonthSubjects")) continue;
                if (aFileName.startsWith("Export")) continue;
                fixTheFile(aFile);
            } // end if
        }//end for i
    } //end scanDataDir

    private static Vector<NoteData> loadData(String theFilename, Object[] objArray) {
        Vector<NoteData> vectNoteData;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        Object tempObject = null;
        NoteData tempNoteData;
        boolean blnStartOfFile = true;
        Exception e = null;

        vectNoteData = new Vector<>();

        // The file's existence should have already
        //   tested true, prior to calling loadData.
        try {
            fis = new FileInputStream(theFilename);
            ois = new ObjectInputStream(fis);

            // Since there IS a file, the assumption is that
            //  there is at least one element of data.
            while (true) {
                try {
                    tempObject = ois.readObject();

                    // If this is the Group properties rather than a
                    //   NoteData, an exception will be thrown (and caught).
                    tempNoteData = (NoteData) tempObject;

                    vectNoteData.addElement(tempNoteData);
                    blnStartOfFile = false;
                } catch (ClassCastException cce) {
                    // The first data element may be the group properties and not
                    //   a NoteData.  In that case, we can assign it here
                    //   and continue on; otherwise we have a problem.
                    if (blnStartOfFile) {
                        objArray[0] = tempObject;
                    } else {
                        e = cce;
                        break;
                    } // end if
                } // end try/catch
            }//end while
        } catch (EOFException eofe) { // Normal, expected.
        } catch (ClassNotFoundException | IOException ee) {
            e = ee;
        } finally {
            try {
                if (ois != null) ois.close();
                if (fis != null) fis.close();
            } catch (IOException ioe) {
                System.out.println("Exception: " + ioe.getMessage());
            } // end try/catch
        } // end try/catch

        if (e != null) {
            e.printStackTrace(System.err);
            // If there was a partial load, we'll allow that data.
        } // end if
        return vectNoteData;
    } // end loadData

    // This could be a bit cleaner, but wanted to preserve the current state of this method
    // in NoteGroup, as much as possible.  Changed only enough to also support search results,
    // while leaving the code looking almost identical although not as efficient.
    // Again - consider the one-shot nature of this utility.
    private int saveDataJson(String theFilename, Object objProperties) {
        BufferedWriter bw = null;
        Exception e = null;
        int notesWritten = 0;
        Object[] theGroup;  // A new 'wrapper' for the Properties + List
        Vector<NoteData> trimmedList = vectGroupData; // We're not trimming or checking length, for this op.

        // Wrap the remaining content, convert it to JSON, and write the file
        try {
            FileOutputStream fileStream = new FileOutputStream(new File(theFilename + ".json"));
            OutputStreamWriter writer = new OutputStreamWriter(fileStream, StandardCharsets.UTF_8);
            bw = new BufferedWriter(writer);

            if (objProperties != null) {
                theGroup = new Object[2];
                theGroup[0] = objProperties;
                theGroup[1] = trimmedList;
            } else {
                theGroup = new Object[1];
                theGroup[0] = trimmedList;
            } // end if there is a properties object

            bw.write(AppUtil.toJsonString(theGroup));
            notesWritten = trimmedList.size(); // This is only set AFTER the write.

        } catch (IOException ioe) {
            // This is a catch-all for other problems that may arise, such as finding a subdirectory of the
            // same name in the directory where you want to put the file, or not having write permission.
            e = ioe;
        } finally {
            if (e != null) {
                // This one may have been ignorable; print the message and see.
                System.out.println("Exception: " + e.getMessage());
            } // end if there was an exception
            // These flush/close lines may seem like overkill, but there is internet support for being so cautious.
            try {
                if (bw != null) {
                    bw.flush();
                    bw.close(); // Also closes the wrapped FileWriter
                }
            } catch (IOException ioe) {
                // This one would be much more serious - raise a 'louder' alarm.
                ioe.printStackTrace(System.err);
            } // end try/catch
        } // end try/catch

        return notesWritten;
    } // end saveDataJson


    // This particular fix will load serialized data and write back out JSON data.
    // And - some of the filenames need to change.
    private void fixTheFile(File theFile) {
        String strGroupFilename = theFile.getAbsolutePath();
        System.out.println("Fixing " + strGroupFilename);
        Object[] objArray = new Object[1];
        vectGroupData = loadData(strGroupFilename, objArray);
        // The properties for the group - cast to proper class
        Object objGroupProperties = objArray[0];

        // Example of fixing data (vs entire files)
        boolean madeChange = false; // initialize this flag
        if (strGroupFilename.endsWith("UpcomingEvents")) {
            for (NoteData tempNoteData : vectGroupData) {
                //System.out.println("Class name of data element: " + tempNoteData.getClass().getName());
                //System.out.println("EventNoteData: " + AppUtil.toJsonString(tempNoteData));
                madeChange = true;
            } // end for
        } else {
            madeChange = true;
        }

        // For this particular fix we want to rewrite every file whether it needed fixing internally or not,
        // so the boolean test below does not actually ever evaluate to 'false'.  (Fools IJ, though).
        if (madeChange) {
            String saveFileName = getNewName(strGroupFilename);
            int nw = saveDataJson(saveFileName, objGroupProperties);
            System.out.println("   wrote " + nw + " notes to " + saveFileName);

            // Now get rid of the original file.
            FileUtils.deleteQuietly(theFile);
        }//end if

    }

    private String getNewName(String strGroupFilename) {

        if (strGroupFilename.endsWith(".todolist")) {
            int nameStart = strGroupFilename.lastIndexOf(File.separatorChar);
            int index = strGroupFilename.lastIndexOf(".todolist");
            String listName = strGroupFilename.substring(nameStart+1, index);
            return strGroupFilename.substring(0, nameStart+1) + "todo_" + listName;
        }
        if (strGroupFilename.endsWith(".sresults")) {
            int nameStart = strGroupFilename.lastIndexOf(File.separatorChar);
            int index = strGroupFilename.lastIndexOf(".sresults");
            String listName = strGroupFilename.substring(nameStart+2, index); // Get past the 'S'.
            return strGroupFilename.substring(0, nameStart+1) + "search_" + listName;
        }
        return strGroupFilename;
    }

    public static void main(String[] args) {
        MemoryBank.debug = true; // Turn on all debugging printouts.
        MemoryBank.setUserDataHome("test.user@lcware.net");

        DataFix dataFix = new DataFix();
        dataFix.scanDataDir(new File(MemoryBank.userDataHome), 0);
    }
}
