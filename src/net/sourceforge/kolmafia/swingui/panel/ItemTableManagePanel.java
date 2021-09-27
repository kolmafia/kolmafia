package net.sourceforge.kolmafia.swingui.panel;

import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionTable;

public class ItemTableManagePanel
	extends ItemManagePanel
{
	public ItemTableManagePanel( final String confirmedText, final String cancelledText, final LockableListModel elementModel,
				     final boolean addFilterField, final boolean addRefreshButton, final boolean[] flags )
	{
		super( confirmedText, cancelledText, elementModel, new ShowDescriptionTable( elementModel, flags ), addFilterField, addRefreshButton );

		ShowDescriptionTable elementList = (ShowDescriptionTable) this.scrollComponent;
		elementList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		elementList.setVisibleRowCount( 8 );
		if ( addFilterField )
		{
			this.filterfield.setModel( elementList.getDisplayModel() );
		}
	}

	public ItemTableManagePanel( final String confirmedText, final String cancelledText, final LockableListModel elementModel )
	{
		this( confirmedText, cancelledText, elementModel,
		      true, ItemManagePanel.shouldAddRefreshButton( elementModel ),
		      new boolean[] { false, false });
	}

	public ItemTableManagePanel( final String confirmedText, final String cancelledText, final LockableListModel elementModel,
				     final boolean addFilterField, final boolean addRefreshButton )
	{
		this( confirmedText, cancelledText, elementModel,
		      addFilterField, addRefreshButton,
		      new boolean[] { false, false });
	}
	
	public ItemTableManagePanel( final String confirmedText, final String cancelledText, final LockableListModel elementModel, final boolean[] flags )
	{
		this( confirmedText, cancelledText, elementModel,
		      true, ItemManagePanel.shouldAddRefreshButton( elementModel ),
		      flags);
	}

	public ItemTableManagePanel( final LockableListModel elementModel, final boolean addFilterField,
				     final boolean addRefreshButton, final boolean[] flags )
	{
		this( null, null, elementModel,
		      addFilterField, addRefreshButton,
		      flags );
	}

	public ItemTableManagePanel( final LockableListModel elementModel )
	{
		this( elementModel,
		      true, ItemManagePanel.shouldAddRefreshButton( elementModel ),
		      new boolean[] {false, false} );
	}

	public ItemTableManagePanel( final LockableListModel elementModel, final boolean[] flags )
	{
		this( elementModel,
		      true, ItemManagePanel.shouldAddRefreshButton( elementModel ),
		      flags);
	}

	public ItemTableManagePanel( final LockableListModel elementModel, final boolean addFilterField,
				     final boolean addRefreshButton )
	{
		this( elementModel,
		      addFilterField, addRefreshButton,
		      new boolean[] {false, false} );
	}

	public ShowDescriptionTable getElementList()
	{
		return (ShowDescriptionTable)this.scrollComponent;
	}

	public Object getSelectedValue()
	{
		return this.getElementList().getSelectedValue();
	}

	@Override
	public Object[] getSelectedValues()
	{
		return this.getElementList().getSelectedValues();
	}
}
