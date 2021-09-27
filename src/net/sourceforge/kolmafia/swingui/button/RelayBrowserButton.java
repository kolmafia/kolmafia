package net.sourceforge.kolmafia.swingui.button;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.swingui.listener.RelayBrowserListener;

public class RelayBrowserButton
	extends ThreadedButton
{
	public RelayBrowserButton( final String label, final String location )
	{
		super( label, new RelayBrowserListener( location ) );
		JComponentUtilities.setComponentSize( this, 32, 32 );
	}

	public RelayBrowserButton( final String tooltip, final String icon, final String location )
	{
		super( JComponentUtilities.getImage( icon ), new RelayBrowserListener( location ) );
		JComponentUtilities.setComponentSize( this, 32, 32 );
		this.setToolTipText( tooltip );
	}

	@Override
	public String toString()
	{
		return this.getText();
	}
}
