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
 *	notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *	notice, this list of conditions and the following disclaimer in
 *	the documentation and/or other materials provided with the
 *	distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *	be used to endorse or promote products derived from this software
 *	without specific prior written permission.
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

package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Component;

import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.UIManager;
     
//DisabledItemsComboBox is a type of JComboBox that
//can disable/enable individual elements.  
public class DisabledItemsComboBox 
	extends JComboBox 
{
	private ArrayList tooltips;
	private HashSet disabledItems = new HashSet(); //these indices will be disabled
	private DisabledItemsRenderer itemRenderer = new DisabledItemsRenderer();
	
	public DisabledItemsComboBox() 
	{
		super();
		//need to use a custom renderer in order to disable
		//individual items within a JComboBox.
		super.setRenderer( this.itemRenderer );
	}

	public void addItem( Object object, boolean disabled ) 
	{
		super.addItem( object );

		if ( disabled )
		{
			this.disabledItems.add( object.toString() );
		}
	}

	@Override
	public void removeAllItems() 
	{
		super.removeAllItems();
		this.disabledItems.clear();
	}

	@Override
	public void removeItemAt( final int index )
	{
		if ( index < 0 || index >= getItemCount() )
		{
			return;
		}

		Object object = super.getItemAt( index );
		this.removeItem( object );
	}

	@Override
	public void removeItem( final Object object )
	{
		if ( object != null )
		{
			this.disabledItems.remove( object.toString() );
			super.removeItem( object );
		}
	}

	@Override
	public void setSelectedIndex( int index )
	{
		if ( index < 0 || index >= getItemCount() )
		{
			super.setSelectedItem( null );
			return;
		}

		Object object = super.getItemAt( index );
		if ( !this.disabledItems.contains( object.toString() ) )
		{
			super.setSelectedIndex( index );
		}
	}

	// This is called whenever we have an existing element that
	// we want to disable or reenable.
	public void setDisabledIndex( int index, boolean disabled )
	{
		if ( index < 0 || index >= getItemCount() )
		{
			return;
		}

		Object object = super.getItemAt( index );

		if ( disabled )
		{
			this.disabledItems.add( object.toString() );
		}
		else
		{
			this.disabledItems.remove( object.toString() );
		}
	}
      
	// provides access to the renderer setTooltips
	public void setTooltips( ArrayList tooltips )
	{
		this.tooltips = tooltips;
	}

	// Custom renderer to disable individual items within a combo box
	// with individual item tooltips
	// AFAIK nothing else in mafia implements individually disabled list elements.
	// If something other than a JComboBox needs to disable list items, then this
	// renderer should be pulled out and placed in ListCellRendererFactory.
	// (and made public)
	private class DisabledItemsRenderer 
		extends DefaultListCellRenderer
	{		
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) 
		{ 
			if ( isSelected )
			{
				setBackground( list.getSelectionBackground() );
				setForeground( list.getSelectionForeground() );
			} 
			else 
			{
				setBackground( list.getBackground() );
				setForeground( list.getForeground() );
			}

			if ( value != null && disabledItems.contains( value.toString() ) )
			{
				setBackground( list.getBackground( ) );

				if ( UIManager.getColor( "Label.disabledForeground" ) != null )
				{
					setForeground( UIManager.getColor( "Label.disabledForeground" ) );
				}
				//Nimbus uses different conventions than every other L+F.  Blah.
				else
				{
					setForeground( UIManager.getColor( "List[Disabled].textForeground" ) );
				}
			}

			if ( value != null && tooltips != null ) 
			{
				if ( index >= 0 && index < tooltips.size() )
				{
					String text = (String) tooltips.get( index );

					list.setToolTipText( text );
				}
				else
				{
					list.setToolTipText( null );
				}
			}

			setFont( list.getFont() );
			setText( ( value == null ) ? "" : value.toString() );

			return this;
		}
	}
}
