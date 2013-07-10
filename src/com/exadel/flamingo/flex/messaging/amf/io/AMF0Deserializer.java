/*
 * www.openamf.org
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package com.exadel.flamingo.flex.messaging.amf.io;

import com.exadel.flamingo.flex.amf.AMF0Body;
import com.exadel.flamingo.flex.amf.AMF0Message;
import flex.messaging.io.ASObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * AMF Deserializer
 *
 * @author Jason Calabrese <jasonc@missionvi.com>
 * @author Pat Maddox <pergesu@users.sourceforge.net>
 * @author Sylwester Lachiewicz <lachiewicz@plusnet.pl>
 * @version $Revision: 56 $, $Date: 2007-11-05 12:19:46 +0200 (Mon, 05 Nov 2007) $
 */
public class AMF0Deserializer {

    private static Log log = LogFactory.getLog(AMF0Deserializer.class);

    private List<Object> storedObjects = null;

    /**
     * The AMF input stream
     */
    protected DataInputStream inputStream;

    /**
     * Number of headers in the packet
     */
    protected int headerCount;

    /**
     * Content of the headers
     */
    protected List<?> headers = new ArrayList<Object>();

    /**
     * Number of bodies
     */
    protected int bodyCount;

    /**
     * Content of the bodies
     */
    protected List<?> bodies = new ArrayList<Object>();

    /**
     * Object to store the deserialized data
     */
    protected AMF0Message message = new AMF0Message();
    

    /**
     * Deserialize message
     *
     * @param inputStream message input stream
     * @throws IOException
     */
    public AMF0Deserializer(DataInputStream inputStream) throws IOException {
        //if (log.isInfoEnabled()) log.info("Deserializing Message, for more info turn on debug level");

        // Save the input stream for this object
        this.inputStream = inputStream;
        
        // Read the binary header
        readHeaders();
        if (log.isDebugEnabled()) log.debug("readHeader");
        // Read the binary body
        readBodies();
        if (log.isDebugEnabled()) log.debug("readBody");
    }

    public AMF0Message getAMFMessage() {
        return message;
    }
    
    /**
     * Read message header
     *
     * @throws IOException
     */
    protected void readHeaders() throws IOException {
        // version
        message.setVersion(inputStream.readUnsignedShort());
        // Find total number of header elements
        headerCount = inputStream.readUnsignedShort();
        if (log.isDebugEnabled()) log.debug("headerCount = " + headerCount);
        // Loop over all the header elements
        for (int i = 0; i < headerCount; i++) {
            // clear storedObjects - references are new for every header
            storedObjects = new ArrayList<Object>();
            String key = inputStream.readUTF();
            // Find the must understand flag
            boolean required = inputStream.readBoolean();
            // Grab the length of the header element
            /*long length =*/ inputStream.readInt();
            // Grab the type of the element
            byte type = inputStream.readByte();
            // Turn the element into real data
            Object value = readData(type);
            // Save the name/value into the headers array
            message.addHeader(key, required, value);
        }
    }

    /**
     * Read message body
     *
     * @throws IOException
     */
    protected void readBodies() throws IOException {
        // Find the total number of body elements
        bodyCount = inputStream.readUnsignedShort();
        if (log.isDebugEnabled()) log.debug("bodyCount = " + bodyCount);

        // Loop over all the body elements
        for (int i = 0; i < bodyCount; i++) {
            //clear storedObjects
            storedObjects = new ArrayList<Object>();
            // The target method
            String method = inputStream.readUTF();
            // The target that the client understands
            String target = inputStream.readUTF();
            // Get the length of the body element
            /*long length =*/ inputStream.readInt();
            // Grab the type of the element
            byte type = inputStream.readByte();
            if (log.isDebugEnabled()) log.debug("type = " + type);
            // Turn the argument elements into real data
            Object data = readData(type);
            // Add the body element to the body object
            message.addBody(method, target, data, type);
        }
    }

