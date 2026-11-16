package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Game extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView optionAView;
    private TextView optionBView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // access Firebase database
        db = FirebaseFirestore.getInstance();

        optionAView = findViewById(R.id.OptionA);
        optionBView = findViewById(R.id.OptionB);

        loadRandomQuestion();
    }

    private void loadRandomQuestion() {
        optionAView.setText("Loading...");
        optionBView.setText("Loading...");
        double rand = Math.random();

        // use rand value for each question in the database to pick a random question
        db.collection("questions")
            .orderBy("rand")
            .startAt(rand)
            .limit(1)
            .get()
            .addOnSuccessListener(snap -> {
                if (!snap.isEmpty()) {
                    showQuestion(snap.getDocuments().get(0));
                }
                else {
                    // random point landed past the last item in database, so wrap around
                    db.collection("questions").orderBy("rand").limit(1).get()
                        .addOnSuccessListener(first -> {
                            if (!first.isEmpty()) {
                                showQuestion(first.getDocuments().get(0));
                            }
                            else {
                                optionAView.setText("No questions found in database.");
                                optionBView.setText("No questions found in database.");
                            }
                        })
                        .addOnFailureListener(this::showError);
                }
            })
            .addOnFailureListener(this::showError);
    }

    private void showQuestion(DocumentSnapshot doc) {
        // text for option A and B
        String optionA = doc.getString("optionA");
        String optionB = doc.getString("optionB");

        // vote counts for A and B
        Long votesA = doc.getLong("votesA");
        votesA = votesA == null ? 0 : votesA;
        Long votesB = doc.getLong("votesB");
        votesB = votesB == null ? 0 : votesB;

        optionAView.setText(optionA + " (" + votesA + " votes)");
        optionBView.setText(optionB + " (" + votesB + " votes)");
    }

    private void showError(Exception e) {
        optionAView.setText("Error loading question: " + e.getMessage());
        optionBView.setText("Error loading question: " + e.getMessage());
    }

    public void onLeftPressed(View view) {
        // left answer (Option A) selected
        loadRandomQuestion();
    }

    public void onRightPressed(View view) {
        // right answer (Option B) selected
        loadRandomQuestion();
    }

    public void onBackPressed(View view) {
        Intent i = new Intent(getBaseContext(), Title.class);
        startActivity(i);
    }
}