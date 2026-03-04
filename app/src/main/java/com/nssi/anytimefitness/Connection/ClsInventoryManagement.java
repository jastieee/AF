package com.nssi.anytimefitness.Connection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class ClsInventoryManagement {

    private final ClsDatabaseCreation dbHelper;

    public ClsInventoryManagement(Context context) {
        dbHelper = new ClsDatabaseCreation(context);
    }

    //  MODEL
    public static class AssetModel {
        public int    id;
        public String club;
        public String assignedType;
        public String typeOfAsset;
        public String asset;
        public String makeBrand;
        public String model;
        public String serialNumber;
        public String dateDelivered;
        public double purchasePrice;
        public String location;
        public String assetTagNo;
        public String barCode;

        /** Returns true if asset has neither a barcode nor an asset tag */
        public boolean isUntagged() {
            return (barCode == null || barCode.trim().isEmpty())
                    && (assetTagNo == null || assetTagNo.trim().isEmpty());
        }

        /** Display label for the list — prefers asset tag, then barcode, then "UNTAGGED" */
        public String getTagDisplay() {
            if (assetTagNo != null && !assetTagNo.trim().isEmpty()) return assetTagNo.trim();
            if (barCode    != null && !barCode.trim().isEmpty())    return barCode.trim();
            return "UNTAGGED";
        }

        /** Initial letter for the avatar circle */
        public String getInitial() {
            if (asset != null && !asset.isEmpty()) return String.valueOf(asset.charAt(0)).toUpperCase();
            return "?";
        }
    }
    //  READ ALL
    public List<AssetModel> getAllAssets() {
        List<AssetModel> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + ClsDatabaseCreation.TABLE_ASSET
                + " ORDER BY " + ClsDatabaseCreation.COL_ASSET + " ASC", null);
        while (c.moveToNext()) list.add(cursorToModel(c));
        c.close(); db.close();
        return list;
    }

    //  SEARCH (name, tag, barcode, serial, club)
    public List<AssetModel> searchAssets(String query) {
        List<AssetModel> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String like = "%" + query + "%";
        Cursor c = db.rawQuery(
                "SELECT * FROM " + ClsDatabaseCreation.TABLE_ASSET +
                " WHERE " + ClsDatabaseCreation.COL_ASSET          + " LIKE ? " +
                " OR "    + ClsDatabaseCreation.COL_ASSET_TAG_NO   + " LIKE ? " +
                " OR "    + ClsDatabaseCreation.COL_BAR_CODE       + " LIKE ? " +
                " OR "    + ClsDatabaseCreation.COL_SERIAL_NUMBER  + " LIKE ? " +
                " OR "    + ClsDatabaseCreation.COL_CLUB           + " LIKE ? " +
                " ORDER BY " + ClsDatabaseCreation.COL_ASSET + " ASC",
                new String[]{like, like, like, like, like});
        while (c.moveToNext()) list.add(cursorToModel(c));
        c.close(); db.close();
        return list;
    }

    //  FIND BY BARCODE — used by Zebra scanner
    public AssetModel findByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + ClsDatabaseCreation.TABLE_ASSET +
                " WHERE " + ClsDatabaseCreation.COL_BAR_CODE + " = ? LIMIT 1",
                new String[]{barcode.trim()});
        AssetModel model = null;
        if (c.moveToFirst()) model = cursorToModel(c);
        c.close(); db.close();
        return model;
    }

    //  FIND BY ASSET TAG
    public AssetModel findByAssetTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + ClsDatabaseCreation.TABLE_ASSET +
                " WHERE " + ClsDatabaseCreation.COL_ASSET_TAG_NO + " = ? LIMIT 1",
                new String[]{tag.trim()});
        AssetModel model = null;
        if (c.moveToFirst()) model = cursorToModel(c);
        c.close(); db.close();
        return model;
    }

    //  GET BY ID
    public AssetModel getAssetById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + ClsDatabaseCreation.TABLE_ASSET +
                " WHERE " + ClsDatabaseCreation.COL_ID + " = ? LIMIT 1",
                new String[]{String.valueOf(id)});
        AssetModel model = null;
        if (c.moveToFirst()) model = cursorToModel(c);
        c.close(); db.close();
        return model;
    }

    //  CREATE
    public long createAsset(AssetModel a) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(ClsDatabaseCreation.TABLE_ASSET, null, modelToValues(a));
        db.close();
        return id;
    }

    //  UPDATE
    public boolean updateAsset(AssetModel a) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.update(ClsDatabaseCreation.TABLE_ASSET, modelToValues(a),
                ClsDatabaseCreation.COL_ID + " = ?",
                new String[]{String.valueOf(a.id)});
        db.close();
        return rows > 0;
    }

    //  DELETE
    public boolean deleteAsset(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(ClsDatabaseCreation.TABLE_ASSET,
                ClsDatabaseCreation.COL_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    //  UNTAGGED ASSETS
    public List<AssetModel> getUntaggedAssets() {
        List<AssetModel> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + ClsDatabaseCreation.TABLE_ASSET +
                " WHERE (" + ClsDatabaseCreation.COL_BAR_CODE + " IS NULL OR " +
                              ClsDatabaseCreation.COL_BAR_CODE + " = '') " +
                " AND (" + ClsDatabaseCreation.COL_ASSET_TAG_NO + " IS NULL OR " +
                            ClsDatabaseCreation.COL_ASSET_TAG_NO + " = '') " +
                " ORDER BY " + ClsDatabaseCreation.COL_ASSET + " ASC", null);
        while (c.moveToNext()) list.add(cursorToModel(c));
        c.close(); db.close();
        return list;
    }

    //  HELPERS
    private AssetModel cursorToModel(Cursor c) {
        AssetModel a = new AssetModel();
        a.id            = c.getInt(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_ID));
        a.club          = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_CLUB));
        a.assignedType  = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_ASSIGNED_TYPE));
        a.typeOfAsset   = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_TYPE_OF_ASSET));
        a.asset         = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_ASSET));
        a.makeBrand     = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_MAKE_BRAND));
        a.model         = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_MODEL));
        a.serialNumber  = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_SERIAL_NUMBER));
        a.dateDelivered = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_DATE_DELIVERED));
        a.purchasePrice = c.getDouble(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_PURCHASE_PRICE));
        a.location      = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_LOCATION));
        a.assetTagNo    = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_ASSET_TAG_NO));
        a.barCode       = c.getString(c.getColumnIndexOrThrow(ClsDatabaseCreation.COL_BAR_CODE));
        return a;
    }

    private ContentValues modelToValues(AssetModel a) {
        ContentValues v = new ContentValues();
        v.put(ClsDatabaseCreation.COL_CLUB,           a.club);
        v.put(ClsDatabaseCreation.COL_ASSIGNED_TYPE,  a.assignedType);
        v.put(ClsDatabaseCreation.COL_TYPE_OF_ASSET,  a.typeOfAsset);
        v.put(ClsDatabaseCreation.COL_ASSET,          a.asset);
        v.put(ClsDatabaseCreation.COL_MAKE_BRAND,     a.makeBrand);
        v.put(ClsDatabaseCreation.COL_MODEL,          a.model);
        v.put(ClsDatabaseCreation.COL_SERIAL_NUMBER,  a.serialNumber);
        v.put(ClsDatabaseCreation.COL_DATE_DELIVERED, a.dateDelivered);
        v.put(ClsDatabaseCreation.COL_PURCHASE_PRICE, a.purchasePrice);
        v.put(ClsDatabaseCreation.COL_LOCATION,       a.location);
        v.put(ClsDatabaseCreation.COL_ASSET_TAG_NO,   a.assetTagNo);
        v.put(ClsDatabaseCreation.COL_BAR_CODE,       a.barCode);
        return v;
    }
}
