package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;

public class TutorialRequest
	extends GenericRequest
{
	public TutorialRequest( final String action )
	{
		super( "tutorial.php" );
		this.addFormField( "action", action );
	}

	@Override
	public void run()
	{
		KoLmafia.updateDisplay( "Visiting the Toot Oriole" );
		super.run();
	}

	@Override
	public void processResults()
	{
		TutorialRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( responseText.contains( "You acquire an item:" ) || responseText.contains( "You've learned everything I can teach you" ) )
		{
			QuestDatabase.setQuestProgress( QuestDatabase.Quest.TOOT, QuestDatabase.FINISHED );
			return;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		return urlString.startsWith( "tutorial.php" );
	}
}
