#version 400 core

#include DRAParameters.fp

vec3 UNINITIALIZED = vec3(maxPixel, maxPixel, maxPixel);

void main()
{
    vec3 maxCumulativePixel = getMaxCumulativePixel();
    if (maxCumulativePixel == BLACK) {
       FragColor = vec4(getLastResult(), 1.0);
    } else {
       vec3 rgbHist = getCurrentPixelRgbHist();
       float fullIntensity = getCurrentPixelFullIntensity();

       vec3 outputPixel = vec3(UNINITIALIZED);
       int targetBucket = getCurrentPixelTargetBucket();

       if (targetBucket < myBuckets) {
          if (rgbHist.r > 0) {
            outputPixel.r = fullIntensity;
          }
          if (rgbHist.g > 0) {
            outputPixel.g = fullIntensity;
          }
          if (rgbHist.b > 0) {
            outputPixel.b = fullIntensity;
          }
       }
       vec3 smoothResult = getSmoothResult(outputPixel, UNINITIALIZED);
       FragColor = vec4(smoothResult, 1.0);
    }

 }
