#version 400 core

#include DRAParameters.fp
uniform sampler2D eFirstTexture;
vec3 UNINITIALIZED = vec3(maxPixel, maxPixel, maxPixel);

void main()
{
    vec3 maxCumulativePixel = getMaxCumulativePixel();
    if (maxCumulativePixel == BLACK) {
       FragColor = vec4(getLastResult(), 1.0);
    } else {
       vec3 rgbHist = getCurrentPixelRgbHist();
       float fullIntensity = getCurrentPixelFullIntensity();
       vec3 eFirstCount = getResultCount(eFirstTexture);

       vec3 outputPixel = vec3(UNINITIALIZED);
       int targetBucket = getCurrentPixelTargetBucket();

       if (targetBucket < myBuckets) {
          if ((rgbHist.r / eFirstCount.r) >  (1.0 + ic_pmin)) {
             outputPixel.r = fullIntensity;
          }
          if ((rgbHist.g / eFirstCount.g) > (1.0 + ic_pmin)) {
             outputPixel.g = fullIntensity;
          }
          if ((rgbHist.b / eFirstCount.b) > (1.0 + ic_pmin)) {
             outputPixel.b = fullIntensity;
          }
       }

       vec3 smoothResult = getSmoothResult(outputPixel, UNINITIALIZED);
       FragColor = vec4(smoothResult, 1.0);
    }

 }
