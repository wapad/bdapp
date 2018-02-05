package com.github.quadtriangle.buydatapack;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;


public class RobiSheba {
    // Robi Sheba API
    private final String BASE = "https://ecare-app.robi.com.bd";
    private final String LOGIN = "/airtel_sc/index.php?r=/accounts/login";
    private final String PACKAGES = "/airtel_sc/index.php?r=/data-packages/get-data-packages";
    private final String BUY_PACKAGE = "/airtel_sc/index.php?r=/data-packages/activate-data-package";
    private final String AUTO_LOGIN = "/airtel_sc/index.php?r=/auto-login/check";
    private final String AUTO_LOGIN_INFO = "http://appsuite.robi.com.bd/airtel_sc/getMsisdn.php";

    private RequestBody formBody;
    public String dataPlan;
    private static final RobiSheba instance = new RobiSheba();
    private SharedPreferences loginPrefs;
    SharedPreferences.Editor loginPrefsEd;
    private OkHttpClient client;

    public static RobiSheba getInstance() {
        return instance;
    }

    public String login(Context ctx, String number, String password) throws JSONException, IOException {
        if (number == null)
            return autoLogin(ctx);
        loginPrefsEd.putString("device_imsi", md5(number).substring(0, 16)).commit();
        formBuilder("login", number, password, null, null);
        String body = getRespBody(BASE + LOGIN);
        JSONObject loginRespJson = new JSONObject(body);
        String status = loginStatus(body, loginRespJson);
        saveLoginInfo(status, loginRespJson);
        return status;
    }

    private String autoLogin(Context ctx) throws JSONException, IOException {
        formBuilder("getLoginInfo", null, null, null, null);
        String body = getRespBody(AUTO_LOGIN_INFO);
        JSONObject respJson = new JSONObject(body);
        if (!respJson.getBoolean("success")) {
            return ctx.getString(R.string.auto_login_failed_msg);
        }
        String id = respJson.getString("id");
        String number = respJson.getString("msisdn");
        loginPrefsEd.putString("device_imsi", md5(number).substring(0, 16)).commit();
        formBuilder("autoLogin", number, null, id, null);
        body = getRespBody(BASE + AUTO_LOGIN);
        JSONObject loginRespJson = new JSONObject(body);
        String status = loginStatus(body, loginRespJson);
        saveLoginInfo(status, loginRespJson);
        return status;
    }

    public List<String> getPackages() throws JSONException, IOException {
        formBuilder("getPack", null, null, null, null);
        String body = getRespBody(BASE + PACKAGES);
        JSONObject packagesJson = new JSONObject(body);
        JSONArray data = packagesJson.getJSONArray("data");
        List<String> items = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            items.add(data.getJSONObject(i).getString("plan_id") + " - " +
                    data.getJSONObject(i).getString("tariff_with_vat"));
        }
        Collections.sort(items, (String s1, String s2) -> {
                    Double d1 = Double.parseDouble(s2.substring(s2.lastIndexOf("Tk. ") + 4));
                    Double d2 = Double.parseDouble(s1.substring(s1.lastIndexOf("Tk. ") + 4));
                    return d2.compareTo(d1);
                }
        );
        return items;
    }

    public String buyPack(Context ctx, String secret) throws JSONException, IOException {
        String formType = secret != null ? "buyReqSecret" : "buyReq";
        formBuilder(formType, null, null, null, secret);
        String body = getRespBody(BASE + BUY_PACKAGE);
        return buyStatus(ctx, body);
    }

    private String getRespBody(String URL) throws IOException {
        Request.Builder builder = new Request.Builder()
                .header("Connection", "keep-alive")
                .url(URL)
                .post(formBody);
        Request request = builder.build();
        Response response = client.newCall(request).execute();
        String body = response.body().string();
        Log.d("buydatapack", body);
        return body;
    }

    private void formBuilder(String formType, String number, String password, String id,
                             String secret) throws JSONException {
        FormBody.Builder builder = new FormBody.Builder()
                .add("app_type", "mobile_app")
                .add("network_type", "mobile")
                .add("device_imsi", loginPrefs.getString("device_imsi", ""));
        switch (formType) {
            case "login":
                builder.add("conn", number)
                        .add("password", password);
                break;
            case "autoLogin":
                builder.add("conn", number)
                        .add("id", id);
                break;
            case "getLoginInfo":
                break;
            case "buyReqSecret":
                builder.add("secret_code", secret);
            case "buyReq":
                builder.add("plan_id", dataPlan)
                        .add("name", dataPlan);
            case "getPack":
                builder.add("session_key", loginPrefs.getString("session_key", ""))
                        .add("ref_number", loginPrefs.getString("ref_number", ""))
                        .add("conn_type", loginPrefs.getString("conn_type", ""))
                        .add("operator", loginPrefs.getString("operator", ""))
                        .add("conn", loginPrefs.getString("conn", ""));
                break;
            default:
                break;
        }
        formBody = builder.build();
    }

    private String loginStatus(String body, JSONObject respJson) {
        if (respJson.has("sessionKey")) {
            return "success";
        } else if (body.contains("invalid_user_credentials")) {
            return "invalid";
        } else if (body.contains("err_unknown")) {
            return "down";
        } else if (body.contains("maintenance")) {
            return "maintenance";
        } else {
            return body;
        }
    }

    private String buyStatus(Context ctx, String body) {
        if (body.contains("pin_sent")) {
            return ctx.getString(R.string.secret_sent);
        } else if (body.contains("Invalid Secret")) {
            return ctx.getString(R.string.wrong_secret);
        } else if (body.contains("\"success\":true")) {
            return ctx.getString(R.string.status_success);
        } else {
            return body;
        }
    }

    // https://stackoverflow.com/a/39420545
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            BigInteger md5Data = new BigInteger(1, md.digest(input.getBytes()));
            return String.format("%032x", md5Data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "0123456789abcdef";
        }
    }

    private void saveLoginInfo(String status, JSONObject loginRespJson) throws JSONException {
        if (status.equals("success")) {
            loginPrefsEd.putBoolean("isLoggedIn", true);
            loginPrefsEd.putString("session_key", loginRespJson.getString("sessionKey"));
            loginPrefsEd.putString("ref_number", loginRespJson.getJSONObject("user").getString("mobile"));
            loginPrefsEd.putString("conn_type", loginRespJson.getJSONArray("connections")
                    .getJSONObject(0).getString("conn_type"));
            loginPrefsEd.putString("operator", loginRespJson.getJSONArray("connections")
                    .getJSONObject(0).getString("operator"));
            loginPrefsEd.putString("conn", loginRespJson.getJSONArray("connections")
                    .getJSONObject(0).getString("conn"));
        }
        loginPrefsEd.apply();
    }

    public void setupClient(Context ctx) {
        loginPrefs = ctx.getSharedPreferences("loginPrefs", MODE_PRIVATE);
        loginPrefsEd = loginPrefs.edit();
        client = new OkHttpClient.Builder()
                .cookieJar(new PersistentCookieJar(
                        new SetCookieCache(), new SharedPrefsCookiePersistor(ctx)))
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}
