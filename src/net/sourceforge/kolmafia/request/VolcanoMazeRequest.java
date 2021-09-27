package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.session.VolcanoMazeManager;

public class VolcanoMazeRequest
	extends GenericRequest
{
	public VolcanoMazeRequest()
	{
		super( "volcanomaze.php?start=1", false );
	}

	public VolcanoMazeRequest( final boolean jump )
	{
		super( "volcanomaze.php?jump=1", false );
	}

	public VolcanoMazeRequest( final int pos )
	{
		super( VolcanoMazeRequest.getMoveURL( pos ), false );
	}

	public VolcanoMazeRequest( final int col, final int row )
	{
		super( VolcanoMazeRequest.getMoveURL( col, row ), false );
	}

	public static String getMoveURL( final int pos )
	{
		int row = pos / VolcanoMazeManager.NCOLS;
		int col = pos % VolcanoMazeManager.NCOLS;
		return VolcanoMazeRequest.getMoveURL( col, row );
	}

	public static String getMoveURL( final int col, final int row )
	{
		return "volcanomaze.php?move=" + col + "," + row + "&ajax=1";
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	@Override
	public void run()
	{
		super.run();

		// A niggling voice in the back of your head (that's me)
		// reminds you that you're heading toward what is likely to be
		// the Final Battle against your Nemesis, and therefore you
		// should probably equip that Legendary Epic Weapon of yours
		// first. Just sayin'.
		if ( this.responseText.indexOf( "A niggling voice" ) != -1 )
		{
			// Should we auto-equip the LEW?
			KoLmafia.updateDisplay( MafiaState.ERROR, "Equip your Legendary Epic Weapon and try again." );
			return;
		}

		// Still, it wouldn't be a final battle without an especially
		// fiendish final puzzle, now would it?

		VolcanoMazeRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "volcanomaze.php" ) )
		{
			return;
		}

		// Parse and save the map
		VolcanoMazeManager.parseResult( responseText );
	}

	private static final Pattern MOVE_PATTERN = Pattern.compile("move=((\\d+),(\\d+))");

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "volcanomaze.php" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "jump=1" ) != -1 )
		{
			RequestLogger.updateSessionLog( "Swimming back to shore" );
			return true;
		}

		Matcher matcher = VolcanoMazeRequest.MOVE_PATTERN.matcher( urlString );
		if ( matcher.find() )
		{
			String message = "Hopping from " + VolcanoMazeManager.currentCoordinates() + " to " + matcher.group(1);
			RequestLogger.updateSessionLog( message );
			return true;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Visiting the lava maze" );

		return true;
	}
}
