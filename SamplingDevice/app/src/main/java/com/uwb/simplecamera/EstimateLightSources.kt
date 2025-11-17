package com.uwb.simplecamera

import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.sqrt
import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log
import java.lang.Math.toRadians
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.tan

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

fun estimateMultipleLightSources(bitmap: Bitmap, realWorldLights: ArrayList<Int>): JSONObject {
    var RW_SIZE = listOf(330.02, 68.7) // 90mm, 68.7 mm (webcam), 5.0 keychainlight
    val CM_TO_MM = 10
    if (!realWorldLights.isNullOrEmpty())
    {
        Log.d("RW_SIZE", realWorldLights.toString())
        RW_SIZE = ArrayList<Double>()
        for (n in realWorldLights)
        {
            RW_SIZE.add(n.toDouble() * CM_TO_MM)
        }
    }
    Log.d("RW_SIZE", RW_SIZE.toString())
    val U_UNITS = 1.0 // scale factor on AR Vuforia Camera 20
    val THRESHOLD = 230.0
    val DIAGONAL_FOV = doubleArrayOf(85.0, 81.6, 80.0)

    val imgColor = Mat()
    Utils.bitmapToMat(bitmap, imgColor)
    Core.flip(imgColor, imgColor, 1)

    val imgGray = Mat()
    Imgproc.cvtColor(imgColor, imgGray, Imgproc.COLOR_BGR2GRAY)
    //Core.flip(imgGray, imgGray, 1)

    val height = imgGray.rows()
    val width = imgGray.cols()
    val centerU = width / 2.0
    val centerV = height / 2.0
    val MIN_RADIUS = if (width >= height) height * 0.01 else width * 0.01
    val MAX_RADIUS = if (width >= height) height * 0.7 else width * 0.7

    val thresholdBinary = Mat()
    Imgproc.threshold(imgGray, thresholdBinary, THRESHOLD, 255.0, Imgproc.THRESH_BINARY)

    val contours = mutableListOf<MatOfPoint>()
    Imgproc.findContours(thresholdBinary, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE)

    val minCircleMaxSize = mutableListOf<Quadruple<Double, Double, Point, Float>>()
    val masks = mutableMapOf<Float, Mat>()

    var numViableContours = 0
    for (contour in contours) {
        val moments = Imgproc.moments(contour)
        val centroidU: Double
        val centroidV: Double
        if (moments.m00 != 0.0) {
            centroidU = (moments.m10 / moments.m00)
            centroidV = (moments.m01 / moments.m00)
            val contour2f = MatOfPoint2f(*contour.toArray())
            val center = Point()
            val radius = FloatArray(1)
            Imgproc.minEnclosingCircle(contour2f, center, radius)
            if (radius[0] >= MIN_RADIUS && radius[0] < MAX_RADIUS && isCircularByEllipse(contour)) {
                // Mask
                val mask = Mat.zeros(imgGray.size(), CvType.CV_8UC1)
                Imgproc.drawContours(mask, listOf(contour), -1, Scalar(255.0), -1)
                val feature = Mat()
                Core.bitwise_and(imgGray, imgGray, feature, mask)
                masks[radius[0]] = feature

                minCircleMaxSize.add(Quadruple(centroidU, centroidV, center, radius[0]))
                numViableContours++
            }
        }
    }

    val sortedCircles = minCircleMaxSize.sortedByDescending { it.fourth }
    val pixelDistances = mutableListOf<Triple<Double, Double, Float>>()
    for ((centroidU, centroidV, _, radius) in sortedCircles) {
        val du = abs(centerU - centroidU)
        val dv = abs(centerV - centroidV)
        pixelDistances.add(Triple(du, dv, radius))
        //Log.d("PIXEL DISTANCE", "$centroidU, $centroidV, $radius")
    }

    val result = JSONObject()
    val lights = JSONArray()
    /*
    val color1 = Core.mean(imgColor).`val`
    val r1 = color1[0]
    val g1 = color1[1]
    val b1 = color1[2]
    val luminance2 = 0.299 * r1 + 0.587 * g1 + 0.114 * b1
    Log.d("NOMASKLUMINOSITY", "mean_intensity_no_mask $luminance2")*/
    for ((i, triple) in pixelDistances.withIndex()) {
        if (lights.length() == 8) {
            break
        }
        val (u, v, radius) = triple
        val diameter = radius * 2
        var rw_source = if (i < RW_SIZE.count()) i else RW_SIZE.count() - 1
        val mmPerPixels = RW_SIZE.getOrNull(rw_source)?.div(diameter) ?: continue
        var x = u * mmPerPixels
        var z = v * mmPerPixels
        val imageAspect = sqrt(width.toDouble() * width.toDouble() + height.toDouble() * height.toDouble())
        var y = (imageAspect/ 2) / tan(toRadians(DIAGONAL_FOV[2] / 2 )) * mmPerPixels

        x = (x / 1000.0).roundTo(3)
        y = (y / 1000.0).roundTo(3)
        z = (z / 1000.0).roundTo(3)
        if (centerU > sortedCircles[i].first) x *= -1
        if (centerV < sortedCircles[i].second) z *= -1

        val mask = masks[radius] ?: continue
        //val average = Core.mean(imgGray, mask)
        val color = Core.mean(imgColor, mask).`val`
        val r = color[0]
        val g = color[1]
        val b = color[2]
        val luminance = 0.3 * r + 0.6 * g + 0.1 * b // perceived brightness (luminance) using Rec. 601

        val light = JSONObject()
        light.put("x", x)
        light.put("y", y)
        light.put("z", z)
        light.put("radius", radius.roundToInt())
        light.put("mean_intensity", luminance.roundTo(1))
        light.put("r", r.roundTo(1))
        light.put("g", g.roundTo(1))
        light.put("b", b.roundTo(1))
        light.put("a", color[3].roundTo(1))

        lights.put(light)
    }

    result.put("lights", lights)
    //result.put("numLights", numViableContours)
    return result
}

