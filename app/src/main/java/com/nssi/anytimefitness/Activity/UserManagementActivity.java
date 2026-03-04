package com.nssi.anytimefitness.Activity;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.nssi.anytimefitness.Connection.ClsDatabaseCreation;
import com.nssi.anytimefitness.Connection.ClsUserManagement;
import com.nssi.anytimefitness.Connection.ClsUserManagement.UserModel;
import com.nssi.anytimefitness.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserManagementActivity extends AppCompatActivity {

    // ── Drawer ──
    private DrawerLayout drawerLayout;
    private FrameLayout btnOpenDrawer, btnCloseDrawer, btnBack;

    // ── Header ──
    private TextView tvUserCount;

    // ── Form ──
    private TextView tvFormTitle, btnClearForm;
    private EditText etFullName, etUsername, etEmail, etPassword, etConfirmPassword, etSearch;
    private Spinner spinnerRole;
    private Button btnSave, btnDelete;

    // ── Permission cards (whole card is clickable, checkbox is visual only) ──
    private LinearLayout permCardInventory, permCardMaintenance, permCardLogs, permCardUserMgmt;
    private CheckBox cbInventory, cbMaintenance, cbLogs, cbUserManagement;

    // ── List ──
    private ListView listViewUsers;
    private UserListAdapter listAdapter;
    private List<UserModel> userList;

    // ── Data ──
    private ClsUserManagement userManager;

    // ── Session ──
    private int sessionUserId;
    private String sessionUsername;

    // ── State ──
    private int selectedUserId = -1;

    private static final String MOD_INVENTORY       = "Inventory";
    private static final String MOD_MAINTENANCE     = "Maintenance";
    private static final String MOD_LOGS            = "Logs";
    private static final String MOD_USER_MANAGEMENT = "User Management";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(R.layout.activity_user_management);

        sessionUserId   = getIntent().getIntExtra("USER_ID", -1);
        sessionUsername = getIntent().getStringExtra("USERNAME");

        userManager = new ClsUserManagement(this);

        bindViews();
        setupDrawer();
        setupRoleSpinner();
        setupPermissionCards();
        setupSearch();
        setupButtons();
        loadUserList();
        setFormMode(false);
    }
    private void bindViews() {
        drawerLayout      = findViewById(R.id.drawerLayout);
        btnBack           = findViewById(R.id.btnBack);
        btnOpenDrawer     = findViewById(R.id.btnOpenDrawer);
        btnCloseDrawer    = findViewById(R.id.btnCloseDrawer);
        tvUserCount       = findViewById(R.id.tvUserCount);
        tvFormTitle       = findViewById(R.id.tvFormTitle);
        btnClearForm      = findViewById(R.id.btnClearForm);
        etSearch          = findViewById(R.id.etSearch);
        etFullName        = findViewById(R.id.etFullName);
        etUsername        = findViewById(R.id.etUsername);
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spinnerRole       = findViewById(R.id.spinnerRole);
        btnSave           = findViewById(R.id.btnSave);
        btnDelete         = findViewById(R.id.btnDelete);
        listViewUsers     = findViewById(R.id.listViewUsers);

        permCardInventory   = findViewById(R.id.permCardInventory);
        permCardMaintenance = findViewById(R.id.permCardMaintenance);
        permCardLogs        = findViewById(R.id.permCardLogs);
        permCardUserMgmt    = findViewById(R.id.permCardUserMgmt);

        cbInventory      = findViewById(R.id.cbInventory);
        cbMaintenance    = findViewById(R.id.cbMaintenance);
        cbLogs           = findViewById(R.id.cbLogs);
        cbUserManagement = findViewById(R.id.cbUserManagement);
    }

    private void setupDrawer() {
        // Disable swipe — only open via button
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        btnOpenDrawer.setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        btnCloseDrawer.setOnClickListener(v ->
                drawerLayout.closeDrawer(GravityCompat.START));

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRoleSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.spinner_item, new String[]{"Admin", "Staff"});
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
        spinnerRole.setSelection(1);
    }

    private void setupPermissionCards() {
        permCardInventory.setOnClickListener(v -> {
            cbInventory.setChecked(!cbInventory.isChecked());
            updateCardBackground(permCardInventory, cbInventory.isChecked());
        });
        permCardMaintenance.setOnClickListener(v -> {
            cbMaintenance.setChecked(!cbMaintenance.isChecked());
            updateCardBackground(permCardMaintenance, cbMaintenance.isChecked());
        });
        permCardLogs.setOnClickListener(v -> {
            cbLogs.setChecked(!cbLogs.isChecked());
            updateCardBackground(permCardLogs, cbLogs.isChecked());
        });
        permCardUserMgmt.setOnClickListener(v -> {
            cbUserManagement.setChecked(!cbUserManagement.isChecked());
            updateCardBackground(permCardUserMgmt, cbUserManagement.isChecked());
        });
    }

    private void updateCardBackground(LinearLayout card, boolean isChecked) {
        card.setBackgroundResource(isChecked
                ? R.drawable.bg_perm_card_on
                : R.drawable.bg_perm_card_off);
    }

    private void setPermissionCard(LinearLayout card, CheckBox cb, boolean checked) {
        cb.setChecked(checked);
        updateCardBackground(card, checked);
    }



    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                String q = s.toString().trim();
                userList = q.isEmpty() ? userManager.getAllUsers() : userManager.searchUsers(q);
                listAdapter.updateList(userList);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupButtons() {
        btnClearForm.setOnClickListener(v -> { clearForm(); setFormMode(false); });
        btnSave.setOnClickListener(v -> {
            if (selectedUserId == -1) handleCreateUser();
            else handleUpdateUser();
        });
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    //  LOAD LIST
    private void loadUserList() {
        userList = userManager.getAllUsers();
        tvUserCount.setText(String.valueOf(userList.size()));

        if (listAdapter == null) {
            listAdapter = new UserListAdapter(userList);
            listViewUsers.setAdapter(listAdapter);

            listViewUsers.setOnItemClickListener((parent, view, position, id) -> {
                loadUserIntoForm(userList.get(position).id);
                setFormMode(true);
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        } else {
            listAdapter.updateList(userList);
            tvUserCount.setText(String.valueOf(userList.size()));
        }
    }

    private void loadUserIntoForm(int userId) {
        UserModel user = userManager.getUserById(userId);
        if (user == null) return;

        selectedUserId = user.id;
        etFullName.setText(user.name);
        etUsername.setText(user.username);
        etEmail.setText(user.email != null ? user.email : "");
        etPassword.setText("");
        etConfirmPassword.setText("");

        String[] roles = {"Admin", "Staff"};
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(user.role)) { spinnerRole.setSelection(i); break; }
        }

        Map<String, Boolean> perms = userManager.getPermissionsForUser(userId);
        setPermissionCard(permCardInventory,   cbInventory,      Boolean.TRUE.equals(perms.get(MOD_INVENTORY)));
        setPermissionCard(permCardMaintenance, cbMaintenance,    Boolean.TRUE.equals(perms.get(MOD_MAINTENANCE)));
        setPermissionCard(permCardLogs,        cbLogs,           Boolean.TRUE.equals(perms.get(MOD_LOGS)));
        setPermissionCard(permCardUserMgmt,    cbUserManagement, Boolean.TRUE.equals(perms.get(MOD_USER_MANAGEMENT)));
    }

    //  CREATE / UPDATE / DELETE
    private void handleCreateUser() {
        if (!validateForm(false)) return;
        long id = userManager.createUser(buildUserModel(), buildPermissions());
        if (id != -1) {
            logActivity("CREATE", "Created user: " + etUsername.getText().toString().trim());
            Toast.makeText(this, "User created successfully.", Toast.LENGTH_SHORT).show();
            clearForm(); loadUserList();
        } else {
            Toast.makeText(this, "Failed. Username may already exist.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleUpdateUser() {
        if (!validateForm(true)) return;
        UserModel user = buildUserModel();
        user.id = selectedUserId;
        if (userManager.updateUser(user, buildPermissions())) {
            logActivity("UPDATE", "Updated user: " + user.username);
            Toast.makeText(this, "User updated.", Toast.LENGTH_SHORT).show();
            clearForm(); setFormMode(false); loadUserList();
        } else {
            Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation() {
        if (selectedUserId == sessionUserId) {
            Toast.makeText(this, "You cannot delete your own account.", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = etFullName.getText().toString().trim();
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Delete \"" + name + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    String uname = etUsername.getText().toString().trim();
                    if (userManager.deleteUser(selectedUserId)) {
                        logActivity("DELETE", "Deleted user: " + uname);
                        Toast.makeText(this, "User deleted.", Toast.LENGTH_SHORT).show();
                        clearForm(); setFormMode(false); loadUserList();
                    } else {
                        Toast.makeText(this, "Delete failed.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    //  VALIDATION
    private boolean validateForm(boolean isEdit) {
        String name     = etFullName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty())     { etFullName.setError("Required"); etFullName.requestFocus(); return false; }
        if (username.isEmpty()) { etUsername.setError("Required"); etUsername.requestFocus(); return false; }
        if (userManager.isUsernameTaken(username, isEdit ? selectedUserId : -1)) {
            etUsername.setError("Already exists"); etUsername.requestFocus(); return false;
        }
        if (!email.isEmpty() && userManager.isEmailTaken(email, isEdit ? selectedUserId : -1)) {
            etEmail.setError("Already in use"); etEmail.requestFocus(); return false;
        }
        if (!isEdit && password.isEmpty()) {
            etPassword.setError("Required"); etPassword.requestFocus(); return false;
        }
        if (!password.isEmpty() && !password.equals(confirm)) {
            etConfirmPassword.setError("Passwords do not match"); etConfirmPassword.requestFocus(); return false;
        }
        return true;
    }

    //  HELPERS
    private UserModel buildUserModel() {
        UserModel u = new UserModel();
        u.name     = etFullName.getText().toString().trim();
        u.username = etUsername.getText().toString().trim();
        u.email    = etEmail.getText().toString().trim();
        u.password = etPassword.getText().toString().trim();
        u.role     = spinnerRole.getSelectedItem().toString();
        return u;
    }

    private Map<String, Boolean> buildPermissions() {
        Map<String, Boolean> p = new HashMap<>();
        p.put(MOD_INVENTORY,       cbInventory.isChecked());
        p.put(MOD_MAINTENANCE,     cbMaintenance.isChecked());
        p.put(MOD_LOGS,            cbLogs.isChecked());
        p.put(MOD_USER_MANAGEMENT, cbUserManagement.isChecked());
        return p;
    }

    private void setFormMode(boolean isEdit) {
        tvFormTitle.setText(isEdit ? "EDIT USER" : "CREATE USER");
        btnSave.setText(isEdit ? "UPDATE USER" : "SAVE USER");
        btnDelete.setVisibility(isEdit ? View.VISIBLE : View.GONE);

        LinearLayout.LayoutParams saveParams = (LinearLayout.LayoutParams) btnSave.getLayoutParams();
        saveParams.weight = isEdit ? 1 : 2;
        btnSave.setLayoutParams(saveParams);

        selectedUserId = isEdit ? selectedUserId : -1;
    }

    private void clearForm() {
        etFullName.setText(""); etUsername.setText(""); etEmail.setText("");
        etPassword.setText(""); etConfirmPassword.setText("");
        spinnerRole.setSelection(1);
        setPermissionCard(permCardInventory,   cbInventory,      false);
        setPermissionCard(permCardMaintenance, cbMaintenance,    false);
        setPermissionCard(permCardLogs,        cbLogs,           false);
        setPermissionCard(permCardUserMgmt,    cbUserManagement, false);
        selectedUserId = -1;
    }

    private void logActivity(String action, String desc) {
        SQLiteDatabase db = new ClsDatabaseCreation(this).getWritableDatabase();
        try {
            ContentValues v = new ContentValues();
            v.put(ClsDatabaseCreation.LOG_USER_ID,    sessionUserId);
            v.put(ClsDatabaseCreation.LOG_USERNAME,   sessionUsername);
            v.put(ClsDatabaseCreation.LOG_ACTION,     action);
            v.put(ClsDatabaseCreation.LOG_MODULE,     "User Management");
            v.put(ClsDatabaseCreation.LOG_DESCRIPTION, desc);
            db.insert(ClsDatabaseCreation.TABLE_ACTIVITY_LOG, null, v);
        } finally { db.close(); }
    }

    //  BACK —
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    //  LIST ADAPTER
    private class UserListAdapter extends BaseAdapter {
        private List<UserModel> data;
        UserListAdapter(List<UserModel> data) { this.data = data; }
        void updateList(List<UserModel> d) { this.data = d; notifyDataSetChanged(); }
        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int p) { return data.get(p); }
        @Override public long getItemId(int p) { return data.get(p).id; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            ViewHolder h;
            if (convertView == null) {
                convertView = LayoutInflater.from(UserManagementActivity.this)
                        .inflate(R.layout.item_user, parent, false);
                h = new ViewHolder();
                h.tvInitial  = convertView.findViewById(R.id.tvUserInitial);
                h.tvName     = convertView.findViewById(R.id.tvItemUserName);
                h.tvUsername = convertView.findViewById(R.id.tvItemUsername);
                h.tvRole     = convertView.findViewById(R.id.tvItemRole);
                convertView.setTag(h);
            } else { h = (ViewHolder) convertView.getTag(); }

            UserModel u = data.get(pos);
            h.tvInitial.setText(u.getInitial());
            h.tvName.setText(u.name);
            h.tvUsername.setText("@" + u.username);
            h.tvRole.setText(u.role);
            return convertView;
        }
        class ViewHolder { TextView tvInitial, tvName, tvUsername, tvRole; }
    }
}