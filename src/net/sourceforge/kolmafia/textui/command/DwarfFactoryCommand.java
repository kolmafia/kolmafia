package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.DwarfContraptionRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;

public class DwarfFactoryCommand
	extends AbstractCommand
{
	public DwarfFactoryCommand()
	{
		this.usage = " report <digits> - Given a string of 7 dwarven digits, report on factory.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String[] tokens = parameters.split( "\\s+" );
		if ( tokens.length < 1 )
		{
			return;
		}

		String option = tokens[ 0 ];

		if ( option.equals( "vacuum" ) )
		{
			String itemString = parameters.substring( 6 ).trim();
			AdventureResult item = ItemFinder.getFirstMatchingItem( itemString );
			if ( item == null )
			{
				return;
			}

			DwarfContraptionRequest request = new DwarfContraptionRequest( "dochamber" );
			request.addFormField( "howmany", String.valueOf( item.getCount() ) );
			request.addFormField( "whichitem", String.valueOf( item.getItemId() ) );
			RequestThread.postRequest( request );

			return;
		}

		if ( option.equals( "check" ) )
		{
			DwarfFactoryRequest.check( false );
			return;
		}

		if ( option.equals( "report" ) )
		{
			if ( tokens.length >= 2 )
			{
				String digits = tokens[ 1 ].trim().toUpperCase();
				DwarfFactoryRequest.report( digits );
			}
			else
			{
				DwarfFactoryRequest.report();
			}
			return;
		}

		if ( option.equals( "setdigits" ) )
		{
			String digits = "";
			if ( tokens.length >= 2 )
			{
				digits = tokens[ 1 ].trim().toUpperCase();
			}

			if ( digits.length() != 7 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Must supply a 7 character digit string" );
				return;
			}
			DwarfFactoryRequest.setDigits( digits );
			return;
		}

		if ( option.equals( "solve" ) )
		{
			DwarfFactoryRequest.solve();
			return;
		}
	}
}
