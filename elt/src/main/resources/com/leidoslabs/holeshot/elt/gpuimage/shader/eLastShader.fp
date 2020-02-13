#version 400 core

#include DRAParameters.fp

vec3 UNINITIALIZED = vec3(0.0, 0.0, 0.0);

void main()
{

    vec3 maxCumulativePixel = getMaxCumulativePixel();
    if (maxCumulativePixel == BLACK) {
       FragColor = vec4(getLastResult(), 1.0);
    } else {
       vec2 texturePosition = getTexturePositionFromFullIntensity(fs_in.vertexInstance);

       vec3 rgbHist = getCurrentPixelRgbHist();
       float fullIntensity = getCurrentPixelFullIntensity();
       vec3 outputPixel = vec3(UNINITIALIZED);
       int targetBucket = getCurrentPixelTargetBucket();

       vec3 maxCumulativePixel = getMaxCumulativePixel();

       if (targetBucket < myBuckets) {

          if (rgbHist.r < maxCumulativePixel.r) {
             outputPixel.r = fullIntensity;
          }
          if (rgbHist.g < maxCumulativePixel.g) {
             outputPixel.g = fullIntensity;
          }
          if (rgbHist.b < maxCumulativePixel.b) {
             outputPixel.b = fullIntensity;
          }
       }
       vec3 smoothResult = getSmoothResult(outputPixel, UNINITIALIZED);
       FragColor = vec4(smoothResult, 1.0);
    }
 }
