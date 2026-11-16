package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class Start extends AppCompatActivity {

    private boolean isTeamMode = false;
    private ArrayList<String> players = new ArrayList<>();
    private TableLayout tablePlayerList;
    private TextView tvTitle;
    private Button btnToggleTeams;
    private View teamsDivider;
    private ArrayList<EditText> playerInputs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_start);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tablePlayerList = findViewById(R.id.tablePlayerList);
        tvTitle = findViewById(R.id.tvTitle);
        btnToggleTeams = findViewById(R.id.btnToggleTeams);
        teamsDivider = findViewById(R.id.teamsDivider);

        Button btnAddPlayer = findViewById(R.id.btnAddPlayer);
        btnAddPlayer.setOnClickListener(v -> addPlayerInput("Player " + (playerInputs.size() + 1)));

        Button btnRemovePlayer = findViewById(R.id.btnRemovePlayer);
        btnRemovePlayer.setOnClickListener(v -> {
            // Prevent removing the very last player
            if (playerInputs.size() > 1) {
                playerInputs.remove(playerInputs.size() - 1);
                refreshPlayerList();
            }
        });

        btnToggleTeams.setOnClickListener(v -> {
            isTeamMode = !isTeamMode;
            if (isTeamMode) {
                tvTitle.setText("Teams");
                btnToggleTeams.setText("Solo");
                teamsDivider.setVisibility(View.VISIBLE);
            } else {
                tvTitle.setText("Players");
                btnToggleTeams.setText("Teams");
                teamsDivider.setVisibility(View.GONE);
            }
            refreshPlayerList();
        });

        // Start with one player
        addPlayerInput("Player 1");
    }

    private void addPlayerInput(String hintName) {
        EditText editText = new EditText(this);
        editText.setHint(hintName);
        editText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        playerInputs.add(editText);
        refreshPlayerList();
    }

    private void refreshPlayerList() {
        tablePlayerList.removeAllViews();

        if (isTeamMode) {
            // Display in team mode
            for (int i = 0; i < playerInputs.size(); i += 2) {
                TableRow row = new TableRow(this);
                row.setLayoutParams(new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT
                ));

                // Remove from parent before adding to new row
                EditText p1 = playerInputs.get(i);
                if (p1.getParent() != null) ((TableRow) p1.getParent()).removeView(p1);
                row.addView(p1);

                if (i + 1 < playerInputs.size()) {
                    EditText p2 = playerInputs.get(i + 1);
                    if (p2.getParent() != null) ((TableRow) p2.getParent()).removeView(p2);
                    row.addView(p2);
                } else {
                    // Empty view for alignment
                    View spacer = new View(this);
                    row.addView(spacer);
                }
                tablePlayerList.addView(row);
            }
        } else {
            // Display in single player mode
            for (EditText et : playerInputs) {
                if (et.getParent() != null) {
                    ((TableRow) et.getParent()).removeView(et);
                }
                TableRow row = new TableRow(this);
                row.setGravity(Gravity.CENTER);
                row.addView(et);
                tablePlayerList.addView(row);
            }
        }
    }

    public void onBackPressed(View view) {
        Intent i = new Intent(getBaseContext(), Title.class);
        startActivity(i);
    }

    public void onStartGamePressed(View view) {
        players.clear();
        for (EditText et : playerInputs) {
            String name = et.getText().toString().trim();
            if (name.isEmpty()) {
                name = et.getHint().toString();
            }
            players.add(name);
        }

        Intent i = new Intent(getBaseContext(), Game.class);
        i.putStringArrayListExtra("PLAYERS", players);
        i.putExtra("IS_TEAM_MODE", isTeamMode);
        startActivity(i);
    }
}