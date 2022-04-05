import com.fasterxml.jackson.core.type.TypeReference;
import net.sf.image4j.codec.bmp.BMPDecoder;
import net.sf.image4j.codec.ico.ICODecoder;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class FileDataAccessor implements DataAccessor {
    static String archiveAreaPath;
    static DateTimeFormatter archiveFileFormat;
    static DateTimeFormatter archiveNameFormat;
    String basePath;
    String theAreaPath;
    String thePrefix;

    static String calendarNoteGroupAreaPath;
    static String eventGroupAreaPath;
    static String goalGroupAreaPath;
    static String searchResultGroupAreaPath;
    static String logGroupAreaPath;
    static String todoListGroupAreaPath;
    static String eventGroupFilePrefix;
    static String goalGroupFilePrefix;
    static String milestoneGroupFilePrefix;
    static String searchResultFilePrefix;
    static String todoListFilePrefix;
    static String logFilePrefix;
    static String locationsFilename;



    Vector<NoteData> foundDataVector;
    static String iconBase;
    static IconFileChooser iconFileChooser;


    static {
        archiveFileFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");
        archiveNameFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd  h:mm:ss a");

        eventGroupFilePrefix = "event_";
        goalGroupFilePrefix = "goal_";
        milestoneGroupFilePrefix = "miles_";
        searchResultFilePrefix = "search_";
        todoListFilePrefix = "todo_";
        logFilePrefix = "log_";

        locationsFilename = "Locations.json";

        iconBase = MemoryBank.mbHome + File.separatorChar + "icons" + File.separatorChar;
        iconFileChooser = new IconFileChooser(iconBase);
    }

    FileDataAccessor() {
        basePath = MemoryBank.userDataHome + File.separatorChar;
        archiveAreaPath = basePath + "Archives";

        calendarNoteGroupAreaPath = basePath + DataArea.CALENDARS.getAreaName() + File.separatorChar;
        eventGroupAreaPath = basePath + DataArea.UPCOMING_EVENTS.getAreaName() + File.separatorChar;
        goalGroupAreaPath = basePath + DataArea.GOALS.getAreaName() + File.separatorChar;
        searchResultGroupAreaPath = basePath + DataArea.SEARCH_RESULTS.getAreaName() + File.separatorChar;
        logGroupAreaPath = basePath + DataArea.LOGS.getAreaName() + File.separatorChar;
        todoListGroupAreaPath = basePath + DataArea.TODO_LISTS.getAreaName() + File.separatorChar;
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
                theAreaFullPath = MemoryBank.mbHome;
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

    //      @SuppressWarnings("rawtypes")
    @Override
    public boolean[][] findDataDays(int year) {
        boolean[][] hasDataArray = new boolean[12][31];
        // Will need to normalize the month integers from 0-11 to 1-12
        // and the day integers from 0-30 to 1-31

        // System.out.println("Searching for data in the year: " + year);
        String FileName = calendarNoteGroupAreaPath + year;
        MemoryBank.debug("Looking in " + FileName);

        String[] foundFiles = null;

        File f = new File(FileName);
        if (f.exists()) {
            if (f.isDirectory()) {
                foundFiles = f.list(new AppUtil.logFileFilter("D"));
            } // end if directory
        } // end if exists

        if (foundFiles == null)
            return hasDataArray;
        int month, day;

        // System.out.println("Found:");
        for (String foundFile : foundFiles) {
            month = Integer.parseInt(foundFile.substring(1, 3));
            day = Integer.parseInt(foundFile.substring(3, 5));
            // System.out.println(" " + foundFiles[i]);
            // System.out.println("\tMonth: " + month + "\tDay: " + day);
            Object[] theGroup = NoteGroupFile.loadFileData(FileName + File.separatorChar + foundFile);

            // Look into the file to see if it has significant content.
            boolean itHasData = false;
            if (theGroup.length == 1) {
                // It is not possible to have a length of one for DayNoteData, where the content is GroupProperties.
                // When only linkages are present in DayNoteData, the file still contains a two-element array
                // although the second element may be null.  So this is just older (pre linkages) data, and in
                // that case it is significant since we didn't ever save 'empty' days.
                itHasData = true;  // and DayNoteData did not get saved, if no notes.
            } else { // new structure; element zero is a GroupProperties.  But nothing from the Properties is
                // significant for purposes of this method; only the length of the second element (the ArrayList).
                ArrayList arrayList = AppUtil.mapper.convertValue(theGroup[1], ArrayList.class);
                if (arrayList.size() > 0) itHasData = true;
            }
            if (itHasData) hasDataArray[month - 1][day - 1] = true;
        } // end for each foundFile

        return hasDataArray;
    } // end findDataDays

    @Override
    public AppOptions getAppOptions() {
        AppOptions appOptions = null;
        String filename = MemoryBank.userDataHome + File.separatorChar + "appOpts.json";

        try {
            String text = FileUtils.readFileToString(new File(filename), StandardCharsets.UTF_8.name());
            appOptions = AppUtil.mapper.readValue(text, AppOptions.class);
            MemoryBank.debug("appOpts from JSON file: " + AppUtil.toJsonString(MemoryBank.appOpts));
        } catch (FileNotFoundException fnfe) {
            appOptions = new AppOptions(); // not a problem; use defaults.
            MemoryBank.debug("User tree options not found; using defaults");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return appOptions;
    }


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

    // Returns an array of 5 LogIcons that are read from a file
    //   of data for the specified day.  There may be one or more
    //   null placeholders in the array.
    @Override
    public Image[] getIconArray(int year, int month, int day) {
        LocalDate ld = LocalDate.of(year, month, day);

        String theFilename = NoteGroupFile.foundFilename(ld, "D");
        if (!new File(theFilename).exists()) return null;

        MemoryBank.debug("Loading: " + theFilename);
        // There is a data file, so there will be 'something' to load.
        Object[] theDayGroup = NoteGroupFile.loadFileData(theFilename);

        // If we have only loaded GroupProperties but no accompanying data, then bail out now.
        Object theObject = theDayGroup[theDayGroup.length - 1];
        String theClass = theObject.getClass().getName();
        System.out.println("The DayGroup class type is: " + theClass);
        if (!theClass.equals("java.util.ArrayList")) return null;

        // The loaded data is a Vector of DayNoteData.
        // Not currently worried about the 'loading' boolean, since MonthView does not re-persist the data.
        Vector<DayNoteData> theDayNotes = AppUtil.mapper.convertValue(theObject, new TypeReference<Vector<DayNoteData>>() { });

        Image[] returnArray = new Image[5];
        int index = 0;
        String iconFileString;
        for (DayNoteData tempDayData : theDayNotes) {
            if (tempDayData.getShowIconOnMonthBoolean()) {
                iconFileString = tempDayData.getIconFileString();
                if (iconFileString == null) { // Then show the default icon
                    iconFileString = DayNoteGroupPanel.defaultIcon.getDescription();
                } // end if

                if (iconFileString.equals("")) {
                    // Show this 'blank' on the month.
                    // Possibly as a 'spacer'.
                    returnArray[index] = null;
                } else {
                    Image theImage = new ImageIcon(iconFileString).getImage();
                    theImage.flush(); // SCR00035 - MonthView does not show all icons for a day.
                    // Review the problem by: start the app on DayNotes, adjust the date to be within a month where one
                    //   of the known bad icons (answer_bad.gif) should be shown (you don't need to go to an exact
                    //   day), then switch to the MonthView (to be contructed for the first time in your session).
                    // Adding a .flush() does fix the problem of some icons (answer_bad.gif) not showing the first time
                    //   the MonthView is displayed but other .gif files didn't need it.
                    // And - other file types may react differently.  This flush is needed in conjuction with a double
                    //   load of the initial month to be shown; that is done in treePanel.treeSelectionChanged().
                    returnArray[index] = theImage;
                } // end if

                index++;
                MemoryBank.debug("MonthView - Set icon " + index);
                if (index > 4) break;
            } // end if
        }

        //System.out.println("getIconArray: " + Arrays.toString(returnArray));
        return returnArray;
    } // end getIconArray

    // At this point this is just a selection operation and the return value is not yet an image, just (some of?)
    // its metadata built into a String.  When the actual image is retrieved by getImageIcon (based on info from this
    // String), THAT is where the description is set.
    public String chooseIcon() {
        String iconFileName = null;
        int returnVal = iconFileChooser.showDialog(null, "Set Icon");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            iconFileName = iconFileChooser.getSelectedFile().getAbsolutePath();
            MemoryBank.debug("Chosen icon file: " + iconFileName);
        }
        return iconFileName;
    }


    @Override
    public ImageIcon getImageIcon(IconInfo iconInfo) {
        ImageIcon theImageIcon = null;
        if (iconInfo.ready()) {
            String baseIconPath = ""; // when dataArea is null we look in the current directory.
            char c = File.separatorChar; // short, for better readability.
            if (iconInfo.dataArea == DataArea.IMAGES) baseIconPath = MemoryBank.mbHome + c + "images" + c;
            if (iconInfo.dataArea == DataArea.APP_ICONS) baseIconPath = MemoryBank.mbHome + c + "icons" + c;
            if (iconInfo.dataArea == DataArea.USER_ICONS) baseIconPath = MemoryBank.userDataHome + c + "icons" + c;

            // Convert file separator characters, if needed.  This makes for file system
            // compatibility (even though we only expect to run on one type of OS).
            String replaceWith = String.valueOf(c);
            if (replaceWith.equals("\\")) replaceWith = "\\\\"; // (we want backslashes, not escape chars).
            String remainingPath = iconInfo.iconName.replaceAll(":", replaceWith);

            String theFilename = baseIconPath + remainingPath + "." + iconInfo.iconFormat;
            //MemoryBank.debug("Full icon filename: " + theFilename);

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

                //===================================== IMPORTANT !!! ================================================
                // ImageIcon docs will say that the description is not used or needed, BUT - it IS used by this app.
                //   This is tricky; the description is picked up by IconNoteComponent.setIcon(ImageIcon theIcon).
                //   With the filename hiding in the place of the description, we can update the associated
                //   IconNoteData, and later restore the image from that.
                theImageIcon.setDescription(theFilename);
            }
        } // end if the IconInfo is 'ready'.
        return theImageIcon;
    } // end getImageIcon


    @Override
    public ImageIcon getImageIcon(IconNoteData iconNoteData) {
        ImageIcon theImageIcon = null;

        if (iconNoteData.iconFileString != null) {
            String theFilename = iconNoteData.iconFileString.toLowerCase();
            //MemoryBank.debug("Full icon filename: " + theFilename);
            if (new File(theFilename).exists()) {

                Image theImage = null;
                if (theFilename.endsWith(".ico")) {
                    try {
                        List<BufferedImage> images = ICODecoder.read(new File(theFilename));
                        theImage = images.get(0);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                } else if (theFilename.endsWith(".bmp")) {
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

                    //=============== IMPORTANT !!! =====================================================================
                    // ImageIcon docs will say that the description is not used or needed, BUT - it IS used by this app.
                    //   This is tricky; the description is picked up by IconNoteComponent.setIcon(ImageIcon theIcon).
                    //   With the filename hiding in the place of the description, we can update the associated
                    //   IconNoteData, and later restore the image from that.
                    theImageIcon.setDescription(theFilename);
                }
            } // end if the IconInfo is 'ready'.
        }
        return theImageIcon;
    } // end getImageIcon


    @Override
    // Usage of this is currently limited to MilestoneNoteComponents.  It relies on the icon files being numbered
    //   (hence the sorting) and also having a readme file (which is ignored and becomes a 'blank' that enters into
    //   the rotation after the initial null is 'scrolled' away, so that a blank remains as an option.  Not very
    //   reusable at this point; refactor will probably be needed if another usage is implemented.
    public ImageIcon[] getImageIcons(DataArea dataArea, String lowerPath) {
        ImageIcon[] theIcons;
        String dirPath;

        if(lowerPath != null && !lowerPath.trim().isEmpty()) {
            // lowerPath may have colons in it, in place of path separator chars if it goes deeper than one level.
            String replaceWith = String.valueOf(File.separatorChar);
            if (replaceWith.equals("\\")) replaceWith = "\\\\"; // (Windows-specific; we don't want escape chars).
            String lowerIconDirPath = lowerPath.replaceAll(":", replaceWith);
            dirPath = dataArea + "/" + lowerIconDirPath + "/";
        } else { // For the current single usage case, this section is never entered.
            dirPath = dataArea.toString() + "/";
        }

        File iconDir = new File(dirPath);
        String[] theFileList = iconDir.list();
        Arrays.sort(theFileList); // So that the icon order does not change arbitrarily.
        theIcons = new ImageIcon[theFileList.length];

        theIcons[0] = null; // A 'blank' icon
        int index = 1;
        for(String filename: theFileList) {
            if(filename.endsWith(".txt")) continue; // Allows for the icon directory to have a 'readme'.
            //System.out.println(filename);
            ImageIcon nextIcon = new ImageIcon(dirPath + filename);
            IconInfo.scaleIcon(nextIcon);
            theIcons[index++] = nextIcon;
        }
        return theIcons;
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

    // This method will return all groups of the same type (not applicable to CalendarNote types).
    // Note that the invoking group will also be in the list, but the calling context can easily remove it from
    // the result, if needed.
    @Override
    public ArrayList getGroupNames(GroupType groupType, boolean filterInactive) {
        // Which type of accessor is used to retrieve the names is a bit constrained by the fact that we currently
        //   only have one type of accessor.  Therefore, unlike the 'getDataAccessor' method that tries to make you
        //   believe that it is so versatile, here we just go directly to the FileDataAccessor.

        switch (groupType) {
            case SEARCH_RESULTS:
                theAreaPath = searchResultGroupAreaPath;
                thePrefix = searchResultFilePrefix;
                break;
            case TODO_LIST:
                theAreaPath = todoListGroupAreaPath;
                thePrefix = todoListFilePrefix;
                break;
            case LOG:
                theAreaPath = logGroupAreaPath;
                thePrefix = logFilePrefix;
                break;
            case EVENTS:
                theAreaPath = eventGroupAreaPath;
                thePrefix = eventGroupFilePrefix;
                break;
            case GOALS:
                theAreaPath = goalGroupAreaPath;
                thePrefix = goalGroupFilePrefix;
                break;
        }

        File dataDir = new File(theAreaPath);

        // Get the complete list of Group filenames.
        String[] theFileList = dataDir.list(
                new FilenameFilter() {
                    // Although this filter does not account for directories, we know that the dataDir for the
                    //  areas that we look in will not under normal program operation contain other directories.
                    public boolean accept(File f, String s) {
                        return s.startsWith(thePrefix);
                    }
                }
        );

        // Filter and normalize the selections.
        // ie, drop the prefixes and file extensions, and exclude the non-active groups.
        ArrayList<String> theGroupNames = new ArrayList<>();
        if (theFileList != null) {
            for (String aName : theFileList) {
                String theGroupName = NoteGroupFile.getGroupNameFromFilename(aName);
                if (filterInactive) {
                    if (!MemoryBank.appOpts.active(groupType, theGroupName)) continue;
                }
                theGroupNames.add(theGroupName);
            } // end for i
        }
        return theGroupNames;
    } // end getGroupNames

    @Override
    public NoteGroupDataAccessor getNoteGroupDataAccessor(GroupInfo groupInfo) {
        return new NoteGroupFile(groupInfo);
    }

    @Override
    public Locations loadLocations() {
        String fileName = MemoryBank.userDataHome + File.separatorChar + locationsFilename;
        Exception e = null;

        try {
            String text = FileUtils.readFileToString(new File(fileName), StandardCharsets.UTF_8.name());
            Locations fromFile = AppUtil.mapper.readValue(text, Locations.class);
            System.out.println("Locations from JSON file: " + AppUtil.toJsonString(fromFile));
            return fromFile;
        } catch (Exception ignore) {
        }  // not a big problem; use defaults.  When trying to cause Exceptions for Testing by making
        // 'bad' filenames, found that none get through the FileUtils; the only Exception I could get
        // was FileNotFound, and that one is effectively 'allowed' anyway, so stopped trying to handle
        // any of them and just take any unhappy path as needing the default handling.
        MemoryBank.debug("Locations not found; using defaults");
        return new Locations();
    }

    @Override
    public Vector<String> loadSubjects(String defaultSubject) {
        // This is the default list that they will get, if the load operation does not succeed.
        Vector<String> subjects = new Vector<>(6, 1);

        String subjectsFilename = makeSubjectFilename(defaultSubject);
        Exception e = null;
        try {
            String text = FileUtils.readFileToString(new File(subjectsFilename), StandardCharsets.UTF_8.name());
            Object theObject;
            theObject = AppUtil.mapper.readValue(text, Object.class);
            subjects = AppUtil.mapper.convertValue(theObject, new TypeReference<Vector<String>>() { });
            System.out.println("Subjects from JSON file: " + AppUtil.toJsonString(subjects));
        } catch (FileNotFoundException fnfe) {
            // not a problem; use defaults.
            MemoryBank.debug("Subjects file not found.  Returning the default list.");
        } catch (IOException ioe) {
            e = ioe;
            e.printStackTrace();
        }

        if (e != null) {
            String ems = "Error in loading " + subjectsFilename + " !\n";
            ems = ems + e.toString();
            ems = ems + "\noperation failed; using default values.";
            MemoryBank.debug(ems);
        } // end if

        return subjects;
    }

    // Develop the file name of the Subjects file from the default
    //   subject that is the input parameter, by adding the
    //   text 'Subjects.json' after the first space, if any.
    private String makeSubjectFilename(String defaultSubject) {
        String subjectsFilename;
        int space = defaultSubject.indexOf(" ");
        String s;
        if (space > -1) s = defaultSubject.substring(0, space);
        else s = defaultSubject;
        s += "Subjects.json";
        subjectsFilename = MemoryBank.userDataHome + File.separatorChar + s;
        return subjectsFilename;
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

    @Override
    public void saveAppOptions() {
        String filename = MemoryBank.userDataHome + File.separatorChar + "appOpts.json";
        MemoryBank.debug("Saving application option data in " + filename);

        try (FileWriter writer = new FileWriter(filename);
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(AppUtil.toJsonString(MemoryBank.appOpts));
            bw.flush();
        } catch (IOException ioe) {
            // Since saveOpts is to be called via a shutdown hook that is not going to
            // wait around for the user to 'OK' an error dialog, any error in saving
            // will only be reported in a printout via MemoryBank.debug because
            // otherwise the entire process will hang up waiting for the user's 'OK'
            // on the dialog that will NOT be showing.

            // A normal user will not see the debug error printout but
            // they will most likely see other popups such as filesystem full, access
            // denied, etc, that a sysadmin type can resolve for them, that will
            // also fix this issue.
            String ems = ioe.getMessage();
            ems = ems + "\nMemory Bank options save operation aborted.";
            MemoryBank.debug(ems);
            // This popup caused a hangup and the vm had to be 'kill'ed.
            // JOptionPane.showMessageDialog(null,
            //    ems, "Error", JOptionPane.ERROR_MESSAGE);
            // Yes, even though the parent was null.
        } // end try/catch
    } // end saveOpts

    @Override
    public boolean saveLocations(Locations theLocations) {
        String fileName = MemoryBank.userDataHome + File.separatorChar + locationsFilename;
        MemoryBank.debug("Saving Locations in " + fileName);

        try (FileWriter writer = new FileWriter(fileName);
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(AppUtil.toJsonString(theLocations));
            bw.flush();
        } catch (IOException ioe) {
            String ems = ioe.getMessage();
            ems = ems + "\nLocations save operation aborted.";
            MemoryBank.debug(ems);
            return false;
        } // end try/catch

        return true;
    }

    @Override
    public boolean saveSubjects(String defaultSubject, Vector<String> subjects) {
        boolean didIt = false;
        String subjectsFilename = makeSubjectFilename(defaultSubject);
        MemoryBank.debug("Saving subject data in " + subjectsFilename);

        try (FileWriter writer = new FileWriter(subjectsFilename);
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(AppUtil.toJsonString(subjects));
            bw.flush();
            didIt = true;
        } catch (IOException ioe) {
            String ems = ioe.getMessage();
            ems = ems + "\nSubjects save operation aborted.";
            MemoryBank.debug(ems);
        } // end try/catch

        return didIt;
    }// end saveSubjects

    @Override
    public Vector<NoteData> scanData(SearchPanel searchPanel) {
        // Make a Vector that can collect search results.
        foundDataVector = new Vector<>();

        // Scan the user's data area for data files - we do a recursive
        //   directory search and each file is examined as soon as it is
        //   found, provided that it passes the file-level filters.
        MemoryBank.debug("Data location is: " + MemoryBank.userDataHome);
        File f = new File(MemoryBank.userDataHome);
        scanDataDir(f, 0, searchPanel); // Indirectly fills the foundDataVector

        return foundDataVector;
    }

    // This method scans a directory for data files.  If it finds a directory rather than a file,
    //   it will recursively call itself for that directory.
    //
    // The SearchPanel interface follows a 'filter out' plan.  To support that, this method starts
    //   with the idea that ALL files will be searched and then considers the filters, to eliminate
    //   candidate files.  If a file is not eliminated after the filters have been considered, the
    //   search method is called for that file.
    private void scanDataDir(File theDir, int level, SearchPanel searchPanel) {
        MemoryBank.dbg("Scanning " + theDir.getName());

        File[] theFiles = theDir.listFiles();
        assert theFiles != null;
        int howmany = theFiles.length;
        MemoryBank.debug("\t\tFound " + howmany + " data files");
        boolean goLook;
        LocalDate dateNoteDate;
        MemoryBank.debug("Level " + level);

        for (File theFile : theFiles) {
            String theFile1Name = theFile.getName();
            if (theFile.isDirectory()) {
                if (theFile1Name.equals("Archives")) continue;
                if (theFile1Name.equals("icons")) continue;
                if (theFile1Name.equals("SearchResults")) continue;
                scanDataDir(theFile, level + 1, searchPanel);
            } else {
                goLook = true;
                String theGroupName = NoteGroupFile.getGroupNameFromFilename(theFile1Name);
                if (theFile1Name.startsWith("goal_")) {
                    if (!searchPanel.searchGoals()) {
                        goLook = false;
                    } else {
                        if (!MemoryBank.appOpts.active(GroupType.GOALS, theGroupName)) goLook = false;
                    }
                } else if (theFile1Name.startsWith("event_")) {
                    if (!searchPanel.searchEvents()) {
                        goLook = false;
                    } else {
                        if (!MemoryBank.appOpts.active(GroupType.EVENTS, theGroupName)) goLook = false;
                    }
                } else if (theFile1Name.startsWith("todo_")) {
                    if (!searchPanel.searchLists()) {
                        goLook = false;
                    } else {
                        if (!MemoryBank.appOpts.active(GroupType.TODO_LIST, theGroupName)) goLook = false;
                    }
                } else if ((theFile1Name.startsWith("D")) && (level > 0)) {
                    if (!searchPanel.searchDays()) goLook = false;
                } else if ((theFile1Name.startsWith("M")) && (level > 0)) {
                    if (!searchPanel.searchMonths()) goLook = false;
                } else if ((theFile1Name.startsWith("Y")) && (level > 0)) {
                    if (!searchPanel.searchYears()) goLook = false;
                } else { // Any other file type not covered above.
                    // This includes search results (for now - SCR0073)
                    goLook = false;
                } // end if / else if

                // Check the Note date, possibly filter out based on 'when'.

                if (goLook) {
                    if (searchPanel.getWhenSetting() != -1) {
                        // This section is only needed if the user has specified a date in the search.
                        dateNoteDate = NoteGroupFile.getDateFromFilename(theFile);
                        if (dateNoteDate != null) {
                            if (searchPanel.filterWhen(dateNoteDate)) goLook = false;
                        } // end if
                    }
                } // end if


                // The Last Modified date of the FILE is not necessarily the same as the Note, but
                //   it CAN be considered when looking for a last mod AFTER a certain date, because
                //   the last mod to ANY note in the file CANNOT be later than the last mod to the
                //   file itself.  Of course this depends on having no outside mods to the filesystem
                //   but we assume that because this is either a dev system (and we trust all devs :)
                //   or the app is being served from a server where only admins have access (and we
                //   trust all admins, of course).
                if (goLook) {
                    if (searchPanel.getLastModSetting() != -1) {
                        // This section is only needed if the user has specified a date in the search.
                        LocalDate dateLastMod = Instant.ofEpochMilli(theFile.lastModified()).atZone(ZoneId.systemDefault()).toLocalDate();
                        if (searchPanel.getLastModSetting() == SearchPanel.AFTER) {
                            if (searchPanel.filterLastMod(dateLastMod)) goLook = false;
                        } // end if
                    }
                } // end if

                if (goLook) {
                    searchDataFile(searchPanel, theFile);
                } // end if
            } // end if
        }//end for i
    }//end scanDataDir


    //---------------------------------------------------------
    // Method Name: searchDataFile
    //
    // File-level (but not item-level) date filtering will
    //   have been done prior to this method being called.
    // Item-level filtering is not done; date-filtering
    //   is done against Calendar notes, by using their
    //   filename, only.  Todo items will all just pass thru
    //   the filter so if not desired, don't search there in the first place.
    //---------------------------------------------------------
    private void searchDataFile(SearchPanel searchPanel, File dataFile) {
        //MemoryBank.debug("Searching: " + dataFile.getName());  // This one is a bit too verbose.
        Vector<AllNoteData> searchDataVector = null;

        // Load the file
        Object[] theGroupData = NoteGroupFile.loadFileData(dataFile);
        if (theGroupData != null && theGroupData[theGroupData.length - 1] != null) {
            // During a search these notes would not be re-preserved anyway, but the reason we care is that
            // the search parameters may have specified a date-specific search; we don't want all Last Mod
            // dates to get updated to this moment and thereby muck up the search results.
            BaseData.loading = true;
            searchDataVector = AppUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<AllNoteData>>() { });
            BaseData.loading = false;
        }
        if (searchDataVector == null) return;

        // Now get on with the search -
        for (AllNoteData vectorItem : searchDataVector) {

            // If we find what we're looking for in/about this note -
            if (searchPanel.foundIt(vectorItem)) {
                // Get the 'foundIn' info -
                // Although this is actually a GroupInfo, we do not need to populate the foundIn.groupId
                //   because search results are not intended to themselves be a part of the traceability chain,
                //   and they cannot be linked to or from.  Currently, the method below does not read in the
                //   data, so it cannot provide the groupId.
                GroupInfo foundIn = NoteGroupFile.getGroupInfoFromFilePath(dataFile);

                // Make new search result data for this find.
                SearchResultData srd = new SearchResultData(vectorItem);

                // The copy constructor used above will preserve the dateLastMod of the original note.
                srd.foundIn = foundIn; // No need to 'copy' foundIn; in this case it can be reused.

                // Add this search result data to our findings.
                foundDataVector.add(srd);

            } // end if
        } // end for
    }// end searchDataFile


}
