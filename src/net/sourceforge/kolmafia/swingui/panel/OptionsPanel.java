/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.Dimension;

import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.swingui.widget.CreationSettingCheckBox;

/**
 * A generic panel which adds a label to the bottom of the KoLPanel to update the panel's status. It also provides a
 * thread which is guaranteed to be a daemon thread for updating the frame which also retrieves a reference to the
 * StaticEntity.getClient()'s current settings.
 */

public abstract class OptionsPanel
	extends GenericPanel
{
	private String[][] options;
	protected JCheckBox[] optionBoxes;
	protected boolean refreshConcoctions;

	public OptionsPanel()
	{
		this( new Dimension( 130, 20 ), new Dimension( 260, 20 ) );
	}

	public OptionsPanel( final Dimension left, final Dimension right )
	{
		super( left, right );

		this.refreshConcoctions = false;
	}

	public void setOptions( String[][] options )
	{
		this.options = options;

		VerifiableElement[] elements = new VerifiableElement[ this.options.length ];

		this.optionBoxes = new JCheckBox[ this.options.length ];

		for ( int i = 0; i < this.options.length; ++i )
		{
			String[] option = this.options[ i ];

			if ( option.length == 0 )
			{
				elements[ i ] = new VerifiableElement();
			}
			else if ( option.length == 1 )
			{
				String text = option[ 0 ];

				JTextArea message = new JTextArea( text );
				message.setColumns( 38 );
				message.setLineWrap( true );
				message.setWrapStyleWord( true );
				message.setEditable( false );
				message.setOpaque( false );
				message.setFont( KoLConstants.DEFAULT_FONT );

				elements[ i ] = new VerifiableElement( message );
			}
			else if ( option.length == 2 )
			{
				this.optionBoxes[ i ] = new JCheckBox();
				elements[ i ] = new VerifiableElement( option[ 1 ], SwingConstants.LEFT, this.optionBoxes[ i ] );
			}
			else
			{
				this.refreshConcoctions = true;
				this.optionBoxes[ i ] = new CreationSettingCheckBox( option[ 0 ] );
				elements[ i ] = new VerifiableElement( option[ 1 ], SwingConstants.LEFT, this.optionBoxes[ i ] );
			}
		}

		this.setContent( elements );
		this.actionCancelled();
	}

	@Override
	public void setEnabled( final boolean isEnabled )
	{
	}

	@Override
	public boolean shouldAddStatusLabel()
	{
		return false;
	}

	@Override
	public void actionConfirmed()
	{
		if ( this.optionBoxes == null )
		{
			return;
		}

		for ( int i = 0; i < this.options.length; ++i )
		{
			String[] option = this.options[ i ];
			JCheckBox optionBox = this.optionBoxes[ i ];

			if ( option.length == 0 || optionBox == null )
			{
				continue;
			}

			Preferences.setBoolean( option[ 0 ], optionBox.isSelected() );
		}

		if ( this.refreshConcoctions )
		{
			ConcoctionDatabase.refreshConcoctions();
		}
	}

	@Override
	public void actionCancelled()
	{
		if ( this.optionBoxes == null )
		{
			return;
		}

		for ( int i = 0; i < this.options.length; ++i )
		{
			String[] option = this.options[ i ];
			JCheckBox optionBox = this.optionBoxes[ i ];

			if ( option.length == 0 || optionBox == null )
			{
				continue;
			}

			optionBox.setSelected( Preferences.getBoolean( option[ 0 ] ) );
		}
	}
}
