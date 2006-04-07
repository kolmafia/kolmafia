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

	public static ImageIcon getSharedImage( String filename )
	{
		String shareDirectory = System.getProperty( "SHARED_MODULE_DIRECTORY" );

		try
		{	return getImage( shareDirectory == null ? "" : shareDirectory, IMAGE_DIRECTORY, filename );
		}
		catch ( FileNotFoundException e )
		{
			System.out.println( e );
			e.printStackTrace();
			System.err.println( "Shared image <" + filename + "> could not be found" );
			return null;
		}
	}

	/**
	 * A public function used to retrieve an image.  Allows referencing images
	 * within a JAR, inside of a class tree, and from the local directory from
	 * which the Java command line is called.  The priority is as listed, in
	 * reverse order.
	 *
	 * @param	directory	the main subtree in which the image can be found, relative to the
	 *						system class loader
	 * @param	subdirectory	the subtree in which the image can be found
	 * @param	filename	the filename of the image
	 */

	public static ImageIcon getImage( String directory, String subdirectory, String filename )
		throws FileNotFoundException
	{
		if ( directory.length() > 0 && (!directory.endsWith( File.separator ) && !directory.endsWith( "/" )) )
			directory += File.separator;

		if ( subdirectory.length() > 0 && (!subdirectory.endsWith( File.separator ) && !subdirectory.endsWith( "/" )) )
			subdirectory += File.separator;

		File override = new File( subdirectory + filename );
		if ( override.exists() )
			return new ImageIcon( subdirectory + filename );

		java.net.URL filenameAsURL;

		String fullname = directory + subdirectory + filename;
		String fulljarname = fullname.replaceAll( File.separator.replaceAll( "\\\\", "\\\\\\\\" ), "/" );

		String halfname = subdirectory + filename;
		String halfjarname = halfname.replaceAll( File.separator.replaceAll( "\\\\", "\\\\\\\\" ), "/" );

		// attempt to retrieve the file from the system class tree (non-JAR)
		filenameAsURL = SYSTEM_CLASSLOADER.getResource( fullname );
		if ( filenameAsURL != null )
			return new ImageIcon( filenameAsURL );

		// attempt to retrieve the file from the system class tree (JAR)
		filenameAsURL = SYSTEM_CLASSLOADER.getResource( fulljarname );
		if ( filenameAsURL != null )
			return new ImageIcon( filenameAsURL );

		// attempt to retrieve the file from the system class tree (non-JAR)
		filenameAsURL = SYSTEM_CLASSLOADER.getResource( halfname );
		if ( filenameAsURL != null )
			return new ImageIcon( filenameAsURL );

		// attempt to retrieve the file from the system class tree (JAR)
		filenameAsURL = SYSTEM_CLASSLOADER.getResource( halfjarname );
		if ( filenameAsURL != null )
			return new ImageIcon( filenameAsURL );

		// attempt to retrieve the file from the Spellcast class tree (non-JAR)
		filenameAsURL = MAINCLASS_CLASSLOADER.getResource( fullname );
		if ( filenameAsURL != null )
			return new ImageIcon( filenameAsURL );

		// attempt to retrieve the file from the Spellcast class tree (JAR)
		filenameAsURL = MAINCLASS_CLASSLOADER.getResource( fulljarname );
		if ( filenameAsURL != null )
			return new ImageIcon( filenameAsURL );

		// attempt to retrieve the file from the Spellcast class tree (non-JAR)
		filenameAsURL = MAINCLASS_CLASSLOADER.getResource( halfname );
		if ( filenameAsURL != null )
			return new ImageIcon( filenameAsURL );

		// attempt to retrieve the file from the Spellcast class tree (JAR)
		filenameAsURL = MAINCLASS_CLASSLOADER.getResource( halfjarname );
		if ( filenameAsURL != null )
			return new ImageIcon( filenameAsURL );

		// if it's gotten this far, the image icon does not exist
		throw new FileNotFoundException( fullname );
	}
}