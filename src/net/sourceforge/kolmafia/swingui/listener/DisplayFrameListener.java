package net.sourceforge.kolmafia.swingui.listener;

import java.awt.Frame;
 	 
import net.sourceforge.kolmafia.KoLDesktop;	 
import net.sourceforge.kolmafia.KoLmafiaGUI;	 
 	 
import net.sourceforge.kolmafia.preferences.Preferences;
	 
import net.sourceforge.kolmafia.swingui.GenericFrame;

public class DisplayFrameListener
	extends ThreadedListener
{
	private final String frameClass;

	public DisplayFrameListener( String frameClass )
	{
		this.frameClass = frameClass;
	}

	protected void execute()
	{
		 if ( this.frameClass == null )	 
		 {	 
			 String interfaceSetting = Preferences.getString( "initialDesktop" );	 
	 
			 Frame [] frames = Frame.getFrames();	 
	 
			 for ( int i = 0; i < frames.length; ++i )	 
			 {	 
				 if ( ( frames[ i ] instanceof GenericFrame ) )	 
				 {	 
					 GenericFrame frame = (GenericFrame) frames[ i ];	 
	 
					 if ( frame.showInWindowMenu() && interfaceSetting.indexOf( frame.getFrameName() ) == -1 )	 
					 {	 
						 frame.setVisible( true );	 
					 }	 
				 }	 
			 }	 
	 
			 if ( KoLDesktop.instanceExists() )	 
			 {	 
				 KoLDesktop.getInstance().setVisible( true );	 
			 }	 
		 }	 
		 else	 
		 {	 
			 KoLmafiaGUI.constructFrame( this.frameClass );	 
		 }
	}
}
