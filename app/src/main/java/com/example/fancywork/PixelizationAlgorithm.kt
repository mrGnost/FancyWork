package com.example.fancywork

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import io.uuddlrlrba.closepixelate.Pixelate
import io.uuddlrlrba.closepixelate.PixelateLayer
import kotlinx.coroutines.*
import org.nield.kotlinstatistics.multiKMeansCluster
import java.util.concurrent.ConcurrentLinkedQueue
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
            newWidth: Int,
            newHeight: Int,
            colorsCount: Int,
            colors: List<Pair<String, Triple<Int, Int, Int>>>):
                Pair<Bitmap, Array<Array<String?>>> {
            val mainColors =
                kmeans(bitmap, colorsCount, 3.0, 10, colors)
            val pixelatedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
            val threadCodes = Array(newWidth) {
                arrayOfNulls<String>(newHeight)
            }
            runBlocking {
                val listOfReturnData = ConcurrentLinkedQueue<Job>()
                for (i in 0 until newWidth)
                    for (j in 0 until newHeight) {
                        listOfReturnData.add(
                            launch {
                                val pixel = pixelatedBitmap.getPixel(i, j)
                                val pixelColor = colorToTriple(pixel)
                                val mainColor = mainColors.minByOrNull { x -> findDistance(x.second, pixelColor) }!!
                                val mainRGB = (mainColor.second.first shl 16) +
                                        (mainColor.second.second shl 8) + mainColor.second.third
                                threadCodes[i][j] = mainColor.first
                                pixelatedBitmap.setPixel(i, j, mainRGB)
                            }
                        )
                    }
                listOfReturnData.joinAll()
            }
            return pixelatedBitmap to threadCodes
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
            k: Int,
            maxDiff: Double,
            maxIterations: Int,
            colors: List<Pair<String, Triple<Int, Int, Int>>>
        ): List<Pair<String, Triple<Int, Int, Int>>> {
            // Извлекаем все цвета пикселей из картинки.
            val imageIntColors = IntArray(image.width * image.height)
            image.getPixels(imageIntColors, 0, image.width, 0, 0, image.width, image.height)
            val imageColors = imageIntColors.map { colorToTriple(it) }
            // Инициализируем центроиды для алгоритма.
            val centers = initCenters(imageColors, k)
            // Заготавливаем списки точек для кластеров.
            val clusters = Array(k) { mutableListOf<Triple<Int, Int, Int>>() }
            var newCenterSum: Triple<Int, Int, Int>
            var newCenter: Triple<Int, Int, Int>

            var diff = 1000000.0
            var iteration = 0
            // Обновляем центроиды, пока они не перестанут смещаться, либо пока не пройдет слишком много итераций.
            while (diff > maxDiff && iteration < maxIterations) {
                clusters.forEach { it.clear() }
                // Для каждой точки выбираем ближайший центроид.
                imageColors.forEach { x -> clusters[centers.minByOrNull { y ->
                    findDistance(x, y.second)
                }!!.first].add(x) }
                diff = 0.0
                // Для каждого центроида меняем его положение на среднее из точек в его кластере.
                for (i in 0 until k) {
                    newCenterSum = clusters[i].fold(Triple(0, 0, 0), {x, y ->
                        Triple(x.first + y.first, x.second + y.second, x.third + y.third)
                    })
                    // Если в кластере этого центроида нет точек, рандомим ему новое поожение.
                    newCenter = if (clusters[i].size != 0) Triple(
                        newCenterSum.first / clusters[i].size,
                        newCenterSum.second / clusters[i].size,
                        newCenterSum.third / clusters[i].size
                    ) else Triple(
                        Random.nextInt(imageColors.minByOrNull { x -> x.first }!!.first, imageColors.maxByOrNull { x -> x.first }!!.first),
                        Random.nextInt(imageColors.minByOrNull { x -> x.second }!!.second, imageColors.maxByOrNull { x -> x.second }!!.second),
                        Random.nextInt(imageColors.minByOrNull { x -> x.third }!!.third, imageColors.maxByOrNull { x -> x.third }!!.third)
                    )
                    // Вычисляем максимальное смещение центроидов.
                    diff = max(diff, findDistance(centers[i].second, newCenter))
                    centers[i] = i to newCenter
                }
                iteration++
            }
            return centers.map { x -> colors.minByOrNull { y -> findDistance(x.second, y.second) }!! }.toList()
        }

        private fun initCenters(colors: List<Triple<Int, Int, Int>>, k: Int):
                MutableList<Pair<Int, Triple<Int, Int, Int>>> {
            val centers = mutableListOf<Pair<Int, Triple<Int, Int, Int>>>()
            for (i in 0 until k) {
                centers.add(i to Triple(
                    Random.nextInt(colors.minByOrNull { x -> x.first }!!.first, colors.maxByOrNull { x -> x.first }!!.first),
                    Random.nextInt(colors.minByOrNull { x -> x.second }!!.second, colors.maxByOrNull { x -> x.second }!!.second),
                    Random.nextInt(colors.minByOrNull { x -> x.third }!!.third, colors.maxByOrNull { x -> x.third }!!.third)
                ))
            }
            return centers
        }
    }
}