import org.apache.commons.io.FileUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class FileDataAccessor implements AppDataAccessor {
    static String archiveAreaPath;
    static DateTimeFormatter archiveFileFormat;
    static DateTimeFormatter archiveNameFormat;
    static String basePath;

    static {
        basePath = MemoryBank.userDataHome + File.separatorChar;
        archiveAreaPath = basePath + "Archives";
        archiveFileFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");
        archiveNameFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd  h:mm:ss a");
    }

    @Override
    public NoteGroupDataAccessor getNoteGroupDataAccessor(GroupInfo groupInfo) {
        return new NoteGroupFile(groupInfo);
    }

    public boolean createArchive() {
        // Make a unique name for the archive
        String archiveFileName = archiveFileFormat.format(LocalDateTime.now());
        MemoryBank.debug("Creating archive at DateTime: " + archiveFileName);

        File f = new File(archiveAreaPath + File.separatorChar + archiveFileName);
        if(!f.mkdirs()) return false;

        // Copy the required items into the archive
        try {
            FileUtils.copyFileToDirectory(new File(basePath + "AppOpts.json"), f);
            FileUtils.copyDirectoryToDirectory(new File(NoteGroupFile.goalGroupAreaPath), f);
            FileUtils.copyDirectoryToDirectory(new File(NoteGroupFile.eventGroupAreaPath), f);
            FileUtils.copyDirectoryToDirectory(new File(NoteGroupFile.todoListGroupAreaPath), f);
            FileUtils.copyDirectoryToDirectory(new File(NoteGroupFile.searchResultGroupAreaPath), f);
        } catch(Exception e) {
            System.out.println("Archiving error: " + e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public String[] getArchiveNames() {
        String[] theFileNames;
        ArrayList<String> theArchiveNames = new ArrayList<>();
        File dataDir = new File(archiveAreaPath);

        theFileNames = dataDir.list( );
        if(theFileNames == null) return null;
        for(String aFileName : theFileNames) {
            LocalDateTime localDateTime = LocalDateTime.parse(aFileName, archiveFileFormat);
            String anArchiveName = archiveNameFormat.format(localDateTime);
            theArchiveNames.add(anArchiveName);
        }
        return theArchiveNames.toArray(new String[0]);
    }

    //  Returns true if the area already existed OR the creation was successful.
    @Override
    public boolean createArea(DataArea dataArea) {
        String theAreaFullPath;
        switch(dataArea) {
            case ARCHIVES:
                theAreaFullPath = archiveAreaPath;
                break;
            case GOALS:
                theAreaFullPath = NoteGroupFile.goalGroupAreaPath;
                break;
            case UPCOMING_EVENTS:
                theAreaFullPath = NoteGroupFile.eventGroupAreaPath;
                break;
            case TODO_LISTS:
                theAreaFullPath = NoteGroupFile.todoListGroupAreaPath;
                break;
            case SEARCH_RESULTS:
                theAreaFullPath = NoteGroupFile.searchResultGroupAreaPath;
                break;
            default:
                theAreaFullPath = MemoryBank.logHome;
                break;
        }
        File f = new File(theAreaFullPath);
        if(f.exists()) return true;
        return f.mkdirs();
    }

}