    /**
     * Reads custom class
     *
     * @return
     * @throws IOException
     */
    protected Object readCustomClass() throws IOException {
        // Grab the explicit type - somehow it works
        String type = inputStream.readUTF();
        if (log.isDebugEnabled()) log.debug("Reading Custom Class: " + type);
        /*
        String mappedJavaClass = OpenAMFConfig.getInstance().getJavaClassName(type);
        if (mappedJavaClass != null) {
            type = mappedJavaClass;
        }
        */
        ASObject aso = new ASObject(type);
        // The rest of the bytes are an object without the 0x03 header
        return readObject(aso);
    }

    protected ASObject readObject() throws IOException {
        ASObject aso = new ASObject();
        return readObject(aso);
    }

    /**
     * Reads an object and converts the binary data into an List
     *
     * @param aso
     * @return
     * @throws IOException
     */
    protected ASObject readObject(ASObject aso) throws IOException {
        storeObject(aso);
        // Init the array
        if (log.isDebugEnabled()) log.debug("reading object");
        // Grab the key
        String key = inputStream.readUTF();
        for (byte type = inputStream.readByte();
             type != 9;
             type = inputStream.readByte()) {
            // Grab the value
            Object value = readData(type);
            // Save the name/value pair in the map
            if (value == null) {
                log.info("Skipping NULL value for :" + key);
            } else {
                aso.put(key, value);
                if (log.isDebugEnabled()) log.debug(" adding {key=" + key + ", value=" + value + ", type=" + type + "}");
            }
            // Get the next name
            key = inputStream.readUTF();
        }
        if (log.isDebugEnabled()) log.debug("finished reading object");
        // Return the map
        return aso;
    }

    /**
     * Reads array
     *
     * @return
     * @throws IOException
     */
    protected List<?> readArray() throws IOException {
        // Init the array
        List<Object> array = new ArrayList<Object>();
        storeObject(array);
        if (log.isDebugEnabled()) log.debug("Reading array");
        // Grab the length of the array
        long length = inputStream.readInt();
        if (log.isDebugEnabled()) log.debug("array length = " + length);
        // Loop over all the elements in the data
        for (long i = 0; i < length; i++) {
            // Grab the type for each element
            byte type = inputStream.readByte();
            // Grab the element
            Object data = readData(type);
            array.add(data);
        }
        // Return the data
        return array;
    }

    /**
     * Store object in  internal array
     *
     * @param o
     */
    private void storeObject(Object o) {
        storedObjects.add(o);
        if (log.isDebugEnabled()) log.debug("storedObjects.size: " + storedObjects.size());
    }

    /**
     * Reads date
     *
     * @return
     * @throws IOException
     */
    protected Date readDate() throws IOException {
        long ms = (long) inputStream.readDouble(); // Date in millis from 01/01/1970

      // here we have to read in the raw
      // timezone offset (which comes in minutes, but incorrectly signed),
      // make it millis, and fix the sign.
      int timeoffset = inputStream.readShort() * 60000 * -1; // now we have millis

      TimeZone serverTimeZone = TimeZone.getDefault();

      // now we subtract the current timezone offset and add the one that was passed
      // in (which is of the Flash client), which gives us the appropriate ms (i think)
      // -alon
      Calendar sent = new GregorianCalendar();
      sent.setTime( (new Date(ms - serverTimeZone.getRawOffset() + timeoffset)));

      TimeZone sentTimeZone = sent.getTimeZone();

      // we have to handle daylight savings ms as well
      if (sentTimeZone.inDaylightTime(sent.getTime()))
      {
          //
          // Implementation note: we are trying to maintain compatibility
          // with J2SE 1.3.1
          //
          // As such, we can't use java.util.Calendar.getDSTSavings() here
          // 
        sent.setTime(new java.util.Date(sent.getTime().getTime() - 3600000));
      }

      return sent.getTime();
    }

    /**
     * Reads flushed stored object
     *
     * @return
     * @throws IOException
     */
    protected Object readFlushedSO() throws IOException {
        int index = inputStream.readUnsignedShort();
        if (log.isDebugEnabled()) log.debug("Object Index: " + index);
        return storedObjects.get(index);
    }

    /**
     * Reads object
     *
     * @return
     */
    protected Object readASObject() {
        return null;
    }

