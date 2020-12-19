import org.apache.commons.io.FileUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileDataAccessor implements AppDataAccessor {
    static String archiveAreaPath;
    static DateTimeFormatter archiveFormat;
    static String basePath;

    static {
        basePath = MemoryBank.userDataHome + File.separatorChar;
        archiveAreaPath = basePath + "Archives";
        archiveFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh-mm-ss");
    }

    @Override
    public NoteGroupDataAccessor getNoteGroupDataAccessor(GroupInfo groupInfo) {
        return new NoteGroupFile(groupInfo);
    }

    public boolean createArchive() {
        // Make a unique name for the archive
        String archiveName = archiveFormat.format(LocalDateTime.now());
        MemoryBank.debug("Creating archive at DateTime: " + archiveName);

        File f = new File(archiveAreaPath + File.separatorChar + archiveName);
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
