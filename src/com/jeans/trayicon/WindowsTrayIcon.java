/***
 * Windows Tray Icon
 * -----------------
 *
 * Written by Jan Struyf
 *
 *  jan.struyf@cs.kuleuven.ac.be
 *  http://jeans.studentenweb.org/java/trayicon/trayicon.html
 *
 * Please mail me if you
 *	- 've found bugs
 *	- like this program
 *	- don't like a particular feature
 *	- would like something to be modified
 *
 * I always give it my best shot to make a program useful and solid, but
 * remeber that there is absolutely no warranty for using this program as
 * stated in the following terms:
 *
 * THERE IS NO WARRANTY FOR THIS PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE
 * LAW. THE COPYRIGHT HOLDER AND/OR OTHER PARTIES WHO MAY HAVE MODIFIED THE
 * PROGRAM, PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  THE ENTIRE RISK AS
 * TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU.  SHOULD THE
 * PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING,
 * REPAIR OR CORRECTION.
 *
 * IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW WILL ANY COPYRIGHT HOLDER,
 * OR ANY OTHER PARTY WHO MAY MODIFY AND/OR REDISTRIBUTE THE PROGRAM,
 * BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR
 * CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE
 * PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING RENDERED
 * INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 * PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER
 * PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * May the Force be with you... Just compile it & use it!
 */

package com.jeans.trayicon;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.util.Enumeration;
import java.util.Vector;

/**
 * WindowsTrayIcon
 * A Java Implementation for showing icons in the Windows System Tray
 *
 * Written by Jan Struyf
 *	(jan.struyf@cs.kuleuven.ac.be)
 *	(http://ace.ulyssis.org/~jeans)
 *
 * Instantiate this class for each icon
 * This file comes with native code in TRAYICON.DLL
 * The DLL should go in C:/WINDOWS/SYSTEM or in your current directory
 *
 * Modified by the KoLmafia development team
 * Library is no longer loaded here; it is loaded by KoLmafia.
 */
public class WindowsTrayIcon {

    public final static String TRAY_VERSION = "1.7.9b";

    private static TrayIconKeeper m_Keeper;
    private static TrayDummyComponent m_Dummy;
    private static MouseListener m_MouseHook;
    private static Window m_CurrentWindow;

/****************************************************************************************************************
 *                                                                                                              *
 * Initialisation / Termination                                                                                 *
 *                                                                                                              *
 ****************************************************************************************************************/

/**
 * Init native library - call this method in the main() method of your app.
 * Each app has it's own hidden window that receives the mouse/menu messages for its Tray Icons.
 * The window title is used by sendWindowsMessage() and isRunning() to identify an app.
 *
 * @param appName	the title for the hidden window
 */
	public static void initTrayIcon(String appName) {
		initTrayIcon(appName, new WindowsTrayIcon());
	}

/**
 * Free all native resources - call this method before System.exit()
 */
	public static void cleanUp() {
		if (m_Keeper != null) {
			m_Keeper.doNotify();
			m_Keeper = null;
		}
		termTrayIcon();
	}

/****************************************************************************************************************
 *                                                                                                              *
 * Constructor                                                                                                  *
 *                                                                                                              *
 ****************************************************************************************************************/

/**
 * Construct a new Tray Icon.
 * Using a Java Image - This can be loaded from a 16x16 GIF or JPG file.
 *
 * @param image	16x16 icon - make sure it's loaded in memory - use MediaTracker
 ** @param w	the icon width - eg. 16
 ** @param h	the icon height - eg. 16
 *
 * @exception TrayIconException - if something goes wrong :O(
 *	- Too many icons allocated
 *	- Error initializing native code DLL
 *	- Error setting up Windows notify procedure
 *	- Error loading icon image
 * @exception InterruptedException - if the thread loading the image is interrupted
 */
	public WindowsTrayIcon(Image image, int w, int h) throws TrayIconException, InterruptedException {
		// Allocate new id for icon (native routine)
		m_ID = getFreeId();
		if (m_ID == TOOMANYICONS)
			throw new TrayIconException("Too many icons allocated");
		if (m_ID == DLLNOTFOUND)
			throw new TrayIconException("Error initializing native code DLL");
		if (m_ID == NOTIFYPROCERR)
			throw new TrayIconException("Error setting up Windows notify procedure");
		// Store image data and size
		setImage(image, w, h);
	}

/****************************************************************************************************************
 *                                                                                                              *
 * Methods                                                                                                      *
 *                                                                                                              *
 ****************************************************************************************************************/

/**
 * Change this icon's Image
 * Using a Java Image - This can be loaded from a 16x16 GIF or JPG file
 *
 ** @param image	16x16 icon - make sure it's loaded in memory - use MediaTracker
 ** @param w	the icon width
 ** @param h	the icon height
 *
 ** @exception TrayIconException - if something goes wrong :O(
 *	- Error loading icon image
 ** @exception InterruptedException - if the thread loading the image is interrupted
 */
	public void setImage(Image image, int w, int h) throws TrayIconException, InterruptedException {
		try {
			// Collect pixel data in array
			int[] pixels = new int[w * h];
			PixelGrabber pg = new PixelGrabber(image, 0, 0, w, h, pixels, 0, w);
			pg.grabPixels();
			if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
				freeIcon();
				throw new TrayIconException("Error loading icon image");
			}
			// Send image data to the native library
			setIconData(m_ID, w, h, pixels);
		} catch (InterruptedException ex) {
			freeIcon();
			throw ex;
		} catch (NullPointerException ex) {
			freeIcon();
			throw ex;
		}
	}

