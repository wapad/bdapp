package com.github.quadtriangle.buydatapack;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private SharedPreferences loginPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        Common.setAppTheme(this);
        setContentView(R.layout.activity_main);
        Common.setupToolbar(this, false);
        listener = Common.registerPrefsChangeListener(this);
        loginPrefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        checkLoggedIn();
        setupDrawer();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(context, SettingsActivity.class));
                return true;
            case R.id.action_update:
                Toast.makeText(context, R.string.update_msg, Toast.LENGTH_LONG).show();
                checkUpdate(Display.DIALOG, true);
                return true;
            case R.id.action_about:
                showAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Common.setAppLocale(base));
    }

    @Override
    protected void onStart() {
        checkUpdateStartup();
        super.onStart();
    }

    private void checkLoggedIn() {
        if (!loginPrefs.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(context, LoginActivity.class));
        }
    }

    private void checkUpdate(Display mode, boolean showUpdated) {
        new AppUpdater(context)
                .setUpdateFrom(UpdateFrom.JSON)
                .setUpdateJSON("https://raw.githubusercontent.com/quadtriangle/buydatapack/master/app/update-changelog.json")
                .setDisplay(mode)
                .showAppUpdated(showUpdated)
                .setButtonDoNotShowAgain(null)
                .setCancelable(false)
                .start();
    }

    private void checkUpdateStartup() {
        boolean isAutoUpdate = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("auto_update", true);
        if (isAutoUpdate) {
            checkUpdate(Display.NOTIFICATION, false);
        }
    }

    private void showAbout() {
        new LibsBuilder()
                .withLibraries("aboutlibraries", "materialdialogs", "appupdater", "support_cardview",
                        "constraint_layout", "materialprogressbar", "okhttp", "okio", "design",
                        "support_v4", "appcompat_v7", "recyclerview_v7", "support_annotations",
                        "fastadapter", "androidiconics", "smsverifycatcher")
                .withAutoDetect(false)
                .withLicenseShown(true)
                .withActivityTheme(Common.getAppTheme(context))
                .withActivityTitle(getString(R.string.about))
                .start(context);
    }

    private void setupDrawer() {

        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withTextColorRes(R.color.colorAccent)
                .addProfiles(
                        new ProfileDrawerItem().withName("Airtel").withEmail(loginPrefs.getString("conn", "gi")).withIcon(R.drawable.ic_user).withIdentifier(1),
                        new ProfileSettingDrawerItem().withName("Logout").withDescription("Logout from this app").withIcon(R.drawable.ic_logout).withIdentifier(2)
                )
                .withOnAccountHeaderListener((view, profile, current) -> {
                    if (profile.getIdentifier() == 2) {
                        loginPrefs.edit().clear().apply();
                        recreate();
                    }
                    return false;
                })
                .build();

        new DrawerBuilder()
                .withActivity(this)
                .withToolbar(findViewById(R.id.toolbar))
                .withAccountHeader(headerResult)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Home").withIcon(R.drawable.ic_home).withIdentifier(1).withSelectable(false).withIdentifier(1),
                        new SectionDrawerItem().withName("Menu Items"),
                        new SecondaryDrawerItem().withName(R.string.about).withIcon(R.drawable.ic_about).withIdentifier(2),
                        new SecondaryDrawerItem().withName(R.string.update).withIcon(R.drawable.ic_update).withIdentifier(3),
                        new SecondaryDrawerItem().withName(R.string.settings).withIcon(R.drawable.ic_setting).withIdentifier(4)
                )
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    if (drawerItem.getIdentifier() == 1) {
                        // we're already in home
                        return false;
                    } else if (drawerItem.getIdentifier() == 2) {
                        showAbout();
                    } else if (drawerItem.getIdentifier() == 3) {
                        Toast.makeText(context, R.string.update_msg, Toast.LENGTH_LONG).show();
                        checkUpdate(Display.DIALOG, true);
                    } else if (drawerItem.getIdentifier() == 4) {
                        startActivity(new Intent(context, SettingsActivity.class));
                    }
                    return false;
                })
                .withShowDrawerOnFirstLaunch(true)
                .build();
    }

    public void onBuyDataPackBtn(View view) {
        startActivity(new Intent(context, BuyPackageActivity.class));
    }
}
