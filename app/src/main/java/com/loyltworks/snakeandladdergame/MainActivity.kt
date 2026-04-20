package com.loyltworks.snakeandladdergame

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.SystemBarStyle
import android.graphics.Color
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private var tvStatus: TextView? = null
    
    // Player boxes and UI elements
    private val playerBoxes = arrayOfNulls<View>(4)
    private val diceResultViews = arrayOfNulls<TextView>(4)
    private val playerIndicators = arrayOfNulls<ImageView>(4)
    private val diceGifViews = arrayOfNulls<GifImageView>(4)
    private val diceContainers = arrayOfNulls<View>(4)
    private val playerNames = arrayOfNulls<TextView>(4)

    private var playerPos = IntArray(4) { 1 }
    private var currentPlayerIdx = 0
    private var numPlayers = 2
    private var isVSai = false
    private var isGameOver = false
    private var isInteractionDisabled = false

    private val snakes = mapOf(
        98 to 2,
        85 to 52,
        61 to 40,
        54 to 16,
        32 to 7
    )
    private val ladders = mapOf(
        3 to 24,
        14 to 77,
        30 to 51,
        41 to 62,
        53 to 72,
        88 to 93
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        gameView = findViewById(R.id.gameView)
        tvStatus = findViewById(R.id.tvStatus)
        
        playerBoxes[0] = findViewById(R.id.boxPlayer1)
        playerBoxes[1] = findViewById(R.id.boxPlayer2)
        playerBoxes[2] = findViewById(R.id.boxPlayer3)
        playerBoxes[3] = findViewById(R.id.boxPlayer4)
        
        playerIndicators[0] = findViewById(R.id.ivPlayIndicator1)
        playerIndicators[1] = findViewById(R.id.ivPlayIndicator2)
        playerIndicators[2] = findViewById(R.id.ivPlayIndicator3)
        playerIndicators[3] = findViewById(R.id.ivPlayIndicator4)

        diceResultViews[0] = findViewById(R.id.tvDiceResult1)
        diceResultViews[1] = findViewById(R.id.tvDiceResult2)
        diceResultViews[2] = findViewById(R.id.tvDiceResult3)
        diceResultViews[3] = findViewById(R.id.tvDiceResult4)

        diceGifViews[0] = findViewById(R.id.ivDiceGif1)
        diceGifViews[1] = findViewById(R.id.ivDiceGif2)
        diceGifViews[2] = findViewById(R.id.ivDiceGif3)
        diceGifViews[3] = findViewById(R.id.ivDiceGif4)

        diceContainers[0] = findViewById(R.id.diceContainer1)
        diceContainers[1] = findViewById(R.id.diceContainer2)
        diceContainers[2] = findViewById(R.id.diceContainer3)
        diceContainers[3] = findViewById(R.id.diceContainer4)

        playerNames[0] = findViewById(R.id.tvPlayer1)
        playerNames[1] = findViewById(R.id.tvPlayer2)
        playerNames[2] = findViewById(R.id.tvPlayer3)
        playerNames[3] = findViewById(R.id.tvPlayer4)

        // Game mode selection from Intent
        numPlayers = intent.getIntExtra("numPlayers", 2)
        isVSai = intent.getBooleanExtra("isVSai", false)

        setupGameMode(numPlayers, isVSai)

        // Rolling dice is now handled by clicking the player's dice area or box
        val playerDiceContainers = arrayOf(
            R.id.diceContainer1, R.id.diceContainer2, R.id.diceContainer3, R.id.diceContainer4
        )
        val playerBoxIds = arrayOf(
            R.id.boxPlayer1, R.id.boxPlayer2, R.id.boxPlayer3, R.id.boxPlayer4
        )

        for (i in 0 until 4) {
            val clickListener = View.OnClickListener {
                if (i == currentPlayerIdx && !isGameOver && !isInteractionDisabled && (isVSai.not() || i == 0)) {
                    // Only allow rolling if it's the current player's turn, not animating,
                    // and either it's not VS AI or it is player 1's turn
                    rollDice()
                }
            }
            findViewById<View>(playerDiceContainers[i])?.setOnClickListener(clickListener)
            findViewById<View>(playerBoxIds[i])?.setOnClickListener(clickListener)
        }
    }
    
    private fun setupGameMode(count: Int, vsAi: Boolean) {
        numPlayers = count
        isVSai = vsAi
        resetGame()
        
        // Update names if VS AI
        if (isVSai) {
            playerNames[0]?.text = "You"
            playerNames[1]?.text = "Computer"
        } else {
            for (i in 0 until 4) {
                playerNames[i]?.text = "Player ${i + 1}"
            }
        }
        
        // Hide unused boxes
        for (i in 0 until 4) {
            playerBoxes[i]?.visibility = if (i < numPlayers) View.VISIBLE else View.INVISIBLE
            if (i >= numPlayers) {
                playerPos[i] = 0 // Offset or hidden
            } else {
                playerPos[i] = 1
            }
        }
        gameView.setPositions(playerPos)
    }

    private fun rollDice() {
        // Disable interaction during roll
        setDiceInteractionEnabled(false)
        val diceValue = Random.nextInt(1, 7)
        
        // Resource IDs for GIFs: R.raw.dice1, R.raw.dice2, etc.
        val gifResId = when (diceValue) {
            1 -> R.raw.dice1
            2 -> R.raw.dice2
            3 -> R.raw.dice3
            4 -> R.raw.dice4
            5 -> R.raw.dice5
            6 -> R.raw.dice6
            else -> R.raw.dice1
        }

        // Play GIF for current player
        val gifView = diceGifViews[currentPlayerIdx]
        val textView = diceResultViews[currentPlayerIdx]
        val container = diceContainers[currentPlayerIdx]
        
        container?.visibility = View.VISIBLE
        
        if (gifView != null) {
            try {
                val gifDrawable = GifDrawable(resources, gifResId)
                gifDrawable.loopCount = 1
                gifView.setImageDrawable(gifDrawable)
                gifView.visibility = View.VISIBLE
                textView?.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
                textView?.visibility = View.VISIBLE
                textView?.text = diceValue.toString()
            }
        } else {
            textView?.text = diceValue.toString()
        }
        
        // Hide other players' GIFs and show their "?" text, hide their dice boxes
        for (i in 0 until 4) {
            if (i != currentPlayerIdx) {
                diceGifViews[i]?.visibility = View.GONE
                diceResultViews[i]?.visibility = View.VISIBLE
                diceResultViews[i]?.text = "?"
                diceContainers[i]?.visibility = View.INVISIBLE
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            movePlayer(diceValue)
        }, 1500) // Increased delay to allow GIF to play
    }

    private fun setDiceInteractionEnabled(enabled: Boolean) {
        isInteractionDisabled = !enabled
    }

    private fun movePlayer(diceValue: Int) {
        val currentPos = playerPos[currentPlayerIdx]
        val targetPos = currentPos + diceValue
        
        if (targetPos > 100) {
            // Player must roll exactly the required number to reach 100. 
            // If the roll pushes them out of bounds, skip their movement entirely.
            finishTurn(diceValue)
        } else if (targetPos > currentPos) {
            val steps = targetPos - currentPos
            playerPos[currentPlayerIdx] = targetPos
            gameView.setPositions(playerPos, animateIdx = currentPlayerIdx, stepByStep = true)
            
            val delay = steps * 250L
            Handler(Looper.getMainLooper()).postDelayed({
                checkSnakesLaddersAfterMove(diceValue)
            }, delay)
        } else {
            finishTurn(diceValue)
        }
    }

    private fun checkSnakesLaddersAfterMove(diceValue: Int) {
        val currentPos = playerPos[currentPlayerIdx]
        val finalPos = getFinalPosition(currentPos)
        
        if (finalPos != currentPos) {
            showSnakeLadderToast(currentPos)
            if (snakes.containsKey(currentPos)) {
                gameView.playSnakeBiteAnimation(currentPos, finalPos, currentPlayerIdx) {
                    playerPos[currentPlayerIdx] = finalPos
                    finishTurn(diceValue)
                }
            } else { // Ladder
                playerPos[currentPlayerIdx] = finalPos
                gameView.setPositions(playerPos, animateIdx = currentPlayerIdx, stepByStep = false)
                Handler(Looper.getMainLooper()).postDelayed({
                    finishTurn(diceValue)
                }, 600)
            }
        } else {
            finishTurn(diceValue)
        }
    }

    private fun finishTurn(diceValue: Int) {
        checkWinCondition()
        
        // Re-enable interactions for the next player (or the same player if bonus roll)
        setDiceInteractionEnabled(true)
        
        if (!isGameOver) {
            // Bonus turn if 1 or 6
            if (diceValue == 1 || diceValue == 6) {
                Toast.makeText(this, "Bonus Turn!", Toast.LENGTH_SHORT).show()
            } else {
                currentPlayerIdx = (currentPlayerIdx + 1) % numPlayers
            }
            updateStatusUI()
            
            if (isVSai && currentPlayerIdx == 1) {
                Handler(Looper.getMainLooper()).postDelayed({
                    rollDice()
                }, 1000)
            }
        }
    }

    private fun getFinalPosition(pos: Int): Int {
        return snakes[pos] ?: ladders[pos] ?: pos
    }

    private fun showSnakeLadderToast(pos: Int) {
        if (snakes.containsKey(pos)) {
            Toast.makeText(this, "Oops! A Snake!", Toast.LENGTH_SHORT).show()
        } else if (ladders.containsKey(pos)) {
            Toast.makeText(this, "Yay! A Ladder!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkWinCondition() {
        if (playerPos[currentPlayerIdx] == 100) {
            isGameOver = true
            val winnerName = if (isVSai) {
                if (currentPlayerIdx == 0) "YOU" else "COMPUTER"
            } else {
                "Player ${currentPlayerIdx + 1}"
            }
            Toast.makeText(this, "Game Over! Winner: $winnerName", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateStatusUI() {
        val currentName = if (isVSai) {
            if (currentPlayerIdx == 0) "Your Turn" else "Computer's Turn"
        } else {
            "Player ${currentPlayerIdx + 1}'s Turn"
        }
        tvStatus?.text = currentName
        
        // Show indicator and dice area for current player only
        for (i in 0 until 4) {
            playerIndicators[i]?.visibility = if (i == currentPlayerIdx && i < numPlayers) View.VISIBLE else View.INVISIBLE
            // Reset state for other players
            if (i != currentPlayerIdx) {
                diceResultViews[i]?.visibility = View.VISIBLE
                diceResultViews[i]?.text = "?"
                diceGifViews[i]?.visibility = View.GONE
                diceContainers[i]?.visibility = View.INVISIBLE
            } else {
                diceContainers[i]?.visibility = View.VISIBLE
            }
        }
    }

    private fun resetGame() {
        playerPos = IntArray(4) { 1 }
        currentPlayerIdx = 0
        isGameOver = false
        isInteractionDisabled = false
        for (i in 0 until 4) {
            diceResultViews[i]?.visibility = View.VISIBLE
            diceResultViews[i]?.text = "?"
            diceGifViews[i]?.visibility = View.GONE
            diceContainers[i]?.visibility = if (i == 0) View.VISIBLE else View.INVISIBLE
        }
        updateStatusUI()
        gameView.setPositions(playerPos)
    }
}