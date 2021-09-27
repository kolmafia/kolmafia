package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.Concoction;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

public class SpacegateEquipmentRequest
	extends CreateItemRequest
{
	public SpacegateEquipmentRequest( final Concoction conc )
	{
		super( "choice.php", conc );
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

		if ( creation.equals( "filter helmet" ) )
		{
			output = "choice.php?whichchoice=1233&option=1";
		}
		else if ( creation.equals( "exo-servo leg braces" ) )
		{
			output = "choice.php?whichchoice=1233&option=2";
		}
		else if ( creation.equals( "rad cloak" ) )
		{
			output = "choice.php?whichchoice=1233&option=3";
		}
		else if ( creation.equals( "gate transceiver" ) )
		{
			output = "choice.php?whichchoice=1233&option=4";
		}
		else if ( creation.equals( "high-friction boots" ) )
		{
			output = "choice.php?whichchoice=1233&option=5";
		}
		else if ( creation.equals( "geological sample kit" ) )
		{
			output = "choice.php?whichchoice=1233&option=6";
		}
		else if ( creation.equals( "botanical sample kit" ) )
		{
			output = "choice.php?whichchoice=1233&option=7";
		}
		else if ( creation.equals( "zoological sample kit" ) )
		{
			output = "choice.php?whichchoice=1233&option=8";
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Cannot create " + creation );
		}

		KoLmafia.updateDisplay( "Creating " + this.getQuantityNeeded() + " " + creation + "..." );

		while ( this.getQuantityNeeded() > 0 && KoLmafia.permitsContinue() )
		{
			this.beforeQuantity = this.createdItem.getCount( KoLConstants.inventory );
			RequestThread.postRequest( new GenericRequest( "place.php?whichplace=spacegate&action=sg_requisition" ) );
			RequestThread.postRequest( new GenericRequest( output ) );
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
			ConcoctionDatabase.refreshConcoctionsNow();
		}
	}
}
