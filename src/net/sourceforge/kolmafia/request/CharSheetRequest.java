package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.moods.HPRestoreItemList;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;

import net.sourceforge.kolmafia.persistence.AscensionSnapshot;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.HTMLParserUtils;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class CharSheetRequest
	extends GenericRequest
{
	private static final Pattern BASE_PATTERN = Pattern.compile( " \\(base: ([\\d,]+)\\)" );
	private static final Pattern AVATAR_PATTERN =
		Pattern.compile( "<img src=[^>]*?(?:images.kingdomofloathing.com|/images)/([^>'\"\\s]+)" );

	private static final HtmlCleaner cleaner = HTMLParserUtils.configureDefaultParser();
	private static final DomSerializer domSerializer = new DomSerializer( cleaner.getProperties() );

	/**
	 * Constructs a new <code>CharSheetRequest</code>. The data in the KoLCharacter entity will be overridden over
	 * the course of this request.
	 */

	public CharSheetRequest()
	{
		// The only thing to do is to retrieve the page from
		// the- all variable initialization comes from
		// when the request is actually run.

		super( "charsheet.php" );
	}

	/**
	 * Runs the request. Note that only the character's statistics are retrieved via this retrieval.
	 */

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public String getHashField()
	{
		return null;
	}

	@Override
	public void run()
	{
		KoLmafia.updateDisplay( "Retrieving character data..." );
		super.run();
	}

	@Override
	public void processResults()
	{
		CharSheetRequest.parseStatus( this.responseText );
	}

	public static final void parseStatus( final String responseText )
	{
		// Set the character's avatar.
		Matcher avatarMatcher = CharSheetRequest.AVATAR_PATTERN.matcher( responseText );

		if ( avatarMatcher.find() )
		{
			KoLCharacter.setAvatar( avatarMatcher.group( 1 ) );
		}

		// Currently, this is used only for parsing the list of skills
		Document doc = null;
		try
		{
			doc = domSerializer.createDOM( cleaner.clean( responseText ) );
		}
		catch ( ParserConfigurationException e )
		{
			e.printStackTrace();
			return;
		}

		// Strip all of the HTML from the server reply
		// and then figure out what to do from there.

		String token = "";
		StringTokenizer cleanContent =
			new StringTokenizer( responseText.replaceAll( "><", "" ).replaceAll( "<.*?>", "\n" ), "\n" );

		while ( !token.startsWith( " (#" ) )
		{
			token = cleanContent.nextToken();
		}

		KoLCharacter.setUserId( StringUtilities.parseInt( token.substring( 3, token.length() - 1 ) ) );
		GenericRequest.skipTokens( cleanContent, 1 );

		String className = cleanContent.nextToken().trim();

		// Hit point parsing begins with the first index of
		// the words indicating that the upcoming token will
		// show the HP values (Current, Maximum).

		while ( !token.startsWith( "Current" ) )
		{
			token = cleanContent.nextToken();
		}

		int currentHP = GenericRequest.intToken( cleanContent );
		while ( !token.startsWith( "Maximum" ) )
		{
			token = cleanContent.nextToken();
		}

		int maximumHP = GenericRequest.intToken( cleanContent );
		token = cleanContent.nextToken();
		KoLCharacter.setHP( currentHP, maximumHP, CharSheetRequest.retrieveBase( token, maximumHP ) );

		// Mana point parsing is exactly the same as hit point
		// parsing - so this is just a copy-paste of the code.

		if ( KoLCharacter.inZombiecore() )
		{
			// Zombie Masters have a Horde.
			while ( !token.startsWith( "Zombie Horde" ) )
			{
				token = cleanContent.nextToken();
			}

			int horde = GenericRequest.intToken( cleanContent );
			KoLCharacter.setMP( horde, horde, horde );
		}
		else if ( !KoLCharacter.isVampyre() )
		{
			// Vampyres have no MP
			while ( !token.startsWith( "Current" ) )
			{
				token = cleanContent.nextToken();
			}

			int currentMP = GenericRequest.intToken( cleanContent );
			while ( !token.startsWith( "Maximum" ) )
			{
				token = cleanContent.nextToken();
			}

			int maximumMP = GenericRequest.intToken( cleanContent );
			token = cleanContent.nextToken();

			KoLCharacter.setMP( currentMP, maximumMP, CharSheetRequest.retrieveBase( token, maximumMP ) );
		}

		// Players with a custom title will have their actual class shown in this area.

		while ( !token.startsWith( "Mus" ) )
		{
			if ( token.equals( "Class:" ) )
			{
				className = cleanContent.nextToken().trim();
				break;
			}
			token = cleanContent.nextToken();
		}

		// Next, you begin parsing the different stat points;
		// this involves hunting for the stat point's name,
		// skipping the appropriate number of tokens, and then
		// reading in the numbers.

		long[] mus = CharSheetRequest.findStatPoints( cleanContent, token, "Mus" );
		long[] mys = CharSheetRequest.findStatPoints( cleanContent, token, "Mys" );
		long[] mox = CharSheetRequest.findStatPoints( cleanContent, token, "Mox" );

		KoLCharacter.setStatPoints( (int)mus[ 0 ], mus[ 1 ], (int)mys[ 0 ], mys[ 1 ], (int)mox[ 0 ], mox[ 1 ] );

		// Drunkenness may or may not exist (in other words,
		// if the character is not drunk, nothing will show
		// up). Therefore, parse it if it exists; otherwise,
		// parse until the "Adventures remaining:" token.

		while ( !token.startsWith( "Temul" ) && !token.startsWith( "Inebr" ) && !token.startsWith( "Tipsi" ) && !token.startsWith( "Drunk" ) && !token.startsWith( "Adven" ) )
		{
			token = cleanContent.nextToken();
		}

		if ( !token.startsWith( "Adven" ) )
		{
			KoLCharacter.setInebriety( GenericRequest.intToken( cleanContent ) );
			while ( !token.startsWith( "Adven" ) )
			{
				token = cleanContent.nextToken();
			}
		}
		else
		{
			KoLCharacter.setInebriety( 0 );
		}

		// Now parse the number of adventures remaining,
		// the monetary value in the character's pocket,
		// and the number of turns accumulated.

		int oldAdventures = KoLCharacter.getAdventuresLeft();
		int newAdventures = GenericRequest.intToken( cleanContent );
		ResultProcessor.processAdventuresLeft( newAdventures - oldAdventures );

		while ( !token.startsWith( "Meat" ) )
		{
			token = cleanContent.nextToken();
		}
		KoLCharacter.setAvailableMeat( GenericRequest.intToken( cleanContent ) );

		// Determine the player's ascension count, if any.
		// This is seen by whether or not the word "Ascensions"
		// appears in their player profile.

		if ( responseText.indexOf( "Ascensions:" ) != -1 )
		{
			while ( !token.startsWith( "Ascensions" ) )
			{
				token = cleanContent.nextToken();
			}
			KoLCharacter.setAscensions( GenericRequest.intToken( cleanContent ) );
		}

		// There may also be a "turns this run" field which
		// allows you to have a Ronin countdown.
		boolean runStats = responseText.indexOf( "(this run)" ) != -1;

		while ( !token.startsWith( "Turns" ) ||
			( runStats && token.indexOf( "(this run)" ) == -1 ) )
		{
			token = cleanContent.nextToken();
		}

		KoLCharacter.setCurrentRun( GenericRequest.intToken( cleanContent ) );
		while ( !token.startsWith( "Days" ) ||
			( runStats && token.indexOf( "(this run)" ) == -1 ) )
		{
			token = cleanContent.nextToken();
		}

		KoLCharacter.setCurrentDays( GenericRequest.intToken( cleanContent ) );

		// Determine the player's zodiac sign, if any. We
		// could read the path in next, but it's easier to
		// read it from the full response text.

		if ( responseText.contains( "Sign:" ) )
		{
			while ( !cleanContent.nextToken().startsWith( "Sign:" ) )
			{
            }
			KoLCharacter.setSign( cleanContent.nextToken() );
		}

		// This is where Path: optionally appears
		KoLCharacter.setRestricted( responseText.contains( "standard.php" ) );

		// Consumption restrictions have special messages.
		//
		// "You may not eat or drink anything."
		// "You may not eat any food or drink any non-alcoholic beverages."
		// "You may not consume any alcohol."

		KoLCharacter.setConsumptionRestriction(
			responseText.contains( "You may not eat or drink anything." ) ?
			AscensionSnapshot.OXYGENARIAN :
			responseText.contains( "You may not eat any food or drink any non-alcoholic beverages." ) ?
			AscensionSnapshot.BOOZETAFARIAN :
			responseText.contains( "You may not consume any alcohol." ) ?
			AscensionSnapshot.TEETOTALER :
			AscensionSnapshot.NOPATH );

		// You are in Hardcore mode, and may not receive items or buffs
		// from other players.

		boolean hardcore = responseText.contains( "You are in Hardcore mode" );
		KoLCharacter.setHardcore( hardcore );

		// You may not receive items from other players until you have
		// played # more Adventures.

		KoLCharacter.setRonin( responseText.contains( "You may not receive items from other players" ) );

		// Deduce interaction from above settings

		CharPaneRequest.setInteraction();

		// See if the player has a store
		KoLCharacter.setStore( responseText.contains( "Mall of Loathing" ) );

		// See if the player has a display case
		KoLCharacter.setDisplayCase( responseText.contains( "in the Museum" ) );

		while ( !token.startsWith( "Skill" ) )
		{
			token = cleanContent.nextToken();
		}

		// The first token says "(click the skill name for more
		// information)" which is not really a skill.

		GenericRequest.skipTokens( cleanContent, 1 );
		token = cleanContent.nextToken();

		List<UseSkillRequest> newSkillSet = new ArrayList<UseSkillRequest>();
		List<UseSkillRequest> permedSkillSet = new ArrayList<UseSkillRequest>();

		List<ParsedSkillInfo> parsedSkillInfos = parseSkills( doc );
		for (ParsedSkillInfo skillInfo : parsedSkillInfos)
		{
			UseSkillRequest currentSkill = null;
			if ( skillInfo.isBadId() )
			{
				System.err.println( "Cannot parse skill ID in 'onclick' attribute for skill: " + skillInfo.name );
			}
			else
			{
				currentSkill = UseSkillRequest.getUnmodifiedInstance( skillInfo.id );
				if ( currentSkill == null )
				{
					System.err.println( "Unknown skill ID in charsheet.php: " + skillInfo.id );
				}
			}

			// Cannot find skill by ID, fall back to skill name check
			if ( currentSkill == null )
			{
				currentSkill = UseSkillRequest.getUnmodifiedInstance( skillInfo.name );
				if ( currentSkill == null )
				{
					System.err.println("Ignoring unknown skill name in charsheet.php: " + skillInfo.name);
					continue;
				}
			}

			boolean shouldAddSkill = true;

			if ( SkillDatabase.isBookshelfSkill( currentSkill.getSkillId() ) )
			{
				shouldAddSkill =
					(!KoLCharacter.inBadMoon() && !KoLCharacter.inAxecore()) || KoLCharacter.kingLiberated();
			}

			if ( currentSkill.getSkillId() == SkillPool.OLFACTION )
			{
				shouldAddSkill =
					(!KoLCharacter.inBadMoon() && !KoLCharacter.inAxecore()) || KoLCharacter.skillsRecalled();
			}

			if ( shouldAddSkill )
			{
				newSkillSet.add( currentSkill );
			}

			if ( skillInfo.permStatus == ParsedSkillInfo.PermStatus.SOFTCORE || skillInfo.permStatus == ParsedSkillInfo.PermStatus.HARDCORE )
			{
				permedSkillSet.add( currentSkill );
			}
		}

		// The Smile of Mr. A no longer appears on the char sheet
		if ( Preferences.getInteger( "goldenMrAccessories" ) > 0 )
		{
			UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance( "The Smile of Mr. A." );
			newSkillSet.add( skill );
		}

		// Toggle Optimality does not appear on the char sheet
		if ( Preferences.getInteger( "skillLevel7254" ) > 0 )
		{
			UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance( "Toggle Optimality" );
			newSkillSet.add( skill );
		}

		// If you have the Cowrruption effect, you can Absorb Cowrruption if a Cow Puncher
		if ( KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.COWRRUPTION ) ) && KoLCharacter.getClassType() == KoLCharacter.COWPUNCHER )
		{
			UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance( "Absorb Cowrruption" );
			newSkillSet.add( skill );
		}

		// If you have looked at your Bird-a-Day calendar today, you can use
		// "Seek out a Bird".
		if ( Preferences.getBoolean( "_canSeekBirds" ) )
		{
			UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance( "Seek out a Bird" );
			newSkillSet.add( skill );
		}
		
		// Set the skills that we saw
		KoLCharacter.setAvailableSkills( newSkillSet );
		KoLCharacter.setPermedSkills( permedSkillSet );

		// Finally, set the class name that we figured out.
		KoLCharacter.setClassName( className );

		// Update uneffect methods and heal amounts for updated skills
		UneffectRequest.reset();
		HPRestoreItemList.updateHealthRestored();
	}

	/**
	 * Helper method used to find the statistic points. This method was created because statistic-point finding is
	 * exactly the same for every statistic point.
	 *
	 * @param tokenizer The <code>StringTokenizer</code> containing the tokens to be parsed
	 * @param searchString The search string indicating the beginning of the statistic
	 * @return The 2-element array containing the parsed statistics
	 */

	private static long[] findStatPoints( final StringTokenizer tokenizer, String token, final String searchString )
	{
		long[] stats = new long[ 2 ];

		while ( !token.startsWith( searchString ) )
		{
			token = tokenizer.nextToken();
		}

		stats[ 0 ] = GenericRequest.intToken( tokenizer );
		token = tokenizer.nextToken();
		int base = CharSheetRequest.retrieveBase( token, (int) stats[ 0 ] );

		while ( !token.startsWith( "(" ) )
		{
			token = tokenizer.nextToken();
		}

		stats[ 1 ] = KoLCharacter.calculateSubpoints( base, GenericRequest.intToken( tokenizer ) );
		return stats;
	}

	/**
	 * Utility method for retrieving the base value for a statistic, given the tokenizer, and assuming that the base
	 * might be located in the next token. If it isn't, the default value is returned instead. Note that this advances
	 * the <code>StringTokenizer</code> one token ahead of the base value for the statistic.
	 *
	 * @param st The <code>StringTokenizer</code> possibly containing the base value
	 * @param defaultBase The value to return, if no base value is found
	 * @return The parsed base value, or the default value if no base value is found
	 */

	private static int retrieveBase( final String token, final int defaultBase )
	{
		Matcher baseMatcher = CharSheetRequest.BASE_PATTERN.matcher( token );
		return baseMatcher.find() ? StringUtilities.parseInt( baseMatcher.group( 1 ) ) : defaultBase;
	}

	/**
	 * Represents the ID, name, and perm status of a single skill parsed from charsheet.php
	 * There is no guarantee that the skill ID, name, or perm status is actually valid.
	 */
	public static class ParsedSkillInfo {
		public enum PermStatus {
			NONE,
			SOFTCORE,
			HARDCORE,
		}

		/** Skill ID. A negative value indicates that the skill ID could not be parsed. */
		public final int id;
		public final String name;
		public final PermStatus permStatus;

		ParsedSkillInfo( int id, String name, PermStatus permStatus ) {
			this.id = id;
			this.name = name;
			this.permStatus = permStatus;
		}

		ParsedSkillInfo( String name, PermStatus permStatus ) {
			this(-1, name, permStatus);
		}

		/**
		 * @return Whether the skill ID is bad (could not be parsed) and shouldn't be used.
		 */
		boolean isBadId() {
			return this.id < 0;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}

			if (!(o instanceof ParsedSkillInfo)) {
				return false;
			}

			ParsedSkillInfo other = (ParsedSkillInfo) o;
			return id == other.id && name.equals(other.name) && permStatus == other.permStatus;
		}

		@Override
		public String toString() {
			return String.format( "ParsedSkillInfo(id=%d, name=%s, permStatus=%s)", this.id, this.name,
					      this.permStatus );
		}
	}

	/**
	 * Parses skill information from charsheet.php.
	 * @param doc Parsed-and-cleaned HTML document
	 */
	public static final List<ParsedSkillInfo> parseSkills(Document doc) {
		// Assumption:
		// In the cleaned-up HTML, each skill is displayed as an <a> tag that looks like any of the following:
		//
		//	(Most of the time)
		//	<a onclick="javascript:poop(&quot;desc_skill.php?whichskill=SKILL_ID&amp;self=true&quot;,&quot;skill&quot;, 350, 300)">Skill Name</a><br>
		//	<a onclick="javascript:poop(&quot;desc_skill.php?whichskill=SKILL_ID&amp;self=true&quot;,&quot;skill&quot;, 350, 300)">Skill Name</a> (P)<br>
		//	<a onclick="javascript:poop(&quot;desc_skill.php?whichskill=SKILL_ID&amp;self=true&quot;,&quot;skill&quot;, 350, 300)">Skill Name</a> (<b>HP</b>)<br>
		//	(Rarely)
		//	<a onclick="skill(SKILL_ID)">Skill Name</a>
		//
		// ...where SKILL_ID is an integer, and P/HP indicate perm status.
		//
		// Skills that you cannot use right now (e.g. due to path restrictions) are wrapped in a <span> tag:
		//
		//	<span id="permskills" ...>...</span>
		NodeList skillNodes = null;
		try
		{
			skillNodes = (NodeList) XPathFactory.newInstance().newXPath().evaluate(
				"//a[contains(@onclick,'skill') and not(ancestor::*[@id='permskills'])]",
				doc,
				XPathConstants.NODESET
			);
		}
		catch ( XPathExpressionException e )
		{
			// Our xpath selector is bad; this build shouldn't be released at all
			e.printStackTrace();
			throw new RuntimeException( "Bad XPath selector" );
		}

		List<ParsedSkillInfo> parsedSkillInfos = new ArrayList<>();

		Matcher[] onclickSkillIdMatchers = {
			Pattern.compile( "\\bwhichskill=(\\d+)" ).matcher( "" ),
			Pattern.compile( "\\bskill\\((\\d+)\\)" ).matcher( "" ),
		};
		for ( int i = 0; i < skillNodes.getLength(); ++i ) {
			Node node = skillNodes.item( i );

			boolean isSkillIdFound = false;
			int skillId = -1;
			String skillName = node.getTextContent();
			ParsedSkillInfo.PermStatus permStatus = ParsedSkillInfo.PermStatus.NONE;

			// Parse following sibling nodes to check perm status
			Node nextSibling = node.getNextSibling();
			while ( nextSibling != null )
			{
				if ( nextSibling.getNodeType() == Node.TEXT_NODE )
				{
					String siblingText = nextSibling.getTextContent();
					if ( siblingText.contains( "(HP)" ) )
					{
						permStatus = ParsedSkillInfo.PermStatus.HARDCORE;
					}
					else if ( siblingText.contains( "(P)" ) )
					{
						permStatus = ParsedSkillInfo.PermStatus.SOFTCORE;
					}

					// If the text node does not contain perm status, keep examining subsequent siblings
				}
				else if ( nextSibling.getNodeName().equals( "b" ) )
				{
					String siblingText = nextSibling.getTextContent();
					if ( siblingText.contains( "HP" ) )
					{
						permStatus = ParsedSkillInfo.PermStatus.HARDCORE;
					}
					else if ( siblingText.contains( "P" ) )
					{
						permStatus = ParsedSkillInfo.PermStatus.SOFTCORE;
					}

					// If a <b></b> tag is encountered, stop examining siblings regardless of what
					// the tag contains
					break;
				}
				else
				{
					// If any other node is encountered, stop examining siblings
					break;
				}

				nextSibling = nextSibling.getNextSibling();
			}

			String onclick = node.getAttributes().getNamedItem( "onclick" ).getNodeValue();

			// Find the first successful matcher
			for ( Matcher skillIdMatcher : onclickSkillIdMatchers )
			{
				skillIdMatcher.reset( onclick );
				if ( skillIdMatcher.find() )
				{
					isSkillIdFound = true;
					skillId = StringUtilities.parseInt( skillIdMatcher.group(1) );
					break;
				}
			}

			if (isSkillIdFound)
			{
				parsedSkillInfos.add(new ParsedSkillInfo( skillId, skillName, permStatus ));
			}
			else
			{
				parsedSkillInfos.add(new ParsedSkillInfo( skillName, permStatus ));
			}
		}

		return parsedSkillInfos;
	}


	public static final void parseStatus( final JSONObject JSON )
		throws JSONException
	{
		int muscle = JSON.getInt( "muscle" );
		long rawmuscle = JSON.getLong( "rawmuscle" );

		int mysticality = JSON.getInt( "mysticality" );
		long rawmysticality = JSON.getLong( "rawmysticality" );

		int moxie = JSON.getInt( "moxie" );
		long rawmoxie = JSON.getLong( "rawmoxie" );

		KoLCharacter.setStatPoints( muscle, rawmuscle,
					    mysticality, rawmysticality,
					    moxie, rawmoxie );
	}
}
