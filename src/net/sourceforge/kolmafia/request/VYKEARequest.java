package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.VYKEACompanionData;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class VYKEARequest
	extends CreateItemRequest
{
	public VYKEARequest( Concoction conc )
	{
		super( "inv_use.php", conc );
		this.addFormField( "whichitem", String.valueOf( ItemPool.VYKEA_INSTRUCTIONS ) );
	}

	@Override
	public void reconstructFields()
	{
		this.constructURLString( "inv_use.php" );
		this.addFormField( "whichitem", String.valueOf( ItemPool.VYKEA_INSTRUCTIONS ) );
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		// Will redirect to choice.php if successful
		return true;
	}

	@Override
	public boolean noCreation()
	{
		return true;
	}

	@Override
	public int getQuantityPossible()
	{
		// You can't create more than one VYKEA companion
		return Math.min( this.quantityPossible, 1 );
	}

	@Override
	public void run()
	{
		// Make sure you don't already have a companion
		if ( VYKEACompanionData.currentCompanion() != VYKEACompanionData.NO_COMPANION )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You already have a VYKEA companion. It would get jealous and turn on you if you build another one today." );
			return;
		}

		// Make sure your VYKEA hex key is in inventory
		if ( !InventoryManager.retrieveItem( ItemPool.VYKEA_HEX_KEY, 1 ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need a VYKEA hex key in order to build a VYKEA companion." );
			return;
		}

		Concoction concoction = this.concoction;
		AdventureResult[] ingredients = concoction.getIngredients();

		if ( ingredients.length < 3 || ingredients[0].getItemId() != ItemPool.VYKEA_INSTRUCTIONS )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "VYKEA companion recipe for '" + concoction.getName() + "' is invalid." );
			return;
		}

		// Get the necessary ingredients
		if ( !this.makeIngredients() )
		{
			return;
		}

		// You need at least 5 planks, 5 rails, and 5 brackets in order
		// to start construction, even though no companion uses all
		// three kinds of components
		if ( !InventoryManager.retrieveItem( ItemPool.VYKEA_PLANK, 5 ) ||
		     !InventoryManager.retrieveItem( ItemPool.VYKEA_RAIL, 5 ) ||
		     !InventoryManager.retrieveItem( ItemPool.VYKEA_BRACKET, 5 ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need a 5 planks, 5 rails, and 5 brackets in order to start construction." );
			return;
		}

		// Make a companion!

		int index = 1;

		// Start by "using" the VYKEA instructions.
		super.run();

		// Iterate over the ingredients and feed them to the appropriate choice
		while ( index < ingredients.length )
		{
			int choice = ChoiceManager.lastChoice;
			int option = 0;

			if ( choice == 0 )
			{
				// If we are not in choice.php, it failed for some reason.
				// Perhaps we could figure out why and give a more informative error.
				KoLmafia.updateDisplay( MafiaState.ERROR, "VYKEA companion creation failed." );
				return;
			}

			AdventureResult ingredient = ingredients[ index ];
			int itemId = ingredient.getItemId();
			int count = ingredient.getCount();

			switch ( choice )
			{
			case 1120:
				switch ( itemId )
				{
				case ItemPool.VYKEA_PLANK:
					option = 1;
					index++;
					break;
				case ItemPool.VYKEA_RAIL:
					option = 2;
					index++;
					break;
				}
				break;
			case 1121:
				switch ( itemId )
				{
				case ItemPool.VYKEA_FRENZY_RUNE:
					option = 1;
					index++;
					break;
				case ItemPool.VYKEA_BLOOD_RUNE:
					option = 2;
					index++;
					break;
				case ItemPool.VYKEA_LIGHTNING_RUNE:
					option = 3;
					index++;
					break;
				default:
					option = 6;
					break;
				}
				break;
			case 1122:
				if ( itemId == ItemPool.VYKEA_DOWEL )
				{
					switch ( count )
					{
					case 1:
						option = 1;
						break;
					case 11:
						option = 2;
						break;
					case 23:
						option = 3;
						break;
					case 37:
						option = 4;
						break;
					}
					index++;
				}
				else
				{
					option = 6;
				}
				break;
			case 1123:
				switch ( itemId )
				{
				case ItemPool.VYKEA_PLANK:
					option = 1;
					index++;
					break;
				case ItemPool.VYKEA_RAIL:
					option = 2;
					index++;
					break;
				case ItemPool.VYKEA_BRACKET:
					option = 3;
					index++;
					break;
				}
				break;
			}

			if ( option == 0 )
			{
				// If we couldn't pick an option, the recipe is incorrect
				KoLmafia.updateDisplay( MafiaState.ERROR, "VYKEA companion recipe is incorrect." );
				return;
			}

			GenericRequest choiceRequest = new GenericRequest( "choice.php" );
			choiceRequest.addFormField( "whichchoice", String.valueOf( choice ) );
			choiceRequest.addFormField( "option", String.valueOf( option ) );
			choiceRequest.run();
		}

		KoLmafia.updateDisplay( "Successfully created " + this.getName() );
	}
}
