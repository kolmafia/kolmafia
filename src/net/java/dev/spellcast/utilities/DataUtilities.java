/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "Spellcast development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.java.dev.spellcast.utilities;

// layout
import java.awt.Color;
import java.awt.Dimension;

// file-related I/O
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

// readers
import java.io.InputStreamReader;
import java.io.BufferedReader;

// Java utilities
import java.util.StringTokenizer;
import java.math.BigInteger;

/**
 * Formed after the same idea as <code>SwingUtilities</code>, this contains common
 * functions needed by many of the data-related classes.  Any methods which
 * are used by multiple instances of a JComponent and have a non-class-specific
 * purpose should be placed into this class in order to simplify the overall design
 * of the system and to facilitate documentation.
 */

public class DataUtilities implements UtilityConstants
{
	/**
	 * A public function used to retrieve the reader for a file.  Allows the
	 * referencing of files contained within a JAR, inside of a class tree,
	 * and from the local directory from which the Java command line is called.
	 * The priority is as listed, in reverse order.  Note that rather than
	 * throwing an exception should the file not be successfully found, this
	 * function will instead print out an error message and simply return null.
	 *
	 * @param	filename	the name of the file to be retrieved
	 */

	public static BufferedReader getReader( String filename )
	{	return getReader( DATA_DIRECTORY, filename );
	}
	
	/**
	 * A public function used to retrieve the reader for a file.  Allows the
	 * referencing of files contained within a JAR, inside of a class tree,
	 * and from the local directory from which the Java command line is called.
	 * The priority is as listed, in reverse order.  Note that rather than
	 * throwing an exception should the file not be successfully found, this
	 * function will instead print out an error message and simply return null.
	 *
	 * @param	subdirectory	the subdirectory of the file
	 * @param	filename	the name of the file to be retrieved
	 */

	public static BufferedReader getReader( String directory, String filename )
	{
		try
		{
			return new BufferedReader( new InputStreamReader( getInputStream( directory, filename ) ) );
		}
		catch ( FileNotFoundException e )
		{
			System.out.println( e );
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * A public function used to retrieve the input stream, given a filename.
	 * Allows referencing images within a JAR, inside of a class tree, and
	 * from the local directory from which the Java command line is called.
	 * The priority is as listed, in reverse order.
	 *
	 * @param	directory	the subtree in which the file can be found
	 * @param	filename	the name of the file to be retrieved
	 */

	public static InputStream getInputStream( String directory, String filename )
		throws FileNotFoundException
	{
		if ( directory.length() > 0 && !directory.endsWith( File.separator ) && !directory.endsWith( "/" ) )
			directory += File.separator;

		InputStream locationAsInputStream;

		String fullname = directory + filename;
		String jarname = fullname.replaceAll( File.separator.replaceAll( "\\\\", "\\\\\\\\" ), "/" );
		
		locationAsInputStream = getInputStream( SYSTEM_CLASSLOADER, fullname, jarname );
		if ( locationAsInputStream != null )
			return locationAsInputStream;

		locationAsInputStream = getInputStream( MAINCLASS_CLASSLOADER, fullname, jarname );
		if ( locationAsInputStream != null )
			return locationAsInputStream;

		// if it's gotten this far, the file does not exist
		throw new FileNotFoundException( fullname );
	}
	
	private static InputStream getInputStream( ClassLoader loader, String filename, String jarname )
	{
		InputStream locationAsInputStream = loader.getResourceAsStream( filename );
		if ( locationAsInputStream != null )
			return locationAsInputStream;

		// attempt to retrieve the file from the system class tree (JAR)
		locationAsInputStream = loader.getResourceAsStream( jarname );
		if ( locationAsInputStream != null )
			return locationAsInputStream;

		return null;
	}

	/**
	 * In a lot of HTML documents and still others, colors are represented using
	 * the RGB values, concatenated as hexadecimal strings.  This function is
	 * used to create that hexadecimal string from a color object.  Note that the
	 * traditional pound symbol will also be part of the string.
	 *
	 * @param	c	The color to be translated to a hexadecimal string.
	 * @return	The hexadecimal string representation of the color
	 */

	public static String toHexString( Color c )
	{
		if ( c == null )
			return "#000000";

		int r = c.getRed(), g = c.getGreen(), b = c.getBlue();
		StringBuffer hexString = new StringBuffer( 7 );
		hexString.append( '#' );  int bitmask = (1 << 24) - 1;
		hexString.append( toHexString( c.getRGB() & bitmask, 6 ) );
		return hexString.toString();
	}

	/**
	 * In a lot of HTML documents and still others, colors are represented using
	 * the RGB values, concatenated as hexadecimal strings.  This function is
	 * used to create a color object from such a hexadecimal string.
	 *
	 * @param	hexString	The hexadecimal string (with # prefix) to be translated to a color
	 * @return	The color represented by this hexadecimal string
	 */

	public static Color toColor( String hexString )
	{
		String r = hexString.substring( 1, 3 );
		String g = hexString.substring( 3, 5 );
		String b = hexString.substring( 5 );

		return new Color( (new BigInteger( r, 16 )).intValue(), (new BigInteger( g, 16 )).intValue(), (new BigInteger( b, 16 )).intValue() );
	}

	/**
	 * The static <code>getHexString()</code> method found in the <code>Long</code>
	 * class has the advantage of converting a value to a hexadecimal string.  However,
	 * it does not fix this hexadecimal string to any length.  Thus, the purpose of this
	 * function is to convert a given long to a zero-filled hexadecimal string of
	 * given length.  Note, however, that negative values and values which cannot be
	 * represented with the given number of hexadecimal digits will cause an exception
	 * to be thrown.
	 *
	 * @param	value	The value to convert to hexadecimal
	 * @param	digitCount	The number of digits to use
	 * @return	The hexadecimal string representation of the given value, set to the given length
	 */

	public static String toHexString( long value, int digitCount )
	{
		if ( value < 0 )
			throw new IllegalArgumentException( "This function cannot convert negative values" );
		String hexString = Long.toHexString( value );
		if ( hexString.length() > digitCount )
			throw new IllegalArgumentException( value + " cannot be represented in " + digitCount + " hexadecimal digits" );

		StringBuffer hexBuffer = new StringBuffer( digitCount );
		int zeroesToAdd = digitCount - hexString.length();
		for ( int i = 0; i < zeroesToAdd; ++i )
			hexBuffer.append( 0 );
		hexBuffer.append( hexString );
		return hexBuffer.toString();
	}

	/**
	 * A method to convert a given text string to its HTML equivalent.  This method
	 * is used primarily by the <code>ChatBuffer</code> component to translate the
	 * plain text values returned by the <code>getChatDisplayForm()</code> to values
	 * that can be displayed in a <code>JEditorPane</code> set to display HTML.
	 *
	 * @param	plainText	The plain text to be converted to HTML
	 * @return	The HTML representation of the plain text.
	 */

	public static String convertToHTML( String plainText )
	{
		if ( plainText == null || plainText.length() == 0 )
			return "";

		String html = plainText;
		html = html.replaceAll( "&", "&amp;" );  // first replace all ampersands
		html = html.replaceAll( "<", "&lt;" );   // then replace all < symbols

		// finally, replace all the new lines with the <br> tag
		html = html.replaceAll( System.getProperty( "line.separator" ),
			"<br>" + System.getProperty( "line.separator" ) );

		// the conversion to HTML is complete
		return html;
	}
}