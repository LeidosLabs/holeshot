#version 400 core

uniform float constantRed;
uniform float constantGreen;
uniform float constantBlue;

out vec4 FragColor;

void main()
{
    FragColor = vec4(constantRed, constantGreen, constantBlue, 1.0);
}


