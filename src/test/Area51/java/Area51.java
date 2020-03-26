import javax.swing.*;

public class Area51 {
    private Notifier optionPane = new Notifier() {};

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
        AppIcon theIcon = new AppIcon("icons/acro.ico");

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

    // Developing and testing linkages and serialization issues.
//    @SuppressWarnings({"unchecked"})
//    private void try2() {
//        // Make an instance of the AppOptions
//        AppOptions appOptions = new AppOptions();
//
//        // Make a new GoalGroup
//        String groupName = "WorldPlan1";
//        GoalGroup goalGroup = new GoalGroup(groupName);
//        appOptions.goalsList.add(groupName);
//        goalGroup.myProperties.goalTitle = "Take Over The World";
//        System.out.println("The goal group properties to JSON is: " + AppUtil.toJsonString(goalGroup.myProperties));
//
//        // Make a todo list item and a link to the Goal.
//        NoteData todoNoteData = new TodoNoteData();
//        todoNoteData.noteString = "Notify the media";
//        LinkData linkage = new LinkData();
//        linkage.setSourceGroupId(UUID.randomUUID());
//        linkage.setSourceNoteData((AllNoteData) (NoteData) todoNoteData);
//        linkage.setTargetGroupId(goalGroup.myProperties.instanceId);
//        //System.out.println("The TodoNoteData to JSON is: " + AppUtil.toJsonString(todoNoteData));
//
//        // Add the linkage to the overall list
//        appOptions.linkages.add(linkage);
//        System.out.println("The AppOptions to JSON is: " + AppUtil.toJsonString(appOptions));
//    }

    public static void main(String[] args) {
        Area51 a51 = new Area51();
        //a51.try1();
//        a51.try2();
        //a51.try3();

    }

}

