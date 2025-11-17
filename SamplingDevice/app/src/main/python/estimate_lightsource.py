import cv2 as cv
import numpy as np
from matplotlib import pyplot as plt
import math

RW_LIGHTSIZE = 330
UNITY_SCALE = 1
THRESHOLD = 230
FOCAL_LENGTH_IN_PIXELS = 2625.591533201383

def estimate(image_bytes):
    np_arr = np.frombuffer(image_bytes, dtype=np.uint8)
    img_color = cv.imdecode(np_arr, cv.IMREAD_UNCHANGED)
    img = cv.cvtColor(img_color, cv.COLOR_BGR2GRAY)
    
    if img is None:
        raise ValueError("Failed to decode image")
        
    img = cv.flip(img, 1)
    height, width = img.shape

    ## threshold image
    return_value, threshold_binary = cv.threshold(img, THRESHOLD, 255, 0)
    moment = cv.moments(threshold_binary)
    
    ## find brightest moment
    if moment['m00'] != 0:
        centroidX = int(moment['m10'] / moment['m00'])
        centroidY = int(moment['m01'] / moment['m00'])
    else:
        centroidX, centroidY = 0, 0

    centerX = width // 2
    centerY = height // 2
    
    ## Find radius of brightest moment
    contours, hierarchy = cv.findContours(threshold_binary, cv.RETR_TREE, cv.CHAIN_APPROX_SIMPLE)
    max_radius = 0
    max_coord = None
    for contour in contours:
        coord, radius = cv.minEnclosingCircle(contour)
        if radius > max_radius:
            max_radius = radius
            max_coord = coord

    ## find distance from image center to centroid center
    du = abs(centerX - centroidX)
    dv = abs(centerY - centroidY)


    ## Calculate mean intensity value of an image's histogram
    img_color = cv.flip(img_color, 1)
    if img_color is None:
        raise FileNotFoundError(f'image not found')
    hist = cv.calcHist([img_color], [0], None, [256], [0, 256])
    # Calculate the mean intensity value
    total_pixels = img_color.shape[0] * img_color.shape[1]
    mean_intensity = np.sum(np.array([i for i in range(256)]) * hist.flatten()) / total_pixels

    ## Calculate mean RGB of mask
    mean_light_color = cv.mean(img_color, mask=threshold_binary)


    ## find distance from camera to light source center
    if max_radius == 0:
        return {
            "ls_found": 0,
            "x" : 0,
            "y" : 0,
            "z" : 0,
            "mean_intensity" : mean_intensity,
            "r" : mean_light_color[0],
            "g" : mean_light_color[1],
            "b" : mean_light_color[2],
            "a" : mean_light_color[3]
        }
        #raise Exception("No contours or any light source representations found in image")


    diameter = max_radius * 2
    mm_per_pixels = RW_LIGHTSIZE / diameter
    u = du * mm_per_pixels
    v = dv * mm_per_pixels
    #focal_length_in_pixels = width / 2 / math.tan(120 / 2)
    distance_from_camera_to_ls = FOCAL_LENGTH_IN_PIXELS * mm_per_pixels
    dcls = math.sqrt(u ** 2 + v ** 2)
    d = math.sqrt(distance_from_camera_to_ls ** 2 - dcls ** 2)
    y = round(d / 1000 * UNITY_SCALE, 6)

    ## convert pixels to mm to Unity units
    x = round(u / 1000 * UNITY_SCALE, 6)
    z = round(v / 1000 * UNITY_SCALE, 6)
    z = z * -1 if centerY < centroidY else z


    return {
        "x" : x,
        "y" : y,
        "z" : z,
        "mean_intensity" : mean_intensity,
        "r" : mean_light_color[0],
        "g" : mean_light_color[1],
        "b" : mean_light_color[2],
        "a" : mean_light_color[3]
    }
    