/**
 * Show/Hide this icon in the Windows System Tray
 *
 ** @param status true = show, false = hide
 */
	public void setVisible(boolean status) {
		showIcon(m_ID, status);
	}

/**
 * Test if this icon is currently visible in the Windows System Tray
 *
 * Returns true if visible
 */
	public boolean isVisible() {
		return testVisible(m_ID) == 1;
	}

/**
 * Changes the text for the ToolTip of this icon
 * The ToolTip is displayed when the user mouses over the icon
 *
 ** @param tip = the new text for the ToolTip
 */
	public void setToolTipText(String tip) {
		setToolTip(m_ID, tip);
	}

/**
 * Display a balloon message for the icon
 */
	public final static int BALLOON_NONE = 0;
	public final static int BALLOON_INFO = 1;
	public final static int BALLOON_WARNING = 2;
	public final static int BALLOON_ERROR = 3;
	public final static int BALLOON_NOSOUND = 0x10;

	public void showBalloon(String msg, String title, int timeout, int flags) throws TrayIconException {
		if (showBalloon(m_ID, msg, title, timeout, flags) == 0) {
			throw new TrayIconException("Error showing Balloon message");
		}
	}

/**
 * Add an ActionLister to this icon
 * Just like with java.awt.Button or javax.swing.JButton
 *
 ** @param listener = your listener
 */
	public void addActionListener(ActionListener listener) {
		if (m_ActList == null) {
			m_ActList = new Vector();
			clickEnable(this, m_ID, true);
		}
		m_ActList.addElement(listener);
	}

	public void removeActionListener(ActionListener listener) {
		m_ActList.removeElement(listener);
	}

	public void addMouseListener(MouseListener listener) {
		if (m_MouseList == null) {
			m_MouseList = new Vector();
			clickEnable(this, m_ID, true);
		}
		m_MouseList.addElement(listener);
	}

	public void removeMouseListener(MouseListener listener) {
		m_MouseList.removeElement(listener);
	}

	public void addBalloonListener(TrayBalloonListener listener) {
		if (m_BalloonList == null) {
			m_BalloonList = new Vector();
			clickEnable(this, m_ID, true);
		}
		m_BalloonList.addElement(listener);
	}

	public void removeBalloonListener(TrayBalloonListener listener) {
		m_BalloonList.removeElement(listener);
	}

/**
 * Set new popup menu
 * The popup menu is displayed when the user right clicks the icon
 * See class TrayIconPopup, TrayIconPopupSimpleItem, ..
 *
 ** @param popup = the popup menu
 */
	public void setPopup(TrayIconPopup popup) {
		if (popup == null) {
			m_Popup = null;
			initPopup(m_ID, -1);
		} else {
			if (m_Popup == null) clickEnable(this, m_ID, true);
			int levels = popup.getNbLevels();
			initPopup(m_ID, levels);
			popup.setTrayIcon(this, m_ID, -1);
			m_Popup = popup;
		}
	}

