/*
 * David Bismut, davidou@mageos.com
 * Intern, SETLabs, Infosys Technologies Ltd. May 2004 - Jul 2004
 * Ecole des Mines de Nantes, France
 */

/*
 *
 * Extended from
 * @(#)BasicTabbedPaneUI.java	1.126 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package tab;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.InputMapUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.View;

import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * UI for <code>CloseAndMaxTabbedPane</code>.
 * <p>
 * Credits to:
 *
 * @author Amy Fowler
 * @author Philip Milne
 * @author Steve Wilson
 * @author Tom Santos
 * @author Dave Moore
 */
public class CloseTabPaneUI
	extends BasicTabbedPaneUI
{

	// Instance variables initialized at installation

	private ContainerListener containerListener;

	private ArrayList htmlViews;
	protected ArrayList tabStates = new ArrayList();

	private Hashtable mnemonicToIndexMap;

	/**
	 * InputMap used for mnemonics. Only non-null if the JTabbedPane has mnemonics associated with it. Lazily created in
	 * initMnemonics.
	 */
	private InputMap mnemonicInputMap;

	// For use when tabLayoutPolicy = SCROLL_TAB_LAYOUT
	protected ScrollableTabSupport tabScroller;

	private int tabCount;

	protected MyMouseMotionListener motionListener;

	// UI creation

	private final int INACTIVE = 0;
	private final int OVER = 1;
	private final int PRESSED = 2;

	protected static final int BUTTONSIZE = 16;
	protected static final int WIDTHDELTA = 8;

	private static BufferedImage closeRedImgB;
	private static BufferedImage closeRedImgI;
	private static JButton closeRedB;

	private static BufferedImage closeGrayImgB;
	private static BufferedImage closeGrayImgI;
	private static JButton closeGrayB;

	static
	{
		try
		{
			CloseTabPaneUI.closeRedImgI = ImageIO.read( JComponentUtilities.getResource( "xred.gif" ) );
		}
		catch ( IOException e1 )
		{
			e1.printStackTrace();
		}

		CloseTabPaneUI.closeRedImgB =
			new BufferedImage( CloseTabPaneUI.BUTTONSIZE, CloseTabPaneUI.BUTTONSIZE, BufferedImage.TYPE_INT_ARGB );

		try
		{
			CloseTabPaneUI.closeGrayImgI = ImageIO.read( JComponentUtilities.getResource( "xgray.gif" ) );
		}
		catch ( IOException e1 )
		{
			e1.printStackTrace();
		}

		CloseTabPaneUI.closeGrayImgB =
			new BufferedImage( CloseTabPaneUI.BUTTONSIZE, CloseTabPaneUI.BUTTONSIZE, BufferedImage.TYPE_INT_ARGB );
	}

	private int overTabIndex = -1;

	private int closeIndexStatus = this.INACTIVE;
	private int maxIndexStatus = this.INACTIVE;

	private boolean mousePressed = false;

	protected JPopupMenu actionPopupMenu;
	protected JMenuItem closeItem;

	public CloseTabPaneUI()
	{

		super();

		// Paint the red close icon

		CloseTabPaneUI.closeRedB = new JButton();
		CloseTabPaneUI.closeRedB.setSize( CloseTabPaneUI.BUTTONSIZE, CloseTabPaneUI.BUTTONSIZE );

		CloseTabPaneUI.closeRedB.setMargin( new Insets( 0, 0, 0, 0 ) );
		CloseTabPaneUI.closeRedB.setBorder( BorderFactory.createEmptyBorder() );
		CloseTabPaneUI.closeRedB.setContentAreaFilled( false );

		// Paint the gray close icon

		CloseTabPaneUI.closeGrayB = new JButton();
		CloseTabPaneUI.closeGrayB.setSize( CloseTabPaneUI.BUTTONSIZE, CloseTabPaneUI.BUTTONSIZE );

		CloseTabPaneUI.closeGrayB.setMargin( new Insets( 0, 0, 0, 0 ) );
		CloseTabPaneUI.closeGrayB.setBorder( BorderFactory.createEmptyBorder() );
		CloseTabPaneUI.closeGrayB.setContentAreaFilled( false );

		// Create a popup menu

		this.actionPopupMenu = new JPopupMenu();
		this.closeItem = new JMenuItem( "Close" );

		this.closeItem.addActionListener( new ActionListener()
		{
			public void actionPerformed( final ActionEvent e )
			{
				( (CloseTabbedPane) CloseTabPaneUI.this.tabPane ).fireCloseTabEvent(
					null, CloseTabPaneUI.this.tabPane.getSelectedIndex() );

			}
		} );

		this.setPopupMenu();
	}

	public boolean highlightTab( final int tabIndex )
	{
		for ( int i = this.tabStates.size(); i <= tabIndex; ++i )
		{
			this.tabStates.add( Boolean.FALSE );
		}

		if ( this.tabStates.get( tabIndex ) == Boolean.TRUE )
		{
			return false;
		}

		this.tabStates.set( tabIndex, Boolean.TRUE );
		return true;
	}

	protected boolean isOneActionButtonEnabled()
	{
		return this.closeIconStyle != CloseTabPaneUI.NO_CLOSE_ICON;
	}

	public boolean isCloseEnabled()
	{
		return this.closeIconStyle != CloseTabPaneUI.NO_CLOSE_ICON;
	}

	public static final int NO_CLOSE_ICON = 0;
	public static final int RED_CLOSE_ICON = 1;
	public static final int GRAY_CLOSE_ICON = 2;

	private int closeIconStyle = CloseTabPaneUI.NO_CLOSE_ICON;

	public void setCloseIconStyle( final int style )
	{
		this.closeIconStyle = style;
		this.setPopupMenu();
	}

	public int getCloseIconStyle()
	{
		return this.closeIconStyle;
	}

	private void setPopupMenu()
	{
		this.actionPopupMenu.removeAll();
		if ( this.closeIconStyle != CloseTabPaneUI.NO_CLOSE_ICON )
		{
			this.actionPopupMenu.add( this.closeItem );
		}
	}

	@Override
	protected int calculateTabWidth( final int tabPlacement, final int tabIndex, final FontMetrics metrics )
	{
		int delta = 2;
		if ( !this.isOneActionButtonEnabled() )
		{
			delta += 6;
		}
		else if ( this.closeIconStyle != CloseTabPaneUI.NO_CLOSE_ICON )
		{
			delta += CloseTabPaneUI.BUTTONSIZE + CloseTabPaneUI.WIDTHDELTA;
		}

		return super.calculateTabWidth( tabPlacement, tabIndex, metrics ) + delta + ( this.closeIconStyle != CloseTabPaneUI.NO_CLOSE_ICON ? 20 : 5 );
	}

	@Override
	protected int calculateTabHeight( final int tabPlacement, final int tabIndex, final int fontHeight )
	{

		return super.calculateTabHeight( tabPlacement, tabIndex, fontHeight ) + 2;
	}

	@Override
	protected void layoutLabel( final int tabPlacement, final FontMetrics metrics, final int tabIndex,
		final String title, final Icon icon, final Rectangle tabRect, final Rectangle iconRect,
		final Rectangle textRect, final boolean isSelected )
	{
		textRect.x = textRect.y = iconRect.x = iconRect.y = 0;

		View v = this.getTextViewForTab( tabIndex );
		if ( v != null )
		{
			this.tabPane.putClientProperty( "html", v );
		}

		SwingUtilities.layoutCompoundLabel(
			this.tabPane, metrics, title, icon, SwingConstants.CENTER, SwingConstants.LEFT, SwingConstants.CENTER,
			SwingConstants.CENTER, tabRect, iconRect, textRect, this.textIconGap );

		this.tabPane.putClientProperty( "html", null );

		iconRect.x = tabRect.x + 8;
		textRect.x = iconRect.x + iconRect.width + this.textIconGap;
	}

	@Override
	protected MouseListener createMouseListener()
	{
		return new MyMouseHandler();
	}

	protected ScrollableTabButton createScrollableTabButton( final int direction )
	{
		return new ScrollableTabButton( direction );
	}

	protected Rectangle newCloseRect( final Rectangle rect )
	{
		int dx = rect.x + rect.width;
		int dy = ( rect.y + rect.height ) / 2 - 6;
		return new Rectangle(
			dx - CloseTabPaneUI.BUTTONSIZE - CloseTabPaneUI.WIDTHDELTA, dy, CloseTabPaneUI.BUTTONSIZE,
			CloseTabPaneUI.BUTTONSIZE );
	}

	protected void updateOverTab( final int x, final int y )
	{
		if ( this.overTabIndex != ( this.overTabIndex = this.getTabAtLocation( x, y ) ) )
		{
			this.tabScroller.tabPanel.repaint();
		}

	}

	protected void updateCloseIcon( final int x, final int y )
	{

		if ( this.overTabIndex != -1 )
		{
			int newCloseIndexStatus = this.INACTIVE;

			Rectangle closeRect = this.newCloseRect( this.rects[ this.overTabIndex ] );
			if ( closeRect.contains( x, y ) )
			{
				newCloseIndexStatus = this.mousePressed ? this.PRESSED : this.OVER;
			}

			if ( this.closeIndexStatus != ( this.closeIndexStatus = newCloseIndexStatus ) )
			{
				this.tabScroller.tabPanel.repaint();
			}
		}
	}

	private void setTabIcons( final int x, final int y )
	{
		//if the mouse isPressed
		if ( !this.mousePressed )
		{
			this.updateOverTab( x, y );
		}

		if ( this.closeIconStyle != CloseTabPaneUI.NO_CLOSE_ICON )
		{
			this.updateCloseIcon( x, y );
		}
	}

	public static ComponentUI createUI( final JComponent c )
	{
		return new CloseTabPaneUI();
	}

	/**
	 * Invoked by <code>installUI</code> to create a layout manager object to manage the <code>JTabbedPane</code>.
	 *
	 * @return a layout manager object
	 * @see TabbedPaneLayout
	 * @see javax.swing.JTabbedPane#getTabLayoutPolicy
	 */
	@Override
	protected LayoutManager createLayoutManager()
	{

		return new TabbedPaneScrollLayout();

	}

	/*
	 * In an attempt to preserve backward compatibility for programs which have extended BasicTabbedPaneUI to do their
	 * own layout, the UI uses the installed layoutManager (and not tabLayoutPolicy) to determine if scrollTabLayout is
	 * enabled.
	 */

	/**
	 * Creates and installs any required subcomponents for the JTabbedPane. Invoked by installUI.
	 *
	 * @since 1.4
	 */
	@Override
	protected void installComponents()
	{

		if ( this.tabScroller == null )
		{
			this.tabScroller = new ScrollableTabSupport( this.tabPane.getTabPlacement() );
			this.tabPane.add( this.tabScroller.viewport );
			this.tabPane.add( this.tabScroller.scrollForwardButton );
			this.tabPane.add( this.tabScroller.scrollBackwardButton );
		}

	}

	/**
	 * Removes any installed subcomponents from the JTabbedPane. Invoked by uninstallUI.
	 *
	 * @since 1.4
	 */
	@Override
	protected void uninstallComponents()
	{

		this.tabPane.remove( this.tabScroller.viewport );
		this.tabPane.remove( this.tabScroller.scrollForwardButton );
		this.tabPane.remove( this.tabScroller.scrollBackwardButton );

		if ( this.htmlViews != null )
		{
			this.htmlViews.clear();
		}

		if ( this.tabStates != null )
		{
			this.tabStates.clear();
		}

		this.resetMnemonics();

		this.htmlViews = null;
		this.tabStates = null;
		this.tabScroller = null;
	}

	@Override
	protected void installListeners()
	{
		if ( ( this.propertyChangeListener = this.createPropertyChangeListener() ) != null )
		{
			this.tabPane.addPropertyChangeListener( this.propertyChangeListener );
		}
		if ( ( this.tabChangeListener = this.createChangeListener() ) != null )
		{
			this.tabPane.addChangeListener( this.tabChangeListener );
		}
		if ( ( this.mouseListener = this.createMouseListener() ) != null )
		{
			this.tabScroller.tabPanel.addMouseListener( this.mouseListener );
		}

		if ( ( this.focusListener = this.createFocusListener() ) != null )
		{
			this.tabPane.addFocusListener( this.focusListener );
		}

		// PENDING(api) : See comment for ContainerHandler
		if ( ( this.containerListener = new ContainerHandler() ) != null )
		{
			this.tabPane.addContainerListener( this.containerListener );
			if ( this.tabPane.getTabCount() > 0 )
			{
				this.htmlViews = this.createHTMLArrayList();
			}
		}

		if ( ( this.motionListener = new MyMouseMotionListener() ) != null )
		{
			this.tabScroller.tabPanel.addMouseMotionListener( this.motionListener );
		}

	}

	@Override
	protected void uninstallListeners()
	{
		if ( this.mouseListener != null )
		{
			this.tabScroller.tabPanel.removeMouseListener( this.mouseListener );
			this.mouseListener = null;
		}

		if ( this.motionListener != null )
		{
			this.tabScroller.tabPanel.removeMouseMotionListener( this.motionListener );
			this.motionListener = null;
		}

		if ( this.focusListener != null )
		{
			this.tabPane.removeFocusListener( this.focusListener );
			this.focusListener = null;
		}

		// PENDING(api): See comment for ContainerHandler
		if ( this.containerListener != null )
		{
			this.tabPane.removeContainerListener( this.containerListener );
			this.containerListener = null;
			if ( this.htmlViews != null )
			{
				this.htmlViews.clear();
				this.htmlViews = null;
			}
		}
		if ( this.tabChangeListener != null )
		{
			this.tabPane.removeChangeListener( this.tabChangeListener );
			this.tabChangeListener = null;
		}
		if ( this.propertyChangeListener != null )
		{
			this.tabPane.removePropertyChangeListener( this.propertyChangeListener );
			this.propertyChangeListener = null;
		}

	}

	@Override
	protected ChangeListener createChangeListener()
	{
		return new TabSelectionHandler();
	}

	@Override
	protected void installKeyboardActions()
	{
		InputMap km = this.getMyInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );

		SwingUtilities.replaceUIInputMap( this.tabPane, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, km );
		km = this.getMyInputMap( JComponent.WHEN_FOCUSED );
		SwingUtilities.replaceUIInputMap( this.tabPane, JComponent.WHEN_FOCUSED, km );

		ActionMap am = this.createMyActionMap();

		SwingUtilities.replaceUIActionMap( this.tabPane, am );

		this.tabScroller.scrollForwardButton.setAction( am.get( "scrollTabsForwardAction" ) );
		this.tabScroller.scrollBackwardButton.setAction( am.get( "scrollTabsBackwardAction" ) );

	}

	InputMap getMyInputMap( final int condition )
	{
		if ( condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT )
		{
			return (InputMap) UIManager.get( "TabbedPane.ancestorInputMap" );
		}
		else if ( condition == JComponent.WHEN_FOCUSED )
		{
			return (InputMap) UIManager.get( "TabbedPane.focusInputMap" );
		}
		return null;
	}

	ActionMap createMyActionMap()
	{
		ActionMap map = new ActionMapUIResource();
		map.put( "navigateNext", new NextAction() );
		map.put( "navigatePrevious", new PreviousAction() );
		map.put( "navigateRight", new RightAction() );
		map.put( "navigateLeft", new LeftAction() );
		map.put( "navigateUp", new UpAction() );
		map.put( "navigateDown", new DownAction() );
		map.put( "navigatePageUp", new PageUpAction() );
		map.put( "navigatePageDown", new PageDownAction() );
		map.put( "setSelectedIndex", new SetSelectedIndexAction() );
		map.put( "scrollTabsForwardAction", new ScrollTabsForwardAction() );
		map.put( "scrollTabsBackwardAction", new ScrollTabsBackwardAction() );
		return map;
	}

	@Override
	protected void uninstallKeyboardActions()
	{
		SwingUtilities.replaceUIActionMap( this.tabPane, null );
		SwingUtilities.replaceUIInputMap( this.tabPane, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null );
		SwingUtilities.replaceUIInputMap( this.tabPane, JComponent.WHEN_FOCUSED, null );
	}

	/**
	 * Reloads the mnemonics. This should be invoked when a memonic changes, when the title of a mnemonic changes, or
	 * when tabs are added/removed.
	 */
	private void updateMnemonics()
	{
		this.resetMnemonics();
		for ( int counter = this.tabPane.getTabCount() - 1; counter >= 0; counter-- )
		{
			int mnemonic = this.tabPane.getMnemonicAt( counter );

			if ( mnemonic > 0 )
			{
				this.addMnemonic( counter, mnemonic );
			}
		}
	}

	/**
	 * Resets the mnemonics bindings to an empty state.
	 */
	private void resetMnemonics()
	{
		if ( this.mnemonicToIndexMap != null )
		{
			this.mnemonicToIndexMap.clear();
			this.mnemonicInputMap.clear();
		}
	}

	/**
	 * Adds the specified mnemonic at the specified index.
	 */
	private void addMnemonic( final int index, final int mnemonic )
	{
		if ( this.mnemonicToIndexMap == null )
		{
			this.initMnemonics();
		}
		this.mnemonicInputMap.put( KeyStroke.getKeyStroke( mnemonic, Event.ALT_MASK ), "setSelectedIndex" );
		this.mnemonicToIndexMap.put( new Integer( mnemonic ), new Integer( index ) );
	}

	/**
	 * Installs the state needed for mnemonics.
	 */
	private void initMnemonics()
	{
		this.mnemonicToIndexMap = new Hashtable();
		this.mnemonicInputMap = new InputMapUIResource();
		this.mnemonicInputMap.setParent( SwingUtilities.getUIInputMap(
			this.tabPane, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ) );
		SwingUtilities.replaceUIInputMap(
			this.tabPane, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, this.mnemonicInputMap );
	}

	// UI Rendering

	@Override
	public void paint( final Graphics g, final JComponent c )
	{
		int tc = this.tabPane.getTabCount();

		if ( this.tabCount != tc )
		{
			this.tabCount = tc;
			this.updateMnemonics();
		}

		int selectedIndex = this.tabPane.getSelectedIndex();
		int tabPlacement = this.tabPane.getTabPlacement();

		this.ensureCurrentLayout();

		// Paint content border
		this.paintContentBorder( g, tabPlacement, selectedIndex );

	}

	@Override
	protected void paintTab( final Graphics g, final int tabPlacement, final Rectangle[] rects, final int tabIndex,
		final Rectangle iconRect, final Rectangle textRect )
	{

		Rectangle tabRect = rects[ tabIndex ];

		int selectedIndex = this.tabPane.getSelectedIndex();
		boolean isSelected = selectedIndex == tabIndex;
		boolean isOver = this.overTabIndex == tabIndex;

		if ( isSelected && tabIndex < this.tabStates.size() )
		{
			this.tabStates.set( tabIndex, Boolean.FALSE );
		}

		Graphics2D g2 = null;
		Shape save = null;
		boolean cropShape = false;
		int cropx = 0;
		int cropy = 0;

		if ( g instanceof Graphics2D )
		{
			g2 = (Graphics2D) g;

			// Render visual for cropped tab edge...
			Rectangle viewRect = this.tabScroller.viewport.getViewRect();
			int cropline;

			cropline = viewRect.x + viewRect.width;
			if ( tabRect.x < cropline && tabRect.x + tabRect.width > cropline )
			{

				cropx = cropline - 1;
				cropy = tabRect.y;
				cropShape = true;
			}

			if ( cropShape )
			{
				save = g2.getClip();
				g2.clipRect( tabRect.x, tabRect.y, tabRect.width, tabRect.height );

			}
		}

		this.paintTabBackground(
			g, tabPlacement, tabIndex, tabRect.x, tabRect.y, tabRect.width, tabRect.height, isSelected );

		this.paintTabBorder( g, tabPlacement, tabIndex, tabRect.x, tabRect.y, tabRect.width, tabRect.height, isSelected );

		String title = this.tabPane.getTitleAt( tabIndex );
		Font font = this.tabPane.getFont();
		FontMetrics metrics = g.getFontMetrics( font );
		Icon icon = this.getIconForTab( tabIndex );

		this.layoutLabel( tabPlacement, metrics, tabIndex, title, icon, tabRect, iconRect, textRect, isSelected );

		this.paintText( g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected );

		this.paintIcon( g, tabPlacement, tabIndex, icon, iconRect, isSelected );

		this.paintFocusIndicator( g, tabPlacement, rects, tabIndex, iconRect, textRect, isSelected );

		if ( cropShape )
		{
			this.paintCroppedTabEdge( g, tabPlacement, tabIndex, isSelected, cropx, cropy );
			g2.setClip( save );

		}
		else if ( this.closeIconStyle != CloseTabPaneUI.NO_CLOSE_ICON && isOver )
		{

			int dx = tabRect.x + tabRect.width - CloseTabPaneUI.BUTTONSIZE - CloseTabPaneUI.WIDTHDELTA;
			int dy = ( tabRect.y + tabRect.height ) / 2 - 6;

			if ( isSelected && this.closeIconStyle == CloseTabPaneUI.GRAY_CLOSE_ICON )
			{
				this.paintCloseIcon( g2, dx, dy, true );
			}
			else if ( !isSelected && this.closeIconStyle == CloseTabPaneUI.RED_CLOSE_ICON )
			{
				this.paintCloseIcon( g2, dx, dy, false );
			}
		}

	}

	protected void paintCloseIcon( final Graphics g, final int dx, final int dy, final boolean isSelected )
	{

		if ( isSelected )
		{
			this.paintActionButton(
				g, dx, dy, this.closeIndexStatus, false, CloseTabPaneUI.closeGrayB, CloseTabPaneUI.closeGrayImgB );
			g.drawImage( CloseTabPaneUI.closeGrayImgI, dx, dy + 1, null );
		}
		else
		{
			this.paintActionButton(
				g, dx, dy, this.closeIndexStatus, false, CloseTabPaneUI.closeRedB, CloseTabPaneUI.closeRedImgB );
			g.drawImage( CloseTabPaneUI.closeRedImgI, dx, dy + 1, null );
		}
	}

	protected void paintActionButton( final Graphics g, final int dx, final int dy, final int status,
		final boolean isOver, final JButton button, final BufferedImage image )
	{

		button.setBorder( null );

		//		button.setBackground(tabScroller.tabPanel.getBackground());
		button.paint( image.getGraphics() );
		g.drawImage( image, dx, dy, null );
	}

	/*
	 * This method will create and return a polygon shape for the given tab rectangle which has been cropped at the
	 * specified cropline with a torn edge visual. e.g. A "File" tab which has cropped been cropped just after the "i":
	 * ------------- | ..... | | . | | ... . | | . . | | . . | | . . | -------------- The x, y arrays below define the
	 * pattern used to create a "torn" edge segment which is repeated to fill the edge of the tab. For tabs placed on
	 * TOP and BOTTOM, this righthand torn edge is created by line segments which are defined by coordinates obtained by
	 * subtracting xCropLen[i] from (tab.x + tab.width) and adding yCroplen[i] to (tab.y). For tabs placed on LEFT or
	 * RIGHT, the bottom torn edge is created by subtracting xCropLen[i] from (tab.y + tab.height) and adding
	 * yCropLen[i] to (tab.x).
	 */

	private void paintCroppedTabEdge( final Graphics g, final int tabPlacement, final int tabIndex,
		final boolean isSelected, final int x, final int y )
	{

		g.setColor( this.shadow );
		g.drawLine( x, y, x, y + this.rects[ tabIndex ].height );

	}

	private void ensureCurrentLayout()
	{
		if ( !this.tabPane.isValid() )
		{
			this.tabPane.validate();
		}
		/*
		 * If tabPane doesn't have a peer yet, the validate() call will silently fail. We handle that by forcing a
		 * layout if tabPane is still invalid. See bug 4237677.
		 */
		if ( !this.tabPane.isValid() )
		{
			TabbedPaneLayout layout = (TabbedPaneLayout) this.tabPane.getLayout();
			layout.calculateLayoutInfo();
		}
	}

	/**
	 * Returns the bounds of the specified tab in the coordinate space of the JTabbedPane component. This is required
	 * because the tab rects are by default defined in the coordinate space of the component where they are rendered,
	 * which could be the JTabbedPane (for WRAP_TAB_LAYOUT) or a ScrollableTabPanel (SCROLL_TAB_LAYOUT). This method
	 * should be used whenever the tab rectangle must be relative to the JTabbedPane itself and the result should be
	 * placed in a designated Rectangle object (rather than instantiating and returning a new Rectangle each time). The
	 * tab index parameter must be a valid tabbed pane tab index (0 to tab count - 1, inclusive). The destination
	 * rectangle parameter must be a valid <code>Rectangle</code> instance. The handling of invalid parameters is
	 * unspecified.
	 *
	 * @param tabIndex the index of the tab
	 * @param dest the rectangle where the result should be placed
	 * @return the resulting rectangle
	 * @since 1.4
	 */

	@Override
	protected Rectangle getTabBounds( final int tabIndex, final Rectangle dest )
	{
		dest.width = this.rects[ tabIndex ].width;
		dest.height = this.rects[ tabIndex ].height;

		Point vpp = this.tabScroller.viewport.getLocation();
		Point viewp = this.tabScroller.viewport.getViewPosition();
		dest.x = this.rects[ tabIndex ].x + vpp.x - viewp.x;
		dest.y = this.rects[ tabIndex ].y + vpp.y - viewp.y;

		return dest;
	}

	private int getTabAtLocation( final int x, final int y )
	{
		this.ensureCurrentLayout();

		int tabCount = this.tabPane.getTabCount();
		for ( int i = 0; i < tabCount; i++ )
		{
			if ( this.rects[ i ].contains( x, y ) )
			{
				return i;
			}
		}
		return -1;
	}

	public int getOverTabIndex()
	{
		return this.overTabIndex;
	}

	/**
	 * Returns the index of the tab closest to the passed in location, note that the returned tab may not contain the
	 * location x,y.
	 */
	private int getClosestTab( final int x, final int y )
	{
		int min = 0;
		int tabCount = Math.min( this.rects.length, this.tabPane.getTabCount() );
		int max = tabCount;
		int tabPlacement = this.tabPane.getTabPlacement();
		boolean useX = tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM;
		int want = useX ? x : y;

		while ( min != max )
		{
			int current = ( max + min ) / 2;
			int minLoc;
			int maxLoc;

			if ( useX )
			{
				minLoc = this.rects[ current ].x;
				maxLoc = minLoc + this.rects[ current ].width;
			}
			else
			{
				minLoc = this.rects[ current ].y;
				maxLoc = minLoc + this.rects[ current ].height;
			}
			if ( want < minLoc )
			{
				max = current;
				if ( min == max )
				{
					return Math.max( 0, current - 1 );
				}
			}
			else if ( want >= maxLoc )
			{
				min = current;
				if ( max - min <= 1 )
				{
					return Math.max( current + 1, tabCount - 1 );
				}
			}
			else
			{
				return current;
			}
		}
		return min;
	}

	// REMIND(aim,7/29/98): This method should be made
	// protected in the next release where
	// API changes are allowed
	//
	boolean requestMyFocusForVisibleComponent()
	{
		return false;
	}

	private class RightAction
		extends AbstractAction
	{
		public void actionPerformed( final ActionEvent e )
		{
			JTabbedPane pane = (JTabbedPane) e.getSource();
			CloseTabPaneUI ui = (CloseTabPaneUI) pane.getUI();
			ui.navigateSelectedTab( SwingConstants.EAST );
		}
	};

	private class LeftAction
		extends AbstractAction
	{
		public void actionPerformed( final ActionEvent e )
		{
			JTabbedPane pane = (JTabbedPane) e.getSource();
			CloseTabPaneUI ui = (CloseTabPaneUI) pane.getUI();
			ui.navigateSelectedTab( SwingConstants.WEST );
		}
	};

	private class UpAction
		extends AbstractAction
	{
		public void actionPerformed( final ActionEvent e )
		{
			JTabbedPane pane = (JTabbedPane) e.getSource();
			CloseTabPaneUI ui = (CloseTabPaneUI) pane.getUI();
			ui.navigateSelectedTab( SwingConstants.NORTH );
		}
	};

	private class DownAction
		extends AbstractAction
	{
		public void actionPerformed( final ActionEvent e )
		{
			JTabbedPane pane = (JTabbedPane) e.getSource();
			CloseTabPaneUI ui = (CloseTabPaneUI) pane.getUI();
			ui.navigateSelectedTab( SwingConstants.SOUTH );
		}
	};

	private class NextAction
		extends AbstractAction
	{
		public void actionPerformed( final ActionEvent e )
		{
			JTabbedPane pane = (JTabbedPane) e.getSource();
			CloseTabPaneUI ui = (CloseTabPaneUI) pane.getUI();
			ui.navigateSelectedTab( SwingConstants.NEXT );
		}
	};

	private class PreviousAction
		extends AbstractAction
	{
		public void actionPerformed( final ActionEvent e )
		{
			JTabbedPane pane = (JTabbedPane) e.getSource();
			CloseTabPaneUI ui = (CloseTabPaneUI) pane.getUI();
			ui.navigateSelectedTab( SwingConstants.PREVIOUS );
		}
	};

	private class PageUpAction
		extends AbstractAction
	{
		public void actionPerformed( final ActionEvent e )
		{
			JTabbedPane pane = (JTabbedPane) e.getSource();
			CloseTabPaneUI ui = (CloseTabPaneUI) pane.getUI();
			int tabPlacement = pane.getTabPlacement();
			if ( tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM )
			{
				ui.navigateSelectedTab( SwingConstants.WEST );
			}
			else
			{
				ui.navigateSelectedTab( SwingConstants.NORTH );
			}
		}
	};

	private class PageDownAction
		extends AbstractAction
	{
		public void actionPerformed( final ActionEvent e )
		{
			JTabbedPane pane = (JTabbedPane) e.getSource();
			CloseTabPaneUI ui = (CloseTabPaneUI) pane.getUI();
			int tabPlacement = pane.getTabPlacement();
			if ( tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM )
			{
				ui.navigateSelectedTab( SwingConstants.EAST );
			}
			else
			{
				ui.navigateSelectedTab( SwingConstants.SOUTH );
			}
		}
	};

	/**
	 * Selects a tab in the JTabbedPane based on the String of the action command. The tab selected is based on the
	 * first tab that has a mnemonic matching the first character of the action command.
	 */
	private class SetSelectedIndexAction
		extends AbstractAction
	{
		public void actionPerformed( final ActionEvent e )
		{
			JTabbedPane pane = (JTabbedPane) e.getSource();

			if ( pane != null && pane.getUI() instanceof CloseTabPaneUI )
			{
				CloseTabPaneUI ui = (CloseTabPaneUI) pane.getUI();
				String command = e.getActionCommand();

				if ( command != null && command.length() > 0 )
				{
					int mnemonic = e.getActionCommand().charAt( 0 );
					if ( mnemonic >= 'a' && mnemonic <= 'z' )
					{
						mnemonic -= 'a' - 'A';
					}
					Integer index = (Integer) ui.mnemonicToIndexMap.get( new Integer( mnemonic ) );
					if ( index != null && pane.isEnabledAt( index.intValue() ) )
					{
						pane.setSelectedIndex( index.intValue() );
					}
				}
			}
		}
	};

	private class ScrollTabsForwardAction
		extends AbstractAction
	{
		public void actionPerformed( final ActionEvent e )
		{
			JTabbedPane pane = null;
			Object src = e.getSource();
			if ( src instanceof JTabbedPane )
			{
				pane = (JTabbedPane) src;
			}
			else if ( src instanceof ScrollableTabButton )
			{
				pane = (JTabbedPane) ( (ScrollableTabButton) src ).getParent();
			}
			else
			{
				return; // shouldn't happen
			}
			CloseTabPaneUI ui = (CloseTabPaneUI) pane.getUI();

			ui.tabScroller.scrollForward( pane.getTabPlacement() );

		}
	}

	private class ScrollTabsBackwardAction
		extends AbstractAction
	{
		public void actionPerformed( final ActionEvent e )
		{
			JTabbedPane pane = null;
			Object src = e.getSource();
			if ( src instanceof JTabbedPane )
			{
				pane = (JTabbedPane) src;
			}
			else if ( src instanceof ScrollableTabButton )
			{
				pane = (JTabbedPane) ( (ScrollableTabButton) src ).getParent();
			}
			else
			{
				return; // shouldn't happen
			}
			CloseTabPaneUI ui = (CloseTabPaneUI) pane.getUI();

			ui.tabScroller.scrollBackward( pane.getTabPlacement() );

		}
	}

	/**
	 * This inner class is marked &quot;public&quot; due to a compiler bug. This class should be treated as a
	 * &quot;protected&quot; inner class. Instantiate it only within subclasses of BasicTabbedPaneUI.
	 */

	private class TabbedPaneScrollLayout
		extends TabbedPaneLayout
	{

		@Override
		protected int preferredTabAreaHeight( final int tabPlacement, final int width )
		{
			return CloseTabPaneUI.this.calculateMaxTabHeight( tabPlacement );
		}

		@Override
		protected int preferredTabAreaWidth( final int tabPlacement, final int height )
		{
			return CloseTabPaneUI.this.calculateMaxTabWidth( tabPlacement );
		}

		@Override
		public void layoutContainer( final Container parent )
		{
			int tabPlacement = CloseTabPaneUI.this.tabPane.getTabPlacement();
			int tabCount = CloseTabPaneUI.this.tabPane.getTabCount();
			Insets insets = CloseTabPaneUI.this.tabPane.getInsets();
			int selectedIndex = CloseTabPaneUI.this.tabPane.getSelectedIndex();
			Component visibleComponent = CloseTabPaneUI.this.getVisibleComponent();

			this.calculateLayoutInfo();

			if ( selectedIndex < 0 )
			{
				if ( visibleComponent != null )
				{
					// The last tab was removed, so remove the component
					CloseTabPaneUI.this.setVisibleComponent( null );
				}
			}
			else
			{
				Component selectedComponent = CloseTabPaneUI.this.tabPane.getComponentAt( selectedIndex );
				boolean shouldChangeFocus = false;

				// In order to allow programs to use a single component
				// as the display for multiple tabs, we will not change
				// the visible compnent if the currently selected tab
				// has a null component. This is a bit dicey, as we don't
				// explicitly state we support this in the spec, but since
				// programs are now depending on this, we're making it work.
				//
				if ( selectedComponent != null )
				{
					CloseTabPaneUI.this.setVisibleComponent( selectedComponent );
				}
				int tx, ty, tw, th; // tab area bounds
				int cx, cy, cw, ch; // content area bounds
				Insets contentInsets = CloseTabPaneUI.this.getContentBorderInsets( tabPlacement );
				Rectangle bounds = CloseTabPaneUI.this.tabPane.getBounds();
				int numChildren = CloseTabPaneUI.this.tabPane.getComponentCount();

				if ( numChildren > 0 )
				{

					// calculate tab area bounds
					tw = bounds.width - insets.left - insets.right;
					th =
						CloseTabPaneUI.this.calculateTabAreaHeight(
							tabPlacement, CloseTabPaneUI.this.runCount, CloseTabPaneUI.this.maxTabHeight );
					tx = insets.left;
					ty = insets.top;

					// calculate content area bounds
					cx = tx + contentInsets.left;
					cy = ty + th + contentInsets.top;
					cw = bounds.width - insets.left - insets.right - contentInsets.left - contentInsets.right;
					ch = bounds.height - insets.top - insets.bottom - th - contentInsets.top - contentInsets.bottom;

					for ( int i = 0; i < numChildren; i++ )
					{
						Component child = CloseTabPaneUI.this.tabPane.getComponent( i );

						if ( child instanceof ScrollableTabViewport )
						{
							JViewport viewport = (JViewport) child;
							Rectangle viewRect = viewport.getViewRect();
							int vw = tw;
							int vh = th;

							int totalTabWidth =
								CloseTabPaneUI.this.rects[ tabCount - 1 ].x + CloseTabPaneUI.this.rects[ tabCount - 1 ].width;
							if ( totalTabWidth > tw )
							{
								// Need to allow space for scrollbuttons
								vw = Math.max( tw - 36, 36 );;
								if ( totalTabWidth - viewRect.x <= vw )
								{
									// Scrolled to the end, so ensure the
									// viewport size is
									// such that the scroll offset aligns with a
									// tab
									vw = totalTabWidth - viewRect.x;
								}
							}

							child.setBounds( tx, ty, vw, vh );

						}
						else if ( child instanceof ScrollableTabButton )
						{
							ScrollableTabButton scrollbutton = (ScrollableTabButton) child;
							Dimension bsize = scrollbutton.getPreferredSize();
							int bx = 0;
							int by = 0;
							int bw = bsize.width;
							int bh = bsize.height;
							boolean visible = false;

							int totalTabWidth =
								CloseTabPaneUI.this.rects[ tabCount - 1 ].x + CloseTabPaneUI.this.rects[ tabCount - 1 ].width;

							if ( totalTabWidth > tw )
							{
								int dir = scrollbutton.scrollsForward() ? SwingConstants.EAST : SwingConstants.WEST;
								scrollbutton.setDirection( dir );
								visible = true;
								bx =
									dir == SwingConstants.EAST ? bounds.width - insets.left - bsize.width : bounds.width - insets.left - 2 * bsize.width;
								by = tabPlacement == SwingConstants.TOP ? ty + th - bsize.height : ty;
							}

							child.setVisible( visible );
							if ( visible )
							{
								child.setBounds( bx, by, bw, bh );
							}

						}
						else
						{
							// All content children...
							child.setBounds( cx, cy, cw, ch );
						}
					}
					if ( shouldChangeFocus )
					{
						if ( !CloseTabPaneUI.this.requestMyFocusForVisibleComponent() )
						{
							CloseTabPaneUI.this.tabPane.requestFocusInWindow();
						}
					}
				}
			}
		}

		@Override
		protected void calculateTabRects( final int tabPlacement, final int tabCount )
		{
			FontMetrics metrics = CloseTabPaneUI.this.getFontMetrics();
			Insets tabAreaInsets = CloseTabPaneUI.this.getTabAreaInsets( tabPlacement );
			int i;

			int x = tabAreaInsets.left - 2;
			int y = tabAreaInsets.top;
			int totalWidth = 0;
			int totalHeight = 0;

			//
			// Calculate bounds within which a tab run must fit
			//

			CloseTabPaneUI.this.maxTabHeight = CloseTabPaneUI.this.calculateMaxTabHeight( tabPlacement );

			CloseTabPaneUI.this.runCount = 0;
			CloseTabPaneUI.this.selectedRun = -1;

			if ( tabCount == 0 )
			{
				return;
			}

			CloseTabPaneUI.this.selectedRun = 0;
			CloseTabPaneUI.this.runCount = 1;

			// Run through tabs and lay them out in a single run
			Rectangle rect;
			for ( i = 0; i < tabCount; i++ )
			{
				rect = CloseTabPaneUI.this.rects[ i ];

				if ( i > 0 )
				{
					rect.x = CloseTabPaneUI.this.rects[ i - 1 ].x + CloseTabPaneUI.this.rects[ i - 1 ].width - 1;
				}
				else
				{
					CloseTabPaneUI.this.tabRuns[ 0 ] = 0;
					CloseTabPaneUI.this.maxTabWidth = 0;
					totalHeight += CloseTabPaneUI.this.maxTabHeight;
					rect.x = x;
				}
				rect.width = CloseTabPaneUI.this.calculateTabWidth( tabPlacement, i, metrics );
				totalWidth = rect.x + rect.width;
				CloseTabPaneUI.this.maxTabWidth = Math.max( CloseTabPaneUI.this.maxTabWidth, rect.width );

				rect.y = y;
				rect.height = CloseTabPaneUI.this.maxTabHeight;

			}

			//tabPanel.setSize(totalWidth, totalHeight);
			CloseTabPaneUI.this.tabScroller.tabPanel.setPreferredSize( new Dimension( totalWidth, totalHeight ) );
		}
	}

	private class ScrollableTabSupport
		implements ChangeListener
	{
		public ScrollableTabViewport viewport;

		public ScrollableTabPanel tabPanel;

		public ScrollableTabButton scrollForwardButton;

		public ScrollableTabButton scrollBackwardButton;

		public int leadingTabIndex;

		private final Point tabViewPosition = new Point( 0, 0 );

		ScrollableTabSupport( final int tabPlacement )
		{
			this.viewport = new ScrollableTabViewport();
			this.tabPanel = new ScrollableTabPanel();
			this.viewport.setView( this.tabPanel );
			this.viewport.addChangeListener( this );

			this.scrollForwardButton = CloseTabPaneUI.this.createScrollableTabButton( SwingConstants.EAST );
			this.scrollBackwardButton = CloseTabPaneUI.this.createScrollableTabButton( SwingConstants.WEST );
			//			scrollForwardButton = new ScrollableTabButton(EAST);
			//			scrollBackwardButton = new ScrollableTabButton(WEST);
		}

		public void scrollForward( final int tabPlacement )
		{
			Dimension viewSize = this.viewport.getViewSize();
			Rectangle viewRect = this.viewport.getViewRect();

			if ( tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM )
			{
				if ( viewRect.width >= viewSize.width - viewRect.x )
				{
					return; // no room left to scroll
				}
			}
			else if ( viewRect.height >= viewSize.height - viewRect.y )
			{
				return;
			}
			this.setLeadingTabIndex( tabPlacement, this.leadingTabIndex + 1 );
		}

		public void scrollBackward( final int tabPlacement )
		{
			if ( this.leadingTabIndex == 0 )
			{
				return; // no room left to scroll
			}
			this.setLeadingTabIndex( tabPlacement, this.leadingTabIndex - 1 );
		}

		public void setLeadingTabIndex( final int tabPlacement, final int index )
		{
			this.leadingTabIndex = index;
			Dimension viewSize = this.viewport.getViewSize();
			Rectangle viewRect = this.viewport.getViewRect();

			this.tabViewPosition.x =
				this.leadingTabIndex == 0 ? 0 : CloseTabPaneUI.this.rects[ this.leadingTabIndex ].x;

			if ( viewSize.width - this.tabViewPosition.x < viewRect.width )
			{
				// We've scrolled to the end, so adjust the viewport size
				// to ensure the view position remains aligned on a tab boundary
				Dimension extentSize = new Dimension( viewSize.width - this.tabViewPosition.x, viewRect.height );
				this.viewport.setExtentSize( extentSize );
			}

			this.viewport.setViewPosition( this.tabViewPosition );
		}

		public void stateChanged( final ChangeEvent e )
		{
			JViewport viewport = (JViewport) e.getSource();
			int tabPlacement = CloseTabPaneUI.this.tabPane.getTabPlacement();
			int tabCount = CloseTabPaneUI.this.tabPane.getTabCount();
			Rectangle vpRect = viewport.getBounds();
			Dimension viewSize = viewport.getViewSize();
			Rectangle viewRect = viewport.getViewRect();

			this.leadingTabIndex = CloseTabPaneUI.this.getClosestTab( viewRect.x, viewRect.y );

			if ( CloseTabPaneUI.this.rects.length <= this.leadingTabIndex )
			{
				return;
			}

			// If the tab isn't right aligned, adjust it.
			if ( this.leadingTabIndex + 1 < tabCount )
			{
				if ( CloseTabPaneUI.this.rects[ this.leadingTabIndex ].x < viewRect.x )
				{
					this.leadingTabIndex++ ;
				}
			}
			Insets contentInsets = CloseTabPaneUI.this.getContentBorderInsets( tabPlacement );

			CloseTabPaneUI.this.tabPane.repaint( vpRect.x, vpRect.y + vpRect.height, vpRect.width, contentInsets.top );
			this.scrollBackwardButton.setEnabled( viewRect.x > 0 );
			this.scrollForwardButton.setEnabled( this.leadingTabIndex < tabCount - 1 && viewSize.width - viewRect.x > viewRect.width );

		}

		@Override
		public String toString()
		{
			return new String(
				"viewport.viewSize=" + this.viewport.getViewSize() + "\n" + "viewport.viewRectangle=" + this.viewport.getViewRect() + "\n" + "leadingTabIndex=" + this.leadingTabIndex + "\n" + "tabViewPosition=" + this.tabViewPosition );
		}

	}

	private class ScrollableTabViewport
		extends JViewport
		implements UIResource
	{
		public ScrollableTabViewport()
		{
			super();
			this.setScrollMode( JViewport.SIMPLE_SCROLL_MODE );
		}
	}

	private class ScrollableTabPanel
		extends JPanel
		implements UIResource
	{
		public ScrollableTabPanel()
		{
			this.setLayout( null );
		}

		@Override
		public void paintComponent( final Graphics g )
		{
			super.paintComponent( g );
			CloseTabPaneUI.this.paintTabArea(
				g, CloseTabPaneUI.this.tabPane.getTabPlacement(), CloseTabPaneUI.this.tabPane.getSelectedIndex() );

		}
	}

	protected class ScrollableTabButton
		extends BasicArrowButton
		implements UIResource, SwingConstants
	{
		public ScrollableTabButton( final int direction )
		{
			super(
				direction, UIManager.getColor( "TabbedPane.selected" ), UIManager.getColor( "TabbedPane.shadow" ),
				UIManager.getColor( "TabbedPane.darkShadow" ), UIManager.getColor( "TabbedPane.highlight" ) );

		}

		public boolean scrollsForward()
		{
			return this.direction == SwingConstants.EAST || this.direction == SwingConstants.SOUTH;
		}

	}

	/**
	 * This inner class is marked &quot;public&quot; due to a compiler bug. This class should be treated as a
	 * &quot;protected&quot; inner class. Instantiate it only within subclasses of BasicTabbedPaneUI.
	 */
	public class TabSelectionHandler
		implements ChangeListener
	{
		public void stateChanged( final ChangeEvent e )
		{
			JTabbedPane tabPane = (JTabbedPane) e.getSource();
			tabPane.revalidate();
			tabPane.repaint();

			if ( tabPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT )
			{
				int index = tabPane.getSelectedIndex();
				if ( index < CloseTabPaneUI.this.rects.length && index != -1 )
				{
					CloseTabPaneUI.this.tabScroller.tabPanel.scrollRectToVisible( CloseTabPaneUI.this.rects[ index ] );
				}
			}
		}
	}

	/**
	 * This inner class is marked &quot;public&quot; due to a compiler bug. This class should be treated as a
	 * &quot;protected&quot; inner class. Instantiate it only within subclasses of BasicTabbedPaneUI.
	 */

	/*
	 * GES 2/3/99: The container listener code was added to support HTML rendering of tab titles. Ideally, we would be
	 * able to listen for property changes when a tab is added or its text modified. At the moment there are no such
	 * events because the Beans spec doesn't allow 'indexed' property changes (i.e. tab 2's text changed from A to B).
	 * In order to get around this, we listen for tabs to be added or removed by listening for the container events. we
	 * then queue up a runnable (so the component has a chance to complete the add) which checks the tab title of the
	 * new component to see if it requires HTML rendering. The Views (one per tab title requiring HTML rendering) are
	 * stored in the htmlViews ArrayList, which is only allocated after the first time we run into an HTML tab. Note
	 * that this vector is kept in step with the number of pages, and nulls are added for those pages whose tab title do
	 * not require HTML rendering. This makes it easy for the paint and layout code to tell whether to invoke the HTML
	 * engine without having to check the string during time-sensitive operations. When we have added a way to listen
	 * for tab additions and changes to tab text, this code should be removed and replaced by something which uses that.
	 */

	private class ContainerHandler
		implements ContainerListener
	{
		public void componentAdded( final ContainerEvent e )
		{
			JTabbedPane tp = (JTabbedPane) e.getContainer();
			Component child = e.getChild();
			if ( child instanceof UIResource )
			{
				return;
			}
			int index = tp.indexOfComponent( child );
			String title = tp.getTitleAt( index );
			boolean isHTML = BasicHTML.isHTMLString( title );
			if ( isHTML )
			{
				if ( CloseTabPaneUI.this.htmlViews == null )
				{
					CloseTabPaneUI.this.htmlViews = CloseTabPaneUI.this.createHTMLArrayList();
				}
				else
				{ // ArrayList already exists
					View v = BasicHTML.createHTMLView( tp, title );
					CloseTabPaneUI.this.htmlViews.add( index, v );
				}
			}
			else if ( CloseTabPaneUI.this.htmlViews != null )
			{
				CloseTabPaneUI.this.htmlViews.add( index, null );
			}
		}

		public void componentRemoved( final ContainerEvent e )
		{
			JTabbedPane tp = (JTabbedPane) e.getContainer();
			Component child = e.getChild();
			if ( child instanceof UIResource )
			{
				return;
			}

			// NOTE 4/15/2002 (joutwate):
			// This fix is implemented using client properties since there is
			// currently no IndexPropertyChangeEvent. Once
			// IndexPropertyChangeEvents have been added this code should be
			// modified to use it.
			Integer indexObj = (Integer) tp.getClientProperty( "__index_to_remove__" );
			if ( indexObj != null )
			{
				int index = indexObj.intValue();
				if ( CloseTabPaneUI.this.htmlViews != null && CloseTabPaneUI.this.htmlViews.size() >= index )
				{
					CloseTabPaneUI.this.htmlViews.remove( index );
				}
			}
		}
	}

	private ArrayList createHTMLArrayList()
	{
		ArrayList htmlViews = new ArrayList();
		int count = this.tabPane.getTabCount();
		if ( count > 0 )
		{
			for ( int i = 0; i < count; i++ )
			{
				String title = this.tabPane.getTitleAt( i );
				if ( BasicHTML.isHTMLString( title ) )
				{
					htmlViews.add( BasicHTML.createHTMLView( this.tabPane, title ) );
				}
				else
				{
					htmlViews.add( null );
				}
			}
		}
		return htmlViews;
	}

	class MyMouseHandler
		extends MouseHandler
	{
		public MyMouseHandler()
		{
			super();
		}

		@Override
		public void mousePressed( final MouseEvent e )
		{

			if ( CloseTabPaneUI.this.closeIndexStatus == CloseTabPaneUI.this.OVER )
			{
				CloseTabPaneUI.this.closeIndexStatus = CloseTabPaneUI.this.PRESSED;
				CloseTabPaneUI.this.tabScroller.tabPanel.repaint();
				return;
			}

			if ( CloseTabPaneUI.this.maxIndexStatus == CloseTabPaneUI.this.OVER )
			{
				CloseTabPaneUI.this.maxIndexStatus = CloseTabPaneUI.this.PRESSED;
				CloseTabPaneUI.this.tabScroller.tabPanel.repaint();
				return;
			}

			if ( CloseTabPaneUI.this.closeIndexStatus == CloseTabPaneUI.this.PRESSED || CloseTabPaneUI.this.maxIndexStatus == CloseTabPaneUI.this.PRESSED )
			{
				return;
			}

			super.mousePressed( e );
		}

		@Override
		public void mouseClicked( final MouseEvent e )
		{

			super.mousePressed( e );

			if ( e.getClickCount() > 1 && CloseTabPaneUI.this.overTabIndex != -1 )
			{
				( (CloseTabbedPane) CloseTabPaneUI.this.tabPane ).fireDoubleClickTabEvent(
					e, CloseTabPaneUI.this.overTabIndex );
			}
		}

		@Override
		public void mouseReleased( final MouseEvent e )
		{

			if ( CloseTabPaneUI.this.overTabIndex == -1 )
			{
				if ( e.isPopupTrigger() )
				{
					( (CloseTabbedPane) CloseTabPaneUI.this.tabPane ).firePopupOutsideTabEvent( e );
				}
				return;
			}

			if ( CloseTabPaneUI.this.isOneActionButtonEnabled() && e.isPopupTrigger() )
			{
				super.mousePressed( e );

				CloseTabPaneUI.this.closeIndexStatus = CloseTabPaneUI.this.INACTIVE; //Prevent undesired action when
				CloseTabPaneUI.this.maxIndexStatus = CloseTabPaneUI.this.INACTIVE; //right-clicking on icons

				CloseTabPaneUI.this.actionPopupMenu.show( CloseTabPaneUI.this.tabScroller.tabPanel, e.getX(), e.getY() );
				return;
			}

			if ( CloseTabPaneUI.this.closeIndexStatus == CloseTabPaneUI.this.PRESSED )
			{

				boolean shouldClose =
					CloseTabPaneUI.this.overTabIndex >= CloseTabPaneUI.this.tabStates.size() || CloseTabPaneUI.this.tabStates.get( CloseTabPaneUI.this.overTabIndex ) == Boolean.FALSE;

				if ( CloseTabPaneUI.this.closeIconStyle == CloseTabPaneUI.GRAY_CLOSE_ICON )
				{
					shouldClose &= CloseTabPaneUI.this.tabPane.getSelectedIndex() == CloseTabPaneUI.this.overTabIndex;
				}
				else
				{
					shouldClose &= CloseTabPaneUI.this.tabPane.getSelectedIndex() != CloseTabPaneUI.this.overTabIndex;
				}

				if ( shouldClose )
				{
					CloseTabPaneUI.this.closeIndexStatus = CloseTabPaneUI.this.OVER;
					CloseTabPaneUI.this.tabScroller.tabPanel.repaint();

					( (CloseTabbedPane) CloseTabPaneUI.this.tabPane ).fireCloseTabEvent(
						e, CloseTabPaneUI.this.overTabIndex );
					return;
				}
			}

			if ( CloseTabPaneUI.this.maxIndexStatus == CloseTabPaneUI.this.PRESSED )
			{
				CloseTabPaneUI.this.maxIndexStatus = CloseTabPaneUI.this.OVER;
				CloseTabPaneUI.this.tabScroller.tabPanel.repaint();

				( (CloseTabbedPane) CloseTabPaneUI.this.tabPane ).fireMaxTabEvent( e, CloseTabPaneUI.this.overTabIndex );
				return;
			}
		}

		@Override
		public void mouseExited( final MouseEvent e )
		{
			if ( !CloseTabPaneUI.this.mousePressed )
			{
				CloseTabPaneUI.this.overTabIndex = -1;
				CloseTabPaneUI.this.tabScroller.tabPanel.repaint();
			}
		}

	}

	class MyMouseMotionListener
		implements MouseMotionListener
	{

		public void mouseMoved( final MouseEvent e )
		{
			if ( CloseTabPaneUI.this.actionPopupMenu.isVisible() )
			{
				return; //No updates when popup is visible
			}
			CloseTabPaneUI.this.mousePressed = false;
			CloseTabPaneUI.this.setTabIcons( e.getX(), e.getY() );
		}

		public void mouseDragged( final MouseEvent e )
		{
			if ( CloseTabPaneUI.this.actionPopupMenu.isVisible() )
			{
				return; //No updates when popup is visible
			}
			CloseTabPaneUI.this.mousePressed = true;
			CloseTabPaneUI.this.setTabIcons( e.getX(), e.getY() );
		}
	}

}