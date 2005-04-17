/**
 * Copyright (c) 2005, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;
import javax.swing.JOptionPane;

/**
 * The main class for the <code>KoLmafia</code> package.  This
 * class encapsulates most of the data relevant to any given
 * session of <code>Kingdom of Loathing</code> and currently
 * functions as the blackboard in the architecture.  When data
 * listeners are implemented, it will continue to manage most
 * of the interactions.
 */

public class KoLmafiaGUI extends KoLmafia
{
	private KoLFrame activeFrame;

	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafia</code> client after setting the default
	 * look and feel of all <code>JFrame</code> objects to decorated.
	 */

	public static void main( String [] args )
	{
		javax.swing.JFrame.setDefaultLookAndFeelDecorated( true );
    	KoLmafiaGUI session = new KoLmafiaGUI();

		String autoLoginSetting =  session.settings.getProperty( "autoLogin" );
		if ( autoLoginSetting != null )
			(new LoginRequest( session, autoLoginSetting, session.getSaveState( autoLoginSetting ), false, false, false )).run();
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public void updateDisplay( int state, String message )
	{
		if ( activeFrame != null )
			activeFrame.updateDisplay( state, message );
	}

	public void requestFocus()
	{
		if ( activeFrame != null )
			activeFrame.requestFocus();
	}

	/**
	 * Initializes the <code>KoLmafia</code> session.  Called after
	 * the login has been confirmed to notify the client that the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String loginname, String sessionID, boolean getBreakfast, boolean isQuickLogin )
	{
		super.initialize( loginname, sessionID, getBreakfast, isQuickLogin );

		if ( !isLoggingIn )
		{
			KoLFrame previousActiveFrame = activeFrame;

			activeFrame = new AdventureFrame( this, AdventureDatabase.getAsLockableListModel( this ), tally );
			activeFrame.pack();
			activeFrame.setLocationRelativeTo( previousActiveFrame );
			activeFrame.setVisible( true );
			previousActiveFrame.setVisible( false );

			activeFrame.requestFocus();
			activeFrame.updateDisplay( ENABLED_STATE, MoonPhaseDatabase.getMoonEffect() );
			previousActiveFrame.dispose();
		}
	}

	/**
	 * Deinitializes the <code>KoLmafia</code> session.  Called after
	 * the user has logged out.
	 */

	public void deinitialize()
	{
		super.deinitialize();

		if ( activeFrame == null )
		{
			activeFrame = new LoginFrame( this, saveStateNames );
			activeFrame.pack();
			activeFrame.setLocationRelativeTo( null );
			activeFrame.setVisible( true );
			activeFrame.requestFocus();
		}
	}

	/**
	 * Makes a request to the hermit, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hermit.
	 */

	protected void makeHermitRequest()
	{
		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want this from the hermit...", "Mugging Hermit for...", JOptionPane.INFORMATION_MESSAGE, null,
			hermitItemNames, hermitItemNames[0] );

		if ( selectedValue == null )
			return;

		int selected = -1;
		for ( int i = 0; selected == -1 && i < hermitItemNames.length; ++i )
			if ( selectedValue.equals( hermitItemNames[i] ) )
				selected = hermitItemNumbers[i];

		try
		{
			int tradeCount = df.parse( JOptionPane.showInputDialog(
				null, "How many " + selectedValue + " to get?", "I want this many!", JOptionPane.INFORMATION_MESSAGE ) ).intValue();

			(new HermitRequest( this, selected, tradeCount )).run();
		}
		catch ( Exception e )
		{
		}
	}

	/**
	 * Makes a request to the trapper, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the trapper.
	 */

	protected void makeTrapperRequest()
	{
		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want this from the trapper...", "1337ing Trapper for...", JOptionPane.INFORMATION_MESSAGE, null,
			trapperItemNames, trapperItemNames[0] );

		if ( selectedValue == null )
			return;

		for ( int i = 0; i < trapperItemNames.length; ++i )
			if ( selectedValue.equals( trapperItemNames[i] ) )
			{
				(new TrapperRequest( this, trapperItemNumbers[i] )).run();
				return;
			}
	}

	/**
	 * Confirms whether or not the user wants to make a drunken
	 * request.  This should be called before doing requests when
	 * the user is in an inebrieted state.
	 *
	 * @return	<code>true</code> if the user wishes to adventure drunk
	 */

	protected boolean confirmDrunkenRequest()
	{
		return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
			"The mafia has stolen your shoes!  Continue adventuring anyway?",
			"You're not drunk!  You see flying penguins and a dancing hermit!", JOptionPane.YES_NO_OPTION );
	}

	public void setVisible( boolean isVisible )
	{	activeFrame.setVisible( isVisible );
	}

	public boolean isVisible()
	{	return activeFrame.isVisible();
	}

	public void deinitializeBuffBot()
	{
		super.deinitializeBuffBot();
		activeFrame.setVisible( true );
	}

	public void makeRequest( Runnable request, int iterations )
	{
		super.makeRequest( request, iterations );
		((AdventureFrame)activeFrame).refreshConcoctionsList();
	}
}
