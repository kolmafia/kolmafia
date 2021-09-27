package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class DecorateTentRequest
	extends GenericRequest
{
	public DecorateTentRequest()
	{
		super( "choice.php" );
	}

	public static final void parseDecoration( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichchoice=1392" ) )
		{
			return;
		}

		int decision = ChoiceManager.extractOptionFromURL( urlString );
		switch ( decision )
		{
		case 1:
			// Muscular Intentions
			//
			// You burn some wood into charcoal, and use it to draw camouflage patterns all over your tent.
			// Like a soldier or a tough hunting guy! Or like a guy who wants to seem like a soldier or a
			// tough hunting guy!
			if ( !responseText.contains( "camouflage patterns" ) )
			{
				return;
			}
			break;
		case 2:
			// Mystical Intentions
			//
			// You burn some wood into charcoal, and use it to draw magical symbols all over your tent.
			// Many of them are just squiggles that you improvise on the spot, but in your experience
			// most magical symbols are exactly that.
			if ( !responseText.contains( "magical symbols" ) )
			{
				return;
			}
			break;
		case 3:
			// Moxious Intentions
			//
			// You burn some wood into charcoal, and use it to draw a sweet skull and crossbones on the
			// side of your tent. Since bears don't understand this iconography, you'll easily be able to
			// sneak away from them while they're puzzling it out.
			if ( !responseText.contains( "sweet skull and crossbones" ) )
			{
				return;
			}
			break;
		case 4:
			return;
		}

		ResultProcessor.processItem( ItemPool.BURNT_STICK, -1 );

		Preferences.setInteger( "campAwayDecoration", decision );
	}
}
