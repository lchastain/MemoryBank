import java.io.File;
import java.time.LocalDate;

// The code for scanDataDir was taken from the current AppTreePanel, then modified slightly.

public class DataFix {

    private DataFix() {
    }

    //------------------------------------------------------------------------------------------
    // Method Name:  scanDataDir
    //
    // This method scans a directory for data files.  If it finds a directory rather than a file,
    //   it will recursively call itself for that directory, unless the directory has been
    //   explicitly excluded.  Then when it encounters a file, it will call the fixit method on
    //   that file.
    //
    //------------------------------------------------------------------------------------------
    private void scanDataDir(File theDir, int level) {
        MemoryBank.dbg("Scanning " + theDir.getName());

        File[] theFiles = theDir.listFiles();
        assert theFiles != null;
        int howmany = theFiles.length;
        MemoryBank.debug("\t\tFound " + howmany + " data files");
        boolean goLook;
        LocalDate dateNoteDate;
        MemoryBank.debug("Level " + level);

        for (File theFile : theFiles) {
            String aFileName = theFile.getName();
            if (theFile.isDirectory()) {
                if (aFileName.equals("Archives")) continue;
                if (aFileName.equals("icons")) continue;
                scanDataDir(theFile, level + 1);
            } else {
                // The list below needs to be a complete list of any files that
                // we don't want to look at; these 'other' type data files
                // will not load properly.  There are other ways to handle this
                // situation but this is what was chosen - it works well enough.
                if (aFileName.endsWith(".dump")) continue;
                if (aFileName.equals("appOpts.json")) continue;
                if (aFileName.endsWith(".txt")) continue;
                if (aFileName.equals("DaySubjects")) continue;
                if (aFileName.equals("DaySubjects.json")) continue;
                if (aFileName.equals("DayNoteDefaults")) continue;
                if (aFileName.equals("DayNoteDefaults.json")) continue;
                if (aFileName.equals("EventNoteDefaults")) continue;
                if (aFileName.equals("EventNoteDefaults.json")) continue;
                if (aFileName.equals("UpcomingSubjects")) continue;
                if (aFileName.equals("UpcomingSubjects.json")) continue;
                if (aFileName.equals("UpcomingLocations")) continue;
                if (aFileName.equals("UpcomingLocations.json")) continue;
                if (aFileName.equals("UpcomingEvents.json")) continue;
                if (aFileName.equals("YearSubjects")) continue;
                if (aFileName.equals("YearSubjects.json")) continue;
                if (aFileName.equals("MonthSubjects")) continue;
                if (aFileName.equals("MonthSubjects.json")) continue;
                fixTheFile(theFile);
            }
        }//end for
    }//end scanDataDir


    // A modification of the searchDataFile method, from AppTreePanel
    private void fixTheFile(File dataFile) {
        String theFilename = dataFile.getName();
        String theAbsolutePath = dataFile.getAbsolutePath();
        System.out.println("Fixing: " + dataFile.getAbsolutePath());

        NoteDataVector noteDataVector = null;

        // Load the data from the file.
        Object[] theGroupData = FixUtil.loadNoteGroupData(dataFile);
        if (theGroupData != null && theGroupData[theGroupData.length - 1] != null) {
            // For this particular 'fix', we don't have any changes to make; it will all be
            // handled by json-ignoring unwanted fields on data load, and then writing the data
            // back out to the file that it came from, without those fields being present.

            // But it cannot just be a simple data load; we have to form the data into its correct
            // class, in order that the constructors run and fields are recreated.
            if (theFilename.startsWith("event_")) {
                noteDataVector = new EventDataVector(theGroupData[theGroupData.length - 1]);
            } else if ((theFilename.startsWith("M") && theFilename.charAt(3) == '_')) {
                noteDataVector = new NoteDataVector(theGroupData[theGroupData.length - 1]);
            } else if ((theFilename.startsWith("Y") && theFilename.charAt(1) == '_')) {
                noteDataVector = new NoteDataVector(theGroupData[theGroupData.length - 1]);
            } else if ((theFilename.startsWith("D") && theFilename.charAt(5) == '_')) {
                noteDataVector = new DayNoteDataVector(theGroupData[theGroupData.length - 1]);
            } else if (theFilename.startsWith("todo_")) {
                noteDataVector = new TodoDataVector(theGroupData[theGroupData.length - 1]);
            } else if (theFilename.startsWith("search_")) {
                noteDataVector = new SearchResultVector(theGroupData[theGroupData.length - 1]);
            }

            // Put the interpreted data back into the Group Data object array, then save it.
            if(null != noteDataVector) {
                System.out.println("NoteDataVector size: " + noteDataVector.getVector().size());
                if(noteDataVector.getVector().size() > 0) {
                    theGroupData[theGroupData.length - 1] = noteDataVector.getVector();
                    int nw = FixUtil.saveNoteGroupData(theAbsolutePath, theGroupData);
                    System.out.println("   wrote " + nw + " notes to " + theFilename);
                }
            }

        }
    }//end fixTheFile

    public static void main(String[] args) {
        NoteData.loading = true; // For this one, we don't want to affect the LMDs.

        MemoryBank.debug = true; // Turn on all debugging printouts.
        //MemoryBank.setUserDataHome("jondo.nonamus@lcware.net");
        //MemoryBank.setUserDataHome("g01@doughmain.net");
        //MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.setUserDataHome("lex@doughmain.net");

        DataFix dataFix = new DataFix();

        dataFix.scanDataDir(new File(MemoryBank.userDataHome), 0);

    }
}
