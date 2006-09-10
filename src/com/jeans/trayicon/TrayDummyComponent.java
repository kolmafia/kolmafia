
package com.jeans.trayicon;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class TrayDummyComponent extends Window {
    
	public TrayDummyComponent() {
		super(new Frame("TrayDummy"));
	}

	public Point getLocationOnScreen() {
		return new Point(0,0);
	}	
}