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

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.java.dev.spellcast.utilities.LockableListModel;

public class CakeArenaManager implements KoLConstants
{
	private KoLmafia client;
	private LimitedSizeChatBuffer results;
	private LockableListModel opponentList;

	public CakeArenaManager( KoLmafia client )
	{
		this.client = client;
		this.results = new LimitedSizeChatBuffer( "Arena Tracker" );
		opponentList = new LockableListModel();
	}

	/**
	 * Registers an opponent inside of the arena manager.
	 * This should be used to update any information that
	 * relates to the arena.
	 */

	public void registerOpponent( int opponentID, String race, String weight )
	{
		ArenaOpponent ao = new ArenaOpponent( opponentID, race, weight );

		int index = opponentList.indexOf( ao );

		if ( index != -1 )
			opponentList.remove( ao );
		else
			index = opponentList.size();

		opponentList.add( index, ao );
	}

	/**
	 * Retrieves the opponents ID based on the string
	 * description for the opponent.
	 */

	public void fightOpponent( String opponent, int eventID, int battleCount )
	{
		Matcher victoryMatcher;
		Pattern victoryPattern = Pattern.compile( "is the winner, and gains (\\d+) experience" );

		for ( int i = 0; i < opponentList.size(); ++i )
		{
			if ( opponent.equals( opponentList.get(i).toString() ) )
			{
				CakeArenaRequest request = new CakeArenaRequest( client, ((ArenaOpponent)opponentList.get(i)).getID(), eventID );
				client.resetContinueState();

				for ( int j = 1; client.permitsContinue() && j <= battleCount; ++j )
				{
					client.updateDisplay( DISABLED_STATE, "Arena battle, round " + j + " in progress..." );
					client.makeRequest( request, 1 );

					victoryMatcher = victoryPattern.matcher( request.responseText );

					if ( victoryMatcher.find() )
						results.append( "<font color=green><b>Round " + j + "</b>: Victory, gained " + victoryMatcher.group(1) + " kills.</font><br>" );
					else
						results.append( "<font color=red><b>Round " + j + "</b>: Defeat, gained 0 kills.</font><br>" );
				}
				return;
			}
		}

		client.updateDisplay( ENABLED_STATE, "Arena battles complete." );
	}

	/**
	 * Returns the chat buffer being used to update the
	 * arena results.
	 */

	public LimitedSizeChatBuffer getResults()
	{	return results;
	}

	/**
	 * Returns a list of opponents are available today at
	 * the cake-shaped arena.
	 */

	public LockableListModel getOpponentList()
	{	return opponentList;
	}

	/**
	 * An internal class which represents a single arena
	 * opponent.  Used to track the opponent.
	 */

	public static class ArenaOpponent
	{
		private int id;
		private String race;
		private String description;

		public ArenaOpponent( int id, String race, String weight )
		{
			this.id = id;
			this.race = race;
			this.description = race + " (" + weight + ")";
		}

		public int getID()
		{	return id;
		}

		public String getRace()
		{	return race;
		}

		public String toString()
		{	return description;
		}

		public boolean equals( Object o )
		{	return o != null && o instanceof ArenaOpponent && id == ((ArenaOpponent)o).id;
		}
	}
}