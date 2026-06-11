package com.loyltworks.snakeandladdergame

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private var layoutAchievement: View? = null
    private var roundedFram: FrameLayout? = null
    private var ivAchievementGif: ImageView? = null
    private var tvAchievementMessage: TextView? = null
    
    // Player boxes and UI elements
    private val playerBoxes = arrayOfNulls<View>(4)
    private val diceResultViews = arrayOfNulls<ImageView>(4)
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
        setContentView(R.layout.snake_activity_main)
        val rootView = findViewById<View>(R.id.main)
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                // Only apply side and bottom padding. Top padding is handled by the layout header or removed for tighter fit.
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // Header buttons
        findViewById<View>(R.id.btnReset)?.setOnClickListener {
            resetGame()
        }
        findViewById<View>(R.id.btnHome)?.setOnClickListener {
            finish()
        }

        gameView = findViewById(R.id.gameView)
        layoutAchievement = findViewById(R.id.layoutAchievement)
        roundedFram = findViewById(R.id.roundedFram)
        ivAchievementGif = findViewById(R.id.ivAchievementGif)
        tvAchievementMessage = findViewById(R.id.tvAchievementMessage)
        
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
            }
        }
        
        // Hide other players' GIFs and show their "?" text, hide their dice boxes
        for (i in 0 until 4) {
            if (i != currentPlayerIdx) {
                diceGifViews[i]?.visibility = View.GONE
                diceResultViews[i]?.visibility = View.VISIBLE
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
                showAchievement("bonus")
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
            showAchievement("snake")
        } else if (ladders.containsKey(pos)) {
            showAchievement("ladder")
        }
    }

    private fun showAchievement(type: String) {
        val bgRes: Int
        val gifRes: Int
        val framRes: Int
        val message: String
        val textColor: Int

        when (type) {
            "snake" -> {
                bgRes = R.drawable.bg_achievement_snake
                framRes = R.drawable.bg_achievement_snake
                gifRes = R.drawable.sanke_oops
                message = "OOPS!! A snake bit you"
                textColor = Color.parseColor("#B22222")
            }
            "ladder" -> {
                bgRes = R.drawable.bg_achievement_ladder
                framRes = R.drawable.bg_circle_green
                gifRes = R.drawable.ladder_yay
                message = "YAY!! You got a ladder"
                textColor = Color.parseColor("#2E7D32")
            }
            "bonus" -> {
                bgRes = R.drawable.bg_achievement_bonus
                framRes = R.drawable.bg_circle_brown
                gifRes = R.drawable.wow
                message = "WOW!! Another chance"
                textColor = Color.parseColor("#8B4513")
            }
            else -> return
        }

        layoutAchievement?.setBackgroundResource(bgRes)
        roundedFram?.setBackgroundResource(framRes)
        ivAchievementGif?.setImageResource(gifRes)
        tvAchievementMessage?.text = message
        tvAchievementMessage?.setTextColor(textColor)

        layoutAchievement?.visibility = View.VISIBLE
        layoutAchievement?.alpha = 0f
        layoutAchievement?.animate()?.alpha(1f)?.setDuration(300)?.start()

        Handler(Looper.getMainLooper()).postDelayed({
            layoutAchievement?.animate()?.alpha(0f)?.setDuration(300)?.withEndAction {
                layoutAchievement?.visibility = View.INVISIBLE
            }?.start()
        }, 2000)
    }

    private fun checkWinCondition() {
        if (playerPos[currentPlayerIdx] == 100) {
            isGameOver = true
            showWinDialog(currentPlayerIdx)
        }
    }

    private fun showWinDialog(winnerIdx: Int) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.snake_dialog_win)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val ivWinnerPlayerIcon = dialog.findViewById<ImageView>(R.id.ivWinnerPlayerIcon)
        val btnBack = dialog.findViewById<Button>(R.id.btnWinBack)
        val btnPlayAgain = dialog.findViewById<Button>(R.id.btnWinPlayAgain)

        // Set correct player icon
        val playerIconRes = when (winnerIdx) {
            0 -> R.drawable.red_player
            1 -> R.drawable.blue_player
            2 -> R.drawable.green_player
            3 -> R.drawable.white_player
            else -> R.drawable.red_player
        }
        ivWinnerPlayerIcon.setImageResource(playerIconRes)

        btnBack.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        btnPlayAgain.setOnClickListener {
            dialog.dismiss()
            resetGame()
        }

        dialog.show()
    }

    private fun updateStatusUI() {
        // Show indicator and dice area for current player only
        for (i in 0 until 4) {
            playerIndicators[i]?.visibility = if (i == currentPlayerIdx && i < numPlayers) View.VISIBLE else View.INVISIBLE
            // Reset state for other players
            if (i != currentPlayerIdx) {
                diceResultViews[i]?.visibility = View.VISIBLE
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
            diceGifViews[i]?.visibility = View.GONE
            diceContainers[i]?.visibility = if (i == 0) View.VISIBLE else View.INVISIBLE
        }
        updateStatusUI()
        layoutAchievement?.visibility = View.INVISIBLE
        gameView.setPositions(playerPos)
    }
}