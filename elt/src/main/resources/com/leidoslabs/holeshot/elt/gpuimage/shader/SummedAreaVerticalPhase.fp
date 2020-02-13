#version 400 core

in VertexData {
   vec2 uniformCoord;
} fs_in;

uniform sampler2D texture1;
uniform int pixelLookback;
uniform int scanStart;

#include HistogramAccess.vp
out vec4 FragColor;

const ivec2 BLACK_HISTOGRAM = ivec2(0,0);
const vec4 BLACK_COLOR = vec4(0.0,0.0,0.0,1.0);

void main()
{
    resetFBSize(fbDim, maxPixel, buckets);
    ivec2 origin = getRowColFromUniformPosition(fs_in.uniformCoord);

    vec4 cumulativeTexture;
    if (origin == BLACK_HISTOGRAM) {
       cumulativeTexture = BLACK_COLOR;
    } else {
       cumulativeTexture = texture(texture1, getTexturePositionForRowCol(origin));
       for (int lookback=1;lookback<pixelLookback;++lookback) {
          ivec2 currentLookback = origin - ivec2(0, lookback*pow(pixelLookback, scanStart));
          if ((currentLookback.x >= 0 && currentLookback.y >= 0) &&
              (currentLookback != BLACK_HISTOGRAM)) {
             vec2 currentLookbackTexPos = getTexturePositionForRowCol(currentLookback);
             vec4 texel = texture(texture1, currentLookbackTexPos);
             cumulativeTexture = vec4(cumulativeTexture.rgb + texel.rgb, 1.0);
          }
       }
    }

    FragColor = vec4(cumulativeTexture.rgb, 1.0);
}