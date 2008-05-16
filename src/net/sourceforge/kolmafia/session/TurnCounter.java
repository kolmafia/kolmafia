/**
 * 
 */
package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TurnCounter
{
	private static final ArrayList relayCounters = new ArrayList();

	private final int value;
	private final String image;
	private final String label;

	public TurnCounter( final int value, final String label, final String image )
	{
		this.value = KoLCharacter.getCurrentRun() + value;
		this.label = label;
		this.image = image;
	}

	public boolean isExempt( final String adventureId )
	{
		if ( this.label.equals( "Wormwood" ) )
		{
			return adventureId.equals( "151" ) || adventureId.equals( "152" ) || adventureId.equals( "153" );
		}

		return false;
	}

	public String getLabel()
	{
		return this.label;
	}

	public String getImage()
	{
		return this.image;
	}

	public boolean equals( final Object o )
	{
		if ( o == null || !( o instanceof TurnCounter ) )
		{
			return false;
		}

		return this.label.equals( ( (TurnCounter) o ).label ) && this.value == ( (TurnCounter) o ).value;
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
			String name = tokens.nextToken();
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
		KoLAdventure adventure = AdventureDatabase.getAdventureByURL( request.getURLString() );

		String adventureId;
		int turnsUsed;
		
		if ( adventure == null )
		{
			adventureId = "";
			turnsUsed = TurnCounter.getTurnsUsed( request );
		}
		else
		{
			adventureId = adventure.getAdventureId();
			turnsUsed = adventure.getRequest().getAdventuresUsed();
		}

		if ( turnsUsed == 0 )
		{
			return null;
		}
		
		TurnCounter current;
		int currentTurns = KoLCharacter.getCurrentRun() + turnsUsed - 1;
	
		TurnCounter expired = null;
		Iterator it = relayCounters.iterator();
	
		while ( it.hasNext() )
		{
			current = (TurnCounter) it.next();
	
			if ( current.value > currentTurns )
			{
				continue;
			}
	
			it.remove();
			if ( current.value <= currentTurns && !current.isExempt( adventureId ) )
			{
				expired = current;
			}
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

	private static final int getTurnsUsed( GenericRequest request )
	{
		if ( !( request instanceof RelayRequest ) )
		{
			return request.getAdventuresUsed();
		}
		
		String path = request.getPath();
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
	
}
