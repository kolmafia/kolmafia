package net.sourceforge.kolmafia.textui.command;

import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

public class AsdonMartinCommand
	extends AbstractCommand
{
	private static final Object [][] DRIVESTYLE = new Object[][]
	{
		{ "Obnoxiously", IntegerPool.get( 0 ), EffectPool.get( EffectPool.OBNOXIOUSLY ) },
		{ "Stealthily", IntegerPool.get( 1 ), EffectPool.get( EffectPool.STEALTHILY ) },
		{ "Wastefully", IntegerPool.get( 2 ), EffectPool.get( EffectPool.WASTEFULLY ) },
		{ "Safely", IntegerPool.get( 3 ), EffectPool.get( EffectPool.SAFELY  ) },
		{ "Recklessly", IntegerPool.get( 4 ), EffectPool.get( EffectPool.RECKLESSLY ) },
		{ "Quickly", IntegerPool.get( 5 ), EffectPool.get( EffectPool.QUICKLY ) },
		{ "Intimidatingly", IntegerPool.get( 6 ), EffectPool.get( EffectPool.INTIMIDATINGLY ) },
		{ "Observantly", IntegerPool.get( 7 ), EffectPool.get( EffectPool.OBSERVANTLY ) },
		{ "Waterproofly", IntegerPool.get( 8 ), EffectPool.get( EffectPool.WATERPROOFLY ) },
	};

	public AsdonMartinCommand()
	{
		this.usage = " drive style|clear, fuel [#] item name  - Get drive buff or convert items to fuel";
	}

	private static int findDriveStyle( final String name )
	{
		for ( int i = 0; i < DRIVESTYLE.length; ++i )
		{
			if ( name.equalsIgnoreCase( (String) DRIVESTYLE[i][0] ) )
			{
				Integer index = (Integer) DRIVESTYLE[i][1];
				return index.intValue();
			}
		}
		return -1;
	}

	private static String driveStyleName( final int index )
	{
		if ( index < 0 || index > 8 )
		{
			return null;
		}
		return (String) DRIVESTYLE[index][0];
	}

	private static int currentDriveStyle()
	{
		List<AdventureResult> active = KoLConstants.activeEffects;
		for ( int i = 0; i < DRIVESTYLE.length; ++i )
		{
			if ( active.contains( DRIVESTYLE[i][2] ) )
			{
				Integer index = (Integer) DRIVESTYLE[i][1];
				return index.intValue();
			}
		}
		return -1;
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( CampgroundRequest.getCurrentWorkshedItem().getItemId() != ItemPool.ASDON_MARTIN )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You do not have an Asdon Martin" );
			return;
		}

		String[] params = parameters.trim().split( "\\s+" );
		String command = params[0];

		if ( command.equals( "drive" ) )
		{
			if ( params.length < 2 )
			{
				RequestLogger.printLine( "Usage: asdonmartin " + this.usage );
				return;
			}
			String driveStyle = params[1];
			if ( driveStyle.equalsIgnoreCase( "clear" ) )
			{
				int currentStyle = AsdonMartinCommand.currentDriveStyle();
				if ( currentStyle == -1 )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "You do not have a driving style" );
					return;
				}
				String request = "campground.php?pwd&preaction=undrive&stop=Stop+Driving+" +
					AsdonMartinCommand.driveStyleName( currentStyle );
				// Remove driving style
				RequestThread.postRequest( new GenericRequest( request ) );
				return;
			}
			else
			{
				int style = AsdonMartinCommand.findDriveStyle( driveStyle );
				if ( style == -1 )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Driving style " + driveStyle + " not recognised" );
					return;
				}
								
				if ( CampgroundRequest.getFuel() < 37 )
				{
					RequestLogger.printLine( "You haven't got enough fuel" );
					return;
				}

				int currentStyle = AsdonMartinCommand.currentDriveStyle();
				if( currentStyle == -1 )
				{
					// Get buff, none to remove or extend
					RequestThread.postRequest( new GenericRequest( "campground.php?preaction=drive&whichdrive=" + style ) );
					return;
				}
				else if ( currentStyle == style )
				{
					// Extend buff
					String request = "campground.php?pwd&preaction=drive&whichdrive=" + style + "&more=Drive+More+" +
						AsdonMartinCommand.driveStyleName( style );
					RequestThread.postRequest( new GenericRequest( request ) );
					return;
				}
				else
				{
					// Remove buff
					String request = "campground.php?pwd&preaction=undrive&stop=Stop+Driving+" +
						AsdonMartinCommand.driveStyleName( currentStyle );
					RequestThread.postRequest( new GenericRequest( request ) );
					// Get new buff
					RequestThread.postRequest( new GenericRequest( "campground.php?preaction=drive&whichdrive=" + style ) );
					return;
				}
			}
		}
		else if ( command.equals( "fuel" ) )
		{
			String param = parameters.substring( 5 );
			AdventureResult item = ItemFinder.getFirstMatchingItem( param, true, null, Match.ASDON );
			if ( item == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, param + " cannot be used as fuel." );
				return;
			}
			if ( !InventoryManager.checkpointedRetrieveItem( item ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have enough " + item.getDataName() + "." );
				return;
			}
			CampgroundRequest request = new CampgroundRequest( "fuelconvertor" );
			request.addFormField( "qty", String.valueOf( item.getCount() ) );
			request.addFormField( "iid", String.valueOf( item.getItemId() ) );
			RequestThread.postRequest( request );
			return;
		}

		RequestLogger.printLine( "Usage: asdonmartin " + this.usage );
	}
}
