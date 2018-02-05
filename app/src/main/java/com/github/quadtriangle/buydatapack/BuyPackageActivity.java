package com.github.quadtriangle.buydatapack;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.stfalcon.smsverifycatcher.SmsVerifyCatcher;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.lang.Thread;


public class BuyPackageActivity extends AppCompatActivity {

    private TextView textView;
    private ProgressBar progressBar;
    private MaterialDialog dialog;
    private MaterialDialog buyPackDialog;

    private int packIndex;
    private BuyPackTask buyPackTask;
    private Activity context;
    private SmsVerifyCatcher smsVerifyCatcher;
    private RobiSheba robiSheba = RobiSheba.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        Common.setAppTheme(this);
        setContentView(R.layout.activity_buy_package);
        Common.setupToolbar(this, R.id.my_child_toolbar, true);
        setupView();
        dialog = Common.showIndeterminateProgressDialog(this, R.string.package_title, R.string.retrieving_pack);
        new SelectPackTask().execute((Void) null);
    }

    @Override
    public void onDestroy() {
        if (buyPackTask != null) {
            buyPackTask.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Common.setAppLocale(base));
    }


    private void setupView() {
        textView = findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        progressBar = findViewById(R.id.progressBar6);
    }

    private class SelectPackTask extends AsyncTask<Void, Void, Boolean> {
        int buyTimes;
        List<String> items;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                items = robiSheba.getPackages();
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            return items != null;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            dialog.dismiss();
            if (success) {
                selectPack();
            }
        }

        @Override
        protected void onCancelled() {

        }

        private void selectPack() {
            MaterialDialog slectPackDialog = new MaterialDialog.Builder(context)
                    .title(R.string.select_pack)
                    .items(items)
                    .cancelable(false)
                    .alwaysCallSingleChoiceCallback()
                    .itemsCallbackSingleChoice(0, (dialog, view, which, text) -> {
                        Toast.makeText(context, text.toString(), Toast.LENGTH_SHORT).show();
                        return true;
                    })
                    .negativeText(R.string.cancel)
                    .onNegative((dialog, which) -> finish())
                    .show();
            slectPackDialog.setActionButton(DialogAction.POSITIVE, R.string.ok);
            slectPackDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener((I) -> {
                packIndex = slectPackDialog.getSelectedIndex();
                robiSheba.dataPlan = items.get(packIndex).split("\\s-\\s")[0];
                slectPackDialog.dismiss();
                get_buy_times();
            });
        }

        private void get_buy_times() {
            final NumberPicker picker = new NumberPicker(context);
            picker.setMinValue(1);
            picker.setMaxValue(100);

            final FrameLayout layout = new FrameLayout(context);
            layout.addView(picker, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER));

            new MaterialDialog.Builder(context)
                    .customView(layout, false)
                    .title(R.string.buy_times)
                    .cancelable(false)
                    .positiveText(R.string.ok)
                    .onPositive((dialog, which) -> {
                        buyTimes = picker.getValue();
                        confirmBuy();
                    })
                    .negativeText(R.string.cancel)
                    .onNegative((dialog, which) -> finish())
                    .show();
        }

        private void confirmBuy() {
            String cost = items.get(packIndex).split("\\sTk.\\s")[1];
            String totalCost = String.format(Locale.ENGLISH, "%.2f", (Double.parseDouble(cost) * buyTimes));
            new MaterialDialog.Builder(context)
                    .title(R.string.confirm_pkg)
                    .content(R.string.comfirm_msg, robiSheba.dataPlan, buyTimes, totalCost)
                    .cancelable(false)
                    .positiveText(R.string.ok)
                    .onPositive((dialog, which) -> {
                        buyPackTask = new BuyPackTask(buyTimes);
                        buyPackTask.execute((Void) null);
                    })
                    .negativeText(R.string.cancel)
                    .onNegative((dialog, which) -> finish())
                    .show();
        }
    }


    public class BuyPackTask extends AsyncTask<Void, Void, Boolean> {
        final int buyTimes;
        String status;
        String secret;

        private BuyPackTask(int buy_times) {
            buyTimes = buy_times;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            startBuyPack();
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            Toast.makeText(context, R.string.success, Toast.LENGTH_LONG).show();
            buyPackDialog.setContent(R.string.done);
            buyPackDialog.setActionButton(DialogAction.NEGATIVE, R.string.exit);
            buyPackDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener((I) -> finish());
        }

        @Override
        protected void onCancelled() {
            Toast.makeText(context, R.string.buy_pack_cancelled, Toast.LENGTH_LONG).show();
            super.onCancelled();
        }

        private void smsListener() {
            smsVerifyCatcher = new SmsVerifyCatcher(context, message -> {
                Pattern pattern = Pattern.compile("(\\d{6})");
                Matcher matcher = pattern.matcher(message);
                if (matcher.find()) {
                    secret = matcher.group(1);
                }
                Toast.makeText(context, secret, Toast.LENGTH_SHORT).show();
                smsVerifyCatcher.onStop();
            });
            // smsVerifyCatcher.setPhoneNumberFilter("Robi Sheba");
            smsVerifyCatcher.setFilter(".*Robi\\se-Care.*\\d{6}\\s.*");
        }

        private void startBuyPack() {
            progressBar.setMax(buyTimes);
            showBuyPackDialog();
            smsListener();
            for (int i = 1; i <= buyTimes; i++) {
                if (isCancelled()) {
                    break;
                }
                textViewAppend(String.format(getString(R.string.trying_to_buy), i));
                smsVerifyCatcher.onStart();
                secret = null;
                try {
                    status = robiSheba.buyPack(null);
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                    status = e.toString();
                }
                textViewAppend(status);
                if (!status.equals(getString(R.string.secret_sent))) {
                    smsVerifyCatcher.onStop();
                    textViewAppend(getString(R.string.failed_to_buy_msg));
                    sleep(15_000);
                    continue;
                }
                textViewAppend(getString(R.string.waiting_for_sms));
                while (secret == null) {
                    sleep(500);
                }
                textViewAppend(getString(R.string.requesting_to_buy));
                try {
                    status = robiSheba.buyPack(secret);
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                    status = e.toString();
                }
                textViewAppend(status);
                if (!status.equals(getString(R.string.status_success))) {
                    textViewAppend(getString(R.string.failed_to_buy_msg));
                    sleep(15_000);
                    continue;
                }
                progressBar.setProgress(i);
                buyPackDialog.incrementProgress(1);
            }
        }

        private void textViewAppend(final String text) {
            runOnUiThread(() -> textView.append(text));
        }

        private void showBuyPackDialog() {
            runOnUiThread(() -> buyPackDialog = new MaterialDialog.Builder(context)
                    .title(R.string.buying_pack)
                    .content(R.string.please_wait)
                    .cancelable(false)
                    .progress(false, buyTimes, true)
                    .positiveText(R.string.hide)
                    .onPositive((dialog, which) -> buyPackDialog.dismiss())
                    .show());
        }

        private void sleep(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}


