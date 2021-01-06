#version 400 core

in VertexData {
   vec2 texturePosition;
} vs_in;


uniform sampler2D imageTexture;
out vec4 FragColor;

void main()
{
   FragColor = texture2D(imageTexture, vs_in.texturePosition);
//   FragColor = vec4(1.0,0.0,0.0,1.0);
}




