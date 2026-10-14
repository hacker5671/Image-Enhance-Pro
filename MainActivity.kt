package com.imageenhancepro

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max

class MainActivity : Activity() {
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var upscaleSpinner: Spinner
    private lateinit var modeSpinner: Spinner
    private var original: Bitmap? = null
    private var result: Bitmap? = null
    private var selectedUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) loadImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(13, 16, 27)
        window.navigationBarColor = android.graphics.Color.rgb(13, 16, 27)
        buildUi()
    }

    private fun buildUi() {
        val bg = android.graphics.Color.rgb(13, 16, 27)
        val panel = android.graphics.Color.rgb(25, 30, 46)
        val white = android.graphics.Color.WHITE
        val accent = android.graphics.Color.rgb(133, 111, 255)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 22, 22, 18)
            setBackgroundColor(bg)
        }
        val title = TextView(this).apply {
            text = "Image Enhance Pro"
            textSize = 27f
            setTextColor(white)
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = "Upscale • Restore • Save"
            textSize = 14f
            setTextColor(0xFFB8BED2.toInt())
            setPadding(0, 4, 0, 18)
        }
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(panel)
            contentDescription = "Selected image preview"
        }
        val previewBox = FrameLayout(this).apply {
            addView(preview, FrameLayout.LayoutParams(-1, -1))
        }
        val pick = button("Choose image", accent, white) { pickImage.launch("image/*") }

        upscaleSpinner = spinner(listOf("2× upscale", "4× upscale", "8× upscale"))
        modeSpinner = spinner(listOf("Offline (on-device)", "Online API (configure endpoint)"))

        val run = button("Enhance image", 0xFF5B47E8.toInt(), white) { enhance() }
        val save = button("Save result", 0xFF27314A.toInt(), white) { saveResult() }

        status = TextView(this).apply {
            text = "Choose an image to get started."
            textSize = 13f
            setTextColor(0xFFD4D9E8.toInt())
            setPadding(0, 14, 0, 0)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(previewBox, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            bottomMargin = 14
        })
        root.addView(pick)
        root.addView(label("Output scale"))
        root.addView(upscaleSpinner)
        root.addView(label("Enhancement mode"))
        root.addView(modeSpinner)
        root.addView(run)
        root.addView(save)
        root.addView(status)
        setContentView(root)
    }

    private fun label(value: String) = TextView(this).apply {
        text = value
        textSize = 13f
        setTextColor(0xFFB8BED2.toInt())
        setPadding(2, 12, 0, 4)
    }

    private fun button(textValue: String, color: Int, textColor: Int, action: () -> Unit) =
        Button(this).apply {
            text = textValue
            setTextColor(textColor)
            setBackgroundColor(color)
            isAllCaps = false
            textSize = 15f
            setOnClickListener { action() }
        }

    private fun spinner(items: List<String>) = Spinner(this).apply {
        adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items)
    }

    private fun loadImage(uri: Uri) {
        selectedUri = uri
        try {
            contentResolver.openInputStream(uri).use { input ->
                original = BitmapFactory.decodeStream(input)
            }
            result = null
            preview.setImageBitmap(original)
            status.text = original?.let { "Loaded ${it.width} × ${it.height}. Choose a scale and enhance." }
                ?: "Could not decode this image."
        } catch (e: Exception) {
            status.text = "Could not open image: ${e.localizedMessage ?: "unknown error"}"
        }
    }

    private fun enhance() {
        val source = original
        if (source == null) {
            status.text = "Choose an image first."
            return
        }
        val factor = when (upscaleSpinner.selectedItemPosition) {
            0 -> 2
            1 -> 4
            else -> 8
        }
        if (modeSpinner.selectedItemPosition == 1) {
            status.text = "Online mode is ready for API integration. Configure your HTTPS endpoint in the source before use."
            return
        }

        // This starter uses high-quality bitmap resampling. It is NOT AI inference yet.
        // Add a compatible Real-ESRGAN ONNX model + inference pipeline to enable offline AI.
        status.text = "Upscaling locally…"
        Thread {
            try {
                val maxPixels = 24_000_000L
                var actualFactor = factor
                while (source.width.toLong() * source.height.toLong() * actualFactor * actualFactor > maxPixels && actualFactor > 2) {
                    actualFactor /= 2
                }
                val w = source.width * actualFactor
                val h = source.height * actualFactor
                val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(out)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
                canvas.drawBitmap(source, null, Rect(0, 0, w, h), paint)
                runOnUiThread {
                    result = out
                    preview.setImageBitmap(out)
                    status.text = "Created ${out.width} × ${out.height} using high-quality resampling. AI model integration is still required for true AI enhancement."
                }
            } catch (e: OutOfMemoryError) {
                runOnUiThread { status.text = "Not enough memory for this scale. Try 2× or a smaller image." }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Upscaling failed: ${e.localizedMessage ?: "unknown error"}" }
            }
        }.start()
    }

    private fun saveResult() {
        val bitmap = result
        if (bitmap == null) {
            status.text = "Enhance an image before saving."
            return
        }
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "ImageEnhancePro_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Image Enhance Pro")
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                status.text = "Could not create an image file."
                return
            }
            contentResolver.openOutputStream(uri).use { output ->
                if (output == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    throw IllegalStateException("Image encoding failed")
                }
            }
            status.text = "Saved to Pictures/Image Enhance Pro."
        } catch (e: Exception) {
            status.text = "Save failed: ${e.localizedMessage ?: "unknown error"}"
        }
    }
}
