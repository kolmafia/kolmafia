package net.sourceforge.kolmafia.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.preferences.Preferences;

public class HugglerMessage
	extends ChatMessage
{
	private String winner;
	private String loser;

	// Yes, I understand that many of the following patterns could be
	// collapsed, using (?:PHRASE1|PHRASE2|PHRASE3...) constructs.  That
	// would certainly be more efficient. But, for now, until CDM is done
	// adding new HMC Radio messages, this is more maintainable.

	private static final Pattern[] WIN_MESSAGES =
	{
		Pattern.compile( "^Accolades for <b>(.*?)</b>, unaccolades for (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> 1, (.*?) 0$" ),
		Pattern.compile( "^<b>(.*?)</b> dropped a bomb on (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> gave (.*?) a lesson in failure.$" ),
		Pattern.compile( "^<b>(.*?)</b> gave (.*?) a lesson in pain.$" ),
		Pattern.compile( "^<b>(.*?)</b> is dancing in (.*?)'s ashes.$" ),
		Pattern.compile( "^<b>(.*?)</b> is eating (.*?)'s lunch.$" ),
		Pattern.compile( "^<b>(.*?)</b> is the victor! (.*?) is the loser.$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?beat down (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?bested (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?blasted (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?conquered (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?cowed (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?crushed (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?decimated (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?defeated (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?destroyed (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?devastated (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?flattened (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?humbled (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?lambasted (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?overcame (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?overpowered (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?overwhelmed (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?pounded (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?roasted (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?sniped (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?thwarted (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?took out (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?tripped over (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?trounced (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?undid (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?vanquished (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> (?:just )?whupped (.*?).$" ),
		Pattern.compile( "^<b>(.*?)</b> made (.*?) beg for mercy.$" ),
		Pattern.compile( "^<b>(.*?)</b> sent (.*?) in to next week.$" ),
		Pattern.compile( "^<b>(.*?)</b> took (.*?) to school.$" ),
		Pattern.compile( "^<b>(.*?)</b> took (.*?) to task.$" ),
	};

	private static final Pattern[] LOSE_MESSAGES =
	{
		Pattern.compile( "^(.*?) can't handle <b>(.*?)</b>.$" ),
		Pattern.compile( "^(.*?) couldn't beat <b>(.*?)</b>.$" ),
		Pattern.compile( "^(.*?) could not handle <b>(.*?)</b>.$" ),
		Pattern.compile( "^(.*?) could not handle what <b>(.*?)</b> was cooking.$" ),
		Pattern.compile( "^(.*?) failed to defeat <b>(.*?)</b>.$" ),
		Pattern.compile( "^(.*?) fell prey to <b>(.*?)</b>.$" ),
		Pattern.compile( "^(.*?) fell to <b>(.*?)</b>.$" ),
		Pattern.compile( "^(.*?) just fell to <b>(.*?)</b>.$" ),
		Pattern.compile( "^(.*?) just took a lesson in defeat from <b>(.*?)</b>.$" ),
		Pattern.compile( "^(.*?) just tripped over <b>(.*?)</b>.$" ),
		Pattern.compile( "^(.*?) lost. +<b>(.*?)</b> won.$" ),
		Pattern.compile( "^(.*?) lost to <b>(.*?)</b>.$" ),
		Pattern.compile( "^(.*?) was burned by <b>(.*?)</b>.$" ),
		Pattern.compile( "^(.*?) went down in a blaze of failure. +Go <b>(.*?)</b>.$" ),
		Pattern.compile( "^Why so sad (.*?)\\? +Did <b>(.*?)</b> just defeat you.$" ),
	};

	public HugglerMessage( String content, String winner, String loser )
	{
		super( "HMC Radio", Preferences.getBoolean( "useHugglerChannel" ) ? "HMC Radio" : "/pvp", content, false );
		this.winner = winner;
		this.loser = loser;
	}

	public HugglerMessage( String content )
	{
		this( content, null, null );
		this.setCombatants( content );
	}

	public String getWinner()
	{
		return this.winner;
	}

	public String getLoser()
	{
		return this.winner;
	}

	private void setCombatants( final String line )
	{
		for ( Pattern pattern : WIN_MESSAGES )
		{
			Matcher matcher = pattern.matcher( line );
			if ( matcher.find() )
			{
				this.winner = matcher.group( 1 );
				this.loser = matcher.group( 2 );
				return;
			}
		}

		for ( Pattern pattern : LOSE_MESSAGES )
		{
			Matcher matcher = pattern.matcher( line );
			if ( matcher.find() )
			{
				this.winner = matcher.group( 2 );
				this.loser = matcher.group( 1 );
				return;
			}
		}
	}

	public static HugglerMessage constructMessage( final String line )
	{
		for ( Pattern pattern : WIN_MESSAGES )
		{
			Matcher matcher = pattern.matcher( line );
			if ( matcher.find() )
			{
				return new HugglerMessage( line, matcher.group( 1 ), matcher.group( 2 ) );
			}
		}

		for ( Pattern pattern : LOSE_MESSAGES )
		{
			Matcher matcher = pattern.matcher( line );
			if ( matcher.find() )
			{
				return new HugglerMessage( line, matcher.group( 2 ), matcher.group( 1 ) );
			}
		}

		return null;
	}
}
