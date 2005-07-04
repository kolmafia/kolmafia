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
import net.java.dev.spellcast.utilities.LockableListModel;

public class CakeArenaManager
{
	private KoLmafia client;
	private LockableListModel opponentList;

	public CakeArenaManager( KoLmafia client )
	{
		this.client = client;
		opponentList = new LockableListModel();
	}

	/**
	 * Registers an opponent inside of the arena manager.
	 * This should be used to update any information that
	 * relates to the arena.
	 */

	public void registerOpponent( int opponentID, String opponent )
	{
		ArenaOpponent ao = new ArenaOpponent( opponentID, opponent );
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
		for ( int i = 0; i < opponentList.size(); ++i )
		{
			if ( opponent.equals( opponentList.get(i).toString() ) )
			{
				client.makeRequest( new CakeArenaRequest( client, ((ArenaOpponent)opponentList.get(i)).getID(), eventID ), battleCount );
				return;
			}
		}
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

	private class ArenaOpponent
	{
		private int id;
		private String opponent;

		public ArenaOpponent( int id, String opponent )
		{
			this.id = id;
			this.opponent = opponent;
		}

		public int getID()
		{	return id;
		}

		public String toString()
		{	return opponent;
		}

		public boolean equals( Object o )
		{
			return o != null && o instanceof ArenaOpponent &&
				id == ((ArenaOpponent)o).id;
		}
	}
}