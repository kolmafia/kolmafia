package net.sourceforge.kolmafia.request;

import net.java.dev.spellcast.utilities.LockableListModel;

public class ClanBuffRequest
	extends GenericRequest
{
	private final int buffId;

	/**
	 * Constructs a new <code>ClanBuffRequest</code> with the specified buff identifier. This constructor is only
	 * available internally. Note that no descendents are possible because of the nature of the constructor.
	 *
	 * @param buffId The unique numeric identifier of the buff
	 */

	private ClanBuffRequest( final int buffId )
	{
		super( "clan_stash.php" );

		this.buffId = buffId;
		this.addFormField( "action", "buyround" );
		this.addFormField( "size", String.valueOf( buffId % 10 ) );
		this.addFormField( "whichgift", String.valueOf( ( buffId / 10 ) ) );
	}

	/**
	 * Returns a list of all the possible requests available through the current implementation of
	 * <code>ClanBuffRequest</code>.
	 *
	 * @return A complete <code>ListModel</code>
	 */

	public static final LockableListModel<ClanBuffRequest> getRequestList()
	{
		LockableListModel<ClanBuffRequest> requestList = new LockableListModel<ClanBuffRequest>();
		for ( int i = 1; i < 9; ++i )
		{
			for ( int j = 1; j <= 3; ++j )
			{
				requestList.add( new ClanBuffRequest( 10 * i + j ) );
			}
		}

		requestList.add( new ClanBuffRequest( 91 ) );

		return requestList;
	}

	/**
	 * Returns the string form of this request, which is the formal name of the buff that this buff request represents.
	 *
	 * @return The formal name of the clan buff requested
	 */

	@Override
	public String toString()
	{
		StringBuilder stringForm = new StringBuilder();
		int size = this.buffId % 10;
		int gift = this.buffId / 10;

		if ( gift != 9 )
		{
			switch ( size )
			{
			case 1:
				stringForm.append( "Cheap " );
				break;
			case 2:
				stringForm.append( "Normal " );
				break;
			case 3:
				stringForm.append( "Expensive " );
				break;
			}
		}

		switch ( gift )
		{
		case 1:
			stringForm.append( "Muscle Training" );
			break;
		case 2:
			stringForm.append( "Mysticality Training" );
			break;
		case 3:
			stringForm.append( "Moxie Training" );
			break;
		case 4:
			stringForm.append( "Temporary Muscle Boost" );
			break;
		case 5:
			stringForm.append( "Temporary Mysticality Boost" );
			break;
		case 6:
			stringForm.append( "Temporary Moxie Boost" );
			break;
		case 7:
			stringForm.append( "Temporary Item Drop Boost" );
			break;
		case 8:
			stringForm.append( "Temporary Meat Drop Boost" );
			break;
		case 9:
			stringForm.append( "Adventure Massage" );
			break;
		}

		return stringForm.toString();
	}
}
