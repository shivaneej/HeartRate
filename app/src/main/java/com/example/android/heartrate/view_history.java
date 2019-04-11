package com.example.android.heartrate;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class view_history extends AppCompatActivity {

    private String usermail1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_history);
        Bundle b = getIntent().getExtras();
        usermail1 = b.getString("email");
        final LinearLayout linearLayout=findViewById(R.id.linearlayout);
        FirebaseApp.initializeApp(view_history.this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(usermail1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
//Log.d(TAG, document.getId() + " => " + document.getData());
                                TextView tv= new TextView(view_history.this);
                                tv.setText(document.getData().get("data").toString());
//tv.setTextColor();
//tv.
                                linearLayout.addView(tv);
                            }
                        } else {
//Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

//        db.collection(usermail1)
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                                //Log.d("Blah", );
////                                Log.d("Blah", document.getTimestamp("timestamp"));
//                                TextView tv= new TextView(view_history.this);
//                                tv.setText(document.getData().get("data").toString());
////                            Log.d("Blah",document.getI());
//                            }
//                        } else {
//                            Log.d("Blah", "Error getting documents: ");
//                        }
//                    }
//                });
    }
}
