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

package io.realm.passwordmanager.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.ColorInt;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.passwordmanager.R;
import io.realm.passwordmanager.model.Credential;
import io.realm.passwordmanager.model.Encrypter;

/**
 * Adapter to display the list of {@link Credential}.
 */
public class CredentialRecyclerAdapter extends RealmRecyclerViewAdapter<Credential, CredentialRecyclerAdapter.MyViewHolder> {
    private final String userIdentity; // used to differentiate messages visually from other users

    private ViewGroup parent;

    public CredentialRecyclerAdapter(OrderedRealmCollection<Credential> data, String userIdentity) {
        super(data, true);
        this.userIdentity = userIdentity;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.credential_item_layout, parent, false);
        this.parent = parent;
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Credential credential = getItem(position);
        if (credential != null) {
            holder.setItem(credential);
        }
        String decryptedPassword = "ERROR";
        try {
            Encrypter encrypter = new Encrypter();
            decryptedPassword = encrypter.decrypt(credential.getPassword());
        } catch (Exception e) {
            Log.e("decryption error", e.getMessage());
        }
        final String dP = decryptedPassword;

        holder.itemView.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(parent.getContext()).inflate(R.layout.credentials_display_dialog, null);
            EditText passwordView = dialogView.findViewById(R.id.password);
            passwordView.setText(dP);
            passwordView.setOnTouchListener((view, event) -> {
                switch ( event.getAction() ) {
                    case MotionEvent.ACTION_UP:
                        passwordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        break;

                    case MotionEvent.ACTION_DOWN:
                        passwordView.setInputType(InputType.TYPE_CLASS_TEXT);
                        break;
                }
                return true;
            });
            TextView usernameView = dialogView.findViewById(R.id.username);
            usernameView.setText(credential.getUsername());
            new AlertDialog.Builder(parent.getContext())
                    .setTitle("Login for " + credential.getLocation())
                    .setView(dialogView)
                    .setNegativeButton("Close", null)
                    .create()
                    .show();
        });
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        String item_id;
        TextView location;
        TextView username;
        ImageView favicon;

        MyViewHolder(View itemView) {
            super(itemView);
            location = itemView.findViewById(R.id.location);
            username = itemView.findViewById(R.id.username);
            favicon = itemView.findViewById(R.id.favicon);
            itemView.findViewById(R.id.edit_button).setOnClickListener(Vv -> {
                View dialogView = LayoutInflater.from(parent.getContext()).inflate(R.layout.dialog_task, null);
                EditText locationText = dialogView.findViewById(R.id.location);
                EditText usernameText = dialogView.findViewById(R.id.username);
                EditText passwordText = dialogView.findViewById(R.id.password);
                Realm realmForDialog = Realm.getDefaultInstance();
                Credential credential = realmForDialog.where(Credential.class).contains("_id", item_id).findFirst();
                locationText.setText(credential.getLocation());
                usernameText.setText(credential.getUsername());
                String decryptedPassword = "ERROR";
                try {
                    Encrypter encrypter = new Encrypter();
                    decryptedPassword = encrypter.decrypt(credential.getPassword());
                } catch (Exception e) {
                    Log.e("decryption error", e.getMessage());
                }
                final String dP = decryptedPassword;
                passwordText.setText(dP);
                new AlertDialog.Builder(parent.getContext())
                        .setTitle("Edit credential")
                        .setView(dialogView)
                        .setPositiveButton("Edit", (dialog, which) -> {
                            String encryptedPassword = "ERROR";
                            try {
                                Encrypter encrypter = new Encrypter();
                                encryptedPassword = encrypter.encrypt(passwordText.getText().toString());
                            } catch (Exception e) {
                                Log.e("encryption error", e.getMessage());
                            }
                            final String eP = encryptedPassword;
                            realmForDialog.executeTransactionAsync( realm -> {
                                Realm realmForDialog2 = Realm.getDefaultInstance();
                                Credential credential2 = realmForDialog2.where(Credential.class).contains("_id", item_id).findFirst();
                                credential2.setLocation(locationText.getText().toString());
                                credential2.setPassword(eP);
                                credential2.setUsername(usernameText.getText().toString());
                            });

                        })
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show();
            });
        }

        void setItem(Credential item){
            this.item_id = item.get_id();
            this.location.setText(item.getLocation());
            this.username.setText(item.getUsername());
            FaviconFetcherTask fTask = new FaviconFetcherTask();
            fTask.imageView = favicon;
            fTask.execute(item.getLocation());
        }

        private class FaviconFetcherTask extends AsyncTask<String, Void, Bitmap> {
            ImageView imageView;

            @Override
            protected Bitmap doInBackground(String... uri) {
                try {
                    URL url = new URL("https", uri[0], "/favicon.ico");
                    HttpURLConnection connection =
                            (HttpURLConnection)url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    return BitmapFactory.decodeStream(input);
                } catch (Exception e) {
                    Log.d("no favicon", e.toString());
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    this.imageView.setImageBitmap(bitmap);
                } else {
                    this.imageView.setImageResource(R.drawable.ic_vpn_key_black_24dp);
                }
            }
        }

    }
}
