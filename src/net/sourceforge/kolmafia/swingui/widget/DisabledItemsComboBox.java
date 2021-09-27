package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Component;

import java.util.HashSet;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.UIManager;
     
//DisabledItemsComboBox is a type of JComboBox that
//can disable/enable individual elements.  
public class DisabledItemsComboBox 
	extends JComboBox 
{
	private List<String> tooltips;
	private final HashSet disabledItems = new HashSet(); //these indices will be disabled
	private final DisabledItemsRenderer itemRenderer = new DisabledItemsRenderer();
	
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

	public void clearDisabledItems()
	{
		this.disabledItems.clear();
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
	public void setTooltips( List<String> tooltips )
	{
		this.tooltips = tooltips;
	}

	public void dumpDisabledItems()
	{
		for ( Object item: disabledItems )
		{
			System.out.println( item );
		}
		System.out.println( "-------" );
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
					String text = tooltips.get( index );

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
