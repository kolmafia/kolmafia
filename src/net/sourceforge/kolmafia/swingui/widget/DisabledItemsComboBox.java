/**
 * Copyright (c) 2005-2011, KoLmafia development team
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
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.UIManager;
     
//DisabledItemsComboBox is a type of JComboBox that
//can disable/enable individual elements.  
public class DisabledItemsComboBox 
	extends JComboBox 
{
	private ArrayList disabledItems = new ArrayList(); //these indices will be disabled
	private DisabledItemsRenderer Drenderer = new DisabledItemsRenderer();
	
	public DisabledItemsComboBox() 
	{
		super();
		//need to use a custom renderer in order to disable
		//individual items within a JComboBox.
		super.setRenderer( this.Drenderer );
	}

	public void addItem(Object anObject, boolean disabled) 
	{
		super.addItem(anObject);
		if (disabled) 
		{
			this.getdisabledItems().add( new Integer( (getItemCount() - 1) ) );
		}
	}

	public void removeAllItems() 
	{
		super.removeAllItems();
		this.setdisabledItems( new ArrayList() );
	}

	public void removeItemAt(final int anIndex) 
	{
		super.removeItemAt(anIndex);
		this.getdisabledItems().remove( new Integer( anIndex ) );
	}

	public void removeItem(final Object anObject) 
	{
		for (int i = 0; i < getItemCount(); i++) 
		{
			if ( getItemAt( i ) == anObject ) 
			{
				this.getdisabledItems().remove( new Integer( i ) );
			}
		}
		super.removeItem(anObject);
	}

	public void setSelectedIndex(int index) 
	{
		if ( !this.disabledItems.contains(new Integer ( index ) ) ) 
		{
			super.setSelectedIndex(index);
		}
	}

	// This is called whenever we have an existing element that
	// we want to disable or reenable.
	public void setDisabledIndex(int index, boolean disabled)
	{
		if ( disabled ) 
		{
			this.getdisabledItems().add( new Integer( index ) );
		}
		else if ( this.getdisabledItems().contains( new Integer( index ) ) ) 
		{
			this.getdisabledItems().remove( new Integer( index ) );
		}
	}
      
	// provides access to the renderer setTooltips
	public void setTooltips(ArrayList tooltips)
	{
		this.getDrenderer().setTooltips(tooltips);
	}

	/**
	 * @return the Drenderer
	 */
	public DisabledItemsRenderer getDrenderer()
	{
		return Drenderer;
	}

	/**
	 * @param Drenderer the Drenderer to set
	 */
	public void setDrenderer(DisabledItemsRenderer Drenderer)
	{
		this.Drenderer = Drenderer;
	}

	/**
	 * @return the disabledItems
	 */
	public ArrayList getdisabledItems()
	{
		return disabledItems;
	}

	/**
	 * @param disabledItems the disabledItems to set
	 */
	public void setdisabledItems(ArrayList disabledItems)
	{
		this.disabledItems = disabledItems;
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
		private ArrayList tooltips;
		
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) 
		{ 
			if (isSelected) 
			{
				setBackground( list.getSelectionBackground() );
				setForeground( list.getSelectionForeground() );
			} 
			else 
			{
				setBackground( list.getBackground() );
				setForeground( list.getForeground() );
			}

			if ( getdisabledItems().contains(new Integer(index) ) ) 
			{
				setBackground( list.getBackground( ) );

				if ( UIManager.getColor("Label.disabledForeground") != null )
				{
					setForeground( UIManager.getColor("Label.disabledForeground") );
				}
				//Nimbus uses different conventions than every other L+F.  Blah.
				else
				{
					setForeground( UIManager.getColor("List[Disabled].textForeground") );
				}
			}

			if ( -1 < index && null != value && null != getTooltips() ) 
			{
				String text = index < this.getTooltips().size() ?
					(String) getTooltips().get( index ) :
					null;

				list.setToolTipText( text );
			}
			setFont( list.getFont() );
			setText( ( value == null ) ? "" : value.toString() );
			return this;
		}
		
		public void setTooltips(ArrayList tooltips) 
		{
			this.tooltips = tooltips;
		}

		/**
		 * @return the tooltips
		 */
		public ArrayList getTooltips()
		{
			return tooltips;
		}
	}
}
