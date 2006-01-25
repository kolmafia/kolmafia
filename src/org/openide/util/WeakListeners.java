/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * THIS CODE HAS BEEN MODIFIED TO REMOVE ALL REFERENCES TO DEPRECATED
 * STATIC INSTANCE METHODS IN THE WeakListenerImpl CLASS.
 */

package org.openide.util;

import java.awt.event.FocusListener;
import java.beans.PropertyChangeListener;
import java.beans.VetoableChangeListener;
import java.util.EventListener;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;


/**
 * <p>A generic weak listener factory.</p>
 *
 * <p>Creates a weak implementation of a listener of type lType.</p>
 *
 * <p>In the following examples, I'll use following naming:</p>
 *
 * <p>There are four objects involved in weak listener usage:</p>
 *
 *  <ul><li>The event source object
 *  <li>The observer - object that wants to listen on source
 *  <li>The listener - the implementation of the corresponding
 *     Listener interface, sometimes the observer itself but
 *     often some observer's inner class delegating the events to the observer.
 *  <li>The weak listener implementation.</ul>
 *
 * <p>The examples are written for ChangeListener. The Utilities
 * have factory methods for the most common listeners used in NetBeans
 * and also one universal factory method you can use for other listeners.</p>
 *
 * <p>How to use it:</p>
 *
 * <p>Here is an example how to write a listener/observer and make it listen
 * on some source:</p>
 *
 * <pre>public class ListenerObserver implements ChangeListener {
 *      private void registerTo(Source source) {
 *          source.addChangeListener( this, source );
 *      }
 *
 *      public void stateChanged(ChangeEvent e) {
 *          doSomething();
 *      }
 *  }</pre>
 *
 * <p>You can also factor out the listener implementation to some other class
 * if you don't want to expose the stateChanged method (better technique):</p>
 *
 * <pre>public class Observer {
 *      private Listener listener;
 *
 *      private void registerTo(Source source) {
 *          listener = new Listener();
 *          source.addChangeListener( listener, source );
 *      }
 *
 *      private class Listener implements ChangeListener {
 *          public void stateChanged(ChangeEvent e) {
 *              doSomething();
 *          }
 *      }
 *  }</pre>
 *
 * <p>Note: The observer keeps the reference to the listener, it won't work
 * otherwise, see below.</p>
 *
 * <p>You can also use the universal factory for other listeners:</p>
 *
 * <pre>public class Observer implements SomeListener {
 *      private void registerTo(Source source) {
 *          source.addSomeListener((SomeListener)(
 *                  SomeListener.class, this, source );
 *      }
 *
 *      public void someEventHappened(SomeEvent e) {
 *          doSomething();
 *      }
 *  }</pre>
 *
 *
 * <p>How to not use it:</p>
 *
 * <p>Here are examples of a common mistakes done when using weak listener:</p>
 *
 * <pre>public class Observer {
 *      private void registerTo(Source source) {
 *          source.addChangeListener(WeakListeners.change(new Listener(), source));
 *      }
 *
 *      private class Listener implements ChangeListener {
 *          public void stateChanged(ChangeEvent e) {
 *              doSomething();
 *          }
 *      }
 *  }</pre>
 *
 * <p>Mistake: There is nobody holding strong reference to the Listener instance,
 * so it may be freed on the next GC cycle.</p>
 *
 * <pre>public class ListenerObserver implements ChangeListener {
 *      private void registerTo(Source source) {
 *          source.addChangeListener(WeakListeners.change(this, null));
 *      }
 *
 *      public void stateChanged(ChangeEvent e) {
 *          doSomething();
 *      }
 *  }</pre>
 *
 * <p>Mistake: The weak listener is unable to unregister itself from the source
 * once the listener is freed. For explanation, read below.</p>
 *
 * <p>How does it work:</p>
 *
 * <p>The weak listener is used as a reference-weakening wrapper
 *  around the listener. It is itself strongly referenced from the implementation
 *  of the source (e.g. from its EventListenerList) but it references
 *  the listener only through WeakReference. It also weak-references
 *  the source. Listener, on the other hand, usually strongly references
 *  the observer (typically through the outer class reference).</p>
 *
 * <p>This means that:</p>
 *
 * <p>If the listener is not strong-referenced from elsewhere, it can be
 *  thrown away on the next GC cycle. This is why you can't use
 *  WeakListeners.change(new MyListener(), ..) as the only reference
 *  to the listener will be the weak one from the weak listener.</p>
 *
 * <p>If the listener-observer pair is not strong-referenced from elsewhere
 *  it can be thrown away on the next GC cycle. This is what the
 *  weak listener was invented for.</p>
 *
 * <p>If the source is not strong-referenced from anywhere, it can be
 *  thrown away on the next GC cycle taking the weak listener with it,
 *  but not the listener and the observer if they are still strong-referenced
 *  (unusual case, but possible).</p>
 *
 *
 * <p>Now what happens when the listener/observer is removed from memory:</p>
 *
 * <p>The weak listener is notified that the reference to the listener was cleared.</p>
 * <p>It tries to unregister itself from the source. This is why it needs
 *  the reference to the source for the registration. The unregistration
 *  is done using reflection, usually looking up the method
 *  remove<listenerType> of the source and calling it.</p>
 *
 *  <p>This may fail if the source don't have the expected remove
 *  method and/or if you provide wrong reference to source. In that case
 *  the weak listener instance will stay in memory and registered by the source,
 *  while the listener and observer will be freed.</p>
 *
 *  <p>There is still one fallback method - if some event come to a weak listener
 *  and the listener is already freed, the weak listener tries to unregister
 *  itself from the object the event came from.</p>
 *
 * @since 4.10
 */
