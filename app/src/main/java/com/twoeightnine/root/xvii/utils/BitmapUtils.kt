package com.twoeightnine.root.xvii.utils

import android.content.Context
import android.graphics.*
import com.twoeightnine.root.xvii.lg.Lg
import java.io.File
import kotlin.math.pow

fun getOrCreateNotificationBackground(context: Context, avatar: Bitmap): Bitmap {
    val hash = avatar.hash()
    val dir = File(context.cacheDir, DIR_NOTIFICATIONS)
    dir.mkdir()

    val file = File(dir, "$hash.png")
    return if (!file.exists()) {
        Lg.i("creating")
        val bitmap = createNotificationBackground(avatar)
        saveBmp(file.absolutePath, bitmap)
        bitmap
    } else {
        Lg.i("opening")
        BitmapFactory.decodeFile(file.absolutePath)
    }
}

fun createNotificationBackground(avatar: Bitmap): Bitmap {

    val backgroundWidth = 720
    val backgroundHeight = 180
    val background = Bitmap.createBitmap(backgroundWidth, backgroundHeight, Bitmap.Config.RGB_565)
    val canvas = Canvas(background)

    val avg = getAverageColor(avatar)
    val averageColor = avg.first
    canvas.drawColor(averageColor)

    val avatarWidth = avatar.width
    val avatarHeight = avatar.height
    val avatarRect = Rect(0, 0, avatarWidth, avatarHeight)

    val backgroundRect = Rect(0, 0, backgroundHeight, backgroundHeight)
    canvas.drawBitmap(avatar, avatarRect, backgroundRect, null)

    val paint = Paint().apply { style = Paint.Style.FILL }
    for (i in 0 until backgroundHeight) {
        for (j in 0 until backgroundHeight) {
            val bias = i.toFloat() / backgroundHeight

            val pixColor = background.getPixel(i, j)
            if (pixColor and 0xff000000.toInt() != 0xff000000.toInt()) continue

            val pixR = (pixColor shr 16) and 0xff
            val pixG = (pixColor shr 8) and 0xff
            val pixB = (pixColor shr 0) and 0xff

            val avgR = (averageColor shr 16) and 0xff
            val avgG = (averageColor shr 8) and 0xff
            val avgB = (averageColor shr 0) and 0xff

            val newR = (pixR * (1 - bias) + avgR * bias).toInt() and 0xff
            val newG = (pixG * (1 - bias) + avgG * bias).toInt() and 0xff
            val newB = (pixB * (1 - bias) + avgB * bias).toInt() and 0xff

            val newColor = (0xff shl 24) or (newR shl 16) or (newG shl 8) or newB
            paint.color = newColor
            canvas.drawPoint(i.toFloat(), j.toFloat(), paint)
        }
    }
    return background
}

fun getColors(avatar: Bitmap): Pair<Int, Int> {
    val avg = getAverageColor(avatar)
    val averageColor = avg.first
    val averageDark = avg.second
    val averageLight = avg.third

    val contrastWithLight = getContrastRatio(averageColor, averageLight)
    val contrastWithDark = getContrastRatio(averageColor, averageDark)

//    Lg.i("light = 0x${Integer.toHexString(averageLight)}; dark = 0x${Integer.toHexString(averageDark)}")
//    Lg.i("w/light = $contrastWithLight; w/dark = $contrastWithDark; w/white = $contrastWithWhite")

    val neededContrast = 4.0
    val textColor = when {
        contrastWithLight > contrastWithDark && contrastWithLight >= neededContrast -> averageLight
        contrastWithDark > contrastWithLight && contrastWithDark >= neededContrast -> averageDark

        contrastWithLight > contrastWithDark -> getColorOfContrast(
                averageColor, averageLight, 10, neededContrast, Color.WHITE
        )

        else -> getColorOfContrast(
                averageColor, averageDark, -10, neededContrast, Color.BLACK
        )
    }
    return Pair(averageColor, textColor)
}

fun getColorOfContrast(back: Int, colorFrom: Int, step: Int, contrast: Double, default: Int): Int {
    for (i in 1..10) {
        var newRed = colorFrom.red() + step * i
        if (newRed > 255) newRed = 255
        if (newRed < 0) newRed = 0

        var newGreen = colorFrom.green() + step * i
        if (newGreen > 255) newGreen = 255
        if (newGreen < 0) newGreen = 0

        var newBlue = colorFrom.blue() + step * i
        if (newBlue > 255) newBlue = 255
        if (newBlue < 0) newBlue = 0

        val newColor = createColor(newRed, newGreen, newBlue)
        val newContrast = getContrastRatio(back, newColor)
//        Lg.i("newColor = 0x${Integer.toHexString(newColor)}; w/this = $newContrast")

        if (newContrast >= contrast) {
            return newColor
        }
    }
    return default
}

