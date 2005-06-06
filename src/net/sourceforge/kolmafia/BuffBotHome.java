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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.TimeZone;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * Holder for the BuffBot log (which should survive outside of
 * the BuffBot Frame
 */

public class BuffBotHome implements KoLConstants
{
	private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();
	private static final TimeZone KINGDOM_TIMEZONE = TimeZone.getTimeZone( "US/Eastern" );

	public static Color NOCOLOR = new Color( 0, 0, 0 );
	public static Color ERRORCOLOR = new Color( 128, 0, 0 );
	public static Color NONBUFFCOLOR = new Color( 0, 0, 128 );
	public static Color BUFFCOLOR = new Color( 0, 128, 0 );

	private KoLmafia client;
	private boolean isActive;

	private PrintWriter activeLogWriter;
	private LockableListModel messages;
	private PrintStream ostream;
	private List pastRecipients;

	private BuffBotFrame frame;
	private static final DateFormat tsdf = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

	/**
	 * Constructs a new <code>BuffBotHome</code>.  However, note that this
	 * does not automatically translate into the messages being displayed;
	 * until a chat display is set, this buffer merely stores the message
	 * content to be displayed.
	 */

	public BuffBotHome( KoLmafia client )
	{
		this.client = client;
		this.messages = new LockableListModel();
		pastRecipients = new ArrayList();

		String dayOfYear = sdf.format(new Date());
		String characterName = client == null ? "" : client.getLoginName();
		String noExtensionName = characterName.replaceAll( "\\p{Punct}", "" ).replaceAll( " ", "_" ).toLowerCase();
		File file = new File( DATA_DIRECTORY + noExtensionName + "_BuffBot" + dayOfYear + ".html" );

		try
		{
			file.getParentFile().mkdirs();
			activeLogWriter = new PrintWriter( new FileOutputStream( file, true ), true );
			activeLogWriter.println( "<html><head><style> body { font-family: sans-serif; } </style>" );
			activeLogWriter.flush();
		}
		catch ( java.io.FileNotFoundException e )
		{	throw new RuntimeException( "The file <" + file.getAbsolutePath() + "> could not be opened for writing" );
		}
		catch ( SecurityException e )
		{	throw new RuntimeException( "The file <" + file.getAbsolutePath() + "> could not be opened for writing" );
		}
	}

	public int getInstanceCount( int meatSent, String name )
	{
		loadPastRecipients( meatSent );
		ostream.close();

		int instanceCount = 0;
		for ( int i = 0; i < pastRecipients.size(); ++i )
			if ( pastRecipients.get(i).equals( name ) )
				++instanceCount;
		return instanceCount;
	}

	public void loadPastRecipients( int meatSent )
	{
		try
		{
			TimeZone.setDefault( KINGDOM_TIMEZONE );
			String dayOfYear = sdf.format(new Date());
			TimeZone.setDefault( DEFAULT_TIMEZONE );

			String characterName = client == null ? "" : client.getLoginName();
			String noExtensionName = characterName.replaceAll( "\\p{Punct}", " " ).replaceAll( " ", "_" ).toLowerCase();

			pastRecipients.clear();
			File datafile = new File( DATA_DIRECTORY + noExtensionName + "_BuffBot" + dayOfYear + "_" + meatSent + ".txt" );
			if ( datafile.exists() )
			{
				BufferedReader istream = new BufferedReader( new InputStreamReader( new FileInputStream( datafile ) ) );
				String line;
				while ( (line = istream.readLine()) != null )
					pastRecipients.add( line );
				istream.close();
			}

			ostream = new PrintStream( new FileOutputStream( datafile, true ), true );
		}
		catch ( IOException e )
		{
		}
	}

	public void addToRecipientList( int meatSent, String name )
	{
		loadPastRecipients( meatSent );
		pastRecipients.add( name );
		ostream.println( name );
		ostream.close();
	}

	/**
	 * Closes the log file used to actively record messages that are
	 * being stored in the buffer.  This formally closes the file and
	 * sets the log file currently being used to null so that no future
	 * updates are attempted.
	 */

	public void deinitialize()
	{
		activeLogWriter.println();
		activeLogWriter.println();
		activeLogWriter.println( "</body></html>" );

		activeLogWriter.close();
		activeLogWriter = null;
		isActive = false;
	}

	/**
	 * An internal function used to indicate that something has changed with
	 * regards to the buffbot.  This method is used whenever no timestamp
	 * is required for a given buffbot entry.
	 */

	public void update( Color c, String entry )
	{
		if ( entry != null )
		{
			messages.add( 0, new BuffMessage( c, entry ) );
			activeLogWriter.println( "<font color=" + DataUtilities.toHexString( c ) + ">" + entry + "</font>" );
			activeLogWriter.flush();

			if ( client instanceof KoLmafiaCLI )
				System.out.println( entry );
		}
	}

	/**
	 * An internal function used to display changes to system status
	 * while the buffbot is running.
	 */

	public void updateStatus(String statusMessage)
	{	frame.setTitle( "KoLmafia: Buffbot - " + statusMessage );
	}

	/**
	 * Adds a time-stamped entry to the log for the buffbot.  In general,
	 * this is the preferred method of modifying the buffbot.  However,
	 * the standard appending procedure is still valid.
	 */

	public void timeStampedLogEntry( Color c, String entry )
	{	update( c, tsdf.format( new Date() ) + ": " + entry );
	}

	public LockableListModel getMessages()
	{	return messages;
	}

	public void setBuffBotActive( boolean isActive )
	{	this.isActive = isActive;
	}

	public void setFrame( BuffBotFrame frame )
	{	this.frame = frame;
	}

	public boolean isBuffBotActive()
	{	return isActive;
	}

	public static BuffMessageRenderer getBuffMessageRenderer()
	{	return new BuffMessageRenderer();
	}

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
