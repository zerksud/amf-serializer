/*
 * www.openamf.org
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package com.exadel.flamingo.flex.messaging.amf.io;

import com.exadel.flamingo.flex.amf.AMF0Body;
import com.exadel.flamingo.flex.amf.AMF0Header;
import com.exadel.flamingo.flex.amf.AMF0Message;
import com.exadel.flamingo.flex.amf.AMF3Object;
import flex.messaging.io.ASObject;
import flex.messaging.io.ASRecordSet;
import org.apache.commons.beanutils.PropertyUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * AMF Serializer
 *
 * @author Jason Calabrese <jasonc@missionvi.com>
 * @author Pat Maddox <pergesu@users.sourceforge.net>
 * @author Sylwester Lachiewicz <lachiewicz@plusnet.pl>
 * @author Richard Pitt
 *
 * @version $Revision: 1.54 $, $Date: 2006/03/25 23:41:41 $
 */
public class AMF0Serializer {

    private static final Log log = LogFactory.getLog(AMF0Serializer.class);

    private static final int MILLS_PER_HOUR = 60000;
    /**
     * Null message
     */
    private static final String NULL_MESSAGE = "null";

    /**
     * The output stream
     */
    protected DataOutputStream outputStream;

    private Map<Object, Integer> storedObjects = new IdentityHashMap<Object, Integer>();
    private int storedObjectCount = 0;

    /**
     * Constructor
     *
     * @param outputStream
     */
    public AMF0Serializer(DataOutputStream outputStream) {
        this.outputStream = outputStream;
    }
    /**
     * Writes message
     *
     * @param message
     * @throws java.io.IOException
     */
    public void serializeMessage(AMF0Message message) throws IOException {
        //if (log.isInfoEnabled())
        //    log.info("Serializing Message, for more info turn on debug level");

        clearStoredObjects();
        outputStream.writeShort(message.getVersion());
        // write header
        outputStream.writeShort(message.getHeaderCount());
        Iterator<AMF0Header> headers = message.getHeaders().iterator();
        while (headers.hasNext()) {
            AMF0Header header = headers.next();
            writeHeader(header);
        }
        // write body
        outputStream.writeShort(message.getBodyCount());
        Iterator<AMF0Body> bodies = message.getBodies();
        while (bodies.hasNext()) {
            AMF0Body body = bodies.next();
            writeBody(body);
        }
    }
    /**
     * Writes message header
     *
     * @param header AMF message header
     * @throws java.io.IOException
     */
    protected void writeHeader(AMF0Header header) throws IOException {
        outputStream.writeUTF(header.getKey());
        outputStream.writeBoolean(header.isRequired());
        // Always, always there is four bytes of FF, which is -1 of course
        outputStream.writeInt(-1);
        writeData(header.getValue());
    }
    /**
     * Writes message body
     *
     * @param body AMF message body
     * @throws java.io.IOException
     */
    protected void writeBody(AMF0Body body) throws IOException {
        // write url
        if (body.getTarget() == null) {
            outputStream.writeUTF(NULL_MESSAGE);
        } else {
            outputStream.writeUTF(body.getTarget());
        }
        // write response
        if (body.getResponse() == null) {
            outputStream.writeUTF(NULL_MESSAGE);
        } else {
            outputStream.writeUTF(body.getResponse());
        }
        // Always, always there is four bytes of FF, which is -1 of course
        outputStream.writeInt(-1);
        // Write the data to the output stream
        writeData(body.getValue());
    }

