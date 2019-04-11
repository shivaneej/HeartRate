package com.example.android.heartrate;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{

    private Button mRunButton,signOutButton;
    public String s;
    private static final int RESULTS_CODE = 123;
    private int mHeartRate;
    public static Context context;
    private FirebaseAuth.AuthStateListener authListener;
    public TextView email;
    private FirebaseAuth auth;
    private GoogleSignInClient mGoogleSignInClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        signOutButton = (Button) findViewById(R.id.signout);
        Button button=findViewById(R.id.view_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this,view_history.class);
                intent.putExtra("email",s);
                startActivity(intent);
            }
        });
        email = (TextView)findViewById(R.id.useremail);
        auth = FirebaseAuth.getInstance();
        context = this;
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mRunButton =  findViewById(R.id.run_button);
        mRunButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(MainActivity.this,CameraActivity.class);
                i.putExtra("email",s);
                startActivity(i);
            }
        });
        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null)
                {
                    Log.d("HEY","User is null");
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }else{
                    s=user.getEmail();
                    email.setText(user.getEmail());
                    Log.d("HEY","Signed in");
                }
            }
        };

        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                auth.signOut();
                mGoogleSignInClient.signOut();
                Log.d("HEY","Signed out");
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });

    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        auth.addAuthStateListener(authListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) {
            auth.removeAuthStateListener(authListener);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
