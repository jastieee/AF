package com.nssi.anytimefitness.Activity;

import android.app.Dialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.nssi.anytimefitness.Connection.ClsDatabaseCreation;
import com.nssi.anytimefitness.Connection.ClsSettingsManagement;
import com.nssi.anytimefitness.R;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    // ── Session ──
    private int userId;
    private String userName, userRole, username;

    // ── DB ──
    private ClsSettingsManagement settingsDb;
    private ClsDatabaseCreation   dbHelper;

    // ── UI ──
    private FrameLayout btnBack;
    private LinearLayout tabAssetType, tabServiceType;
    private View         tabAssetTypeIndicator, tabServiceTypeIndicator;
    private TextView     tvTabAssetType, tvTabServiceType;
    private EditText     etSearch;
    private FrameLayout  btnAdd;
    private ListView     listView;
    private LinearLayout emptyState;
    private TextView     tvEmptyLabel;

    // ── State ──
    private static final int TAB_ASSET   = 0;
    private static final int TAB_SERVICE = 1;
    private int currentTab = TAB_ASSET;

    private List<ClsSettingsManagement.TypeModel> currentList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(R.layout.activity_settings);

        userId   = getIntent().getIntExtra("USER_ID", -1);
        userName = getIntent().getStringExtra("USER_NAME");
        userRole = getIntent().getStringExtra("USER_ROLE");
        username = getIntent().getStringExtra("USERNAME");

        settingsDb = new ClsSettingsManagement(this);
        dbHelper   = new ClsDatabaseCreation(this);

        bindViews();
        setupTabs();
        setupSearch();
        setupAddButton();
        selectTab(TAB_ASSET);
    }

