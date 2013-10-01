/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import net.sourceforge.kolmafia.request.CakeArenaRequest;

import net.sourceforge.kolmafia.swingui.FamiliarTrainingFrame;

public class CakeArenaManager
{
	private static final LockableListModel opponentList = new LockableListModel();

	/**
	 * Registers an opponent inside of the arena manager. This should be used to update any information that relates to
	 * the arena.
	 */

	public static final void registerOpponent( final int opponentId, final String name, final String race,
		final int weight )
	{
		ArenaOpponent ao = new ArenaOpponent( opponentId, name, race, weight );

		int index = CakeArenaManager.opponentList.indexOf( ao );

		if ( index != -1 )
		{
			CakeArenaManager.opponentList.remove( ao );
		}
		else
		{
			index = CakeArenaManager.opponentList.size();
		}

		CakeArenaManager.opponentList.add( index, ao );
	}

	/**
	 * Retrieves the opponents Id based on the string description for the opponent.
	 */

	public static final void fightOpponent( final String target, final int eventId, final int repeatCount )
	{
		for ( int i = 0; i < CakeArenaManager.opponentList.size(); ++i )
		{
			ArenaOpponent opponent = (ArenaOpponent) CakeArenaManager.opponentList.get( i );
			if ( target.equals( opponent.toString() ) )
			{
				FamiliarTrainingFrame.getResults().clear();

				int opponentId = opponent.getId();
				CakeArenaRequest request = new CakeArenaRequest( opponentId, eventId );

				for ( int j = 1; KoLmafia.permitsContinue() && j <= repeatCount; ++j )
				{
					KoLmafia.updateDisplay( "Arena battle, round " + j + " in progress..." );
					RequestThread.postRequest( request );

					Matcher victoryMatcher = CakeArenaRequest.WIN_PATTERN.matcher( request.responseText );
					StringBuffer text = new StringBuffer();

					if ( victoryMatcher.find() )
					{
						text.append( "<font color=green><b>Round " + j + " of " + repeatCount + "</b></font>: " );
					}
					else
					{
						text.append( "<font color=red><b>Round " + j + " of " + repeatCount + "</b></font>: " );
					}

					int start = request.responseText.indexOf( "<body>" );
					int end = request.responseText.indexOf( "</table>", start );

					String body = request.responseText.substring( start, end );
					body = body.replaceAll( "<p>", KoLConstants.LINE_BREAK );
					body = body.replaceAll( "<.*?>", "" );
					body = body.replaceAll( KoLConstants.LINE_BREAK, "<br>" );
					text.append( body );

					text.append( "<br><br>" );
					FamiliarTrainingFrame.getResults().append( text.toString() );

				}

				KoLmafia.updateDisplay( "Arena battles complete." );
				return;
			}
		}
	}

	/**
	 * Returns a list of opponents are available today at the cake-shaped arena.
	 */

	public static final LockableListModel getOpponentList()
	{
		if ( CakeArenaManager.opponentList.isEmpty() )
		{
			RequestThread.postRequest( new CakeArenaRequest() );
		}

		return CakeArenaManager.opponentList;
	}

	public static final ArenaOpponent getOpponent( final int opponentId )
	{
		int count = CakeArenaManager.opponentList.size();

		for ( int i = 0; i < count; ++i )
		{
			ArenaOpponent ao = (ArenaOpponent) CakeArenaManager.opponentList.get( i );
			if ( ao.getId() == opponentId )
			{
				return ao;
			}
		}

		return null;
	}

	public static final String eventIdToName( final int eventId )
	{
		switch ( eventId )
		{
		case 1:
			return "Ultimate Cage Match";
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

	public static final int eventNameToId( final String eventName )
	{
		return	eventName.equals( "Ultimate Cage Match" ) ? 1 :
			eventName.equals( "Scavenger Hunt" ) ? 2 :
			eventName.equals( "Obstacle Course" ) ? 3 :
			eventName.equals( "Hide and Seek" ) ? 4 :
			0;
	}

	/**
	 * An internal class which represents a single arena opponent. Used to track the opponent.
	 */

	public static class ArenaOpponent
	{
		private final int id;
		private final String name;
		private final String race;
		private final int weight;
		private final String description;

		public ArenaOpponent( final int id, final String name, final String race, final int weight )
		{
			this.id = id;
			this.name = name;
			this.race = race;
			this.weight = weight;
			this.description = race + " (" + weight + " lbs)";
		}

		public int getId()
		{
			return this.id;
		}

		public String getName()
		{
			return this.name;
		}

		public String getRace()
		{
			return this.race;
		}

		public int getWeight()
		{
			return this.weight;
		}

		@Override
		public String toString()
		{
			return this.description;
		}

		@Override
		public boolean equals( final Object o )
		{
			return o != null && o instanceof ArenaOpponent && this.id == ( (ArenaOpponent) o ).id;
		}

		@Override
		public int hashCode()
		{
			return this.id;
		}
	}
}