    /**
     * Writes Data
     *
     * @param value
     * @throws java.io.IOException
     */
    protected void writeData(Object value) throws IOException {
        if (value == null) {
            // write null object
            outputStream.writeByte(AMF0Body.DATA_TYPE_NULL);
        } else if (value instanceof AMF3Object) {
            writeAMF3Data((AMF3Object)value);
        } else if (isPrimitiveArray(value)) {
            writePrimitiveArray(value);
        } else if (value instanceof Number) {
            // write number object
            outputStream.writeByte(AMF0Body.DATA_TYPE_NUMBER);
            outputStream.writeDouble(((Number) value).doubleValue());
        } else if (value instanceof String) {
           writeString((String)value);
        } else if (value instanceof Character) {
            // write String object
            outputStream.writeByte(AMF0Body.DATA_TYPE_STRING);
            outputStream.writeUTF(value.toString());
        } else if (value instanceof Boolean) {
            // write boolean object
            outputStream.writeByte(AMF0Body.DATA_TYPE_BOOLEAN);
            outputStream.writeBoolean(((Boolean) value).booleanValue());
        } else if (value instanceof Date) {
            // write Date object
            outputStream.writeByte(AMF0Body.DATA_TYPE_DATE);
            outputStream.writeDouble(((Date) value).getTime());
            int offset = TimeZone.getDefault().getRawOffset();
            outputStream.writeShort(offset / MILLS_PER_HOUR);
        } else {

            if (storedObjects.containsKey(value)) {
                writeStoredObject(value);
                return;
            }
            storeObject(value);

            if (value instanceof Object[]) {
                // write Object Array
                writeArray((Object[]) value);
            } else if (value instanceof Iterator) {
                write((Iterator<?>) value);
            } else if (value instanceof Collection) {
                write((Collection<?>) value);
            } else if (value instanceof Map) {
                writeMap((Map<?, ?>) value);
            } else if (value instanceof ResultSet) {
                ASRecordSet asRecordSet = new ASRecordSet();
                asRecordSet.populate((ResultSet) value);
                writeData(asRecordSet);
            } else if (value instanceof Document) {
                write((Document) value);
            } else {
                /*
                MM's gateway requires all objects to be marked with the
                Serializable interface in order to be serialized
                That should still be followed if possible, but there is
                no good reason to enforce it.
                */
                writeObject(value);
            }
        }
    }

