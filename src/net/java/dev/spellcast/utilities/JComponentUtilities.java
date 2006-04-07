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
import java.io.FileNotFoundException;
import java.net.URL;

// components
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JComboBox;
import javax.swing.JList;

/**
 * Formed after the same idea as <code>SwingUtilities</code>, this contains common
 * functions needed by many of the JComponent-related classes.  Any methods which
 * are used by multiple instances of a JComponent and have a non-class-specific
 * purpose should be placed into this class in order to simplify the overall design
 * of the system and to facilitate documentation.
 */

public class JComponentUtilities implements UtilityConstants
{
	/**
	 * Sets the minimum, maximum, and preferred size of the component to the
	 * given width and height, effectively forcing the component to the given
	 * width and height, unless restrictions to the component's size exist due
	 * to external reasons.
	 *
	 * @param	component	the component whose size is to be set
	 * @param	width	the new forced width of the component
	 * @param	height	the new forced height of the component
	 */

	public static void setComponentSize( JComponent component, int width, int height )
	{	setComponentSize( component, new Dimension( width, height ) );
	}

	/**
	 * Sets the minimum, maximum, and preferred size of the component to the
	 * given dimension, effectively forcing the component to the given width
	 * and height, unless restrictions to the component's size exist due to
	 * external reasons.
	 *
	 * @param	component	the component whose size is to be set
	 * @param	d	the new forced size of the component, as a <code>Dimension</code>
	 */

	public static void setComponentSize( JComponent component, Dimension d )
	{
		component.setPreferredSize( d );
		component.setMinimumSize( d );
		component.setSize( d );

		if ( component instanceof JTextField || component instanceof JPasswordField || component instanceof JList || component instanceof JComboBox )
			component.setMaximumSize( new Dimension( Integer.MAX_VALUE, (int) d.getHeight() ) );
		else
			component.setMaximumSize( d );
	}

	/**
	 * Creates a label with the given properties already preset.  The label will
	 * also, by default, be opaque, so that the background color specified will
	 * be able to show through.
	 *
	 * @param	label	the string to be displayed by the <code>JLabel</code>
	 * @param	alignment	the horizontal alignment of the <code>JLabel</code>
	 * @param	background	the background color to be used for the <code>JLabel</code>
	 * @param	foreground	the foreground color to be used for the <code>JLabel</code>
	 */

	public static JLabel createLabel( String label, int alignment, Color background, Color foreground )
	{
		JLabel presetLabel = new JLabel( label, alignment );
		presetLabel.setForeground( foreground );
		presetLabel.setBackground( background );
		presetLabel.setOpaque( true );
		return presetLabel;
	}

	/**
	 * A public function used to retrieve an image.  Allows referencing images
	 * within a JAR, inside of a class tree, and from the local directory from
	 * which the Java command line is called.  The priority is as listed, in
	 * reverse order.  Note that rather than throwing an exception should the
	 * file not be successfully found, this function will instead print out an
	 * error message and simply return null.
	 *
	 * @param	filename	the filename of the image
	 */

	public static ImageIcon getImage( String filename )
	{
		try
		{	return getImage( IMAGE_DIRECTORY, filename );
		}
		catch ( FileNotFoundException e )
		{
			System.out.println( e );
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * A public function used to retrieve an image.  Allows referencing images
	 * within a JAR, inside of a class tree, and from the local directory from
	 * which the Java command line is called.  The priority is as listed, in
	 * reverse order.
	 *
	 * @param	directory	the subtree in which the image can be found
	 * @param	filename	the filename of the image
	 */

	public static ImageIcon getImage( String directory, String filename )
		throws FileNotFoundException
	{
		if ( directory.length() > 0 && !directory.endsWith( File.separator ) && !directory.endsWith( "/" ) )
			directory += File.separator;

		File override = new File( directory + filename );
		if ( override.exists() )
			return new ImageIcon( directory + filename );

		ImageIcon result =  null;
		String fullname = directory + filename;
		String jarname = fullname.replaceAll( File.separator.replaceAll( "\\\\", "\\\\\\\\" ), "/" );

		result = getImage( SYSTEM_CLASSLOADER, fullname, jarname );
		if ( result != null )
			return result;

		result = getImage( MAINCLASS_CLASSLOADER, fullname, jarname );
		if ( result != null )
			return result;

		// if it's gotten this far, the image icon does not exist
		throw new FileNotFoundException( fullname );
	}
	
	private static ImageIcon getImage( ClassLoader loader, String filename, String jarname )
	{
		URL filenameAsURL = loader.getResource( filename );
		if ( filenameAsURL != null )
			return new ImageIcon( filenameAsURL );
		
		filenameAsURL = loader.getResource( jarname );
		if ( filenameAsURL != null )
			return new ImageIcon( filenameAsURL );
	
		return null;
	}
}