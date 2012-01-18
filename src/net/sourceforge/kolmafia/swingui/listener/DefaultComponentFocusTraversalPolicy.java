package net.sourceforge.kolmafia.swingui.listener;

import java.awt.Component;
import java.awt.Container;

import java.lang.ref.WeakReference;
import java.util.Comparator;

import javax.swing.JPanel;
import javax.swing.LayoutFocusTraversalPolicy;

public class DefaultComponentFocusTraversalPolicy
	extends LayoutFocusTraversalPolicy
{
	private WeakReference component;

	public DefaultComponentFocusTraversalPolicy( Component component )
	{
		this.component = new WeakReference( component );

		this.setComparator( getComparator() );
	}

	public void setComparator( Comparator c )
	{
		if ( c != null )
		{
			super.setComparator( new DefaultComponentFirstComparator( c ) );
		}
	}

	public Component getDefaultComponent( Container container )
	{
		Component component = (Component) this.component.get();

		if ( component != null )
		{
			return component;
		}

		return super.getDefaultComponent( container );
	}

	private class DefaultComponentFirstComparator
		implements Comparator
	{
		private Comparator parent;

		public DefaultComponentFirstComparator( Comparator parent )
		{
			this.parent = parent;
		}

		public int compare( Object o1, Object o2 )
		{
			Component defaultComponent = (Component) DefaultComponentFocusTraversalPolicy.this.component.get();

			if ( defaultComponent == null )
			{
				return this.parent.compare( o1, o2 );
			}

			int compare1 = this.parent.compare( o1, defaultComponent );
			int compare2 = this.parent.compare( o2, defaultComponent );

			// If either o1 or o2 is the default component, that
			// comes first

			if ( compare1 == 0 )
			{
				return -1;
			}

			if ( compare2 == 0 )
			{
				return 1;
			}

			// Otherwise, they both occur in the same direction relative
			// to the default component, just compare them.

			return this.parent.compare( o1, o2 );
		}
	}
}
