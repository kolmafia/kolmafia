/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core;

import java.io.UnsupportedEncodingException;
import java.io.Serializable;
import java.util.Arrays;


/**
 * The <b>SVNPropertyValue</b> represents an object wrapper for string and binary version controlled 
 * properties providing a set of specific methods to work with them. 
 *  
 * <p/>
 * Since version 1.2 the <code>SVNKit</code> library supports binary properties as well.
 * 
 * @author  TMate Software Ltd.
 * @version 1.3
 * @since   1.2
 */
public class SVNPropertyValue implements Serializable {

    private static final long serialVersionUID = 4845L;
    
    private String myValue;
    private byte[] myData;

    /**
     * Creates a new property value object from the given byte array.
     * 
     * <p>
     * This method is intended to instantiate binary property values. However if <code>propertyName</code> is of 
     * {@link SVNProperty#isSVNProperty(String) svn domain}, then it attempts 
     * to encode the passed bytes into a <code>String</code> value using the 
     * <span class="javastring">"UTF-8"</span> charset. Finally, the property value object is created via
     * a call to {@link #create(String)}. In this way the text nature of the property is automatically 
     * preserved to avoid binary/text properties mess.  
     * 
     * @param  propertyName  property name
     * @param  data          array containing property bytes
     * @param  offset        offset in <code>data</code> to copy bytes from
     * @param  length        amount of bytes to copy from <code>data</code>
     * @return               new property value object; <span class="javakeyword">null</span> if 
     *                       <code>data</code> is <span class="javakeyword">null</span>  
     * 
     */
    public static SVNPropertyValue create(String propertyName, byte[] data, int offset, int length) {
        if (data == null) {
            return null;
        }
        if (SVNProperty.isSVNProperty(propertyName)) {
            String value;
            try {
                value = new String(data, offset, length, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                value = new String(data, offset, length);
            }
            return new SVNPropertyValue(value);
        }
        return new SVNPropertyValue(data, offset, length);
    }

    /**
     * Creates a new property value object from the given byte array.
     * 
     * <p>
     * This method is equivalent to <code>create(propertyName, data, 0, data.length)</code>.
     *  
     * @param  propertyName  property name
     * @param  data          array containing property bytes
     * @return               new property value object; <span class="javakeyword">null</span> if 
     *                       <code>data</code> is <span class="javakeyword">null</span>  
     * @see                  #create(String, byte[], int, int)
     */
    public static SVNPropertyValue create(String propertyName, byte[] data) {
        if (data == null) {
            return null;
        }
        return create(propertyName, data, 0, data.length);
    }

    /**
     * Creates a new property value object representing a text property value.
     * 
     * <p/>
     * This method is intended to create text property values only.
     * 
     * @param  propertyValue text property value which is stored as is 
     * @return               new property value object; <span class="javakeyword">null</span> if 
     *                       <code>propertyValue</code> is <span class="javakeyword">null</span>  
     */
    public static SVNPropertyValue create(String propertyValue) {
        if (propertyValue == null){
            return null;            
        }
        return new SVNPropertyValue(propertyValue);
    }

    /**
     * Returns <code>byte[]</code> representation of <code>value</code>.
     * 
     * <p/>
     * If <code>value</code> is a {@link SVNPropertyValue#isString() string} property value, then bytes of 
     * the string are encoded using the <span class="javastring">"UTF-8"</span> charset and returned by this 
     * method. If encoding fails, then bytes are encoded using the default platform's charset.
     * 
     * <p/>
     * Otherwise, {@link SVNPropertyValue#getBytes()} is returned.
     * 
     * @param  value  property value object
     * @return        bytes of the property value represented by <code>value</code>; 
     *                <span class="javakeyword">null</span> if <code>value</code> is 
     *                <span class="javakeyword">null</span> 
     */
    public static byte[] getPropertyAsBytes(SVNPropertyValue value) {
        if (value == null) {
            return null;
        }
        if (value.isString()) {
            try {
                return value.getString().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                return value.getString().getBytes();
            }
        }
        return value.getBytes();
    }

    /**
     * Returns <code>String</code> representation of <code>value</code>.
     * 
     * <p/>
     * If <code>value</code> is a {@link SVNPropertyValue#isBinary() binary} property value, then its bytes are
     * converted to a <code>String</code> encoding them with the <span class="javastring">"UTF-8"</span> charset 
     * and returned back to the caller. If that encoding fails, bytes are encoded with the default platform's 
     * charset.
     * 
     * <p/>
     * Otherwise, {@link SVNPropertyValue#getString()} is returned.
     * 
     * @param  value property value object 
     * @return       string property value; <span class="javakeyword">null</span> if <code>value</code> is
     *               <span class="javakeyword">null</span>
     */
    public static String getPropertyAsString(SVNPropertyValue value) {
        if (value == null) {
            return null;
        }
        if (value.isBinary()) {
            try {
                return new String(value.getBytes(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return new String(value.getBytes());
            }
        }
        return value.getString();
    }

    /**
     * Says whether the property value wrapped by this object is binary or not.
     * @return  <span class="javakeyword">true</span> if binary, otherwise <span class="javakeyword">false</span>
     */
    public boolean isBinary() {
        return myData != null;
    }

    /**
     * Returns property value bytes.
     * 
     * <p/>
     * Note: this will be always <span class="javakeyword">null</span> for <code>String</code> property values. 
     * 
     * @return byte array with property value bytes
     */
    public byte[] getBytes() {
        return myData;
    }

    /**
     * Says whether the property value wrapped by this object is <code>String</code> or not.
     * @return  <span class="javakeyword">true</span> if textual, otherwise <span class="javakeyword">false</span>
     */
    public boolean isString() {
        return myValue != null;
    }

    /**
     * Returns property value string.
     * 
     * <p/>
     * Note: this will be always <span class="javakeyword">null</span> for binary property values. 
     * 
     * @return property value string
     */
    public String getString() {
        return myValue;
    }

    /**
     * Returns a string representation of this object.
     * 
     * <p/>
     * Note: irrelevant for binary properties.
     *  
     * @return string representation of this object 
     */
    public String toString() {
        if (isBinary()) {
            return "property is binary";
        }
        return getString();
    }

    /**
     * Says whether this object and <code>obj</code> are equal or not.
     * 
     * @param  obj  object to compare with 
     * @return      <span class="javakeyword">true</span> in the following cases: 
     *              <ul>
     *              <li/><code>obj</code> is the same as this one (by reference) 
     *              <li/>if <code>obj</code> is an <code>SVNPropertyValue</code> and either has got the same
     *              <code>String</code> value in case this object holds a <code>String</code> value, or 
     *              the same byte array contents if this object represents a binary property value
     *              </ul>         
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }

        if (obj instanceof SVNPropertyValue) {
            SVNPropertyValue value = (SVNPropertyValue) obj;
            if (isString()) {
                return myValue.equals(getPropertyAsString(value));
            } else if (isBinary()) {
                return Arrays.equals(myData, getPropertyAsBytes(value));
            }
        }
        return false;
    }

    /**
     * Returns the hash code for this object. If this object represents a {@link #isString() string} property,
     * then returns the hash code of the <code>String</code> object. Otherwise, this object represents 
     * a binary property and the hash code of the <code>byte[]</code> array is returned.
     * 
     * @return hash code
     */
    public int hashCode() {
        if (myValue != null) {
            return myValue.hashCode();
        }
        if (myData != null) {
            return myData.hashCode();
        }
        return super.hashCode();
    }

    private SVNPropertyValue(byte[] data, int offset, int length) {
        myData = new byte[length];
        System.arraycopy(data, offset, myData, 0, length);
    }

    private SVNPropertyValue(String propertyValue) {
        myValue = propertyValue;
    }

}