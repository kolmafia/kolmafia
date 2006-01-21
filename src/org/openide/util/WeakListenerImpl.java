/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * THIS CODE HAS BEEN MODIFIED TO REMOVE ALL REFERENCES TO THE
 * ErrorManager CLASS IN THE org.openide PACKAGE.  THIS IS TO
 * MINIMIZE THE NUMBER OF IMPORTS REQUIRED IN ORDER TO MAKE USE
 * OF THIS MODULE.
 */

package org.openide.util;

import java.beans.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.EventListener;
import java.util.EventObject;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

import javax.swing.event.*;

// import org.openide.ErrorManager;

/**
 * A listener wrapper that delegates to another listener but hold
 * only weak reference to it, so it does not prevent it to be finalized.
 *
 * @author Jaroslav Tulach
 */
abstract class WeakListenerImpl implements java.util.EventListener {
    /** weak reference to listener */
    private ListenerReference ref;
    /** class of the listener */
    Class listenerClass;
    /** weak reference to source */
    private Reference source;

    /**
     * @param listenerClass class/interface of the listener
     * @param l listener to delegate to, l must be an instance of
     * listenerClass
     */
    protected WeakListenerImpl(Class listenerClass, java.util.EventListener l) {
        this.listenerClass = listenerClass;
        ref = new ListenerReference (l, this);
    }

    /** Setter for the source field. If a WeakReference to an underlying listener is
     * cleared and enqueued, that is, the original listener is garbage collected,
     * then the source field is used for deregistration of this WeakListenerImpl, thus making
     * it eligible for garbage collection if no more references exist.
     *
     * This method is particularly useful in cases where the underlying listener was
     * garbage collected and the event source, on which this listener is listening on,
     * is quiet, i.e. does not fire any events for long periods. In this case, this listener
     * is not removed from the event source until an event is fired. If the source field is
     * set however, WeakListenerImpls that lost their underlying listeners are removed
     * as soon as the ReferenceQueue notifies the WeakListenerImpl.
     *
     * @param source is any Object or null, though only setting an object
     * that has an appropriate remove*listenerClass*Listener method and on which this listener is listening on,
     * is useful.
     */
    protected final void setSource (Object source) {
        if (source == null) {
            this.source = null;
        } else {
            this.source = new WeakReference (source);
        }
    }

    /** Method name to use for removing the listener.
    * @return name of method of the source object that should be used
    *   to remove the listener from listening on source of events
    */
    protected abstract String removeMethodName ();

    /** Getter for the target listener.
    * @param ev the event the we want to distribute
    * @return null if there is no listener because it has been finalized
    */
    protected final java.util.EventListener get (java.util.EventObject ev) {
        Object l = ref.get (); // get the consumer

	// if the event consumer is gone, unregister us from the event producer
	if (l == null) {
            ref.requestCleanUp (ev == null ? null : ev.getSource ());
        }

        return (EventListener)l;
    }

    Object getImplementator() {
        return this;
    }

