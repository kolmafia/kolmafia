/**
 * Copyright (c) 2005, KoLmafia development team
 * http://kolmafia.sourceforge.net/
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
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

package net.sourceforge.kolmafia;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.DefaultListCellRenderer;

import java.io.File;
import java.io.PrintStream;

import java.util.List;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Date;
import java.text.DateFormat;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * Holder for the BuffBot log (which should survive outside of
 * the BuffBot frame)
 */

public class BuffBotHome extends StaticEntity
{
	private static final DateFormat TIMESTAMP_FORMAT = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT );

	public static Color NOCOLOR = new Color( 0, 0, 0 );
	public static Color ERRORCOLOR = new Color( 128, 0, 0 );
	public static Color NONBUFFCOLOR = new Color( 0, 0, 128 );
	public static Color BUFFCOLOR = new Color( 0, 128, 0 );

	private static boolean isActive;

	private static TreeMap pastRecipients = new TreeMap();
	private static LockableListModel messages = new LockableListModel();
	private static PrintStream textLogStream = System.out;
	private static PrintStream hypertextLogStream = System.out;

	private static BuffBotFrame frame;

	/**
	 * Constructs a new <code>BuffBotHome</code>.  However, note that this
	 * does not automatically translate into the messages being displayed;
	 * until a chat display is set, this buffer merely stores the message
	 * content to be displayed.
	 */

	public static void reset()
	{
		messages.clear();
		pastRecipients.clear();

		// Create the text log file which shows only the buffs
		// which have been requested in a comma-delimited format.

		textLogStream = getPrintStream( ".log" );

		// Create the standard HTML log which can be opened
		// up to see all activity.

		hypertextLogStream = getPrintStream( ".html" );
		hypertextLogStream.println( "<html><head><style> body { font-family: sans-serif; } </style>" );
		hypertextLogStream.flush();
	}

	/**
	 * Retrieves the file which would be associated with the current player,
	 * placed in the given folder and given the appropriate extension.
	 */

	private static final File getFile( String extension )
	{
		String dayOfYear = DATED_FILENAME_FORMAT.format( new Date() );
		return new File( "buffs/" + KoLCharacter.baseUserName() + "_" + dayOfYear + extension );
	}

	/**
	 * Retrieves the output stream which would be associated with the current
	 * player, placed in the given folder and given the appropriate extension.
	 */

	private static final PrintStream getPrintStream( String extension )
	{
		File output = getFile( extension );

		try
		{
			output.getParentFile().mkdirs();
			return new LogStream( output );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e, "Failed to open <" + output.getAbsolutePath() + "> for output" );
			return null;
		}
	}

	/**
	 * Retrieves all the past recipients of the buff associated with the
	 * given meat amount.
	 */

	private static List getPastRecipients( int meatSent )
	{
		// First, check to see if a rollover has occurred since the last
		// time you updated the past recipients list.

		int today = MoonPhaseDatabase.getPhaseStep();
		if ( StaticEntity.getIntegerProperty( "lastBuffbotRun" ) != today )
		{
			StaticEntity.setProperty( "lastBuffbotRun", String.valueOf( today ) );
			pastRecipients.clear();
		}

		Integer key = new Integer( meatSent );
		if ( !pastRecipients.containsKey( key ) )
			pastRecipients.put( key, new SortedListModel() );

		return (List) pastRecipients.get( key );
	}

	/**
	 * Returns the number of times the given name has requested the buff
	 * associated with the given meat amount.
	 */

	public static int getInstanceCount( int meatSent, String name )
	{
		List pastRecipients = getPastRecipients( meatSent );
		BuffRecord record = new BuffRecord( name );

		int index = pastRecipients.indexOf( record );
		return index == -1 ? 0 : ((BuffRecord) pastRecipients.get(index)).getCount();
	}

	private static class BuffRecord implements Comparable
	{
		private int count;
		private String name;

		public BuffRecord( String name )
		{
			this.name = name;
			this.count = 1;
		}

		public int getCount()
		{	return count;
		}

		public void incrementCount()
		{	++count;
		}

		public int compareTo( Object o )
		{	return name.compareToIgnoreCase( ((BuffRecord)o).name );
		}

		public boolean equals( Object o )
		{	return name.equalsIgnoreCase( ((BuffRecord)o).name );
		}
	}

	/**
	 * Registers the given name as a recipient of the buff associated
	 * with the given meat amount.
	 */

	public static void addToRecipientList( int meatSent, String name )
	{
		List pastRecipients = getPastRecipients( meatSent );
		BuffRecord record = new BuffRecord( name );

		int index = pastRecipients.indexOf( record );
		if ( index == -1 )
			pastRecipients.add( record );
		else
			((BuffRecord) pastRecipients.get(index)).incrementCount();
	}

	/**
	 * Closes the log file used to actively record messages that are
	 * being stored in the buffer.  This formally closes the file and
	 * sets the log file currently being used to null so that no future
	 * updates are attempted.
	 */

	public static void deinitialize()
	{
		hypertextLogStream.println();
		hypertextLogStream.println();
		hypertextLogStream.println( "</body></html>" );

		hypertextLogStream.close();
		hypertextLogStream = null;
		isActive = false;
	}

	/**
	 * An internal function used to indicate that something has changed with
	 * regards to the buffbot.  This method is used whenever no timestamp
	 * is required for a given buffbot entry.
	 */

	public static void update( Color c, String entry )
	{
		if ( entry != null && hypertextLogStream != null )
		{
			messages.add( 0, new BuffMessage( c, entry ) );
			hypertextLogStream.println( "<br><font color=" + DataUtilities.toHexString( c ) + ">" + entry + "</font>" );
			hypertextLogStream.flush();

			KoLmafiaCLI.printLine( entry );
			if ( messages.size() > 100 )
				messages.remove( 100 );
		}
	}

	/**
	 * Adds a time-stamped entry to the log for the buffbot.  In general,
	 * this is the preferred method of modifying the buffbot.  However,
	 * the standard appending procedure is still valid.
	 */

	public static void timeStampedLogEntry( Color c, String entry )
	{	update( c, TIMESTAMP_FORMAT.format( new Date() ) + ": " + entry );
	}

	/**
	 * Adds the given buff to the comma-delimited list of events for the
	 * buffbot.  This is used to register whenever a buff has been requested
	 * and successfully processed.
	 */

	public static void recordBuff( String name, String buff, int casts, int meatSent )
	{
		textLogStream.println( TIMESTAMP_FORMAT.format( new Date() ) + "," + name + "," +
			KoLmafia.getPlayerId( name ) + "," + buff + "," + casts + "," + meatSent );
	}

	/**
	 * Sets the frame that should be updated whenever a status
	 * message arrives.
	 */

	public static void setFrame( BuffBotFrame frame )
	{	BuffBotHome.frame = frame;
	}

	/**
	 * An internal function used to display changes to system status
	 * while the buffbot is running.
	 */

	public static void updateStatus( String statusMessage )
	{
		if ( frame != null )
			frame.setTitle( VERSION_NAME + ": Buffbot - " + statusMessage );
	}

	/**
	 * Sets the current active state for the buffbot.  Note that
	 * this does not affect whether or not the buffbot continues
	 * logging events - it merely affects whether or not the the
	 * buffbot itself is running.
	 */

	public static void setBuffBotActive( boolean isActive )
	{	BuffBotHome.isActive = isActive;
	}

	/**
	 * Returns whether or not the buffbot is currently active.
	 * Note that this does not say whether or not the buffbot
	 * is currently logging data - int only states whether or
	 * not the buffbot itself is running.
	 */

	public static boolean isBuffBotActive()
	{	return isActive;
	}

	/**
	 * Used to retrieve the list of messages being updated by this
	 * <code>BuffBotHome</code>.  This should only be used if there
	 * is a need to display the messages in some list form.
	 */

	public static LockableListModel getMessages()
	{	return messages;
	}

	/**
	 * Returns an instance of the cell renderer which should be used
	 * to display the buff messages inside of a list setting.
	 */

	public static DefaultListCellRenderer getMessageRenderer()
	{	return new BuffMessageRenderer();
	}

	/**
	 * An internal class which represents the renderer which should
	 * be used to display the buff messages.
	 */

	private static class BuffMessageRenderer extends DefaultListCellRenderer
	{
		public BuffMessageRenderer()
		{	setOpaque( true );
		}

		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
		{
			Component defaultComponent = super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null || !(value instanceof BuffMessage) )
				return defaultComponent;

			BuffMessage bm = (BuffMessage) value;
			((JLabel)defaultComponent).setText( bm.message );
			defaultComponent.setForeground( bm.c );
			return defaultComponent;
		}
	}

	/**
	 * An internal class which represents the message associated with
	 * the given buff.
	 */

	private static class BuffMessage
	{
		private Color c;
		private String message;

		public BuffMessage( Color c, String message )
		{
			this.c = c;
			this.message = message;
		}
	}
}
