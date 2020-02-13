#version 400 core

in VertexData {
   vec2 uniformCoord;
} fs_in;

uniform sampler2D rawImage;
uniform sampler2D eFirstTexture;
uniform sampler2D eMinTexture;
uniform sampler2D eMaxTexture;
uniform sampler2D eLastTexture;
uniform sampler2D cumulativeHistogram;

uniform float visibleImageArea;
uniform int histogramDownsampling;
uniform ivec2 histFBDim;

uniform float ic_a;
uniform float ic_b;

out vec4 FragColor;

#include HistogramAccess.vp


vec3 roundTo(in vec3 source, in float numDigits) {
   float factor = pow(10.0,numDigits);
   return round(source/factor) * factor;
}

void main()
{
    resetFBSize(fbDim, maxPixel, fbDim.x * fbDim.y);
    vec4 rawPixel = texture(rawImage, getTexturePositionFromUniform(fs_in.uniformCoord));
    vec4 rawPixelFull = rawPixel * maxPixel;

    resetFBSize(histFBDim, maxPixel, buckets);
    ivec2 originRowCol = ivec2(0,0);
    vec2 originPixelPos = getTexturePositionForRowCol(originRowCol);

    float sigDigits = 0.0;
    vec3 eFirst = roundTo(texture(eFirstTexture, originPixelPos).rgb, sigDigits);
    vec3 eMin = roundTo(texture(eMinTexture, originPixelPos).rgb, sigDigits);
    vec3 eMax = roundTo(texture(eMaxTexture, originPixelPos).rgb, sigDigits);
    vec3 eLast = roundTo(texture(eLastTexture, originPixelPos).rgb, sigDigits);

    ivec2 rawRowColRed = getRowColPositionForIntensity(rawPixel.r);
    ivec2 rawRowColGreen = getRowColPositionForIntensity(rawPixel.g);
    ivec2 rawRowColBlue = getRowColPositionForIntensity(rawPixel.b);

    vec2 rawTexPosRed = getTexturePositionForRowCol(rawRowColRed);
    vec2 rawTexPosGreen = getTexturePositionForRowCol(rawRowColGreen);
    vec2 rawTexPosBlue = getTexturePositionForRowCol(rawRowColBlue);

    vec4 rawRgbHistRed = texture(cumulativeHistogram, rawTexPosRed);
    vec4 rawRgbHistGreen = texture(cumulativeHistogram, rawTexPosGreen);
    vec4 rawRgbHistBlue = texture(cumulativeHistogram, rawTexPosBlue);

    vec3 rawCount = vec3(rawRgbHistRed.r, rawRgbHistGreen.g, rawRgbHistBlue.b);

    vec3 eMinAdjusted = max(eFirst, eMin - ic_a*(eMax-eMin));
    vec3 eMaxAdjusted = min(eLast, eMax + ic_b*(eMax-eMin));
    vec3 yMax=vec3(maxPixel, maxPixel, maxPixel);
    vec3 b = yMax/(eMaxAdjusted - eMinAdjusted);
    vec3 h = eMin;
    vec3 y = b * (rawPixelFull.rgb - h);

    FragColor = vec4(y/maxPixel, rawPixel.a);
}