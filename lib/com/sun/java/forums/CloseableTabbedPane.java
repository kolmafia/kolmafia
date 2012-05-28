
package com.sun.java.forums;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * <p>
 * A <code>JTabbedPane</code> which has a close ('X') icon on each tab.
 * </p>
 * <p>
 * To add a tab, use the method addTab(String, Component)
 * </p>
 * <p>
 * To have an extra icon on each tab (e.g. like in JBuilder, showing the file type) use the method addTab(String,
 * Component, Icon). Only clicking the 'X' closes the tab.
 * </p>
 * <p>
 * <b>Source</b>: <br>
 * <a href=http://forums.java.sun.com/thread.jspa?threadID=337070&start=15> Java Forums - JTabbedPane with close icons,
 * Post #15 </a>
 * </p>
 */

public class CloseableTabbedPane
	extends JTabbedPane
	implements MouseListener, MouseMotionListener
{

	/**
	 * The <code>EventListenerList</code>.
	 */
	private EventListenerList listenerList = null;

	/**
	 * The viewport of the scrolled tabs.
	 */
	private JViewport headerViewport = null;

	/**
	 * The normal closeicon.
	 */
	private Icon normalCloseIcon = null;

	/**
	 * The closeicon when the mouse is over.
	 */
	private Icon hooverCloseIcon = null;

	/**
	 * The closeicon when the mouse is pressed.
	 */
	private Icon pressedCloseIcon = null;

	/**
	 * Creates a new instance of <code>CloseableTabbedPane</code>
	 */
	public CloseableTabbedPane()
	{
		super();
		this.init( SwingConstants.LEFT );
	}

	/**
	 * Creates a new instance of <code>CloseableTabbedPane</code>
	 *
	 * @param horizontalTextPosition the horizontal position of the text (e.g. SwingUtilities.TRAILING or
	 *            SwingUtilities.LEFT)
	 */
	public CloseableTabbedPane( final int horizontalTextPosition )
	{
		super();
		this.init( horizontalTextPosition );
	}

	@Override
	public void setSelectedIndex( final int index )
	{
		int lastIndex = this.getSelectedIndex();

		super.setSelectedIndex( index );

		if ( lastIndex != -1 && lastIndex < this.getTabCount() )
		{
			this.setBackgroundAt( lastIndex, null );
			this.setForegroundAt( lastIndex, null );
		}

		this.setBackgroundAt( index, new Color( 205, 205, 255 ) );
		this.setForegroundAt( index, Color.black );
	}

	/**
	 * Initializes the <code>CloseableTabbedPane</code>
	 *
	 * @param horizontalTextPosition the horizontal position of the text (e.g. SwingUtilities.TRAILING or
	 *            SwingUtilities.LEFT)
	 */
	private void init( final int horizontalTextPosition )
	{
		this.listenerList = new EventListenerList();
		this.addMouseListener( this );
		this.addMouseMotionListener( this );
		this.setUI( new CloseableTabbedPaneUI( horizontalTextPosition ) );
	}

	/**
	 * Allows setting own closeicons.
	 *
	 * @param normal the normal closeicon
	 * @param hoover the closeicon when the mouse is over
	 * @param pressed the closeicon when the mouse is pressed
	 */
	public void setCloseIcons( final Icon normal, final Icon hoover, final Icon pressed )
	{
		this.normalCloseIcon = normal;
		this.hooverCloseIcon = hoover;
		this.pressedCloseIcon = pressed;
	}

	/**
	 * Adds a <code>Component</code> represented by a title and no icon.
	 *
	 * @param title the title to be displayed in this tab
	 * @param component the component to be displayed when this tab is clicked
	 */
	@Override
	public void addTab( final String title, final Component component )
	{
		this.addTab( title, component, null );
	}

	/**
	 * Adds a <code>Component</code> represented by a title and an icon.
	 *
	 * @param title the title to be displayed in this tab
	 * @param component the component to be displayed when this tab is clicked
	 * @param extraIcon the icon to be displayed in this tab
	 */
	public void addTab( final String title, final Component component, final Icon extraIcon )
	{
		this.insertTab( title, extraIcon, component, title, this.getTabCount() );
	}

	@Override
	public void insertTab( String title, final Icon extraIcon, final Component component, final String tooltip,
		final int index )
	{
		boolean doPaintCloseIcon = true;
		try
		{

			title = "    " + title + "        ";

			Object prop = null;
			if ( ( prop = ( (JComponent) component ).getClientProperty( "isClosable" ) ) != null )
			{
				doPaintCloseIcon = ( (Boolean) prop ).booleanValue();
			}
		}
		catch ( Exception ignored )
		{/* Could probably be a ClassCastException */
		}

		component.addPropertyChangeListener( "isClosable", new PropertyChangeListener()
		{
			public void propertyChange( final PropertyChangeEvent e )
			{
				Object newVal = e.getNewValue();
				int index = -1;
				if ( e.getSource() instanceof Component )
				{
					index = CloseableTabbedPane.this.indexOfComponent( (Component) e.getSource() );
				}
				if ( index != -1 && newVal != null && newVal instanceof Boolean )
				{
					CloseableTabbedPane.this.setCloseIconVisibleAt( index, ( (Boolean) newVal ).booleanValue() );
				}
			}
		} );

		super.insertTab( title, doPaintCloseIcon ? new CloseTabIcon( extraIcon ) : null, component, tooltip, index );

		if ( this.headerViewport == null )
		{
			Component[] components = this.getComponents();
			for ( int i = 0; i < components.length; ++i )
			{
				if ( "TabbedPane.scrollableViewport".equals( components[ i ].getName() ) )
				{
					this.headerViewport = (JViewport) components[ i ];
				}
			}
		}
	}

	/**
	 * Sets the closeicon at <code>index</code>.
	 *
	 * @param index the tab index where the icon should be set
	 * @param icon the icon to be displayed in the tab
	 * @throws IndexOutOfBoundsException if index is out of range (index < 0 || index >= tab count)
	 */
	private void setCloseIconVisibleAt( final int index, final boolean iconVisible )
		throws IndexOutOfBoundsException
	{
		super.setIconAt( index, iconVisible ? new CloseTabIcon( null ) : null );
	}

	/**
	 * Invoked when the mouse button has been clicked (pressed and released) on a component.
	 *
	 * @param e the <code>MouseEvent</code>
	 */
	public void mouseClicked( final MouseEvent e )
	{
		this.processMouseEvents( e );
	}

	/**
	 * Invoked when the mouse enters a component.
	 *
	 * @param e the <code>MouseEvent</code>
	 */
	public void mouseEntered( final MouseEvent e )
	{
	}

	/**
	 * Invoked when the mouse exits a component.
	 *
	 * @param e the <code>MouseEvent</code>
	 */
	public void mouseExited( final MouseEvent e )
	{
		for ( int i = 0; i < this.getTabCount(); i++ )
		{
			CloseTabIcon icon = (CloseTabIcon) this.getIconAt( i );
			if ( icon != null )
			{
				icon.mouseover = false;
			}
		}
		this.repaint();
	}

	/**
	 * Invoked when a mouse button has been pressed on a component.
	 *
	 * @param e the <code>MouseEvent</code>
	 */
	public void mousePressed( final MouseEvent e )
	{
		this.processMouseEvents( e );
	}

	/**
	 * Invoked when a mouse button has been released on a component.
	 *
	 * @param e the <code>MouseEvent</code>
	 */
	public void mouseReleased( final MouseEvent e )
	{
	}

	/**
	 * Invoked when a mouse button is pressed on a component and then dragged. <code>MOUSE_DRAGGED</code> events will
	 * continue to be delivered to the component where the drag originated until the mouse button is released
	 * (regardless of whether the mouse position is within the bounds of the component).<br/> <br/> Due to
	 * platform-dependent Drag&Drop implementations, <code>MOUSE_DRAGGED</code> events may not be delivered during a
	 * native Drag&Drop operation.
	 *
	 * @param e the <code>MouseEvent</code>
	 */
	public void mouseDragged( final MouseEvent e )
	{
		this.processMouseEvents( e );
	}

	/**
	 * Invoked when the mouse cursor has been moved onto a component but no buttons have been pushed.
	 *
	 * @param e the <code>MouseEvent</code>
	 */
	public void mouseMoved( final MouseEvent e )
	{
		this.processMouseEvents( e );
	}

	/**
	 * Processes all caught <code>MouseEvent</code>s.
	 *
	 * @param e the <code>MouseEvent</code>
	 */
	private void processMouseEvents( final MouseEvent e )
	{
		int tabNumber = this.getUI().tabForCoordinate( this, e.getX(), e.getY() );
		if ( tabNumber < 0 )
		{
			return;
		}
		boolean otherWasOver = false;
		for ( int i = 0; i < this.getTabCount(); i++ )
		{
			if ( i != tabNumber )
			{
				CloseTabIcon ic = (CloseTabIcon) this.getIconAt( i );
				if ( ic != null )
				{
					if ( ic.mouseover )
					{
						otherWasOver = true;
					}
					ic.mouseover = false;
				}
			}
		}
		if ( otherWasOver )
		{
			this.repaint();
		}
		CloseTabIcon icon = (CloseTabIcon) this.getIconAt( tabNumber );
		if ( icon != null )
		{
			Rectangle rect = icon.getBounds();
			boolean vpIsNull = this.headerViewport == null;
			Point pos = vpIsNull ? new Point() : this.headerViewport.getViewPosition();
			int vpDiffX = vpIsNull ? 0 : this.headerViewport.getX();
			int vpDiffY = vpIsNull ? 0 : this.headerViewport.getY();
			Rectangle drawRect =
				new Rectangle( rect.x - pos.x + vpDiffX, rect.y - pos.y + vpDiffY, rect.width, rect.height );

			if ( e.getID() == MouseEvent.MOUSE_PRESSED )
			{
				icon.mousepressed = e.getModifiers() == InputEvent.BUTTON1_MASK;
				this.repaint( drawRect );
			}
			else if ( e.getID() == MouseEvent.MOUSE_MOVED || e.getID() == MouseEvent.MOUSE_DRAGGED || e.getID() == MouseEvent.MOUSE_CLICKED )
			{
				pos.x += e.getX() - vpDiffX;
				pos.y += e.getY() - vpDiffY;
				if ( rect.contains( pos ) )
				{
					if ( e.getID() == MouseEvent.MOUSE_CLICKED )
					{
						int selIndex = this.getSelectedIndex();
						if ( this.fireCloseTab( selIndex ) )
						{
							if ( selIndex > 0 )
							{
								// to prevent uncatchable null-pointers
								Rectangle rec = this.getUI().getTabBounds( this, selIndex - 1 );

								MouseEvent event =
									new MouseEvent(
										(Component) e.getSource(), e.getID() + 1, System.currentTimeMillis(),
										e.getModifiers(), rec.x, rec.y, e.getClickCount(), e.isPopupTrigger(),
										e.getButton() );
								this.dispatchEvent( event );
							}
							//the tab is being closed
							//removeTabAt(tabNumber);
							this.remove( selIndex );
						}
						else
						{
							icon.mouseover = false;
							icon.mousepressed = false;
							this.repaint( drawRect );
						}
					}
					else
					{
						icon.mouseover = true;
						icon.mousepressed = e.getModifiers() == InputEvent.BUTTON1_MASK;
					}
				}
				else
				{
					icon.mouseover = false;
				}
				this.repaint( drawRect );
			}
		}
	}

	/**
	 * Adds an <code>CloseableTabbedPaneListener</code> to the tabbedpane.
	 *
	 * @param l the <code>CloseableTabbedPaneListener</code> to be added
	 */
	public void addCloseableTabbedPaneListener( final CloseableTabbedPaneListener l )
	{
		this.listenerList.add( CloseableTabbedPaneListener.class, l );
	}

	/**
	 * Removes an <code>CloseableTabbedPaneListener</code> from the tabbedpane.
	 *
	 * @param l the listener to be removed
	 */
	public void removeCloseableTabbedPaneListener( final CloseableTabbedPaneListener l )
	{
		this.listenerList.remove( CloseableTabbedPaneListener.class, l );
	}

	/**
	 * Returns an array of all the <code>SearchListener</code>s added to this <code>SearchPane</code> with
	 * addSearchListener().
	 *
	 * @return all of the <code>SearchListener</code>s added or an empty array if no listeners have been added
	 */
	public CloseableTabbedPaneListener[] getCloseableTabbedPaneListener()
	{
		return (CloseableTabbedPaneListener[]) this.listenerList.getListeners( CloseableTabbedPaneListener.class );
	}

	/**
	 * Notifies all listeners that have registered interest for notification on this event type.
	 *
	 * @param tabIndexToClose the index of the tab which should be closed
	 * @return true if the tab can be closed, false otherwise
	 */
	protected boolean fireCloseTab( final int tabIndexToClose )
	{
		boolean closeit = true;
		// Guaranteed to return a non-null array
		Object[] listeners = this.listenerList.getListenerList();
		for ( int i = 0; i < listeners.length; ++i )
		{
			if ( listeners[ i ] instanceof CloseableTabbedPaneListener )
			{
				if ( !( (CloseableTabbedPaneListener) listeners[ i ] ).closeTab( tabIndexToClose ) )
				{
					closeit = false;
					break;
				}
			}
		}
		return closeit;
	}

	/**
	 * The class which generates the 'X' icon for the tabs. The constructor accepts an icon which is extra to the 'X'
	 * icon, so you can have tabs like in JBuilder. This value is null if no extra icon is required.
	 */
	class CloseTabIcon
		implements Icon
	{
		/**
		 * the x position of the icon
		 */
		private int x_pos;

		/**
		 * the y position of the icon
		 */
		private int y_pos;

		/**
		 * the width the icon
		 */
		private final int width;

		/**
		 * the height the icon
		 */
		private final int height;

		/**
		 * the additional fileicon
		 */
		private final Icon fileIcon;

		/**
		 * true whether the mouse is over this icon, false otherwise
		 */
		private boolean mouseover = false;

		/**
		 * true whether the mouse is pressed on this icon, false otherwise
		 */
		private boolean mousepressed = false;

		/**
		 * Creates a new instance of <code>CloseTabIcon</code>
		 *
		 * @param fileIcon the additional fileicon, if there is one set
		 */
		public CloseTabIcon( final Icon fileIcon )
		{
			this.fileIcon = fileIcon;
			this.width = 16;
			this.height = 16;
		}

		/**
		 * Draw the icon at the specified location. Icon implementations may use the Component argument to get
		 * properties useful for painting, e.g. the foreground or background color.
		 *
		 * @param c the component which the icon belongs to
		 * @param g the graphic object to draw on
		 * @param x the upper left point of the icon in the x direction
		 * @param y the upper left point of the icon in the y direction
		 */
		public void paintIcon( final Component c, final Graphics g, final int x, final int y )
		{
			boolean doPaintCloseIcon = true;
			try
			{
				// JComponent.putClientProperty("isClosable", Boolean.FALSE);
				JTabbedPane tabbedpane = (JTabbedPane) c;
				int tabNumber = tabbedpane.getUI().tabForCoordinate( tabbedpane, x, y );
				JComponent curPanel = (JComponent) tabbedpane.getComponentAt( tabNumber );
				Object prop = null;
				if ( ( prop = curPanel.getClientProperty( "isClosable" ) ) != null )
				{
					doPaintCloseIcon = ( (Boolean) prop ).booleanValue();
				}
			}
			catch ( Exception ignored )
			{/* Could probably be a ClassCastException */
			}
			if ( doPaintCloseIcon )
			{
				this.x_pos = x;
				this.y_pos = y;
				int y_p = y + 1;

				if ( CloseableTabbedPane.this.normalCloseIcon != null && !this.mouseover )
				{
					CloseableTabbedPane.this.normalCloseIcon.paintIcon( c, g, x, y_p );
				}
				else if ( CloseableTabbedPane.this.hooverCloseIcon != null && this.mouseover && !this.mousepressed )
				{
					CloseableTabbedPane.this.hooverCloseIcon.paintIcon( c, g, x, y_p );
				}
				else if ( CloseableTabbedPane.this.pressedCloseIcon != null && this.mousepressed )
				{
					CloseableTabbedPane.this.pressedCloseIcon.paintIcon( c, g, x, y_p );
				}
				else
				{
					y_p++ ;

					Color col = g.getColor();

					if ( this.mousepressed && this.mouseover )
					{
						g.setColor( Color.WHITE );
						g.fillRect( x + 1, y_p, 12, 13 );
					}

					g.setColor( Color.black );
					/*
					 * g.drawLine(x+1, y_p, x+12, y_p); g.drawLine(x+1, y_p+13, x+12, y_p+13); g.drawLine(x, y_p+1, x,
					 * y_p+12); g.drawLine(x+13, y_p+1, x+13, y_p+12);
					 */
					if ( this.mouseover )
					{
						g.setColor( Color.GRAY );
					}
					g.drawLine( x + 4, y_p + 4, x + 9, y_p + 9 );
					g.drawLine( x + 4, y_p + 5, x + 8, y_p + 9 );
					g.drawLine( x + 5, y_p + 4, x + 9, y_p + 8 );
					g.drawLine( x + 9, y_p + 4, x + 4, y_p + 9 );
					g.drawLine( x + 9, y_p + 5, x + 5, y_p + 9 );
					g.drawLine( x + 8, y_p + 4, x + 4, y_p + 8 );

					g.setColor( col );
					if ( this.fileIcon != null )
					{
						this.fileIcon.paintIcon( c, g, x + this.width, y_p );
					}
				}
			}
		}

		/**
		 * Returns the icon's width.
		 *
		 * @return an int specifying the fixed width of the icon.
		 */
		public int getIconWidth()
		{
			return this.width + ( this.fileIcon != null ? this.fileIcon.getIconWidth() : 0 );
		}

		/**
		 * Returns the icon's height.
		 *
		 * @return an int specifying the fixed height of the icon.
		 */
		public int getIconHeight()
		{
			return this.height;
		}

		/**
		 * Gets the bounds of this icon in the form of a <code>Rectangle<code>
		 * object. The bounds specify this icon's width, height, and location
		 * relative to its parent.
		 * @return a rectangle indicating this icon's bounds
		 */
		public Rectangle getBounds()
		{
			return new Rectangle( this.x_pos, this.y_pos, this.width, this.height );
		}
	}

	/**
	 * A specific <code>BasicTabbedPaneUI</code>.
	 */
	class CloseableTabbedPaneUI
		extends BasicTabbedPaneUI
	{

		/**
		 * the horizontal position of the text
		 */
		private int horizontalTextPosition = SwingConstants.LEFT;

		/**
		 * Creates a new instance of <code>CloseableTabbedPaneUI</code>
		 */
		public CloseableTabbedPaneUI()
		{
		}

		/**
		 * Creates a new instance of <code>CloseableTabbedPaneUI</code>
		 *
		 * @param horizontalTextPosition the horizontal position of the text (e.g. SwingUtilities.TRAILING or
		 *            SwingUtilities.LEFT)
		 */
		public CloseableTabbedPaneUI( final int horizontalTextPosition )
		{
			this.horizontalTextPosition = horizontalTextPosition;
		}

		/**
		 * Layouts the label
		 *
		 * @param tabPlacement the placement of the tabs
		 * @param metrics the font metrics
		 * @param tabIndex the index of the tab
		 * @param title the title of the tab
		 * @param icon the icon of the tab
		 * @param tabRect the tab boundaries
		 * @param iconRect the icon boundaries
		 * @param textRect the text boundaries
		 * @param isSelected true whether the tab is selected, false otherwise
		 */
		@Override
		protected void layoutLabel( final int tabPlacement, final FontMetrics metrics, final int tabIndex,
			final String title, final Icon icon, final Rectangle tabRect, final Rectangle iconRect,
			final Rectangle textRect, final boolean isSelected )
		{

			textRect.x = textRect.y = iconRect.x = iconRect.y = 0;

			javax.swing.text.View v = this.getTextViewForTab( tabIndex );
			if ( v != null )
			{
				this.tabPane.putClientProperty( "html", v );
			}

			SwingUtilities.layoutCompoundLabel(
				this.tabPane, metrics, title, icon, SwingConstants.CENTER, SwingConstants.CENTER,
				SwingConstants.CENTER, this.horizontalTextPosition, tabRect, iconRect, textRect, this.textIconGap + 10 );

			this.tabPane.putClientProperty( "html", null );

			int xNudge = this.getTabLabelShiftX( tabPlacement, tabIndex, isSelected );
			int yNudge = this.getTabLabelShiftY( tabPlacement, tabIndex, isSelected );
			iconRect.x += xNudge;
			iconRect.y += yNudge;
			textRect.x += xNudge;
			textRect.y += yNudge;
		}
	}

	public void highlightTab( final int tabIndex )
	{
		this.setBackgroundAt( tabIndex, new Color( 255, 205, 205 ) );
		this.setForegroundAt( tabIndex, Color.black );
	}

	@Override
	public boolean isFocusable()
	{
		return false;
	}
}
