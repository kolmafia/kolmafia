package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.session.SorceressLairManager;

import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.StarChartRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public abstract class UseLinkDecorator
{
	private static final Pattern ACQUIRE_PATTERN =
		Pattern.compile( "(You acquire|O hai, I made dis)([^<]*?<b>.*?</b>.*?)</td>", Pattern.DOTALL );

	public static final void decorate( final String location, final StringBuffer buffer )
	{
		if ( buffer.indexOf( "You acquire" ) == -1 &&
		     buffer.indexOf( "O hai, I made dis" ) == -1)
		{
			return;
		}

		// No use link if you get the item via pickpocketing; you're still in battle
		if ( buffer.indexOf( "deftly slip your fingers" ) != -1 )
		{
			return;
		}

		String text = buffer.toString();
		buffer.setLength( 0 );

		Matcher useLinkMatcher = UseLinkDecorator.ACQUIRE_PATTERN.matcher( text );

		int specialLinkId = 0;
		String specialLinkText = null;

		while ( useLinkMatcher.find() )
		{
			String itemName = useLinkMatcher.group( 2 );
			if ( itemName.indexOf( "<br>" ) != -1 )
			{
				itemName = itemName.substring( 0, itemName.indexOf( "<br>" ) );
			}

			int itemCount = itemName.indexOf( ":" ) != -1 ? 1 : 2;

			if ( itemCount == 1 )
			{
				itemName = itemName.substring( itemName.indexOf( ":" ) + 1 ).replaceAll( "<.*?>", "" ).trim();
			}
			else
			{
				itemName = itemName.replaceAll( "<.*?>", "" );
				itemName = itemName.substring( itemName.indexOf( " " ) + 1 ).trim();
			}

			int itemId = ItemDatabase.getItemId( itemName, itemCount, false );

			String useType = null;
			String useLocation = null;

			boolean addCreateLink =
				location != null && location.indexOf( "combine.php" ) == -1 && location.indexOf( "cocktail.php" ) == -1 && location.indexOf( "cook.php" ) == -1 && location.indexOf( "paster" ) == -1 && location.indexOf( "smith" ) == -1;

			AdventureResult creation = null;
			CreateItemRequest irequest = null;

			int mixingMethod = KoLConstants.NOCREATE;
			int consumeMethod = ItemDatabase.getConsumptionType( itemId );

			// Retrieve the known ingredient uses for the item.
			SortedListModel creations = ConcoctionDatabase.getKnownUses( itemId );

			// If you find goat cheese, let the trapper link handle it.
			addCreateLink &= !creations.isEmpty() && itemId != 322;

			// If you find ore, let the trapper link handle it.
			addCreateLink &= itemId != 363 || itemId != 364 || itemId != 365;

			// Dictionaries and bridges should link to the chasm quest.
			addCreateLink &=
				itemId != FightRequest.DICTIONARY1.getItemId() && itemId != AdventureRequest.BRIDGE.getItemId();

			// Enchanted beans are primarily used for the beanstalk quest.
			addCreateLink &=
				itemId != KoLAdventure.BEAN.getItemId() || KoLCharacter.getLevel() < 10 || KoLCharacter.hasItem( KoLAdventure.SOCK ) || KoLCharacter.hasItem( KoLAdventure.ROWBOAT );

			// Skip items which are multi-use or are mp restores.
			addCreateLink &= consumeMethod != KoLConstants.CONSUME_MULTIPLE && consumeMethod != KoLConstants.MP_RESTORE;

			if ( addCreateLink )
			{
				addCreateLink = false;

				for ( int i = 0; !addCreateLink && i < creations.size(); ++i )
				{
					creation = (AdventureResult) creations.get( i );
					mixingMethod = ConcoctionDatabase.getMixingMethod( creation.getItemId() );

					// Only accept if it's a creation method that the editor kit
					// currently understands and links.

					switch ( mixingMethod )
					{
					case KoLConstants.NOCREATE:
					case KoLConstants.PIXEL:
					case KoLConstants.ROLLING_PIN:
					case KoLConstants.CRIMBO05:
					case KoLConstants.CLOVER:
					case KoLConstants.STILL_BOOZE:
					case KoLConstants.STILL_MIXER:
					case KoLConstants.SMITH:
					case KoLConstants.SMITH_WEAPON:
					case KoLConstants.SMITH_ARMOR:
					case KoLConstants.CATALYST:
						continue;
					}

					irequest = CreateItemRequest.getInstance( creation.getItemId() );
					addCreateLink =
						ConcoctionDatabase.isPermittedMethod( mixingMethod ) && irequest != null && irequest.getQuantityPossible() > 0;
				}
			}

			// Certain items get use special links to minimize the amount
			// of scrolling to find the item again.

			if ( location.startsWith( "inventory.php" ) )
			{
				switch ( itemId )
				{
				case 1423:
				case 1424:
				case 1425:
				case 1426:
				case 1427:

					specialLinkId = itemId;
					specialLinkText = "squeeze";
					break;

				case 2079:
				case 2080:
				case 2081:
				case 2083:
				case 2095:

					specialLinkId = itemId;
					specialLinkText = "fold";
					break;

				case 2221:
				case 2222:
				case 2223:
				case 2224:
				case 2225:
				case 2226:

					// specialLinkId = itemId;
					// specialLinkText = "melt";
					break;
				}
			}

			// If you can add a creation link, then add one instead.
			// That way, the player can click and KoLmafia will save
			// the player a click or two (well, if they trust it).

			if ( addCreateLink )
			{
				switch ( mixingMethod )
				{
				case KoLConstants.STARCHART:
					useType =
						StarChartRequest.CHART.getCount( KoLConstants.inventory ) + "," + StarChartRequest.STARS.getCount( KoLConstants.inventory ) + "," + StarChartRequest.LINES.getCount( KoLConstants.inventory );
					useLocation = "starchart.php";
					break;

				case KoLConstants.COMBINE:
					useType = "combine";
					useLocation = KoLCharacter.inMuscleSign() ? "knoll.php?place=paster" : "combine.php";
					break;

				case KoLConstants.MIX:
				case KoLConstants.MIX_SPECIAL:
				case KoLConstants.MIX_SUPER:
					useType = "mix";
					useLocation = "cocktail.php";
					break;

				case KoLConstants.COOK:
				case KoLConstants.COOK_REAGENT:
				case KoLConstants.SUPER_REAGENT:
				case KoLConstants.COOK_PASTA:
					useType = "cook";
					useLocation = "cook.php";
					break;

				case KoLConstants.JEWELRY:
				case KoLConstants.EXPENSIVE_JEWELRY:
					useType = "jewelry";
					useLocation = "jewelry.php";
					break;
				}
			}
			else
			{
				switch ( consumeMethod )
				{
				case KoLConstants.GROW_FAMILIAR:

					if ( itemId == 275 )
					{
						useType = "council";
						useLocation = "council.php";
					}

					break;

				case KoLConstants.CONSUME_EAT:

					if ( itemId == 322 )
					{
						AdventureResult cheese = new AdventureResult( itemId, 1 );
						useType = String.valueOf( cheese.getCount( KoLConstants.inventory ) );
						useLocation = "trapper.php";
					}
					else
					{
						useType = KoLCharacter.canEat() ? "eat" : null;
						useLocation = "inv_eat.php?pwd=" + GenericRequest.passwordHash + "&which=1&whichitem=";
					}

					break;

				case KoLConstants.CONSUME_DRINK:
					useType = KoLCharacter.canDrink() ? "drink" : null;
					useLocation = "inv_booze.php?pwd=" + GenericRequest.passwordHash + "&which=1&whichitem=";
					break;

				case KoLConstants.CONSUME_MULTIPLE:
				case KoLConstants.HP_RESTORE:
				case KoLConstants.MP_RESTORE:
				case KoLConstants.HPMP_RESTORE:

					AdventureResult result = new AdventureResult( itemId, 1 );
					itemCount =
						Math.min( UseItemRequest.maximumUses( itemId ), result.getCount( KoLConstants.inventory ) );

					if ( itemCount == 0 )
					{
						useType = null;
						useLocation = null;
					}
					else if ( itemCount == 1 )
					{
						String page = ( consumeMethod == KoLConstants.CONSUME_MULTIPLE ) ? "3" : "1";
						useType = "use";
						useLocation = "inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=" + page + "&whichitem=";
					}
					else if ( Preferences.getBoolean( "relayUsesInlineLinks" ) )
					{
						useType = "use";
						useLocation = "#";
					}
					else
					{
						useType = "use";
						useLocation = "multiuse.php?passitem=";
					}

					break;

				case KoLConstants.CONSUME_USE:
				case KoLConstants.MESSAGE_DISPLAY:
				case KoLConstants.INFINITE_USES:

					switch ( itemId )
					{
					case UseItemRequest.MACGUFFIN_DIARY:

						useType = "read";
						useLocation = "diary.php?textversion=1";
						break;

					case 75: // spooky sapling
					case 76: // Spooky-Gro fertilizer

						AdventureResult map = new AdventureResult( 74, 1 );
						if ( KoLCharacter.hasItem( map ) )
						{
							useType = "map";
							useLocation = "inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=3&whichitem=";
							itemId = 74;
						}
						break;

					case 146: // dinghy plans

						AdventureResult planks = new AdventureResult( 140, 1 );
						if ( !KoLCharacter.hasItem( planks ) && HermitRequest.getWorthlessItemCount() > 0 )
						{
							useType = "planks";
							useLocation = "hermit.php?autopermit=on&action=trade&pwd=" + GenericRequest.passwordHash +"&quantity=1&whichitem=140";
						}
						break;

					case 2095: // towel

						useType = "fold";
						useLocation = "inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=3&whichitem=";
						break;

					default:

						useType = "use";
						useLocation = "inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=3&whichitem=";
						break;
					}

					break;

				case KoLConstants.EQUIP_HAT:
				case KoLConstants.EQUIP_WEAPON:
				case KoLConstants.EQUIP_OFFHAND:
				case KoLConstants.EQUIP_SHIRT:
				case KoLConstants.EQUIP_PANTS:
				case KoLConstants.EQUIP_ACCESSORY:
				case KoLConstants.EQUIP_FAMILIAR:

					useType = null;
					int outfit = EquipmentDatabase.getOutfitWithItem( itemId );

					if ( outfit != -1 && EquipmentDatabase.hasOutfit( outfit ) )
					{
						useType = "outfit";
						useLocation = "inv_equip.php?action=outfit&which=2&whichoutfit=" + outfit;
					}
					else
					{
						useType = "equip";
						useLocation = "inv_equip.php?pwd=" + GenericRequest.passwordHash + "&which=2&action=equip&whichitem=";
					}

					break;

				case KoLConstants.CONSUME_ZAP:
					useType = "zap";
					useLocation = "wand.php?whichwand=";
					break;

				default:

					// Soft green echo eyedrop antidote gets an uneffect link

					if ( itemId == UneffectRequest.REMEDY.getItemId() )
					{
						useType = "use";
						useLocation = "uneffect.php";
					}

					// Special handling for star charts, lines, and stars, where
					// KoLmafia shows you how many of each you have.

					else if ( itemId == StarChartRequest.CHART.getItemId() || itemId == StarChartRequest.STARS.getItemId() || itemId == StarChartRequest.LINES.getItemId() )
					{
						useType =
							StarChartRequest.CHART.getCount( KoLConstants.inventory ) + "," + StarChartRequest.STARS.getCount( KoLConstants.inventory ) + "," + StarChartRequest.LINES.getCount( KoLConstants.inventory );
						useLocation = "starchart.php";
					}

					// Hedge maze puzzle and hedge maze key have a link to the maze
					// for easy access.

					else if ( itemId == SorceressLairManager.HEDGE_KEY.getItemId() || itemId == SorceressLairManager.PUZZLE_PIECE.getItemId() )
					{
						useType = "maze";
						useLocation = "hedgepuzzle.php";
					}

					// The different kinds of ores will only have a link if they're
					// the ones applicable to the trapper quest.

					else if ( itemId == 363 || itemId == 364 || itemId == 365 )
					{
						AdventureResult ore =
							new AdventureResult( Preferences.getString( "trapperOre" ), itemCount, false );

						if ( ore.getItemId() == itemId )
						{
							useType = String.valueOf( ore.getCount( KoLConstants.inventory ) );
							useLocation = "trapper.php";
						}
					}

					// Pixels have handy links indicating how many white pixels are
					// present in the player's inventory.

					else if ( itemId == 459 || itemId == 461 || itemId == 462 || itemId == 463 )
					{
						AdventureResult white = new AdventureResult( 459, 1 );
						useType =
							String.valueOf( white.getCount( KoLConstants.inventory ) + CreateItemRequest.getInstance(
								459 ).getQuantityPossible() ) + " white";
						useLocation = "mystic.php";
					}

					// Disintegrating sheet music gets a link which lets you sing it
					// to yourself.  We'll call it "hum" for now.

					else if ( itemId == 2192 )
					{
						useType = "sing";
						useLocation =
							"curse.php?action=use&pwd=" + GenericRequest.passwordHash + "&whichitem=2192&targetplayer=" + KoLCharacter.getUserName();
					}

					// Link which uses the plans when you acquire the planks.

					else if ( itemId == 140 )
					{
						AdventureResult plans = new AdventureResult( 146, 1 );
						if ( KoLCharacter.hasItem( plans ) )
						{
							useType = "plans";
							useLocation = "inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=3&whichitem=";
							itemId = 146;
						}
					}

					// Link to the guild upon completion of the Citadel quest.

					else if ( itemId == 1656 )
					{
						useType = "guild";
						useLocation = "guild.php?place=paco";
					}

					// Link to the untinkerer if you find an abridged dictionary.

					else if ( itemId == AdventureRequest.ABRIDGED.getItemId() )
					{
						useType = "untinker";
						useLocation = "town_right.php?action=untinker&pwd=" + GenericRequest.passwordHash + "&whichitem=";
					}

					// Link to the chasm if you just untinkered a dictionary.

					else if ( itemId == FightRequest.DICTIONARY1.getItemId() || itemId == AdventureRequest.BRIDGE.getItemId() )
					{
						useType = "chasm";
						useLocation = "mountains.php?pwd=" + GenericRequest.passwordHash + "&orcs=1";
					}

					// Bounty items get a count and a link
					// to the Bounty Hunter Hunter.

					else if ( itemId == Preferences.getInteger( "currentBountyItem" ) || ItemDatabase.isBountyItem( itemId ) )
					{
						Preferences.setString( "currentBountyItem", String.valueOf( itemId ) );
						AdventureResult item = new AdventureResult( itemId, 0 );
						useType = String.valueOf( item.getCount( KoLConstants.inventory ) );
						useLocation = "bhh.php";
					}
				}
			}

			if ( useType != null && useLocation != null )
			{
				if ( useLocation.endsWith( "=" ) )
				{
					useLocation += itemId;
				}

				if ( useLocation.equals( "#" ) )
				{
					useLinkMatcher.appendReplacement( buffer, "$1$2" );

					// Append a multi-use field rather than forcing
					// an additional page load.

					buffer.append( "</td></tr><tr><td colspan=2 align=center><div id=\"multiuse" );
					buffer.append( itemId );
					buffer.append( "\">" );

					buffer.append( "<form><input type=text size=3 id=\"quantity" );
					buffer.append( itemId );
					buffer.append( "\" value=" );
					buffer.append( Math.min( itemCount, UseItemRequest.maximumUses( itemId ) ) );
					buffer.append( ">&nbsp;<input type=button class=button value=\"Use\" onClick=\"multiUse('" );

					if ( consumeMethod == KoLConstants.MP_RESTORE )
					{
						buffer.append( "skills.php" );
					}
					else
					{
						buffer.append( "multiuse.php" );
					}

					buffer.append( "', " );
					buffer.append( itemId );
					buffer.append( "); void(0);\"></form></div>" );
				}
				else if ( !Preferences.getBoolean( "relayUsesInlineLinks" ) || !useLocation.startsWith( "inv" ) )
				{
					useLinkMatcher.appendReplacement(
						buffer,
						"$1$2 <font size=1>[<a href=\"" + useLocation.trim() + "\">" + useType + "</a>]</font>" );
				}
				else
				{
					String[] pieces = useLocation.toString().split( "\\?" );

					useLinkMatcher.appendReplacement(
						buffer,
						"$1$2 <font size=1>[<a href=\"javascript: " + "singleUse('" + pieces[ 0 ] + "', '" + pieces[ 1 ] + "'); void(0);\">" + useType + "</a>]</font>" );
				}

				buffer.append( "</td>" );
			}
			else
			{
				useLinkMatcher.appendReplacement( buffer, "$0" );
			}
		}

		useLinkMatcher.appendTail( buffer );

		if ( specialLinkText != null )
		{
			StaticEntity.singleStringReplace(
				buffer,
				"</center></blockquote>",
				"<p><center><a href=\"inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=2&whichitem=" + specialLinkId + "\">[" + specialLinkText + " it again]</a></center></blockquote>" );
		}
	}
}