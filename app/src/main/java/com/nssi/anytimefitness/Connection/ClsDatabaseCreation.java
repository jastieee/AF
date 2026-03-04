package com.nssi.anytimefitness.Connection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class ClsDatabaseCreation extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "GymAssets.db";
    private static final int DATABASE_VERSION = 4; // bumped from 1 → 2 for Settings module

    public static final String TABLE_ASSET = "asset_inventory";
    public static final String TABLE_MAINTENANCE = "asset_maintenance";
    public static final String TABLE_USERS = "users";
    public static final String TABLE_ASSET_TYPE = "asset_types";
    public static final String TABLE_SERVICE_TYPE = "service_types";
    public static final String TABLE_ACTIVITY_LOG = "activity_logs";
    public static final String TABLE_ROLES = "roles";
    public static final String TABLE_MODULES = "modules";
    public static final String TABLE_ROLE_PERMISSIONS = "role_permissions";

    // Asset Table Columns
    public static final String COL_ID = "id";
    public static final String COL_CLUB = "club";
    public static final String COL_ASSIGNED_TYPE = "assigned_type";
    public static final String COL_TYPE_OF_ASSET = "type_of_asset";
    public static final String COL_ASSET = "asset";
    public static final String COL_MAKE_BRAND = "make_brand";
    public static final String COL_MODEL = "model";
    public static final String COL_SERIAL_NUMBER = "serial_number";
    public static final String COL_DATE_DELIVERED = "date_delivered";
    public static final String COL_PURCHASE_PRICE = "purchase_price";
    public static final String COL_LOCATION = "location_in_club";
    public static final String COL_ASSET_TAG_NO = "asset_tag_no";
    public static final String COL_BAR_CODE = "bar_code";

    // Maintenance Table Columns
    public static final String MAINT_ID = "id";
    public static final String MAINT_CLUB = "club";
    public static final String MAINT_ASSIGNED_TYPE = "assigned_type";
    public static final String MAINT_TYPE_OF_ASSET = "type_of_asset";
    public static final String MAINT_ASSET = "asset";
    public static final String MAINT_MAKE_BRAND = "make_brand";
    public static final String MAINT_MODEL = "model";
    public static final String MAINT_SERIAL_NUMBER = "serial_number";
    public static final String MAINT_ASSET_TAG_NO = "asset_tag_no";
    public static final String MAINT_BAR_CODE = "bar_code";
    public static final String MAINT_DATE_OF_SERVICE = "date_of_service";
    public static final String MAINT_TYPE_OF_SERVICE = "type_of_service";
    public static final String MAINT_ISSUE_DETAILS = "issue_details";
    public static final String MAINT_DIAGNOSIS = "diagnosis_root_cause";
    public static final String MAINT_MATERIALS_USED = "materials_used";
    public static final String MAINT_SOLUTION_APPLIED = "solution_applied";
    public static final String MAINT_DATE_COMPLETED = "date_completed";
    public static final String MAINT_STATUS = "status";

    // Users Table Columns
    public static final String COL_USER_ID = "id";
    public static final String COL_USER_NAME = "name";
    public static final String COL_USER_USERNAME = "username";
    public static final String COL_USER_EMAIL = "email";
    public static final String COL_USER_PASSWORD = "password";
    public static final String COL_USER_ROLE = "role";

    // Asset Types Table Columns
    public static final String COL_ASSET_TYPE_ID = "id";
    public static final String COL_ASSET_TYPE_NAME = "asset_type_name";
    public static final String COL_ASSET_TYPE_DESCRIP = "asset_type_descrip";

    // Service Types
    public static final String COL_SERVICE_TYPE_ID = "id";
    public static final String COL_SERVICE_TYPE_NAME = "service_type_name";
    public static final String COL_SERVICE_TYPE_DESCRIP = "service_type_descrip";

    // Activity Log
    public static final String LOG_ID = "id";
    public static final String LOG_USER_ID = "user_id";
    public static final String LOG_USERNAME = "username";
    public static final String LOG_ACTION = "log_action";
    public static final String LOG_MODULE = "module";
    public static final String LOG_DESCRIPTION = "description";
    public static final String LOG_TIMESTAMP = "timestamp";

    // Roles
    public static final String COL_ROLE_ID = "id";
    public static final String COL_ROLE_NAME = "role_name";
    public static final String COL_ROLE_DESCRIP = "role_description";

    // Modules
    public static final String COL_MODULE_ID = "id";
    public static final String COL_MODULE_NAME  = "module_name";
    public static final String COL_MODULE_DESCRIP = "module_description";

    // Role Permissions
    public static final String COL_PERM_ID  = "id";
    public static final String COL_PERM_USER_ID = "user_id";
    public static final String COL_PERM_MODULE_ID = "module_id";
    public static final String COL_PERM_ACCESS = "can_access";

    public ClsDatabaseCreation(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static final String CREATE_USER_TABLE =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_USER_NAME + " TEXT NOT NULL, " +
                    COL_USER_USERNAME + " TEXT NOT NULL UNIQUE, " +
                    COL_USER_EMAIL + " TEXT UNIQUE, " +
                    COL_USER_PASSWORD + " TEXT NOT NULL, " +
                    COL_USER_ROLE + " TEXT NOT NULL CHECK(" + COL_USER_ROLE + " IN ('Admin', 'Staff')))";

    private static final String CREATE_ASSET_TABLE =
            "CREATE TABLE " + TABLE_ASSET + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_CLUB + " TEXT, " +
                    COL_ASSIGNED_TYPE + " TEXT, " +
                    COL_TYPE_OF_ASSET + " TEXT, " +
                    COL_ASSET + " TEXT, " +
                    COL_MAKE_BRAND + " TEXT, " +
                    COL_MODEL + " TEXT, " +
                    COL_SERIAL_NUMBER + " TEXT, " +
                    COL_DATE_DELIVERED + " TEXT, " +
                    COL_PURCHASE_PRICE + " REAL, " +
                    COL_LOCATION + " TEXT, " +
                    COL_ASSET_TAG_NO + " TEXT, " +
                    COL_BAR_CODE + " TEXT" +
                    ");";

    private static final String CREATE_MAINTENANCE_TABLE =
            "CREATE TABLE " + TABLE_MAINTENANCE + " (" +
                    MAINT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    MAINT_CLUB + " TEXT, " +
                    MAINT_ASSIGNED_TYPE + " TEXT, " +
                    MAINT_TYPE_OF_ASSET + " TEXT, " +
                    MAINT_ASSET + " TEXT, " +
                    MAINT_MAKE_BRAND + " TEXT, " +
                    MAINT_MODEL + " TEXT, " +
                    MAINT_SERIAL_NUMBER + " TEXT, " +
                    MAINT_ASSET_TAG_NO + " TEXT, " +
                    MAINT_BAR_CODE + " TEXT, " +
                    MAINT_DATE_OF_SERVICE + " TEXT, " +
                    MAINT_TYPE_OF_SERVICE + " TEXT, " +
                    MAINT_ISSUE_DETAILS + " TEXT, " +
                    MAINT_DIAGNOSIS + " TEXT, " +
                    MAINT_MATERIALS_USED + " TEXT, " +
                    MAINT_SOLUTION_APPLIED + " TEXT, " +
                    MAINT_DATE_COMPLETED + " TEXT" +
                    ");";

    private static final String CREATE_ASSET_TYPE_TABLE =
            "CREATE TABLE " + TABLE_ASSET_TYPE + " (" +
                    COL_ASSET_TYPE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_ASSET_TYPE_NAME + " TEXT NOT NULL UNIQUE, " +
                    COL_ASSET_TYPE_DESCRIP + " TEXT " +
                    ");";

    private static final String CREATE_SERVICE_TYPE_TABLE =
            "CREATE TABLE " + TABLE_SERVICE_TYPE + " (" +
                    COL_SERVICE_TYPE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_SERVICE_TYPE_NAME + " TEXT NOT NULL UNIQUE, " +
                    COL_SERVICE_TYPE_DESCRIP + " TEXT " +
                    ");";

    private static final String CREATE_ACTIVITY_LOG_TABLE =
            "CREATE TABLE " + TABLE_ACTIVITY_LOG + " (" +
                    LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    LOG_USER_ID + " INTEGER, " +
                    LOG_USERNAME + " TEXT, " +
                    LOG_ACTION + " TEXT, " +
                    LOG_MODULE + " TEXT, " +
                    LOG_DESCRIPTION + " TEXT, " +
                    LOG_TIMESTAMP + " TEXT DEFAULT (datetime('now','localtime'))" +
                    ");";

    private static final String CREATE_ROLES_TABLE =
            "CREATE TABLE " + TABLE_ROLES + " (" +
                    COL_ROLE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_ROLE_NAME + " TEXT NOT NULL UNIQUE, " +
                    COL_ROLE_DESCRIP + " TEXT" +
                    ");";

    private static final String CREATE_MODULES_TABLE =
            "CREATE TABLE " + TABLE_MODULES + " (" +
                    COL_MODULE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_MODULE_NAME + " TEXT NOT NULL UNIQUE, " +
                    COL_MODULE_DESCRIP + " TEXT" +
                    ");";

    private static final String CREATE_ROLE_PERMISSIONS_TABLE =
            "CREATE TABLE " + TABLE_ROLE_PERMISSIONS + " (" +
                    COL_PERM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_PERM_USER_ID + " INTEGER NOT NULL, " +
                    COL_PERM_MODULE_ID + " INTEGER NOT NULL, " +
                    COL_PERM_ACCESS + " INTEGER DEFAULT 0, " +
                    "FOREIGN KEY(" + COL_PERM_USER_ID + ") REFERENCES " + TABLE_ROLES + "(" + COL_ROLE_ID + "), " +
                    "FOREIGN KEY(" + COL_PERM_MODULE_ID + ") REFERENCES " + TABLE_MODULES + "(" + COL_MODULE_ID + "), " +
                    "UNIQUE(" + COL_PERM_USER_ID + ", " + COL_PERM_MODULE_ID + ")" +
                    ");";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_ASSET_TABLE);
        db.execSQL(CREATE_MAINTENANCE_TABLE);
        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_ASSET_TYPE_TABLE);
        db.execSQL(CREATE_SERVICE_TYPE_TABLE);
        db.execSQL(CREATE_ACTIVITY_LOG_TABLE);
        db.execSQL(CREATE_ROLES_TABLE);
        db.execSQL(CREATE_MODULES_TABLE);
        db.execSQL(CREATE_ROLE_PERMISSIONS_TABLE);

        seedRoles(db);
        seedModules(db);
        seedDefaultPermissions(db);
        seedDefaultAdmin(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 → 2: add Settings module + its permission for admin
        if (oldVersion < 3) {

            db.execSQL("ALTER TABLE " + TABLE_MAINTENANCE + " ADD COLUMN status TEXT DEFAULT 'Open'");

            db.execSQL(
                "INSERT OR IGNORE INTO " + TABLE_MODULES +
                " (" + COL_MODULE_NAME + ", " + COL_MODULE_DESCRIP + ") " +
                "VALUES ('Settings', 'Manage asset types and service types')"
            );

            Cursor c = db.rawQuery(
                "SELECT " + COL_MODULE_ID + " FROM " + TABLE_MODULES +
                " WHERE " + COL_MODULE_NAME + " = 'Settings' LIMIT 1", null);
            if (c.moveToFirst()) {
                int settingsModuleId = c.getInt(0);
                ContentValues cv = new ContentValues();
                cv.put(COL_PERM_USER_ID,   1); // default admin
                cv.put(COL_PERM_MODULE_ID, settingsModuleId);
                cv.put(COL_PERM_ACCESS,    1);
                db.insertWithOnConflict(TABLE_ROLE_PERMISSIONS, null, cv,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }
            c.close();
        }
    }

    // ─────────────────────────────────────────────
    //  SEED ROLES
    // ─────────────────────────────────────────────
    private void seedRoles(SQLiteDatabase db) {
        String[] roles = {"Admin", "Staff"};
        String[] descs = {
                "Full access to all modules",
                "Limited access to assigned modules"
        };
        for (int i = 0; i < roles.length; i++) {
            ContentValues values = new ContentValues();
            values.put(COL_ROLE_NAME, roles[i]);
            values.put(COL_ROLE_DESCRIP, descs[i]);
            db.insert(TABLE_ROLES, null, values);
        }
    }

    // ─────────────────────────────────────────────
    //  SEED MODULES — Settings added as module 6
    // ─────────────────────────────────────────────
    private void seedModules(SQLiteDatabase db) {
        String[] modules = {
                "Inventory",
                "Maintenance",
                "Logs",
                "Reports",
                "User Management",
                "Settings"          // ← new
        };
        String[] descs = {
                "Asset inventory tracking and management",
                "Asset maintenance and service records",
                "System activity and audit logs",
                "Generate and view reports",
                "Manage user accounts and roles",
                "Manage asset types and service types"  // ← new
        };
        for (int i = 0; i < modules.length; i++) {
            ContentValues values = new ContentValues();
            values.put(COL_MODULE_NAME, modules[i]);
            values.put(COL_MODULE_DESCRIP, descs[i]);
            db.insert(TABLE_MODULES, null, values);
        }
    }

    // ─────────────────────────────────────────────
    //  SEED DEFAULT ADMIN
    // ─────────────────────────────────────────────
    private void seedDefaultAdmin(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COL_USER_NAME, "Administrator");
        values.put(COL_USER_USERNAME, "admin");
        values.put(COL_USER_EMAIL, "admin@anytimefitness.com");
        values.put(COL_USER_PASSWORD, "admin123");
        values.put(COL_USER_ROLE, "Admin");
        db.insert(TABLE_USERS, null, values);
    }

    // ─────────────────────────────────────────────
    //  SEED DEFAULT PERMISSIONS — now 6 modules
    // ─────────────────────────────────────────────
    private void seedDefaultPermissions(SQLiteDatabase db) {
        int totalModules = 6; // includes Settings

        for (int moduleId = 1; moduleId <= totalModules; moduleId++) {
            ContentValues values = new ContentValues();
            values.put(COL_PERM_USER_ID, 1);
            values.put(COL_PERM_MODULE_ID, moduleId);
            values.put(COL_PERM_ACCESS, 1);
            db.insert(TABLE_ROLE_PERMISSIONS, null, values);
        }
    }
}
