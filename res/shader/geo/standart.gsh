#version 150

//wanted to do some subdivision and displacement, but didn't get to it because of time constraints.
//this geometry shader just passes each face right through

layout(triangles) in;
layout(triangle_strip, max_vertices = 3) out;

in vec4[] gPos;
in vec3[] gWPos;
in vec4[] gNor;
in vec4[] gEye;
in vec2[] gUV;

out vec4 fPos;
out vec3 fWPos;
out vec4 fNor;
out vec4 fEye;
out vec2 fUV;

void main(void) {
   for(int i=0; i<3; i++){
      fPos = gPos[i];
      fWPos = gWPos[i];
      gl_Position = fPos;
      fNor = gNor[i];
      fEye=gEye[i];
      fUV = gUV[i];
      EmitVertex();
   }
   EndPrimitive();
}