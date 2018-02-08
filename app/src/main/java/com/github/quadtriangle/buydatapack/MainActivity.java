package com.github.quadtriangle.buydatapack;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private View mainView;
    private View usageView;
    private WebView usageWv;

    private Context context;
    private RobiSheba robiSheba;

    private MaterialDialog dialog;
    private SharedPreferences loginPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        robiSheba = new RobiSheba(this);
        Common.setAppTheme(this);
        setContentView(R.layout.activity_main);
        setupView();
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

    private void setupView() {
        mainView = findViewById(R.id.main_view);
        usageView = findViewById(R.id.usage_view);
        usageWv = findViewById(R.id.usageWv);
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
                        new ProfileDrawerItem().withName(R.string.airtel).withEmail(loginPrefs.getString("conn", "")).withIcon(R.drawable.ic_user).withIdentifier(1),
                        new ProfileSettingDrawerItem().withName(R.string.logout).withDescription(R.string.logout_description).withIcon(R.drawable.ic_logout).withIdentifier(2)
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
                        new PrimaryDrawerItem().withName(R.string.home).withIcon(R.drawable.ic_home).withIdentifier(1).withSelectable(false).withIdentifier(1),
                        new SectionDrawerItem().withName(R.string.menu_items),
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

    public void onRewardsBtn(View view) {
        dialog = Common.showIndeterminateProgressDialog(context, R.string.offer, R.string.getting_rewards_msg);
        new GetBodyTask("rewards").execute((Void) null);
    }

    public void onDataBalanceBtn(View view) {
        dialog = Common.showIndeterminateProgressDialog(context, R.string.data_balance, R.string.get_data_balance_msg);
        new GetBodyTask("dataBalance").execute((Void) null);
    }

    public void onAccountBalanceBtn(View view) {
        dialog = Common.showIndeterminateProgressDialog(context, R.string.ac_balance, R.string.getting_ac_balance_msg);
        new GetBodyTask("acBalance").execute((Void) null);
    }

    public void onUsageHistoryBtn(View view) {
        mainView.setVisibility(View.GONE);
        usageView.setVisibility(View.VISIBLE);
        dialog = Common.showIndeterminateProgressDialog(context, R.string.usage_history, R.string.usage_history_msg);
        new GetBodyTask("usageInfo").execute((Void) null);
    }

    public void onUsageWvBackBtn(View view) {
        usageView.setVisibility(View.GONE);
        mainView.setVisibility(View.VISIBLE);
    }


    private class GetBodyTask extends AsyncTask<Void, Void, Boolean> {
        private JSONObject respJson;
        private String reqType;
        private String status;


        public GetBodyTask(String reqType) {
            this.reqType = reqType;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                switch (reqType) {
                    case "rewards":
                        respJson = robiSheba.getRewards();
                        break;
                    case "dataBalance":
                        respJson = robiSheba.getDataBalance();
                        break;
                    case "acBalance":
                        respJson = robiSheba.getAccountBalance();
                        break;
                    case "usageInfo":
                        respJson = robiSheba.getUsageHistory();
                        break;
                    default:
                        break;

                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                status = e.toString();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            dialog.dismiss();
            try {
                if (success && respJson.getBoolean("success")) {
                    switch (reqType) {
                        case "rewards":
                            showRewards();
                            break;
                        case "dataBalance":
                            showDataBalance();
                            break;
                        case "acBalance":
                            showAcBalance();
                            break;
                        case "usageInfo":
                            showUsageInfo();
                            break;
                        default:
                            break;

                    }
                }
            } catch (JSONException e) {
                status = e.toString();
                e.printStackTrace();
            }
            if (status != null) {
                new MaterialDialog.Builder(context)
                        .content(status)
                        .cancelable(false)
                        .positiveText(R.string.ok)
                        .show();
            }
        }

        private void showRewards() throws JSONException {
            JSONArray offers = respJson.getJSONArray("offers");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < offers.length(); i++) {
                builder.append(offers.getJSONObject(i).getString("message"));
                builder.append("<br/><br/>");
            }
            new MaterialDialog.Builder(context)
                    .title(R.string.offer)
                    .content(Html.fromHtml(builder.toString()))
                    .cancelable(false)
                    .positiveText(R.string.ok)
                    .show();
        }

        private void showDataBalance() throws JSONException {
            JSONArray data = respJson.getJSONArray("data");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < data.length(); i++) {
                builder.append(getString(R.string.pack_data_balance_msg1));
                builder.append(data.getJSONObject(i).getString("name"));
                builder.append(getString(R.string.remaining_data_balance_msg2));
                builder.append(data.getJSONObject(i).getString("remaining_volume"));
                builder.append(getString(R.string.exp_date_data_balance_msg3));
                builder.append(data.getJSONObject(i).getString("expiry_time"));
                builder.append("</b><br/>");
            }
            new MaterialDialog.Builder(context)
                    .title(R.string.data_balance)
                    .content(Html.fromHtml(builder.toString()))
                    .cancelable(false)
                    .positiveText(R.string.ok)
                    .show();
        }

        private void showAcBalance() throws JSONException {
            String balance = respJson.getString("balance");
            String lrdate = respJson.getString("lrdate");
            String lramunt = respJson.getString("lramunt");
            new MaterialDialog.Builder(context)
                    .title(R.string.ac_balance)
                    .content(R.string.show_ac_balance_msg, balance, lramunt, lrdate)
                    .cancelable(false)
                    .positiveText(R.string.ok)
                    .show();
        }

        private void showUsageInfo() throws JSONException {
            JSONArray data = respJson.getJSONArray("data");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < data.length(); i++) {
                if (data.getJSONObject(i).getString("amountChanged").equals("Tk. 0")) {
                    continue;
                }
                builder.append("<tr><td>");
                builder.append(data.getJSONObject(i).getString("eventType"));
                builder.append("</td><td>");
                builder.append(data.getJSONObject(i).getString("callDateTime"));
                builder.append("</td><td>");
                builder.append(data.getJSONObject(i).getString("mode"));
                builder.append("</td><td>");
                builder.append(data.getJSONObject(i).getString("description"));
                builder.append("</td><td>");
                builder.append(data.getJSONObject(i).getString("amountChanged"));
                builder.append("</td><td>");
                builder.append(data.getJSONObject(i).getString("volumeDuration"));
                builder.append("</td><td>");
                builder.append(data.getJSONObject(i).getString("afterCallBalance"));
                builder.append("</td></tr>");
            }
            usageWv.loadDataWithBaseURL(null, String.format(getString(R.string.usage_html), builder.toString()), "text/html; charset=utf-8", "UTF-8", null);
        }
    }
}
