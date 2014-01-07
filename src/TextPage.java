// TextPage.java - by D. Lee Chastain       9/01/2003
//
// This component consists of lines of text spread across one or
//   more 'pages', with an optional bounding box and header.  It can be
//   scaled as well as printed.  The text is received through the
//   constuctor as a Vector of lines, all other settings such as 
//   size, header, etc, are specified by separate 'set' methods.
//   The paint method of this component allows it to be represented
//   one page at a time.
//
// There is no need (yet?) to make this a JComponent.  Since JComponent
//   comes from Component, it would just be additional unneeded overhead.
//
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.PrintGraphics;
import java.awt.PrintJob;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.text.DateFormat;
import java.util.Date;
import java.util.Vector;

public class TextPage extends Component {
  private static final long serialVersionUID = 6610176903300017933L;

  // This flag is separate and listed first, for obvious reasons.
  private static boolean debugPrint = false; // true;

  private static double screenResHorizontal;
  private static double screenResVertical;

  private int pageNumber;
  private String header;
  private Font headerFont = new Font("Serif", Font.BOLD, 14);
  private Font font;
  private Vector<String> lines;
  private boolean showHeader;
  private boolean showFooter;
  private boolean showBox;
  private Image offscreen_buffer;
  private int pageCount;
  private FontMetrics headerFontMetrics;
  private FontMetrics textFontMetrics;

  private TextPageListener twimc; // To Whom It May Concern

  // Current Date and Time, for header
  private String todaynow;

  // The areas in which the printer cannot print.
  //  How much - left/right or top/bottom
  private int topPageMargin;
  private int bottomPageMargin;
  private int leftPageMargin;
  private int rightPageMargin;

  // Internal margin between boxes and text.
  private int margin = 10;

  // Bounds which includes the header, box, and text.
  // On screen, is viewablePageRect; on paper, is printablePageRect.
  private Rectangle usablePageRect; // so - refer to it as usablePageRect.

  // The bounds of the box.
  private Rectangle boxRect;

  // The bounds of the text.
  private Rectangle textRect;

  // Number of lines per page.
  private int linesPerPage;

  // The lines after they have been broken so they fit
  //   within the width of the textRect.
  private Vector<String> brokenLines;

  static {
    int res = Toolkit.getDefaultToolkit().getScreenResolution();
    dbp("screen page resolution: " + res);

    // The vertical resolution does not have the same problem as the 
    //   horizontal (it depends only on font height vs character width),
    //   and so the same value that is used by the printer will work
    //   here to get the page to break at the same line.
    screenResVertical = 72; 

    screenResHorizontal = 72; // Just a starting point...
  } // end static

  //--------------------------------------------------
  // Constructor
  //--------------------------------------------------
  TextPage(Vector<String> lines) {
    this.lines = lines;
    todaynow = DateFormat.getDateTimeInstance().format(new Date());

    showHeader = true;
    showFooter = true;
    showBox = true;
    offscreen_buffer = null;
    pageCount = 0;
    twimc = null;
  } // end constructor
  //--------------------------------------------------

  public void addTextPageListener(TextPageListener tpl) {
    twimc = tpl;
  } // end addTextPageListener

  // For debug printing.  Short name to declutter source.
  private static void dbp(String s) {
    if(debugPrint) System.out.println(s);
  } // end dbp

  Rectangle inset(Rectangle r, int left, int top, int right, int bottom) {
    Rectangle s = new Rectangle(r);
    s.x += left;
    s.y += top;
    s.width -= (left+right);
    s.height -= (top+bottom);
    return s;
  } // end inset

  // Returns the page count relative to the metrics in g.
  int getPageCount() { return pageCount; }