    public String toString () {
        Object listener = ref.get();
        return getClass().getName() + "[" + (listener == null ? "null" : listener.getClass().getName() + "]");
    }
    /** Weak property change listener
    * @deprecated use appropriate method instead
    */
    static class PropertyChange extends WeakListenerImpl
        implements PropertyChangeListener {
        /** Constructor.
        * @param l listener to delegate to
        */
        public PropertyChange (PropertyChangeListener l) {
            super (PropertyChangeListener.class, l);
        }

        /** Constructor.
        * @param clazz required class
        * @param l listener to delegate to
        */
        PropertyChange (Class clazz, PropertyChangeListener l) {
            super (clazz, l);
        }

        /** Tests if the object we reference to still exists and
        * if so, delegate to it. Otherwise remove from the source
        * if it has removePropertyChangeListener method.
        */
        public void propertyChange (PropertyChangeEvent ev) {
            PropertyChangeListener l = (PropertyChangeListener)super.get (ev);
            if (l != null) l.propertyChange (ev);
        }

        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removePropertyChangeListener"; // NOI18N
        }
    }

    /** Weak vetoable change listener
    * @deprecated use appropriate method instead
    */
    static class VetoableChange extends WeakListenerImpl
        implements VetoableChangeListener {
        /** Constructor.
        * @param l listener to delegate to
        */
        public VetoableChange (VetoableChangeListener l) {
            super (VetoableChangeListener.class, l);
        }

        /** Tests if the object we reference to still exists and
        * if so, delegate to it. Otherwise remove from the source
        * if it has removePropertyChangeListener method.
        */
        public void vetoableChange (PropertyChangeEvent ev) throws PropertyVetoException {
            VetoableChangeListener l = (VetoableChangeListener)super.get (ev);
            if (l != null) l.vetoableChange (ev);
        }

        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removeVetoableChangeListener"; // NOI18N
        }
    }

    /** Weak document modifications listener.
    * This class if final only for performance reasons,
    * can be happily unfinaled if desired.
    * @deprecated use appropriate method instead
    */
    static final class Document extends WeakListenerImpl
        implements DocumentListener {
        /** Constructor.
        * @param l listener to delegate to
        */
        public Document (final DocumentListener l) {
            super (DocumentListener.class, l);
        }

        /** Gives notification that an attribute or set of attributes changed.
        * @param ev event describing the action
        */
        public void changedUpdate(DocumentEvent ev) {
            final DocumentListener l = docGet(ev);
            if (l != null) l.changedUpdate(ev);
        }

        /** Gives notification that there was an insert into the document.
        * @param ev event describing the action
        */
        public void insertUpdate(DocumentEvent ev) {
            final DocumentListener l = docGet(ev);
            if (l != null) l.insertUpdate(ev);
        }

        /** Gives notification that a portion of the document has been removed.
        * @param ev event describing the action
        */
        public void removeUpdate(DocumentEvent ev) {
            final DocumentListener l = docGet(ev);
            if (l != null) l.removeUpdate(ev);
        }

        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removeDocumentListener"; // NOI18N
        }

        /** Getter for the target listener.
        * @param event the event the we want to distribute
        * @return null if there is no listener because it has been finalized
        */
        private DocumentListener docGet (DocumentEvent ev) {
            DocumentListener l = (DocumentListener)super.ref.get ();
            if (l == null) {
                super.ref.requestCleanUp (ev.getDocument());
            }
            return l;
        }
    } // end of Document inner class

    /** Weak swing change listener.
    * This class if final only for performance reasons,
    * can be happily unfinaled if desired.
    * @deprecated use appropriate method instead
    */
    static final class Change extends WeakListenerImpl
        implements ChangeListener {
        /** Constructor.
        * @param l listener to delegate to
        */
        public Change (ChangeListener l) {
            super (ChangeListener.class, l);
        }

        /** Called when new file system is added to the pool.
        * @param ev event describing the action
        */
        public void stateChanged (final ChangeEvent ev) {
            ChangeListener l = (ChangeListener)super.get(ev);
            if (l != null) l.stateChanged (ev);
        }

        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removeChangeListener"; // NOI18N
        }

    }

    public static EventListener create (Class lType, Class apiType, EventListener l, Object source) {
        ProxyListener pl = new ProxyListener (lType, apiType, l);
        pl.setSource (source);
        return (EventListener)pl.proxy;
    }


    /** Weak version of focus listener.
    * This class if final only for performance reasons,
    * can be happily unfinaled if desired.
    * @deprecated use appropriate method instead
    */
    static final class Focus extends WeakListenerImpl
        implements FocusListener {
        /** Constructor.
        * @param l listener to delegate to
        */
        public Focus (FocusListener l) {
            super (FocusListener.class, l);
        }

        /** Delegates to the original listener.
        */
        public void focusGained(FocusEvent ev) {
            FocusListener l = (FocusListener)super.get (ev);
            if (l != null) l.focusGained (ev);
        }

        /** Delegates to the original listener.
        */
        public void focusLost(FocusEvent ev) {
            FocusListener l = (FocusListener)super.get (ev);
            if (l != null) l.focusLost (ev);
        }

        /** Method name to use for removing the listener.
        * @return name of method of the source object that should be used
        *   to remove the listener from listening on source of events
        */
        protected String removeMethodName () {
            return "removeFocusListener"; // NOI18N
        }

    }


        /** Proxy interface that delegates to listeners.
    */
    private static class ProxyListener extends WeakListenerImpl implements InvocationHandler {
        /** proxy generated for this listener */
        public final Object proxy;

        /** Equals method */
        private static Method equalsMth;

        /** */
        private static Method getEquals() {
            if (equalsMth == null) {
                try {
                    equalsMth = Object.class.getMethod("equals", new Class[] { Object.class }); // NOI18N
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
            return equalsMth;
        }

        /** Class -> Reference(Constructor) */
        private static final java.util.WeakHashMap constructors = new java.util.WeakHashMap ();
        /** @param listener listener to delegate to
        */
        public ProxyListener (Class c, Class api, java.util.EventListener listener) {
            super (api, listener);
            try {
                Reference ref = (Reference)constructors.get (c);
                Constructor proxyConstructor = ref == null ? null : (Constructor)ref.get ();

                if (proxyConstructor == null) {
                    Class proxyClass = Proxy.getProxyClass (c.getClassLoader (), new Class[] { c });
                    proxyConstructor = proxyClass.getConstructor (new Class[] { InvocationHandler.class });
                    constructors.put (c, new SoftReference (proxyConstructor));
                }

                Object p;
                try {
                    p = proxyConstructor.newInstance(new Object[] { this });
                } catch (java.lang.NoClassDefFoundError err) {
                    // if for some reason the actual creation of the instance
                    // from constructor fails, try it once more using regular
                    // method, see issue 30449
                    p = Proxy.newProxyInstance (c.getClassLoader (), new Class[] { c }, this);
                }
                proxy = p;
            } catch (Exception ex) {
                IllegalStateException e = new IllegalStateException (ex.getMessage ());
//                ErrorManager.getDefault ().annotate (e, ex);
                throw e;
            }
        }

        public java.lang.Object invoke (
            Object proxy, Method method, Object[] args
        ) throws Throwable {
            if (method.getDeclaringClass () == Object.class) {
                // a method from object => call it on your self
                if (method == getEquals()) {
                    boolean ret = equals(args[0]);
                    return (ret ? Boolean.TRUE : Boolean.FALSE);
                }

                return method.invoke (this, args);
            }

            // listeners method
            EventObject ev =
                args != null && (args[0] instanceof EventObject) ? (EventObject)args[0] : null;

            Object listener = super.get (ev);
            if (listener != null) {
                return method.invoke (listener, args);
            } else {
                return null;
            }
        }

        /** Remove method name is composed from the name of the listener.
        */
        protected String removeMethodName () {
            String name = listenerClass.getName ();

            // strip package name
            int dot = name.lastIndexOf('.');
            name = name.substring (dot + 1);

            // in case of inner interfaces/classes we also strip the outer
            // class' name
            int i = name.lastIndexOf('$'); // NOI18N
            if (i >= 0) {
                name = name.substring(i + 1);
            }

            return "remove".concat(name); // NOI18N
        }

        /** To string prints class.
        */
        public String toString () {
            return super.toString () + "[" + listenerClass + "]"; // NOI18N
        }

        /** Equal is extended to equal also with proxy object.
        */
        public boolean equals (Object obj) {
            return proxy == obj || this == obj;
        }

        Object getImplementator() {
            return proxy;
        }
    }

    /** Reference that also holds ref to WeakListenerImpl.
    */
    private static final class ListenerReference extends WeakReference
    implements Runnable {
        private static Class lastClass;
        private static String lastMethodName;
        private static Method lastRemove;
        private static Object LOCK = new Object ();


        WeakListenerImpl weakListener;

        public ListenerReference (
            Object ref,
            WeakListenerImpl weakListener
        ) {
//            super (ref, Utilities.activeReferenceQueue());
super( ref );

            this.weakListener = weakListener;
        }

        /** Requestes cleanup of the listener with a provided source.
         * @param source source of the cleanup
         */
        public synchronized void requestCleanUp (Object source) {
            if (weakListener == null) {
                // already being handled
                return;
            }

            if (weakListener.source != source) {
                // plan new cleanup into the activeReferenceQueue with this listener and
                // provided source
                weakListener.source = new WeakReference (source) {
                    ListenerReference doNotGCRef = new ListenerReference (new Object (), weakListener);
                };
            }
        }

        public void run () {
            // prepare array for passing arguments to getMethod/invoke
            Object[] params = new Object[1];
            Class[] types = new Class[1];
            Object src = null; // On whom we're listening
            Method remove = null;

            WeakListenerImpl ref;
            synchronized (this) {
                ref = weakListener;
                if (ref.source == null || (src = ref.source.get()) == null) {
                    return;
                }
                // we are going to clean up the listener
                weakListener = null;
            }

            Class methodClass = src.getClass();
            String methodName = ref.removeMethodName();


            synchronized (LOCK) {
                if (lastClass == methodClass && lastMethodName == methodName && lastRemove != null) {
                    remove = lastRemove;
                }
            }

            // get the remove method or use the last one
            if (remove == null) {
                types[0] = ref.listenerClass;
                remove = getRemoveMethod(methodClass, methodName, types[0]);
                if (remove == null) {
//                    ErrorManager.getDefault().log (ErrorManager.WARNING,
//                        "Can't remove " + ref.listenerClass.getName() + //NOI18N
//                        " using method " + methodName + //NOI18N
//                        " from " + src); //NOI18N
                    return;
                } else {
                    synchronized (LOCK) {
                        lastClass = methodClass;
                        lastMethodName = methodName;
                        lastRemove = remove;
                    }
                }
            }

            params[0] = ref.getImplementator(); // Whom to unregister
            try {
                remove.invoke (src, params);
            } catch (Exception ex) { // from invoke(), should not happen
//                ErrorManager.getDefault().annotate(ex, "Problem encountered while calling " + methodClass + "." + methodName + "(...) on " + src); // NOI18N
//                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
            }
        }

        /* can return null */
        private Method getRemoveMethod(Class methodClass, String methodName, Class listenerClass) {
            final Class[] clarray = new Class[] { listenerClass };
            Method m = null;

            try {
                m = methodClass.getMethod(methodName, clarray);
            } catch (NoSuchMethodException e) {
                do {
                    try {
                        m = methodClass.getDeclaredMethod(methodName, clarray);
                    } catch (NoSuchMethodException ex) {
                    }
                    methodClass = methodClass.getSuperclass();
                } while ((m == null) && (methodClass != Object.class));
            }

            if (m != null && (!Modifier.isPublic(m.getModifiers()) ||
                              !Modifier.isPublic(m.getDeclaringClass().getModifiers()))) {
                m.setAccessible(true);
            }
            return m;
        }
    }
}