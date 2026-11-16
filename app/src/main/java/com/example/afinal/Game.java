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

import java.util.ArrayList;

public class Game extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor gravitySensor;

    private View fillLeft, fillRight;
    private TextView tvQuestion, tvOptionLeft, tvOptionRight;
    private TextView tvStatsLeft, tvStatsRight, tvCurrentPlayer;
    private Button btnNextQuestion;

    private ArrayList<String> players;
    private boolean isTeamMode;
    private int currentPlayerIndex = 0;

    private float currentLeftFill = 0f;
    private float currentRightFill = 0f;
    private boolean isChoiceLocked = false;

    // Question data structure
    private static class QuestionData {
        String question;
        String optLeft;
        String optRight;
        int statLeft;
        int statRight;

        QuestionData(String q, String l, String r, int sl, int sr) {
            question = q;
            optLeft = l;
            optRight = r;
            statLeft = sl;
            statRight = sr;
        }
    }

    private ArrayList<QuestionData> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;

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

        fillLeft = findViewById(R.id.fillLeft);
        fillRight = findViewById(R.id.fillRight);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvOptionLeft = findViewById(R.id.tvOptionLeft);
        tvOptionRight = findViewById(R.id.tvOptionRight);
        tvStatsLeft = findViewById(R.id.tvStatsLeft);
        tvStatsRight = findViewById(R.id.tvStatsRight);
        tvCurrentPlayer = findViewById(R.id.tvCurrentPlayer);
        btnNextQuestion = findViewById(R.id.btnNextQuestion);

        if (getIntent() != null) {
            players = getIntent().getStringArrayListExtra("PLAYERS");
            isTeamMode = getIntent().getBooleanExtra("IS_TEAM_MODE", false);
        }

        setupDummyData();

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

    // Can get rid of this once Firebase questions set up
    private void setupDummyData() {
        questions.add(new QuestionData("Be able to fly or be invisible?", "Fly", "Invisible", 65, 35));
        questions.add(new QuestionData("Travel 100 years to the past or future?", "Past", "Future", 20, 80));
        questions.add(new QuestionData("Never eat pizza again or never eat burgers again?", "No Pizza", "No Burgers", 45, 55));
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
            // For teams, assigning players alternating Team 1 vs Team 2
            int teamNum = (currentPlayerIndex % 2) + 1;
            tvCurrentPlayer.setText("Team " + teamNum + ": " + playerName);
        } else {
            tvCurrentPlayer.setText("Player: " + playerName);
        }

        // Load question data
        QuestionData qData = questions.get(currentQuestionIndex % questions.size());
        tvQuestion.setText(qData.question);
        tvOptionLeft.setText(qData.optLeft);
        tvOptionRight.setText(qData.optRight);

        resumeSensor();
    }

    private void loadNextTurn() {
        currentPlayerIndex++;
        currentQuestionIndex++;
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

        // Show Stats
        QuestionData qData = questions.get(currentQuestionIndex % questions.size());
        tvStatsLeft.setText(qData.statLeft + "%");
        tvStatsRight.setText(qData.statRight + "%");
        tvStatsLeft.setVisibility(View.VISIBLE);
        tvStatsRight.setVisibility(View.VISIBLE);

        btnNextQuestion.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isChoiceLocked) return;

        // X-axis gravity:
        // Tilted left, x is positive.
        // Tilted right, x is negative.
        float x = event.values[0];

        float maxTilt = 8.0f;
        float decayRate = 0.02f;
        float fillRate = 0.04f;

        if (x > 2.0f) {
            // Tilting left
            currentLeftFill += fillRate * (x / maxTilt);
            currentRightFill -= decayRate * 2;
        } else if (x < -2.0f) {
            // Tilting right
            currentRightFill += fillRate * (-x / maxTilt);
            currentLeftFill -= decayRate * 2;
        } else {
            // Neutral, decay both
            currentLeftFill -= decayRate;
            currentRightFill -= decayRate;
        }

        // Clamp values
        currentLeftFill = Math.max(0f, Math.min(1f, currentLeftFill));
        currentRightFill = Math.max(0f, Math.min(1f, currentRightFill));

        // Apply to UI
        fillLeft.setScaleY(currentLeftFill);
        fillRight.setScaleY(currentRightFill);

        // Check for lock
        if (currentLeftFill >= 1f) {
            lockChoice(true);
        } else if (currentRightFill >= 1f) {
            lockChoice(false);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void resumeSensor() {
        if (sensorManager != null && gravitySensor != null) {
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