  // This applies only to the visual page, not the printed one.
  //  So - let's 'prefer' a standard 8.5" x 11" piece of paper.
  //  but - normalize it to the printer's resolution, so that it will
  //    show the same page and line breaks / wraparounds.
  public Dimension getPreferredSize() {
    // Unfortunately, using the same font, the screen is able to pack
    //  in more characters per line than the printer. This comes from the
    //  actual FontMetrics, which has no 'set' methods and so is not
    //  changeable.  The FontMetrics comes from the graphics context
    //  (printer vs screen) which is also not changeable except by changing
    //  the font itself, which would do no good since we've already set 
    //  the screen font and printer font to the same values.  So -

    // Since we cannot affect the FontMetrics where the change is
    //   really needed, we will go for the next best solution, which
    //   is to shrink the size of the usable page so that we normalize
    //   the screen's characters per line back to the same value as
    //   the characters per line for the printer.  Again unfortunately,
    //   this adjustment happens to be different for each point size.
    //   This solution will only work with monospaced fonts.

    double ptSize = font.getSize2D();
    dbp("text Font point size: " + ptSize);

    // The values of horizontal resolutions for the available fonts 
    //   - determined by T&E.
// Obviously - more are needed....
    if     (ptSize == 11) screenResHorizontal = 74;
    else if(ptSize == 12) screenResHorizontal = 69.7;
    else if(ptSize == 13) screenResHorizontal = 74;
    else if(ptSize == 14) screenResHorizontal = 68;
    else if(ptSize == 15) screenResHorizontal = 70;
    else if(ptSize == 16) screenResHorizontal = 74;
    
    dbp("In getPreferredSize, screenResHorizontal = " + screenResHorizontal);

    return new Dimension((int)( 8.5 * screenResHorizontal),
                         (int)(11.0 * screenResVertical));
  } // end getPreferredSize

  public void drawit(Graphics g) {
    int Eks, Why;
    String s;

    prepareLayoutValues(g); // sets boxRect

    // Draw the header.
    g.setFont(headerFont);
    Eks = boxRect.x;
    Why = boxRect.y - headerFontMetrics.getDescent();
    if(showHeader) g.drawString(header, Eks, Why);
    //       dbp(header + "\tX: " + Eks + "\tY: " + Why);

    // Draw the date.
    int todayWidth = headerFontMetrics.stringWidth(todaynow);
    Eks = boxRect.x + boxRect.width - todayWidth;
    if(showHeader) g.drawString(todaynow, Eks, Why);
    //       dbp(today + "\tX: " + Eks + "\tY: " + Why);

    // Draw the footer (page number).
    if(showFooter) {
      s = "Page " + (pageNumber + 1) + " of " + pageCount;
      g.setFont(headerFont);
      Eks = boxRect.x + boxRect.width - headerFontMetrics.stringWidth(s);
      Why = boxRect.y + boxRect.height + headerFontMetrics.getAscent();
      g.drawString(s, Eks, Why);
    } // end if showFooter

    // Draw the box (Of the rectangles, this is the only one that is drawn)
    if(showBox) g.drawRect(boxRect.x, 
        boxRect.y, boxRect.width-1, boxRect.height-1);

    // Find the starting line on the current page.
    int l = Math.min(brokenLines.size(), pageNumber * linesPerPage);
    
    // Draw the strings for this page.
    g.setFont(font);
    Eks = textRect.x;
    Why = textRect.y + textFontMetrics.getAscent();
    for (int i=0; i<linesPerPage && l+i < brokenLines.size(); i++) {
      s = brokenLines.elementAt(l + i); 
      g.drawString(s, Eks, Why);
      Why += textFontMetrics.getHeight();
    } // end for i
  } // end drawit

  public void paint(Graphics g) {
    if(offscreen_buffer != null) {
      g.drawImage(offscreen_buffer, 0, 0, this);
      return;
    } // end if

    // This code runs only one time per mod to the component..
    //----------------------------------------------------------
    offscreen_buffer = createImage(getPreferredSize().width,
        getPreferredSize().height);

    Graphics offg = offscreen_buffer.getGraphics();
    drawit(offg);
    g.drawImage(offscreen_buffer, 0, 0, this);
    //----------------------------------------------------------
  } // end paint


