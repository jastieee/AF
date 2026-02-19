package com.nssi.anytimefitness.Activity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nssi.anytimefitness.Connection.ClsDatabaseCreation;
import com.nssi.anytimefitness.R;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private ImageView ivTogglePassword;
    private boolean isPasswordVisible = false;

    private ClsDatabaseCreation dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen / hide status bar for immersive feel
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_login);

        dbHelper = new ClsDatabaseCreation(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

        // Toggle password visibility
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            } else {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(R.drawable.ic_visibility);
            }
            isPasswordVisible = !isPasswordVisible;
            etPassword.setSelection(etPassword.getText().length()); // keep cursor at end
        });

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty()) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + ClsDatabaseCreation.TABLE_USERS +
                        " WHERE " + ClsDatabaseCreation.COL_USER_USERNAME + " = ? " +
                        " AND " + ClsDatabaseCreation.COL_USER_PASSWORD + " = ?",
                new String[]{username, password}
        );

        if (cursor != null && cursor.moveToFirst()) {
            int userId   = cursor.getInt(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_ID));
            String name  = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_NAME));
            String role  = cursor.getString(cursor.getColumnIndexOrThrow(ClsDatabaseCreation.COL_USER_ROLE));
            cursor.close();

            // Pass user session data to the next screen
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("USER_NAME", name);
            intent.putExtra("USER_ROLE", role);
            intent.putExtra("USERNAME", username);
            startActivity(intent);
            finish(); // prevent going back to login

        } else {
            if (cursor != null) cursor.close();
            Toast.makeText(this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
        }

        db.close();
    }
}