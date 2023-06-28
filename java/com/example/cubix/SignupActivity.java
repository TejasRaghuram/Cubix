package com.example.cubix;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    DatabaseReference dRef;
    EditText email;
    EditText name;
    EditText password;
    Button submit;
    TextView login;
    TextView error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        getSupportActionBar().hide();

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        dRef = firebaseDatabase.getReference();

        email = findViewById(R.id.signup_email);
        name = findViewById(R.id.signup_name);
        password = findViewById(R.id.signup_password);
        submit = findViewById(R.id.signup_submit);
        login = findViewById(R.id.signup_login);
        error = findViewById(R.id.signup_error);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(i);
                finish();
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(email.getText().toString().equals("") || password.getText().toString().equals("") || name.getText().toString().equals(""))
                {
                    error.setText("Error: Empty Email, Password, or Name");
                    email.setText("");
                    password.setText("");
                }
                else
                {
                    mAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if(task.isSuccessful())
                                    {
                                        HashMap<String, Object> userData = new HashMap<>();
                                        userData.put("Name", name.getText().toString());
                                        dRef.child("Users")
                                                .child(mAuth.getCurrentUser().getUid())
                                                .setValue(userData);

                                        Intent i = new Intent(getApplicationContext(), LayoutActivity.class);
                                        startActivity(i);
                                        finish();
                                    }
                                    else
                                    {
                                        error.setText("Error: Please Try Again");
                                        email.setText("");
                                        password.setText("");
                                    }
                                }
                            });
                }

            }
        });
    }
}