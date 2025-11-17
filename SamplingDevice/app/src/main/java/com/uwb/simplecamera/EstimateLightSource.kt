package com.uwb.simplecamera

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.sqrt

fun Double.roundTo(digits: Int) = "%.${digits}f".format(this).toDouble()

fun estimateImageFromBitmap(bitmap: Bitmap): Map<String, Any?> {
    val RW_LIGHTSIZE = 330.0
    val UNITY_SCALE = 1.0
    val THRESHOLD = 230.0
    val FOCAL_LENGTH_IN_PIXELS = 2625.591533201383


    val mat = Mat()
    Utils.bitmapToMat(bitmap, mat)
    //Core.rotate(mat, mat, Core.ROTATE_90_COUNTERCLOCKWISE)
    val gray = Mat()
    Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
    Core.flip(gray, gray, +1) // Flip horizontally

    val height = gray.rows()
    val width = gray.cols()

    val thresholdBinary = Mat()
    Imgproc.threshold(gray, thresholdBinary, THRESHOLD, 255.0, Imgproc.THRESH_BINARY)

    val moments = Imgproc.moments(thresholdBinary)
    val centroidX = if (moments.m00 != 0.0) (moments.m10 / moments.m00) else 0.0
    val centroidY = if (moments.m00 != 0.0) (moments.m01 / moments.m00) else 0.0

    val centerX = width / 2.0
    val centerY = height / 2.0
    val du = abs(centerX - centroidX).toDouble()
    val dv = abs(centerY - centroidY).toDouble()

    val contours = mutableListOf<MatOfPoint>()
    Imgproc.findContours(thresholdBinary.clone(), contours, Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)

    var maxRadius = 0f
    var maxCenter = Point()

    for (contour in contours) {
        val contour2f = MatOfPoint2f(*contour.toArray())
        val center = Point()
        val radius = FloatArray(1)
        Imgproc.minEnclosingCircle(contour2f, center, radius)
        if (radius[0] > maxRadius) {
            maxRadius = radius[0]
            maxCenter = center
        }
    }

    val flippedMat = Mat()
    Core.flip(mat, flippedMat, +1)
    val meanIntensity = calculateMeanIntensity(flippedMat)
    val meanColor = Core.mean(flippedMat, thresholdBinary)

    if (maxRadius == 0f) {
        return mapOf(
            "ls_found" to 0,
            "x" to 0,
            "y" to 0,
            "z" to 0,
            "mean_intensity" to meanIntensity,
            "r" to meanColor.`val`[0],
            "g" to meanColor.`val`[1],
            "b" to meanColor.`val`[2],
            "a" to meanColor.`val`[3]
        )
    }

    val diameter = maxRadius * 2
    val mmPerPixel = RW_LIGHTSIZE / diameter
    val u = du * mmPerPixel
    val v = dv * mmPerPixel
    val distanceFromCameraToLS = FOCAL_LENGTH_IN_PIXELS * mmPerPixel
    val dcls = sqrt(u * u + v * v)
    val d = sqrt(distanceFromCameraToLS * distanceFromCameraToLS - dcls * dcls)

    val x = (u / 1000.0 * UNITY_SCALE).roundTo(6)
    val y = (d / 1000.0 * UNITY_SCALE).roundTo(6)
    var z = (v / 1000.0 * UNITY_SCALE).roundTo(6)
    if (centerY < centroidY) z *= -1


    return mapOf(
        "ls_found" to 1,
        "x" to x,
        "y" to y,
        "z" to z,
        "mean_intensity" to meanIntensity,
        "r" to meanColor.`val`[0],
        "g" to meanColor.`val`[1],
        "b" to meanColor.`val`[2],
        "a" to meanColor.`val`[3]
    )
}

fun calculateMeanIntensity(flippedMat: Mat): Double {
    val hist = Mat()
    val channels = MatOfInt(0)
    val histSize = MatOfInt(256)
    val ranges = MatOfFloat(0f, 256f)
    Imgproc.calcHist(listOf(flippedMat), channels, Mat(), hist, histSize, ranges)

    val totalPixels = flippedMat.rows() * flippedMat.cols()
    var meanIntensity = 0.0
    for (i in 0 until 256) {
        meanIntensity += i * hist.get(i, 0)[0]
    }
    meanIntensity /= totalPixels
    return meanIntensity
}