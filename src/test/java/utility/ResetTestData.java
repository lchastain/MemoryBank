import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

// REMEMBER - just putting new files into the Resources is not enough!  You will need to do a full
// project rebuild, in order to pull the resources into the 'real' ('out') place where they are accessed.

public class ResetTestData {
    public ResetTestData() {
    }

    public static void main(String[] args) throws IOException {
        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        try {
            FileUtils.cleanDirectory(testData);
        } catch (Exception ignore){}

        // Retrieve a fresh set of test data from test resources.
        // This test user has a rich set of data, includes Search Results and Todo Lists
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);
    }
}
