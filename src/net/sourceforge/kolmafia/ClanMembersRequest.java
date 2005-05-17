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

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.Date;
import javax.swing.JOptionPane;
import java.text.SimpleDateFormat;
import java.util.Collections;

public class ClanMembersRequest extends KoLRequest
{
	public ClanMembersRequest( KoLmafia client )
	{	super( client, "showclan.php" );
	}

	public void run()
	{
		// First, you need to know which clan you
		// belong to.  This is done by doing a
		// profile lookup on yourself.

		updateDisplay( DISABLED_STATE, "Determining clan ID..." );
		ProfileRequest clanIDLookup = new ProfileRequest( client, client.getCharacterData().getUsername() );
		clanIDLookup.run();

		Matcher clanIDMatcher = Pattern.compile( "showclan\\.php\\?whichclan=(\\d+)" ).matcher( clanIDLookup.responseText );
		if ( !clanIDMatcher.find() )
		{
			updateDisplay( ERROR_STATE, "Your character does not belong to a clan." );
			return;
		}

		// Now that you know which clan you belong
		// to, you can do a clan lookup to get a
		// complete list of clan members in one hit

		addFormField( "whichclan", clanIDMatcher.group(1) );
		updateDisplay( DISABLED_STATE, "Retrieving clan member list..." );
		super.run();

		// Now, parse out the complete list of clan
		// members so you can act on it.

		int lastMatchIndex = 0;
		Matcher memberMatcher = Pattern.compile( "<a class=nounder href=\"showplayer\\.php\\?who=(\\d+)\">(.*?)</a>" ).matcher( responseText );

		List memberList = new ArrayList();

		while ( memberMatcher.find( lastMatchIndex ) )
		{
			lastMatchIndex = memberMatcher.end();

			String playerID = memberMatcher.group(1);
			String playerName = memberMatcher.group(2);

			client.registerPlayer( playerName, playerID );
			memberList.add( playerName );
		}

		updateDisplay( ENABLED_STATE, "Member list retrieved." );

		// With the clan member list retrieved, you make sure
		// the user wishes to spend the time to do so.

		boolean continueProcessing = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
			"This process should take " + ((int)(memberList.size() / 15) + 1) + " minutes to complete.\nAre you sure you want to continue?",
			"Member list retrieved!", JOptionPane.YES_NO_OPTION );

		if ( !continueProcessing )
			return;

		// It has been confirmed that the user wishes to
		// continue.  Now, you need to retrieve the cutoff
		// horizon the user wishes to use.

		Date cutoffDate;

		try
		{
			int daysIdle = df.parse( JOptionPane.showInputDialog( "How many days since last login?", "30" ) ).intValue();
			long millisecondsIdle = 86400000L * daysIdle;
			cutoffDate = new Date( System.currentTimeMillis() - millisecondsIdle );
		}
		catch ( Exception e )
		{
			// If user doesn't enter an integer, then just return
			// without doing anything

			return;
		}

		// Now that it's known what the uesr wishes to do,
		// you begin the attempt to process the request.

		updateDisplay( DISABLED_STATE, "Processing request..." );

		Iterator memberIterator = memberList.iterator();
		SimpleDateFormat sdf = new SimpleDateFormat( "MMMM d, yyyy" );
		Pattern lastonPattern = Pattern.compile( "<b>Last Login:</b> (.*?)<br>" );

		String currentMember;
		ProfileRequest memberLookup;
		Matcher lastonMatcher;
		List idleList = new ArrayList();

		// In order to determine the last time the player
		// was idle, you need to manually examine each of
		// the player profiles for each player.

		for ( int i = 1; memberIterator.hasNext() && client.permitsContinue(); ++i )
		{
			updateDisplay( NOCHANGE, "Examining member " + i + " of " + memberList.size() + "..." );
			currentMember = (String) memberIterator.next();
			memberLookup = new ProfileRequest( client, currentMember );
			memberLookup.run();

			lastonMatcher = lastonPattern.matcher( memberLookup.responseText );
			lastonMatcher.find();

			try
			{
				if ( cutoffDate.after( sdf.parse( lastonMatcher.group(1) ) ) )
					idleList.add( currentMember );
			}
			catch ( Exception e )
			{
			}

			// Manually add in a bit of lag so that it doesn't turn into
			// hammering the server for information.

			KoLRequest.delay( 4000 );
		}

		// Now that all of the member profiles have been retrieved,
		// you show the user the complete list of members.

		if ( idleList.size() == 0 )
			JOptionPane.showMessageDialog( null, "No idle accounts detected!" );
		else
		{
			Collections.sort( idleList );
			Object selectedValue = JOptionPane.showInputDialog( null, idleList.size() + " idle members:",
				"Idle hands!", JOptionPane.INFORMATION_MESSAGE, null, idleList.toArray(), idleList.get(0) );

			// Now, you need to determine what to do with the data;
			// sometimes, it shows too many players, and the person
			// wishes to retain some and cancels the process.

			if ( selectedValue != null )
			{
				(new ClanBootRequest( client, idleList.toArray() )).run();
				updateDisplay( ENABLED_STATE, "Idle members have been booted." );
			}
			else
			{
				File file = new File( "data/" + "IdleMembers.txt" );

				try
				{
					file.getParentFile().mkdirs();
					PrintStream ostream = new PrintStream( new FileOutputStream( file, true ), true );

					for ( int i = 0; i < idleList.size(); ++i )
						ostream.println( idleList.get(i) );
				}
				catch ( Exception e )
				{	throw new RuntimeException( "The file <" + file.getAbsolutePath() + "> could not be opened for writing" );
				}

				updateDisplay( ENABLED_STATE, "List of idle members saved to " + file.getAbsolutePath() );
			}
		}
	}

	private class ClanBootRequest extends KoLRequest
	{
		public ClanBootRequest( KoLmafia client, Object [] members )
		{
			super( client, "" );
			addFormField( "pwd", client.getPasswordHash() );
			addFormField( "action", "modify" );
			for ( int i = 0; i < members.length; ++i )
				addFormField( "boot" + client.getPlayerID( (String) members[i] ), "on" );
		}
	}
}
