import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.util.Vector;

// The code for scanDataDir was brought over from the last DataFix.

public class DataFix {
    boolean usingOldInfo = true;

    private DataFix() {
    }

    // This method finds data files.  It does not load them, but it filters the ones that
    // it finds and if they pass the filter, sends them to 'fixTheFile', which does load.
    private void scanDataDir(File theDir, int level) {
        MemoryBank.dbg("Scanning " + theDir.getAbsolutePath());

        File[] theFiles = theDir.listFiles();
        assert theFiles != null;
        int howmany = theFiles.length;
        MemoryBank.debug("\t\tFound " + howmany + " items");

        for (File aFile : theFiles) {
            String aFileName = aFile.getName();
            if (aFile.isDirectory()) { // Some recursion may be called for..
                if (aFileName.equals("Archives")) continue; // but not here..
                if (aFileName.equals("icons")) continue; // or here..
                scanDataDir(aFile, level + 1); // yes, recursion is needed.
            } else {
                // We are only interested in Search Results and Day Notes.
                // This is a change to how files were being filtered; see earlier versions if needed.
                if (aFileName.startsWith("search_")) {
                    fixTheFile(aFile);
                } else if(aFileName.startsWith("D") && (aFileName.charAt(5) == '_')) {
                    if(usingOldInfo) {
                        fixTheFile(aFile);
                    }
                }
            } // end if
        }//end for i
    } //end scanDataDir

    // A modification of the searchDataFile method, from AppTreePanel
    private void fixTheFile(File dataFile) {
        String theFilename = dataFile.getName();
        String theAbsolutePath = dataFile.getAbsolutePath();
        System.out.println("Fixing: " + dataFile.getAbsolutePath());
        Vector<DayNoteData> noteDataVector = new Vector<>();

        // Load the data from the file.  Casting to the right Vector type will have
        // all the desired effects (see the readme).
        Object[] theGroupData = FixUtil.loadNoteGroupData(dataFile);
        if (theGroupData != null && theGroupData[theGroupData.length - 1] != null) {

            if ((theFilename.startsWith("D") && theFilename.charAt(5) == '_')) {
                noteDataVector = FixUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<DayNoteData>>() {
                });
                // Put the adjusted data Vector back into the object array that was loaded from file.
                theGroupData[0] = noteDataVector;
            } else if (theFilename.startsWith("search_")) {
                SearchResultGroupProperties srgp =  FixUtil.mapper.convertValue(theGroupData[0], SearchResultGroupProperties.class);

                if(usingOldInfo) {  // True on the first run, False after @JsonIgnore sps on the second run.
                    srgp.searchPanelSettings = srgp.sps;
                }
                theGroupData[0] = srgp;
            }
        }

        // And then save the object array back into the file it came from.
        int nw = FixUtil.saveNoteGroupData(theAbsolutePath, theGroupData);
        System.out.println("   wrote " + nw + " notes to " + theFilename);

    }//end searchDataFile

    public static void main(String[] args) {
        NoteData.loading = true; // For this one, we don't want to affect the LMDs.

        MemoryBank.debug = true; // Turn on all debugging printouts.
        //MemoryBank.setUserDataHome("g02@doughmain.net");
        //MemoryBank.setUserDataHome("jondo.nonamus@lcware.net");
        //MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.setUserDataHome("lex@doughmain.net");

        DataFix dataFix = new DataFix();

        dataFix.usingOldInfo = false;
        dataFix.scanDataDir(new File(MemoryBank.userDataHome), 0);

    }
}
