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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.SyncUser;
import io.realm.passwordmanager.model.Credential;
import io.realm.passwordmanager.model.Encrypter;
import io.realm.passwordmanager.ui.CredentialRecyclerAdapter;

/**
 * Displays the list of {@link Credential} available to the current user, with
 * the possibility to add new ones.
 */
public class CredentialActivity extends AppCompatActivity {
    private Realm realm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.credentials_activity);

        setSupportActionBar(findViewById(R.id.toolbar));

        setTitle("Credentials");

        findViewById(R.id.btn_add_credential).setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_task, null);
            EditText locationText = dialogView.findViewById(R.id.location);
            EditText usernameText = dialogView.findViewById(R.id.username);
            EditText passwordText = dialogView.findViewById(R.id.password);
            new AlertDialog.Builder(CredentialActivity.this)
                    .setTitle("Add a credential")
                    .setView(dialogView)
                    .setPositiveButton("Add", (dialog, which) -> {
                        String encryptedPassword = "ERROR";
                        try {
                            Encrypter encrypter = new Encrypter();
                            encryptedPassword = encrypter.encrypt(passwordText.getText().toString());
                        } catch (Exception e) {
                            Log.e("encryption error", e.getMessage());
                        }
                        Encrypter encrypter = new Encrypter();
                        Credential cred = new Credential();
                        cred.setLocation(locationText.getText().toString());
                        try {
                            cred.setPassword(encryptedPassword);
                        } catch (Exception e) {
                            Log.e("encrypt error", e.getMessage());
                        }
                        cred.setUsername(usernameText.getText().toString());
                        cred.set_id("_" + System.currentTimeMillis());
                        realm.executeTransactionAsync(realm -> realm.copyToRealm(cred));
                    })
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show();
        });


        realm = Realm.getDefaultInstance();
        RealmResults<Credential> credentials = realm
                .where(Credential.class)
                .findAllAsync();

        CredentialRecyclerAdapter credentialsRecyclerAdapter = new CredentialRecyclerAdapter(credentials, SyncUser.current().getIdentity());
        RecyclerView publicRecyclerView = findViewById(R.id.recycler_view_credentials);
        publicRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        publicRecyclerView.setAdapter(credentialsRecyclerAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            SyncUser syncUser = SyncUser.current();
            if (syncUser != null) {
                syncUser.logOut();
                Intent intent = new Intent(this, WelcomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
