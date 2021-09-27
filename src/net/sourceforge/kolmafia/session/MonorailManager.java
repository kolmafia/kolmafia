package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.RequestEditorKit;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonorailManager
{
	public static final Map<String, String[]> lyleSpoilers = new HashMap<>();
	static
	{
		MonorailManager.lyleSpoilers.put( "Exchange 10 shovelfuls of dirt and 10 hunks of granite for an earthenware muffin tin!",
		                                  new String[]{"", "earthenware muffin tin"} );
		MonorailManager.lyleSpoilers.put( "Order a blueberry muffin", new String[]{ "", "blueberry muffin" } );
		MonorailManager.lyleSpoilers.put( "Order a bran muffin", new String[]{ "", "bran muffin" } );
		MonorailManager.lyleSpoilers.put( "Order a chocolate chip muffin", new String[]{ "", "chocolate chip muffin" } );
		MonorailManager.lyleSpoilers.put( "Back to the Platform!", new String[]{ "", null } );
	}

	public static void resetMuffinOrder()
	{
		String muffinOrder = Preferences.getString( "muffinOnOrder" );
		if (muffinOrder.equals("blueberry muffin") ||
				muffinOrder.equals("bran muffin") ||
				muffinOrder.equals("chocolate chip muffin"))
		{
			Preferences.setString( "muffinOnOrder", "earthenware muffin tin" );
		}
	}

	public static Object[][] choiceSpoilers(final int choice, final StringBuffer buffer )
	{
		if ( choice != 1308 || buffer == null )
		{
			return null;
		}

		// Lazy! They're lazy, I tell you!
		// Options in On A Downtown Train are dynamically assigned to values
		// rather than being bound to them, so it's literally impossible to
		// know what each choice will do without parsing them and reading their value.

		// We'll have to imitate RequestEditorKit.addChoiceSpoilers( final String location, final StringBuffer buffer )
		List<ChoiceManager.Option> options = new ArrayList<>();

		Matcher matcher = Pattern.compile( "name=choiceform\\d+(.*?)</form>", Pattern.DOTALL ).matcher( buffer );

		while ( matcher.find() )
		{
			String currentSection = matcher.group( 1 );
			Matcher optionMatcher = RequestEditorKit.OPTION_PATTERN.matcher( currentSection );
			if ( !optionMatcher.find() )
			{	// this wasn't actually a choice option - strange!
				continue;
			}
			Matcher buttonTextMatcher = RequestEditorKit.BUTTON_TEXT_PATTERN.matcher( currentSection );
			if ( !buttonTextMatcher.find() )
			{	// no... button? a blank one, maybe? weird!
				continue;
			}

			String buttonText = buttonTextMatcher.group( 1 );
			int choiceNumber = StringUtilities.parseInt( optionMatcher.group( 1 ) );

			if ( lyleSpoilers.containsKey( buttonText ) )
			{
				String[] thisOption = MonorailManager.lyleSpoilers.get( buttonText );
				options.add( new ChoiceManager.Option( thisOption[ 0 ], choiceNumber, thisOption[ 1 ] ) );
			}
		}

		return new Object[][] { new String[] { "" }, new String[] { "On a Downtown Train" }, options.toArray() };
	}
}