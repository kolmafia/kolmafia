package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.Concoction;

public class Crimbo05Request
	extends CreateItemRequest
{
	public Crimbo05Request( final Concoction conc )
	{
		super( "crimbo_uncle.php", conc );

		this.addFormField( "action", "1" );
		this.addFormField( "whichitem", String.valueOf( this.getItemId() ) );
	}

	@Override
	public void run()
	{
		// Attempting to make the ingredients will pull the
		// needed items from the closet if they are missing.
		// In this case, it will also create the needed white
		// pixels if they are not currently available.

		if ( !this.makeIngredients() )
		{
			return;
		}

		KoLmafia.updateDisplay( "Creating " + this.getQuantityNeeded() + " " + this.getName() + "..." );
		this.addFormField( "quantity", String.valueOf( this.getQuantityNeeded() ) );
		super.run();
	}
}
