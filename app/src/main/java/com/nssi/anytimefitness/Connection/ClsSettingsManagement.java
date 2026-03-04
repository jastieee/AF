package com.nssi.anytimefitness.Connection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class ClsSettingsManagement {

    private final ClsDatabaseCreation dbHelper;

    public ClsSettingsManagement(Context context) {
        dbHelper = new ClsDatabaseCreation(context);
    }

    //  MODEL
    public static class TypeModel {
        public int    id;
        public String name;
        public String description;

        /** First letter for the avatar circle */
        public String getInitial() {
            if (name != null && !name.isEmpty())
                return String.valueOf(name.charAt(0)).toUpperCase();
            return "?";
        }
    }

    // ═════════════════════════════════════════════
    //  ASSET TYPES
    // ═════════════════════════════════════════════

    public List<TypeModel> getAllAssetTypes() {
        return queryTypes(ClsDatabaseCreation.TABLE_ASSET_TYPE,
                ClsDatabaseCreation.COL_ASSET_TYPE_ID,
                ClsDatabaseCreation.COL_ASSET_TYPE_NAME,
                ClsDatabaseCreation.COL_ASSET_TYPE_DESCRIP,
                null);
    }

    public List<TypeModel> searchAssetTypes(String query) {
        return queryTypes(ClsDatabaseCreation.TABLE_ASSET_TYPE,
                ClsDatabaseCreation.COL_ASSET_TYPE_ID,
                ClsDatabaseCreation.COL_ASSET_TYPE_NAME,
                ClsDatabaseCreation.COL_ASSET_TYPE_DESCRIP,
                query);
    }

    public long addAssetType(String name, String description) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(ClsDatabaseCreation.COL_ASSET_TYPE_NAME,    name.trim());
        v.put(ClsDatabaseCreation.COL_ASSET_TYPE_DESCRIP, description != null ? description.trim() : "");
        long id = db.insert(ClsDatabaseCreation.TABLE_ASSET_TYPE, null, v);
        db.close();
        return id;
    }

    public boolean updateAssetType(int id, String name, String description) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(ClsDatabaseCreation.COL_ASSET_TYPE_NAME,    name.trim());
        v.put(ClsDatabaseCreation.COL_ASSET_TYPE_DESCRIP, description != null ? description.trim() : "");
        int rows = db.update(ClsDatabaseCreation.TABLE_ASSET_TYPE, v,
                ClsDatabaseCreation.COL_ASSET_TYPE_ID + " = ?",
                new String[]{ String.valueOf(id) });
        db.close();
        return rows > 0;
    }

    public boolean deleteAssetType(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(ClsDatabaseCreation.TABLE_ASSET_TYPE,
                ClsDatabaseCreation.COL_ASSET_TYPE_ID + " = ?",
                new String[]{ String.valueOf(id) });
        db.close();
        return rows > 0;
    }

    /** Returns true if any asset in the inventory uses this asset-type name */
    public boolean isAssetTypeInUse(String typeName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + ClsDatabaseCreation.TABLE_ASSET +
                        " WHERE " + ClsDatabaseCreation.COL_TYPE_OF_ASSET + " = ?",
                new String[]{ typeName });
        boolean inUse = false;
        if (c.moveToFirst()) inUse = c.getInt(0) > 0;
        c.close(); db.close();
        return inUse;
    }

    // ═════════════════════════════════════════════
    //  SERVICE TYPES
    // ═════════════════════════════════════════════

    public List<TypeModel> getAllServiceTypes() {
        return queryTypes(ClsDatabaseCreation.TABLE_SERVICE_TYPE,
                ClsDatabaseCreation.COL_SERVICE_TYPE_ID,
                ClsDatabaseCreation.COL_SERVICE_TYPE_NAME,
                ClsDatabaseCreation.COL_SERVICE_TYPE_DESCRIP,
                null);
    }

    public List<TypeModel> searchServiceTypes(String query) {
        return queryTypes(ClsDatabaseCreation.TABLE_SERVICE_TYPE,
                ClsDatabaseCreation.COL_SERVICE_TYPE_ID,
                ClsDatabaseCreation.COL_SERVICE_TYPE_NAME,
                ClsDatabaseCreation.COL_SERVICE_TYPE_DESCRIP,
                query);
    }

    public long addServiceType(String name, String description) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(ClsDatabaseCreation.COL_SERVICE_TYPE_NAME,    name.trim());
        v.put(ClsDatabaseCreation.COL_SERVICE_TYPE_DESCRIP, description != null ? description.trim() : "");
        long id = db.insert(ClsDatabaseCreation.TABLE_SERVICE_TYPE, null, v);
        db.close();
        return id;
    }

    public boolean updateServiceType(int id, String name, String description) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(ClsDatabaseCreation.COL_SERVICE_TYPE_NAME,    name.trim());
        v.put(ClsDatabaseCreation.COL_SERVICE_TYPE_DESCRIP, description != null ? description.trim() : "");
        int rows = db.update(ClsDatabaseCreation.TABLE_SERVICE_TYPE, v,
                ClsDatabaseCreation.COL_SERVICE_TYPE_ID + " = ?",
                new String[]{ String.valueOf(id) });
        db.close();
        return rows > 0;
    }

    public boolean deleteServiceType(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(ClsDatabaseCreation.TABLE_SERVICE_TYPE,
                ClsDatabaseCreation.COL_SERVICE_TYPE_ID + " = ?",
                new String[]{ String.valueOf(id) });
        db.close();
        return rows > 0;
    }

    /** Returns true if any maintenance record uses this service-type name */
    public boolean isServiceTypeInUse(String typeName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + ClsDatabaseCreation.TABLE_MAINTENANCE +
                        " WHERE " + ClsDatabaseCreation.MAINT_TYPE_OF_SERVICE + " = ?",
                new String[]{ typeName });
        boolean inUse = false;
        if (c.moveToFirst()) inUse = c.getInt(0) > 0;
        c.close(); db.close();
        return inUse;
    }

    //  INTERNAL QUERY HELPER
    private List<TypeModel> queryTypes(String table, String colId, String colName,
                                       String colDescrip, String searchQuery) {
        List<TypeModel> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c;
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String like = "%" + searchQuery.trim() + "%";
            c = db.rawQuery(
                    "SELECT * FROM " + table +
                            " WHERE " + colName + " LIKE ? OR " + colDescrip + " LIKE ?" +
                            " ORDER BY " + colName + " ASC",
                    new String[]{ like, like });
        } else {
            c = db.rawQuery(
                    "SELECT * FROM " + table + " ORDER BY " + colName + " ASC", null);
        }

        while (c.moveToNext()) {
            TypeModel m = new TypeModel();
            m.id          = c.getInt(c.getColumnIndexOrThrow(colId));
            m.name        = c.getString(c.getColumnIndexOrThrow(colName));
            m.description = c.getString(c.getColumnIndexOrThrow(colDescrip));
            list.add(m);
        }
        c.close(); db.close();
        return list;
    }

    public TypeModel findAssetTypeByName(String name) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Normalize & ↔ and before comparing
        String normalized = name.trim()
                .replace(" & ", " and ")
                .replace(" &amp; ", " and ");


        Cursor c = db.rawQuery(
                "SELECT * FROM " + ClsDatabaseCreation.TABLE_ASSET_TYPE +
                        " WHERE LOWER(REPLACE(REPLACE(" + ClsDatabaseCreation.COL_ASSET_TYPE_NAME + ", ' & ', ' and '), ' &amp; ', ' and ')) = LOWER(?)" +
                        " LIMIT 1",
                new String[]{ normalized.toLowerCase() });
        TypeModel model = null;
        if (c.moveToFirst()) {
            model = new TypeModel();
            model.id   = c.getInt(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_ASSET_TYPE_ID));
            model.name = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_ASSET_TYPE_NAME));
        }
        c.close(); db.close();
        return model;
    }
}