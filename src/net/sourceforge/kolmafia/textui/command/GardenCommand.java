package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest.CropType;

import net.sourceforge.kolmafia.session.Limitmode;

public class GardenCommand
	extends AbstractCommand
{
	public GardenCommand()
	{
		this.usage = " [pick] - get status of garden, or harvest it.";
	}

	private boolean checkMushroomGarden( CropType cropType )
	{
		if ( cropType != CropType.MUSHROOM )
		{
			KoLmafia.updateDisplay( "You don't have a mushroom garden." );
			return false;
		}
		if ( Preferences.getBoolean( "_mushroomGardenVisited" ) )
		{
			KoLmafia.updateDisplay( "You've already dealt with your mushroom garden today." );
			return false;
		}
		if ( KoLCharacter.isFallingDown() )
		{
			KoLmafia.updateDisplay( "You are too drunk to enter your mushroom garden." );
			return false;
		}
		if ( KoLCharacter.getAdventuresLeft() <= 0 )
		{
			KoLmafia.updateDisplay( "You need an available turn to fight through piranha plants." );
			return false;
		}
		return true;
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( KoLCharacter.isEd() || KoLCharacter.inNuclearAutumn() || Limitmode.limitCampground() )
		{
			KoLmafia.updateDisplay( "You can't get to your campground to visit your garden." );
			return;
		}

		AdventureResult crop = CampgroundRequest.getCrop();
		CropType cropType = CampgroundRequest.getCropType( crop );
		
		if ( crop == null )
		{
			KoLmafia.updateDisplay( "You don't have a garden." );
			return;
		}

		if ( parameters.equals( "" ) )
		{
			int count = crop.getPluralCount();
			String name = crop.getPluralName();
			String gardenType = cropType.toString();
			KoLmafia.updateDisplay( "Your " + gardenType + " garden has " + count + " " + name + " in it." );
			return;
		}

		if ( parameters.equals( "fertilize" ) )
		{
			// Mushroom garden only
			if ( checkMushroomGarden( cropType ) )
			{
				CampgroundRequest.harvestMushrooms( false );
			}
			return;
		}

		if ( parameters.equals( "pick" ) )
		{
			// Mushroom garden only
			if ( cropType == CropType.MUSHROOM && checkMushroomGarden( cropType ) )
			{
				CampgroundRequest.harvestMushrooms( true );
				return;
			}

			int count = crop.getCount();
			if ( count == 0 )
			{
				KoLmafia.updateDisplay( "There is nothing ready to pick in your garden." );
				return;
			}

			CampgroundRequest.harvestCrop();
			return;
		}
	}
}
