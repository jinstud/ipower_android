package com.ipower.tattoo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;


public class WelcomeActivity extends Activity {

    private Button buttonFacebook;
    private Button buttonGoogle;
    private Button buttonSignUp;
    private Button buttonSignIn;

    private AlertDialog dialog;
    private AlertDialog dialog_prev;

    private static final int REQUEST_CODE_RESOLUTION = 0;
    private ConnectionResult connectionResult;
    private boolean googleSignInClicked;
    private boolean intentInProgress;

    private GoogleApiClient googleApiClient = null;
    private Session.StatusCallback sessionStatusCallback;

    private ProgressDialog loadingIndicator;

    final Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);

        loadingIndicator = new ProgressDialog(context);
        loadingIndicator.setMessage(context.getResources().getString(R.string.loading_dots));
        loadingIndicator.setCancelable(false);

        googleApiClient = new GoogleApiClient.Builder(context)
            .addConnectionCallbacks(new ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                    try {
                        if (Plus.PeopleApi.getCurrentPerson(googleApiClient) != null) {
                            Person currentPerson = Plus.PeopleApi
                                    .getCurrentPerson(googleApiClient);
                            String[] person = currentPerson.getDisplayName().split(" ");
                            Map<String, String> user_data = new HashMap<String, String>();
                            user_data.put("firstname", person[0]);
                            user_data.put("lastname", (person.length > 1 ? person[1] : ""));
                            user_data.put("email", Plus.AccountApi.getAccountName(googleApiClient));
                            Auth.sign_up(user_data, context);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }

                    Auth.logged_in("google");
                    welcomeOut();
                }

                @Override
                public void onConnectionSuspended(int i) {
                    googleApiClient.connect();
                    Auth.logged_out();
                    welcomeIn();
                }
            })
            .addOnConnectionFailedListener(new OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult result) {
                    if (!result.hasResolution()) {
                        Toast toast = Toast.makeText(context,
                                GooglePlayServicesUtil.getErrorString(result.getErrorCode()),
                                Toast.LENGTH_SHORT);
                        toast.show();

                        Auth.logged_out();
                        welcomeIn();

                        return;
                    }

                    if (!intentInProgress) {
                        connectionResult = result;

                        if (googleSignInClicked) {
                            resolveSignInError();
                            return;
                        }

                        Auth.logged_out();
                        welcomeIn();
                    }
                }
            }).addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(Plus.SCOPE_PLUS_LOGIN).build();

        sessionStatusCallback = new Session.StatusCallback() {
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                if (session.isOpened()) {
                    Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
                        @Override
                        public void onCompleted(GraphUser user, Response response) {
                            if (user != null) {
                                Map<String, String> user_data = new HashMap<String, String>();
                                user_data.put("firstname", user.getFirstName() == null ? "" : user.getFirstName());
                                user_data.put("lastname", user.getLastName() == null ? "" : user.getLastName());
                                user_data.put("email", user.getProperty("email").toString());
                                Auth.sign_up(user_data, context);
                            }
                        }
                    });

                    Bundle parameters = new Bundle();
                    parameters.putString("fields", "firstname, lastname, email");

                    request.setParameters(parameters);
                    request.executeAsync();

                    Auth.logged_in("facebook");

                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }

                    welcomeOut();
                    return;
                }

                if (state == SessionState.CLOSED || state == SessionState.CLOSED_LOGIN_FAILED || exception != null) {
                    if (exception != null) {
                        session.closeAndClearTokenInformation();
                    }

                    Auth.logged_out();
                    welcomeIn();
                }
            }
        };

        buttonFacebook = (Button)findViewById(R.id.buttonFacebook);
        buttonFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                openFacebookSession(false);
            }
        });

        buttonGoogle = (Button)findViewById(R.id.buttonGoogle);
        buttonGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                signInWithGplus();
            }
        });

        buttonSignUp = (Button)findViewById(R.id.buttonSignUp);
        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater layoutInflater = LayoutInflater.from(context);
                final View signUpView = layoutInflater.inflate(R.layout.dialog_sign_up, null);
                Button buttonTermsAndPrivacy = (Button)signUpView.findViewById(R.id.buttonTermsAndPrivacy);
                buttonTermsAndPrivacy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        LayoutInflater layoutInflater = LayoutInflater.from(context);
                        final View termsAndPrivacyView = layoutInflater.inflate(R.layout.dialog_terms_privacy, null);
                        AlertDialog.Builder builder = new AlertDialog.Builder(context).setInverseBackgroundForced(true);
                        builder.setView(termsAndPrivacyView);
                        if (dialog != null && dialog.isShowing()) {
                            dialog_prev = dialog;
                            dialog.dismiss();
                        }
                        dialog = builder.create();
                        Button buttonOk = (Button) termsAndPrivacyView.findViewById(R.id.buttonOk);
                        buttonOk.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (dialog_prev != null) {
                                    dialog.dismiss();
                                    dialog = dialog_prev;
                                    dialog.show();
                                    dialog_prev = null;
                                }
                            }
                        });
                        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                if (dialog_prev != null) {
                                    dialog.dismiss();
                                    dialog = dialog_prev;
                                    dialog.show();
                                    dialog_prev = null;
                                }
                            }
                        });
                        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (dialog_prev != null) {
                                    dialog.dismiss();
                                    dialog = dialog_prev;
                                    dialog.show();
                                    dialog_prev = null;
                                }
                            }
                        });

                        loadingIndicator.show();

                        WebView webTermsPrivacyView = (WebView)termsAndPrivacyView.findViewById(R.id.webTermsPrivacyView);
                        webTermsPrivacyView.setWebViewClient(new WebViewClient() {
                            private boolean isError = false;

                            @Override
                            public void onPageFinished(WebView view, String url) {
                                if (loadingIndicator.isShowing()) {
                                    loadingIndicator.dismiss();
                                }

                                if (!isError) {
                                    dialog.show();

                                    Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
                                    Point size = new Point();
                                    display.getSize(size);

                                    dialog.getWindow().setLayout(size.x, size.y);
                                }
                            }

                            @Override
                            public void onReceivedError(WebView view, int errorCode, String description, String url) {
                                dialog.dismiss();

                                isError = true;

                                iPowerApplication.makeToast(R.string.auth_error_connection_failed);
                            }
                        });

                        webTermsPrivacyView.loadUrl("http://www.ipowertattoo.com/terms-and-conditions.html?cache=" + System.currentTimeMillis());
                    }
                });
                Button buttonSignUp = (Button)signUpView.findViewById(R.id.buttonSignUp);
                buttonSignUp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        EditText editFirstName = (EditText)signUpView.findViewById(R.id.editFirstName);
                        EditText editLastName = (EditText)signUpView.findViewById(R.id.editLastName);
                        EditText editEmail = (EditText)signUpView.findViewById(R.id.editEmail);
                        EditText editPassword = (EditText)signUpView.findViewById(R.id.editPassword);

                        InputMethodManager imm = (InputMethodManager)getSystemService(
                                Context.INPUT_METHOD_SERVICE);

                        if (editFirstName.hasFocus()) {
                            editFirstName.clearFocus();
                            imm.hideSoftInputFromWindow(editFirstName.getWindowToken(), 0);
                        }

                        if (editLastName.hasFocus()) {
                            editLastName.clearFocus();
                            imm.hideSoftInputFromWindow(editLastName.getWindowToken(), 0);
                        }

                        if (editEmail.hasFocus()) {
                            editEmail.clearFocus();
                            imm.hideSoftInputFromWindow(editEmail.getWindowToken(), 0);
                        }

                        if (editPassword.hasFocus()) {
                            editPassword.clearFocus();
                            imm.hideSoftInputFromWindow(editPassword.getWindowToken(), 0);
                        }

                        String error_message = null;
                        int length = 0;

                        length = editFirstName.getText().toString().length();
                        if (length < 1 || length > 32) {
                            error_message = context.getResources().getString(R.string.auth_error_firstname);
                            editFirstName.requestFocus();
                        } else {
                            length = editLastName.getText().toString().length();
                            if (length < 1 || length > 32) {
                                error_message = context.getResources().getString(R.string.auth_error_lastname);
                                editLastName.requestFocus();
                            } else {
                                if (!isValidEmail(editEmail.getText().toString())) {
                                    error_message = context.getResources().getString(R.string.auth_error_email);
                                    editEmail.requestFocus();
                                } else {
                                    length = editPassword.getText().toString().length();
                                    if (length < 4 || length > 20) {
                                        error_message = context.getResources().getString(R.string.auth_error_password);
                                        editPassword.requestFocus();
                                    }
                                }
                            }
                        }

                        if (error_message == null) {
                            Map<String, String> user_data = new HashMap<String, String>();
                            user_data.put("firstname", editFirstName.getText().toString());
                            user_data.put("lastname", editLastName.getText().toString());
                            user_data.put("email", editEmail.getText().toString());
                            user_data.put("password", editPassword.getText().toString());

                            Auth.sign_up(user_data, new Auth.AuthCallback() {
                                @Override
                                public void onAuthFinished() {
                                    Auth.logged_in("email");
                                    welcomeOut();
                                }
                            }, context);
                        } else {
                            Toast toast = Toast.makeText(context,
                                    error_message,
                                    Toast.LENGTH_SHORT);
                            //toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    }
                });
                final AlertDialog.Builder builder = new AlertDialog.Builder(context).setInverseBackgroundForced(true);
                builder.setView(signUpView);
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                dialog = builder.create();
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                    }
                });
                dialog.show();

                Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);

                dialog.getWindow().setLayout(size.x, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });

        buttonSignIn = (Button) findViewById(R.id.buttonSignIn);
        buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater layoutInflater = LayoutInflater.from(context);
                final View signInView = layoutInflater.inflate(R.layout.dialog_sign_in, null);
                Button buttonFacebook = (Button)signInView.findViewById(R.id.buttonFacebook);
                buttonFacebook.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        dialog.dismiss();
                        openFacebookSession(false);
                    }
                });
                Button buttonGoogle = (Button)signInView.findViewById(R.id.buttonGoogle);
                buttonGoogle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        dialog.dismiss();
                        signInWithGplus();
                    }
                });
                Button buttonForgotPassword = (Button)signInView.findViewById(R.id.buttonForgotPassword);
                buttonForgotPassword.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        LayoutInflater layoutInflater = LayoutInflater.from(context);
                        final View passwordResetView = layoutInflater.inflate(R.layout.dialog_password_reset, null);
                        AlertDialog.Builder builder = new AlertDialog.Builder(context).setInverseBackgroundForced(true);
                        builder.setView(passwordResetView);
                        if (dialog != null && dialog.isShowing()) {
                            dialog_prev = dialog;
                            dialog.dismiss();
                        }
                        dialog = builder.create();
                        Button buttonBack = (Button) passwordResetView.findViewById(R.id.buttonBack);
                        buttonBack.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (dialog_prev != null) {
                                    dialog.dismiss();
                                    dialog = dialog_prev;
                                    dialog.show();
                                    dialog_prev = null;
                                }
                            }
                        });
                        Button buttonReset = (Button) passwordResetView.findViewById(R.id.buttonReset);
                        buttonReset.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                EditText editEmail = (EditText)passwordResetView.findViewById(R.id.editEmail);

                                if (editEmail.getText().toString().isEmpty()) {
                                    editEmail.requestFocus();
                                } else {
                                    InputMethodManager imm = (InputMethodManager)getSystemService(
                                            Context.INPUT_METHOD_SERVICE);

                                    if (editEmail.hasFocus()) {
                                        editEmail.clearFocus();
                                        imm.hideSoftInputFromWindow(editEmail.getWindowToken(), 0);
                                    }

                                    Map<String, String> user_data = new HashMap<String, String>();
                                    user_data.put("email", editEmail.getText().toString());

                                    Auth.reset(user_data, new Auth.AuthCallback() {
                                        @Override
                                        public void onAuthFinished() {
                                            dialog.dismiss();
                                            dialog_prev = null;
                                        }
                                    }, context);
                                }
                            }
                        });
                        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                if (dialog_prev != null) {
                                    dialog.dismiss();
                                    dialog = dialog_prev;
                                    dialog.show();
                                    dialog_prev = null;
                                }
                            }
                        });
                        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (dialog_prev != null) {
                                    dialog.dismiss();
                                    dialog = dialog_prev;
                                    dialog.show();
                                    dialog_prev = null;
                                }
                            }
                        });
                        dialog.show();

                        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
                        Point size = new Point();
                        display.getSize(size);

                        dialog.getWindow().setLayout(size.x, ViewGroup.LayoutParams.WRAP_CONTENT);
                    }
                });
                Button buttonSignIn = (Button)signInView.findViewById(R.id.buttonSignIn);
                buttonSignIn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        EditText editEmail = (EditText)signInView.findViewById(R.id.editEmail);
                        EditText editPassword = (EditText)signInView.findViewById(R.id.editPassword);

                        if (editEmail.getText().toString().isEmpty()) {
                            editEmail.requestFocus();
                        } else if (editPassword.getText().toString().isEmpty()) {
                            editPassword.requestFocus();
                        } else {
                            InputMethodManager imm = (InputMethodManager)getSystemService(
                                    Context.INPUT_METHOD_SERVICE);

                            if (editEmail.hasFocus()) {
                                editEmail.clearFocus();
                                imm.hideSoftInputFromWindow(editEmail.getWindowToken(), 0);
                            }

                            if (editPassword.hasFocus()) {
                                editPassword.clearFocus();
                                imm.hideSoftInputFromWindow(editPassword.getWindowToken(), 0);
                            }

                            Map<String, String> user_data = new HashMap<String, String>();
                            user_data.put("email", editEmail.getText().toString());
                            user_data.put("password", editPassword.getText().toString());

                            Auth.sign_in(user_data, new Auth.AuthCallback() {
                                @Override
                                public void onAuthFinished() {
                                    Auth.logged_in("email");

                                    if (dialog != null && dialog.isShowing()) {
                                        dialog.dismiss();
                                    }

                                    welcomeOut();
                                }
                            }, context);
                        }
                    }
                });
                AlertDialog.Builder builder = new AlertDialog.Builder(context).setInverseBackgroundForced(true);
                builder.setView(signInView);
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                dialog = builder.create();
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                    }
                });
                dialog.show();

                Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);

                dialog.getWindow().setLayout(size.x, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });

        String login = Auth.preferences.getString("setting_account_login", "");
        if (login.equals("facebook")) {
            welcomeOut();
        } else if (login.equals("google")) {
            welcomeOut();
        } else if (login.equals("email") && !(Auth.preferences.getString("setting_account_email", null) != null)) {
            welcomeOut();
        } else {
            Auth.logged_out();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Auth.preferences.getString("setting_account_login", "") == "") {
            welcomeIn();
        }

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    private void welcomeIn() {
        ImageView logo = (ImageView) findViewById(R.id.imageLogoView);
        LinearLayout loginBlock = (LinearLayout) findViewById(R.id.loginBlock);

        if (loginBlock.getVisibility() == View.INVISIBLE) {
            TranslateAnimation animation = new TranslateAnimation(
                    TranslateAnimation.ABSOLUTE, 0.0f,
                    TranslateAnimation.ABSOLUTE, 0.0f,
                    TranslateAnimation.RELATIVE_TO_PARENT, 0.0f,
                    TranslateAnimation.RELATIVE_TO_PARENT, -0.2f);
            animation.setStartOffset(500);
            animation.setDuration(300);
            animation.setFillAfter(true);
            animation.setInterpolator(new DecelerateInterpolator());
            logo.setAnimation(animation);

            Animation in = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            in.setStartOffset(800);
            loginBlock.startAnimation(in);
            loginBlock.setVisibility(View.VISIBLE);
        }
    }

    private void welcomeOut() {
        final LinearLayout loginBlock = (LinearLayout) findViewById(R.id.loginBlock);

        if (loginBlock.getVisibility() == View.VISIBLE) {
            final ImageView logo = (ImageView) findViewById(R.id.imageLogoView);
            final TranslateAnimation animation = (TranslateAnimation)logo.getAnimation();
            animation.setInterpolator(new ReverseInterpolator());
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation arg0) {}

                @Override
                public void onAnimationRepeat(Animation arg0) {}

                @Override
                public void onAnimationEnd(Animation arg0) {
                    Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                    WelcomeActivity.this.startActivity(intent);
                    WelcomeActivity.this.finish();
                }
            });

            animation.start();

            Animation out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
            out.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation arg0) {}

                @Override
                public void onAnimationRepeat(Animation arg0) {}

                @Override
                public void onAnimationEnd(Animation arg0) {
                    loginBlock.setVisibility(View.INVISIBLE);
                    logo.setAnimation(animation);
                }
            });

            loginBlock.startAnimation(out);
        } else {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            WelcomeActivity.this.startActivity(intent);
            WelcomeActivity.this.finish();
        }
    }

    private void openFacebookSession(boolean silently) {
        openActiveSession(this, !silently, Arrays.asList(new String[]{"email", "public_profile"}), sessionStatusCallback);
    }

    private static Session openActiveSession(Activity activity, boolean allowLoginUI, List permissions, Session.StatusCallback callback) {
        Session.OpenRequest openRequest = new Session.OpenRequest(activity).setPermissions(permissions).setCallback(callback);
        Session session = new Session.Builder(activity).build();
        if (SessionState.CREATED_TOKEN_LOADED.equals(session.getState()) || allowLoginUI) {
            Session.setActiveSession(session);
            session.openForRead(openRequest);
            return session;
        }
        return null;
    }

    @Override
     public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_RESOLUTION) {
            if (resultCode != RESULT_OK) {
                googleSignInClicked = false;
            }

            intentInProgress = false;

            if (!googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
        }

        if (loadingIndicator.isShowing()) {
            loadingIndicator.dismiss();
        }

        if (Session.getActiveSession() != null) {
            Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_welcome, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void signInWithGplus() {
        if (!googleApiClient.isConnecting()) {
            googleSignInClicked = true;
            resolveSignInError();
        }
    }

    private void resolveSignInError() {
        if (connectionResult != null && connectionResult.hasResolution()) {
            try {
                loadingIndicator.show();
                intentInProgress = true;
                connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
            } catch (IntentSender.SendIntentException e) {
                intentInProgress = false;
                googleApiClient.connect();
            }
        } else {
            googleApiClient.connect();
        }
    }

    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}