/**
 * Free all native resources for this icon
 * On exit use cleanUp()
 */
	public void freeIcon() {
		clickEnable(this, m_ID, false);
		freeIcon(m_ID);
	}

	public static native void setAlwaysOnTop(Component wnd, boolean onTop);

public final static int UNICODE_CONV_BALLOON = 2;
public final static int UNICODE_CONV_SUPPORT = 1;

	public static native void enableUnicodeConversion(int component, boolean enable);

	public static native boolean hasUnicodeConversion(int component);

	public static native boolean supportsBalloonMessages();

/**
 * Return error code from native library - use for debugging
 */
	public static native int getLastError();

// No error occured since the last call to getLastError()
// There are a lot errors declared but they are only there for debug reasons
	public static final int NOERR = 0;

// The ActionListeners of the icon need to be notified when the user clicks it
// In the Windows API this is accomplished using a Notify Procedure
 	public static final int NOTIFYPROCERR = -1;

// The DLL has a fixed data structure that can contain up to 100 icons
// Hope that's enough for you
	public static final int TOOMANYICONS = -2;

// This happens when C++ is out of memory
	public static final int NOTENOUGHMEM = -3;

// Each icon has one unique id number
	public static final int WRONGICONID = -4;

// The native code can't locate the DLL
// Try moving it to C:/WINDOWS/SYSTEM or something like that
	public static final int DLLNOTFOUND = -5;

// Invocation code can't find your Java VM during callback
	public static final int NOVMS = -6;

// Invocation API can't attach native thread to your Java VM
	public static final int ERRTHREAD = -7;

// Error in lookup of the notifyListeners() method in this class
// The DLL has to do this when the user clicks one of your icons
	public static final int METHODID = -8;

// Not really an error..
// This happens when the user clicks an icon that has no ActionListener yet
	public static final int NOLISTENER = -9;

// One of the Invocation JNI Functions returned an error
	public static final int JNIERR = -10;

// Error showing balloon
	public static final int ERRORBALLOON = -18;

/****************************************************************************************************************
 *                                                                                                              *
 * Windows messaging code for detecting previous instance                                                       *
 *                                                                                                              *
 ****************************************************************************************************************/

/**
 * Checks if there's an instance with hidden window title = appName running.
 * Can be used to detect that another instance of your app is already running (so exit..)
 *
 * @param appName	the title of the hidden window to search for
 */
	public static native boolean isRunning(String appName);

/**
 * Send a message to another app (message can contain an integer).
 * Can be used to detect that another instance of your app is already running.
 * That instance can for example restore it's window after it receives the windows
 * message - see demo app for more info
 *
 * @param appName	the title of the hidden window to search for
 * @param message	the integer message to send (only native int size supported)
 */
	public static native int sendWindowsMessage(String appName, int message);

/**
 * Set callback method for receiving windows messages.
 * See sendWindowsMessage() for more information or take a look at the demo app
 *
 * @param callback	the callback method for this app
 */
	public static void setWindowsMessageCallback(TrayIconCallback callback) {
		m_WMessageCallback = callback;
	}

/**
 * Keep TrayIcon alive (make sure application does not exit)
 */
	public static void keepAlive() {
		if (m_Keeper == null) {
			m_Keeper = new TrayIconKeeper();
			m_Keeper.start();
		}
	}

	public final static int FLASHW_STOP = 0;
	public final static int FLASHW_CAPTION = 1;
	public final static int FLASHW_TRAY = 2;
	public final static int FLASHW_ALL = FLASHW_CAPTION | FLASHW_TRAY;
	public final static int FLASHW_TIMER = 4;
	public final static int FLASHW_TIMERNOFG = 12;

	public static void flashWindow(Frame wnd) throws TrayIconException {
		flashWindow(wnd, FLASHW_ALL | FLASHW_TIMERNOFG, 0, 0);
	}

	public static void flashWindow(Frame wnd, int flags, int count, int timeout) throws TrayIconException {
		flashWindow(wnd.getTitle(), flags, count, timeout);
	}

	public static void flashWindow(String title, int flags, int count, int timeout) throws TrayIconException {
		if (!flashWindowImpl(title, flags, count, timeout)) {
			throw new TrayIconException("Flash window not supported");
		}
	}

	public static native boolean flashWindowImpl(String title, int flags, int count, int timeout);

	public static void setCurrentWindow(Window wnd) {
		m_CurrentWindow = wnd;
	}

	public static native String getWindowsVersionString();

	public static native int getWindowsVersion();

	public final static int WIN_VER_UNKNOWN = 0;
	public final static int WIN_VER_WIN32 = 1;
	public final static int WIN_VER_95 = 2;
	public final static int WIN_VER_98 = 3;
	public final static int WIN_VER_ME = 4;
	public final static int WIN_VER_NT = 5;
	public final static int WIN_VER_2K = 6;
	public final static int WIN_VER_XP = 7;
	public final static int WIN_VER_NET = 8;

	public static boolean supportsBallonInfo() {
		int version = getWindowsVersion();
		return version >= WIN_VER_2K;
	}

