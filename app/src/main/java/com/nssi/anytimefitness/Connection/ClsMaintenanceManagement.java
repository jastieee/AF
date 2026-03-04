package com.nssi.anytimefitness.Connection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class ClsMaintenanceManagement {

    private final ClsDatabaseCreation dbHelper;

    public ClsMaintenanceManagement(Context context) {
        dbHelper = new ClsDatabaseCreation(context);
    }

    //  MODEL
    public static class MaintenanceModel {
        public int    id;
        // Asset info (read-only, filled from inventory)
        public String club;
        public String assignedType;
        public String typeOfAsset;
        public String asset;
        public String makeBrand;
        public String model;
        public String serialNumber;
        public String assetTagNo;
        public String barCode;
        // Service info (editable)
        public String dateOfService;
        public String typeOfService;
        public String issueDetails;
        public String diagnosis;
        public String materialsUsed;
        public String solutionApplied;
        public String dateCompleted;
        public String status; // Open / In Progress / Completed / Cancelled

        public String getInitial() {
            if (asset != null && !asset.isEmpty())
                return String.valueOf(asset.charAt(0)).toUpperCase();
            return "?";
        }

        public String getTagDisplay() {
            if (assetTagNo != null && !assetTagNo.trim().isEmpty()) return assetTagNo.trim();
            if (barCode    != null && !barCode.trim().isEmpty())    return barCode.trim();
            return "UNTAGGED";
        }
    }

    //  READ ALL
    public List<MaintenanceModel> getAllRecords() {
        return query(null, null);
    }

    //  FILTER BY STATUS
    public List<MaintenanceModel> getByStatus(String status) {
        return query(ClsDatabaseCreation.MAINT_STATUS + " = ?", new String[]{ status });
    }

    //  SEARCH
    public List<MaintenanceModel> search(String q) {
        String like = "%" + q + "%";
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<MaintenanceModel> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + ClsDatabaseCreation.TABLE_MAINTENANCE +
                        " WHERE " + ClsDatabaseCreation.MAINT_ASSET          + " LIKE ? " +
                        " OR "    + ClsDatabaseCreation.MAINT_ASSET_TAG_NO   + " LIKE ? " +
                        " OR "    + ClsDatabaseCreation.MAINT_BAR_CODE       + " LIKE ? " +
                        " OR "    + ClsDatabaseCreation.MAINT_CLUB           + " LIKE ? " +
                        " OR "    + ClsDatabaseCreation.MAINT_TYPE_OF_SERVICE + " LIKE ? " +
                        " ORDER BY " + ClsDatabaseCreation.MAINT_ID + " DESC",
                new String[]{ like, like, like, like, like });
        while (c.moveToNext()) list.add(cursorToModel(c));
        c.close(); db.close();
        return list;
    }

    //  CREATE
    public long createRecord(MaintenanceModel m) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(ClsDatabaseCreation.TABLE_MAINTENANCE, null, modelToValues(m));
        db.close();
        return id;
    }

    //  UPDATE SERVICE DETAILS ONLY
    public boolean updateServiceDetails(MaintenanceModel m) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(ClsDatabaseCreation.MAINT_DATE_OF_SERVICE,  m.dateOfService);
        v.put(ClsDatabaseCreation.MAINT_TYPE_OF_SERVICE,  m.typeOfService);
        v.put(ClsDatabaseCreation.MAINT_ISSUE_DETAILS,    m.issueDetails);
        v.put(ClsDatabaseCreation.MAINT_DIAGNOSIS,        m.diagnosis);
        v.put(ClsDatabaseCreation.MAINT_MATERIALS_USED,   m.materialsUsed);
        v.put(ClsDatabaseCreation.MAINT_SOLUTION_APPLIED, m.solutionApplied);
        v.put(ClsDatabaseCreation.MAINT_DATE_COMPLETED,   m.dateCompleted);
        v.put(ClsDatabaseCreation.MAINT_STATUS,           m.status);
        int rows = db.update(ClsDatabaseCreation.TABLE_MAINTENANCE, v,
                ClsDatabaseCreation.MAINT_ID + " = ?",
                new String[]{ String.valueOf(m.id) });
        db.close();
        return rows > 0;
    }

    //  DELETE
    public boolean deleteRecord(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(ClsDatabaseCreation.TABLE_MAINTENANCE,
                ClsDatabaseCreation.MAINT_ID + " = ?",
                new String[]{ String.valueOf(id) });
        db.close();
        return rows > 0;
    }

    //  INTERNAL HELPERS
    private List<MaintenanceModel> query(String where, String[] args) {
        List<MaintenanceModel> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT * FROM " + ClsDatabaseCreation.TABLE_MAINTENANCE;
        if (where != null) sql += " WHERE " + where;
        sql += " ORDER BY " + ClsDatabaseCreation.MAINT_ID + " DESC";
        Cursor c = db.rawQuery(sql, args);
        while (c.moveToNext()) list.add(cursorToModel(c));
        c.close(); db.close();
        return list;
    }

    private MaintenanceModel cursorToModel(Cursor c) {
        MaintenanceModel m = new MaintenanceModel();
        m.id              = c.getInt(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_ID));
        m.club            = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_CLUB));
        m.assignedType    = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_ASSIGNED_TYPE));
        m.typeOfAsset     = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_TYPE_OF_ASSET));
        m.asset           = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_ASSET));
        m.makeBrand       = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_MAKE_BRAND));
        m.model           = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_MODEL));
        m.serialNumber    = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_SERIAL_NUMBER));
        m.assetTagNo      = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_ASSET_TAG_NO));
        m.barCode         = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_BAR_CODE));
        m.dateOfService   = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_DATE_OF_SERVICE));
        m.typeOfService   = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_TYPE_OF_SERVICE));
        m.issueDetails    = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_ISSUE_DETAILS));
        m.diagnosis       = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_DIAGNOSIS));
        m.materialsUsed   = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_MATERIALS_USED));
        m.solutionApplied = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_SOLUTION_APPLIED));
        m.dateCompleted   = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_DATE_COMPLETED));
        m.status          = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.MAINT_STATUS));
        return m;
    }

    private ContentValues modelToValues(MaintenanceModel m) {
        ContentValues v = new ContentValues();
        v.put(ClsDatabaseCreation.MAINT_CLUB,             m.club);
        v.put(ClsDatabaseCreation.MAINT_ASSIGNED_TYPE,    m.assignedType);
        v.put(ClsDatabaseCreation.MAINT_TYPE_OF_ASSET,    m.typeOfAsset);
        v.put(ClsDatabaseCreation.MAINT_ASSET,            m.asset);
        v.put(ClsDatabaseCreation.MAINT_MAKE_BRAND,       m.makeBrand);
        v.put(ClsDatabaseCreation.MAINT_MODEL,            m.model);
        v.put(ClsDatabaseCreation.MAINT_SERIAL_NUMBER,    m.serialNumber);
        v.put(ClsDatabaseCreation.MAINT_ASSET_TAG_NO,     m.assetTagNo);
        v.put(ClsDatabaseCreation.MAINT_BAR_CODE,         m.barCode);
        v.put(ClsDatabaseCreation.MAINT_DATE_OF_SERVICE,  m.dateOfService);
        v.put(ClsDatabaseCreation.MAINT_TYPE_OF_SERVICE,  m.typeOfService);
        v.put(ClsDatabaseCreation.MAINT_ISSUE_DETAILS,    m.issueDetails);
        v.put(ClsDatabaseCreation.MAINT_DIAGNOSIS,        m.diagnosis);
        v.put(ClsDatabaseCreation.MAINT_MATERIALS_USED,   m.materialsUsed);
        v.put(ClsDatabaseCreation.MAINT_SOLUTION_APPLIED, m.solutionApplied);
        v.put(ClsDatabaseCreation.MAINT_DATE_COMPLETED,   m.dateCompleted);
        v.put(ClsDatabaseCreation.MAINT_STATUS,           m.status != null ? m.status : "Open");
        return v;
    }
}