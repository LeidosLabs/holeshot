#version 400 core

in VertexData {
   vec2 uniformCoord;
} fs_in;

uniform sampler2D inputImage;
uniform sampler2D uTonalTransferSampler;

uniform float ic_sub;
uniform float ic_mul;
uniform float ic_gamma;

out vec4 FragColor;

#include HistogramAccess.vp
void main()
{
    resetFBSize(fbDim, maxPixel, buckets);
    vec4 inputPixel = texture(inputImage, getTexturePositionFromUniform(fs_in.uniformCoord));
    vec3 BLACK = vec3(0.0, 0.0, 0.0);

    //Copied from limelight
    if (inputPixel.rgb == BLACK) {
       FragColor = inputPixel;
    } else {
       vec3 lookupValue = vec3(inputPixel);

       //we don't currently have a way of manipulating these but they would normally be uniforms
       float uBrightness = ic_sub;
       float uContrast = ic_mul;
       vec3 uGamma = vec3(ic_gamma, ic_gamma, ic_gamma);

       lookupValue = clamp(pow((lookupValue + uBrightness) * uContrast, uGamma), 0.0, 1.0);

       // We set max pixel to 65536 rather than maxPixel because its a 16 bit lut.
       resetFBSize(ivec2(256,256), 65536, 65536);

       ivec2 ttcRed = getRowColPositionForIntensity(lookupValue.r);
       ivec2 ttcGreen = getRowColPositionForIntensity(lookupValue.g);
       ivec2 ttcBlue = getRowColPositionForIntensity(lookupValue.b);

       vec4 resultVec = clamp(vec4(texture(uTonalTransferSampler, getTexturePositionForRowCol(ttcRed)).x,
                              texture(uTonalTransferSampler, getTexturePositionForRowCol(ttcGreen)).x,
                              texture(uTonalTransferSampler, getTexturePositionForRowCol(ttcBlue)).x, inputPixel.a), 0.0, 1.0);

       FragColor = resultVec;
    }
}