/****************************************************************************************************************
 *                                                                                                              *
 * Next section is for inter use only -- or for hackers :O)                                                     *
 *                                                                                                              *
 ****************************************************************************************************************/

// Constructor
	private WindowsTrayIcon() {
	}

// Each icon has a unique id ranging from 0..99
	private int m_ID;
// Each icon can have a popup menu - activated when user right clicks the icon
	private TrayIconPopup m_Popup;
// Each icon can have any number of ActionListeners - notified when user clicks (left/right) the icon
	private Vector m_ActList, m_MouseList, m_BalloonList;
// Each application can have one WindowsMessageCallback - notified when another app uses sendWindowsMessage
	private static TrayIconCallback m_WMessageCallback;

	private final static int MOUSE_BTN_UP = 1;
	private final static int MOUSE_BTN_DOUBLE = 2;

	public static TrayDummyComponent getDummyComponent() {
		if (m_Dummy == null) m_Dummy = new TrayDummyComponent();
		return m_Dummy;
	}

/**
 * Private method called by native library when user clicks mouse button
 *
 ** @param button = "Left" or "Right" or "Middle"
 */
	private void notifyMouseListeners(int button, int mask, int xp, int yp) {
		int clicks = (mask & MOUSE_BTN_DOUBLE) != 0 ? 2 : 1;
		boolean up = (mask & MOUSE_BTN_UP) != 0;
		if (m_ActList != null && clicks == 1 && up == false) {
			ActionEvent evt = null;
			if (button == 0) evt = new ActionEvent(this,0,"Left");
			else if (button == 1) evt = new ActionEvent(this,0,"Right");
			else evt = new ActionEvent(this,0,"Middle");
			for (Enumeration elements = m_ActList.elements(); elements.hasMoreElements(); ) {
				ActionListener listener = (ActionListener)elements.nextElement();
				listener.actionPerformed(evt);
			}
		}
		if (m_MouseList != null) {
			int modifiers = 0;
			if (button == 0) modifiers |= MouseEvent.BUTTON1_MASK;
			else if (button == 1) modifiers |= MouseEvent.BUTTON2_MASK;
			else modifiers |= MouseEvent.BUTTON3_MASK;
			// (Component source, int id, long when, int modifiers, int x, int y, int clickCount, boolean popupTrigger)
			MouseEvent evt = new MouseEvent(getDummyComponent(), 0, 0, modifiers, xp, yp, clicks, button == 1);
			for (Enumeration elements = m_MouseList.elements(); elements.hasMoreElements(); ) {
				MouseListener listener = (MouseListener)elements.nextElement();
				if (up) listener.mouseReleased(evt);
				else listener.mousePressed(evt);
			}
		}
	}

/**
 * Private method called by native library when something happens with the balloon message
 *
 */
	private void notifyBalloonListeners(int mask) {
		if (m_BalloonList != null) {
			TrayBalloonEvent evt = new TrayBalloonEvent(mask);
			for (Enumeration elements = m_BalloonList.elements(); elements.hasMoreElements(); ) {
				TrayBalloonListener listener = (TrayBalloonListener)elements.nextElement();
				listener.balloonChanged(evt);
			}
		}
	}

/**
 * Private method called by native library when user selects popup menu item
 *
 ** @param id = id of menu item (each menu item has unique id)
 */
	private void notifyMenuListeners(int id) {
		if (m_Popup != null) m_Popup.onSelected(id);
	}