public final class WeakListeners {
    /** No instances.
     */
    private WeakListeners () {
    }


    /** Generic factory method to create weak listener for any listener
     * interface.
     *
     * @param lType the type of listener to create. It can be any interface,
     *     but only interfaces are allowed.
     * @param l the listener to delegate to, l must be an instance
     *     of lType
     * @param source the source that the listener should detach from when
     *     listener l is freed, can be null
     * @return an instance of lType delegating all the interface
     * calls to l.
     */
    public static EventListener create (Class lType, EventListener l, Object source) {
        if (!lType.isInterface()) {
            throw new IllegalArgumentException ("Not interface: " + lType);
        }
        return WeakListenerImpl.create (lType, lType, l, source);
    }

    /**
     * <p>The most generic factory method to create weak listener for any listener
     * interface that moreover behaves like a listener of another type.
     * This can be useful to correctly remove listeners from a source when
     * hierarchies of listeners are used.</p>
     *
     * <p>For example {@link javax.naming.event.EventContext} allows to add an
     * instance of {@link javax.naming.event.ObjectChangeListener} but using
     * method addNamingListener. Method removeNamingListener
     * is then used to remove it. To help the weak listener support to correctly
     * find the right method one have to use:</p>
     *
     * <pre>ObjectChangeListener l = (ObjectChangeListener)WeakListeners.create (
     *   ObjectChangeListener.class, // the actual class of the returned listener
     *   NamingListener.class, // but it always will be used as NamingListener
     *   yourObjectListener,
     *   someContext
     * );
     * someContext.addNamingListener ("", 0, l);</pre>
     *
     * <p>This will correctly create ObjectChangeListener
     * and unregister it by calling removeNamingListener.</p>
     *
     * @param lType the type the listener shall implement. It can be any interface,
     *     but only interfaces are allowed.
     * @param apiType the interface the returned object will be used as. It
     *     shall be equal to lType or its superinterface
     * @param l the listener to delegate to, l must be an instance
     *     of lType
     * @param source the source that the listener should detach from when
     *     listener l is freed, can be null
     * @return an instance of lType delegating all the interface
     * calls to l.
     * @since 4.12
     */
    public static EventListener create (Class lType, Class apiType, EventListener l, Object source) {
        if (!lType.isInterface()) {
            throw new IllegalArgumentException ("Not interface: " + lType);
        }
        if (!apiType.isInterface()) {
            throw new IllegalArgumentException ("Not interface: " + apiType);
        }
        if (!apiType.isAssignableFrom(lType)) {
            throw new IllegalArgumentException (apiType + " has to be assignableFrom " + lType); // NOI18N
        }
        return WeakListenerImpl.create (lType, apiType, l, source);
    }
}