    /**
     * Writes Object
     *
     * @param object
     * @throws java.io.IOException
     */
    protected void writeObject(Object object) throws IOException {
        if (log.isDebugEnabled())
        {
            if (object == null)
            {
                log.debug("Writing object, object param == null");
            }
            else
            {
                log.debug("Writing object, class = " + object.getClass());
            }
        }
        //String customClassName = null;
        /*
            OpenAMFConfig.getInstance().getCustomClassName(
                object.getClass().getName());
        */
        //if (customClassName == null) {
            outputStream.writeByte(AMF0Body.DATA_TYPE_OBJECT);
        /*} else {
            if (log.isDebugEnabled()) {
                log.debug("customClassName : " + customClassName);
            }
            outputStream.writeByte(AMF0Body.DATA_TYPE_CUSTOM_CLASS);
            outputStream.writeUTF(customClassName);
        }*/
        try {
            PropertyDescriptor[] properties =
                PropertyUtils.getPropertyDescriptors(object);
            for (int i = 0; i < properties.length; i++) {
                if (!properties[i].getName().equals("class")) {
                    String propertyName = properties[i].getName();
                    Method readMethod = properties[i].getReadMethod();
                    Object propertyValue = null;
                    if (readMethod == null) {
                        log.error(
                            "unable to find readMethod for : "
                                + propertyName
                                + " writing null!");
                    } else {
                        log.debug("invoking readMethod " + readMethod);
                        propertyValue =
                            readMethod.invoke(object, new Object[0]);
                    }
                    log.debug(propertyName + " = " + propertyValue);
                    outputStream.writeUTF(propertyName);
                    writeData(propertyValue);
                }
            }
            outputStream.writeShort(0);
            outputStream.writeByte(AMF0Body.DATA_TYPE_OBJECT_END);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error(e, e);
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Writes Array Object - call <code>writeData</code> foreach element
     *
     * @param array
     * @throws java.io.IOException
     */
    protected void writeArray(Object[] array) throws IOException {
        outputStream.writeByte(AMF0Body.DATA_TYPE_ARRAY);
        outputStream.writeInt(array.length);
        for (int i = 0; i < array.length; i++) {
            writeData(array[i]);
        }
    }

    protected void writePrimitiveArray(Object array) throws IOException {
        writeArray(convertPrimitiveArrayToObjectArray(array));
    }

    protected Object[] convertPrimitiveArrayToObjectArray(Object array) {
        Class<?> componentType = array.getClass().getComponentType();

        Object[] result = null;

        if (componentType == null)
        {
            throw new NullPointerException("componentType is null");
        }
        else if (componentType == Character.TYPE)
        {
            char[] carray = (char[]) array;
            result = new Object[carray.length];
            for (int i = 0; i < carray.length; i++)
            {
                result[i] = new Character(carray[i]);
            }
        }
        else if (componentType == Byte.TYPE)
        {
            byte[] barray = (byte[]) array;
            result = new Object[barray.length];
            for (int i = 0; i < barray.length; i++)
            {
                result[i] = new Byte(barray[i]);
            }
        }
        else if (componentType == Short.TYPE)
        {
            short[] sarray = (short[]) array;
            result = new Object[sarray.length];
            for (int i = 0; i < sarray.length; i++)
            {
                result[i] = new Short(sarray[i]);
            }
        }
        else if (componentType == Integer.TYPE)
        {
            int[] iarray = (int[]) array;
            result = new Object[iarray.length];
            for (int i = 0; i < iarray.length; i++)
            {
                result[i] = Integer.valueOf(iarray[i]);
            }
        }
        else if (componentType == Long.TYPE)
        {
            long[] larray = (long[]) array;
            result = new Object[larray.length];
            for (int i = 0; i < larray.length; i++)
            {
                result[i] = new Long(larray[i]);
            }
        }
        else if (componentType == Double.TYPE)
        {
            double[] darray = (double[]) array;
            result = new Object[darray.length];
            for (int i = 0; i < darray.length; i++)
            {
                result[i] = new Double(darray[i]);
            }
        }
        else if (componentType == Float.TYPE)
        {
            float[] farray = (float[]) array;
            result = new Object[farray.length];
            for (int i = 0; i < farray.length; i++)
            {
                result[i] = new Float(farray[i]);
            }
        }
        else if (componentType == Boolean.TYPE)
        {
            boolean[] barray = (boolean[]) array;
            result = new Object[barray.length];
            for (int i = 0; i < barray.length; i++)
            {
                result[i] = new Boolean(barray[i]);
            }
        }
        else {
            throw new IllegalArgumentException(
                    "unexpected component type: "
                    + componentType.getClass().getName());
        }

        return result;
    }

    /**
     * Writes Iterator - convert to List and call <code>writeCollection</code>
     *
     * @param iterator Iterator
     * @throws java.io.IOException
     */
    protected void write(Iterator<?> iterator) throws IOException {
        List<Object> list = new ArrayList<Object>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        write(list);
    }
    /**
     * Writes collection
     *
     * @param collection Collection
     * @throws java.io.IOException
     */
    protected void write(Collection<?> collection) throws IOException {
        outputStream.writeByte(AMF0Body.DATA_TYPE_ARRAY);
        outputStream.writeInt(collection.size());
        for (Iterator<?> objects = collection.iterator(); objects.hasNext();) {
            Object object = objects.next();
            writeData(object);
        }
    }
    /**
     * Writes Object Map
     *
     * @param map
     * @throws java.io.IOException
     */
    protected void writeMap(Map<?, ?> map) throws IOException {
        if (map instanceof ASObject && ((ASObject) map).getType() != null) {
            if (log.isDebugEnabled())
                log.debug(
                    "Writing Custom Class: " + ((ASObject) map).getType());
            outputStream.writeByte(AMF0Body.DATA_TYPE_CUSTOM_CLASS);
            outputStream.writeUTF(((ASObject) map).getType());
        } else {
            if (log.isDebugEnabled())
            {
                log.debug("Writing Map");
            }
            outputStream.writeByte(AMF0Body.DATA_TYPE_MIXED_ARRAY);
            outputStream.writeInt(0);
        }
        for (Iterator<?> entrys = map.entrySet().iterator(); entrys.hasNext();) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>)entrys.next();
            log.debug(entry.getKey() + ": " + entry.getValue());
            outputStream.writeUTF(entry.getKey().toString());
            writeData(entry.getValue());
        }
        outputStream.writeShort(0);
        outputStream.writeByte(AMF0Body.DATA_TYPE_OBJECT_END);
    }

    /**
     * Writes XML Document
     *
     * @param document
     * @throws java.io.IOException
     */
    protected void write(Document document) throws IOException {
        outputStream.writeByte(AMF0Body.DATA_TYPE_XML);
        Element docElement = document.getDocumentElement();
        String xmlData = convertDOMToString(docElement);
        if (log.isDebugEnabled())
            log.debug("Writing xmlData: \n" + xmlData);
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        baOutputStream.write(xmlData.getBytes("UTF-8"));
        outputStream.writeInt(baOutputStream.size());
        baOutputStream.writeTo(outputStream);
    }

