package com.code

import java.io.File
import java.io.FileOutputStream
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions

class MainActivity : AppCompatActivity() {

    private val MODEL_ASSET = "gemma3-270m-it-q8.task"

    private var llm: LlmInference? = null

    // UI elemek
    private lateinit var scroll: ScrollView
    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var sendBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cím
        val title = TextView(this).apply {
            text = "Gemma 3 – 270M (on-device) • MediaPipe LLM Inference"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
        }

        // Üzenetlista (görgethető)
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)
        }
        scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(
                messages,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        // Input sor + küldés
        input = EditText(this).apply {
            hint = "Írd be a kérdésed…"
            textSize = 16f
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 4
            setPadding(16)
            background = roundedBg(
                radius = 24f,
                fill = 0xFFFFFFFF.toInt(),
                stroke = 0xFFE0E0E0.toInt()
            )
        }

        sendBtn = Button(this).apply {
            text = "Küldés"
            setOnClickListener { onSend() }
        }

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8)
            addView(
                input,
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply { setMargins(8) }
            )
            addView(
                sendBtn,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(8) }
            )
        }

        // Konténer
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24)
            addView(title)
            addView(
                scroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                ).apply {
                    topMargin = 12
                    bottomMargin = 12
                }
            )
            addView(inputRow)
        }

        setContentView(container)

        // LLM init
        initLlm()

        // Kezdő üzenet
        addAssistantBubble("Szia! Készen állok. Kérdezz bátran. 🙂")
    }

    private fun ensureModelOnDisk(assetName: String): String {
        // kimásoljuk az assets-ből az app saját tárhelyére
        val outFile = File(filesDir, assetName)
        if (!outFile.exists()) {
            assets.open(assetName).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outFile.absolutePath
    }

private fun initLlm() {
    try {
        val dst = File(filesDir, MODEL_ASSET)
        if (!dst.exists()) {
            assets.open(MODEL_ASSET).use { input ->
                FileOutputStream(dst).use { output -> input.copyTo(output) }
            }
        }
        val opts = LlmInferenceOptions.builder()
            .setModelPath(dst.absolutePath)
            .setMaxTokens(1024)
            .build()
        llm = LlmInference.createFromOptions(this, opts)
    } catch (e: Exception) {
        addAssistantBubble("LLM init hiba: ${e.message}")
        llm = null
    }
}
    
    private fun onSend() {
        val text = input.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return

        addUserBubble(text)
        input.setText("")
        generate(text)
    }

    private fun generate(userQuestion: String) {
        if (userQuestion.isBlank()) return

        // 1) Prompt összeállítás
        val prompt = buildString {
            appendLine("Válaszolj tömören és magyarul.")
            append("Kérdés: ")
            appendLine(userQuestion)
        }

        // 2) „Gondolkodom…” buborék
        addAssistantBubble("Gondolkodom…")

        // 3) LLM példány ellenőrzése
        val engine = llm ?: run {
            replaceLastThinkingIfAny("Hiba: LLM nem inicializált.")
            return
        }

        // 4) Coroutine indítás (Main), LLM hívás IO szálon
        lifecycleScope.launch {
            val reply = withContext(Dispatchers.IO) {
                try {
                    engine.generateResponse(prompt)
                } catch (e: Exception) {
                    "Hiba történt: ${e.message ?: "ismeretlen hiba"}"
                }
            }
            // 5) UI frissítés a fő szálon
            replaceLastThinkingIfAny(reply)
        }
    }

    // ——— Buborékkészítés ———

    private fun addUserBubble(text: String) = addBubble(text, isUser = true)
    private fun addAssistantBubble(text: String) = addBubble(text, isUser = false)

    private fun addBubble(text: String, isUser: Boolean) {
        val bubble = TextView(this).apply {
            this.text = text
            textSize = 16f
            setPadding(20, 14, 20, 14)
            // Színek: user = kékes, assistant = szürke
            val fill = if (isUser) 0xFFE3F2FD.toInt() else 0xFFF1F3F4.toInt()
            val stroke = if (isUser) 0xFF90CAF9.toInt() else 0xFFE0E0E0.toInt()
            background = roundedBg(radius = 24f, fill = fill, stroke = stroke)
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isUser) Gravity.END else Gravity.START
            addView(bubble)
            setPadding(6, 8, 6, 8)
        }

        messages.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(6) }
        )
        scrollToBottom()
    }

    // Ha az utolsó asszisztens-buborék „Gondolkodom…”, cseréljük le a tényleges válaszra,
    // különben adjunk hozzá egy új asszisztens buborékot.
    private fun replaceLastThinkingIfAny(answer: String) {
        val count = messages.childCount
        if (count == 0) {
            addAssistantBubble(answer); return
        }
        val lastRow = messages.getChildAt(count - 1) as? LinearLayout
        val maybeBubble = lastRow?.getChildAt(0) as? TextView
        if (maybeBubble != null && maybeBubble.text.toString().trim() == "Gondolkodom…") {
            maybeBubble.text = answer
        } else {
            addAssistantBubble(answer)
        }
        scrollToBottom()
    }

    private fun roundedBg(radius: Float, fill: Int, stroke: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fill)
            setStroke(2, stroke)
        }

    private fun scrollToBottom() {
        scroll.post {
            scroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        llm?.close()
        llm = null
    }
}

