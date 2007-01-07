/**
 * Copyright (c) 2005-2006, KoLmafia development team
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;

public class CakeArenaManager extends StaticEntity
{
	public static final Pattern WIN_PATTERN = Pattern.compile( "is the winner, and gains (\\d+) experience" );
	private static LockableListModel opponentList = new LockableListModel();

	/**
	 * Registers an opponent inside of the arena manager.
	 * This should be used to update any information that
	 * relates to the arena.
	 */

	public static void registerOpponent( int opponentId, String name, String race, int weight )
	{
		ArenaOpponent ao = new ArenaOpponent( opponentId, name, race, weight );

		int index = opponentList.indexOf( ao );

		if ( index != -1 )
			opponentList.remove( ao );
		else
			index = opponentList.size();

		opponentList.add( index, ao );
	}

	/**
	 * Retrieves the opponents Id based on the string
	 * description for the opponent.
	 */

	public static void fightOpponent( String opponent, int eventId, int repeatCount )
	{
		for ( int i = 0; i < opponentList.size(); ++i )
		{
			if ( opponent.equals( opponentList.get(i).toString() ) )
			{
				FamiliarTrainingFrame.getResults().clearBuffer();

				Matcher victoryMatcher;
				Pattern victoryPattern = WIN_PATTERN;
				CakeArenaRequest request = new CakeArenaRequest( ((ArenaOpponent)opponentList.get(i)).getId(), eventId );

				for ( int j = 1; KoLmafia.permitsContinue() && j <= repeatCount; ++j )
				{
					KoLmafia.updateDisplay( "Arena battle, round " + j + " in progress..." );
					RequestThread.postRequest( request );

					victoryMatcher = victoryPattern.matcher( request.responseText );
					StringBuffer text = new StringBuffer();

					if ( victoryMatcher.find() )
						text.append( "<font color=green><b>Round " + j + " of " + repeatCount + "</b></font>: " );
					else
						text.append( "<font color=red><b>Round " + j + " of " + repeatCount + "</b></font>: " );

					text.append( request.responseText.substring( 0, request.responseText.indexOf( "</table>" ) ).replaceAll(
						"><" , "" ).replaceAll( "<.*?>", " " ) );

					text.append( "<br><br>" );
					FamiliarTrainingFrame.getResults().append( text.toString() );

				}

				KoLmafia.updateDisplay( "Arena battles complete." );
				return;
			}
		}
	}

	/**
	 * Returns a list of opponents are available today at
	 * the cake-shaped arena.
	 */

	public static LockableListModel getOpponentList()
	{
		if ( opponentList.isEmpty() )
			RequestThread.postRequest( new CakeArenaRequest() );

		return opponentList;
	}

	public static String getEvent( int eventId )
	{
		switch ( eventId )
		{
		case 1:
			return "Cage Match";
		case 2:
			return "Scavenger Hunt";
		case 3:
			return "Obstacle Course";
		case 4:
			return "Hide and Seek";
		default:
			return "Unknown Event";
		}
	}

	/**
	 * An internal class which represents a single arena
	 * opponent.  Used to track the opponent.
	 */

	public static class ArenaOpponent
	{
		private int id;
		private String name;
		private String race;
		private int weight;
		private String description;

		public ArenaOpponent( int id, String name, String race, int weight )
		{
			this.id = id;
			this.name = name;
			this.race = race;
			this.weight = weight;
			this.description = race + " (" + weight + " lbs)";
		}

		public int getId()
		{	return id;
		}

		public String getName()
		{	return name;
		}

		public String getRace()
		{	return race;
		}

		public int getWeight()
		{	return weight;
		}

		public String toString()
		{	return description;
		}

		public boolean equals( Object o )
		{	return o != null && o instanceof ArenaOpponent && id == ((ArenaOpponent)o).id;
		}
	}
}
