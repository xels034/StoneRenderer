#version 150

in vec4 fPos;
in vec3 fWPos;
in vec4 fNor;
in vec4 fEye;
in vec2 fUV;

out vec4 outColor;

uniform sampler1D heatTex;
uniform samplerCube sky;
uniform vec3 lightDir;

uniform float heat;

//let the ground plane fade out into the skybox
float blendOut(float offset, float scale){
    float a = fPos.z;//use distance from camera
    a-=offset; //shift the begin of the effect way from the camera
    a/=scale; //stretch out the fading
    a = 1-a; //inverting, so near=opaque
    return clamp(a,0,1);
}

float shadow(float radius, float strength){
    float s = length(fWPos); //plane model is centered around 0,0,0, where the shadow should be
    s /= radius; //stretch out the effect
    s += (1-strength);
    return clamp(s,0,1);
}

float diffuse(float fallOff){
    vec4 ld = normalize(vec4(lightDir,0));
    float lambert = max(dot(-ld,fNor),0);
    return 1-pow(1-lambert,fallOff); //make it look more like oren-nayar by multiplying the inverse
}

vec3 radiate(){
    float dist = pow(length(fWPos),1.5);
    vec3 col = texture(heatTex, (heat-600)/8000).rgb; //shift the heat -600, because the stone is actually cooler than the specified temperature (because of textures) 
    return col * max((heat-680),0)/(dist*500); //divide by dist for fallow. divide also by 500 to get a reasonably luminocity
}

void main(void) {

    float a = blendOut(0,30);
    float s = shadow(1, 1.8);
    s = pow(s,0.8);
    vec3 ref = textureLod(sky, reflect(fEye,fNor).xyz, 8).rgb; //diffuse reflection of the sky

    vec3 color = vec3(0.3,0.3,0.3);

    color *= diffuse(1.5);
    
    color += ref*0.25;
    color *= s; //shadow affects sky reflection aswell
    color += radiate();

    outColor = vec4(color,a);
}
