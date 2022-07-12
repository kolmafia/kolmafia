package net.sourceforge.kolmafia.swingui.panel;

import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import java.awt.*;

public class PreferenceWatcherPanel extends Box implements Listener {
    public PreferenceWatcherPanel() {
        super(BoxLayout.Y_AXIS);
        PreferenceListenerRegistry.registerPreferenceListener("watchedPreferences", this);
        this.update();
    }

    public void populate() {
        String preferencesString = Preferences.getString("watchedPreferences");
        String[] preferences = preferencesString.split(",");

        for (var preference : preferences) {
            this.add(new WatchedPreference(preference));
        }
    }

    @Override
    public void update() {
        this.removeAll();
        this.populate();
        this.revalidate();
        this.repaint();
    }

    public static class WatchedPreference extends Panel implements Listener {
        private final String preference;
        private final JLabel value;

        public WatchedPreference(final String preference) {
            super();
            this.setLayout(new GridLayout(1, 2));
            this.preference = preference;
            this.addListener(preference);

            this.add(new JLabel(preference));
            this.value = new JLabel();
            this.add(this.value);
            this.update();
            this.setMaximumSize( this.getPreferredSize() );
        }

        public void addListener(String preference) {
            PreferenceListenerRegistry.registerPreferenceListener(preference, this);
        }

        @Override
        public void update() {
            this.value.setText(Preferences.getString(this.preference));
        }
    }

}
