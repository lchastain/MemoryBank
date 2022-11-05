import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Vector;

@SuppressWarnings("rawtypes")
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
    static String plainNoteGroupAreaPath;
    static String todoListGroupAreaPath;
    static String eventGroupFilePrefix;
    static String goalGroupFilePrefix;
    static String milestoneGroupFilePrefix;
    static String searchResultFilePrefix;
    static String todoListFilePrefix;
    static String logFilePrefix;
    static String noteFilePrefix;
    static String locationsFilename;
    static String userDataHome; // User data top-level directory 'mbankData'

    Vector<NoteData> foundDataVector;
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
        noteFilePrefix = "notes_";

        locationsFilename = "Locations.json";

        setUserDataHome(MemoryBank.userEmail);
    } // end static section

    FileDataAccessor() {
        basePath = userDataHome + File.separatorChar;
        archiveAreaPath = basePath + "Archives";

        calendarNoteGroupAreaPath = basePath + DataArea.CALENDARS.getAreaName() + File.separatorChar;
        eventGroupAreaPath = basePath + DataArea.UPCOMING_EVENTS.getAreaName() + File.separatorChar;
        goalGroupAreaPath = basePath + DataArea.GOALS.getAreaName() + File.separatorChar;
        searchResultGroupAreaPath = basePath + DataArea.SEARCH_RESULTS.getAreaName() + File.separatorChar;
        logGroupAreaPath = basePath + DataArea.LOGS.getAreaName() + File.separatorChar;
        plainNoteGroupAreaPath = basePath + DataArea.NOTES.getAreaName() + File.separatorChar;
        todoListGroupAreaPath = basePath + DataArea.TODO_LISTS.getAreaName() + File.separatorChar;
    }

    public static void setUserDataHome(String userEmail) {
        // User data - personal notes, different for each user.
        String loc;
        if (MemoryBank.appEnvironment == null || MemoryBank.appEnvironment.equals("ide")) {
            loc = MemoryBank.currentDir + "/mbDevData/" + userEmail;
        } else {
            String userHome = System.getProperty("user.home"); // Home directory.
            loc = userHome + "/mbankData/" + userEmail;
        }
        System.out.println("Data location for " + userEmail + " is: " + loc);
//        String traceString = Thread.currentThread().getStackTrace()[2].toString();
//        MemoryBank.debug("Call to setUserDataHome, from: " + traceString);

        // Next - look for the data for this specific user.
        boolean goodUserDataLoc = true;
        File f = new File(loc);
        if (f.exists()) {
            if (!f.isDirectory()) { // Bad location, if it already exists and is not a directory.
                goodUserDataLoc = false;
            } // end if directory
        } else { // Create a new directory for this user
            if (!f.mkdirs()) { // Bad location, if we were unable to make the needed directory.
                goodUserDataLoc = false;
            } // end if
        } // end if exists

        if (goodUserDataLoc) {
            userDataHome = loc;
        } else {  // Build up and display an informative error message.
            JPanel le = new JPanel(new GridLayout(5, 1, 0, 0));

            String oneline;
            oneline = "The MemoryBank program was not able to access or create:";
            JLabel el1 = new JLabel(oneline);
            JLabel el2 = new JLabel(loc, JLabel.CENTER);
            oneline = "This could be due to insufficient permissions ";
            oneline += "or a problem with the file system.";
            JLabel el3 = new JLabel(oneline);
            oneline = "Please try again with a different location, or see";
            oneline += " your system administrator";
            JLabel el4 = new JLabel(oneline);
            oneline = "if you believe the location is a valid one.";
            JLabel el5 = new JLabel(oneline, JLabel.CENTER);

            el1.setFont(Font.decode("Dialog-bold-14"));
            el2.setFont(Font.decode("Dialog-bold-14"));
            el3.setFont(Font.decode("Dialog-bold-14"));
            el4.setFont(Font.decode("Dialog-bold-14"));
            el5.setFont(Font.decode("Dialog-bold-14"));

            le.add(el1);
            le.add(el2);
            le.add(el3);
            le.add(el4);
            le.add(el5);

            JOptionPane.showMessageDialog(null, le,
                    "Problem with specified location", JOptionPane.ERROR_MESSAGE);
        } // end if

        if (!goodUserDataLoc) {  // Some validity testing here..
            System.exit(0);
        } // end if

    } // end setUserDataHome

    static void archiveGroupType(File archiveRepo, GroupType groupType) throws IOException {
        String archiveRepoPath = archiveRepo.getAbsolutePath();
        File theSourceDir = null;
        File theDestDir = null;

        switch (groupType) {
            case GOALS -> {
                theSourceDir = new File(NoteGroupFile.goalGroupAreaPath);
                theDestDir = new File(archiveRepoPath + File.separatorChar + DataArea.GOALS.getAreaName());
            }
            case EVENTS -> {
                theSourceDir = new File(NoteGroupFile.eventGroupAreaPath);
                theDestDir = new File(archiveRepoPath + File.separatorChar + DataArea.UPCOMING_EVENTS.getAreaName());
            }
            case NOTES -> {
                theSourceDir = new File(NoteGroupFile.plainNoteGroupAreaPath);
                theDestDir = new File(archiveRepoPath + File.separatorChar + DataArea.NOTES.getAreaName());
            }
            case TODO_LIST -> {
                theSourceDir = new File(NoteGroupFile.todoListGroupAreaPath);
                theDestDir = new File(archiveRepoPath + File.separatorChar + DataArea.TODO_LISTS.getAreaName());
            }
            case SEARCH_RESULTS -> {
                theSourceDir = new File(NoteGroupFile.searchResultGroupAreaPath);
                theDestDir = new File(archiveRepoPath + File.separatorChar + DataArea.SEARCH_RESULTS.getAreaName());
            }
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
        if (!new File(archiveRepoPath + File.separatorChar + DataArea.NOTES.getAreaName()).mkdir()) return false;
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
            archiveGroupType(archiveRepo, GroupType.NOTES);
            archiveGroupType(archiveRepo, GroupType.TODO_LIST);
            archiveGroupType(archiveRepo, GroupType.SEARCH_RESULTS);

        } catch (Exception e) {
            System.out.println("Archiving error: " + e);
            return false;
        }
        return true;
    }

    //  Returns true if the area already existed OR the creation was successful.
    @Override  // The interface implementation
    public boolean createArea(DataArea dataArea) {
        String theAreaFullPath = switch (dataArea) {
            case ARCHIVES -> archiveAreaPath;
            case GOALS -> NoteGroupFile.goalGroupAreaPath;
            case UPCOMING_EVENTS -> NoteGroupFile.eventGroupAreaPath;
            case LOGS -> NoteGroupFile.logGroupAreaPath;
            case NOTES -> NoteGroupFile.plainNoteGroupAreaPath;
            case TODO_LISTS -> NoteGroupFile.todoListGroupAreaPath;
            case SEARCH_RESULTS -> NoteGroupFile.searchResultGroupAreaPath;
            default -> throw new IllegalStateException("Unexpected value: " + dataArea);
        };
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
        //MemoryBank.debug("Looking in " + FileName);

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
        String filename = userDataHome + File.separatorChar + "appOpts.json";

        try {
            String text = FileUtils.readFileToString(new File(filename), StandardCharsets.UTF_8.name());
            appOptions = AppUtil.mapper.readValue(text, AppOptions.class);
            //MemoryBank.debug("appOpts from JSON file: " + AppUtil.toJsonString(appOptions));
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
        String filename = userDataHome + File.separatorChar + DataArea.ARCHIVES.getAreaName();
        filename += File.separatorChar + archiveDirectoryName + File.separatorChar + "appOpts.json";

        try {
            String text = FileUtils.readFileToString(new File(filename), StandardCharsets.UTF_8.name());
            appOptions = AppUtil.mapper.readValue(text, AppOptions.class);
            //MemoryBank.debug("Archived appOpts from JSON file: " + AppUtil.toJsonString(appOptions));
        } catch (Exception anyException) {
            e = anyException;
        }

        if (e != null) {
            e.printStackTrace();
            String ems = "Error in loading " + filename + " !\n";
            ems = ems + e;
            ems = ems + "\nReview the stack trace.";
            MemoryBank.debug(ems);
        } // end if

        // Archives do not themselves have Archives; therefore the row that was preserved in these options will
        //   be off by one because the Archive Tree does not have that leaf (row).  We fix that, before returning.
        // Update 17 Apr 2022 - now this int is not being used at all by an archive, due to additional potential
        //   row number changes due to the new ability to collapse the Calendar Notes.  But we leave this adjustment
        //   in place anyway, for now.
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

    // Returns an array of 5 Icons that are read from a file
    //   of data for the specified day.  There may be one or more
    //   null placeholders in the array.
    @Override
    public Image[] getIconArray(int year, int month, int day) {
        LocalDate ld = LocalDate.of(year, month, day);

        String theFilename = NoteGroupFile.foundFilename(ld, "D");
        //System.out.println("Found this data file: " + theFilename);
        if (!new File(theFilename).exists()) return null;

        MemoryBank.debug("Loading: " + theFilename);
        // There is a data file, so there will be 'something' to load.
        Object[] theDayGroup = NoteGroupFile.loadFileData(theFilename);

        // If we have only loaded GroupProperties but no accompanying data, then bail out now.
        // The last entry in theDayGroup array will be an ArrayList, IF there is data for that day.
        Object theObject = theDayGroup[theDayGroup.length - 1];
        String theClass = theObject.getClass().getName();
        //System.out.println("The DayGroup class type is: " + theClass);
        if (!theClass.equals("java.util.ArrayList")) return null;

        // The loaded data is a Vector of DayNoteData.
        // Not currently worried about the 'loading' boolean, since MonthView does not re-persist the data.
        Vector<DayNoteData> theDayNotes = AppUtil.mapper.convertValue(theObject, new TypeReference<>() {
        });

        Image[] returnArray = new Image[5];
        int index = 0;
        String iconFileString;
        for (DayNoteData tempDayData : theDayNotes) {
            if (tempDayData.getShowIconOnMonthBoolean()) {
                iconFileString = tempDayData.getIconFileString();
                if (iconFileString == null) { // Then show the default icon
                    iconFileString = DayNoteGroupPanel.defaultIcon.getDescription();
                } // end if

                if (iconFileString.equals("")) { // NOT the same handling as null.
                    // Show this 'blank' on the month, possibly as a 'spacer'.
                    returnArray[index] = null;
                } else {
                    java.net.URL imgURL = FileDataAccessor.class.getResource(iconFileString);
                    if (imgURL != null) {
                        Image theImage = Toolkit.getDefaultToolkit().getImage(imgURL);
                        IconInfo.scaleIcon(new ImageIcon(theImage));
                        theImage.flush(); // SCR00035 - MonthView does not show all icons for a day.
                        // Review the problem by: start the app on DayNotes, adjust the date to be within a month where one
                        //   of the known bad icons (answer_bad.gif) should be shown (you don't need to go to an exact
                        //   day), then switch to the MonthView (to be contructed for the first time in your session).
                        // Adding a .flush() does fix the problem of some icons (answer_bad.gif) not showing the first time
                        //   the MonthView is displayed but other .gif files didn't need it.
                        // And - other file types may react differently.  This flush is needed in conjuction with a double
                        //   load of the initial month to be shown; that is done in treePanel.treeSelectionChanged().
                        returnArray[index] = theImage;
                    } else {
                        returnArray[index] = null;
                    }
                } // end if

                index++;
                MemoryBank.debug("MonthView - Set icon " + index);
                if (index > 4) break;
            } // end if
        }

        //System.out.println("getIconArray: " + Arrays.toString(returnArray));
        return returnArray;
    } // end getIconArray

    // At this point this is just a selection operation and the return value is not yet an image, just
    // the filesystem path to it, as a String.
    // The actual image will be retrieved by getImageIcon, based on the identifier in this String.
    public String chooseIcon() {
        String iconFileName = null;
        int returnVal = iconFileChooser.showDialog(null, "Set Icon");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            iconFileName = iconFileChooser.getSelectedFile().getAbsolutePath();
            MemoryBank.debug("Chosen icon file: " + iconFileName);
        }
        // Example:
        //Chosen icon file: C:\Users\Lee\workspace\Memory Bank\target\classes\icons\anchor.gif
        //IconNoteData.setIconFileString to: "c:|users|lee|workspace|memory bank|target|classes|icons|anchor.gif"

        return iconFileName;
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
            case SEARCH_RESULTS -> {
                theAreaPath = searchResultGroupAreaPath;
                thePrefix = searchResultFilePrefix;
            }
            case TODO_LIST -> {
                theAreaPath = todoListGroupAreaPath;
                thePrefix = todoListFilePrefix;
            }
            case LOG -> {
                theAreaPath = logGroupAreaPath;
                thePrefix = logFilePrefix;
            }
            case NOTES -> {
                theAreaPath = plainNoteGroupAreaPath;
                thePrefix = noteFilePrefix;
            }
            case EVENTS -> {
                theAreaPath = eventGroupAreaPath;
                thePrefix = eventGroupFilePrefix;
            }
            case GOALS -> {
                theAreaPath = goalGroupAreaPath;
                thePrefix = goalGroupFilePrefix;
            }
        }

        File dataDir = new File(theAreaPath);

        // Get the complete list of Group filenames.
        // Although this filter does not account for directories, we know that the dataDir for the
        //  areas that we look in will not under normal program operation contain other directories.
        String[] theFileList = dataDir.list(
                (f, s) -> s.startsWith(thePrefix)
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
        String fileName = userDataHome + File.separatorChar + locationsFilename;
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
        // This is the the empty vector that they will get if the load operation does not succeed.
        Vector<String> subjects = new Vector<>(6, 1);

        String subjectsFilename = makeSubjectFilename(defaultSubject);
        Exception e = null;
        try {
            String text = FileUtils.readFileToString(new File(subjectsFilename), StandardCharsets.UTF_8.name());
            Object theObject;
            theObject = AppUtil.mapper.readValue(text, Object.class);
            subjects = AppUtil.mapper.convertValue(theObject, new TypeReference<>() {
            });
            //System.out.println("Subjects from JSON file: " + AppUtil.toJsonString(subjects));
        } catch (FileNotFoundException fnfe) {
            // not a problem; use defaults.
            MemoryBank.debug("Subjects file not found.  Returning the default list.");
        } catch (IOException ioe) {
            e = ioe;
            e.printStackTrace();
        }

        if (e != null) {
            String ems = "Error in loading " + subjectsFilename + " !\n";
            ems = ems + e;
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
        subjectsFilename = userDataHome + File.separatorChar + s;
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
        String filename = userDataHome + File.separatorChar + "appOpts.json";
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
        String fileName = userDataHome + File.separatorChar + locationsFilename;
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
        MemoryBank.debug("Data location is: " + userDataHome);
        File f = new File(userDataHome);
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
            searchDataVector = AppUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<>() {
            });
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
