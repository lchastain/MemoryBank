import javax.swing.*;
import java.awt.*;

public class Area51 {
    private Notifier optionPane = new Notifier() {
    };

    static {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("lex@doughmain.net");
    }

    private Area51() {
    }

    // Developing a new picklist widget, to replace a system-native File chooser.  It
    // looks like all that is needed is a direct usage of JOptionPane.  argh!
    // This was while working to replace the filechooser in TodoNoteGroup used in merging.
    private void try1() {
        ImageIcon theIcon = new ImageIcon("icons/acro.ico");

        String[] nums = {
                "00", "01", "02", "03", "04", "05", "06", "07", "08", "09",
                "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
                "20", "21", "22", "23", "24", "25", "26", "27", "28", "29",
                "30", "31", "32", "33", "34", "35", "36", "37", "38", "39",
                "40", "41", "42", "43", "44", "45", "46", "47", "48", "49",
                "50", "51", "52", "53", "54", "55", "56", "57", "58", "59",
                "60", "61", "62", "63", "64", "65", "66", "67", "68", "69",
                "70", "71", "72", "73", "74", "75", "76", "77", "78", "79",
                "80", "81", "82", "83", "84", "85", "86", "87", "88", "89",
                "90", "91", "92", "93", "94", "95", "96", "97", "98", "99"};


        String theChoice = optionPane.showInputDialog(null, "Choose a group",
                "Group Chooser", JOptionPane.PLAIN_MESSAGE, null, nums, "89");

        System.out.println("The choice is: " + theChoice);
    }

    // Developing linkages and testing serialization issues.
    private void try2() {
        // Make a new GoalGroup
        String groupName = "WorldPlan1";
        GoalGroupPanel goalGroup = new GoalGroupPanel(groupName);
        ((GoalGroupProperties) goalGroup.myNoteGroup.myProperties).longTitle = "Take Over The World";
        System.out.println("The goal group properties to JSON is: " + AppUtil.toJsonString(goalGroup.myNoteGroup.myProperties));

        // Make a todo list item and a link to the Goal.
        TodoNoteData todoNoteData = new TodoNoteData();
        todoNoteData.noteString = "Notify the media";

//        LinkTargetData linkage = new LinkTargetData(goalGroup.myProperties.instanceId, null);
//        todoNoteData.linkTargets.setNotes(linkage);

        System.out.println("The TodoNoteData with link to a Goal, to JSON is: " + AppUtil.toJsonString(todoNoteData));
    }


    // Developing linkages and testing serialization issues.
    private void try3() {
        // Make a new EventNoteGroup
        String groupName = "holidays";
        EventNoteGroupPanel goalGroup = new EventNoteGroupPanel(groupName);

        // Make a new EventNoteData
        EventNoteData eventNoteData = new EventNoteData();
        eventNoteData.noteString = "An exciting event";

        // Make a todo list item.
        TodoNoteData todoNoteData = new TodoNoteData();
        todoNoteData.noteString = "Notify the media";

        // Make a  link to the Event Note
        // We send a 'new' NoteData vs the original eventNoteData, in order to strip off unwanted Event data members.
        // Also this approach has the effect of isolating changes to the encapsulated NoteData (such as nulling out
        // its linkTargets to avoid infinite recursion) from the original EventNoteData.
        NoteData targetNoteData = new NoteData(eventNoteData);
//        LinkTargetData linkage = new LinkTargetData(UUID.randomUUID(), targetNoteData);
//        todoNoteData.linkTargets.setNotes(linkage);

        System.out.println("The TodoNoteData with link to an Event and its note, to JSON is: " + AppUtil.toJsonString(todoNoteData));
    }

    // Developing linkages and testing serialization issues.
    private void try4() {

        // Make a new GoalGroup
        String groupName = "WorldPlan1";
        GoalGroupPanel goalGroup = new GoalGroupPanel(groupName);
        ((GoalGroupProperties) goalGroup.myNoteGroup.myProperties).longTitle = "Take Over The World";
        System.out.println("The goal group properties to JSON is: " + AppUtil.toJsonString(goalGroup.myNoteGroup.myProperties));

        // Make a todo list item and a link to the Goal.
        TodoNoteData todoNoteData = new TodoNoteData();
        todoNoteData.noteString = "Notify the media";

//        LinkTargetData linkage = new LinkTargetData(goalGroup.myProperties.instanceId, null);
//        todoNoteData.linkTargets.setNotes(linkage);

        System.out.println("The TodoNoteData with link to a Goal, to JSON is: " + AppUtil.toJsonString(todoNoteData));
    }

    private void try5() {
                JFrame f = new JFrame();
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                Box rowOne = Box.createHorizontalBox();
                rowOne.add(new JLabel("Username"));
                rowOne.add(new JTextField());
                Component rowTwo = Box.createHorizontalGlue();

                f.add(rowOne, BorderLayout.NORTH);
//                f.setNotes(rowTwo, BorderLayout.SOUTH);
                f.setSize(300, 200);
                f.setVisible(true);
    }

    public static void main(String[] args) {
        Area51 a51 = new Area51();
        //a51.try1();
        //a51.try2();
        //a51.try3();
        //a51.try5();
        GroupInfo groupInfo = new GroupInfo("Tuesday, October 20, 2020", GroupType.DAY_NOTES);
        System.out.println(CalendarNoteGroup.getDateFromGroupName(groupInfo).toString());
        groupInfo = new GroupInfo("September 2020", GroupType.MONTH_NOTES);
        System.out.println(CalendarNoteGroup.getDateFromGroupName(groupInfo).toString());
        groupInfo = new GroupInfo("2018", GroupType.YEAR_NOTES);
        System.out.println(CalendarNoteGroup.getDateFromGroupName(groupInfo).toString());

    }

}

