package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class ClanFortuneDecorator
{
	private static final Pattern QUESTION_PATTERN = Pattern.compile( "name=\"q(\\d)\" required (?:value=\"(.*?)\"|)" );

	public static final void decorateQuestion( final StringBuffer buffer )
	{
		// Don't decorate if no questions set
		String[] q = new String[3];
		q[0] = Preferences.getString( "clanFortuneWord1" );
		q[1] = Preferences.getString( "clanFortuneWord2" );
		q[2] = Preferences.getString( "clanFortuneWord3" );
		if ( q[0].length() + q[1].length() + q[2].length() == 0 )
		{
			return;
		}
		// Check for questions, don't replace unless null
		Matcher matcher = QUESTION_PATTERN.matcher( buffer.toString() );
		while ( matcher.find() )
		{
			int num = StringUtilities.parseInt( matcher.group( 1 ) );
			String question = matcher.group( 2 );
			if ( num >= 1 && num <= 3 )
			{
				if ( q[num-1].length() > 0 && question == null )
				{
					String findString = "name=\"q" + num + "\" required ";
					String replaceString = "name=\"q" + num + "\" required value=\"" + q[num-1] + "\"";
					StringUtilities.singleStringReplace( buffer, findString, replaceString );
				}
			}
		}
	}

	public static final void decorateAnswer( final StringBuffer buffer )
	{
		// Don't decorate if no answers set
		String[] q = new String[3];
		q[0] = Preferences.getString( "clanFortuneReply1" );
		q[1] = Preferences.getString( "clanFortuneReply2" );
		q[2] = Preferences.getString( "clanFortuneReply3" );
		if ( q[0].length() + q[1].length() + q[2].length() == 0 )
		{
			return;
		}
		// Check for answers, don't replace unless null (in case one day KoL supports remembering them
		Matcher matcher = QUESTION_PATTERN.matcher( buffer.toString() );
		while ( matcher.find() )
		{
			int num = StringUtilities.parseInt( matcher.group( 1 ) );
			String question = matcher.group( 2 );
			if ( num >= 1 && num <= 3 )
			{
				if ( q[num-1].length() > 0 && question == null )
				{
					String findString = "name=\"q" + num + "\" required ";
					String replaceString = "name=\"q" + num + "\" required value=\"" + q[num-1] + "\"";
					StringUtilities.singleStringReplace( buffer, findString, replaceString );
				}
			}
		}
	}
}
