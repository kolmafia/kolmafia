package net.sourceforge.kolmafia.textui.command;

import java.util.Map;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.session.NumberologyManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class NumberologyCommand
	extends AbstractCommand
{
	public NumberologyCommand()
	{
		this.usage = "[?] [N] - list possible results from Calculate the Universe or submit a number";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( !StringUtilities.isNumeric( parameters ) )
		{
			Map<Integer,Integer> results = NumberologyManager.reverseNumberology();
			boolean found = false;
			for ( Map.Entry<Integer,Integer> entry : results.entrySet() )
			{
				int result = entry.getKey();
				String prize = NumberologyManager.numberologyPrize( result );
				if ( prize != NumberologyManager.TRY_AGAIN )
				{
					int seed = entry.getValue();
					RequestLogger.printLine( "[" + result + "] Calculate the Universe with " + seed + " to get: " + prize );
					found = true;
				}
			}
			if ( !found )
			{
				RequestLogger.printLine( "No valid results!" );
			}
			return;
		}

		int result = Math.abs( StringUtilities.parseInt( parameters ) ) % 100;
		String prize = NumberologyManager.numberologyPrize( result );
		MafiaState error = KoLmafiaCLI.isExecutingCheckOnlyCommand ? MafiaState.CONTINUE : MafiaState.ERROR;

		// If it's "Try Again", don't waste our time
		if ( prize == NumberologyManager.TRY_AGAIN )
		{
			KoLmafia.updateDisplay( error, "Result " + result + " is " + prize );
			return;
		}

		Map<Integer,Integer> results = null;
		int adventureDelta = 0;
		while ( adventureDelta < 100 )
		{
			results = NumberologyManager.reverseNumberology( adventureDelta, 0 );
			if ( results.containsKey( result ) )
			{
				break;
			}
			adventureDelta++;
		}

		// If the prize is available with current adventures and spleen, do it.
		if ( adventureDelta == 0 )
		{
			if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( "\"numberology " + result + "\" (" + prize + ") is currently available." );
				return;
			}

			int seed = results.get( result );

			NumberologyManager.calculateTheUniverse( seed );
			return;
		}

		int spleenMin = KoLCharacter.getSpleenUse();
		int spleenMax = KoLCharacter.getSpleenLimit();
		int spleenDelta = spleenMin + 1;
		while ( spleenDelta <= spleenMax )
		{
			results = NumberologyManager.reverseNumberology( 0, spleenDelta - spleenMin );
			if ( results.containsKey( result ) )
			{
				break;
			}
			spleenDelta++;
		}

		// This is probably not possible, but...
		if ( adventureDelta == 100 && spleenDelta >= spleenMax )
		{
			KoLmafia.updateDisplay( error, "Result " + result + " not found!" );
			return;
		}

		// Give forecast
		StringBuilder buffer = new StringBuilder( "\"numberology " );
		buffer.append( result );
		buffer.append( "\" (" );
		buffer.append( prize );
		buffer.append( ") is not currently available but will be in" );

		if ( adventureDelta != 100 )
		{
			buffer.append( " " );
			buffer.append( adventureDelta );
			buffer.append( " turn" );
			if ( adventureDelta != 1 )
			{
				buffer.append( "s" );
			}
		}

		if ( spleenDelta <= spleenMax )
		{
			if ( adventureDelta != 100 )
			{
				buffer.append( " or" );
			}
			buffer.append( " " );
			buffer.append( ( spleenDelta - spleenMin ) );
			buffer.append( " spleen" );
		}

		buffer.append( "." );
		KoLmafia.updateDisplay( error, buffer.toString() );
	}
}
