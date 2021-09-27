package net.sourceforge.kolmafia.swingui;

import net.java.dev.spellcast.utilities.ActionPanel;

public class GenericPanelFrame
	extends GenericFrame
{
	/**
	 * Constructs an empty <code>KoLPanelFrame</code> which uses the givenand has the given title.
	 */

	public GenericPanelFrame( final String title )
	{
		super( title );
	}

	/**
	 * Constructs a <code>KoLPanelFrame</code> which contains the givenand has the given title. The content panel for
	 * this frame will be initialized to the panel that is provided.
	 */

	public GenericPanelFrame( final String title, final ActionPanel panel )
	{
		super( title );
		this.setContentPanel( panel );
	}

	/**
	 * Sets the content panel for this <code>KoLPanelFrame</code> to the given panel. This can only be called once, and
	 * is used to initialize the <code>KoLPanelFrame</code> in the event that the panel is not known at construction
	 * time (for example, for descendant classes).
	 */

	public void setContentPanel( final ActionPanel panel )
	{
		if ( this.getClass() == GenericPanelFrame.class )
		{
			this.frameName = panel.getClass().getName();
			this.frameName = this.frameName.substring( this.frameName.lastIndexOf( "." ) + 1 );
		}

		this.setCenterComponent( panel );
	}
}
