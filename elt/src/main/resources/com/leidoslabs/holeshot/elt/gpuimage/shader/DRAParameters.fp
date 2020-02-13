in VertexData {
   flat int vertexInstance;
} fs_in;

uniform sampler2D cumulativeHistogram;
uniform sampler2D lastResult;
uniform int histogramDownsampling;

uniform float ic_pmin;
uniform float ic_pmax;
uniform float maxAdjustPerFrame;
uniform ivec2 histDim;

out vec4 FragColor;

const vec3 BLACK = vec3(0,0,0);

#include HistogramAccess.vp

vec3 getCurrentResult(sampler2D resultSampler) {
   resetFBSize(fbDim, maxPixel, 1);
   ivec2 originRowCol = ivec2(0,0);
   vec2 originPixelPos = getTexturePositionForRowCol(originRowCol);
   return texture(resultSampler, originPixelPos).rgb;
}

vec3 getLastResult() {
   return getCurrentResult(lastResult);
}

vec3 getMaxCumulativePixel() {
   resetFBSize(histDim, maxPixel, buckets);
   float maxPixelIntensity = getUniformIntensity(maxPixel-1);
   ivec2 maxPixelRowCol = getRowColPositionForIntensity(maxPixelIntensity);
   vec2 maxPixelPos = getTexturePositionForRowCol(maxPixelRowCol);
   return texture(cumulativeHistogram, maxPixelPos).rgb;
}

vec2 getCurrentPixelTexturePosition() {
   resetFBSize(histDim, maxPixel, buckets);
   return getTexturePositionFromFullIntensity(fs_in.vertexInstance);
}
vec3 getCurrentPixelRgbHist() {
   resetFBSize(histDim, maxPixel, buckets);
   return texture(cumulativeHistogram, getCurrentPixelTexturePosition()).rgb;
}
ivec2 getCurrentPixelRowCol() {
   resetFBSize(histDim, maxPixel, buckets);
   return getRowColFromTexturePosition(getCurrentPixelTexturePosition());
}

float getCurrentPixelIntensity() {
   resetFBSize(histDim, maxPixel, buckets);
   return getIntensityFromRowCol(getCurrentPixelRowCol());
}

float getCurrentPixelFullIntensity() {
   resetFBSize(histDim, maxPixel, buckets);
   return getFullIntensity(getCurrentPixelIntensity());
}

int getCurrentPixelTargetBucket() {
   resetFBSize(histDim, maxPixel, buckets);
   return getTargetBucket(getCurrentPixelIntensity());
}

vec3 getSmoothResult(vec3 outputPixel, vec3 uninitializedValue) {
    vec3 smoothResult;
    vec3 lastResultVec = getLastResult();
    if (lastResultVec == uninitializedValue || maxAdjustPerFrame < 0) {
       smoothResult = outputPixel;
    } else {
       float maxMove = maxAdjustPerFrame * maxPixel;
       smoothResult = lastResultVec + maxMove * (smoothstep(lastResultVec - maxMove, lastResultVec + maxMove, outputPixel) * 2.0 - 1.0);
    }
    return smoothResult;
 }

vec3 getResultCount(sampler2D resultSampler) {
    vec3 resultVec = getCurrentResult(resultSampler);

    resetFBSize(histDim, maxPixel, buckets);
    vec3 resultVecNormalized = resultVec / maxPixel;

    ivec2 resultVecRowColRed = getRowColPositionForIntensity(resultVecNormalized.r);
    ivec2 resultVecRowColGreen = getRowColPositionForIntensity(resultVecNormalized.g);
    ivec2 resultVecRowColBlue = getRowColPositionForIntensity(resultVecNormalized.b);

    vec2 resultVecTexPosRed = getTexturePositionForRowCol(resultVecRowColRed);
    vec2 resultVecTexPosGreen = getTexturePositionForRowCol(resultVecRowColGreen);
    vec2 resultVecTexPosBlue = getTexturePositionForRowCol(resultVecRowColBlue);

    vec4 resultVecRgbHistRed = texture(cumulativeHistogram, resultVecTexPosRed);
    vec4 resultVecRgbHistGreen = texture(cumulativeHistogram, resultVecTexPosGreen);
    vec4 resultVecRgbHistBlue = texture(cumulativeHistogram, resultVecTexPosBlue);

    vec3 resultVecCount = vec3(resultVecRgbHistRed.r, resultVecRgbHistGreen.g, resultVecRgbHistBlue.b);

    return resultVecCount;
}
