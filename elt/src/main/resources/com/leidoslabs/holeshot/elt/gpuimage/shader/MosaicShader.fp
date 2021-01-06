#version 400 core

in VertexData {
   vec2 texturePosition;
} vs_in;

uniform sampler2D imageLayer;
out vec4 FragColor;

void main()
{
    FragColor = texture(imageLayer, vs_in.texturePosition);
}


