/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

import java.io.File;
import java.io.PrintStream;

import java.text.DateFormat;

import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.session.ContactManager;

import net.sourceforge.kolmafia.swingui.BuffBotFrame;

import net.sourceforge.kolmafia.utilities.IntegerCache;

public class BuffBotHome
{
	private static final DateFormat TIMESTAMP_FORMAT =
		DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT );

	public static final Color NOCOLOR = new Color( 0, 0, 0 );
	public static final Color ERRORCOLOR = new Color( 128, 0, 0 );
	public static final Color NONBUFFCOLOR = new Color( 0, 0, 128 );
	public static final Color BUFFCOLOR = new Color( 0, 128, 0 );

	private static boolean isActive = false;

	private static final TreeMap pastRecipients = new TreeMap();
	private static final LockableListModel messages = new LockableListModel();
	private static PrintStream textLogStream = System.out;
	private static PrintStream hypertextLogStream = System.out;

	/**
	 * Constructs a new <code>BuffBotHome</code>. However, note that this does not automatically translate into the
	 * messages being displayed; until a chat display is set, this buffer merely stores the message content to be
	 * displayed.
	 */

	public static final void loadSettings()
	{
		BuffBotHome.messages.clear();
		BuffBotHome.pastRecipients.clear();

		// Create the text log file which shows only the buffs
		// which have been requested in a comma-delimited format.

		BuffBotHome.textLogStream = BuffBotHome.getPrintStream( ".txt" );

		// Create the standard HTML log which can be opened
		// up to see all activity.

		BuffBotHome.hypertextLogStream = BuffBotHome.getPrintStream( ".html" );
		BuffBotHome.hypertextLogStream.println( "<html><head><style> body { font-family: sans-serif; } </style>" );
		BuffBotHome.hypertextLogStream.flush();
	}

	/**
	 * Retrieves the file which would be associated with the current player, placed in the given folder and given the
	 * appropriate extension.
	 */

	private static final File getFile( final String extension )
	{
		return new File(
			KoLConstants.BUFFBOT_LOCATION,
			KoLCharacter.baseUserName() + "_" + KoLConstants.DAILY_FORMAT.format( new Date() ) + "_" + extension );
	}

	/**
	 * Retrieves the output stream which would be associated with the current player, placed in the given folder and
	 * given the appropriate extension.
	 */

	private static final PrintStream getPrintStream( final String extension )
	{
		File output = BuffBotHome.getFile( extension );
		return LogStream.openStream( output, false );
	}

	/**
	 * Retrieves all the past recipients of the buff associated with the given meat amount.
	 */

	private static final List getPastRecipients( final int meatSent )
	{
		Integer key = IntegerCache.valueOf( meatSent );
		if ( !BuffBotHome.pastRecipients.containsKey( key ) )
		{
			BuffBotHome.pastRecipients.put( key, new SortedListModel() );
		}

		return (List) BuffBotHome.pastRecipients.get( key );
	}

	/**
	 * Returns the number of times the given name has requested the buff associated with the given meat amount.
	 */

	public static final int getInstanceCount( final int meatSent, final String name )
	{
		List pastRecipients = BuffBotHome.getPastRecipients( meatSent );
		BuffRecord record = new BuffRecord( name );

		int index = pastRecipients.indexOf( record );
		return index == -1 ? 0 : ( (BuffRecord) pastRecipients.get( index ) ).getCount();
	}

	private static class BuffRecord
		implements Comparable
	{
		private int count;
		private final String name;
		private boolean deny;

		public BuffRecord( final String name )
		{
			this.name = name;
			this.count = 1;
			this.deny = false;
		}

		public int getCount()
		{
			return this.count;
		}

		public void incrementCount()
		{
			if ( this.count != Integer.MAX_VALUE )
			{
				++this.count;
			}
		}

		public void restrict()
		{
			this.deny = true;
		}

		public boolean isPermitted()
		{
			return !this.deny;
		}

		public int compareTo( final Object o )
		{
			return this.name.compareToIgnoreCase( ( (BuffRecord) o ).name );
		}

		public boolean equals( final Object o )
		{
			return this.name.equalsIgnoreCase( ( (BuffRecord) o ).name );
		}
	}

	/**
	 * Registers the given name as a recipient of the buff associated with the given meat amount.
	 */

	public static final void addToRecipientList( final int meatSent, final String name )
	{
		List pastRecipients = BuffBotHome.getPastRecipients( meatSent );
		BuffRecord record = new BuffRecord( name );

		int index = pastRecipients.indexOf( record );
		if ( index == -1 )
		{
			pastRecipients.add( record );
		}
		else
		{
			( (BuffRecord) pastRecipients.get( index ) ).incrementCount();
		}
	}

	/**
	 * Causes the given player to be permanently ignored from all future buff requests.
	 */

	public static final void denyFutureBuffs( final String name )
	{
		List pastRecipients = BuffBotHome.getPastRecipients( 0 );
		BuffRecord record = new BuffRecord( name );

		int index = pastRecipients.indexOf( record );
		if ( index == -1 )
		{
			record.restrict();
			pastRecipients.add( record );
		}
		else
		{
			( (BuffRecord) pastRecipients.get( index ) ).restrict();
		}
	}

	public static final boolean isPermitted( final String name )
	{
		List pastRecipients = BuffBotHome.getPastRecipients( 0 );
		BuffRecord record = new BuffRecord( name );

		int index = pastRecipients.indexOf( record );
		if ( index == -1 )
		{
			return true;
		}

		return ( (BuffRecord) pastRecipients.get( index ) ).isPermitted();
	}

	/**
	 * Closes the log file used to actively record messages that are being stored in the buffer. This formally closes
	 * the file and sets the log file currently being used to null so that no future updates are attempted.
	 */

	public static final void deinitialize()
	{
		BuffBotHome.hypertextLogStream.println();
		BuffBotHome.hypertextLogStream.println();
		BuffBotHome.hypertextLogStream.println( "</body></html>" );

		BuffBotHome.hypertextLogStream.close();
		BuffBotHome.hypertextLogStream = null;
		BuffBotHome.isActive = false;
	}

	/**
	 * An internal function used to indicate that something has changed with regards to the buffbot. This method is used
	 * whenever no timestamp is required for a given buffbot entry.
	 */

	public static final void update( final Color c, final String entry )
	{
		if ( entry != null && BuffBotHome.hypertextLogStream != null )
		{
			BuffBotHome.messages.add( 0, new BuffMessage( c, entry ) );
			BuffBotHome.hypertextLogStream.println( "<br><font color=" + DataUtilities.toHexString( c ) + ">" + entry + "</font>" );
			BuffBotHome.hypertextLogStream.flush();

			RequestLogger.printLine( entry );
			if ( BuffBotHome.messages.size() > 100 )
			{
				BuffBotHome.messages.remove( 100 );
			}
		}
	}

	/**
	 * Adds a time-stamped entry to the log for the buffbot. In general, this is the preferred method of modifying the
	 * buffbot. However, the standard appending procedure is still valid.
	 */

	public static final void timeStampedLogEntry( final Color c, final String entry )
	{
		BuffBotHome.update( c, BuffBotHome.TIMESTAMP_FORMAT.format( new Date() ) + ": " + entry );
	}

	/**
	 * Adds the given buff to the comma-delimited list of events for the buffbot. This is used to register whenever a
	 * buff has been requested and successfully processed.
	 */

	public static final void recordBuff( final String name, final String buff, final int casts, final int meatSent )
	{
		BuffBotHome.textLogStream.println( BuffBotHome.TIMESTAMP_FORMAT.format( new Date() ) + "," + name + "," + ContactManager.getPlayerId( name ) + "," + buff + "," + casts + "," + meatSent );
	}

	/**
	 * Sets the frame that should be updated whenever a status message arrives.
	 */

	public static final void setFrame( final BuffBotFrame frame )
	{
	}

	/**
	 * Sets the current active state for the buffbot. Note that this does not affect whether or not the buffbot
	 * continues logging events - it merely affects whether or not the the buffbot itself is running.
	 */

	public static final void setBuffBotActive( final boolean isActive )
	{
		BuffBotHome.isActive = isActive;
	}

	/**
	 * Returns whether or not the buffbot is currently active. Note that this does not say whether or not the buffbot is
	 * currently logging data - int only states whether or not the buffbot itself is running.
	 */

	public static final boolean isBuffBotActive()
	{
		return BuffBotHome.isActive;
	}

	/**
	 * Used to retrieve the list of messages being updated by this <code>BuffBotHome</code>. This should only be used
	 * if there is a need to display the messages in some list form.
	 */

	public static final LockableListModel getMessages()
	{
		return BuffBotHome.messages;
	}

	/**
	 * Returns an instance of the cell renderer which should be used to display the buff messages inside of a list
	 * setting.
	 */

	public static final DefaultListCellRenderer getMessageRenderer()
	{
		return new BuffMessageRenderer();
	}

	/**
	 * An internal class which represents the renderer which should be used to display the buff messages.
	 */

	private static class BuffMessageRenderer
		extends DefaultListCellRenderer
	{
		public BuffMessageRenderer()
		{
			this.setOpaque( true );
		}

		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			Component defaultComponent =
				super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null || !( value instanceof BuffMessage ) )
			{
				return defaultComponent;
			}

			BuffMessage bm = (BuffMessage) value;
			( (JLabel) defaultComponent ).setText( bm.message );
			defaultComponent.setForeground( bm.c );
			return defaultComponent;
		}
	}

	/**
	 * An internal class which represents the message associated with the given buff.
	 */

	private static class BuffMessage
	{
		private final Color c;
		private final String message;

		public BuffMessage( final Color c, final String message )
		{
			this.c = c;
			this.message = message;
		}
	}
}
