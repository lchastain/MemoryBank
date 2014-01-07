/* ***************************************************************************
 *
 * File:  $Id: LogIcon.java,v 1.5 2006/07/22 02:06:46 lee Exp $
 *
 * Author:  D. Lee Chastain
 *
 * $Log: LogIcon.java,v $
 * Revision 1.5  2006/07/22 02:06:46  lee
 * Changes in support of the new data/component Note hierarchy.
 *
 * Revision 1.4  2006/02/20 00:54:32  lee
 * Added serialVersionUID, for -Xlint.
 *
 * Revision 1.3  2005/08/21 13:56:14  lee
 * Added the 'no-parameter' constructor, in support of the 'blank' default icon.
 *
 * Revision 1.2  2005/08/20 15:21:43  lee
 * Converted tabs to spaces, fixed much indentation, changed debug
 * printout methodology.
 *
 * Revision 1.1  2005/08/07 15:37:05  lee
 * Initial Version.
 *
 ****************************************************************************/
/**  Addition to an ImageIcon to provide .ico file support
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageProducer;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.swing.ImageIcon;

public class LogIcon extends ImageIcon {
    private static final long serialVersionUID = -1747855358689601291L;

    // Pass-thru constructor to ImageIcon.
    LogIcon() {
        super();
        setDescription(""); // empty is ok, null is not.
    }

    LogIcon(String filename) {
        // Note: Do not construct with null - handle prior.
        MemoryBank.debug("Constructing LogIcon: " + filename);
        Image myImage;

        // If this icon is being constructed for the iconChooser (or
        //   as a result of an iconChooser 'choice'), then 'filename'
        //   will be the full path to the icon file in Program Data.

        // Get the position of 'icons' in filename.
        int iconsIndex = filename.indexOf("icons");

        // If this icon is being reconstructed from saved
        //   user data then 'filename' will be a relative
        //   (short) path, so we need to prefix it with the
        //   path to the user's data.
        if (iconsIndex == 0) {
            filename = MemoryBank.userDataDirPathName + "/" + filename;
            iconsIndex = filename.indexOf("icons");
        } // end if

        // Convert file separator characters, if needed.
        char sep = File.separatorChar;
        if (sep != '/') filename = filename.replace(sep, '/');

        if (filename.endsWith(".ico")) {
            WinIcon wicon;
            wicon = new IconFile(filename).getIcon();
            if (wicon == null) return;
            ImageProducer ip = wicon.getSource();
            myImage = Toolkit.getDefaultToolkit().createImage(ip);
        } else {
            myImage = Toolkit.getDefaultToolkit().getImage(filename);
            if (myImage == null) return;
        } // end if

        // The 'description' is used when saving -
        setDescription(filename.substring(iconsIndex));

        setImage(myImage);
        loadImage(myImage);
        MemoryBank.init();
    } // end constructor


    public void paintIcon(Component c, Graphics g, int x, int y) {
        super.paintIcon(c, g, x, y);
    } // end paintIcon

    //-----------------------------------------------------
    // Method Name: scaleIcon
    //
    //-----------------------------------------------------
    public static LogIcon scaleIcon(LogIcon li) {
        int theHeight, theWidth;

        boolean scaleIt = false;
        Image tmpImg = li.getImage();

        theHeight = li.getIconHeight();
        if (theHeight > 36) {
            theHeight = 36;
            scaleIt = true;
        } // end if

        theWidth = li.getIconWidth();
        if (theWidth > 36) {
            theWidth = 36;
            scaleIt = true;
        } // end if

        if (scaleIt) tmpImg = tmpImg.
                getScaledInstance(theWidth, theHeight, Image.SCALE_SMOOTH);

        li.setImage(tmpImg);
        return li;
    } // end scaleIcon


    //-----------------------------------------
    // Inner Classes
    //-----------------------------------------
    public class WinIcon {
        public IconEntry entry;
        public BitmapHeader bmh;
        public Color[] colors;
        public byte[] data;
        public byte[] maskData;

        public int getWidth() {
            return entry.width;
        }

        public int getHeight() {
            return entry.height;
        }

        public ImageProducer getSource() {
            // make a color model
            int transparent = -1;
            int length = (int) bmh.clrImportant;
            if (length == 0) length = colors.length;
            if (length <= 255) {
                transparent = length;
                length++;
            } else {
                transparent = 255;
            } // end if
            byte[] r = new byte[length];
            byte[] g = new byte[length];
            byte[] b = new byte[length];
            for (int i = 0; i < length; i++) {
                if (i >= colors.length) {
                    // transparent color is "0xFADED0"
                    r[i] = (byte) 250;
                    g[i] = (byte) 206;
                    b[i] = (byte) 192;
                } else {
                    r[i] = (byte) colors[i].getRed();
                    g[i] = (byte) colors[i].getGreen();
                    b[i] = (byte) colors[i].getBlue();
                } // end if
            } // end for
            IndexColorModel cm = null;
            if (transparent >= 0) {
                // use transparent color
                cm = new IndexColorModel(8, length, r, g, b, transparent);
            } else {
                // don't use transparent color
                cm = new IndexColorModel(8, length, r, g, b);
            }
            // make the bytes for the image
            byte[] pix = new byte[entry.height * entry.width];
            int index = 0;  // index into data array
            int mindex = 0; // index into mask data
            int bitsLeft = 8;
            int mBitsLeft = 8;
            int curr = data[index++];
            int mdata = maskData[mindex++];
            int pixPerByte = entry.width * entry.height / data.length;
            int bitsPerPix = 8 / pixPerByte;
            int mask = 255 << (8 - bitsPerPix);
            for (int i = entry.height - 1; i >= 0; i--) {
                for (int j = 0; j < entry.width; j++) {
                    if (bitsLeft == 0) {
                        curr = data[index++];
                        bitsLeft = 8;
                    }
                    if (mBitsLeft == 0) {
                        mdata = maskData[mindex++];
                        mBitsLeft = 8;
                    }
                    int dat = (curr & mask & 255) >>> (8 - bitsPerPix);
                    if ((mdata & 128) != 0) dat = transparent;
                    pix[i * entry.width + j] = (byte) dat;
                    curr <<= bitsPerPix;
                    bitsLeft -= bitsPerPix;
                    mdata <<= 1;
                    mBitsLeft -= 1;
                }
            }
            // make a MemoryImageSource which is an ImageProducer
            MemoryImageSource src = new MemoryImageSource(entry.width, entry.height, cm, pix, 0, entry.width);
            return src;
        }

        public void paint(Graphics g, int x, int y, Color c1) {
            int index = 0, mindex = 0;
            int curr = data[index++];
            int mdata = maskData[mindex++];
            int pixPerByte = entry.width * entry.height / data.length;
            int bitsPerPix = 8 / pixPerByte;
            int mask = 255 << (8 - bitsPerPix);
            if (bitsPerPix == 0) return;
            int bitsLeft = 8, mBitsLeft = 8;
            for (int i = 0; i < entry.height; i++) {
                for (int j = 0; j < entry.width; j++) {
                    if (bitsLeft == 0) {
                        curr = data[index++];
                        bitsLeft = 8;
                    }
                    if (mBitsLeft == 0) {
                        mdata = maskData[mindex++];
                        mBitsLeft = 8;
                    }
                    int dat = (curr & mask & 255) >>> (8 - bitsPerPix);
                    if ((mdata & 128) != 0) g.setColor(c1);
                    else if ((dat < colors.length) && ((mdata & 128) == 0)) {
                        g.setColor(colors[dat]);
                    }
                    g.drawLine(x + j, y - i, x + j, y - i);
                    curr <<= bitsPerPix;
                    bitsLeft -= bitsPerPix;
                    mdata <<= 1;
                    mBitsLeft -= 1;
                } // end for j
            } // end for i
        } // end paint

        public void paintMono(Graphics g, int x, int y, Color c1, Color c2) {
            int index = 0;
            int curr = maskData[index++];
            int bitsLeft = 8;
            for (int i = 0; i < entry.height; i++) {
                for (int j = 0; j < entry.width; j++) {
                    if (bitsLeft == 0) {
                        curr = data[index++];
                        bitsLeft = 8;
                    }
                    if ((curr & 128) == 0) {
                        g.setColor(c1);
                        g.drawLine(x + j, y - i, x + j, y - i);
                    } else {
                        g.setColor(c2);
                        g.drawLine(x + j, y - i, x + j, y - i);
                    } // end if
                    curr <<= 1;
                    bitsLeft -= 1;
                } // end for j
            } // end for i
        } // end paintMono

        public void paintMask(Graphics g, int x, int y, Color c1, Color c2) {
            int index = 0;
            int curr = maskData[index++];
            int bitsLeft = 8;
            for (int i = 0; i < entry.height; i++) {
                for (int j = 0; j < entry.width; j++) {
                    if (bitsLeft == 0) {
                        curr = maskData[index++];
                        bitsLeft = 8;
                    }
                    if ((curr & 128) == 0) {
                        g.setColor(c1);
                        g.drawLine(x + j, y - i, x + j, y - i);
                    } else {
                        g.setColor(c2);
                        g.drawLine(x + j, y - i, x + j, y - i);
                    }
                    curr <<= 1;
                    bitsLeft -= 1;
                } // end for j
            } // end for i
        } // end paintMask
    } // end class WinIcon


    //-------------------------------------------------------------------------

    private class IconFile {
        public Vector<WinIcon> icons;        // contains WinIcon objects
        public String basename;
        private boolean valid;

        private IconFile(String filename) {
            icons = new Vector<WinIcon>(1);
            valid = true;
            try {
                FileInputStream fis = new FileInputStream(filename);
                basename = filename.substring(0, filename.length() - 4);
                readFromStream(fis);
                fis.close();
            } catch (Exception e) {
                MemoryBank.debug(e.toString());
                valid = false;
            }
        } // end constructor

        public WinIcon getIcon() {
            return getIcon(32, 32);
        }

        public WinIcon getIcon(int width, int height) {
            for (int i = 0; i < icons.size(); i++) {
                WinIcon wi = icons.elementAt(i);
                if ((wi.getHeight() == height) && (wi.getWidth() == width))
                    return wi;
            } // end if
            return null;
        } // end getIcon

        private void readFromStream(InputStream is)
                throws IOException {
            WinInputStream dis = new WinInputStream(is);
            // read header
            int type = 0, count = 0;
            try {
                type = dis.readUnsignedShortX();
                count = dis.readUnsignedShortX();
            } catch (IOException ioe) {
                valid = false;
                MemoryBank.debug("Read Header: " + ioe);
                throw ioe;
            }

            if ((count == 0) || (type != 1)) {
                MemoryBank.debug("Count " + count + " type " + type);
                if (count == 0) throw new IOException("No icons found");
                else if (type != 1)
                    throw new IOException("Type " + type + " not understood");
            }

            // read individual entries
            Vector<IconEntry> iconEntries = new Vector<IconEntry>(count);
            for (int i = 0; i < count; i++) {
                IconEntry ie = new IconEntry();
                ie.readFrom(dis);
                MemoryBank.debug(ie.toString());
                if (!ie.isValid()) throw new IOException("Invalid icon entry");
                iconEntries.addElement(ie);

                // read the bitmap header
                BitmapHeader bmh = new BitmapHeader();
                try {
                    dis.seek(ie.imageOffset);
                    bmh.readFrom(dis);
                } catch (IOException ioe) {
                    MemoryBank.debug("Read Bitmap Header: " + ioe);
                    throw (ioe);
                }
                if (!bmh.isValid()) continue;

                MemoryBank.debug(bmh.toString());

                // read the colors
                int pixPerByte = 0;
                Color[] colors = null;
                if (ie.colourCount > 0) {
                    int c = ie.colourCount;
                    if (c == 8) c = 16;
                    if ((c != 2) && (c != 16) && (c != 256)) {
                        MemoryBank.debug("Invalid number of colors: " + ie.colourCount);
                        throw new IOException("Invalid number of colors: " + ie.colourCount);
                    } else if (c == 2) pixPerByte = 8;
                    else if (c == 16) pixPerByte = 2;
                    else pixPerByte = 1;
                    colors = new Color[c];
                    RGBQuad q = new RGBQuad();
                    for (int j = 0; j < c; j++) {
                        colors[j] = null;
                        q.readFrom(dis);
                        if (!q.isValid()) continue;
                        colors[j] = q.toColour();
                    }
                }

                // read the image data
                int bytesInData = ie.width * ie.height / pixPerByte;
                int bytesInMask = ie.width * ie.height / 8;
                MemoryBank.debug("Require " + bytesInData + " bytes for pixel data");
                MemoryBank.debug("Require " + bytesInMask + " bytes for mask data");
                if (bytesInData + bytesInMask != bmh.sizeImage) {
                    MemoryBank.debug("But there are only " + bmh.sizeImage + " bytes of data.");
                    throw new IOException("Wrong number of bytes");
                }
                byte[] data = new byte[bytesInData];
                byte[] mask = new byte[bytesInMask];
                try {
                    dis.read(data);
                    dis.read(mask);
                } catch (IOException ioe) {
                    MemoryBank.debug("Read Data: " + ioe.getMessage());
                    throw ioe;
                }

                // create the WinIcon object
                WinIcon ic = new WinIcon();
                ic.entry = ie;
                ic.bmh = bmh;
                ic.colors = colors;
                ic.data = data;
                ic.maskData = mask;
                icons.addElement(ic);
            }
        } // end readFromString

        public boolean isValid() {
            return valid;
        }

        public String toString() {
            return (basename + " contains " + icons.size() + " icons");
        } // end toString
    } // end class IconFile

    //----------------------------------------------------------------

    private class BitmapHeader {
        private boolean valid;
        public long size, width, height;
        public int planes, bitCount;
        public long compression, sizeImage, xPelsPerMeter, yPelsPerMeter;
        public long clrUsed, clrImportant;

        public BitmapHeader() {
            valid = false;
        }

        public void readFrom(WinInputStream is) {
            try {
                size = is.readIntX();
                width = is.readIntX();
                height = is.readIntX();
                planes = is.readUnsignedShortX();
                bitCount = is.readUnsignedShortX();
                compression = is.readIntX();
                sizeImage = is.readIntX();
                xPelsPerMeter = is.readIntX();
                yPelsPerMeter = is.readIntX();
                clrUsed = is.readIntX();
                clrImportant = is.readIntX();
                valid = true;
            } catch (IOException ioe) {
                valid = false;
                return;
            }
            if (size != 40) valid = false;
        }

        public String toString() {
            if (!valid) return "Invalid bitmap header";
            else return "Bitmap header: " + width + "x" + height +
                    " planes " + planes +
                    " bitCount " + bitCount +
                    " sizeImage " + sizeImage +
                    " used " + clrUsed + " important " + clrImportant;
        }

        public boolean isValid() {
            return valid;
        }
    } // end BitmapHeader

    //----------------------------------------------------------------------

    public class RGBQuad {
        private boolean valid;
        private int r, g, b;

        public RGBQuad() {
            valid = false;
        }

        public void readFrom(WinInputStream is) {
            try {
                byte[] buf = new byte[4];
                is.read(buf);
                r = (buf[2] + 256) % 256;
                g = (buf[1] + 256) % 256;
                b = (buf[0] + 256) % 256;
                valid = true;
            } catch (IOException ioe) {
                valid = false;
                return;
            }
        }

        public String toString() {
            if (!valid) return "Invalid RBQuad";
            else return "RGBQuad";
        }

        public Color toColour() {
            if (valid) return new Color(r, g, b);
            else return null;
        }

        public boolean isValid() {
            return valid;
        }
    }
    //----------------------------------------------------------------------

    public class WinInputStream extends DataInputStream {
        public static final int bufSize = 4096;
        private long markPos = 0;

        public WinInputStream(InputStream is) {
            super(new BufferedInputStream(is, bufSize));
            mark(bufSize);
        }

        public void seek(long posn) throws IOException {
            reset();
            skip(posn - markPos);
        }

        public void setMarkPos(long posn) throws IOException {
            seek(posn);
            mark(bufSize);
            markPos = posn;
        }

        public long getMarkPos() {
            return markPos;
        }

        public int readUnsignedShortX() throws IOException {
            byte[] buf = new byte[2];
            int[] is = new int[2];
            read(buf);
            for (int i = 0; i < 2; i++) {
                is[i] = (buf[i] + 256) % 256;
            }
            return (is[1] << 8) | is[0];
        }

        public int readIntX() throws IOException {
            byte[] buf = new byte[4];
            int[] is = new int[4];
            read(buf);
            for (int i = 0; i < 4; i++) {
                is[i] = (buf[i] + 256) % 256;
            }
            int ret = (is[3] << 24) | (is[2] << 16) | (is[1] << 8) | is[0];
            return ret;
        }
    }
    //----------------------------------------------------------------------

    public class IconEntry {
        private boolean valid;
        public byte width, height, reserved;
        public int colourCount, reserved1, reserved2;
        public int bytesInResource, imageOffset;

        public IconEntry() {
            valid = false;
        }

        public void readFrom(WinInputStream is) {
            try {
                byte[] buf = new byte[4];
                is.read(buf);
                width = buf[0];
                height = buf[1];
                colourCount = (buf[2] + 256) % 256;
                if (colourCount == 0) colourCount = 256;
                reserved = buf[3];
                reserved1 = is.readUnsignedShortX();
                reserved2 = is.readUnsignedShortX();
                bytesInResource = is.readIntX();
                imageOffset = is.readIntX();
                valid = true;
            } catch (IOException ioe) {
                valid = false;
                return;
            }
        }

        public String toString() {
            if (!valid) return "Invalid icon";
            else return "Icon " + width + "x" + height +
                    " with " + colourCount +
                    " colours";
        }

        public boolean isValid() {
            return valid;
        }
    } // end class IconEntry

} // end class LogIcon