fun estimateMultipleDarkSources(bitmap: Bitmap): JSONObject {
    val RW_SIZE = listOf(50.0) // mm
    val U_UNITS = 1.0 // scale factor on AR Vuforia Camera
    val THRESHOLD = 50.0
    val MIN_RADIUS = 15.0
    val DIAGONAL_FOV = doubleArrayOf(70.0, 85.0, 81.6)
    val FOCAL_LENGTH_IN_PIXELS = 2625.591533201383

    val imgColor = Mat()
    Utils.bitmapToMat(bitmap, imgColor)
    Core.flip(imgColor, imgColor, 1)

    val imgGray = Mat()
    Imgproc.cvtColor(imgColor, imgGray, Imgproc.COLOR_BGR2GRAY)
    Core.flip(imgGray, imgGray, 1)

    val height = imgGray.rows()
    val width = imgGray.cols()
    val centerU = width / 2.0
    val centerV = height / 2.0

    val thresholdBinary = Mat()
    Imgproc.threshold(imgGray, thresholdBinary, THRESHOLD, 255.0, Imgproc.THRESH_BINARY_INV)

    val contours = mutableListOf<MatOfPoint>()
    Imgproc.findContours(thresholdBinary, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE)

    val minCircleMaxSize = mutableListOf<Quadruple<Double, Double, Point, Float>>()
    val masks = mutableMapOf<Float, Mat>()

    var numViableContours = 0
    for (contour in contours) {
        val moments = Imgproc.moments(contour)
        val centroidU: Double
        val centroidV: Double
        if (moments.m00 != 0.0) {
            centroidU = (moments.m10 / moments.m00)
            centroidV = (moments.m01 / moments.m00)
            val contour2f = MatOfPoint2f(*contour.toArray())
            val center = Point()
            val radius = FloatArray(1)
            Imgproc.minEnclosingCircle(contour2f, center, radius)
            if (radius[0] > MIN_RADIUS) {
                // Mask
                Log.d("OPENCV_CALCULATION", "$centroidU, $centroidV, $center, ${radius[0]}")
                val mask = Mat.zeros(imgGray.size(), CvType.CV_8UC1)
                Imgproc.drawContours(mask, listOf(contour), -1, Scalar(255.0), -1)
                val feature = Mat()
                Core.bitwise_and(imgGray, imgGray, feature, mask)
                masks[radius[0]] = feature

                minCircleMaxSize.add(Quadruple(centroidU, centroidV, center, radius[0]))
                numViableContours++
                //Log.d("WHY", "$centroidU, $centroidV, $center, ${radius[0]}")
            }
        }
    }

    val sortedCircles = minCircleMaxSize.sortedByDescending { it.fourth }
    val pixelDistances = mutableListOf<Triple<Double, Double, Float>>()
    for ((centroidU, centroidV, _, radius) in sortedCircles) {
        val du = abs(centerU - centroidU)
        val dv = abs(centerV - centroidV)
        pixelDistances.add(Triple(du, dv, radius))
    }

    val result = JSONObject()
    val lights = JSONArray()
    for ((i, triple) in pixelDistances.withIndex()) {
        if (i == 8) {
            break
        }
        val (du, dv, radius) = triple
        val diameter = radius * 2
        val mmPerPixels = RW_SIZE.getOrNull(0)?.div(diameter) ?: continue
        val u = du * mmPerPixels
        val v = dv * mmPerPixels
        //val distanceFromCameraToLS = FOCAL_LENGTH_IN_PIXELS * mmPerPixels
        val dcls = sqrt(u * u + v * v)
        //val distanceFromCameraToLS = dcls / tan(toRadians(DIAGONAL_FOV[1] / 2.0))
        //Log.d("MYCALC", distanceFromCameraToLS.toString())
        //val d = sqrt(distanceFromCameraToLS * distanceFromCameraToLS - dcls * dcls)
        val imageAspect = sqrt(width.toDouble() * width.toDouble() + height.toDouble() * height.toDouble())
        val d = (imageAspect/ 2) / tan(toRadians(DIAGONAL_FOV[0] / 2 )) * mmPerPixels


        var x = (u / 1000.0 * U_UNITS).roundTo(3)
        val y = (d / 1000.0 * U_UNITS).roundTo(3)
        var z = (v / 1000.0 * U_UNITS).roundTo(3)
        if (centerU < sortedCircles[i].first) x *= -1
        if (centerV < sortedCircles[i].second) z *= -1

        val mask = masks[radius] ?: continue
        //val average = Core.mean(imgGray, mask)
        val color = Core.mean(imgColor, mask).`val`
        val r = color[0]
        val g = color[1]
        val b = color[2]
        val luminance = 0.299 * r + 0.587 * g + 0.114 * b // perceived brightness (luminance) using Rec. 601

        val light = JSONObject()
        light.put("x", x)
        light.put("y", y)
        light.put("z", z)
        light.put("radius", radius.roundToInt())
        light.put("mean_intensity", luminance.roundTo(1))
        light.put("r", r.roundTo(1))
        light.put("g", g.roundTo(1))
        light.put("b", b.roundTo(1))
        light.put("a", color[3].roundTo(1))

        lights.put(light)
    }

    result.put("lights", lights)
    //result.put("numLights", numViableContours)
    return result
}

fun isCircularByEllipse(contour: MatOfPoint, ratioMin: Double = 0.8, ratioMax: Double = 1.2): Boolean {
    // fitEllipse needs at least 5 points
    if (contour.total() < 5) return false

    val contour2f = MatOfPoint2f(*contour.toArray())
    val ellipse = Imgproc.fitEllipse(contour2f)

    val majorAxis = maxOf(ellipse.size.width, ellipse.size.height)
    val minorAxis = minOf(ellipse.size.width, ellipse.size.height)

    if (majorAxis == 0.0) return false

    val ratio = minorAxis / majorAxis  // always <= 1

    return ratio >= ratioMin && ratio <= ratioMax
}