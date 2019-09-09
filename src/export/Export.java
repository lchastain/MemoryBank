import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Vector;

public class Export {
    private Vector<String> exportDataVector;

    private void doExport() {
        dlgWorkingDialog.setLocationRelativeTo(rightPane); // This can be needed if windowed app has moved from center screen.
        showWorkingDialog(true); // Show the 'Working...' dialog

        // Make sure that the most recent changes, if any,
        //   will be included in the export.
        if (theNoteGroup != null) {
            theNoteGroup.preClose();
        } // end if

        // Now make a Vector that can collect the search results.
        exportDataVector = new Vector<>(0, 1);

        // Now scan the user's data area for data files -
        // We do a recursive directory search and each
        //   file is examined as soon as it is found,
        //   provided that it passes the file-level filters.
        MemoryBank.debug("Data location is: " + MemoryBank.userDataHome);
        File f = new File(MemoryBank.userDataHome);
        exportDataDir(f, 0); // Indirectly fills the exportDataVector
        writeExportFile();

        showWorkingDialog(false);
    } // end doExport


    //--------------------------------------------------------
    // Method Name:  exportDataDir
    //
    // This method scans a directory for data files.  If it
    //   finds a directory rather than a file, it will
    //   recursively call itself for that directory.
    //--------------------------------------------------------
    private void exportDataDir(File theDir, int level) {
        MemoryBank.dbg("Scanning " + theDir.getName());

        File[] theFiles = theDir.listFiles();
        assert theFiles != null;
        int howmany = theFiles.length;
        MemoryBank.debug("\t\tFound " + howmany + " data files");
        MemoryBank.debug("Level " + level);

        boolean goLook;
        for (File theFile1 : theFiles) {
            goLook = false;
            String theFile = theFile1.getName();
            if (theFile1.isDirectory()) {
                if (theFile.equals("Archives")) continue;
                if (theFile.equals("icons")) continue;
                exportDataDir(theFile1, level + 1);
            } else {
                if (theFile.equals("UpcomingEvents")) goLook = true;
                if (theFile.startsWith("todo_")) goLook = true;
                if ((theFile.startsWith("D")) && (level > 0)) goLook = true;
                if ((theFile.startsWith("M")) && (level > 0)) goLook = true;
                if ((theFile.startsWith("Y")) && (level > 0)) goLook = true;
            } // end if / else

            if (goLook) exportDataFile(theFile1);
        }//end for i
    }//end exportDataDir


    //---------------------------------------------------------
    // Method Name: exportDataFile
    //
    //---------------------------------------------------------
    private void exportDataFile(File dataFile) {
        MemoryBank.debug("Searching: " + dataFile.getName());
        noteDataVector = new Vector<>();
        loadNoteData(dataFile);

        // Construct an Excel-readable string for every Note
        for (NoteData ndTemp : noteDataVector) {
            String multiline = convertLinefeeds(ndTemp.extendedNoteString);

            String s = ndTemp.noteString + "|";
            s += multiline + "|";
            s += ndTemp.subjectString;

            // Get the Date for this note, if available
            Date dateTmp = ndTemp.getNoteDate();
            if (dateTmp == null) dateTmp = AppUtil.getDateFromFilename(dataFile);

            if (null != dateTmp) s += "|" + dateTmp.toString();
            exportDataVector.add(s);

        } // end for
    }//end exportDataFile


    // This mechanism will be used so that a multiline string
    // may be exported on a single line for Excel import.  Then
    // it will be subject to a (user-initiated) global search
    // and replace (replacing with alt-0010).
    private String convertLinefeeds(String extendedNoteString) {
        String retVal = "";
        if (null == extendedNoteString) return retVal;
        if (extendedNoteString.trim().equals("")) return retVal;

        retVal = extendedNoteString.replaceAll("\n", "[linefeed]");

        return retVal;
    }

    private int writeExportFile() {
        try {
            // Make a unique filename for the results
            String strResultsFileName = "Export" + AppUtil.getTimestamp();
            strResultsFileName += ".txt";

            String strResultsPath = MemoryBank.userDataHome + File.separatorChar;
            System.out.println(strResultsFileName + " results: " + exportDataVector.size());

            // Make the File, then save the results into it.
            FileWriter fstream = new FileWriter(strResultsPath + strResultsFileName);
            //saveNoteData(rf); // Saves ob1kenoby and then the noteDataVector

            PrintWriter out = new PrintWriter(fstream);
            for (String tempData : exportDataVector) {
                out.println(tempData);
            }//end for i
            out.flush();
            out.close();
            fstream.close();
        } catch (IOException ioe) {
            System.err.println("Error: " + ioe.getMessage());
            return 1; // Problem
        }//end try/catch

        return 0;  // Success
    }//end writeExportFile


}
