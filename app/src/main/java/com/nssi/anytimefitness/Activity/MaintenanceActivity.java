package com.nssi.anytimefitness.Activity;

import android.app.Dialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.nssi.anytimefitness.Connection.ClsDatabaseCreation;
import com.nssi.anytimefitness.Connection.ClsInventoryManagement;
import com.nssi.anytimefitness.Connection.ClsMaintenanceManagement;
import com.nssi.anytimefitness.Connection.ClsSettingsManagement;
import com.nssi.anytimefitness.R;

import java.util.Arrays;
import java.util.List;

public class MaintenanceActivity extends AppCompatActivity {

    // ── Header ──
    private TextView tvRecordCount;

    // ── Tabs ──
    private TextView tabAll, tabOpen, tabInProgress, tabCompleted, tabCancelled;
    private String currentFilter = null; // null = ALL

    // ── Action bar ──
    private EditText etSearch;
    private Button btnAddMaintenance;

    // ── List ──
    private ListView listViewMaintenance;
    private LinearLayout emptyState;
    private MaintenanceRowAdapter listAdapter;
    private List<ClsMaintenanceManagement.MaintenanceModel> recordList;

    // ── Data ──
    private ClsMaintenanceManagement maintenanceManager;
    private ClsInventoryManagement   inventoryManager;
    private ClsSettingsManagement    settingsManager;

    // ── Session ──
    private int    sessionUserId;
    private String sessionUserName, sessionUserRole, sessionUsername;

    // ── Status options ──
    private static final String[] STATUS_OPTIONS = { "Open", "In Progress", "Completed", "Cancelled" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(R.layout.activity_maintenance);

        sessionUserId   = getIntent().getIntExtra("USER_ID", -1);
        sessionUserName = getIntent().getStringExtra("USER_NAME");
        sessionUserRole = getIntent().getStringExtra("USER_ROLE");
        sessionUsername = getIntent().getStringExtra("USERNAME");

        maintenanceManager = new ClsMaintenanceManagement(this);
        inventoryManager   = new ClsInventoryManagement(this);
        settingsManager    = new ClsSettingsManagement(this);

        bindViews();
        setupTabs();
        setupSearch();
        loadList();
    }

    // ─────────────────────────────────────────────
    //  BIND
    // ─────────────────────────────────────────────
    private void bindViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvRecordCount      = findViewById(R.id.tvRecordCount);
        tabAll             = findViewById(R.id.tabAll);
        tabOpen            = findViewById(R.id.tabOpen);
        tabInProgress      = findViewById(R.id.tabInProgress);
        tabCompleted       = findViewById(R.id.tabCompleted);
        tabCancelled       = findViewById(R.id.tabCancelled);
        etSearch           = findViewById(R.id.etSearch);
        btnAddMaintenance  = findViewById(R.id.btnAddMaintenance);
        listViewMaintenance = findViewById(R.id.listViewMaintenance);
        emptyState         = findViewById(R.id.emptyState);

