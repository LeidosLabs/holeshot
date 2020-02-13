#version 400 core

#include DRAParameters.fp
uniform sampler2D eLastTexture;

vec3 UNINITIALIZED = vec3(0.0, 0.0, 0.0);

void main()
{
    vec3 maxCumulativePixel = getMaxCumulativePixel();
    if (maxCumulativePixel == BLACK) {
       FragColor = vec4(getLastResult(), 1.0);
    } else {
       vec3 rgbHist = getCurrentPixelRgbHist();
       float fullIntensity = getCurrentPixelFullIntensity();
       vec3 eLastCount = getResultCount(eLastTexture);

       vec3 outputPixel = vec3(UNINITIALIZED);
       int targetBucket = getCurrentPixelTargetBucket();

       if (targetBucket < myBuckets) {
          if ((rgbHist.r / eLastCount.r) < ic_pmax) {
             outputPixel.r = fullIntensity;
          }
          if ((rgbHist.g / eLastCount.g) < ic_pmax) {
             outputPixel.g = fullIntensity;
          }
          if ((rgbHist.b / eLastCount.b) < ic_pmax) {
             outputPixel.b = fullIntensity;
          }
       }

       vec3 smoothResult = getSmoothResult(outputPixel, UNINITIALIZED);
       FragColor = vec4(smoothResult, 1.0);
    }

 }
