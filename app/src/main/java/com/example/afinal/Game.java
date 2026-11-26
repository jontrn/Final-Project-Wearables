package com.example.afinal;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class Game extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor gravitySensor;

    private View fillLeft, fillRight;
    private TextView tvQuestion, tvOptionLeft, tvOptionRight;
    private TextView tvStatsLeft, tvStatsRight, tvCurrentPlayer, tvScore;
    private Button btnNextQuestion;

    private ArrayList<String> players;
    private boolean isTeamMode;
    private int currentPlayerIndex = 0;
    private int[] scores;

    private float currentLeftFill = 0f;
    private float currentRightFill = 0f;
    private boolean isChoiceLocked = false;

    private FirebaseFirestore db;
    private long currentVotesLeft = 0;
    private long currentVotesRight = 0;

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

        db = FirebaseFirestore.getInstance();

        fillLeft = findViewById(R.id.fillLeft);
        fillRight = findViewById(R.id.fillRight);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvOptionLeft = findViewById(R.id.tvOptionLeft);
        tvOptionRight = findViewById(R.id.tvOptionRight);
        tvStatsLeft = findViewById(R.id.tvStatsLeft);
        tvStatsRight = findViewById(R.id.tvStatsRight);
        tvCurrentPlayer = findViewById(R.id.tvCurrentPlayer);
        tvScore = findViewById(R.id.tvScore);
        btnNextQuestion = findViewById(R.id.btnNextQuestion);

        if (getIntent() != null) {
            players = getIntent().getStringArrayListExtra("PLAYERS");
            isTeamMode = getIntent().getBooleanExtra("IS_TEAM_MODE", false);
        }
        if (players == null) players = new ArrayList<>();
        if (players.isEmpty()) players.add("Player 1");

        if (isTeamMode) {
            scores = new int[2];
        } else {
            scores = new int[players.size()];
        }

        // Setup pivot for fill views to scale from the bottom
        View mainView = findViewById(R.id.main);
        mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                fillLeft.setPivotY(fillLeft.getHeight());
                fillRight.setPivotY(fillRight.getHeight());
            }
        });

        btnNextQuestion.setOnClickListener(v -> loadNextTurn());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        }

        loadCurrentTurn();
    }

    private void loadCurrentTurn() {
        isChoiceLocked = false;
        currentLeftFill = 0f;
        currentRightFill = 0f;
        fillLeft.setScaleY(0f);
        fillRight.setScaleY(0f);

        tvStatsLeft.setVisibility(View.GONE);
        tvStatsRight.setVisibility(View.GONE);
        btnNextQuestion.setVisibility(View.GONE);

        // Update Player text
        String playerName = players.get(currentPlayerIndex % players.size());
        if (isTeamMode) {
            int teamNum = (currentPlayerIndex % 2) + 1;
            tvCurrentPlayer.setText("Team " + teamNum + ": " + playerName);
        } else {
            tvCurrentPlayer.setText("Player: " + playerName);
        }

        updateScoreDisplay();

        loadRandomQuestion();
    }

    private void updateScoreDisplay() {
        if (isTeamMode) {
            int teamNum = (currentPlayerIndex % 2) + 1;
            tvScore.setText("Team " + teamNum + " Score: " + scores[currentPlayerIndex % 2]);
        } else {
            tvScore.setText("Score: " + scores[currentPlayerIndex % players.size()]);
        }
    }

    private void loadRandomQuestion() {
        tvOptionLeft.setText("Loading...");
        tvOptionRight.setText("Loading...");
        pauseSensor(); // pause while loading

        double rand = Math.random();

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
                                tvOptionLeft.setText("No questions found.");
                                tvOptionRight.setText("No questions found.");
                            }
                        })
                        .addOnFailureListener(this::showError);
                }
            })
            .addOnFailureListener(this::showError);
    }

    private void showQuestion(DocumentSnapshot doc) {
        String optionA = doc.getString("optionA");
        String optionB = doc.getString("optionB");

        Long votesA = doc.getLong("votesA");
        currentVotesLeft = votesA == null ? 0 : votesA;
        Long votesB = doc.getLong("votesB");
        currentVotesRight = votesB == null ? 0 : votesB;

        tvOptionLeft.setText(optionA);
        tvOptionRight.setText(optionB);
        
        // Ready for sensor input
        resumeSensor();
    }

    private void showError(Exception e) {
        tvOptionLeft.setText("Error loading.");
        tvOptionRight.setText("Error loading.");
    }

    private void loadNextTurn() {
        currentPlayerIndex++;
        loadCurrentTurn();
    }

    private void lockChoice(boolean choseLeft) {
        isChoiceLocked = true;
        pauseSensor();

        // Snap to full
        if (choseLeft) {
            fillLeft.setScaleY(1f);
            fillRight.setScaleY(0f);
        } else {
            fillRight.setScaleY(1f);
            fillLeft.setScaleY(0f);
        }

        // Check if point awarded (guessed the majority)
        boolean isCorrect = false;
        if (choseLeft && currentVotesLeft >= currentVotesRight) {
            isCorrect = true;
        } else if (!choseLeft && currentVotesRight >= currentVotesLeft) {
            isCorrect = true;
        }

        if (isCorrect) {
            if (isTeamMode) {
                scores[currentPlayerIndex % 2]++;
            } else {
                scores[currentPlayerIndex % players.size()]++;
            }
            updateScoreDisplay();
        }

        // Show Stats based on Firebase data
        long total = currentVotesLeft + currentVotesRight;
        int statLeft = 0;
        int statRight = 0;
        if (total > 0) {
            statLeft = (int) ((currentVotesLeft * 100) / total);
            statRight = (int) ((currentVotesRight * 100) / total);
        } else {
            // Default 50/50 if nobody has voted yet
            statLeft = 50;
            statRight = 50;
        }

        tvStatsLeft.setText(statLeft + "%");
        tvStatsRight.setText(statRight + "%");
        tvStatsLeft.setVisibility(View.VISIBLE);
        tvStatsRight.setVisibility(View.VISIBLE);

        btnNextQuestion.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isChoiceLocked) return;

        float x = event.values[0];

        float maxTilt = 8.0f;
        float decayRate = 0.02f;
        float fillRate = 0.04f;

        if (x > 2.0f) {
            currentLeftFill += fillRate * (x / maxTilt);
            currentRightFill -= decayRate * 2;
        } else if (x < -2.0f) {
            currentRightFill += fillRate * (-x / maxTilt);
            currentLeftFill -= decayRate * 2;
        } else {
            currentLeftFill -= decayRate;
            currentRightFill -= decayRate;
        }

        currentLeftFill = Math.max(0f, Math.min(1f, currentLeftFill));
        currentRightFill = Math.max(0f, Math.min(1f, currentRightFill));

        fillLeft.setScaleY(currentLeftFill);
        fillRight.setScaleY(currentRightFill);

        if (currentLeftFill >= 1f) {
            lockChoice(true);
        } else if (currentRightFill >= 1f) {
            lockChoice(false);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void resumeSensor() {
        if (sensorManager != null && gravitySensor != null && !isChoiceLocked) {
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    private void pauseSensor() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isChoiceLocked) {
            resumeSensor();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseSensor();
    }

    public void onBackPressed(View view) {
        pauseSensor();
        Intent i = new Intent(getBaseContext(), Title.class);
        startActivity(i);
        finish();
    }
}
