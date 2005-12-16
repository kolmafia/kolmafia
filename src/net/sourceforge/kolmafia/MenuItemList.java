package net.sourceforge.kolmafia;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JSeparator;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.java.dev.spellcast.utilities.LockableListModel;


/**
 * A special class which renders the menu holding the list of menu items
 * synchronized to a lockable list model.
 */

public abstract class MenuItemList extends JMenu implements ListDataListener
{
	private int headerCount;

	public MenuItemList( String title, LockableListModel model )
	{
		super( title );

		// Add the headers to the list of items which
		// need to be added.

		JComponent [] headers = getHeaders();

		for ( int i = 0; i < headers.length; ++i )
			this.add( headers[i] );

		// Add a separator between the headers and the
		// elements displayed in the list.  Also go
		// ahead and initialize the header count.

		this.add( new JSeparator() );
		this.headerCount = headers.length + 1;

		// Now, add everything that's contained inside of
		// the current list.

		for ( int i = 0; i < model.size(); ++i )
			this.add( (JComponent) model.get(i) );

		// Add this as a listener to the list so that the menu gets
		// updated whenever the list updates.

		model.addListDataListener( this );
	}

	public abstract JComponent [] getHeaders();

	/**
	 * Called whenever contents have been added to the original list; a
	 * function required by every <code>ListDataListener</code>.
	 *
	 * @param	e	the <code>ListDataEvent</code> that triggered this function call
	 */

	public synchronized void intervalAdded( ListDataEvent e )
	{
		LockableListModel source = (LockableListModel) e.getSource();
		int index0 = e.getIndex0();  int index1 = e.getIndex1();

		if ( index1 >= source.size() || source.size() + headerCount == getMenuComponentCount() )
			return;

		for ( int i = index0; i <= index1; ++i )
			add( (JComponent) source.get(i), i + headerCount );

		validate();
	}

	/**
	 * Called whenever contents have been removed from the original list;
	 * a function required by every <code>ListDataListener</code>.
	 *
	 * @param	e	the <code>ListDataEvent</code> that triggered this function call
	 */

	public synchronized void intervalRemoved( ListDataEvent e )
	{
		LockableListModel source = (LockableListModel) e.getSource();
		int index0 = e.getIndex0();  int index1 = e.getIndex1();

		if ( index1 + headerCount >= getMenuComponentCount() || source.size() + headerCount == getMenuComponentCount() )
			return;

		for ( int i = index1; i >= index0; --i )
			remove( i + headerCount );

		validate();
	}

	/**
	 * Called whenever contents in the original list have changed; a
	 * function required by every <code>ListDataListener</code>.
	 *
	 * @param	e	the <code>ListDataEvent</code> that triggered this function call
	 */

	public synchronized void contentsChanged( ListDataEvent e )
	{
		LockableListModel source = (LockableListModel) e.getSource();
		int index0 = e.getIndex0();  int index1 = e.getIndex1();

		if ( index1 + headerCount >= getMenuComponentCount() || source.size() + headerCount == getMenuComponentCount() )
			return;

		for ( int i = index1; i >= index0; --i )
		{
			remove( i + headerCount );
			add( (JComponent) source.get(i), i + headerCount );
		}

		validate();
	}
}
