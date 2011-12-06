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
		super.setComparator( new DefaultComponentFirstComparator( c ) );
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

			int compare1 = this.parent.compare( o1, defaultComponent );
			int compare2 = this.parent.compare( o2, defaultComponent );

			// If o1 would normally occur before the default and
			// if o2 would normally occur after, the cycle means that
			// it now occurs after.

			if ( compare1 < 0 && compare2 > 0 )
			{
				return 1;
			}

			// If o1 would normally occur after the default and
			// if o2 would normally occur before, the cycle means that
			// it now occurs before.

			if ( compare2 < 0 && compare1 > 0 )
			{
				return 1;
			}

			// If either o1 or o2 is the default component, the other
			// switches places relative to it.

			if ( compare1 == 0 )
			{
				return 0 - compare2;
			}

			if ( compare2 == 0 )
			{
				return 0 - compare1;
			}

			// Otherwise, they both occur in the same direction relative
			// to the default component, just compare them.

			return this.parent.compare( o1, o2 );
		}
	}
}