#version 150

in vec3 vPos;
in vec3 vNor;
in vec2 vUV;

out vec4 gPos;
out vec3 gWPos;
out vec4 gNor;
out vec2 gUV;

out vec4 gEye;

uniform mat4 MVP;
uniform mat4 M;
uniform mat4 V;
uniform mat4 P;

void main(void) {

    gPos = MVP * vec4(vPos,1);
    gWPos = (M * vec4(vPos,1)).xyz;
    gNor = transpose(inverse(M)) * vec4(vNor,0);

    vec4 pos = inverse(V)*vec4(0,0,0,1);
    gEye = normalize(pos - vec4(vPos,1));

    gUV = vUV;
    gl_Position = gPos;
}