    /**
     * Reads object
     *
     * @return
     */
    protected Object readAMF3Data() throws IOException {
    	ObjectInput amf3 = new AMF3Deserializer(inputStream);
		
        try {
            return amf3.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads object from inputstream with selected type
     *
     * @param type
     * @return
     * @throws IOException
     */
    protected Object readData(byte type) throws IOException {
        if (log.isDebugEnabled()) log.debug("Reading data of type " + AMF0Body.getObjectTypeDescription(type));
        switch (type) {
            case AMF0Body.DATA_TYPE_NUMBER: // 0
                return new Double(inputStream.readDouble());
            case AMF0Body.DATA_TYPE_BOOLEAN: // 1
                return Boolean.valueOf(inputStream.readBoolean());
            case AMF0Body.DATA_TYPE_STRING: // 2
                return inputStream.readUTF();
            case AMF0Body.DATA_TYPE_OBJECT: // 3
                return readObject();
            case AMF0Body.DATA_TYPE_MOVIE_CLIP: // 4
                throw new IOException("Unknown/unsupported object type " + AMF0Body.getObjectTypeDescription(type));
            case AMF0Body.DATA_TYPE_NULL: // 5
            case AMF0Body.DATA_TYPE_UNDEFINED: //6
                return null;
            case AMF0Body.DATA_TYPE_REFERENCE_OBJECT: // 7
                return readFlushedSO();
            case AMF0Body.DATA_TYPE_MIXED_ARRAY: // 8
                /*long length =*/ inputStream.readInt();
                //don't do anything with the length
                return readObject();
            case AMF0Body.DATA_TYPE_OBJECT_END: // 9
                return null;
            case AMF0Body.DATA_TYPE_ARRAY: // 10
                return readArray();
            case AMF0Body.DATA_TYPE_DATE: // 11
                return readDate();
            case AMF0Body.DATA_TYPE_LONG_STRING: // 12
                return readLongUTF(inputStream);
            case AMF0Body.DATA_TYPE_AS_OBJECT: // 13
                return readASObject();
            case AMF0Body.DATA_TYPE_RECORDSET: // 14
                return null;
            case AMF0Body.DATA_TYPE_XML: // 15
                return convertToDOM(inputStream);
            case AMF0Body.DATA_TYPE_CUSTOM_CLASS: // 16
                return readCustomClass();
            case AMF0Body.DATA_TYPE_AMF3_OBJECT: // 17
                return readAMF3Data();
            default :
                throw new IOException("Unknown/unsupported object type " + AMF0Body.getObjectTypeDescription(type));
        }
    }

    /**
     * This is a hacked verison of Java's DataInputStream.readUTF(), which only
     * supports Strings <= 65535 UTF-8-encoded characters
     */
    private Object readLongUTF(DataInputStream in) throws IOException {
      int utflen = in.readInt();
      StringBuffer str = new StringBuffer(utflen);
      byte bytearr [] = new byte[utflen];
      int c, char2, char3;
      int count = 0;

      in.readFully(bytearr, 0, utflen);

      while (count < utflen) {
        c = bytearr[count] & 0xff;
        switch (c >> 4) {
          case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
            /* 0xxxxxxx*/
            count++;
            str.append((char)c);
            break;
          case 12: case 13:
            /* 110x xxxx   10xx xxxx*/
            count += 2;
            if (count > utflen)
              throw new UTFDataFormatException();
            char2 = bytearr[count-1];
            if ((char2 & 0xC0) != 0x80)
              throw new UTFDataFormatException(); 
            str.append((char)(((c & 0x1F) << 6) | (char2 & 0x3F)));
              break;
          case 14:
            /* 1110 xxxx  10xx xxxx  10xx xxxx */
            count += 3;
            if (count > utflen)
              throw new UTFDataFormatException();
            char2 = bytearr[count-2];
            char3 = bytearr[count-1];
            if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
              throw new UTFDataFormatException();   
            str.append((char)(((c     & 0x0F) << 12) |
                              ((char2 & 0x3F) << 6)  |
                              ((char3 & 0x3F) << 0)));
            break;
          default:
            /* 10xx xxxx,  1111 xxxx */
            throw new UTFDataFormatException();     
        }
      }

      // The number of chars produced may be less than utflen
      return new String(str);
    }
    
    public static Document convertToDOM(InputStream is) throws IOException {
        Document document = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        is.skip(4); // skip length
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(is));
        } catch (Exception e) {
            log.error(e, e);
            throw new IOException("Error while parsing xml: " + e.getMessage());
        }
        return document;
    }
    
