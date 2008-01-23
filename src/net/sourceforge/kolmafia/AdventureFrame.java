/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.swingui.panel.ChoiceOptionsPanel;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public class AdventureFrame
	extends AdventureOptionsFrame
{
	private static AdventureFrame INSTANCE = null;
	private JProgressBar requestMeter = null;

	private JSplitPane sessionGrid;
	private final AdventureSelectPanel adventureSelect;

	/**
	 * Constructs a new <code>AdventureFrame</code>. All constructed panels are placed into their corresponding tabs,
	 * with the content panel being defaulted to the adventure selection panel.
	 */

	public AdventureFrame()
	{
		super( "Adventure" );

		// Construct the adventure select container
		// to hold everything related to adventuring.

		AdventureFrame.INSTANCE = this;
		this.adventureSelect = new AdventureSelectPanel( true );

		JPanel adventureDetails = new JPanel( new BorderLayout( 20, 20 ) );
		adventureDetails.add( this.adventureSelect, BorderLayout.CENTER );

		this.requestMeter = new JProgressBar();
		this.requestMeter.setOpaque( true );
		this.requestMeter.setStringPainted( true );

		JPanel meterPanel = new JPanel( new BorderLayout( 10, 10 ) );
		meterPanel.add( Box.createHorizontalStrut( 20 ), BorderLayout.WEST );
		meterPanel.add( this.requestMeter, BorderLayout.CENTER );
		meterPanel.add( Box.createHorizontalStrut( 20 ), BorderLayout.EAST );

		adventureDetails.add( meterPanel, BorderLayout.SOUTH );

		this.framePanel.setLayout( new BorderLayout( 20, 20 ) );
		this.framePanel.add( adventureDetails, BorderLayout.NORTH );
		this.framePanel.add( this.getSouthernTabs(), BorderLayout.CENTER );

		AdventureFrame.updateSelectedAdventure( AdventureDatabase.getAdventure( Preferences.getString( "lastAdventure" ) ) );
		this.fillDefaultConditions();

		JComponentUtilities.setComponentSize( this.framePanel, 640, 480 );
		CharsheetFrame.removeExtraTabs();
	}

	public boolean shouldAddStatusBar()
	{
		return false;
	}

	public void setStatusMessage( final String message )
	{
		if ( this.requestMeter == null || message.length() == 0 )
		{
			return;
		}

		this.requestMeter.setString( message );
	}

	public static final void updateRequestMeter( final int value, final int maximum )
	{
		if ( AdventureFrame.INSTANCE == null || AdventureFrame.INSTANCE.requestMeter == null )
		{
			return;
		}

		AdventureFrame.INSTANCE.requestMeter.setMaximum( maximum );
		AdventureFrame.INSTANCE.requestMeter.setValue( value );
	}

	public static final void updateSelectedAdventure( final KoLAdventure location )
	{
		if ( AdventureFrame.INSTANCE == null || location == null || AdventureFrame.INSTANCE.zoneSelect == null || AdventureFrame.INSTANCE.locationSelect == null )
		{
			return;
		}

		if ( AdventureFrame.INSTANCE.locationSelect.getSelectedValue() == location || !KoLConstants.conditions.isEmpty() )
		{
			return;
		}

		if ( AdventureFrame.INSTANCE.zoneSelect instanceof FilterAdventureField )
		{
			( (FilterAdventureField) AdventureFrame.INSTANCE.zoneSelect ).setText( location.getZone() );
		}
		else
		{
			( (JComboBox) AdventureFrame.INSTANCE.zoneSelect ).setSelectedItem( location.getParentZoneDescription() );
		}

		AdventureFrame.INSTANCE.locationSelect.setSelectedValue( location, true );
		AdventureFrame.INSTANCE.locationSelect.ensureIndexIsVisible( AdventureFrame.INSTANCE.locationSelect.getSelectedIndex() );
	}

	public boolean useSidePane()
	{
		return true;
	}

	public JTabbedPane getSouthernTabs()
	{
		super.getSouthernTabs();
		this.tabs.insertTab( "Overview", null, this.getAdventureSummary(), null, 0 );
		ChoiceOptionsPanel choicePanel = new ChoiceOptionsPanel();
		this.tabs.insertTab( "Choice Advs", null, new SimpleScrollPane( choicePanel ), null, 1 );
		AdventureFrame.this.locationSelect.addListSelectionListener( choicePanel.getUpdateListener() );
		return this.tabs;
	}

	private JSplitPane getAdventureSummary()
	{
		this.sessionGrid =
			new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true, this.getAdventureSummary(
				"defaultDropdown1", this.locationSelect ), this.getAdventureSummary(
				"defaultDropdown2", this.locationSelect ) );

		int location = Preferences.getInteger( "defaultDropdownSplit" );

		if ( location == 0 )
		{
			this.sessionGrid.setDividerLocation( 0.5 );
		}
		else
		{
			this.sessionGrid.setDividerLocation( location );
		}

		this.sessionGrid.setResizeWeight( 0.5 );
		return this.sessionGrid;
	}

	public void dispose()
	{
		Preferences.setString( "defaultDropdownSplit", String.valueOf( this.sessionGrid.getLastDividerLocation() ) );
		super.dispose();
	}
}