        btnAddMaintenance.setOnClickListener(v -> showAddEditDialog(null));
    }

    // ─────────────────────────────────────────────
    //  TABS
    // ─────────────────────────────────────────────
    private void setupTabs() {
        tabAll.setOnClickListener(v        -> selectTab(null,          tabAll));
        tabOpen.setOnClickListener(v       -> selectTab("Open",        tabOpen));
        tabInProgress.setOnClickListener(v -> selectTab("In Progress", tabInProgress));
        tabCompleted.setOnClickListener(v  -> selectTab("Completed",   tabCompleted));
        tabCancelled.setOnClickListener(v  -> selectTab("Cancelled",   tabCancelled));
    }

    private void selectTab(String filter, TextView selected) {
        currentFilter = filter;
        etSearch.setText("");

        TextView[] tabs = { tabAll, tabOpen, tabInProgress, tabCompleted, tabCancelled };
        for (TextView t : tabs) {
            t.setBackgroundResource(R.drawable.bg_perm_card_off);
            t.setTextColor(0xFF7743DB);
        }
        selected.setBackgroundResource(R.drawable.bg_button_primary);
        selected.setTextColor(0xFFFFFFFF);

        loadList();
    }

    // ─────────────────────────────────────────────
    //  SEARCH
    // ─────────────────────────────────────────────
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                String q = s.toString().trim();
                recordList = q.isEmpty() ? getFilteredList() : maintenanceManager.search(q);
                if (listAdapter != null) listAdapter.updateList(recordList);
                updateEmptyState();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ─────────────────────────────────────────────
    //  LOAD LIST
    // ─────────────────────────────────────────────
    private void loadList() {
        recordList = getFilteredList();
        tvRecordCount.setText(String.valueOf(maintenanceManager.getAllRecords().size()));

        if (listAdapter == null) {
            listAdapter = new MaintenanceRowAdapter(recordList);
            listViewMaintenance.setAdapter(listAdapter);
            listViewMaintenance.setOnItemClickListener((parent, view, pos, id) ->
                    showAddEditDialog(recordList.get(pos)));
            listViewMaintenance.setOnItemLongClickListener((parent, view, pos, id) -> {
                confirmDelete(recordList.get(pos));
                return true;
            });
        } else {
            listAdapter.updateList(recordList);
        }
        updateEmptyState();
    }

    private List<ClsMaintenanceManagement.MaintenanceModel> getFilteredList() {
        return currentFilter == null
                ? maintenanceManager.getAllRecords()
                : maintenanceManager.getByStatus(currentFilter);
    }

    private void updateEmptyState() {
        boolean empty = recordList == null || recordList.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        listViewMaintenance.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ═══════════════════════════════════════════════════════════
    //  ADD / EDIT DIALOG
    // ═══════════════════════════════════════════════════════════
    private void showAddEditDialog(ClsMaintenanceManagement.MaintenanceModel existing) {
        Dialog dialog = new Dialog(this, R.style.DarkAlertDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_maintenance);
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.94),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        boolean isEdit = existing != null;

        // ── Title ──
        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        tvTitle.setText(isEdit ? "EDIT MAINTENANCE RECORD" : "ADD MAINTENANCE RECORD");

        // ── Scan section (hide when editing — asset already linked) ──
        EditText etScanInput  = dialog.findViewById(R.id.etScanInput);
        FrameLayout btnScan   = dialog.findViewById(R.id.btnScan);
        TextView tvScanError  = dialog.findViewById(R.id.tvScanError);

        // ── Asset read-only fields ──
        TextView tvAssetName  = dialog.findViewById(R.id.tvAssetName);
        TextView tvClub       = dialog.findViewById(R.id.tvClub);
        TextView tvTypeAsset  = dialog.findViewById(R.id.tvTypeOfAsset);
        TextView tvMakeBrand  = dialog.findViewById(R.id.tvMakeBrand);
        TextView tvModel      = dialog.findViewById(R.id.tvModel);
        TextView tvSerial     = dialog.findViewById(R.id.tvSerialNumber);
        TextView tvAssetTag   = dialog.findViewById(R.id.tvAssetTagNo);
        TextView tvBarCode    = dialog.findViewById(R.id.tvBarCode);

        // ── Service editable fields ──
        EditText etDateOfService   = dialog.findViewById(R.id.etDateOfService);
        Spinner  spinnerServiceType = dialog.findViewById(R.id.spinnerServiceType);
        Spinner  spinnerStatus     = dialog.findViewById(R.id.spinnerStatus);
        EditText etIssueDetails    = dialog.findViewById(R.id.etIssueDetails);
        EditText etDiagnosis       = dialog.findViewById(R.id.etDiagnosis);
        EditText etMaterialsUsed   = dialog.findViewById(R.id.etMaterialsUsed);
        EditText etSolutionApplied = dialog.findViewById(R.id.etSolutionApplied);
        EditText etDateCompleted   = dialog.findViewById(R.id.etDateCompleted);
        Button   btnSave           = dialog.findViewById(R.id.btnDialogSave);
        Button   btnCancel         = dialog.findViewById(R.id.btnDialogCancel);

        // ── Populate Service Type spinner from DB ──
        List<ClsSettingsManagement.TypeModel> serviceTypes = settingsManager.getAllServiceTypes();
        String[] serviceTypeNames = new String[serviceTypes.size() + 1];
        serviceTypeNames[0] = "— Select Service Type —";
        for (int i = 0; i < serviceTypes.size(); i++) serviceTypeNames[i + 1] = serviceTypes.get(i).name;
        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, serviceTypeNames);
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerServiceType.setAdapter(serviceAdapter);

        // ── Populate Status spinner ──
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, STATUS_OPTIONS);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        // ── Helper to fill asset read-only fields ──
        final ClsInventoryManagement.AssetModel[] linkedAsset = { null };

        Runnable fillAssetFields = () -> {
            ClsInventoryManagement.AssetModel a = linkedAsset[0];
            if (a == null) return;
            tvAssetName.setText(a.asset   != null ? a.asset      : "—");
            tvClub.setText(a.club         != null ? a.club        : "—");
            tvTypeAsset.setText(a.typeOfAsset != null ? a.typeOfAsset : "—");
            tvMakeBrand.setText(a.makeBrand  != null ? a.makeBrand  : "—");
            tvModel.setText(a.model         != null ? a.model       : "—");
            tvSerial.setText(a.serialNumber != null ? a.serialNumber : "—");
            tvAssetTag.setText(a.assetTagNo != null && !a.assetTagNo.isEmpty() ? a.assetTagNo : "—");
            tvBarCode.setText(a.barCode     != null && !a.barCode.isEmpty()    ? a.barCode    : "—");
        };

        // ── EDIT MODE: pre-fill everything ──
        if (isEdit) {
            etScanInput.setVisibility(View.GONE);
            btnScan.setVisibility(View.GONE);

            // Fill asset fields from existing record
            tvAssetName.setText(existing.asset        != null ? existing.asset        : "—");
            tvClub.setText(existing.club              != null ? existing.club          : "—");
            tvTypeAsset.setText(existing.typeOfAsset  != null ? existing.typeOfAsset  : "—");
            tvMakeBrand.setText(existing.makeBrand    != null ? existing.makeBrand    : "—");
            tvModel.setText(existing.model            != null ? existing.model         : "—");
            tvSerial.setText(existing.serialNumber    != null ? existing.serialNumber  : "—");
            tvAssetTag.setText(existing.assetTagNo    != null ? existing.assetTagNo    : "—");
            tvBarCode.setText(existing.barCode        != null ? existing.barCode       : "—");

            // Fill service fields
            etDateOfService.setText(existing.dateOfService   != null ? existing.dateOfService   : "");
            etIssueDetails.setText(existing.issueDetails     != null ? existing.issueDetails     : "");
            etDiagnosis.setText(existing.diagnosis           != null ? existing.diagnosis         : "");
            etMaterialsUsed.setText(existing.materialsUsed   != null ? existing.materialsUsed   : "");
            etSolutionApplied.setText(existing.solutionApplied != null ? existing.solutionApplied : "");
            etDateCompleted.setText(existing.dateCompleted   != null ? existing.dateCompleted   : "");

            // Pre-select service type spinner
            if (existing.typeOfService != null) {
                for (int i = 0; i < serviceTypeNames.length; i++) {
                    if (serviceTypeNames[i].equals(existing.typeOfService)) {
                        spinnerServiceType.setSelection(i);
                        break;
                    }
                }
            }

            // Pre-select status spinner
            if (existing.status != null) {
                for (int i = 0; i < STATUS_OPTIONS.length; i++) {
                    if (STATUS_OPTIONS[i].equals(existing.status)) {
                        spinnerStatus.setSelection(i);
                        break;
                    }
                }
            }

            btnSave.setText("UPDATE RECORD");
        }

        // ── SCAN LOGIC (add mode only) ──
        Runnable performScan = () -> {
            String input = etScanInput.getText().toString().trim();
            if (input.isEmpty()) return;

            tvScanError.setVisibility(View.GONE);

            // Try barcode first, then asset tag
            ClsInventoryManagement.AssetModel found = inventoryManager.findByBarcode(input);
            if (found == null) found = inventoryManager.findByAssetTag(input);

            if (found == null) {
                tvScanError.setVisibility(View.VISIBLE);
                linkedAsset[0] = null;
                // Clear asset fields
                tvAssetName.setText("—"); tvClub.setText("—"); tvTypeAsset.setText("—");
                tvMakeBrand.setText("—"); tvModel.setText("—"); tvSerial.setText("—");
                tvAssetTag.setText("—"); tvBarCode.setText("—");
            } else {
                linkedAsset[0] = found;
                fillAssetFields.run();
            }
        };

        etScanInput.setOnEditorActionListener((v, actionId, event) -> {
            performScan.run();
            return true;
        });

        btnScan.setOnClickListener(v -> performScan.run());

        // ── CANCEL ──
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // ── SAVE ──
        btnSave.setOnClickListener(v -> {
            // Validate asset linked (add mode)
            if (!isEdit && linkedAsset[0] == null) {
                tvScanError.setText("Please scan a valid asset before saving.");
                tvScanError.setVisibility(View.VISIBLE);
                return;
            }

            // Validate required service fields
            String dateOfService = etDateOfService.getText().toString().trim();
            if (dateOfService.isEmpty()) {
                etDateOfService.setError("Date of service is required");
                etDateOfService.requestFocus();
                return;
            }

            int serviceTypePos = spinnerServiceType.getSelectedItemPosition();
            if (serviceTypePos == 0) {
                Toast.makeText(this, "Please select a service type.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Build model
            ClsMaintenanceManagement.MaintenanceModel m = new ClsMaintenanceManagement.MaintenanceModel();

            if (isEdit) {
                m.id           = existing.id;
                // Keep asset info from existing
                m.club         = existing.club;
                m.assignedType = existing.assignedType;
                m.typeOfAsset  = existing.typeOfAsset;
                m.asset        = existing.asset;
                m.makeBrand    = existing.makeBrand;
                m.model        = existing.model;
                m.serialNumber = existing.serialNumber;
                m.assetTagNo   = existing.assetTagNo;
                m.barCode      = existing.barCode;
            } else {
                ClsInventoryManagement.AssetModel a = linkedAsset[0];
                m.club         = a.club;
                m.assignedType = a.assignedType;
                m.typeOfAsset  = a.typeOfAsset;
                m.asset        = a.asset;
                m.makeBrand    = a.makeBrand;
                m.model        = a.model;
                m.serialNumber = a.serialNumber;
                m.assetTagNo   = a.assetTagNo;
                m.barCode      = a.barCode;
            }

            m.dateOfService   = dateOfService;
            m.typeOfService   = serviceTypeNames[serviceTypePos];
            m.status          = STATUS_OPTIONS[spinnerStatus.getSelectedItemPosition()];
            m.issueDetails    = etIssueDetails.getText().toString().trim();
            m.diagnosis       = etDiagnosis.getText().toString().trim();
            m.materialsUsed   = etMaterialsUsed.getText().toString().trim();
            m.solutionApplied = etSolutionApplied.getText().toString().trim();
            m.dateCompleted   = etDateCompleted.getText().toString().trim();

            boolean success;
            if (isEdit) {
                success = maintenanceManager.updateServiceDetails(m);
                if (success) logActivity("UPDATE", "Updated maintenance record for: " + m.asset);
            } else {
                success = maintenanceManager.createRecord(m) != -1;
                if (success) logActivity("CREATE", "Created maintenance record for: " + m.asset);
            }

            if (success) {
                Toast.makeText(this,
                        isEdit ? "Record updated." : "Record saved.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadList();
            } else {
                Toast.makeText(this, "Failed to save record.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();

        // Auto-focus scan input in add mode
        if (!isEdit) etScanInput.requestFocus();
    }

    // ─────────────────────────────────────────────
    //  DELETE CONFIRMATION
    // ─────────────────────────────────────────────
    private void confirmDelete(ClsMaintenanceManagement.MaintenanceModel item) {
        new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("Delete Record")
                .setMessage("Delete maintenance record for \"" + item.asset + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    if (maintenanceManager.deleteRecord(item.id)) {
                        logActivity("DELETE", "Deleted maintenance record for: " + item.asset);
                        Toast.makeText(this, "Record deleted.", Toast.LENGTH_SHORT).show();
                        loadList();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─────────────────────────────────────────────
    //  LOG ACTIVITY
    // ─────────────────────────────────────────────
    private void logActivity(String action, String desc) {
        SQLiteDatabase db = new ClsDatabaseCreation(this).getWritableDatabase();
        try {
            ContentValues v = new ContentValues();
            v.put(ClsDatabaseCreation.LOG_USER_ID,     sessionUserId);
            v.put(ClsDatabaseCreation.LOG_USERNAME,    sessionUsername);
            v.put(ClsDatabaseCreation.LOG_ACTION,      action);
            v.put(ClsDatabaseCreation.LOG_MODULE,      "Maintenance");
            v.put(ClsDatabaseCreation.LOG_DESCRIPTION, desc);
            db.insert(ClsDatabaseCreation.TABLE_ACTIVITY_LOG, null, v);
        } finally { db.close(); }
    }

    // ─────────────────────────────────────────────
    //  LIST ADAPTER
    // ─────────────────────────────────────────────
    private class MaintenanceRowAdapter extends BaseAdapter {
        private List<ClsMaintenanceManagement.MaintenanceModel> data;

        MaintenanceRowAdapter(List<ClsMaintenanceManagement.MaintenanceModel> data) { this.data = data; }
        void updateList(List<ClsMaintenanceManagement.MaintenanceModel> d) { this.data = d; notifyDataSetChanged(); }

        @Override public int getCount()          { return data.size(); }
        @Override public Object getItem(int p)   { return data.get(p); }
        @Override public long getItemId(int p)   { return data.get(p).id; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            ViewHolder h;
            if (convertView == null) {
                convertView = LayoutInflater.from(MaintenanceActivity.this)
                        .inflate(R.layout.item_maintenance_row, parent, false);
                h = new ViewHolder();
                h.tvAsset       = convertView.findViewById(R.id.tvRowAsset);
                h.tvServiceType = convertView.findViewById(R.id.tvRowServiceType);
                h.tvDate        = convertView.findViewById(R.id.tvRowDate);
                h.tvClub        = convertView.findViewById(R.id.tvRowClub);
                h.tvStatus      = convertView.findViewById(R.id.tvRowStatus);
                convertView.setTag(h);
            } else { h = (ViewHolder) convertView.getTag(); }

            ClsMaintenanceManagement.MaintenanceModel m = data.get(pos);
            h.tvAsset.setText(m.asset           != null ? m.asset           : "—");
            h.tvServiceType.setText(m.typeOfService != null ? m.typeOfService : "—");
            h.tvDate.setText(m.dateOfService    != null ? m.dateOfService   : "—");
            h.tvClub.setText(m.club             != null ? m.club            : "—");

            // Status badge color
            String status = m.status != null ? m.status : "Open";
            h.tvStatus.setText(status.toUpperCase());
            switch (status) {
                case "Completed":
                    h.tvStatus.setTextColor(0xFF7743DB);
                    h.tvStatus.setBackgroundResource(R.drawable.bg_role_badge);
                    break;
                case "In Progress":
                    h.tvStatus.setTextColor(0xFFFFA500);
                    h.tvStatus.setBackgroundResource(R.drawable.bg_perm_card_off);
                    break;
                case "Cancelled":
                    h.tvStatus.setTextColor(0xFFFF6B6B);
                    h.tvStatus.setBackgroundResource(R.drawable.bg_btn_danger);
                    break;
                default: // Open
                    h.tvStatus.setTextColor(0xFF443355);
                    h.tvStatus.setBackgroundResource(R.drawable.bg_perm_card_off);
                    break;
            }

            convertView.setBackgroundResource(pos % 2 == 0
                    ? R.drawable.bg_user_list_item
                    : android.R.color.transparent);

            return convertView;
        }

        class ViewHolder {
            TextView tvAsset, tvServiceType, tvDate, tvClub, tvStatus;
        }
    }
}