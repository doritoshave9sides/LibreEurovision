package com.eurovisionfoss.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.eurovisionfoss.app.RankedCountry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val WATERMARK = "Libre Eurovision\uD83E\uDD29"  // 🤩

object ImageExporter {

    // Portrait 9:16 — Top 12
    suspend fun exportTop12(context: Context, ranked: List<RankedCountry>): Intent =
        withContext(Dispatchers.IO) {
            val w = 1080
            val h = 1920
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawCard(canvas, w, h, ranked.take(12), "MY TOP 12", isSquare = false)
            saveAndShare(context, bitmap, "top12")
        }

    // Square 1:1 — Top 5
    suspend fun exportTop5(context: Context, ranked: List<RankedCountry>): Intent =
        withContext(Dispatchers.IO) {
            val size = 1080
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawCard(canvas, size, size, ranked.take(5), "MY TOP 5", isSquare = true)
            saveAndShare(context, bitmap, "top5")
        }

    private fun drawCard(
        canvas: Canvas,
        w: Int,
        h: Int,
        entries: List<RankedCountry>,
        title: String,
        isSquare: Boolean
    ) {
        val bg = Paint().apply { color = Color.parseColor("#121212"); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

        val padding = 48f
        val rowHeight = if (isSquare) (h - 280f) / entries.size else (h - 380f) / entries.size
        val startY = if (isSquare) 200f else 280f

        // Watermark at top
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#BB86FC")
            textSize = if (isSquare) 44f else 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(WATERMARK, w / 2f, if (isSquare) 100f else 110f, wmPaint)

        // Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = if (isSquare) 72f else 88f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, w / 2f, if (isSquare) 165f else 210f, titlePaint)

        // Rows
        entries.forEachIndexed { index, item ->
            val y = startY + index * rowHeight
            val pts = item.eurovisionPoints

            // Row background
            val rowBg = Paint().apply {
                color = Color.parseColor("#1E1E1E")
                style = Paint.Style.FILL
            }
            val rowRect = RectF(padding, y, w - padding, y + rowHeight - 8f)
            canvas.drawRoundRect(rowRect, 16f, 16f, rowBg)

            val textY = y + rowHeight * 0.62f

            // Rank
            val rankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#AAAAAA")
                textSize = if (isSquare) 36f else 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("${item.rank}", padding + 24f, textY, rankPaint)

            // Flag emoji
            val flagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = if (isSquare) 52f else 60f
            }
            canvas.drawText(item.country.flag, padding + 100f, textY, flagPaint)

            // Country name
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = if (isSquare) 40f else 46f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText(item.country.name, padding + 200f, textY, namePaint)

            // Points badge
            val badgeColor = pointsBadgeColorInt(pts)
            val badgePaint = Paint().apply {
                color = badgeColor
                style = Paint.Style.FILL
            }
            val bx = w - padding - 160f
            val badgeRect = RectF(bx, y + 12f, w - padding - 12f, y + rowHeight - 20f)
            canvas.drawRoundRect(badgeRect, 12f, 12f, badgePaint)

            val ptsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (pts >= 10) Color.BLACK else Color.WHITE
                textSize = if (isSquare) 36f else 40f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                if (pts > 0) "$pts pts" else "0 pts",
                bx + 74f,
                y + rowHeight * 0.62f,
                ptsPaint
            )
        }

        // Vienna 2026 footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#666666")
            textSize = if (isSquare) 32f else 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Vienna 2026", w / 2f, h - padding, footerPaint)
    }

    private fun pointsBadgeColorInt(pts: Int): Int = when (pts) {
        12   -> Color.parseColor("#FFD700")
        10   -> Color.parseColor("#B0BEC5")
        8    -> Color.parseColor("#CD7F32")
        7    -> Color.parseColor("#34C759")
        6    -> Color.parseColor("#30B0C7")
        5    -> Color.parseColor("#007AFF")
        4    -> Color.parseColor("#5856D6")
        3    -> Color.parseColor("#AF52DE")
        2    -> Color.parseColor("#FF9500")
        1    -> Color.parseColor("#FF3B30")
        else -> Color.parseColor("#636366")
    }

    private fun saveAndShare(context: Context, bitmap: Bitmap, suffix: String): Intent {
        val file = File(context.cacheDir, "libre_eurovision_$suffix.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "My Eurovision 2026 ranking — scored with Libre Eurovision")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
