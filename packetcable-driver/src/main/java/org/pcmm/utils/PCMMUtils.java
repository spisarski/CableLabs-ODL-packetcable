/**
 @header@
 */
package org.pcmm.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PCMMUtils {

    private final static Logger logger = LoggerFactory.getLogger(PCMMUtils.class);

    public static void WriteBinaryDump(final String rootFileName, final byte[] buffer) {
        // Make this Unique
        final String fileName = "/tmp/" + rootFileName + "-" + java.util.UUID.randomUUID() + ".bin";
        try {

            logger.info("Open fileName " + fileName);
            final FileOutputStream outputStream = new FileOutputStream(fileName);

            // write() writes as many bytes from the buffer
            // as the length of the buffer. You can also
            // use
            // write(buffer, offset, length)
            // if you want to write a specific number of
            // bytes, or only part of the buffer.
            outputStream.write(buffer);

            // Always close files.
            outputStream.close();

            logger.info("Wrote " + buffer.length + " bytes");
        } catch (IOException ex) {
            logger.error("Error writing file '" + fileName + "'", ex);
            // Or we could just do this:
            // ex.printStackTrace();
        }
    }

    public static byte[] ReadBinaryDump(final String fileName) {
        // The name of the file to open.
        // String fileName = "COPSReportMessage.txt";
        try {
            final FileInputStream inputStream = new FileInputStream(fileName);
            // Use this for reading the data.
            final byte[] buffer = new byte[inputStream.available()];
            // read fills buffer with data and returns
            // the number of bytes read (which of course
            // may be less than the buffer size, but
            // it will never be more).
            final int total = inputStream.read(buffer);

            // Always close files.
            inputStream.close();

            logger.info("Read " + total + " bytes");
            return buffer;
        } catch (final FileNotFoundException ex) {
            logger.info("Unable to open file '" + fileName + "'");
        } catch (IOException ex) {
            logger.error("Error reading file '" + fileName + "'", ex);
            // Or we could just do this:
            // ex.printStackTrace();
        }
        return null;
    }
}
