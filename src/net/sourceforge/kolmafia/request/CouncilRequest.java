package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLCharacter;

public class CouncilRequest
	extends GenericRequest
{
	public CouncilRequest()
	{
		super( KoLCharacter.isKingdomOfExploathing() ? "place.php" : "council.php" );
		if ( KoLCharacter.isKingdomOfExploathing() )
		{
			this.addFormField( "whichplace", "exploathing" );
			this.addFormField( "action", "expl_council" );
		}
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return KoLCharacter.isKingdomOfExploathing();
	}

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
}
