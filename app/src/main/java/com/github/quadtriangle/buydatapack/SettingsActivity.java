package com.github.quadtriangle.buydatapack;

import android.content.Context;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private OnSharedPreferenceChangeListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAppTheme();
        setContentView(R.layout.preference_activity_custom);
        registerPrefsChangeListener();
        setupToolber();

        if (getFragmentManager().findFragmentById(R.id.content_frame) == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new SettingsFragment())
                    .commit();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(setAppLocale(base));
    }

    private void registerPrefsChangeListener() {
        listener = (sharedPreferences, key) -> {
            if (key.equals("theme") || key.equals("language")) {
                recreate();
            }
        };

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(listener);
    }

    private void setupToolber() {
        Toolbar myChildToolbar =
                findViewById(R.id.my_settings_toolbar);
        setSupportActionBar(myChildToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setAppTheme() {
        String selectedTheme = PreferenceManager.getDefaultSharedPreferences(this).getString("theme", "Light");
        switch (selectedTheme) {
            case "Light":
                setTheme(R.style.LightTheme);
                break;
            case "Dark":
                setTheme(R.style.DarkTheme);
                break;
            default:
                setTheme(R.style.LightTheme);
                break;
        }
    }

    public Context setAppLocale(Context baseContext) {
        String lang = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("language", "en");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources res = baseContext.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        if (Build.VERSION.SDK_INT >= 17) {
            config.setLocale(locale);
            baseContext = baseContext.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
        }

        return baseContext;
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }


    }
}
