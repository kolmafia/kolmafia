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
	private CreateFrameRunnable displayer;
	private LimitedSizeChatBuffer buffer;

	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafia</code> client after setting the default
	 * look and feel of all <code>JFrame</code> objects to decorated.
	 */

	public static void main( String [] args )
	{
		javax.swing.JFrame.setDefaultLookAndFeelDecorated( true );
    	KoLmafiaGUI session = new KoLmafiaGUI();

		String login = session.settings.getProperty( "autoLogin" );
		String password = session.getSaveState( login );

		if ( password != null )
			(new LoginRequest( session, login, password, false, false, false )).run();
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public void updateDisplay( int state, String message )
	{
		super.updateDisplay( state, message );

		if ( displayer != null && displayer.getCreation() != null )
		{
			((KoLFrame)displayer.getCreation()).updateDisplay( state, message );
			if ( isBuffBotActive() )
				buffBotHome.updateStatus( message );
		}
	}

	public void requestFocus()
	{
		if ( displayer != null && displayer.getCreation() != null )
			((KoLFrame)displayer.getCreation()).requestFocus();
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

		if ( loginRequest != null )
		{
			updateDisplay( ENABLED_STATE, "Session timed-in." );
			return;
		}

		if ( !isLoggingIn )
		{
			CreateFrameRunnable previousDisplayer = displayer;

			Object [] parameters = new Object[2];
			parameters[0] = this;
			parameters[1] = tally;

			displayer = new CreateFrameRunnable( AdventureFrame.class, parameters );
			displayer.run();

			((KoLFrame)previousDisplayer.getCreation()).setVisible( false );
			((KoLFrame)previousDisplayer.getCreation()).dispose();
		}
	}

	/**
	 * Deinitializes the <code>KoLmafia</code> session.  Called after
	 * the user has logged out.
	 */

	public void deinitialize()
	{
		super.deinitialize();

		Object [] parameters = new Object[2];
		parameters[0] = this;
		parameters[1] = saveStateNames;

		if ( displayer == null )
		{
			displayer = new CreateFrameRunnable( LoginFrame.class, parameters );
			displayer.run();
		}
	}

	public void pwnClanOtori()
	{
		if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
			"This attempt to pwn Clan Otori will cost you 13 meat.\nAre you sure you want to continue?",
			"YES!  This does use meat!", JOptionPane.YES_NO_OPTION ) )
				return;

		super.pwnClanOtori();
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
	 * Makes a request to the hunter, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hunter.
	 */

	protected void makeHunterRequest()
	{
		if ( hunterItems.isEmpty() )
			(new BountyHunterRequest( this )).run();

		Object [] hunterItemArray = hunterItems.toArray();

		String selectedValue = (String) JOptionPane.showInputDialog(
			null, "I want to sell this to the hunter...", "The Quilted Thicker Picker Upper!", JOptionPane.INFORMATION_MESSAGE, null,
			hunterItemArray, hunterItemArray[0] );

		if ( selectedValue != null )
			(new BountyHunterRequest( this, TradeableItemDatabase.getItemID( selectedValue ) )).run();

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
			" You see flying penguins and a dancing hermit!\nThe mafia has stolen your shoes!\n(KoLmafia thinks you're too drunk!)\nContinue adventuring anyway?\n",
			"You're not drunk!?", JOptionPane.YES_NO_OPTION );
	}

	public void setVisible( boolean isVisible )
	{
		if ( displayer != null && displayer.getCreation() != null )
			((KoLFrame)displayer.getCreation()).setVisible( isVisible );
	}

	public boolean isVisible()
	{
		if ( displayer != null && displayer.getCreation() != null )
			return ((KoLFrame)displayer.getCreation()).isVisible();
		return false;
	}

	public void deinitializeBuffBot()
	{
		super.deinitializeBuffBot();

		if ( displayer != null && displayer.getCreation() != null )
			((KoLFrame)displayer.getCreation()).setVisible( true );
	}
}