//      BIND
    private void bindViews() {
        btnBack                 = findViewById(R.id.btnBack);
        tabAssetType            = findViewById(R.id.tabAssetType);
        tabServiceType          = findViewById(R.id.tabServiceType);
        tabAssetTypeIndicator   = findViewById(R.id.tabAssetTypeIndicator);
        tabServiceTypeIndicator = findViewById(R.id.tabServiceTypeIndicator);
        tvTabAssetType          = findViewById(R.id.tvTabAssetType);
        tvTabServiceType        = findViewById(R.id.tvTabServiceType);
        etSearch                = findViewById(R.id.etSearch);
        btnAdd                  = findViewById(R.id.btnAdd);
        listView                = findViewById(R.id.listView);
        emptyState              = findViewById(R.id.emptyState);
        tvEmptyLabel            = findViewById(R.id.tvEmptyLabel);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupTabs() {
        tabAssetType.setOnClickListener(v -> {
            etSearch.setText("");
            selectTab(TAB_ASSET);
        });
        tabServiceType.setOnClickListener(v -> {
            etSearch.setText("");
            selectTab(TAB_SERVICE);
        });
    }

    private void selectTab(int tab) {
        currentTab = tab;

        boolean isAsset = (tab == TAB_ASSET);

        // Active tab: full colour + underline; inactive: muted + transparent underline
        tvTabAssetType.setTextColor(isAsset ? 0xFF7743DB : 0x80443355);
        tvTabServiceType.setTextColor(isAsset ? 0x80443355 : 0xFF7743DB);
        tabAssetTypeIndicator.setBackgroundColor(isAsset ? 0xFF7743DB : 0x00000000);
        tabServiceTypeIndicator.setBackgroundColor(isAsset ? 0x00000000 : 0xFF7743DB);

        loadList();
    }


    //  SEARCH
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { loadList(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupAddButton() {
        btnAdd.setOnClickListener(v -> showAddEditDialog(null));
    }

    private void loadList() {
        String query = etSearch.getText().toString().trim();
        boolean hasQuery = !query.isEmpty();

        if (currentTab == TAB_ASSET) {
            currentList = hasQuery
                    ? settingsDb.searchAssetTypes(query)
                    : settingsDb.getAllAssetTypes();
        } else {
            currentList = hasQuery
                    ? settingsDb.searchServiceTypes(query)
                    : settingsDb.getAllServiceTypes();
        }

        boolean isEmpty = currentList.isEmpty();
        listView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        tvEmptyLabel.setText(hasQuery ? "No results for \"" + query + "\"" : "No types added yet");

        listView.setAdapter(new TypeAdapter());
    }

    //  ADD / EDIT DIALOG
    private void showAddEditDialog(ClsSettingsManagement.TypeModel editTarget) {
        Dialog dialog = new Dialog(this, R.style.DarkAlertDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_type);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88f),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView    tvTitle    = dialog.findViewById(R.id.tvDialogTitle);
        TextView    tvSubtitle = dialog.findViewById(R.id.tvDialogSubtitle);
        EditText    etName     = dialog.findViewById(R.id.etTypeName);
        EditText    etDescrip  = dialog.findViewById(R.id.etTypeDescrip);
        FrameLayout btnCancel  = dialog.findViewById(R.id.btnCancel);
        FrameLayout btnSave    = dialog.findViewById(R.id.btnSave);

        boolean isEdit = (editTarget != null);
        String  label  = (currentTab == TAB_ASSET) ? "ASSET TYPE" : "SERVICE TYPE";

        tvTitle.setText(isEdit ? "EDIT " + label : "ADD " + label);
        tvSubtitle.setText(isEdit ? "Update the details below" : "Fill in the details below");

        if (isEdit) {
            etName.setText(editTarget.name);
            etDescrip.setText(editTarget.description);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name    = etName.getText().toString().trim();
            String descrip = etDescrip.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Name is required");
                etName.requestFocus();
                return;
            }

            boolean success;
            if (isEdit) {
                success = (currentTab == TAB_ASSET)
                        ? settingsDb.updateAssetType(editTarget.id, name, descrip)
                        : settingsDb.updateServiceType(editTarget.id, name, descrip);
                if (success) logActivity("UPDATE", label + " updated: " + name);
            } else {
                long newId = (currentTab == TAB_ASSET)
                        ? settingsDb.addAssetType(name, descrip)
                        : settingsDb.addServiceType(name, descrip);
                success = (newId != -1);
                if (success) logActivity("ADD", label + " added: " + name);
            }

            if (success) {
                Toast.makeText(this,
                        label + (isEdit ? " updated." : " added."), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadList();
            } else {
                Toast.makeText(this,
                        "Name already exists or an error occurred.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void confirmDelete(ClsSettingsManagement.TypeModel item) {
        String label = (currentTab == TAB_ASSET) ? "asset type" : "service type";

        // Warn if the type is still referenced
        boolean inUse = (currentTab == TAB_ASSET)
                ? settingsDb.isAssetTypeInUse(item.name)
                : settingsDb.isServiceTypeInUse(item.name);

        String message = inUse
                ? "\"" + item.name + "\" is currently in use. Deleting it will not remove existing records. Continue?"
                : "Delete " + label + " \"" + item.name + "\"?";

        new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("Delete " + label.substring(0, 1).toUpperCase() + label.substring(1))
                .setMessage(message)
                .setPositiveButton("Delete", (d, w) -> {
                    boolean deleted = (currentTab == TAB_ASSET)
                            ? settingsDb.deleteAssetType(item.id)
                            : settingsDb.deleteServiceType(item.id);
                    if (deleted) {
                        logActivity("DELETE", label + " deleted: " + item.name);
                        Toast.makeText(this, label + " deleted.", Toast.LENGTH_SHORT).show();
                        loadList();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    //  ADAPTER
    private class TypeAdapter extends BaseAdapter {

        @Override public int getCount()              { return currentList.size(); }
        @Override public Object getItem(int pos)     { return currentList.get(pos); }
        @Override public long getItemId(int pos)     { return currentList.get(pos).id; }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_settings_type, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ClsSettingsManagement.TypeModel item = currentList.get(position);
            holder.tvInitial.setText(item.getInitial());
            holder.tvName.setText(item.name);
            holder.tvDescrip.setText(
                    (item.description != null && !item.description.isEmpty())
                            ? item.description : "No description");

            holder.btnEdit.setOnClickListener(v -> showAddEditDialog(item));
            holder.btnDelete.setOnClickListener(v -> confirmDelete(item));

            // Subtle fade-in
            AlphaAnimation fade = new AlphaAnimation(0f, 1f);
            fade.setDuration(250);
            fade.setStartOffset(position * 40L);
            convertView.startAnimation(fade);

            return convertView;
        }

        class ViewHolder {
            TextView    tvInitial, tvName, tvDescrip;
            FrameLayout btnEdit, btnDelete;

            ViewHolder(View v) {
                tvInitial  = v.findViewById(R.id.tvInitial);
                tvName     = v.findViewById(R.id.tvTypeName);
                tvDescrip  = v.findViewById(R.id.tvTypeDescrip);
                btnEdit    = v.findViewById(R.id.btnEdit);
                btnDelete  = v.findViewById(R.id.btnDelete);
            }
        }
    }

    //  ACTIVITY LOG
    private void logActivity(String action, String description) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(ClsDatabaseCreation.LOG_USER_ID,    userId);
            values.put(ClsDatabaseCreation.LOG_USERNAME,   username);
            values.put(ClsDatabaseCreation.LOG_ACTION,     action);
            values.put(ClsDatabaseCreation.LOG_MODULE,     "Settings");
            values.put(ClsDatabaseCreation.LOG_DESCRIPTION, description);
            db.insert(ClsDatabaseCreation.TABLE_ACTIVITY_LOG, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }
}