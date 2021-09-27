package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PottedTeaTreeRequest
	extends GenericRequest
{
	private static final TreeMap<Integer,PottedTea> idToTea = new TreeMap<Integer,PottedTea>();
	private static final TreeMap<String,PottedTea> canonicalNameToTea = new TreeMap<String,PottedTea>();
	public static final List<PottedTea> teas = new ArrayList<PottedTea>();

	static
	{
		PottedTeaTreeRequest.registerTea( ItemPool.ACTIVI_TEA, "Spleen Item" );
		PottedTeaTreeRequest.registerTea( ItemPool.ALACRI_TEA, "Initiative" );
		PottedTeaTreeRequest.registerTea( ItemPool.BOO_TEA, "Spooky damage" );
		PottedTeaTreeRequest.registerTea( ItemPool.CHARI_TEA, "Meat" );
		PottedTeaTreeRequest.registerTea( ItemPool.CRAFT_TEA, "Free crafting" );
		PottedTeaTreeRequest.registerTea( ItemPool.CRUEL_TEA, "Spleen Item" );
		PottedTeaTreeRequest.registerTea( ItemPool.DEXTERI_TEA, "Moxie" );
		PottedTeaTreeRequest.registerTea( ItemPool.FEROCI_TEA, "Muscle" );
		PottedTeaTreeRequest.registerTea( ItemPool.FLAMIBILI_TEA, "Hot damage" );
		PottedTeaTreeRequest.registerTea( ItemPool.FLEXIBILI_TEA, "Moxie stats" );
		PottedTeaTreeRequest.registerTea( ItemPool.FROST_TEA, "Hot resistance" );
		PottedTeaTreeRequest.registerTea( ItemPool.GILL_TEA, "Underwater" );
		PottedTeaTreeRequest.registerTea( ItemPool.IMPREGNABILI_TEA, "DR" );
		PottedTeaTreeRequest.registerTea( ItemPool.IMPROPRIE_TEA, "Sleaze damage" );
		PottedTeaTreeRequest.registerTea( ItemPool.INSANI_TEA, "Random Monster Mods" );
		PottedTeaTreeRequest.registerTea( ItemPool.IRRITABILI_TEA, "+Combat" );
		PottedTeaTreeRequest.registerTea( ItemPool.LOYAL_TEA, "Familiar weight" );
		PottedTeaTreeRequest.registerTea( ItemPool.MANA_TEA, "MP" );
		PottedTeaTreeRequest.registerTea( ItemPool.MEDIOCRI_TEA, "+ML" );
		PottedTeaTreeRequest.registerTea( ItemPool.MONSTROSI_TEA, "-ML" );
		PottedTeaTreeRequest.registerTea( ItemPool.MORBIDI_TEA, "Spooky resist" );
		PottedTeaTreeRequest.registerTea( ItemPool.NAS_TEA, "Stench damage" );
		PottedTeaTreeRequest.registerTea( ItemPool.NET_TEA, "Stench resist" );
		PottedTeaTreeRequest.registerTea( ItemPool.NEUROPLASTICI_TEA, "Myst stats" );
		PottedTeaTreeRequest.registerTea( ItemPool.OBSCURI_TEA, "-Combat" );
		PottedTeaTreeRequest.registerTea( ItemPool.PHYSICALI_TEA, "Muscle stats" );
		PottedTeaTreeRequest.registerTea( ItemPool.PROPRIE_TEA, "Sleaze resist" );
		PottedTeaTreeRequest.registerTea( ItemPool.ROYAL_TEA, "Royalty" );
		PottedTeaTreeRequest.registerTea( ItemPool.SERENDIPI_TEA, "Item" );
		PottedTeaTreeRequest.registerTea( ItemPool.SOBRIE_TEA, "Drunk reduction" );
		PottedTeaTreeRequest.registerTea( ItemPool.TOAST_TEA, "Cold resist" );
		PottedTeaTreeRequest.registerTea( ItemPool.TWEN_TEA, "Lots of boosts" );
		PottedTeaTreeRequest.registerTea( ItemPool.UNCERTAIN_TEA, "???" );
		PottedTeaTreeRequest.registerTea( ItemPool.VITALI_TEA, "HP" );
		PottedTeaTreeRequest.registerTea( ItemPool.VORACI_TEA, "Stomach increase" );
		PottedTeaTreeRequest.registerTea( ItemPool.WIT_TEA, "Myst" );
		PottedTeaTreeRequest.registerTea( ItemPool.YET_TEA, "Cold damage" );
	}

    private static void registerTea( int id, String effect )
	{
		String name = ItemPool.get( id, 1 ).getName();
		PottedTea tea = new PottedTea( name, id, effect );
		PottedTeaTreeRequest.idToTea.put( id, tea );
		PottedTeaTreeRequest.canonicalNameToTea.put( StringUtilities.getCanonicalName( name ), tea );
		PottedTeaTreeRequest.teas.add( tea );
	}

	private static String [] CANONICAL_TEA_ARRAY;
	static
	{
		Set<String> keys = PottedTeaTreeRequest.canonicalNameToTea.keySet();
		PottedTeaTreeRequest.CANONICAL_TEA_ARRAY = keys.toArray( new String[ keys.size() ] );
	}

    public static final List<String> getMatchingNames( final String substring )
	{
		return StringUtilities.getMatchingNames(PottedTeaTreeRequest.CANONICAL_TEA_ARRAY, substring );
	}

	public static PottedTea canonicalNameToTea( String name )
	{
		return PottedTeaTreeRequest.canonicalNameToTea.get( name );
	}

	private final PottedTea tea;

	// Shake the tree
	public PottedTeaTreeRequest()
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1104" );
		this.addFormField( "option", "1" );
		this.tea = null;
	}

	// Pick a specific tea
	public PottedTeaTreeRequest( PottedTea tea )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1105" );
		this.addFormField( "option", "1" );
		this.addFormField( "itemid", String.valueOf( tea.id ) );
		this.tea = tea;
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		// If you don't have a potted tea tree, punt
		if ( !KoLConstants.campground.contains( ItemPool.get( ItemPool.POTTED_TEA_TREE, 1 ) ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a potted tea tree." );
			return;
		}

		// If you already used your potted tea tree, punt
		if ( Preferences.getBoolean( "_pottedTeaTreeUsed" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already harvested your potted tea tree today." );
			return;
		}

		GenericRequest campRequest = new GenericRequest( "campground.php" );
		campRequest.addFormField( "action", "teatree" );
		campRequest.run();

		if ( this.tea != null )
		{
			GenericRequest pickRequest = new GenericRequest( "choice.php" );
			pickRequest.addFormField( "whichchoice", "1104" );
			pickRequest.addFormField( "option", "2" );
			pickRequest.run();
		}

		super.run();
	}

	public static class PottedTea
	{
		public int id;
		public String name;
		private final String effect;

		public PottedTea( String name, int id, String effect )
		{
			this.id = id;
			this.name = name;
			this.effect = effect;
		}

		@Override
		public String toString()
		{
			return this.name;
		}

		public String effect()
		{
			return this.effect;
		}
	}
	public static final Pattern URL_TEA_PATTERN = Pattern.compile( "itemid=(\\d+)" );
	public static PottedTea extractTeaFromURL( final String urlString )
	{
		Matcher matcher = PottedTeaTreeRequest.URL_TEA_PATTERN.matcher( urlString );
		return  matcher.find() ?
			PottedTeaTreeRequest.idToTea.get( StringUtilities.parseInt( matcher.group( 1 ) ) ) :
			null;
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) )
		{
			return false;
		}

		int choice = ChoiceManager.extractChoiceFromURL( urlString );

		if ( choice != 1104 && choice != 1105 )
		{
			return false;
		}
		
		String teaharvested = null;

		if ( choice == 1104 && urlString.contains( "option=1" ) )
		{
			teaharvested = "shake";
		}
		else if ( choice == 1105 )
		{
			PottedTea tea = PottedTeaTreeRequest.extractTeaFromURL( urlString );
			if ( tea != null )
			teaharvested = tea.toString();
		}

		if ( teaharvested == null )
		{
			return true;
		}

		String message = "teatree " + teaharvested;
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
