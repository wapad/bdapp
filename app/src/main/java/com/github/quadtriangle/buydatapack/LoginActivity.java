package com.github.quadtriangle.buydatapack;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.stfalcon.smsverifycatcher.SmsVerifyCatcher;

import org.json.JSONException;

import java.io.IOException;

import cat.ereza.customactivityoncrash.config.CaocConfig;


public class LoginActivity extends AppCompatActivity {

    private EditText mNumberView;
    private EditText mPasswordView;
    private MaterialDialog dialog;
    private CheckBox saveLoginCheckBox;
    private View mainLayout;
    private View loginLayout;

    private SharedPreferences.Editor loginPrefsEditor;
    private OnSharedPreferenceChangeListener listener;
    private Context context;
    private LoginTask mAuthTask = null;
    private RobiSheba robiSheba = RobiSheba.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupCrashHandler();
        context = this;
        robiSheba.context = this;
        Common.setAppTheme(this);
        setContentView(R.layout.activity_login);
        Common.setupToolbar(this, R.id.my_toolbar, false);
        listener = Common.registerPrefsChangeListener(this);
        setupView();
        setupRememberMe();
        SmsVerifyCatcher.isStoragePermissionGranted(this, null);
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

    private void setupCrashHandler() {
        CaocConfig.Builder.create()
                .showRestartButton(false)
                .trackActivities(true)
                .apply();
    }

    private void setupView() {
        mainLayout = findViewById(R.id.main_layout);
        loginLayout = findViewById(R.id.login_layout);
        mNumberView = findViewById(R.id.number);
        mPasswordView = findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener((textView, id, event) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });
        saveLoginCheckBox = findViewById(R.id.remember_me);
        Button mSignInButton = findViewById(R.id.login_btn);
        mSignInButton.setOnClickListener(view -> attemptLogin());
    }

    public void onBackBtn(View view) {
        loginLayout.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);
    }

    public void onLoginWithPasswordBtn(View view) {
        mainLayout.setVisibility(View.GONE);
        loginLayout.setVisibility(View.VISIBLE);
    }

    public void onAutoLoginBtn(View view) {
        dialog = Common.showIndeterminateProgressDialog(context, R.string.login,
                R.string.trying_auto_login);
        mAuthTask = new LoginTask(null, null);
        mAuthTask.execute((Void) null);
    }


    private void setupRememberMe() {
        SharedPreferences loginPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        loginPrefsEditor = loginPreferences.edit();
        Boolean saveLogin = loginPreferences.getBoolean("saveLogin", true);
        if (saveLogin) {
            mNumberView.setText(loginPreferences.getString("username", ""));
            mPasswordView.setText(loginPreferences.getString("password", ""));
            saveLoginCheckBox.setChecked(true);
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

    private boolean isNumberValid(String number) {
        return number.startsWith("016") && number.length() == 11;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }


    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mNumberView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String number = mNumberView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid numbers.
        if (TextUtils.isEmpty(number)) {
            mNumberView.setError(getString(R.string.error_field_required));
            focusView = mNumberView;
            cancel = true;
        } else if (!isNumberValid(number)) {
            mNumberView.setError(getString(R.string.error_invalid_number));
            focusView = mNumberView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            try {
                assert inputManager != null;
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            } catch (NullPointerException e) {
                e.printStackTrace();

            }

            if (saveLoginCheckBox.isChecked()) {
                loginPrefsEditor.putBoolean("saveLogin", true);
                loginPrefsEditor.putString("username", number);
                loginPrefsEditor.putString("password", password);
                loginPrefsEditor.commit();
            } else {
                loginPrefsEditor.clear();
                loginPrefsEditor.commit();
            }
            dialog = Common.showIndeterminateProgressDialog(context, R.string.login, R.string.trying_login);
            mAuthTask = new LoginTask(number, password);
            mAuthTask.execute((Void) null);
        }
    }

    public class LoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mNumber;
        private final String mPassword;
        private String status;

        LoginTask(String number, String password) {
            mNumber = number;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                status = robiSheba.login(mNumber, mPassword);
            } catch (JSONException e) {
                e.printStackTrace();
                status = e.toString();
            } catch (IOException e) {
                e.printStackTrace();
                status = getString(R.string.connect_problem_msg);
            }
            return status.equals("success");
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            dialog.dismiss();
            if (success) {
                Toast.makeText(context, R.string.login_success, Toast.LENGTH_LONG).show();
                startActivity(new Intent(context, BuyPackageActivity.class));
            } else if (status.equals("invalid")) {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            } else {
                showStatus(status);
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            dialog.dismiss();
        }

        private void showStatus(String status) {
            final String dialogTitle = getString(R.string.login_failed);
            String message;
            switch (status) {
                case "maintenance":
                    message = getString(R.string.maintenance_msg);
                    break;
                case "down":
                    message = getString(R.string.down_msg);
                    break;
                default:
                    message = status;
                    break;
            }

            final String dialogMessage = message;
            new MaterialDialog.Builder(context)
                    .title(dialogTitle)
                    .content(dialogMessage)
                    .negativeText(R.string.ok)
                    .onNegative((dialog, which) -> finish())
                    .show();
        }
    }
}

