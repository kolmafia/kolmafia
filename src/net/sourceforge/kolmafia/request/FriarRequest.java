package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

public class FriarRequest
	extends GenericRequest
{
	private int option = 0;

	private static final Pattern ID_PATTERN = Pattern.compile( "action=buffs.*?bro=(\\d+)" );

	public static final String[] BLESSINGS = { "food", "familiar", "booze", };

	public FriarRequest( final int option )
	{
		super( "friars.php" );

		this.addFormField( "action", "buffs" );
		if ( option >= 1 && option <= 3 )
		{
			this.option = option;
			this.addFormField( "bro", String.valueOf( option ) );
		}
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		if ( this.option == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Decide which friar to visit." );
			return;
		}

		KoLmafia.updateDisplay( "Visiting the Deep Fat Friars..." );
		super.run();
	}

	@Override
	public void processResults()
	{
		if ( this.responseText == null || this.responseText.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't find the Deep Fat Friars." );
			return;
		}

		FriarRequest.parseResponse( this.getURLString(), this.responseText );

		if ( this.responseText.indexOf( "one of those per day." ) != -1 )
		{
			KoLmafia.updateDisplay( "You can only get one blessing a day from the Deep Fat Friars." );
			return;
		}

		KoLmafia.updateDisplay( "You've been blessed." );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		// No, seriously, you can only get one of those per day.
		// Brother <name> smiles and rubs some ashes on your face.
		if ( responseText.indexOf( "one of those per day." ) != -1 ||
		     responseText.indexOf( "smiles and rubs some ashes" ) != -1 )
		{
			Preferences.setBoolean( "friarsBlessingReceived", true );
			Preferences.setInteger( "lastFriarCeremonyAscension", Preferences.getInteger( "knownAscensions" ));
			QuestDatabase.setQuestProgress( Quest.FRIAR, QuestDatabase.FINISHED );
			return;
		}

		// First visit to friars, not as Ed:
		//
		// Please, Adventurer, help us! We were performing a ritual at
		// our Infernal Gate, and Brother Starfish dropped the
		// butterknife. All of the infernal creatures escaped our
		// grasp, and have tainted our grove. Please clean the taint!
		// Collect the three items necessary to perform the ritual
		// which will banish these fiends back to their own realm.
		//
		// First visit, as Ed:
		//
		// In the midst of the grove, you encounter three men in brown
		// robes and funny haircuts, who are conversing in hushed and
		// worried tones. One of them notices you as you approach, and
		// raises his arms in supplication.
		//
		// Subsequent visit, without all the items:
		//
		// One of the monks shakes his head at you. "You don't have all
		// the ritual items yet. Please hurry, the infestation can only
		// get worse."
		//
		// Subsequent visit, with all the items:
		//
		// You've got all three of the ritual items, Adventurer! Hurry
		// to the center of the circle, and perform the ritual!
		if ( responseText.contains( "We were performing a ritual" ) ||
		     responseText.contains( "raises his arms in supplication" ) || 
		     responseText.contains( "don't have all the ritual items yet" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FRIAR, "step1" );
			return;
		}

		if ( responseText.contains( "You've got all three of the ritual items" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FRIAR, "step2" );
			return;
		}

		// Visiting the standing stones without all the items:
		//
		// Hmm. You don't appear to have all of the elements necessary
		// to perform the ritual.
		// 
		if ( responseText.contains( "don't appear to have all of the elements" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.FRIAR, "step1" );
			return;
		}

		// Visiting the standing stones with all the items, not as Ed:
		//
		// Oh, and listen. Don't be a stranger -- drop by and visit us,
		// and maybe get a blessing. We're good at blessings.
		//
		// Visiting the standing stones with all the items, as Ed:
		//
		// Please return to us if there's ever anything we can do for
		// you in return. We're good at blessings, for instance.
		if ( responseText.contains( "We're good at blessings" ) )
		{
			QuestDatabase.setQuestProgress( Quest.FRIAR, QuestDatabase.FINISHED );
			return;
		}
	}

	public static final boolean registerRequest( final String location )
	{
		if ( !location.startsWith( "friars.php" ) )
		{
			return false;
		}

		Matcher matcher = FriarRequest.ID_PATTERN.matcher( location );

		if ( !matcher.find() )
		{
			return true;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "friars blessing " + matcher.group( 1 ) );
		return true;
	}
}
