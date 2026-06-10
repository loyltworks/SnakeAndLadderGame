package com.loyltworks.snakeandladdergame

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import android.graphics.Color
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class SelectionActivity : AppCompatActivity() {

    private var selectedPlayers = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContentView(R.layout.activity_selection)

        val layoutModeSelection = findViewById<View>(R.id.layoutModeSelection)
        val layoutPlayerSelection = findViewById<View>(R.id.layoutPlayerSelection)

        val btnPlayWithFriends = findViewById<View>(R.id.btnPlayWithFriends)
        val btnVersusAi = findViewById<View>(R.id.btnVersusAi)

        val btn2Players = findViewById<View>(R.id.btn2Players)
        val btn3Players = findViewById<View>(R.id.btn3Players)
        val btn4Players = findViewById<View>(R.id.btn4Players)

        val btnPlay = findViewById<View>(R.id.btnPlay)
        val btnHome = findViewById<View>(R.id.btnHome)

        val btnExit = findViewById<View>(R.id.btnExit)

        btnExit.setOnClickListener {
            finish() // Exit the game activity back to the caller
        }

        btnPlayWithFriends.setOnClickListener {
            layoutModeSelection.visibility = View.GONE
            layoutPlayerSelection.visibility = View.VISIBLE
            btnExit.visibility = View.GONE
            btnHome.visibility = View.VISIBLE
        }

        btnVersusAi.setOnClickListener {
            startGame(2, true)
        }

        btnHome.setOnClickListener {
            layoutPlayerSelection.visibility = View.GONE
            layoutModeSelection.visibility = View.VISIBLE
            btnHome.visibility = View.GONE
            btnExit.visibility = View.VISIBLE
        }

        btn2Players.setOnClickListener {
            selectedPlayers = 2
            btn2Players.alpha = 1.0f
            btn3Players.alpha = 0.4f
            btn4Players.alpha = 0.4f
        }

        btn3Players.setOnClickListener {
            selectedPlayers = 3
            btn2Players.alpha = 0.4f
            btn3Players.alpha = 1.0f
            btn4Players.alpha = 0.4f
        }

        btn4Players.setOnClickListener {
            selectedPlayers = 4
            btn2Players.alpha = 0.4f
            btn3Players.alpha = 0.4f
            btn4Players.alpha = 1.0f
        }

        btnPlay.setOnClickListener {
            startGame(selectedPlayers, false)
        }
    }

    private fun startGame(numPlayers: Int, isVSai: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("numPlayers", numPlayers)
        intent.putExtra("isVSai", isVSai)
        startActivity(intent)
    }
}
