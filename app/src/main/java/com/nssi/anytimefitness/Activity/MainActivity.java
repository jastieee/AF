package com.nssi.anytimefitness.Activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.nssi.anytimefitness.Connection.ClsDatabaseCreation;
import com.nssi.anytimefitness.R;

public class MainActivity extends AppCompatActivity {

    // Header views
    private TextView tvUserName, tvUserRole;
    private FrameLayout btnLogout;

    // Module cards
    private FrameLayout cardInventory, cardMaintenance, cardLogs, cardUserManagement, cardSettings;

    // Session data (passed from LoginActivity)
    private int userId;
    private String userName, userRole, username;

    // DB
    private ClsDatabaseCreation dbHelper;

    // Module name constants — must match what was seeded in ClsDatabaseCreation.seedModules()
    private static final String MODULE_INVENTORY        = "Inventory";
    private static final String MODULE_MAINTENANCE      = "Maintenance";
    private static final String MODULE_LOGS             = "Logs";
    private static final String MODULE_USER_MANAGEMENT  = "User Management";
    private static final String MODULE_SETTINGS         = "Settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immersive full screen, same as login
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_main);

        // ── Receive session from LoginActivity ──
        userId   = getIntent().getIntExtra("USER_ID", -1);
        userName = getIntent().getStringExtra("USER_NAME");
        userRole = getIntent().getStringExtra("USER_ROLE");
        username = getIntent().getStringExtra("USERNAME");

        if (userId == -1 || userName == null || userRole == null) {
            redirectToLogin();
            return;
        }

        dbHelper = new ClsDatabaseCreation(this);

        bindViews();
        populateHeader();
        applyPermissions();
        setCardListeners();
        animateCardsIn();
    }


    //  BIND VIEWS
    private void bindViews() {
        tvUserName          = findViewById(R.id.tvUserName);
        tvUserRole          = findViewById(R.id.tvUserRole);
        btnLogout           = findViewById(R.id.btnLogout);
        cardInventory       = findViewById(R.id.cardInventory);
        cardMaintenance     = findViewById(R.id.cardMaintenance);
        cardLogs            = findViewById(R.id.cardLogs);
        cardUserManagement  = findViewById(R.id.cardUserManagement);
        cardSettings        = findViewById(R.id.cardSettings);
    }

    private void populateHeader() {
        tvUserName.setText(userName);
        tvUserRole.setText(userRole);
    }

    private void applyPermissions() {
        cardInventory.setVisibility(
                hasAccess(MODULE_INVENTORY) ? View.VISIBLE : View.GONE);

        cardMaintenance.setVisibility(
                hasAccess(MODULE_MAINTENANCE) ? View.VISIBLE : View.GONE);

        cardLogs.setVisibility(
                hasAccess(MODULE_LOGS) ? View.VISIBLE : View.GONE);

        cardSettings.setVisibility(
                hasAccess(MODULE_SETTINGS) ? View.VISIBLE : View.GONE);

        cardUserManagement.setVisibility(
                hasAccess(MODULE_USER_MANAGEMENT) ? View.VISIBLE : View.GONE);
    }


    private boolean hasAccess(String moduleName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        boolean access = false;

        try {
            Cursor cursor = db.rawQuery(
                    "SELECT rp." + ClsDatabaseCreation.COL_PERM_ACCESS +
                            " FROM " + ClsDatabaseCreation.TABLE_ROLE_PERMISSIONS + " rp" +
                            " INNER JOIN " + ClsDatabaseCreation.TABLE_MODULES + " m" +
                            "   ON rp." + ClsDatabaseCreation.COL_PERM_MODULE_ID +
                            "    = m." + ClsDatabaseCreation.COL_MODULE_ID +
                            " INNER JOIN " + ClsDatabaseCreation.TABLE_ROLES + " r" +
                            "   ON rp." + ClsDatabaseCreation.COL_PERM_USER_ID +
                            "    = r." + ClsDatabaseCreation.COL_ROLE_ID +
                            " WHERE m." + ClsDatabaseCreation.COL_MODULE_NAME + " = ?" +
                            "   AND r." + ClsDatabaseCreation.COL_ROLE_NAME   + " = ?" +
                            "   AND rp." + ClsDatabaseCreation.COL_PERM_ACCESS + " = 1",
                    new String[]{ moduleName, userRole }
            );

            if (cursor != null) {
                access = cursor.moveToFirst();
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }

        return access;
    }

    private void setCardListeners() {

        cardInventory.setOnClickListener(v -> navigateTo(InventoryActivity.class));

        cardMaintenance.setOnClickListener(v -> navigateTo(MaintenanceActivity.class));

//        cardLogs.setOnClickListener(v -> navigateTo(LogsActivity.class));

        cardSettings.setOnClickListener(v -> navigateTo(SettingsActivity.class));

        cardUserManagement.setOnClickListener(v -> navigateTo(UserManagementActivity.class));

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }


    //  NAVIGATE TO MODULE
    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(MainActivity.this, targetActivity);
        intent.putExtra("USER_ID",   userId);
        intent.putExtra("USER_NAME", userName);
        intent.putExtra("USER_ROLE", userRole);
        intent.putExtra("USERNAME",  username);
        startActivity(intent);
    }


    //  LOGOUT CONFIRMATION DIALOG
    private void showLogoutDialog() {
        new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    logActivity("LOGOUT", "User signed out");
                    redirectToLogin();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    //  LOG ACTIVITY TO DB
    private void logActivity(String action, String description) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(ClsDatabaseCreation.LOG_USER_ID,    userId);
            values.put(ClsDatabaseCreation.LOG_USERNAME,   username);
            values.put(ClsDatabaseCreation.LOG_ACTION,     action);
            values.put(ClsDatabaseCreation.LOG_MODULE,     "Dashboard");
            values.put(ClsDatabaseCreation.LOG_DESCRIPTION, description);
            db.insert(ClsDatabaseCreation.TABLE_ACTIVITY_LOG, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void animateCardsIn() {
        FrameLayout[] cards = { cardInventory, cardMaintenance, cardLogs, cardUserManagement, cardSettings };
        int delay = 80;

        for (int i = 0; i < cards.length; i++) {
            if (cards[i].getVisibility() != View.VISIBLE) continue;

            AnimationSet set = new AnimationSet(true);

            AlphaAnimation fade = new AlphaAnimation(0f, 1f);
            fade.setDuration(350);

            TranslateAnimation slide = new TranslateAnimation(0, 0, 40, 0);
            slide.setDuration(350);

            set.addAnimation(fade);
            set.addAnimation(slide);
            set.setStartOffset(i * delay);
            set.setFillAfter(true);

            cards[i].startAnimation(set);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showLogoutDialog();
    }
}