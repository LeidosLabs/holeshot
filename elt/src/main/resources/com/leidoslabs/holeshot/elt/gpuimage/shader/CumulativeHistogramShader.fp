#version 400 core

in VertexData {
   vec2 uniformCoord;
} fs_in;

out vec4 FragColor;

uniform sampler2D summedAreaData;

#include HistogramAccess.vp


vec4 getCumulativeTexture(ivec2 origin) {
   vec4 cumulativeTexture = texture(summedAreaData, getTexturePositionForRowCol(origin));

    if (origin.y > 0) {
       ivec2 areaB = ivec2(fbDim.x-1,origin.y-1);
       ivec2 areaC = ivec2(origin.x, origin.y-1);
       cumulativeTexture += texture(summedAreaData, getTexturePositionForRowCol(areaB));
       cumulativeTexture -= texture(summedAreaData, getTexturePositionForRowCol(areaC));
    }
    return cumulativeTexture;
 }

void main()
{
    resetFBSize(fbDim, maxPixel, buckets);

    ivec2 origin = getRowColFromUniformPosition(fs_in.uniformCoord);

    vec4 cumulativeTexture = getCumulativeTexture(origin);

    FragColor = vec4(cumulativeTexture.rgb, 1.0);

}