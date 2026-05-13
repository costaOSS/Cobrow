package com.cobrow.browser.engine;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.cobrow.browser.data.CobrowDatabase;
import com.cobrow.browser.data.Credential;
import com.cobrow.browser.utils.CryptoUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CredentialManager {
    private final Context context;
    private final CobrowDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CredentialManager(Context context) {
        this.context = context;
        this.db = CobrowDatabase.get(context);
    }

    @JavascriptInterface
    public void onFormSubmit(String host, String username, String password) {
        if (host == null || username == null || password == null || username.isEmpty() || password.isEmpty()) return;

        executor.execute(() -> {
            List<Credential> existing = db.credentialDao().getByHost(host);
            boolean found = false;
            for (Credential c : existing) {
                if (c.username.equals(username)) {
                    found = true;
                    if (!CryptoUtils.decrypt(c.password).equals(password)) {
                        // Offer update
                        showUpdateDialog(host, username, password, c);
                    }
                    break;
                }
            }
            if (!found) {
                showSaveDialog(host, username, password);
            }
        });
    }

    private void showSaveDialog(String host, String username, String password) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            new AlertDialog.Builder(context)
                    .setTitle("Save Password?")
                    .setMessage("Do you want to save the password for " + username + " on " + host + "?")
                    .setPositiveButton("Save", (d, w) -> {
                        executor.execute(() -> db.credentialDao().insert(new Credential(host, username, CryptoUtils.encrypt(password))));
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Never", null)
                    .show();
        });
    }

    private void showUpdateDialog(String host, String username, String password, Credential old) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            new AlertDialog.Builder(context)
                    .setTitle("Update Password?")
                    .setMessage("Do you want to update the saved password for " + username + " on " + host + "?")
                    .setPositiveButton("Update", (d, w) -> {
                        executor.execute(() -> {
                            db.credentialDao().delete(old);
                            db.credentialDao().insert(new Credential(host, username, CryptoUtils.encrypt(password)));
                        });
                        Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    public void autofill(WebView webView, String host) {
        executor.execute(() -> {
            List<Credential> credentials = db.credentialDao().getByHost(host);
            if (!credentials.isEmpty()) {
                Credential c = credentials.get(0); // Take first for now
                String password = CryptoUtils.decrypt(c.password);
                if (!CryptoUtils.isEncrypted(c.password)) {
                    c.password = CryptoUtils.encrypt(c.password);
                    db.credentialDao().delete(credentials.get(0));
                    db.credentialDao().insert(c);
                }
                String js = "javascript:(function(){" +
                        "var inputs = document.getElementsByTagName('input');" +
                        "for(var i=0; i<inputs.length; i++){" +
                        "  if(inputs[i].type == 'password'){" +
                        "    inputs[i].value = '" + escapeJs(password) + "';" +
                        "    if(i > 0 && (inputs[i-1].type == 'text' || inputs[i-1].type == 'email')) inputs[i-1].value = '" + escapeJs(c.username) + "';" +
                        "  }" +
                        "}" +
                        "})()";
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> webView.loadUrl(js));
            }
        });
    }

    public void injectFormDetection(WebView webView) {
        String js = "javascript:(function(){" +
                "window.addEventListener('submit', function(e){" +
                "  var form = e.target;" +
                "  var username = '', password = '';" +
                "  var inputs = form.getElementsByTagName('input');" +
                "  for(var i=0; i<inputs.length; i++){" +
                "    if(inputs[i].type == 'password') password = inputs[i].value;" +
                "    else if(inputs[i].type == 'text' || inputs[i].type == 'email') username = inputs[i].value;" +
                "  }" +
                "  CobrowPasswordManager.onFormSubmit(window.location.host, username, password);" +
                "});" +
                "})()";
        webView.loadUrl(js);
    }

    private String escapeJs(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
