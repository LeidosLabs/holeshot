#version 400 core

in VertexData {
   vec3 colorFactor;
} fs_in;

out vec4 FragColor;

#include HistogramAccess.vp

void main()
{
    resetFBSize(fbDim, maxPixel, buckets);
    FragColor = vec4(fs_in.colorFactor, 1.0);
}