import java.util.Vector;

// The data for a single paragraph.  The associated text is provided for visual review and
//   for paragraph length calculation but is not added when deserializing since the
//   contentData will do that fragment-by-fragment.
class ParagraphData {
    String paragraphTextFragment;
//    int startOffset, endOffset;
    Vector<AttributeData> paragraphAttributeData;
    Vector<ContentData> contentData;

    ParagraphData() {
        paragraphTextFragment = "";
        paragraphAttributeData = new Vector<>(0, 1);
        contentData = new Vector<>(0, 1);
    }

    ParagraphData(String theText) {
        this();
        paragraphTextFragment = theText;
    }

//    ParagraphData(String theText, int start, int end) {
//        this();
//        paragraphTextFragment = theText;
//        startOffset = start;
//        endOffset = end;
//    }

    void addContent(ContentData theContent) {
        contentData.add(theContent);
    }


}
