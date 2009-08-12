/**
 *
 */
package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.StringTokenizer;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TurnCounter
implements Comparable
{
	private static final ArrayList relayCounters = new ArrayList();

	private final int value;
	private final String image;
	private final String label;
	private int lastWarned;

	public TurnCounter( final int value, final String label, final String image )
	{
		this.value = KoLCharacter.getCurrentRun() + value;
		this.label = label.replaceAll( ":", "" );
		this.image = image.replaceAll( ":", "" );
		this.lastWarned = -1;
	}

	public boolean isExempt( final String adventureId )
	{
		if ( this.label.equals( "Wormwood" ) )
		{
			return adventureId.equals( "151" ) || adventureId.equals( "152" ) || adventureId.equals( "153" );
		}
		else if ( this.label.equals( "Dance Card" ) )
		{
			return adventureId.equals( "109" );
		}
		else if ( this.label.equals( "Communications Windchimes" ) ||
			  this.label.equals( "PADL Phone" ) )
		{
			// These counters just tell you when it is safe to use
			// the device again. There's nothing wrong with not
			// adventuring on the battlefield when they expire.

			return true;
		}
		else if ( this.label.indexOf( "Recharge</font>" ) != -1 )
		{
			return true;
		}

		return false;
	}

	public String imageURL()
	{
		// Ideally, this would be part of the counter that is saved in
		// the preferences, so users could configure their own counters
		// with linked URLs.
		//
		// If I can come up with a backwards compatible way to make
		// that work, I'll do it. Until then, it works only for
		// internally generated counter.s

		KoLAdventure adventure = null;

		if ( this.label.equals( "Dance Card" ) )
		{
			adventure = AdventureDatabase.getAdventure( "Haunted Ballroom" );
		}

		if ( adventure != null )
		{
			return adventure.getRequest().getURLString();
		}

		return null;
	}

	public String getLabel()
	{
		return this.label;
	}

	public String getImage()
	{
		return this.image;
	}
	
	public int getTurnsRemaining()
	{
		return this.value - KoLCharacter.getCurrentRun();
	}

	public boolean equals( final Object o )
	{
		if ( o == null || !( o instanceof TurnCounter ) )
		{
			return false;
		}

		return this.label.equals( ( (TurnCounter) o ).label ) && this.value == ( (TurnCounter) o ).value;
	}

	public int compareTo( final Object o )
	{
		if ( o == null || !( o instanceof TurnCounter ) )
		{
			return -1;
		}

		return this.value - ( (TurnCounter) o ).value;
	}

	public static final void clearCounters()
	{
		relayCounters.clear();
		TurnCounter.saveCounters();
	}

	public static final void loadCounters()
	{
		relayCounters.clear();

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
			startCounting( turns, name, image, false );
		}
	}

	public static final void saveCounters()
	{
		int currentTurns = KoLCharacter.getCurrentRun();

		StringBuffer counters = new StringBuffer();
		Iterator it = relayCounters.iterator();

		while ( it.hasNext() )
		{
			TurnCounter current = (TurnCounter) it.next();

			if ( current.value < currentTurns )
			{
				it.remove();
				continue;
			}

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

	public static final TurnCounter getExpiredCounter( GenericRequest request )
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

		TurnCounter current;
		int thisTurn = KoLCharacter.getCurrentRun();
		int currentTurns = thisTurn + turnsUsed - 1;

		TurnCounter expired = null;
		Iterator it = relayCounters.iterator();

		while ( it.hasNext() )
		{
			current = (TurnCounter) it.next();

			if ( current.value > currentTurns )
			{
				continue;
			}

			if ( current.value < thisTurn + 3 )
			{
				it.remove();
			}
			
			if ( current.value >= thisTurn && !current.isExempt( adventureId )
				&& current.lastWarned != thisTurn )
			{
				if ( expired != null )
				{
					KoLmafia.updateDisplay( "Also expiring: " + expired.label +
						" (" + (expired.value - thisTurn) + ")" );
				}
				expired = current;
			}
			current.lastWarned = thisTurn;
		}

		return expired;
	}

	public static final String getUnexpiredCounters()
	{
		TurnCounter current;
		int currentTurns = KoLCharacter.getCurrentRun();

		StringBuffer counters = new StringBuffer();
		Iterator it = relayCounters.iterator();

		while ( it.hasNext() )
		{
			current = (TurnCounter) it.next();

			if ( current.value < currentTurns )
			{
				it.remove();
				continue;
			}

			if ( counters.length() > 0 )
			{
				counters.append( KoLConstants.LINE_BREAK );
			}

			counters.append( current.label );
			counters.append( " (" );
			counters.append( current.value - currentTurns );
			counters.append( ")" );
		}

		return counters.toString();
	}

	public static final void startCounting( final int value, final String label, final String image, boolean save )
	{
		if ( value < 0 )
		{
			return;
		}

		TurnCounter counter = new TurnCounter( value, label, image );

		if ( !relayCounters.contains( counter ) )
		{
			relayCounters.add( counter );
			if ( save )
			{
				TurnCounter.saveCounters();
			}
		}
	}

	public static final void startCounting( final int value, final String label, final String image )
	{
		TurnCounter.startCounting( value, label, image, true );
	}

	public static final void stopCounting( final String label )
	{
		TurnCounter current;
		Iterator it = relayCounters.iterator();

		while ( it.hasNext() )
		{
			current = (TurnCounter) it.next();
			if ( current.label.equals( label ) )
			{
				it.remove();
			}
		}

		TurnCounter.saveCounters();
	}

	public static final boolean isCounting( final String label, final int value )
	{
		TurnCounter current;
		int searchValue = KoLCharacter.getCurrentRun() + value;

		Iterator it = relayCounters.iterator();

		while ( it.hasNext() )
		{
			current = (TurnCounter) it.next();
			if ( current.label.equals( label ) && current.value == searchValue )
			{
				return true;
			}
		}

		return false;
	}

	public static final boolean isCounting( final String label )
	{
		TurnCounter current;
		Iterator it = TurnCounter.relayCounters.iterator();

		while ( it.hasNext() )
		{
			current = (TurnCounter) it.next();
			if ( current.label.equals( label ) )
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
		StringBuffer buf = new StringBuffer();
		TurnCounter current;
		Iterator it = TurnCounter.relayCounters.iterator();

		while ( it.hasNext() )
		{
			current = (TurnCounter) it.next();
			if ( current.value < minTurns || current.value > maxTurns )
			{
				continue;
			}
			if ( checkExempt && current.isExempt( "" ) )
			{
				continue;
			}
			if ( current.label.toLowerCase().indexOf( label ) == -1 )
			{
				continue;
			}
			if ( buf.length() != 0 )
			{
				buf.append( "\t" );
			}
			buf.append( current.label );
		}

		return buf.toString();
	}

	private static final int getTurnsUsed( GenericRequest request )
	{
		if ( !( request instanceof RelayRequest ) )
		{
			return request.getAdventuresUsed();
		}

		String path = request.getPath();

		if ( path.equals( "adventure.php" ) )
		{
			// Assume unknown adventure locations take 1 turn each
			// This is likely not true under the Sea, for example,
			// but it's as good a guess as any we can make.

			return 1;
		}

		int turnMultiplier = 0;

		if ( path.equals( "cook.php" ) )
		{
			turnMultiplier = KoLCharacter.hasChef() ? 0 : 1;
		}
		else if ( path.equals( "cocktail.php" ) )
		{
			turnMultiplier = KoLCharacter.hasBartender() ? 0 : 1;
		}
		else if ( path.equals( "smith.php" ) )
		{
			turnMultiplier = 1;
		}
		else if ( path.equals( "jewelry.php" ) )
		{
			turnMultiplier = 3;
		}

		String action = request.getFormField( "action" );

		if ( action != null )
		{
			if ( action.equals( "wokcook" ) )
			{
				turnMultiplier = 1;
			}
			else if ( action.equals( "pulverize" ) )
			{
				turnMultiplier = 0;
			}
		}

		if ( turnMultiplier == 0 )
		{
			return 0;
		}

		String quantity = request.getFormField( "quantity" );

		if ( quantity == null || quantity.length() == 0 )
		{
			return 0;
		}

		return turnMultiplier * StringUtilities.parseInt( quantity );
	}

	public static final void deleteByHash( final int hash )
	{
		Iterator it = relayCounters.iterator();

		while ( it.hasNext() )
		{
			if ( System.identityHashCode( it.next() ) == hash )
			{
				it.remove();
			}
		}

		TurnCounter.saveCounters();
	}

	public static final Iterator iterator()
	{
		Collections.sort( relayCounters );
		return relayCounters.iterator();
	}
}
