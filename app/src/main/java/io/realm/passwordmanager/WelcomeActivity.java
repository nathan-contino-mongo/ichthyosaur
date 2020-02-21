/*
 * Copyright 2018 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.passwordmanager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.realm.ObjectServerError;
import io.realm.Realm;
import io.realm.SyncConfiguration;
import io.realm.SyncCredentials;
import io.realm.SyncUser;

import static io.realm.passwordmanager.util.Constants.AUTH_URL;

public class WelcomeActivity extends AppCompatActivity {

    private EditText usernameTextView;
    private EditText passwordTextView;
    private View progressView;
    private View loginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);
        SyncUser user = SyncUser.current();
        if (user != null) {
            setUpDefaultRealm();
            navigateToListOfChatRooms();
        }

        // Set up the login form.
        usernameTextView = findViewById(R.id.username);
        passwordTextView = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.login_button);
        Button createAccountButton = findViewById(R.id.create_account_button);
        loginButton.setOnClickListener(view -> attemptLogin(false));
        createAccountButton.setOnClickListener(view -> attemptLogin(true));
        loginFormView = findViewById(R.id.login_form);
        progressView = findViewById(R.id.login_progress);
    }

    private void attemptLogin(boolean createAccount) {
        // Reset nicknameTextView.
        usernameTextView.setError(null);
        // Store values at the time of the login attempt.
        String username = usernameTextView.getText().toString();
        String password = passwordTextView.getText().toString();

        showProgress(true);

        SyncCredentials credentials = SyncCredentials.usernamePassword(username, password, createAccount);
        SyncUser.logInAsync(credentials, AUTH_URL, new SyncUser.Callback<SyncUser>() {
            @Override
            public void onSuccess(SyncUser user) {
                setUpDefaultRealm();
            }

            @Override
            public void onError(ObjectServerError error) {
                showProgress(false);
                usernameTextView.setError("Uh oh something went wrong!");
                usernameTextView.requestFocus();
                Log.e("Login error", error.toString());
            }
        });
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        loginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });
        progressView.setVisibility(show ? View.VISIBLE : View.GONE);
        progressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setUpDefaultRealm() {
        SyncConfiguration config = SyncUser.current()
                .createConfiguration("/~/credentials")
                .fullSynchronization()
                //.waitForInitialRemoteData(30, TimeUnit.SECONDS)
                .build();
        Realm.setDefaultConfiguration(config);
        navigateToListOfChatRooms();
    }

    private void navigateToListOfChatRooms() {
        Intent intent = new Intent(WelcomeActivity.this, CredentialActivity.class);
        startActivity(intent);
    }
}

