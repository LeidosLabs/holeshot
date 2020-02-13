#version 400 core

in VertexData {
   vec2 texturePosition;
} vs_in;

uniform sampler2D imageTexture;
out vec4 FragColor;

void main()
{
    FragColor = texture(imageTexture, vs_in.texturePosition);
}