/**
 * Private method called by native library when it receives a sendWindowsMessage event
 * See sendWindowsMessage() for more information or take a look at the demo app
 *
 ** @param lParam =* @parameter send along with windows message
 */
	private static int callWindowsMessage(int lParam) {
		if (m_WMessageCallback != null) return m_WMessageCallback.callback(lParam);
		else return 0;
	}

	private static void callMouseHook(int xp, int yp) {
		if (m_MouseHook != null) {
			MouseEvent evt = new MouseEvent(getDummyComponent(), 0, 0, 0, xp, yp, 1, true);
			m_MouseHook.mousePressed(evt);
		}
	}

/**
 * Modify property of menu item
 *
 ** @param menuId = the id of the menu item
 ** @param what = which property to modify
 ** @param state = true property enabled
 */
	void modifyPopup(int menuId, int what, boolean state) {
		modifyPopup(m_ID, menuId, what, state);
	}

/**
 * Init new popup menu - used by setPopup()
 *
 ** @param id = the icon's id
 ** @param nblevels = the submenu depth of the new popup
 */
	static native void initPopup(int id, int nblevels);

// Constants for builing a popup menu
// Used by subclasses of TrayIconPopupItem
	final static int POPUP_TYPE_ITEM        = 0;	// Simple item
	final static int POPUP_TYPE_SEPARATOR   = 1;	// Separator
	final static int POPUP_TYPE_CHECKBOX    = 2;	// Checkbox item
	final static int POPUP_TYPE_INIT_LEVEL  = 3;	// First item of submenu
	final static int POPUP_TYPE_DONE_LEVEL  = 4;	// Last item of submenu

// Enable/Disable and friends
	final static int POPUP_MODE_ENABLE      = 1;
	final static int POPUP_MODE_CHECK       = 2;
	final static int POPUP_MODE_DEFAULT     = 4;

/**
 * Add popup menu item - used by setTrayIcon() in subclasses of TrayIconPopupItem
 *
 ** @param id = the icon's id
 ** @param level = the submenu level
 ** @param name = the name of the menu item
 ** @param type = POPUP_TYPE_ITEM or POPUP_TYPE_SEPARATOR or..
 */
	static native int subPopup(int id, int level, String name, int type, int extra);

/**
 * Modify menu item properties
 *
 ** @param id = the icon's id
 ** @param menuId = the id of the menu item
 ** @param what = property to modify
 ** @param state = on/off
 */
	private static native void modifyPopup(int id, int menuId, int what, boolean state);

/**
 * Allocate a new id for icon - used in constructor
 */
	private static native int getFreeId();

/**
 * Set bitmap data for icon - used in constructor and setImage()
 *
 ** @param id = the icon's id
 ** @param w, h = the images size
 ** @param pixels = the pixel array
 */
	private static native void setIconData(int id, int w, int h, int pixels[]);

/**
 * Make Tray Icon visible/invisible - used by setVisible()
 *
 ** @param id = the icon's id
 ** @param hide = visible/invisible?
 */
	private static native void showIcon(int id, boolean hide);

/**
 * Test if Tray Icon is in the system tray - used by isVisible()
 *
 ** @param id = the icon's id
 */
	private static native int testVisible(int id);

/**
 * Enable mouse/menu messages for icon - used by addActionListener() and setPopup()
 *
 ** @param ico = the icons class (this)
 ** @param id = the icon's id
 ** @param enable = enable/disable mouse events?
 */
	private static native void clickEnable(WindowsTrayIcon ico, int id, boolean enable);

/**
 * Set tooltip - used by setToolTip(String tip)
 *
 ** @param id = the icon's id
 ** @param tip = the new tooltip
 */
	private static native void setToolTip(int id, String tip);

/**
 * Free all native resources for this icon - used by freeIcon()
 *
 ** @param id = the icon's id
 */
	private static native void freeIcon(int id);

	private static native void detectAllClicks(int id);

	public static native void initJAWT();

	public static native void initHook();

	public static native void setMouseHookEnabled(int enable);

	public static void setMouseClickHook(MouseListener listener) {
		m_MouseHook = listener;
		setMouseHookEnabled(listener == null ? 0 : 1);
	}

	private static native void initTrayIcon(String appName, WindowsTrayIcon cls);

	private static native int showBalloon(int id, String msg, String title, int timeout, int flags);

	private static native void termTrayIcon();

	public static Window getCurrentWindow() {
		return m_CurrentWindow;
	}
}
