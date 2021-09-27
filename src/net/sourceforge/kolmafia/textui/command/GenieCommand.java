package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenieRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class GenieCommand
	extends AbstractCommand
{
	public GenieCommand()
	{
		this.usage = " effect <effectname> | monster <monstername> | stat (mus|mys|mox|all) | meat | item (pony|pocket|shirt) | wish <wish> | freedom";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		String wish = "";
		if ( parameters.startsWith( "wish " ) )
		{
			wish = parameters.substring( 5 );
		}
		// meat
		else if ( parameters.equals( "meat" ) )
		{
			wish = "I was rich";
		}
		// item
			// pocket wish
			// pony
			// shirt
		else if ( parameters.startsWith( "item " ) )
		{
			parameters = parameters.substring( 5 );
			if ( parameters.startsWith( "pocket" ) )
			{
				if ( Preferences.getInteger( "_genieWishesUsed" ) == 3 || !InventoryManager.hasItem( ItemPool.GENIE_BOTTLE ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Don't use a pocket wish to make a pocket wish." );
					return;
				}
				wish = "for more wishes";
			}
			else if ( parameters.startsWith( "pony" ) )
			{
				wish = "for a pony";
			}
			else if ( parameters.startsWith( "shirt" ) )
			{
				wish = "for a blessed rustproof +2 gray dragon scale mail";
			}
		}
		// stat [mus|mys|mox|all]
		else if ( parameters.startsWith( "stat " ) )
		{
			parameters = parameters.substring( 5 );
			if ( parameters.startsWith( "mus" ) )
			{
				wish = "I was a little bit taller";
			}
			else if ( parameters.startsWith( "mys" ) )
			{
				wish = "I had a rabbit in a hat with a bat";
			}
			else if ( parameters.startsWith( "mox" ) )
			{
				wish = "I was a baller";
			}
			else if ( parameters.startsWith( "all" ) )
			{
				wish = "I was big";
			}
		}
		// freedom
		else if ( parameters.equals( "freedom" ) )
		{
			wish = "you were free";
		}
		// effect [name]
		else if ( parameters.startsWith( "effect " ) )
		{
			parameters = parameters.substring( 7 );
			List<String> effectNames = EffectDatabase.getMatchingNames( parameters );
			if ( effectNames.size() != 1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, parameters + "does not match exactly one effect" );
				return;
			}
			String name = effectNames.get( 0 );
			wish = "to be " + name;
		}
		// monster
		else if ( parameters.startsWith( "monster " ) )
		{
			parameters = parameters.substring( 8 );
			MonsterData monster = MonsterDatabase.findMonster( parameters, true, false );
			if ( monster == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, parameters + " does not match a monster." );
				return;
			}
			wish = "to fight " + parameters;
		}

		if ( wish != "" )
		{
			RequestThread.postRequest( new GenieRequest( wish ) );
		}
	}
}
