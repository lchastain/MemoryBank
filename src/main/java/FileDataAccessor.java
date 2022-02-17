import net.sf.image4j.codec.bmp.BMPDecoder;
import net.sf.image4j.codec.ico.ICODecoder;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileDataAccessor implements DataAccessor {
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

    static void archiveGroupType(File archiveRepo, GroupType groupType) throws IOException {
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
            case LOG:
                theSourceDir = new File(NoteGroupFile.logGroupAreaPath);
                theDestDir = new File(archiveRepoPath + File.separatorChar + DataArea.LOGS.getAreaName());
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

        if (theFiles != null) {
            for (File aFile : theFiles) {
                GroupInfo groupInfo = NoteGroupFile.getGroupInfoFromFilePath(aFile);
                // This GroupInfo will not be entirely accurate for sub-panels because the Type will be the type
                //   of the parent panel, but that's exactly what we need for the condition below, and the
                //   misrepresentation is not retained beyond this method.
                if (MemoryBank.appOpts.active(groupInfo.groupType, groupInfo.getGroupName())) {
                    FileUtils.copyFileToDirectory(aFile, theDestDir);
                }
            }
        }
    } // end archiveGroupType


    @Override
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
        if (!new File(archiveRepoPath + File.separatorChar + DataArea.LOGS.getAreaName()).mkdir()) return false;
        if (!new File(archiveRepoPath + File.separatorChar + DataArea.TODO_LISTS.getAreaName()).mkdir()) return false;
        if (!new File(archiveRepoPath + File.separatorChar + DataArea.SEARCH_RESULTS.getAreaName()).mkdir())
            return false;

        // Copy the appOpts and active NoteGroups into the archive -
        try {
            File theAppOpts = new File(basePath + "AppOpts.json");
            if (theAppOpts.exists()) { // It may not, if a bozo new user decides to Archive as their first action.
                FileUtils.copyFileToDirectory(theAppOpts, archiveRepo);
            }

            // Copy the active notegroups into the archive
            archiveGroupType(archiveRepo, GroupType.GOALS);
            archiveGroupType(archiveRepo, GroupType.EVENTS);
            archiveGroupType(archiveRepo, GroupType.LOG);
            archiveGroupType(archiveRepo, GroupType.TODO_LIST);
            archiveGroupType(archiveRepo, GroupType.SEARCH_RESULTS);

        } catch (Exception e) {
            System.out.println("Archiving error: " + e.toString());
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
            case LOGS:
                theAreaFullPath = NoteGroupFile.logGroupAreaPath;
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


    @Override
    public String getArchiveStorageName(String archiveName) {
        LocalDateTime localDateTime = LocalDateTime.parse(archiveName, archiveNameFormat);
        return archiveFileFormat.format(localDateTime);
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

    @Override
    public ImageIcon getImageIcon(IconInfo iconInfo) {
        ImageIcon theImageIcon = null;
        if(iconInfo.ready() ) {
            String basePath = ""; // when dataArea is null we look in the current directory.
            char c = File.separatorChar; // short, for better readability.
            if (iconInfo.dataArea == DataArea.IMAGES) basePath = MemoryBank.logHome + c + "images" + c;
            if (iconInfo.dataArea == DataArea.APP_ICONS) basePath = MemoryBank.logHome + c + "icons" + c;
            if (iconInfo.dataArea == DataArea.USER_ICONS) basePath = MemoryBank.userDataHome + c + "icons" + c;

            // Convert file separator characters, if needed.  This makes for file system
            // compatibility (even though we only expect to run on one type of OS).
            String replaceWith = String.valueOf(c);
            if(replaceWith.equals("\\")) replaceWith = "\\\\"; // (we want backslashes, not escape chars).
            String remainingPath = iconInfo.iconName.replaceAll(":", replaceWith);

            String theFilename = basePath + remainingPath + "." + iconInfo.iconFormat;
            MemoryBank.debug("  Full icon filename: " + theFilename);

            Image theImage = null;
            if (iconInfo.iconFormat.equalsIgnoreCase("ico")) {
                try {
                    List<BufferedImage> images = ICODecoder.read(new File(theFilename));
                    theImage = images.get(0);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            } else if (iconInfo.iconFormat.equalsIgnoreCase("bmp")) {
                try {
                    theImage = BMPDecoder.read(new File(theFilename));
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            } else { // This handles .png, .jpg, .gif
                theImage = Toolkit.getDefaultToolkit().getImage(theFilename);
            } // end if

            if (theImage != null) {
                theImageIcon = new ImageIcon();
                theImageIcon.setImage(theImage);

                // ImageIcon docs will say that the description is not used or needed, BUT - it IS used by this app
                //   when saving - this is tricky; the description is picked up by the iconNoteComponent when the rest
                //   of the icon appears to come thru as null.  With the filename hiding in the place of the
                //   description, we can restore it as needed.
                // See also:  iconNoteComponent.mouseClicked and setIcon.
                theImageIcon.setDescription(theFilename);
            }
        } // end if the IconInfo is 'ready'.
        return theImageIcon;
    } // end getImageIcon


    @Override  // The interface implementation
    public AppOptions getArchiveOptions(String archiveName) {
        AppOptions appOptions = null;
        LocalDateTime theArchiveTimestamp = LocalDateTime.parse(archiveName, archiveNameFormat);
        String archiveDirectoryName = archiveFileFormat.format(theArchiveTimestamp);

        Exception e = null;
        String filename = MemoryBank.userDataHome + File.separatorChar + DataArea.ARCHIVES.getAreaName();
        filename += File.separatorChar + archiveDirectoryName + File.separatorChar + "appOpts.json";

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
