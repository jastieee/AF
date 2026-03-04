package com.nssi.anytimefitness.Connection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClsUserManagement {

    private final ClsDatabaseCreation dbHelper;

    public ClsUserManagement(Context context) {
        dbHelper = new ClsDatabaseCreation(context);
    }

    //  MODEL — User with permissions

    public static class UserModel {
        public int id;
        public String name;
        public String username;
        public String email;
        public String password;
        public String role;
        // key = module name, value = true/false
        public Map<String, Boolean> modulePermissions = new HashMap<>();

        // Returns first letter of name for avatar
        public String getInitial() {
            return (name != null && !name.isEmpty())
                    ? String.valueOf(name.charAt(0)).toUpperCase()
                    : "?";
        }
    }

    //  READ — Get all users

    public List<UserModel> getAllUsers() {
        List<UserModel> users = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(
                    ClsDatabaseCreation.TABLE_USERS,
                    new String[]{
                            ClsDatabaseCreation.COL_USER_ID,
                            ClsDatabaseCreation.COL_USER_NAME,
                            ClsDatabaseCreation.COL_USER_USERNAME,
                            ClsDatabaseCreation.COL_USER_EMAIL,
                            ClsDatabaseCreation.COL_USER_ROLE
                    },
                    null, null, null, null,
                    ClsDatabaseCreation.COL_USER_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    UserModel user = new UserModel();
                    user.id       = cursor.getInt(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_ID));
                    user.name     = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_NAME));
                    user.username = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_USERNAME));
                    user.email    = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_EMAIL));
                    user.role     = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_ROLE));
                    users.add(user);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return users;
    }

    //  READ
    public UserModel getUserById(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        UserModel user = null;
        Cursor cursor = null;

        try {
            cursor = db.query(
                    ClsDatabaseCreation.TABLE_USERS,
                    null,
                    ClsDatabaseCreation.COL_USER_ID + " = ?",
                    new String[]{ String.valueOf(userId) },
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                user = new UserModel();
                user.id       = cursor.getInt(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_ID));
                user.name     = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_NAME));
                user.username = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_USERNAME));
                user.email    = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_EMAIL));
                user.password = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_PASSWORD));
                user.role     = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_ROLE));

                // Load this user's module permissions
                user.modulePermissions = getPermissionsForUser(userId, db);
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return user;
    }

    //  READ MODULES

    public List<Map<String, Object>> getAllModules() {
        List<Map<String, Object>> modules = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    ClsDatabaseCreation.TABLE_MODULES,
                    new String[]{
                            ClsDatabaseCreation.COL_MODULE_ID,
                            ClsDatabaseCreation.COL_MODULE_NAME,
                            ClsDatabaseCreation.COL_MODULE_DESCRIP
                    },
                    null, null, null, null,
                    ClsDatabaseCreation.COL_MODULE_ID + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Map<String, Object> module = new HashMap<>();
                    module.put("id",   cursor.getInt(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_MODULE_ID)));
                    module.put("name", cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_MODULE_NAME)));
                    module.put("desc", cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_MODULE_DESCRIP)));
                    modules.add(module);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return modules;
    }

    //  READ


    public Map<String, Boolean> getPermissionsForUser(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, Boolean> perms = getPermissionsForUser(userId, db);
        db.close();
        return perms;
    }

    // Internal overload that reuses an open db connection
    private Map<String, Boolean> getPermissionsForUser(int userId, SQLiteDatabase db) {
        Map<String, Boolean> perms = new HashMap<>();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(
                    "SELECT m." + ClsDatabaseCreation.COL_MODULE_NAME +
                            ", rp." + ClsDatabaseCreation.COL_PERM_ACCESS +
                            " FROM " + ClsDatabaseCreation.TABLE_ROLE_PERMISSIONS + " rp" +
                            " INNER JOIN " + ClsDatabaseCreation.TABLE_MODULES + " m" +
                            "   ON rp." + ClsDatabaseCreation.COL_PERM_MODULE_ID +
                            "    = m." + ClsDatabaseCreation.COL_MODULE_ID +
                            " WHERE rp." + ClsDatabaseCreation.COL_PERM_USER_ID + " = ?",
                    new String[]{ String.valueOf(userId) }
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String moduleName = cursor.getString(0);
                    boolean canAccess = cursor.getInt(1) == 1;
                    perms.put(moduleName, canAccess);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return perms;
    }



    public long createUser(UserModel user, Map<String, Boolean> permissions) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long newUserId = -1;

        db.beginTransaction();
        try {
            // 1. Insert user
            ContentValues userValues = new ContentValues();
            userValues.put(ClsDatabaseCreation.COL_USER_NAME,     user.name);
            userValues.put(ClsDatabaseCreation.COL_USER_USERNAME,  user.username);
            userValues.put(ClsDatabaseCreation.COL_USER_EMAIL,     user.email);
            userValues.put(ClsDatabaseCreation.COL_USER_PASSWORD,  user.password);
            userValues.put(ClsDatabaseCreation.COL_USER_ROLE,      user.role);

            newUserId = db.insert(ClsDatabaseCreation.TABLE_USERS, null, userValues);

            if (newUserId == -1) {
                // Insert failed (e.g. duplicate username)
                db.endTransaction();
                return -1;
            }

            // 2. Insert permissions for each module
            insertPermissions(db, (int) newUserId, permissions);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        return newUserId;
    }


    public boolean updateUser(UserModel user, Map<String, Boolean> permissions) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean success = false;

        db.beginTransaction();
        try {
            ContentValues userValues = new ContentValues();
            userValues.put(ClsDatabaseCreation.COL_USER_NAME,    user.name);
            userValues.put(ClsDatabaseCreation.COL_USER_USERNAME, user.username);
            userValues.put(ClsDatabaseCreation.COL_USER_EMAIL,   user.email);
            userValues.put(ClsDatabaseCreation.COL_USER_ROLE,    user.role);

            // Only update password if a new one was provided
            if (user.password != null && !user.password.isEmpty()) {
                userValues.put(ClsDatabaseCreation.COL_USER_PASSWORD, user.password);
            }

            int rows = db.update(
                    ClsDatabaseCreation.TABLE_USERS,
                    userValues,
                    ClsDatabaseCreation.COL_USER_ID + " = ?",
                    new String[]{ String.valueOf(user.id) }
            );

            if (rows > 0) {
                // Delete old permissions then re-insert updated ones
                db.delete(
                        ClsDatabaseCreation.TABLE_ROLE_PERMISSIONS,
                        ClsDatabaseCreation.COL_PERM_USER_ID + " = ?",
                        new String[]{ String.valueOf(user.id) }
                );
                insertPermissions(db, user.id, permissions);
                success = true;
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        return success;
    }

    //  DELETE


    public boolean deleteUser(int userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean success = false;

        db.beginTransaction();
        try {
            // Delete permissions first (FK constraint)
            db.delete(
                    ClsDatabaseCreation.TABLE_ROLE_PERMISSIONS,
                    ClsDatabaseCreation.COL_PERM_USER_ID + " = ?",
                    new String[]{ String.valueOf(userId) }
            );

            // Delete user
            int rows = db.delete(
                    ClsDatabaseCreation.TABLE_USERS,
                    ClsDatabaseCreation.COL_USER_ID + " = ?",
                    new String[]{ String.valueOf(userId) }
            );

            success = rows > 0;
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        return success;
    }

    //  VALIDATE


    public boolean isUsernameTaken(String username, int excludeUserId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        boolean taken = false;
        Cursor cursor = null;

        try {
            if (excludeUserId == -1) {
                cursor = db.query(
                        ClsDatabaseCreation.TABLE_USERS,
                        new String[]{ ClsDatabaseCreation.COL_USER_ID },
                        ClsDatabaseCreation.COL_USER_USERNAME + " = ?",
                        new String[]{ username },
                        null, null, null
                );
            } else {
                cursor = db.query(
                        ClsDatabaseCreation.TABLE_USERS,
                        new String[]{ ClsDatabaseCreation.COL_USER_ID },
                        ClsDatabaseCreation.COL_USER_USERNAME + " = ? AND " +
                                ClsDatabaseCreation.COL_USER_ID + " != ?",
                        new String[]{ username, String.valueOf(excludeUserId) },
                        null, null, null
                );
            }
            taken = (cursor != null && cursor.getCount() > 0);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return taken;
    }

    //  VALIDATE

    public boolean isEmailTaken(String email, int excludeUserId) {
        if (email == null || email.isEmpty()) return false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        boolean taken = false;
        Cursor cursor = null;

        try {
            if (excludeUserId == -1) {
                cursor = db.query(
                        ClsDatabaseCreation.TABLE_USERS,
                        new String[]{ ClsDatabaseCreation.COL_USER_ID },
                        ClsDatabaseCreation.COL_USER_EMAIL + " = ?",
                        new String[]{ email },
                        null, null, null
                );
            } else {
                cursor = db.query(
                        ClsDatabaseCreation.TABLE_USERS,
                        new String[]{ ClsDatabaseCreation.COL_USER_ID },
                        ClsDatabaseCreation.COL_USER_EMAIL + " = ? AND " +
                                ClsDatabaseCreation.COL_USER_ID + " != ?",
                        new String[]{ email, String.valueOf(excludeUserId) },
                        null, null, null
                );
            }
            taken = (cursor != null && cursor.getCount() > 0);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return taken;
    }

    //  SEARCH

    public List<UserModel> searchUsers(String query) {
        List<UserModel> users = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            String like = "%" + query + "%";
            cursor = db.query(
                    ClsDatabaseCreation.TABLE_USERS,
                    new String[]{
                            ClsDatabaseCreation.COL_USER_ID,
                            ClsDatabaseCreation.COL_USER_NAME,
                            ClsDatabaseCreation.COL_USER_USERNAME,
                            ClsDatabaseCreation.COL_USER_EMAIL,
                            ClsDatabaseCreation.COL_USER_ROLE
                    },
                    ClsDatabaseCreation.COL_USER_NAME + " LIKE ? OR " +
                            ClsDatabaseCreation.COL_USER_USERNAME + " LIKE ?",
                    new String[]{ like, like },
                    null, null,
                    ClsDatabaseCreation.COL_USER_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    UserModel user = new UserModel();
                    user.id       = cursor.getInt(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_ID));
                    user.name     = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_NAME));
                    user.username = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_USERNAME));
                    user.email    = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_EMAIL));
                    user.role     = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_ROLE));
                    users.add(user);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return users;
    }

    //  HELPER

    private void insertPermissions(SQLiteDatabase db, int userId, Map<String, Boolean> permissions) {
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            String moduleName = entry.getKey();
            boolean canAccess = entry.getValue();

            // Get module_id by module name
            Cursor modCursor = db.query(
                    ClsDatabaseCreation.TABLE_MODULES,
                    new String[]{ ClsDatabaseCreation.COL_MODULE_ID },
                    ClsDatabaseCreation.COL_MODULE_NAME + " = ?",
                    new String[]{ moduleName },
                    null, null, null
            );

            if (modCursor != null && modCursor.moveToFirst()) {
                int moduleId = modCursor.getInt(0);
                modCursor.close();

                ContentValues permValues = new ContentValues();
                permValues.put(ClsDatabaseCreation.COL_PERM_USER_ID,   userId);
                permValues.put(ClsDatabaseCreation.COL_PERM_MODULE_ID, moduleId);
                permValues.put(ClsDatabaseCreation.COL_PERM_ACCESS,    canAccess ? 1 : 0);

                db.insertWithOnConflict(
                        ClsDatabaseCreation.TABLE_ROLE_PERMISSIONS,
                        null,
                        permValues,
                        SQLiteDatabase.CONFLICT_REPLACE
                );
            } else {
                if (modCursor != null) modCursor.close();
            }
        }
    }

    //  COUNT — Total users

    public int getUserCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + ClsDatabaseCreation.TABLE_USERS, null);
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return count;
    }
}