  /**
   * Most of this code was cribbed from Java's DataOutputStream.writeUTF method
   * which only supports Strings <= 65535 UTF-encoded characters.
   */
  protected int writeString(String str)
   throws IOException {
    int strlen = str.length();
    int utflen = 0;
    char[] charr = new char[strlen];
    int c, count = 0;

    str.getChars(0, strlen, charr, 0);

    // check the length of the UTF-encoded string
    for (int i = 0; i < strlen; i++) {
      c = charr[i];
      if ((c >= 0x0001) && (c <= 0x007F)) {
        utflen++;
      } else if (c > 0x07FF) {
        utflen += 3;
      } else {
        utflen += 2;
      }
    }

    /**
     * if utf-encoded String is < 64K, use the "String" data type, with a
     * two-byte prefix specifying string length; otherwise use the "Long String"
     * data type, withBUG#298 a four-byte prefix
     */
    byte[] bytearr;
    if (utflen <= 65535) {
      outputStream.writeByte(AMF0Body.DATA_TYPE_STRING);
      bytearr = new byte[utflen+2];
    } else {
      outputStream.writeByte(AMF0Body.DATA_TYPE_LONG_STRING);
      bytearr = new byte[utflen+4];
      bytearr[count++] = (byte) ((utflen >>> 24) & 0xFF);
      bytearr[count++] = (byte) ((utflen >>> 16) & 0xFF);
    }

    bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
    bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);
    for (int i = 0; i < strlen; i++) {
      c = charr[i];
      if ((c >= 0x0001) && (c <= 0x007F)) {
        bytearr[count++] = (byte) c;
      } else if (c > 0x07FF) {
        bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
        bytearr[count++] = (byte) (0x80 | ((c >>  6) & 0x3F));
        bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
      } else {
        bytearr[count++] = (byte) (0xC0 | ((c >>  6) & 0x1F));
        bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
      }
    }

    outputStream.write(bytearr);
    return utflen + 2;
  }

    private void writeStoredObject(Object obj) throws IOException {
        if (log.isDebugEnabled())
            log.debug("Writing object reference for " + obj);
        outputStream.write(AMF0Body.DATA_TYPE_REFERENCE_OBJECT);
        outputStream.writeShort((storedObjects.get(obj)).intValue());
    }

    private void storeObject(Object obj) {
        storedObjects.put(obj, Integer.valueOf(storedObjectCount++));
    }

    private void clearStoredObjects() {
        storedObjects = new IdentityHashMap<Object, Integer>();
        storedObjectCount = 0;
    }

    protected boolean isPrimitiveArray(Object obj) {
        if (obj == null)
            return false;
        return obj.getClass().isArray() && obj.getClass().getComponentType().isPrimitive();
    }

    private void writeAMF3Data(AMF3Object data) throws IOException {
        outputStream.writeByte(AMF0Body.DATA_TYPE_AMF3_OBJECT);
        //ObjectOutput amf3 = GraniteContext.getCurrentInstance().getGraniteConfig().newAMF3Serializer(outputStream);
        ObjectOutput amf3 = new AMF3Serializer(outputStream);
        amf3.writeObject(data.getValue());
    }

    public static String convertDOMToString(Node node) {
        StringBuffer sb = new StringBuffer();
        if (node.getNodeType() == Node.TEXT_NODE) {
            sb.append(node.getNodeValue());
        } else {
            String currentTag = node.getNodeName();
            sb.append('<');
            sb.append(currentTag);
            appendAttributes(node, sb);
            sb.append('>');
            if (node.getNodeValue() != null) {
                sb.append(node.getNodeValue());
            }

            appendChildren(node, sb);

            appendEndTag(sb, currentTag);
        }
        return sb.toString();
    }

    private static void appendAttributes(Node node, StringBuffer sb) {
        if (node instanceof Element) {
            NamedNodeMap nodeMap = node.getAttributes();
            for (int i = 0; i < nodeMap.getLength(); i++) {
                sb.append(' ');
                sb.append(nodeMap.item(i).getNodeName());
                sb.append('=');
                sb.append('"');
                sb.append(nodeMap.item(i).getNodeValue());
                sb.append('"');
            }
        }
    }

    private static void appendChildren(Node node, StringBuffer sb) {
        if (node.hasChildNodes()) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                sb.append(convertDOMToString(children.item(i)));
            }
        }
    }

    private static void appendEndTag(StringBuffer sb, String currentTag) {
        sb.append('<');
        sb.append('/');
        sb.append(currentTag);
        sb.append('>');
    }
}