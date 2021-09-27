package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.Modifiers.ModifierList;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

public class PantogramRequest
	extends GenericRequest
{
	private static final Pattern urlPattern = Pattern.compile( "m=(\\d)&e=(\\d)&s1=(.*)?&s2=(.*)?&s3=(.*)" );

	public PantogramRequest()
	{
		super( "choice.php" );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "choice.php" ) || !urlString.contains( "whichchoice=1270" ) )
		{
			return;
		}

		if ( !responseText.contains( "You acquire an item" ) )
		{
			// Maybe return an error here later
			return;
		}
		ResultProcessor.processResult( ItemPool.get( ItemPool.PANTOGRAM_PANTS, 1 ) );

		Matcher matcher = urlPattern.matcher( urlString );
		ModifierList modList = new ModifierList();
		if ( matcher.find() )
		{
			// If the matcher doesn't find anything, then the user probably manually constructed
			// the URL in a different order.  For now at least, that is not handled.
			String stat = matcher.group( 1 );
			if ( stat.equals( "1" ) )
			{
				modList.addModifier( "Muscle", "10" );
			}
			else if ( stat.equals( "2" ) )
			{
				modList.addModifier( "Mysticality", "10" );
			}
			else if ( stat.equals( "3" ) )
			{
				modList.addModifier( "Moxie", "10" );
			}

			String element = matcher.group( 2 );
			if ( element.equals( "1" ) )
			{
				modList.addModifier( "Hot Resistance", "2" );
			}
			else if ( element.equals( "2" ) )
			{
				modList.addModifier( "Cold Resistance", "2" );
			}
			else if ( element.equals( "3" ) )
			{
				modList.addModifier( "Spooky Resistance", "2" );
			}
			else if ( element.equals( "4" ) )
			{
				modList.addModifier( "Sleaze Resistance", "2" );
			}
			else if ( element.equals( "5" ) )
			{
				modList.addModifier( "Stench Resistance", "2" );
			}

			// Bottom left
			String slot1 = matcher.group( 3 );
			if ( slot1.startsWith( "-1" ) )
			{
				modList.addModifier( "Maximum HP", "40" );
			}
			else if ( slot1.startsWith( "-2" ) )
			{
				modList.addModifier( "Maximum MP", "20" );
			}
			else if ( slot1.startsWith( "464" ) )
			{
				modList.addModifier( "HP Regen Min", "5" );
				modList.addModifier( "HP Regen Max", "10" );
				ResultProcessor.removeItem( ItemPool.RED_PIXEL_POTION );
			}
			else if ( slot1.startsWith( "830" ) )
			{
				modList.addModifier( "HP Regen Min", "5" );
				modList.addModifier( "HP Regen Max", "15" );
				ResultProcessor.removeItem( ItemPool.ROYAL_JELLY );
			}
			else if ( slot1.startsWith( "2438" ) )
			{
				modList.addModifier( "HP Regen Min", "10" );
				modList.addModifier( "HP Regen Max", "20" );
				ResultProcessor.removeItem( ItemPool.MASSAGE_OIL );
			}
			else if ( slot1.startsWith( "1658" ) )
			{
				modList.addModifier( "MP Regen Min", "5" );
				modList.addModifier( "MP Regen Max", "10" );
				ResultProcessor.removeItem( ItemPool.CHERRY_CLOACA_COLA );
			}
			else if ( slot1.startsWith( "5789" ) )
			{
				modList.addModifier( "MP Regen Min", "5" );
				modList.addModifier( "MP Regen Max", "15" );
				ResultProcessor.removeItem( ItemPool.BUBBLIN_CRUDE );
			}
			else if ( slot1.startsWith( "8455" ) )
			{
				modList.addModifier( "MP Regen Min", "10" );
				modList.addModifier( "MP Regen Max", "20" );
				ResultProcessor.removeItem( ItemPool.GLOWING_NEW_AGE_CRYSTAL );
			}
			else if ( slot1.startsWith( "705" ) )
			{
				modList.addModifier( "Mana Cost", "-3" );
				ResultProcessor.removeItem( ItemPool.BACONSTONE );
			}

			// Bottom right
			String slot2 = matcher.group( 4 );
			if ( slot2.startsWith( "-1" ) )
			{
				modList.addModifier( "Weapon Damage", "20" );
			}
			else if ( slot2.startsWith( "-2" ) )
			{
				modList.addModifier( "Spell Damage Percent", "20" );
			}
			else if ( slot2.startsWith( "173" ) )
			{
				modList.addModifier( "Meat Drop", "30" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.TACO_SHELL, -1 ) );
			}
			else if ( slot2.startsWith( "706" ) )
			{
				modList.addModifier( "Meat Drop", "60" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.PORQUOISE, -1 ) );
			}
			else if ( slot2.startsWith( "80" ) )
			{
				modList.addModifier( "Item Drop", "15" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.GRAVY_BOAT, -1 ) );
			}
			else if ( slot2.startsWith( "7338" ) )
			{
				modList.addModifier( "Item Drop", "30" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.TINY_DANCER, -1 ) );
			}
			else if ( slot2.startsWith( "747" ) )
			{
				modList.addModifier( "Experience (Muscle)", "3" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.KNOB_FIRECRACKER, -3 ) );
			}
			else if ( slot2.startsWith( "559" ) )
			{
				modList.addModifier( "Experience (Mysticality)", "3" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.CAN_LID, -3 ) );
			}
			else if ( slot2.startsWith( "27" ) )
			{
				modList.addModifier( "Experience (Moxie)", "3" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.SPIDER_WEB, -3 ) );
			}
			else if ( slot2.startsWith( "7327" ) )
			{
				modList.addModifier( "Experience Percent (Muscle)", "25" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.SYNTHETIC_MARROW, -5 ) );
			}
			else if ( slot2.startsWith( "7324" ) )
			{
				modList.addModifier( "Experience Percent (Mysticality)", "25" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.HAUNTED_BATTERY, -5 ) );
			}
			else if ( slot2.startsWith( "7330" ) )
			{
				modList.addModifier( "Experience Percent (Moxie)", "25" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.FUNK, -5 ) );
			}

			// Bottom center
			String slot3 = matcher.group( 5 );
			if ( slot3.startsWith( "-1" ) )
			{
				modList.addModifier( "Combat Rate", "-5" );
			}
			else if ( slot3.startsWith( "-2" ) )
			{
				modList.addModifier( "Combat Rate", "5" );
			}
			else if ( slot3.startsWith( "70%" ) )
			{
				modList.addModifier( "Initiative", "50" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.BAR_SKIN, -1 ) );
			}
			else if ( slot3.startsWith( "704" ) )
			{
				modList.addModifier( "Critical Hit Percent", "10" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.HAMETHYST, -1 ) );
			}
			else if ( slot3.startsWith( "865" ) )
			{
				modList.addModifier( "Familiar Weight", "10" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.LEAD_NECKLACE, -11 ) );
			}
			else if ( slot3.startsWith( "6851" ) )
			{
				modList.addModifier( "Candy Drop", "100" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.HUGE_BOWL_OF_CANDY, -1 ) );
			}
			else if ( slot3.startsWith( "3495" ) )
			{
				// Makes you a better diver... not handled yet
				//list.addModifier( "", "" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.SEA_SALT_CRYSTAL, -11 ) );
			}
			else if ( slot3.startsWith( "9008" ) )
			{
				modList.addModifier( "Fishing Skill", "5" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.WRIGGLING_WORM, -1 ) );
			}
			else if ( slot3.startsWith( "1907" ) )
			{
				modList.addModifier( "Pool Skill", "5" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.EIGHT_BALL, -15 ) );
			}
			else if ( slot3.startsWith( "14" ) )
			{
				// Purple Avatar
				ResultProcessor.processResult( ItemPool.get( ItemPool.MOXIE_WEED, -99 ) );
			}
			else if ( slot3.startsWith( "24" ) )
			{
				// Occasional Hilarity
				modList.addModifier( "Drops Items", "true" );
				ResultProcessor.processResult( ItemPool.get( ItemPool.TEN_LEAF_CLOVER, -1 ) );
			}

			modList.addModifier( "Lasts Until Rollover", "true" );

			Preferences.setString( "_pantogramModifier", modList.toString() );
			Modifiers.overrideModifier( "Item:[" + ItemPool.PANTOGRAM_PANTS + "]", modList.toString() );
			KoLCharacter.recalculateAdjustments();
			KoLCharacter.updateStatus();
		}
	}
}
