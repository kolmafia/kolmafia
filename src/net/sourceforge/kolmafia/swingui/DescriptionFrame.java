package net.sourceforge.kolmafia.swingui;

import javax.swing.JFrame;
import javax.swing.ToolTipManager;

import net.sourceforge.kolmafia.ImageCachingEditorKit;
import net.sourceforge.kolmafia.RequestEditorKit;

import net.sourceforge.kolmafia.request.GenericRequest;

public class DescriptionFrame
	extends RequestFrame
{
	private static DescriptionFrame INSTANCE = null;

	public DescriptionFrame( final String title )
	{
		super( title );
		this.mainDisplay.setEditorKit( new ImageCachingEditorKit() );
		ToolTipManager.sharedInstance().registerComponent( this.mainDisplay );
	}

	public DescriptionFrame()
	{
		this( "Documentation" );
		DescriptionFrame.INSTANCE = this;
	}

	@Override
	public boolean hasSideBar()
	{
		return false;
	}

	public static final void showLocation( final String location )
	{
		DescriptionFrame.showRequest( RequestEditorKit.extractRequest( location ) );
	}

	public static final void showRequest( final GenericRequest request )
	{
		if ( DescriptionFrame.INSTANCE == null )
		{
			GenericFrame.createDisplay( DescriptionFrame.class );
		}
		else
		{
			// Ensure it is brought to front if only refreshing
			int sta = DescriptionFrame.INSTANCE.getExtendedState() & ~JFrame.ICONIFIED & JFrame.NORMAL;
			DescriptionFrame.INSTANCE.setExtendedState(sta);
			DescriptionFrame.INSTANCE.setAlwaysOnTop( true );
			DescriptionFrame.INSTANCE.toFront();
			DescriptionFrame.INSTANCE.requestFocus();
			DescriptionFrame.INSTANCE.setAlwaysOnTop( false );
		}
		DescriptionFrame.INSTANCE.refresh( request );
	}
}
