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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.IOException;

import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * Formed after the same idea as <code>SwingUtilities</code>, this contains common functions needed by many of the
 * JComponent-related classes. Any methods which are used by multiple instances of a JComponent and have a
 * non-class-specific purpose should be placed into this class in order to simplify the overall design of the system and
 * to facilitate documentation.
 */

public class JComponentUtilities
{
	/**
	 * Sets the minimum, maximum, and preferred size of the component to the given width and height, effectively forcing
	 * the component to the given width and height, unless restrictions to the component's size exist due to external
	 * reasons.
	 *
	 * @param component the component whose size is to be set
	 * @param width the new forced width of the component
	 * @param height the new forced height of the component
	 */

	public static void setComponentSize( final JComponent component, int width, int height )
	{
		if ( width == -1 )
		{
			width = component.getPreferredSize().width;
		}

		if ( height == -1 )
		{
			height = component.getPreferredSize().height;
		}

		Dimension d = new Dimension( width, height );
		component.setPreferredSize( d );
		component.setMinimumSize( d );
		component.setSize( d );

		if ( component instanceof JTextField || component instanceof JList || component instanceof JComboBox )
		{
			component.setMaximumSize( new Dimension( Integer.MAX_VALUE, height ) );
		}
		else
		{
			component.setMaximumSize( d );
		}
	}

	/**
	 * Sets the minimum, maximum, and preferred size of the component to the given dimension, effectively forcing the
	 * component to the given width and height, unless restrictions to the component's size exist due to external
	 * reasons.
	 *
	 * @param component the component whose size is to be set
	 * @param d the new forced size of the component, as a <code>Dimension</code>
	 */

	public static void setComponentSize( final JComponent component, final Dimension d )
	{
		JComponentUtilities.setComponentSize( component, d.width, d.height );
	}

	/**
	 * Creates a label with the given properties already preset. The label will also, by default, be opaque, so that the
	 * background color specified will be able to show through.
	 *
	 * @param label the string to be displayed by the <code>JLabel</code>
	 * @param alignment the horizontal alignment of the <code>JLabel</code>
	 * @param background the background color to be used for the <code>JLabel</code>
	 * @param foreground the foreground color to be used for the <code>JLabel</code>
	 */

	public static JLabel createLabel( final String label, final int alignment, final Color background,
		final Color foreground )
	{
		JLabel presetLabel = new JLabel( label, alignment );
		presetLabel.setForeground( foreground );
		presetLabel.setBackground( background );
		presetLabel.setOpaque( true );
		return presetLabel;
	}

	/**
	 * A public function used to retrieve an image. Allows referencing images within a JAR, inside of a class tree, and
	 * from the local directory from which the Java command line is called. The priority is as listed, in reverse order.
	 * Note that rather than throwing an exception should the file not be successfully found, this function will instead
	 * print out an error message and simply return null.
	 *
	 * @param filename the filename of the image
	 */

	public static ImageIcon getImage( final String filename )
	{
		return JComponentUtilities.getImage( UtilityConstants.IMAGE_DIRECTORY, filename );
	}

	/**
	 * A public function used to retrieve an image. Allows referencing images within a JAR, inside of a class tree, and
	 * from the local directory from which the Java command line is called. The priority is as listed, in reverse order.
	 *
	 * @param directory the subtree in which the image can be found
	 * @param filename the filename of the image
	 */

	public static ImageIcon getImage( String directory, String filename )
	{
		if ( filename.startsWith( "http://" ) )
		{
			return new ImageIcon( filename );
		}

		directory = directory.replaceAll( File.separator.replaceAll( "\\\\", "\\\\\\\\" ), "/" );
		filename = filename.replaceAll( File.separator.replaceAll( "\\\\", "\\\\\\\\" ), "/" );

		if ( directory.length() > 0 && !directory.endsWith( "/" ) )
		{
			directory += "/";
		}

		ImageIcon result = null;
		String fullname = directory + filename;

		File override = new File( UtilityConstants.ROOT_LOCATION, fullname );
		if ( override.exists() )
		{
			try
			{
				return new ImageIcon( override.getCanonicalPath() );
			}
			catch ( IOException e )
			{
			}
		}

		result = JComponentUtilities.getImage( UtilityConstants.SYSTEM_CLASSLOADER, fullname );
		if ( result != null )
		{
			return result;
		}

		result = JComponentUtilities.getImage( UtilityConstants.MAINCLASS_CLASSLOADER, fullname );
		if ( result != null )
		{
			return result;
		}

		// if it's gotten this far, the image icon does not exist
		return null;
	}

	public static URL getResource( final String filename )
	{
		return JComponentUtilities.getResource( UtilityConstants.IMAGE_DIRECTORY, filename );
	}

	public static URL getResource( String directory, String filename )
	{
		directory = directory.replaceAll( File.separator.replaceAll( "\\\\", "\\\\\\\\" ), "/" );
		filename = filename.replaceAll( File.separator.replaceAll( "\\\\", "\\\\\\\\" ), "/" );

		if ( directory.length() > 0 && !directory.endsWith( "/" ) )
		{
			directory += "/";
		}

		String fullname = directory + filename;

		try
		{
			File override = new File( UtilityConstants.ROOT_LOCATION, fullname );
			if ( override.exists() )
			{
				return override.toURI().toURL();
			}
		}
		catch ( Exception e )
		{
			// It's a malformed filename.  Just continue on
			// with the next possibility.
		}

		URL result = JComponentUtilities.getResource( UtilityConstants.SYSTEM_CLASSLOADER, fullname );
		if ( result != null )
		{
			return result;
		}

		result = JComponentUtilities.getResource( UtilityConstants.MAINCLASS_CLASSLOADER, fullname );
		if ( result != null )
		{
			return result;
		}

		// if it's gotten this far, the image icon does not exist
		return null;
	}

	private static URL getResource( final ClassLoader loader, final String filename )
	{
		return loader.getResource( filename );
	}

	private static ImageIcon getImage( final ClassLoader loader, final String filename )
	{
		URL filenameAsURL = loader.getResource( filename );
		return filenameAsURL == null ? null : new ImageIcon( filenameAsURL );
	}

	public static void addHotKey( final JComponent component, final int event, final ActionListener listener )
	{
		JComponentUtilities.addHotKey( component, event, 0, listener );
	}

	public static void addHotKey( final JComponent component, final int event, final int modifier,
		final ActionListener listener )
	{
		component.registerKeyboardAction(
			listener, KeyStroke.getKeyStroke( event, modifier ), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
	}

	public static void addGlobalHotKey( final JComponent component, final int event, final ActionListener listener )
	{
		JComponentUtilities.addGlobalHotKey( component, event, 0, listener );
	}

	public static void addGlobalHotKey( final JComponent component, final int event, final int modifier,
		final ActionListener listener )
	{
		component.registerKeyboardAction(
			listener, KeyStroke.getKeyStroke( event, modifier ), JComponent.WHEN_IN_FOCUSED_WINDOW );
	}
}
