package net.sourceforge.kolmafia.textui.command;

import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.objectpool.EffectPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.utilities.WikiUtilities;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class WikiLookupCommand
	extends AbstractCommand
{
	public WikiLookupCommand()
	{
		this.usage = " [effect|familiar|item|skill|outfit|monster|location] <target> - go to appropriate KoL Wiki page. If not specified, defaults to effect then item if no matches.";
	}

	@Override
	public void run( final String command, final String parameters )
	{
		int index = parameters.indexOf( " " );
		String target = parameters;
		String type = null;
		if ( index != -1 )
		{
			type = parameters.substring( 0, index ).toLowerCase().trim();
			if ( type.startsWith( "effect" ) || type.startsWith( "familiar" ) || type.startsWith( "item" ) ||
				type.startsWith( "skill" ) || type.startsWith( "outfit" ) || type.startsWith( "monster" ) || 
				type.startsWith( "location" ) )
			{
				target = parameters.substring( index + 1 ).trim();
			}
			else
			{
			// No type specified
				type = "item or effect";
			}
		}
		else
		{
			// No type specified
			type = "item or effect";
		}
		if ( command.equals( "lookup" ) && target.length() > 0 )
		{
			if ( type.startsWith( "effect" ) || type.startsWith( "item or effect" ) )
 			{
				List names = EffectDatabase.getMatchingNames( target );
				if ( names.size() == 1 )
				{
					int effectId = EffectDatabase.getEffectId( (String) names.get( 0 ) );
					AdventureResult result = EffectPool.get( effectId );
					WikiUtilities.showWikiDescription( result );
					return;
				}
			}

			if ( type.startsWith( "familiar" ) )
			{
				int num = FamiliarDatabase.getFamiliarId( target );
				if ( num != -1 )
				{
					target = FamiliarDatabase.getFamiliarName( num );
					WikiUtilities.showWikiDescription( target );
				}
 				return;
 			}

			if ( type.startsWith( "item" ) )
			{
				AdventureResult result = ItemFinder.getFirstMatchingItem( target, Match.ANY );
				if ( result != null )
				{
					WikiUtilities.showWikiDescription( result );
					return;
				}
			}

			if ( type.startsWith( "skill" ) )
			{
				List names = SkillDatabase.getMatchingNames( target );
				if ( names.size() == 1 )
				{
					int num = SkillDatabase.getSkillId( (String) names.get( 0 ) );
					target = SkillDatabase.getSkillName( num );
					WikiUtilities.showWikiDescription( target );
					return;
				}
			}

			if ( type.startsWith( "outfit" ) )
			{
				SpecialOutfit so = EquipmentManager.getMatchingOutfit( target );
				if ( so != null )
				{
					WikiUtilities.showWikiDescription( so.toString() );
					return;
				}
			}

			if ( type.startsWith( "monster" ) )
			{
				MonsterData monster = MonsterDatabase.findMonster( target, true, false );
				if ( monster != null )
				{
					WikiUtilities.showWikiDescription( monster );
					return;
				}
			}

			if ( type.startsWith( "location" ) )
			{
				KoLAdventure content = AdventureDatabase.getAdventure( target );
				if ( content != null )
				{
					WikiUtilities.showWikiDescription( content.getAdventureName() );
					return;
				}
			}
 		}

		RelayLoader.openSystemBrowser( "http://kol.coldfront.net/thekolwiki/index.php/Special:Search?search=" + StringUtilities.getURLEncode( target ) + "&go=Go" );
	}
}
