package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.Concoction;

import net.sourceforge.kolmafia.preferences.Preferences;

public class BarrelShrineRequest
	extends CreateItemRequest
{
	public BarrelShrineRequest( final Concoction conc )
	{
		super( "choice.php", conc );
	}

	public static boolean availableBarrelItem( final String itemName )
	{
		if ( Preferences.getBoolean( "barrelShrineUnlocked" ) )
		{
			if ( itemName.equals( "barrel lid" ) && !Preferences.getBoolean( "_barrelPrayer" ) && !Preferences.getBoolean( "prayedForProtection" ) )
			{
				return true;
			}
			if ( itemName.equals( "barrel hoop earring" ) && !Preferences.getBoolean( "_barrelPrayer" ) && !Preferences.getBoolean( "prayedForGlamour" ) )
			{
				return true;
			}
			if ( itemName.equals( "bankruptcy barrel" ) && !Preferences.getBoolean( "_barrelPrayer" ) && !Preferences.getBoolean( "prayedForVigor" ) )
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public void run()
	{
		if ( !KoLmafia.permitsContinue() || this.getQuantityNeeded() <= 0 )
		{
			return;
		}

		String creation = this.getName();
		String output = null;

		if ( creation.equals( "barrel lid" ) )
		{
			output = "choice.php?whichchoice=1100&option=1";
		}
		else if ( creation.equals( "barrel hoop earring" ) )
		{
			output = "choice.php?whichchoice=1100&option=2";
		}
		else if ( creation.equals( "bankruptcy barrel" ) )
		{
			output = "choice.php?whichchoice=1100&option=3";
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Cannot create " + creation );
		}

		KoLmafia.updateDisplay( "Creating " + this.getQuantityNeeded() + " " + creation + "..." );

		while ( this.getQuantityNeeded() > 0 && KoLmafia.permitsContinue() )
		{
			this.beforeQuantity = this.createdItem.getCount( KoLConstants.inventory );
			GenericRequest request = new GenericRequest( "da.php?barrelshrine=1" ) ;
			RequestThread.postRequest( request );
			request.constructURLString( output );
			RequestThread.postRequest( request );
			int createdQuantity = this.createdItem.getCount( KoLConstants.inventory ) - this.beforeQuantity;

			if ( createdQuantity == 0 )
			{
				if ( KoLmafia.permitsContinue() )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Creation failed, no results detected." );
				}

				return;
			}

			KoLmafia.updateDisplay( "Successfully created " + creation + " (" + createdQuantity + ")" );
			this.quantityNeeded -= createdQuantity;
		}
	}
}