  // Different graphics contexts will cause differences in placement
  //   of line and page breaks between the printout and the screen.
  // This method calculates and sets up critical values based on the 
  //   font size and screen/page resolution in g, in an attempt to
  //   'normalize' the screen values so that it looks identical to the
  //   printout.
  void prepareLayoutValues(Graphics g) {
    Dimension pageSize;
    String s, leftovers;
    int stringWidth;
    float resHorizontal, resVertical;


    // Get the metrics for the different fonts.
    textFontMetrics = g.getFontMetrics(font);
    headerFontMetrics = g.getFontMetrics(headerFont);

    leftovers = "";

    if (g instanceof PrintGraphics) {
      PrintJob pj = ((PrintGraphics)g).getPrintJob();
      pageSize = pj.getPageDimension();
      dbp("PrintGraphics - pageDimension: " + pageSize);
      // This has (so far) always been 612 x 792.

      resHorizontal = pj.getPageResolution();
      resVertical = resHorizontal;
      dbp("PrintJob page resolution: " + resHorizontal);
      // This has (so far) always been 72.
      // This page size divided by this resolution comes out 
      //    to an 8.5" x 11" piece of paper.
    } else { // Values for the screen -
      pageSize = new Dimension(getPreferredSize());
      resHorizontal = pageSize.width / ((float) 8.5);
      resVertical = pageSize.height  / ((float)11.0);
      dbp("Screen Graphics - pageDimension: " + pageSize);
    } // end if

    margin = (int) (.142857142857 * resHorizontal);
    topPageMargin = (int)(.25 * resVertical);
    leftPageMargin = (int)(.25 * resHorizontal);
    rightPageMargin = (int)(.25 * resHorizontal);
    bottomPageMargin = (int)(.5 * resVertical);

    // Debug printing -
    dbp("Using horizontal resolution: " + resHorizontal);
    dbp("Using vertical resolution: " + resVertical);
    dbp("Page Size: " + pageSize);
    dbp("minor margin: " + margin);
    dbp("Top Page Margin: " + topPageMargin);
    dbp("Left Page Margin: " + leftPageMargin);
    dbp("Right Page Margin: " + rightPageMargin);
    dbp("Bottom Page Margin: " + bottomPageMargin);

    // Parameters: x, y, width, height
    usablePageRect = new Rectangle(leftPageMargin, topPageMargin, 
        pageSize.width -  (leftPageMargin + rightPageMargin), 
        pageSize.height - (topPageMargin + bottomPageMargin));
       dbp("Usable Page Rectangle: " + usablePageRect);

    // inset is a rectangle generator - left, top, right, bottom
    boxRect = inset(usablePageRect, margin, 
        headerFontMetrics.getHeight(), margin, 
        headerFontMetrics.getHeight() );

       dbp("Box Rectangle: " + boxRect);
    textRect = inset(boxRect, margin, margin, margin, margin);
       dbp("Text Rectangle: " + textRect);

    int lpp = textRect.height/textFontMetrics.getHeight();

    int lineMax = textRect.width;
    dbp("Line max is: " + lineMax);
//future:  textRect.width + margin/2 - allow some overflow...

      // The metrics may have changed so recompute the broken lines.
      brokenLines = new Vector<String>();
      for (int i=0; i<lines.size(); i++) {
        s = (String)lines.elementAt(i);
        stringWidth = textFontMetrics.stringWidth(s);

        dbp("[" + s + "]");
        dbp("stringWidth = " + stringWidth);
        boolean moreInfo = false;

        if(stringWidth > lineMax) {
          int lastSpacePos = s.lastIndexOf(" ");
          dbp("Last space is at: " + lastSpacePos);
          // if(some condition) moreInfo = true;
        }

        while(stringWidth > lineMax) {
          // The line will be too long; break it.
          dbp("  Breaking this line");

// first, drop off enough chars to get within the limit.  Then,
// check for spaces and keep dropping them off until a better
//  break point is reached.
          
if (moreInfo) dbp("Trying:");

          while( stringWidth > lineMax ) {
            leftovers = s.substring(s.length() - 1) + leftovers;
            s = s.substring(0, s.length() - 1);
            stringWidth = textFontMetrics.stringWidth(s);
if (moreInfo) {
  dbp("  string: " + "[" + s + "]");
  dbp("  leftovers: " + "[" + leftovers + "]");
  dbp("  new width: " + stringWidth);
} // end if
          } // end while
          dbp("stringWidth after break = " + stringWidth);

          brokenLines.addElement(s);
          s = leftovers;
          leftovers = "";
          stringWidth = textFontMetrics.stringWidth(s);
        } // end while
        brokenLines.addElement(s);
      } // end for i

      linesPerPage = lpp;
      pageCount = ((brokenLines.size()-1) / linesPerPage) + 1;
      if(twimc != null) twimc.fontChanged();
      dbp(" ");
  } // end prepareLayoutValues

  // Sets the font and repaints the display.
  public void setFont(Font f) {
    if(font != null) {
      if(font.equals(f)) {
        dbp("Font setting no different!");
        return;
      } // end if
    } // end if

    dbp("Setting a new Font!");
    font = f;
    offscreen_buffer = null; // to force a redraw -
    repaint();
  } // end setFont

  public void setHeader(String s) { header = s; }

  public void setOptions(boolean b1, boolean b2, boolean b3) {
    showHeader = b1;
    showFooter = b2;
    showBox = b3;
  } // end setOptions

  // p is 0-based.
  public boolean setPage(int p) {
    if(pageNumber == p) return true;
    if(p < pageCount) {
      pageNumber = p;
      offscreen_buffer = null;
      repaint();
      if(twimc != null) twimc.pageChanged();
      return true;	
    } else return false;
  } // end setPage

} // end class TextPage
