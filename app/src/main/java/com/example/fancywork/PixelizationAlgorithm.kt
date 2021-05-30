package com.example.fancywork

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import io.uuddlrlrba.closepixelate.Pixelate
import io.uuddlrlrba.closepixelate.PixelateLayer
import org.nield.kotlinstatistics.multiKMeansCluster
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

class PixelizationAlgorithm {

    companion object {
        // Method for getting thread colors from resources.
        fun getThreadColors(resources: Resources): List<Pair<String, Triple<Int, Int, Int>>> {
            val stream = resources.openRawResource(R.raw.colors)
            val colors = stream
                .bufferedReader()
                .readLines()
                .drop(1)
                .map { x -> x.split(",") }
                .map { x -> x[0] to Triple(x[1].toInt(), x[2].toInt(), x[3].toInt()) }
            stream.close()
            return colors
        }

        // This method makes a pixelated bitmap from image bitmap and provides an array of thread codes.
        fun getPixelsFromImage(
            bitmap: Bitmap,
            pixelSize: Int,
            colorsCount: Int,
            colors: List<Pair<String, Triple<Int, Int, Int>>>):
                Pair<Bitmap, Array<Array<String?>>> {
            val mainColors =
                kmeans(bitmap, bitmap.width, bitmap.height, colorsCount, 3.0, 100, colors)
            val pixelatedBitmap = Pixelate.fromBitmap(
                bitmap,
                PixelateLayer.Builder(PixelateLayer.Shape.Square)
                    .setSize(pixelSize.toFloat())
                    .setEnableDominantColors(true)
                    .build()
            )
            val pixelatedWidth = ceil(bitmap.width.toDouble() / pixelSize).toInt()
            val pixelatedHeight = ceil(bitmap.height.toDouble() / pixelSize).toInt()
            val threadCodes = Array(pixelatedWidth) {
                arrayOfNulls<String>(pixelatedHeight)
            }
            val bitmapColors = IntArray(pixelatedWidth * pixelatedHeight)
            for (i in 0 until bitmap.width step pixelSize)
                for (j in 0 until bitmap.height step pixelSize) {
                    val pixel = pixelatedBitmap.getPixel(i, j)
                    val pixelColor = colorToTriple(pixel)
                    val mainColor = colors.minByOrNull { x -> findDistance(x.second, pixelColor) }!!
                    val mainRGB = (mainColor.second.first shl 16) +
                            (mainColor.second.second shl 8) + mainColor.second.third
                    threadCodes[i / pixelSize][j / pixelSize] = mainColor.first
                    bitmapColors[j / pixelSize * pixelatedWidth + i / pixelSize] = mainRGB
                }
            val resultBitmap =
                Bitmap.createBitmap(bitmapColors, pixelatedWidth, pixelatedHeight, Bitmap.Config.RGB_565)
            return resultBitmap to threadCodes
        }

        private fun colorToTriple(color: Int): Triple<Int, Int, Int> {
            return Triple(
                (color shr 16) and 0xff,
                (color shr 8) and 0xff,
                color and 0xff
            )
        }

        private fun findDistance(x: Triple<Int, Int, Int>, colorsAv: Triple<Int, Int, Int>): Double {
            return (((1 + max(x.first, colorsAv.first)).toDouble() / (1 + min(
                x.first,
                colorsAv.first
            ))).pow(2)
                    + ((1 + max(x.second, colorsAv.second)).toDouble() / (1 + min(
                x.second,
                colorsAv.second
            ))).pow(2)
                    + ((1 + max(x.third, colorsAv.third)).toDouble() / (1 + min(
                x.third,
                colorsAv.third
            ))).pow(2))
        }

        private fun kmeans(
            image: Bitmap,
            width: Int,
            height: Int,
            k: Int,
            minDiff: Double,
            maxIterations: Int,
            colors: List<Pair<String, Triple<Int, Int, Int>>>
        ): List<Pair<String, Triple<Int, Int, Int>>> {
            val imageColors = (1 until width)
                .flatMap { x -> (1 until height).map { y -> colorToTriple(image.getPixel(x, y)) } }
            val centers = mutableListOf<Pair<Int, Triple<Int, Int, Int>>>()
            for (i in 0 until k) {
                centers.add(i to Triple(
                    Random.nextInt(imageColors.minByOrNull { x -> x.first }!!.first, imageColors.maxByOrNull { x -> x.first }!!.first),
                    Random.nextInt(imageColors.minByOrNull { x -> x.second }!!.second, imageColors.maxByOrNull { x -> x.second }!!.second),
                    Random.nextInt(imageColors.minByOrNull { x -> x.third }!!.third, imageColors.maxByOrNull { x -> x.third }!!.third)
                ))
            }

            var diff = 1000000.0
            var iter = 0
            while (diff > minDiff && iter < maxIterations) {
                val clusters = Array(k) { mutableListOf<Triple<Int, Int, Int>>() }
                imageColors.forEach { x -> clusters[centers.minByOrNull { y ->
                    findDistance(x, y.second)
                }!!.first].add(x) }
                diff = 0.0
                for (i in 0 until k) {
                    val newCenterSum = clusters[i].fold(Triple(0, 0, 0), {x, y ->
                        Triple(x.first + y.first, x.second + y.second, x.third + y.third)
                    })
                    val newCenter = if (clusters[i].size != 0) Triple(
                        newCenterSum.first / clusters[i].size,
                        newCenterSum.second / clusters[i].size,
                        newCenterSum.third / clusters[i].size
                    ) else Triple(
                        Random.nextInt(imageColors.minByOrNull { x -> x.first }!!.first, imageColors.maxByOrNull { x -> x.first }!!.first),
                        Random.nextInt(imageColors.minByOrNull { x -> x.second }!!.second, imageColors.maxByOrNull { x -> x.second }!!.second),
                        Random.nextInt(imageColors.minByOrNull { x -> x.third }!!.third, imageColors.maxByOrNull { x -> x.third }!!.third)
                    )
                    diff = max(diff, findDistance(centers[i].second, newCenter))
                    centers[i] = i to newCenter
                }
                iter++
            }
            return centers.map { x -> colors.minByOrNull { y -> findDistance(x.second, y.second) }!! }.toList()
        }
    }
}