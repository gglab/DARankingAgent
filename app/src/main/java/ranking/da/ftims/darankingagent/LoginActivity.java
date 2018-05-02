package ranking.da.ftims.darankingagent;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import retrofit2.Call;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private UserLoginTask mAuthTask = null;
    private EditText mLoginView;
    private EditText mPasswordView;
    private EditText mServerView;
    private View mProgressView;
    private View mLoginFormView;

    static Retrofit retrofit;
    static DARankingAppService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mLoginView = (EditText) findViewById(R.id.login);
        mServerView = (EditText) findViewById(R.id.server);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
            mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mServerView.setError(null);
        mLoginView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String server = mServerView.getText().toString();
        String login = mLoginView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }
        if (TextUtils.isEmpty(server)){
            mServerView.setError(getString(R.string.error_field_required));
            focusView = mServerView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(login)) {
            mLoginView.setError(getString(R.string.error_field_required));
            focusView = mLoginView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            Log.i("DA", "Starting login task for: " + server);
            Log.i("DA", "user: " + login);
            Log.i("DA","pass: " + password);

            mAuthTask = new UserLoginTask(server, login, password);
            mAuthTask.authenticateUser();
            showProgress(false);
            //mAuthTask.startApp();
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    public class UserLoginTask {
        private TokenCredentials token;
        private DARankingAppCredentials credentials;
        private DARankingAppDriver driver;

        UserLoginTask(String server, String login, String password) {
            retrofit = new Retrofit.Builder().baseUrl(server).addConverterFactory((GsonConverterFactory.create())).build();
            service = retrofit.create(DARankingAppService.class);
            credentials = new DARankingAppCredentials();
            credentials.password = password;
            credentials.remeberMe = true;
            credentials.username = login;
            Log.i("DA","UserLoginTask");
        }

        public void authenticateUser(){
            Log.i("DA", "authenticateUser");
            Call<ResponseAuthentication> userCredentials = service.authenticate(credentials);
            try{
                userCredentials.enqueue(new Callback<ResponseAuthentication>() {
                    @Override
                    public void onResponse(Call<ResponseAuthentication> call, Response<ResponseAuthentication> response) {
                        if(response.isSuccessful()){
                            Log.i("DA", "Success: " + response.body().toString());
                            String responseHeader = response.headers().get("Authorization");
                            TokenCredentials.tokenId = responseHeader;
                            Log.i("DA", TokenCredentials.tokenId);
                            getDriver();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseAuthentication> call, Throwable t) {
                        Log.e("DA", "Fail: " + t.toString());
                    }
                });
            }
            catch(Exception e){
                Log.e("DA", "Exception: " + e.toString());
            }
        }

        public void getDriver() {
            Log.i("DA", "getDriver");
            Call<DARankingAppDriver> driverCall = service.getDriver(TokenCredentials.tokenId, credentials.username);
            try {
                driverCall.enqueue(new Callback<DARankingAppDriver>() {
                    @Override
                    public void onResponse(Call<DARankingAppDriver> call, Response<DARankingAppDriver> response) {
                        if (response.isSuccessful()) {
                            Log.i("DA", "Success: " + response.body().toString());
                            driver = response.body();
                            Log.i("DA", driver.id);
                            Log.i("DA", driver.name);
                            Log.i("DA", driver.rank);
                            startApp();
                        }
                    }

                    @Override
                    public void onFailure(Call<DARankingAppDriver> call, Throwable t) {
                        Log.e("DA", "Failed: " + t.toString());
                    }
                });
            } catch (Exception e) {
                Log.e("DA", "Failed: " + e.toString());
            }
        }

        public void startApp(){
            Intent i = new Intent(LoginActivity.this, DrivingAnalyticsAgent.class);
            i.putExtra("driver", driver);
            startActivity(i);
        }
    }
}