    //private static final String data1 = "00 03 00 00 00 01 00 04 6E 75 6C 6C 00 02 2F 31 00 00 00 D1 0A 00 00 00 01 11 0A 81 23 4D 66 6C 65 78 2E 6D 65 73 73 61 67 69 6E 67 2E 6D 65 73 73 61 67 65 73 2E 43 6F 6D 6D 61 6E 64 4D 65 73 73 61 67 65 13 6F 70 65 72 61 74 69 6F 6E 1D 6D 65 73 73 61 67 65 52 65 66 54 79 70 65 1B 63 6F 72 72 65 6C 61 74 69 6F 6E 49 64 11 63 6C 69 65 6E 74 49 64 15 74 69 6D 65 54 6F 4C 69 76 65 13 6D 65 73 73 61 67 65 49 64 0F 68 65 61 64 65 72 73 13 74 69 6D 65 73 74 61 6D 70 09 62 6F 64 79 17 64 65 73 74 69 6E 61 74 69 6F 6E 04 05 01 06 01 01 04 00 06 49 38 31 45 34 34 39 33 33 2D 43 35 35 34 2D 43 33 33 46 2D 41 37 37 34 2D 43 33 36 44 46 36 30 44 33 39 32 36 0A 0B 01 01 04 00 0A 05 01 06 01";
    //private static final String data1 = "00 03 00 00 00 01 00 0B 2F 31 2F 6F 6E 52 65 73 75 6C 74 00 00 FF FF FF FF 11 0A 81 03 55 66 6C 65 78 2E 6D 65 73 73 61 67 69 6E 67 2E 6D 65 73 73 61 67 65 73 2E 41 63 6B 6E 6F 77 6C 65 64 67 65 4D 65 73 73 61 67 65 17 64 65 73 74 69 6E 61 74 69 6F 6E 0F 68 65 61 64 65 72 73 1B 63 6F 72 72 65 6C 61 74 69 6F 6E 49 64 13 6D 65 73 73 61 67 65 49 64 13 74 69 6D 65 73 74 61 6D 70 11 63 6C 69 65 6E 74 49 64 15 74 69 6D 65 54 6F 4C 69 76 65 09 62 6F 64 79 01 01 06 49 63 38 34 39 32 32 39 39 2D 36 34 66 39 2D 34 66 34 30 2D 38 66 38 35 2D 39 38 31 30 33 34 30 64 34 37 32 33 06 49 62 39 32 33 38 30 33 32 2D 64 65 30 63 2D 34 30 36 35 2D 62 36 31 65 2D 35 66 33 64 34 30 34 61 32 64 32 36 08 01 00 00 01 0E D8 09 B4 8D 06 49 36 31 33 38 35 65 61 31 2D 66 66 35 36 2D 34 35 32 37 2D 39 65 35 66 2D 34 33 39 66 62 37 36 38 39 32 39 31 04 00 01";
    private static final String data1   = "00 03 00 00 00 01 00 04 6E 75 6C 6C 00 02 2F 33 00 00 00 92 0A 00 00 00 01 11 0A 81 13 4F 66 6C 65 78 2E 6D 65 73 73 61 67 69 6E 67 2E 6D 65 73 73 61 67 65 73 2E 52 65 6D 6F 74 69 6E 67 4D 65 73 73 61 67 65 0D 73 6F 75 72 63 65 13 6F 70 65 72 61 74 69 6F 6E 09 62 6F 64 79 17 64 65 73 74 69 6E 61 74 69 6F 6E 11 63 6C 69 65 6E 74 49 64 15 74 69 6D 65 54 6F 4C 69 76 65 13 6D 65 73 73 61 67 65 49 64 0F 68 65 61 64 65 72 73 13 74 69 6D 65 73 74 61 6D 70 01 06 0D 75 70 64 61 74 65 09 03 01 0A 73 39 66 6C 65 78 2E 74 65 73 74 64 72 69 76 65 2E 73 74 6F 72 65 2E 50 72 6F 64 75 63 74 09 6E 61 6D 65 0B 70 72 69 63 65 0B 69 6D 61 67 65 11 63 61 74 65 67 6F 72 79 07 75 69 64 13 70 72 6F 64 75 63 74 49 64 17 64 65 73 63 72 69 70 74 69 6F 6E 06 0D 70 72 6F 64 20 33 05 40 12 3D 70 A3 D7 0A 3D 06 13 70 72 6F 64 33 2E 70 6E 67 06 17 70 72 6F 64 75 63 74 20 63 61 74 06 49 33 41 44 38 35 46 34 37 2D 31 35 33 36 2D 42 38 34 46 2D 36 36 34 42 2D 30 31 41 30 37 45 30 32 38 34 39 31 04 03 06 25 70 72 6F 64 20 64 65 73 63 72 69 70 74 69 6F 6E 20 33 06 0F 70 72 6F 64 75 63 74 01 04 00 06 49 37 32 31 32 39 46 38 37 2D 45 41 45 46 2D 35 46 37 35 2D 41 37 37 30 2D 30 31 41 30 41 35 45 43 42 37 34 32 0A 0B 01 15 44 53 45 6E 64 70 6F 69 6E 74 06 0D 6D 79 2D 61 6D 66 01 04 00";
    //private static final String data1   = "00 03 00 00 00 01 00 0B 2F 32 2F 6F 6E 52 65 73 75 6C 74 00 00 FF FF FF FF 11 0A 81 03 55 66 6C 65 78 2E 6D 65 73 73 61 67 69 6E 67 2E 6D 65 73 73 61 67 65 73 2E 41 63 6B 6E 6F 77 6C 65 64 67 65 4D 65 73 73 61 67 65 09 62 6F 64 79 11 63 6C 69 65 6E 74 49 64 1B 63 6F 72 72 65 6C 61 74 69 6F 6E 49 64 17 64 65 73 74 69 6E 61 74 69 6F 6E 0F 68 65 61 64 65 72 73 13 6D 65 73 73 61 67 65 49 64 15 74 69 6D 65 54 6F 4C 69 76 65 13 74 69 6D 65 73 74 61 6D 70 0A 07 43 66 6C 65 78 2E 6D 65 73 73 61 67 69 6E 67 2E 69 6F 2E 41 72 72 61 79 43 6F 6C 6C 65 63 74 69 6F 6E 09 03 01 0A 63 39 66 6C 65 78 2E 74 65 73 74 64 72 69 76 65 2E 73 74 6F 72 65 2E 50 72 6F 64 75 63 74 11 63 61 74 65 67 6F 72 79 17 64 65 73 63 72 69 70 74 69 6F 6E 0B 69 6D 61 67 65 09 6E 61 6D 65 0B 70 72 69 63 65 13 70 72 6F 64 75 63 74 49 64 06 17 70 72 6F 64 75 63 74 20 63 61 74 06 25 70 72 6F 64 20 64 65 73 63 72 69 70 74 69 6F 6E 20 31 06 13 70 72 6F 64 31 2E 70 6E 67 06 0D 70 72 6F 64 20 31 05 40 16 3D 70 A3 D7 0A 3D 04 01 06 49 34 61 64 65 63 36 65 31 2D 35 37 66 61 2D 34 32 61 38 2D 38 35 36 38 2D 65 65 39 62 37 64 65 38 35 32 65 35 06 49 38 46 36 36 39 31 32 39 2D 39 44 46 35 2D 44 34 31 31 2D 31 36 34 31 2D 30 31 33 35 37 39 37 41 37 37 39 43 01 01 06 49 38 36 61 38 66 32 31 36 2D 35 34 34 34 2D 34 34 36 30 2D 39 33 37 37 2D 39 64 37 62 37 62 65 34 65 64 32 61 04 00 08 01 42 70 F0 13 81 5B 20 00";
    
    public static void main(String[] args) throws Exception {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(toByteArray(data1)));
        
        AMF0Deserializer des = new AMF0Deserializer(dis);
        System.out.println(des.getAMFMessage().toString());
    }
    
    private static byte[] toByteArray(String hex) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String[] toks = hex.split(" ");
        for (int i = 0; i < toks.length; i++)
            baos.write(Integer.parseInt(toks[i], 16));
        return baos.toByteArray();
    }
}