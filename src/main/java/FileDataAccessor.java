import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private void archiveGroupType(File archiveRepo, GroupType groupType) throws IOException {
        String archiveRepoPath = archiveRepo.getAbsolutePath();
        File theSourceDir = null;
        File theDestDir = null;

        switch (groupType) {
            case GOALS:
                theSourceDir = new File(NoteGroupFile.goalGroupAreaPath);
                theDestDir = new File(archiveRepoPath + File.separatorChar + DataArea.GOALS.getAreaName());
                break;
            case EVENTS:
                theSourceDir = new File(NoteGroupFile.eventGroupAreaPath);
                theDestDir = new File(archiveRepoPath + File.separatorChar + DataArea.UPCOMING_EVENTS.getAreaName());
                break;
            case TODO_LIST:
                theSourceDir = new File(NoteGroupFile.todoListGroupAreaPath);
                theDestDir = new File(archiveRepoPath + File.separatorChar + DataArea.TODO_LISTS.getAreaName());
                break;
            case SEARCH_RESULTS:
                theSourceDir = new File(NoteGroupFile.searchResultGroupAreaPath);
                theDestDir = new File(archiveRepoPath + File.separatorChar + DataArea.SEARCH_RESULTS.getAreaName());
                break;
        }
        if (theSourceDir == null) return;

        File[] theFiles = theSourceDir.listFiles();

        for (File aFile : theFiles) {
            GroupInfo groupInfo = NoteGroupFile.getGroupInfoFromFile(aFile);
            if (MemoryBank.appOpts.active(groupInfo.groupType, groupInfo.getGroupName())) {
                FileUtils.copyFileToDirectory(aFile, theDestDir);
            }
        }
    }


    public boolean createArchive() {
        // Make a unique name for the archive
        String archiveFileName = archiveFileFormat.format(LocalDateTime.now());
        MemoryBank.debug("Creating archive at DateTime: " + archiveFileName);

        // Create the directory with the archive name, and its content directories as well.
        String archiveRepoPath = archiveAreaPath + File.separatorChar + archiveFileName;
        File archiveRepo = new File(archiveRepoPath);
        if (!archiveRepo.mkdirs()) return false;
        if (!new File(archiveRepoPath + File.separatorChar + DataArea.GOALS.getAreaName()).mkdir()) return false;
        if (!new File(archiveRepoPath + File.separatorChar + DataArea.UPCOMING_EVENTS.getAreaName()).mkdir())
            return false;
        if (!new File(archiveRepoPath + File.separatorChar + DataArea.TODO_LISTS.getAreaName()).mkdir()) return false;
        if (!new File(archiveRepoPath + File.separatorChar + DataArea.SEARCH_RESULTS.getAreaName()).mkdir())
            return false;

        // Copy the appOpts and active NoteGroups into the archive -
        try {
            FileUtils.copyFileToDirectory(new File(basePath + "AppOpts.json"), archiveRepo);

            // Copy the active notegroups into the archive
            archiveGroupType(archiveRepo, GroupType.GOALS);
            archiveGroupType(archiveRepo, GroupType.EVENTS);
            archiveGroupType(archiveRepo, GroupType.TODO_LIST);
            archiveGroupType(archiveRepo, GroupType.SEARCH_RESULTS);

            // Leftover, from initial solution when ALL notegroups were being taken.  Just keep this for a little while...
            //      FileUtils.copyDirectoryToDirectory(new File(NoteGroupFile.goalGroupAreaPath), archiveRepo);
            //      FileUtils.copyDirectoryToDirectory(new File(NoteGroupFile.eventGroupAreaPath), archiveRepo);
            //      FileUtils.copyDirectoryToDirectory(new File(NoteGroupFile.todoListGroupAreaPath), archiveRepo);
            //      FileUtils.copyDirectoryToDirectory(new File(NoteGroupFile.searchResultGroupAreaPath), archiveRepo);
        } catch (Exception e) {
            System.out.println("Archiving error: " + e.getMessage());
            return false;
        }
        return true;
    }

    //  Returns true if the area already existed OR the creation was successful.
    @Override  // The interface implementation
    public boolean createArea(DataArea dataArea) {
        String theAreaFullPath;
        switch (dataArea) {
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
        if (f.exists()) return true;
        return f.mkdirs();
    }


    @Override  // The interface implementation
    public String[] getArchiveNames() {
        String[] theFileNames;
        ArrayList<String> theArchiveNames = new ArrayList<>();
        File dataDir = new File(archiveAreaPath);

        theFileNames = dataDir.list();
        if (theFileNames == null) return null;
        for (String aFileName : theFileNames) {
            LocalDateTime localDateTime = LocalDateTime.parse(aFileName, archiveFileFormat);
            String anArchiveName = archiveNameFormat.format(localDateTime);
            theArchiveNames.add(anArchiveName);
        }
        return theArchiveNames.toArray(new String[0]);
    }


    @Override  // The interface implementation
    public AppOptions getArchiveOptions(String archiveName) {
        AppOptions appOptions = null;
        LocalDateTime theArchiveTimestamp = LocalDateTime.parse(archiveName, archiveNameFormat);
        String archiveFileName = archiveFileFormat.format(theArchiveTimestamp);

        Exception e = null;
//        String filename = MemoryBank.userDataHome + File.separatorChar + "appOpts.json";
        String filename = MemoryBank.userDataHome + File.separatorChar + DataArea.ARCHIVES.getAreaName();
        filename += File.separatorChar + archiveFileName + File.separatorChar + "appOpts.json";

        try {
            String text = FileUtils.readFileToString(new File(filename), StandardCharsets.UTF_8.name());
            appOptions = AppUtil.mapper.readValue(text, AppOptions.class);
            MemoryBank.debug("appOpts from JSON file: " + AppUtil.toJsonString(MemoryBank.appOpts));
        } catch (Exception anyException) {
            e = anyException;
        }

        if (e != null) {
            e.printStackTrace();
            String ems = "Error in loading " + filename + " !\n";
            ems = ems + e.toString();
            ems = ems + "\nReview the stack trace.";
            MemoryBank.debug(ems);
        } // end if

        // Archives do not themselves have Archives; therefore the row that was preserved in these options will
        // be off by one because the Archive Tree does not have that leaf (row).  Fix that, before returning.
        if (appOptions != null) {
            appOptions.theSelectionRow -= 1;
        }

        return appOptions;
    }


    // The archive name cannot be used directly as a directory name due to the presence
    //   of the colons in the time portion.  So, we need to parse the archive name with
    //   the format that was used to make the directory name from the original date, in
    //   order to get back to that original date.
    @Override  // The interface implementation
    public LocalDateTime getDateTimeForArchiveName(String archiveName) {
        if (archiveName == null) return null;
        return LocalDateTime.parse(archiveName, archiveNameFormat);
    }


    @Override
    public NoteGroupDataAccessor getNoteGroupDataAccessor(GroupInfo groupInfo) {
        return new NoteGroupFile(groupInfo);
    }

    @Override
    public boolean removeArchive(LocalDateTime localDateTime) {
        String archiveFileName = archiveFileFormat.format(localDateTime);
        MemoryBank.debug("Removing archive: " + archiveFileName);

        String archiveRepoPath = archiveAreaPath + File.separatorChar + archiveFileName;
        File archiveRepo = new File(archiveRepoPath);
        try {
            FileUtils.deleteDirectory(archiveRepo); // This one can remove non-empty directories.
            return true;
        } catch (Exception e) {
            System.out.println("  Exception during archive removal: " + e.getMessage());
        }
        return false;
    }


}
