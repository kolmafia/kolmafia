/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.Crimbo09Request;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TurnCounter
	implements Comparable<TurnCounter>
{
	private static final ArrayList<TurnCounter> relayCounters = new ArrayList<TurnCounter>();
	private static final HashSet<String> ALL_LOCATIONS = new HashSet<String>();

	private final int value;
	private final String image;
	private final String label;
	private String URL;
	private String parsedLabel;
	private HashSet<String> exemptions;
	private int lastWarned;

	public TurnCounter( final int value, final String label, final String image )
	{
		this.value = KoLCharacter.getCurrentRun() + value;
		this.label = label.replaceAll( ":", "" );
		this.image = image.replaceAll( ":", "" );
		this.lastWarned = -1;
		this.parsedLabel = this.label;
		int pos = this.parsedLabel.lastIndexOf( " " );
		while ( pos != -1 )
		{
			String word = this.parsedLabel.substring( pos + 1 ).trim();
			if ( word.equals( "loc=*" ) )
			{
				this.exemptions = TurnCounter.ALL_LOCATIONS;
			}
			else if ( word.startsWith( "loc=" ) )
			{
				if ( this.exemptions == null )
				{
					this.exemptions = new HashSet<String>();
				}
				this.exemptions.add( word.substring( 4 ) );
			}
			else if ( word.indexOf( ".php" ) != -1 )
			{
				this.URL = word;
			}
			else break;

			this.parsedLabel = this.parsedLabel.substring( 0, pos ).trim();
			pos = this.parsedLabel.lastIndexOf( " " );
		}
		if ( this.parsedLabel.length() == 0 )
		{
			this.parsedLabel = "Manual";
		}
	}

	public boolean isExempt( final String adventureId )
	{
		if ( this.exemptions == TurnCounter.ALL_LOCATIONS ||
			(this.exemptions != null && this.exemptions.contains( adventureId )) )
		{
			return true;
		}

		return false;
	}

	public String imageURL()
	{
		if ( this.URL != null ) return this.URL;

		if ( this.exemptions != null && this.exemptions.size() == 1 )
		{	// Exactly one exempt location
			String loc = this.exemptions.iterator().next();
			return "adventure.php?snarfblat=" + loc;
		}

		return null;
	}

	public String getLabel()
	{
		return this.parsedLabel;
	}

	public String getImage()
	{
		return this.image;
	}

	public int getTurnsRemaining()
	{
		return this.value - KoLCharacter.getCurrentRun();
	}

	public static int turnsRemaining( final String label )
	{
		Iterator<TurnCounter> it = TurnCounter.relayCounters.iterator();

		while ( it.hasNext() )
		{
			TurnCounter current = it.next();
			if ( current.parsedLabel.equals( label ) )
			{
				return current.value - KoLCharacter.getCurrentRun();
			}
		}

		return -1;
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( o == null || !( o instanceof TurnCounter ) )
		{
			return false;
		}

		return this.label.equals( ( (TurnCounter) o ).label ) && this.value == ( (TurnCounter) o ).value;
	}

	@Override
	public int hashCode()
	{
		int hash = 0;
		hash += this.value;
		hash += 31 * (this.label != null ? this.label.hashCode() : 0);
		return hash;
	}

	public int compareTo( final TurnCounter o )
	{
		if ( o == null || !( o instanceof TurnCounter ) )
		{
			return -1;
		}

		return this.value - ( (TurnCounter) o ).value;
	}

	public static final void clearCounters()
	{
		TurnCounter.relayCounters.clear();
		TurnCounter.saveCounters();
	}

	public static final void loadCounters()
	{
		TurnCounter.relayCounters.clear();

		String counters = Preferences.getString( "relayCounters" );
		if ( counters.length() == 0 )
		{
			return;
		}

		StringTokenizer tokens = new StringTokenizer( counters, ":" );
		while ( tokens.hasMoreTokens() )
		{
			int turns = StringUtilities.parseInt( tokens.nextToken() ) - KoLCharacter.getCurrentRun();
			if ( !tokens.hasMoreTokens() ) break;
			String name = tokens.nextToken();
			if ( !tokens.hasMoreTokens() ) break;
			String image = tokens.nextToken();
			startCountingInternal( turns, name, image );
		}
	}

	public static final void saveCounters()
	{
		StringBuilder counters = new StringBuilder();
		Iterator<TurnCounter> it = TurnCounter.relayCounters.iterator();

		while ( it.hasNext() )
		{
			TurnCounter current = it.next();

			if ( counters.length() > 0 )
			{
				counters.append( ":" );
			}

			counters.append( current.value );
			counters.append( ":" );
			counters.append( current.label );
			counters.append( ":" );
			counters.append( current.image );
		}

		Preferences.setString( "relayCounters", counters.toString() );
	}

	public static final TurnCounter getExpiredCounter( GenericRequest request, boolean informational )
	{
		String URL = request.getURLString();
		KoLAdventure adventure = AdventureDatabase.getAdventureByURL( URL );

		String adventureId;
		int turnsUsed;

		if ( adventure != null )
		{
			adventureId = adventure.getAdventureId();
			turnsUsed = adventure.getRequest().getAdventuresUsed();
		}
		else if ( AdventureDatabase.getUnknownName( URL ) != null )
		{
			adventureId = "";
			turnsUsed = 1;
		}
		else
		{
			adventureId = "";
			turnsUsed = TurnCounter.getTurnsUsed( request );
		}

		if ( turnsUsed == 0 )
		{
			return null;
		}

		int thisTurn = KoLCharacter.getCurrentRun();
		int currentTurns = thisTurn + turnsUsed - 1;

		Iterator<TurnCounter> it = TurnCounter.relayCounters.iterator();

		while ( it.hasNext() )
		{
			TurnCounter current = it.next();

			if ( current.value > currentTurns ||
				current.lastWarned == thisTurn ||
				current.isExempt( adventureId ) != informational )
			{
				continue;
			}

			if ( informational && current.value > thisTurn )
			{	// Defer until later, there's no point in reporting an
				// informational counter prior to actual expiration.
				continue;
			}

			if ( current.value < thisTurn )
			{
				it.remove();
			}

			current.lastWarned = thisTurn;
			return current;
		}

		return null;
	}

	public static final String getUnexpiredCounters()
	{
		int currentTurns = KoLCharacter.getCurrentRun();

		StringBuilder counters = new StringBuilder();
		Iterator<TurnCounter> it = TurnCounter.relayCounters.iterator();

		while ( it.hasNext() )
		{
			TurnCounter current = it.next();

			if ( current.value < currentTurns )
			{	// Can't remove the counter - a counterScript may still
				// be waiting for it to be delivered.
				//it.remove();
				continue;
			}

			if ( counters.length() > 0 )
			{
				counters.append( KoLConstants.LINE_BREAK );
			}

			counters.append( current.parsedLabel );
			counters.append( " (" );
			counters.append( current.value - currentTurns );
			counters.append( ")" );
		}

		return counters.toString();
	}

	public static final void startCounting( final int value, final String label, final String image )
	{
		TurnCounter.startCountingInternal( value, label, image );
		TurnCounter.saveCounters();
	}

	private static final void startCountingInternal( final int value, final String label, final String image )
	{
		if ( value >= 0 )
		{
			TurnCounter counter = new TurnCounter( value, label, image );

			if ( !TurnCounter.relayCounters.contains( counter ) )
			{
				TurnCounter.relayCounters.add( counter );
			}
		}
	}

	public static final void stopCounting( final String label )
	{
		Iterator<TurnCounter> it = TurnCounter.relayCounters.iterator();

		while ( it.hasNext() )
		{
			TurnCounter current = it.next();
			if ( current.parsedLabel.equals( label ) )
			{
				it.remove();
			}
		}

		TurnCounter.saveCounters();
	}

	public static final boolean isCounting( final String label, final int value )
	{
		int searchValue = KoLCharacter.getCurrentRun() + value;

		Iterator<TurnCounter> it = TurnCounter.relayCounters.iterator();

		while ( it.hasNext() )
		{
			TurnCounter current = it.next();
			if ( current.parsedLabel.equals( label ) && current.value == searchValue )
			{
				return true;
			}
		}

		return false;
	}

	public static final boolean isCounting( final String label )
	{
		Iterator<TurnCounter> it = TurnCounter.relayCounters.iterator();

		while ( it.hasNext() )
		{
			TurnCounter current = it.next();
			if ( current.parsedLabel.equals( label ) && current.value >= KoLCharacter.getCurrentRun() )
			{
				return true;
			}
		}

		return false;
	}

	public static final String getCounters( String label, int minTurns, int maxTurns )
	{
		label = label.toLowerCase();
		boolean checkExempt = label.length() == 0;
		minTurns += KoLCharacter.getCurrentRun();
		maxTurns += KoLCharacter.getCurrentRun();
		StringBuilder buf = new StringBuilder();
		Iterator<TurnCounter> it = TurnCounter.relayCounters.iterator();

		while ( it.hasNext() )
		{
			TurnCounter current = it.next();
			if ( current.value < minTurns || current.value > maxTurns )
			{
				continue;
			}
			if ( checkExempt && current.isExempt( "" ) )
			{
				continue;
			}
			if ( current.parsedLabel.toLowerCase().indexOf( label ) == -1 )
			{
				continue;
			}
			if ( buf.length() != 0 )
			{
				buf.append( "\t" );
			}
			buf.append( current.parsedLabel );
		}

		return buf.toString();
	}

	private static final int getTurnsUsed( GenericRequest request )
	{
		if ( !( request instanceof RelayRequest ) )
		{
			return request.getAdventuresUsed();
		}

		String urlString = request.getURLString();
		String path = request.getPath();

		if ( urlString.startsWith( "adventure.php" ) )
		{
			// Assume unknown adventure locations take 1 turn each
			// This is likely not true under the Sea, for example,
			// but it's as good a guess as any we can make.

			return 1;
		}

		if ( path.equals( "crimbo09.php" ) )
		{
			return Crimbo09Request.getTurnsUsed( request );
		}

		if ( path.equals( "craft.php" ) || path.equals( "guild.php" ) )
		{
			return CreateItemRequest.getAdventuresUsed( request );
		}

		return 0;
	}

	public static final void deleteByHash( final int hash )
	{
		Iterator<TurnCounter> it = TurnCounter.relayCounters.iterator();

		while ( it.hasNext() )
		{
			if ( System.identityHashCode( it.next() ) == hash )
			{
				it.remove();
			}
		}

		TurnCounter.saveCounters();
	}

	public static final int count()
	{
		return TurnCounter.relayCounters.size();
	}

	public static final Iterator<TurnCounter> iterator()
	{
		Collections.sort( TurnCounter.relayCounters );
		return TurnCounter.relayCounters.iterator();
	}
}
