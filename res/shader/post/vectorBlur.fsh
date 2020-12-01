
//working one-pass blur split up in horizontal and vertical components. This was an attempt
//at a correct two-pass blur. However, correct results require 2 textures to
//read from simultaneously.

#version 150

in vec4 fPos;
in vec3 fwPos;
in vec4 fNorm;
in vec4 fEye;
in vec2 fUV;

uniform sampler2D fboTex;
uniform int quality;
uniform vec2 dir;
uniform ivec2 texDim;
uniform int maxLOD;
uniform float strength;
uniform float threshold;

const int wLen=32;

uniform float[wLen] weights;

out vec4 outColor;

void main(void) {
    vec4 sum = vec4(0.0f, 0.0f, 0.0f, 0.0f);
    vec4 sample;
    float factor;
    vec2 tmpTexCord;
    int totalSamples = (quality*2 + 1);

    //taking too few samples (less than 1 per pixel. so eg. 4 samples for a diameter of 4)
    //results in noticable repetition of the source image instead of a blur. To allow fewer
    //samples and still achieve a greater radius, read from a higher mipmap level, and let
    //the linear interpolation from the sampler do the trick to avoid image visible image repetition
    float lod = log2(length(dir) / quality);
    lod = min(lod,maxLOD);//go only as high as there are mipmap levels. calculated once per drawcall from the outside

    //uniform controlled controlled. maybe not the most efficient, but pretty useful still
    for(float i=-quality; i<=quality; i++){
        factor = (i / quality);
        tmpTexCord = fUV + (dir*factor)/texDim;
        
        sample = textureLod(fboTex, tmpTexCord, lod);
        //force a positive value
        factor *= sign(factor);

        sample *= weights[int(factor*wLen)];
        sum += sample;
    }

    sum /= totalSamples;
    
    float fac = max(0, length(sum.rgb)-threshold);//only regions brither than the threshold let the light spill and appear as glow
    
    sum = vec4(fac*normalize(sum.rgb), 1);//apply luminocity thresholding. the color however is not that of the thresholded component, but the apparent (0-1 range). just looks nicer.

    outColor = pow(sum, vec4(1.25)) * strength; //pow() makes the falloff more appealing. Also increases the saturation of the glow, but that looks even nicer
    //outColor = sum*strength;

    outColor = max(outColor, vec4(0,0,0,0));
    outColor.a = clamp(outColor.a, 0,1);

}
