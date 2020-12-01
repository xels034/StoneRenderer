#version 330

in vec4 fPos;
in vec3 fWPos;
in vec4 fNorm;
in vec4 fEye;
in vec2 fUV;

uniform float exposure;
uniform sampler2DMS fboTex;
uniform ivec2 dim; //dimensions of the texture
//uniform int samples;

out vec4 outColor;

//averadge all multisamples. is this just a box filter?
void main(void) {

    const int samples = 4;

    ivec2 coords = ivec2(fUV.x*dim.x,
                         fUV.y*dim.y);

    for(highp int i=0; i<samples; i++){
        outColor += texelFetch(fboTex, coords, i);
    }

    outColor /= samples;
    outColor *= exposure; //adjust exposure of taken image, simulating an adapting camera sensor
    outColor.a = 1;
}
