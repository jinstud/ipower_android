package com.ipower.tattoo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;
import java.util.Random;

import cz.msebera.android.httpclient.Header;

public class Auth {

    final static SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);
    final static SharedPreferences.Editor editor = preferences.edit();

    interface AuthCallback {
        void onAuthFinished();
    }

    public static void sign_up(final Map<String, String> user, final AuthCallback authCallback, final Context context) {
        if (!user.get("email").equals(preferences.getString("setting_account_email", null))) {
            RequestParams params = new RequestParams();
            params.add("firstname", user.get("firstname"));
            params.add("lastname", user.get("lastname"));
            params.add("email", user.get("email"));
            params.add("language", Locale.getDefault().getLanguage());
            if (user.containsKey("password")) {
                params.add("password", user.get("password"));
            } else {
                params.add("password", randomString(6));
            }

            final ProgressDialog loadingIndicator = new ProgressDialog(context);
            if (authCallback != null) {
                loadingIndicator.setMessage(iPowerApplication.context.getResources().getString(R.string.loading_dots));
                loadingIndicator.setCancelable(false);
                loadingIndicator.show();
            } else {
                editor.putString("setting_account_email", user.get("email"));
                editor.putString("setting_account_name", user.get("firstname") + " " + user.get("lastname"));
                editor.commit();
            }

            AsyncHttpClient client = new AsyncHttpClient();

            client.post("https://ipower.tattoo/api/sign-up/?cache=" + System.currentTimeMillis(), params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    if (loadingIndicator.isShowing()) {
                        loadingIndicator.dismiss();
                    }

                    if (authCallback != null) {
                        boolean success, exists;

                        try {
                            exists = response.getBoolean("exists");
                        } catch (JSONException e) {
                            exists = false;
                        }

                        try {
                            success = response.getBoolean("success");
                        } catch (JSONException e) {
                            success = false;
                        }

                        if (exists) {
                            iPowerApplication.makeToast(R.string.auth_error_account_exists);
                            return;
                        }

                        if (!success) {
                            JSONObject error;
                            String error_message = null;

                            try {
                                error = response.getJSONObject("error");
                            } catch (JSONException e) {
                                error = null;
                            }

                            if (error != null) {
                                try {
                                    if (error.getBoolean("firstname")) {
                                        error_message = iPowerApplication.context.getResources().getString(R.string.auth_error_firstname);
                                    }
                                } catch (JSONException e) {}
                                try {
                                    if (error.getBoolean("lastname")) {
                                        error_message = iPowerApplication.context.getResources().getString(R.string.auth_error_lastname);
                                    }
                                } catch (JSONException e) {}
                                try {
                                    if (error.getBoolean("email")) {
                                        error_message = iPowerApplication.context.getResources().getString(R.string.auth_error_email);
                                    }
                                } catch (JSONException e) {}
                                try {
                                    if (error.getBoolean("password")) {
                                        error_message = iPowerApplication.context.getResources().getString(R.string.auth_error_password);
                                    }
                                } catch (JSONException e) {}
                            }

                            if (error_message == null) {
                                error_message = iPowerApplication.context.getResources().getString(R.string.auth_error_connection_failed);
                            }

                            iPowerApplication.makeToast(error_message);
                        } else {
                            editor.putString("setting_account_email", user.get("email"));
                            editor.putString("setting_account_name", user.get("firstname") + " " + user.get("lastname"));
                            editor.commit();

                            iPowerApplication.makeToast(R.string.auth_success_sign_up);

                            authCallback.onAuthFinished();
                        }
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    loadingIndicator.dismiss();
                    iPowerApplication.makeToast(R.string.auth_error_connection_failed);
                }
            });
        }
    }

    public static void sign_up(Map<String, String> user, Context context) {
        sign_up(user, null, context);
    }

    public static void sign_in(final Map<String, String> user, final AuthCallback authCallback, final Context context) {
        RequestParams params = new RequestParams();
        params.add("email", user.get("email"));
        params.add("password", user.get("password"));

        final ProgressDialog loadingIndicator = new ProgressDialog(context);
        loadingIndicator.setMessage(iPowerApplication.context.getResources().getString(R.string.loading_dots));
        loadingIndicator.setCancelable(false);
        loadingIndicator.show();

        AsyncHttpClient client = new AsyncHttpClient();

        client.post("https://ipower.tattoo/api/sign-in/?cache=" + System.currentTimeMillis(), params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                loadingIndicator.dismiss();

                boolean success;

                try {
                    success = response.getBoolean("success");
                } catch (JSONException e) {
                    success = false;
                }

                if (success) {
                    String name;

                    try {
                        name = response.getString("name");
                    } catch (JSONException e) {
                        name = null;
                    }

                    if (name != null) {
                        editor.putString("setting_account_email", user.get("email"));
                        editor.putString("setting_account_name", name);
                        editor.commit();
                    }

                    authCallback.onAuthFinished();
                } else {
                    iPowerApplication.makeToast(R.string.auth_error_incorrect_email_or_password);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                loadingIndicator.dismiss();
                iPowerApplication.makeToast(R.string.auth_error_connection_failed);
            }
        });
    }

    public static void reset(final Map<String, String> user, final AuthCallback authCallback, final Context context) {
        RequestParams params = new RequestParams();
        params.add("email", user.get("email"));

        final ProgressDialog loadingIndicator = new ProgressDialog(context);
        loadingIndicator.setMessage(iPowerApplication.context.getResources().getString(R.string.loading_dots));
        loadingIndicator.setCancelable(false);
        loadingIndicator.show();

        AsyncHttpClient client = new AsyncHttpClient();

        client.post("https://ipower.tattoo/api/reset/?cache=" + System.currentTimeMillis(), params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                loadingIndicator.dismiss();

                String message = null;
                boolean success, not_found;

                try {
                    success = response.getBoolean("success");
                } catch (JSONException e) {
                    success = false;
                }

                try {
                    not_found = response.getBoolean("not_found");
                } catch (JSONException e) {
                    not_found = false;
                }

                if (not_found) {
                    message = iPowerApplication.context.getResources().getString(R.string.auth_email_not_found);
                } else if (success) {
                    message = iPowerApplication.context.getResources().getString(R.string.auth_reset_instructions);
                } else {
                    JSONObject error;

                    try {
                        error = response.getJSONObject("error");
                    } catch (JSONException e) {
                        error = null;
                    }

                    if (error != null) {
                        try {
                            if (error.getBoolean("email")) {
                                message = iPowerApplication.context.getResources().getString(R.string.auth_error_email);
                            }
                        } catch (JSONException e) {}
                    }

                    if (message == null) {
                        message = iPowerApplication.context.getResources().getString(R.string.auth_error_connection_failed);
                    }
                }

                iPowerApplication.makeToast(message);

                if (success && !not_found) {
                    authCallback.onAuthFinished();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                loadingIndicator.dismiss();
                iPowerApplication.makeToast(R.string.auth_error_connection_failed);
            }
        });

    }

    public static void logged_out() {
        editor.remove("setting_account_login");
        editor.remove("setting_account_email");
        editor.remove("setting_account_name");
        editor.commit();
    }

    public static void logged_in(String login) {
        editor.putString("setting_account_login", login);
        editor.commit();
    }

    static String randomString(int len) {
        final String AB = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(len);
        for(int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }
}
