package com.loyltworks.snakeandladdergame

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selection)

        findViewById<Button>(R.id.btn2P).setOnClickListener { startGame(2, false) }
        findViewById<Button>(R.id.btn3P).setOnClickListener { startGame(3, false) }
        findViewById<Button>(R.id.btn4P).setOnClickListener { startGame(4, false) }
        findViewById<Button>(R.id.btnAI).setOnClickListener { startGame(2, true) }
    }

    private fun startGame(numPlayers: Int, isVSai: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("numPlayers", numPlayers)
        intent.putExtra("isVSai", isVSai)
        startActivity(intent)
    }
}
