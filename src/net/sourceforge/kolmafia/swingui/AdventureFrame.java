/**
 * Copyright (c) 2005-2008, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.swingui.panel.AdventureSelectPanel;
import net.sourceforge.kolmafia.swingui.panel.ChoiceOptionsPanel;
import net.sourceforge.kolmafia.swingui.panel.CustomCombatPanel;
import net.sourceforge.kolmafia.swingui.panel.MoodOptionsPanel;
import net.sourceforge.kolmafia.swingui.panel.RestoreOptionsPanel;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public class AdventureFrame
	extends GenericFrame
{
	private static AdventureSelectPanel adventureSelector = null;
	private static JProgressBar requestMeter = null;
	private static JSplitPane sessionGrid;

	/**
	 * Constructs a new <code>AdventureFrame</code>. All constructed panels are placed into their corresponding tabs,
	 * with the content panel being defaulted to the adventure selection panel.
	 */

	public AdventureFrame()
	{
		super( "Adventure" );

		// Construct the adventure select container
		// to hold everything related to adventuring.

		AdventureFrame.adventureSelector = new AdventureSelectPanel( true );

		JPanel adventureDetails = new JPanel( new BorderLayout( 20, 20 ) );
		adventureDetails.add( AdventureFrame.adventureSelector, BorderLayout.CENTER );

		AdventureFrame.requestMeter = new JProgressBar();
		AdventureFrame.requestMeter.setOpaque( true );
		AdventureFrame.requestMeter.setStringPainted( true );

		JPanel meterPanel = new JPanel( new BorderLayout( 10, 10 ) );
		meterPanel.add( Box.createHorizontalStrut( 20 ), BorderLayout.WEST );
		meterPanel.add( AdventureFrame.requestMeter, BorderLayout.CENTER );
		meterPanel.add( Box.createHorizontalStrut( 20 ), BorderLayout.EAST );

		adventureDetails.add( meterPanel, BorderLayout.SOUTH );

		this.framePanel.setLayout( new BorderLayout( 20, 20 ) );
		this.framePanel.add( adventureDetails, BorderLayout.NORTH );
		this.framePanel.add( this.getSouthernTabs(), BorderLayout.CENTER );

		AdventureFrame.updateSelectedAdventure( AdventureDatabase.getAdventure( Preferences.getString( "lastAdventure" ) ) );
		AdventureFrame.adventureSelector.fillDefaultConditions();

		JComponentUtilities.setComponentSize( this.framePanel, 640, 480 );
	}

	public boolean shouldAddStatusBar()
	{
		return false;
	}

	public void setStatusMessage( final String message )
	{
		if ( AdventureFrame.requestMeter == null || message.length() == 0 )
		{
			return;
		}

		AdventureFrame.requestMeter.setString( message );
	}

	public static final void updateRequestMeter( final int value, final int maximum )
	{
		if ( AdventureFrame.requestMeter == null )
		{
			return;
		}

		AdventureFrame.requestMeter.setMaximum( maximum );
		AdventureFrame.requestMeter.setValue( value );
	}

	public static final void updateSelectedAdventure( final KoLAdventure location )
	{
		if ( AdventureFrame.adventureSelector == null )
		{
			return;
		}

		AdventureFrame.adventureSelector.updateSelectedAdventure( location );
	}

	public boolean useSidePane()
	{
		return true;
	}

	public JTabbedPane getSouthernTabs()
	{
		// Components of custom combat and choice adventuring,
		// combined into one friendly panel.

		GenericScrollPane restoreScroller = new GenericScrollPane( new RestoreOptionsPanel() );
		JComponentUtilities.setComponentSize( restoreScroller, 560, 400 );

		this.tabs.addTab( "HP/MP Usage", restoreScroller );

		this.tabs.addTab( "Mood Setup", new MoodOptionsPanel() );
		this.tabs.addTab( "Custom Combat", new CustomCombatPanel() );

		this.tabs.insertTab( "Overview", null, this.getAdventureSummary(), null, 0 );
		ChoiceOptionsPanel choicePanel = new ChoiceOptionsPanel();
		this.tabs.insertTab( "Choice Advs", null, new GenericScrollPane( choicePanel ), null, 1 );

		AdventureFrame.adventureSelector.addSelectedLocationListener( choicePanel.getUpdateListener() );
		return this.tabs;
	}

	private JSplitPane getAdventureSummary()
	{
		AdventureFrame.sessionGrid =
			new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true,
				AdventureFrame.adventureSelector.getAdventureSummary( "defaultDropdown1" ),
				AdventureFrame.adventureSelector.getAdventureSummary( "defaultDropdown2" ) );

		int location = Preferences.getInteger( "defaultDropdownSplit" );

		if ( location == 0 )
		{
			AdventureFrame.sessionGrid.setDividerLocation( 0.5 );
		}
		else
		{
			AdventureFrame.sessionGrid.setDividerLocation( location );
		}

		AdventureFrame.sessionGrid.setResizeWeight( 0.5 );
		return AdventureFrame.sessionGrid;
	}

	public void dispose()
	{
		Preferences.setInteger( "defaultDropdownSplit", this.sessionGrid.getLastDividerLocation() );
		super.dispose();
	}
}