/**
 * returns average color of the right part of [bitmap]
 */
fun getAverageColor(bitmap: Bitmap): Triple<Int, Int, Int> {

    val avgThreshold = bitmap.width * 3 / 4

    var avgColorR = 0
    var avgColorG = 0
    var avgColorB = 0
    var avgColorsCount = 0

    var darkAvgR = 0
    var darkAvgG = 0
    var darkAvgB = 0
    var darkColorsCount = 0

    var lightAvgR = 0
    var lightAvgG = 0
    var lightAvgB = 0
    var lightColorsCount = 0

    for (i in 0 until bitmap.width) {
        for (j in 0 until bitmap.height) {
            val pixel = bitmap.getPixel(i, j)
            if (pixel.hasAlpha()) continue

            val r = pixel.red()
            val g = pixel.green()
            val b = pixel.blue()

            if (i >= avgThreshold) {
                avgColorR += r
                avgColorG += g
                avgColorB += b
                avgColorsCount++
            }

            if (r + g + b < 370) {
                darkAvgR += r
                darkAvgG += g
                darkAvgB += b
                darkColorsCount++
            } else {
                lightAvgR += r
                lightAvgG += g
                lightAvgB += b
                lightColorsCount++
            }

        }
    }

    if (avgColorsCount != 0) {
        avgColorR /= avgColorsCount
        avgColorG /= avgColorsCount
        avgColorB /= avgColorsCount
    }

    if (darkColorsCount != 0) {
        darkAvgR /= darkColorsCount
        darkAvgG /= darkColorsCount
        darkAvgB /= darkColorsCount
    } // else dark will be 0,0,0 i.e. black

    if (lightColorsCount != 0) {
        lightAvgR /= lightColorsCount
        lightAvgG /= lightColorsCount
        lightAvgB /= lightColorsCount
    } else {
        lightAvgR = 255
        lightAvgG = 255
        lightAvgB = 255
    }
    return Triple(
            createColor(avgColorR, avgColorG, avgColorB),
            createColor(darkAvgR, darkAvgG, darkAvgB),
            createColor(lightAvgR, lightAvgG, lightAvgB)
    )
}

fun Bitmap.hash(): Int {
    var hash = 31
    (0 until width).step(5).forEach { x ->
        (0 until height).step(5).forEach { y ->
            hash = (hash * 31 + getPixel(x, y)).rem(Int.MAX_VALUE) // it is prime
        }
    }
    return hash
}

private fun Int.hasAlpha() = this and 0xff000000.toInt() != 0xff000000.toInt()

private fun Int.red() = (this shr 16) and 0xff

private fun Int.green() = (this shr 8) and 0xff

private fun Int.blue() = this and 0xff

private fun createColor(r: Int, g: Int, b: Int) =
        0xff000000.toInt() or ((r and 0xff) shl 16) or ((g and 0xff) shl 8) or (b and 0xff)

private fun Int.relativeLuminance(): Float {
    val rs = red().toFloat() / 255
    val gs = green().toFloat() / 255
    val bs = blue().toFloat() / 255

    val r = if (rs <= 0.03928) {
        rs / 12.92
    } else {
        ((rs + 0.055) / 1.055).pow(2.4)
    }
    val g = if (rs <= 0.03928) {
        gs / 12.92
    } else {
        ((gs + 0.055) / 1.055).pow(2.4)
    }
    val b = if (rs <= 0.03928) {
        bs / 12.92
    } else {
        ((bs + 0.055) / 1.055).pow(2.4)
    }
    return (0.2126 * r + 0.7152 * g + 0.0722 * b).toFloat()
}

private fun getContrastRatio(c1: Int, c2: Int): Double {
    val rl1 = c1.relativeLuminance()
    val rl2 = c2.relativeLuminance()

    return if (rl1 > rl2) {
        (rl1 + 0.05) / (rl2 + 0.05)
    } else {
        (rl2 + 0.05) / (rl1 + 0.05)
    }
}
//fun getBlurredAvatar(avatar: Bitmap): Bitmap {
//    val blurred = Bitmap.createBitmap(avatar.width, avatar.height)
//}