package com.nssi.anytimefitness.Activity;

import android.app.Dialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nssi.anytimefitness.Connection.ClsSettingsManagement;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.nssi.anytimefitness.Connection.ClsDatabaseCreation;
import com.nssi.anytimefitness.Connection.ClsInventoryManagement;
import com.nssi.anytimefitness.Connection.ClsInventoryManagement.AssetModel;
import com.nssi.anytimefitness.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    // ── Header ──
    private TextView tvAssetCount;

    // ── Action bar ──
    private EditText etSearch;
    private Button btnUploadCsv, btnRegisterTag, btnAddItem;

    // ── List ──
    private ListView listViewAssets;
    private LinearLayout emptyState;
    private AssetRowAdapter listAdapter;
    private List<AssetModel> assetList;

    // ── Data ──
    private ClsInventoryManagement inventoryManager;

    // ── Session ──
    private int sessionUserId;
    private String sessionUsername;

    // ── CSV file picker ──
    private ActivityResultLauncher<String[]> csvPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(R.layout.activity_inventory);

        sessionUserId   = getIntent().getIntExtra("USER_ID", -1);
        sessionUsername = getIntent().getStringExtra("USERNAME");

        inventoryManager = new ClsInventoryManagement(this);

        csvPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> { if (uri != null) processCsvFile(uri); });

        bindViews();
        setupSearch();
        setupButtons();
        loadAssetList();
    }


    private void bindViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvAssetCount   = findViewById(R.id.tvAssetCount);
        etSearch       = findViewById(R.id.etSearch);
        btnUploadCsv   = findViewById(R.id.btnUploadCsv);
        btnRegisterTag = findViewById(R.id.btnRegisterTag);
        btnAddItem     = findViewById(R.id.btnAddItem);
        listViewAssets = findViewById(R.id.listViewAssets);
        emptyState     = findViewById(R.id.emptyState);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                String q = s.toString().trim();
                assetList = q.isEmpty()
                        ? inventoryManager.getAllAssets()
                        : inventoryManager.searchAssets(q);
                listAdapter.updateList(assetList);
                updateEmptyState();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupButtons() {
        btnUploadCsv.setOnClickListener(v ->
                csvPickerLauncher.launch(new String[]{"text/csv", "text/comma-separated-values", "*/*"}));
        btnAddItem.setOnClickListener(v -> showAddItemDialog(null));
        btnRegisterTag.setOnClickListener(v -> showRegisterTagDialog());
    }

    private void loadAssetList() {
        assetList = inventoryManager.getAllAssets();
        tvAssetCount.setText(String.valueOf(assetList.size()));

        if (listAdapter == null) {
            listAdapter = new AssetRowAdapter(assetList);
            listViewAssets.setAdapter(listAdapter);
            listViewAssets.setOnItemClickListener((parent, view, position, id) ->
                    showAddItemDialog(assetList.get(position)));
        } else {
            listAdapter.updateList(assetList);
            tvAssetCount.setText(String.valueOf(assetList.size()));
        }
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean empty = assetList == null || assetList.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        listViewAssets.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showAddItemDialog(AssetModel existing) {
        Dialog dialog = new Dialog(this, R.style.DarkAlertDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_item);
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // ── Bind dialog views — no etDialogClub, club assigned handles both ──
        EditText etBarCode          = dialog.findViewById(R.id.etDialogBarCode);
        EditText etAssetTagNo       = dialog.findViewById(R.id.etDialogAssetTagNo);
        EditText etAsset            = dialog.findViewById(R.id.etDialogAsset);
        EditText etAssignedType     = dialog.findViewById(R.id.etDialogAssignedType);
        Spinner  spinnerTypeOfAsset = dialog.findViewById(R.id.spinnerTypeOfAsset);
        EditText etMakeBrand        = dialog.findViewById(R.id.etDialogMakeBrand);
        EditText etModel            = dialog.findViewById(R.id.etDialogModel);
        EditText etSerialNumber     = dialog.findViewById(R.id.etDialogSerialNumber);
        EditText etDateDelivered    = dialog.findViewById(R.id.etDialogDateDelivered);
        EditText etPurchasePrice    = dialog.findViewById(R.id.etDialogPurchasePrice);
        EditText etLocation         = dialog.findViewById(R.id.etDialogLocation);
        Button   btnSave            = dialog.findViewById(R.id.btnDialogSave);
        Button   btnCancel          = dialog.findViewById(R.id.btnDialogCancel);

        // ── Populate Type of Asset spinner from DB ──
        ClsSettingsManagement settingsMgr = new ClsSettingsManagement(this);
        List<ClsSettingsManagement.TypeModel> assetTypes = settingsMgr.getAllAssetTypes();
        List<String> assetTypeNames = new ArrayList<>();
        assetTypeNames.add("— Select Type —");
        for (ClsSettingsManagement.TypeModel t : assetTypes) assetTypeNames.add(t.name);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, assetTypeNames);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeOfAsset.setAdapter(typeAdapter);

        // ── Pre-fill if editing ──
        boolean isEdit = existing != null;
        if (isEdit) {
            etBarCode.setText(existing.barCode != null ? existing.barCode : "");
            etAssetTagNo.setText(existing.assetTagNo != null ? existing.assetTagNo : "");
            etAsset.setText(existing.asset != null ? existing.asset : "");

            // club assigned: prefer assignedType, fallback to club
            String clubValue = (existing.assignedType != null && !existing.assignedType.isEmpty())
                    ? existing.assignedType
                    : (existing.club != null ? existing.club : "");
            etAssignedType.setText(clubValue);

            if (existing.typeOfAsset != null) {
                int idx = assetTypeNames.indexOf(existing.typeOfAsset);
                if (idx >= 0) spinnerTypeOfAsset.setSelection(idx);
            }
            etMakeBrand.setText(existing.makeBrand != null ? existing.makeBrand : "");
            etModel.setText(existing.model != null ? existing.model : "");
            etSerialNumber.setText(existing.serialNumber != null ? existing.serialNumber : "");
            etDateDelivered.setText(existing.dateDelivered != null ? existing.dateDelivered : "");
            etPurchasePrice.setText(existing.purchasePrice > 0 ? String.valueOf(existing.purchasePrice) : "");
            etLocation.setText(existing.location != null ? existing.location : "");
            btnSave.setText("UPDATE ASSET");
        }

        // ── Zebra barcode scan ──
        etBarCode.setOnEditorActionListener((v, actionId, event) -> {
            String bc = etBarCode.getText().toString().trim();
            if (!bc.isEmpty() && !isEdit) {
                AssetModel found = inventoryManager.findByBarcode(bc);
                if (found != null) {
                    dialog.dismiss();
                    showAddItemDialog(found);
                }
            }
            return false;
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String assetName = etAsset.getText().toString().trim();
            if (assetName.isEmpty()) {
                etAsset.setError("Asset name is required");
                etAsset.requestFocus();
                return;
            }

            AssetModel a    = new AssetModel();
            a.barCode       = etBarCode.getText().toString().trim();
            a.assetTagNo    = etAssetTagNo.getText().toString().trim();
            a.asset         = assetName;
            // club assigned = club — both fields get the same value
            a.assignedType  = etAssignedType.getText().toString().trim();
            a.club          = a.assignedType;
            int typePos     = spinnerTypeOfAsset.getSelectedItemPosition();
            a.typeOfAsset   = (typePos > 0) ? assetTypeNames.get(typePos) : "";
            a.makeBrand     = etMakeBrand.getText().toString().trim();
            a.model         = etModel.getText().toString().trim();
            a.serialNumber  = etSerialNumber.getText().toString().trim();
            a.dateDelivered = etDateDelivered.getText().toString().trim();
            String priceStr = etPurchasePrice.getText().toString().trim();
            a.purchasePrice = priceStr.isEmpty() ? 0 : Double.parseDouble(priceStr);
            a.location      = etLocation.getText().toString().trim();

            if (isEdit) {
                a.id = existing.id;
                if (inventoryManager.updateAsset(a)) {
                    logActivity("UPDATE", "Updated asset: " + a.asset);
                    Toast.makeText(this, "Asset updated.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadAssetList();
                } else {
                    Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (inventoryManager.createAsset(a) != -1) {
                    logActivity("CREATE", "Added asset: " + a.asset);
                    Toast.makeText(this, "Asset saved.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadAssetList();
                } else {
                    Toast.makeText(this, "Failed to save.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ── Long press = delete ──
        listViewAssets.setOnItemLongClickListener((parent, view, position, id) -> {
            AssetModel target = assetList.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Delete Asset")
                    .setMessage("Delete \"" + target.asset + "\"? This cannot be undone.")
                    .setPositiveButton("Delete", (d, w) -> {
                        if (inventoryManager.deleteAsset(target.id)) {
                            logActivity("DELETE", "Deleted asset: " + target.asset);
                            Toast.makeText(this, "Asset deleted.", Toast.LENGTH_SHORT).show();
                            loadAssetList();
                        }
                    })
                    .setNegativeButton("Cancel", null).show();
            return true;
        });

        dialog.show();
    }

    private void showRegisterTagDialog() {
        List<AssetModel> untagged = inventoryManager.getUntaggedAssets();

        Dialog dialog = new Dialog(this, R.style.DarkAlertDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_register_tag);
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.75),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText etSearch     = dialog.findViewById(R.id.etRegisterSearch);
        Spinner  spinnerAsset = dialog.findViewById(R.id.spinnerUntagged);
        EditText etBarcode    = dialog.findViewById(R.id.etRegisterBarcode);
        EditText etAssetTag   = dialog.findViewById(R.id.etRegisterAssetTag);
        Button   btnSave      = dialog.findViewById(R.id.btnRegisterSave);
        Button   btnCancel    = dialog.findViewById(R.id.btnRegisterCancel);

        List<AssetModel> filtered = new ArrayList<>(untagged);
        UntaggedSpinnerAdapter spinnerAdapter = new UntaggedSpinnerAdapter(filtered);
        spinnerAsset.setAdapter(spinnerAdapter);

        if (untagged.isEmpty()) {
            Toast.makeText(this, "No untagged assets found.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                String q = s.toString().toLowerCase().trim();
                filtered.clear();
                for (AssetModel a : untagged) {
                    if (a.asset != null && a.asset.toLowerCase().contains(q)) filtered.add(a);
                }
                spinnerAdapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            int selectedPos = spinnerAsset.getSelectedItemPosition();
            if (filtered.isEmpty() || selectedPos < 0) {
                Toast.makeText(this, "No asset selected.", Toast.LENGTH_SHORT).show();
                return;
            }
            String newBarcode  = etBarcode.getText().toString().trim();
            String newAssetTag = etAssetTag.getText().toString().trim();

            if (newBarcode.isEmpty() && newAssetTag.isEmpty()) {
                Toast.makeText(this, "Enter at least a barcode or asset tag.", Toast.LENGTH_SHORT).show();
                return;
            }

            AssetModel target = filtered.get(selectedPos);
            if (!newBarcode.isEmpty())  target.barCode    = newBarcode;
            if (!newAssetTag.isEmpty()) target.assetTagNo = newAssetTag;

            if (inventoryManager.updateAsset(target)) {
                logActivity("REGISTER", "Registered tag for asset: " + target.asset
                        + " | Tag: " + newAssetTag + " | Barcode: " + newBarcode);
                Toast.makeText(this, "Tag registered successfully.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadAssetList();
            } else {
                Toast.makeText(this, "Failed to register tag.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void processCsvFile(Uri uri) {
        int inserted = 0, skipped = 0;
        ClsSettingsManagement settingsMgr = new ClsSettingsManagement(this);

        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            boolean firstLine = true;

            int idxClub = -1, idxAssignedType = -1, idxTypeOfAsset = -1;
            int idxAsset = -1, idxMakeBrand = -1, idxModel = -1;
            int idxSerialNumber = -1, idxDateDelivered = -1, idxPurchasePrice = -1;
            int idxLocation = -1, idxAssetTagNo = -1, idxBarCode = -1;

            while ((line = reader.readLine()) != null) {

                if (firstLine) {
                    firstLine = false;
                    String[] headers = splitCsvLine(line);
                    for (int i = 0; i < headers.length; i++) {
                        String h = headers[i].toLowerCase().trim();
                        switch (h) {
                            case "club":
                            case "club assigned":
                            case "club_assigned":         idxClub          = i; break;
                            case "assigned type":
                            case "assigned_type":
                            case "club assigned type":    idxAssignedType  = i; break;
                            case "type of asset":
                            case "type_of_asset":
                            case "asset type":            idxTypeOfAsset   = i; break;
                            case "asset":
                            case "asset name":            idxAsset         = i; break;
                            case "make/brand":
                            case "make brand":
                            case "make_brand":
                            case "brand":                 idxMakeBrand     = i; break;
                            case "model":
                            case "model number":          idxModel         = i; break;
                            case "serial number":
                            case "serial_number":
                            case "serial no":
                            case "serial no.":            idxSerialNumber  = i; break;
                            case "date delivered":
                            case "date_delivered":        idxDateDelivered = i; break;
                            case "purchase price":
                            case "purchase_price":
                            case "price":                 idxPurchasePrice = i; break;
                            case "location in club":
                            case "location in the club":
                            case "location_in_club":
                            case "location":              idxLocation      = i; break;
                            case "asset tag no.":
                            case "asset tag no":
                            case "asset_tag_no":
                            case "asset tag":             idxAssetTagNo    = i; break;
                            case "bar code":
                            case "barcode":
                            case "bar_code":              idxBarCode       = i; break;
                        }
                    }
                    continue;
                }

                String[] cols = splitCsvLine(line);
                if (cols.length < 2) { skipped++; continue; }

                AssetModel a = new AssetModel();

                // club assigned = club — read whichever column is present, sync both
                String clubValue = "";
                if (idxAssignedType >= 0) clubValue = safeGet(cols, idxAssignedType);
                if (clubValue.isEmpty() && idxClub >= 0) clubValue = safeGet(cols, idxClub);
                a.assignedType  = clubValue;
                a.club          = clubValue; // always keep in sync

                a.typeOfAsset   = idxTypeOfAsset   >= 0 ? safeGet(cols, idxTypeOfAsset)   : "";
                a.asset         = idxAsset         >= 0 ? safeGet(cols, idxAsset)         : "";
                a.makeBrand     = idxMakeBrand     >= 0 ? safeGet(cols, idxMakeBrand)     : "";
                a.model         = idxModel         >= 0 ? safeGet(cols, idxModel)         : "";
                a.serialNumber  = idxSerialNumber  >= 0 ? safeGet(cols, idxSerialNumber)  : "";
                a.dateDelivered = idxDateDelivered >= 0 ? safeGet(cols, idxDateDelivered) : "";
                a.location      = idxLocation      >= 0 ? safeGet(cols, idxLocation)      : "";
                a.assetTagNo    = idxAssetTagNo    >= 0 ? safeGet(cols, idxAssetTagNo)    : "";
                a.barCode       = idxBarCode       >= 0 ? safeGet(cols, idxBarCode)       : "";

                String priceStr = idxPurchasePrice >= 0 ? safeGet(cols, idxPurchasePrice) : "";
                a.purchasePrice = priceStr.isEmpty() ? 0
                        : Double.parseDouble(priceStr.replaceAll("[^0-9.]", ""));

                // Asset name is required
                if (a.asset.isEmpty()) { skipped++; continue; }

                // Validate type of asset against DB
                if (!a.typeOfAsset.isEmpty()) {
                    ClsSettingsManagement.TypeModel matched = settingsMgr.findAssetTypeByName(a.typeOfAsset);
                    if (matched == null) { skipped++; continue; }
                }

                long id = inventoryManager.createAsset(a);
                if (id != -1) inserted++;
                else skipped++;
            }
            reader.close();

            logActivity("CSV_IMPORT", "Imported " + inserted + " assets from CSV (" + skipped + " skipped)");
            loadAssetList();

            new AlertDialog.Builder(this)
                    .setTitle("CSV Import Complete")
                    .setMessage("✓ " + inserted + " assets imported\n" +
                            (skipped > 0 ? "⚠ " + skipped + " rows skipped (unknown asset type or missing asset name)" : ""))
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String[] splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') { inQuotes = !inQuotes; }
            else if (c == ',' && !inQuotes) { result.add(sb.toString().trim()); sb.setLength(0); }
            else { sb.append(c); }
        }
        result.add(sb.toString().trim());
        return result.toArray(new String[0]);
    }

    private String safeGet(String[] arr, int index) {
        if (index >= arr.length) return "";
        String val = arr[index].trim();
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2)
            val = val.substring(1, val.length() - 1).trim();
        return val;
    }

    private void logActivity(String action, String desc) {
        SQLiteDatabase db = new ClsDatabaseCreation(this).getWritableDatabase();
        try {
            ContentValues v = new ContentValues();
            v.put(ClsDatabaseCreation.LOG_USER_ID,     sessionUserId);
            v.put(ClsDatabaseCreation.LOG_USERNAME,    sessionUsername);
            v.put(ClsDatabaseCreation.LOG_ACTION,      action);
            v.put(ClsDatabaseCreation.LOG_MODULE,      "Inventory");
            v.put(ClsDatabaseCreation.LOG_DESCRIPTION, desc);
            db.insert(ClsDatabaseCreation.TABLE_ACTIVITY_LOG, null, v);
        } finally { db.close(); }
    }

    @Override
    public void onBackPressed() { super.onBackPressed(); }

    private class AssetRowAdapter extends BaseAdapter {
        private List<AssetModel> data;
        AssetRowAdapter(List<AssetModel> data) { this.data = data; }
        void updateList(List<AssetModel> d) { this.data = d; notifyDataSetChanged(); }
        @Override public int getCount()        { return data.size(); }
        @Override public Object getItem(int p) { return data.get(p); }
        @Override public long getItemId(int p) { return data.get(p).id; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            ViewHolder h;
            if (convertView == null) {
                convertView = LayoutInflater.from(InventoryActivity.this)
                        .inflate(R.layout.item_asset_row, parent, false);
                h = new ViewHolder();
                h.tvAssetName = convertView.findViewById(R.id.tvRowAssetName);
                h.tvAssetTag  = convertView.findViewById(R.id.tvRowAssetTag);
                h.tvBarcode   = convertView.findViewById(R.id.tvRowBarcode);
                h.tvClub      = convertView.findViewById(R.id.tvRowClub);
                h.tvType      = convertView.findViewById(R.id.tvRowType);
                h.tvStatus    = convertView.findViewById(R.id.tvRowStatus);
                convertView.setTag(h);
            } else { h = (ViewHolder) convertView.getTag(); }

            AssetModel a = data.get(pos);
            h.tvAssetName.setText(a.asset != null ? a.asset : "—");
            h.tvAssetTag.setText(a.assetTagNo != null && !a.assetTagNo.isEmpty() ? a.assetTagNo : "—");
            h.tvBarcode.setText(a.barCode != null && !a.barCode.isEmpty() ? a.barCode : "—");
            // assignedType and club are always in sync — show assignedType, fallback to club
            h.tvClub.setText(a.assignedType != null && !a.assignedType.isEmpty()
                    ? a.assignedType : (a.club != null ? a.club : "—"));
            h.tvType.setText(a.typeOfAsset != null ? a.typeOfAsset : "—");

            if (a.isUntagged()) {
                h.tvStatus.setText("UNTAGGED");
                h.tvStatus.setTextColor(0xFFFF6B6B);
                h.tvStatus.setBackgroundResource(R.drawable.bg_btn_danger);
            } else {
                h.tvStatus.setText("TAGGED");
                h.tvStatus.setTextColor(0xFF7743DB);
                h.tvStatus.setBackgroundResource(R.drawable.bg_role_badge);
            }

            convertView.setBackgroundResource(pos % 2 == 0
                    ? R.drawable.bg_user_list_item
                    : android.R.color.transparent);

            return convertView;
        }

        class ViewHolder {
            TextView tvAssetName, tvAssetTag, tvBarcode, tvClub, tvType, tvStatus;
        }
    }

    private class UntaggedSpinnerAdapter extends ArrayAdapter<AssetModel> {
        UntaggedSpinnerAdapter(List<AssetModel> data) {
            super(InventoryActivity.this, R.layout.spinner_item, data);
            setDropDownViewResource(R.layout.spinner_dropdown_item);
        }
        @Override public String toString() { return ""; }
        private String label(AssetModel a) {
            // use assignedType as club label since they are the same
            String club = (a.assignedType != null && !a.assignedType.isEmpty())
                    ? a.assignedType : a.club;
            return (a.asset != null ? a.asset : "?") +
                    (club != null && !club.isEmpty() ? " — " + club : "");
        }
        @Override public View getView(int pos, View convertView, ViewGroup parent) {
            View v = super.getView(pos, convertView, parent);
            ((TextView) v.findViewById(android.R.id.text1)).setText(label(getItem(pos)));
            return v;
        }
        @Override public View getDropDownView(int pos, View convertView, ViewGroup parent) {
            View v = super.getDropDownView(pos, convertView, parent);
            ((TextView) v.findViewById(android.R.id.text1)).setText(label(getItem(pos)));
            return v;
        }
    }
}