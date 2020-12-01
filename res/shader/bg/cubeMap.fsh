#version 150

in vec4 fPos;
in vec3 fWPos;
in vec4 fNor;
in vec4 fEye;
in vec2 fUV;

out vec4 outColor;

uniform samplerCube tex;

void main(void){

    outColor = texture(tex, fWPos);
    outColor.a=1;
}