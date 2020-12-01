package extern;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

/**
 * A utility to load TGAs. Note: NOT THREAD SAFE
 *
 * Fresh cut of code but largely influeneced by the TGA loading class
 * provided as part of the Java Monkey Engine (JME). Why not check out
 * what they're doing over at http://www.jmonkeyengine.com. kudos to
 * Mark Powell.
 *
 * @author Kevin Glass
 */
public class TGALoader {
        /** The width of the texture that needs to be generated */
        private static int texWidth;
        /** The height of the texture that needs to be generated */
        private static int texHeight;
        /** The width of the TGA image */
        private static int width;
        /** The height of the TGA image */
        private static int height;
        /** The bit depth of the image */
        private static short pixelDepth;

        /**
         * Create a new TGA Loader
         */
        private TGALoader() {
        }

        /**
         * Flip the endian-ness of the short
         *
         * @param signedShort The short to flip
         * @return The flipped short
         */
        private static short flipEndian(short signedShort) {
                int input = signedShort & 0xFFFF;
                return (short) (input << 8 | (input & 0xFF00) >>> 8);
        }
       
        /**
         * Get the last bit depth read from a TGA
         *
         * @return The last bit depth read
         */
        public static int getLastDepth() {
                return pixelDepth;
        }
       
        /**
         * Get the last width read from a TGA
         *
         * @return Get the last width in pixels fread from a TGA
         */
        public static int getLastWidth() {
                return width;
        }

        /**
         * Get the last height read from a TGA
         *
         * @return Get the last height in pixels fread from a TGA
         */
        public static int getLastHeight() {
                return height;
        }

        /**
         * Get the last required texture width for a loaded image
         *
         * @return Get the ast required texture width for a loaded image
         */
        public static int getLastTexWidth() {
                return texWidth;
        }

        /**
         * Get the ast required texture height for a loaded image
         *
         * @return Get the ast required texture height for a loaded image
         */
        public static int getLastTexHeight() {
                return texHeight;
        }
       
        /**
         * Load a TGA image from the specified stream
         *
         * @param fis The stream from which we'll load the TGA
         * @throws IOException Indicates a failure to read the TGA
         * @return The byte buffer containing texture data
         */
        public static ByteBuffer loadImage(InputStream fis) throws IOException {
                return loadImage(fis,true);
        }
       
        /**
         * Load a TGA image from the specified stream
         *
         * @param fis The stream from which we'll load the TGA
         * @param flipped True if we loading in flipped mode (used for cursors)
         * @return The byte buffer containing texture data
         * @throws IOException Indicates a failure to read the TGA
         */
        public static ByteBuffer loadImage(InputStream fis, boolean flipped) throws IOException {
                byte red = 0;
                byte green = 0;
                byte blue = 0;
                byte alpha = 0;
               
                BufferedInputStream bis = new BufferedInputStream(fis, 100000);
                DataInputStream dis = new DataInputStream(bis);
               
                // Read in the Header
                short idLength = (short) dis.read();
                /*short colorMapType = (short) dis.read();
                short imageType = (short) dis.read();
                short cMapStart = flipEndian(dis.readShort());
                short cMapLength = flipEndian(dis.readShort());
                short cMapDepth = (short) dis.read();
                short xOffset = flipEndian(dis.readShort());
                short yOffset = flipEndian(dis.readShort());*/
               
                width = flipEndian(dis.readShort());
                height = flipEndian(dis.readShort());
                pixelDepth = (short) dis.read();
               
                texWidth = get2Fold(width);
                texHeight = get2Fold(height);
               
                //short imageDescriptor = (short) dis.read();
                // Skip image ID
                if (idLength > 0) {
                        bis.skip(idLength);
                }
               
                byte[] rawData = null;
                if (pixelDepth == 32)
                        rawData = new byte[texWidth * texHeight * 4];
                else
                        rawData = new byte[texWidth * texHeight * 3];
               
                if (pixelDepth == 24) {
                        for (int i = height-1; i >= 0; i--) {
                                for (int j = 0; j < width; j++) {
                                        blue = dis.readByte();
                                        green = dis.readByte();
                                        red = dis.readByte();
                                       
                                        int ofs = ((j + (i * texWidth)) * 3);
                                        rawData[ofs] = red;
                                        rawData[ofs + 1] = green;
                                        rawData[ofs + 2] = blue;
                                }
                        }
                } else if (pixelDepth == 32) {
                        if (flipped) {
                                for (int i = height-1; i >= 0; i--) {
                                        for (int j = 0; j < width; j++) {
                                                blue = dis.readByte();
                                                green = dis.readByte();
                                                red = dis.readByte();
                                                alpha = dis.readByte();
                                               
                                                int ofs = ((j + (i * texWidth)) * 4);
                                               
                                                rawData[ofs] = red;
                                                rawData[ofs + 1] = green;
                                                rawData[ofs + 2] = blue;
                                                rawData[ofs + 3] = alpha;
                                               
                                                if (alpha == 0) {
                                                        rawData[ofs + 2] = (byte) 0;
                                                        rawData[ofs + 1] = (byte) 0;
                                                        rawData[ofs] = (byte) 0;
                                                }
                                        }
                                }
                        } else {
                                for (int i = 0; i < height; i++) {
                                        for (int j = 0; j < width; j++) {
                                                blue = dis.readByte();
                                                green = dis.readByte();
                                                red = dis.readByte();
                                                alpha = dis.readByte();
                                               
                                                int ofs = ((j + (i * texWidth)) * 4);
                                               
                                                rawData[ofs + 2] = red;
                                                rawData[ofs + 1] = green;
                                                rawData[ofs] = blue;
                                                rawData[ofs + 3] = alpha;
                                               
                                                if (alpha == 0) {
                                                        rawData[ofs + 2] = (byte) 0;
                                                        rawData[ofs + 1] = (byte) 0;
                                                        rawData[ofs] = (byte) 0;
                                                }
                                        }
                                }
                        }
                }
                fis.close();
               
                // Get a pointer to the image memory
                ByteBuffer scratch = BufferUtils.createByteBuffer(rawData.length);
                scratch.put(rawData);
                scratch.flip();
               
                return scratch;
        }

    /**
     * Get the closest greater power of 2 to the fold number
     *
     * @param fold The target number
     * @return The power of 2
     */
    private static int get2Fold(int fold) {
        int ret = 2;
        while (ret < fold) {
            ret *= 2;
        }
        return ret;
    }
}
