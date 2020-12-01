#version 330

in vec4 fPos;
in vec3 fWPos;
in vec4 fNor;
in vec4 fEye;
in vec2 fUV;

out vec4 outColor;

uniform sampler2D colTex;
uniform sampler2D lavaTex;
uniform samplerCube worldTex;
uniform sampler1D heatTex;
uniform vec3 lightDir;

uniform float heat;

//like fNor, but gone through bumpmapping
vec4 bNor;

//do not use UV coordinates, as they are unsuitable for spherical objects
//instead, use a cube-projection in world space. as the object isn't moving/rotating, its fine

//this mapping supports a generic offset/scale value aswell as a distorsion factor. The texture coordinates are
//distorted by the texture itself
vec4 map(sampler2D tex, vec2 offset, float size, float dist){
    vec2 dvYZ = (texture(tex, vec2(fWPos.y, fWPos.z)*size)).xy;
    vec2 dvXZ = (texture(tex, vec2(fWPos.x, fWPos.z)*size)).xy;
    vec2 dvXY = (texture(tex, vec2(fWPos.x, fWPos.y)*size)).xy;

    //use 3 different projections. This way, there is no visible stretching of the texture, like
    //there would be with simple projection
    vec4 xProj = texture(tex, vec2(fWPos.y, fWPos.z)*size + dvYZ*dist + offset);
    vec4 yProj = texture(tex, vec2(fWPos.x, fWPos.z)*size + dvXZ*dist + offset);
    vec4 zProj = texture(tex, vec2(fWPos.x, fWPos.y)*size + dvXY*dist + offset);

    //vector storing how much the frament points in a certain dimension
    vec4 projNor = fNor*sign(fNor);//converting all negative parts to positive ones

    //use the above vector to mix the 3 different projections.
    vec4 col = xProj*projNor.x + yProj*projNor.y + zProj*projNor.z;

    return col;
}

//found on the internet, doesn't really work
vec4 bump2(sampler2D bm, float s, float d, float str){
    vec3 off= vec3(-1,0,1);
    vec2 size = vec2(2,0);
    
    float s11 = map(bm, vec2(0),s, d).x;
    float s01 = map(bm, off.xy, s, d).x;
    float s21 = map(bm, off.zy, s, d).x;
    float s10 = map(bm, off.yx, s, d).x;
    float s12 = map(bm, off.yz, s, d).x;

    vec3 va = normalize(vec3(size.xy, s21-s01));
    vec3 vb = normalize(vec3(size.yx, s12-s10));
    
    vec4  ret = vec4(cross(va, vb)*s11, 0);
    //ret.x*=ret.a;
    //ret.y*=ret.a;
    //ret.z*=ret.a;
    ret.a=0;
    
    return ret+fNor;
}

//the difference of value in x or y direction is also a good indicator of where a normal would be pointing
vec4 bump(sampler2D bm, float s, float d, float str){    

    //take 2 samples near to each other. the difference is the amount the normal should point in that direction
    float dx = length(map(bm, vec2(0), s, d).rgb) - length(map(bm, vec2(0.01, 0), s, d).rgb);
    float dy = length(map(bm, vec2(0), s, d).rgb) - length(map(bm, vec2(0, 0.01), s, d).rgb);

    return vec4(dx,dy,0,0)*str+fNor;
}

float diffuse(float fallOff, vec4 nor){
    float l;
   
    vec4 ld = normalize(vec4(lightDir,0));
    l = clamp(dot(-ld,nor),0,1);
    l = 1-pow(1-l,fallOff); //make it look more like oren-nayar by multiplying the inverse
    return l;
}

//use either a specified normal or the default one
float diffuse(float f){
    return diffuse(f, bNor);
}


float phong(float fallOff){
    vec4 ld = normalize(vec4(lightDir, 0));
    vec4 r = reflect(ld,bNor);    
    return pow(clamp(dot(fEye,r),0,1),fallOff);
}

//generic fresnel-like ramp. actually its just 1 - lambert
float fresnel(float fallOff, float strength, float boost){
    float fresnel = 1-clamp(dot(fEye,bNor),0,1);
    fresnel = pow(fresnel,fallOff); 
    fresnel *= strength; 
    fresnel += boost;
    return fresnel;
}

//fake shadow. the closer a fragment is to the ground, the darker it gets
float shadow(float str){
    float s = 1-fWPos.z ;
    s *= str;
    return 1-clamp(s,0,1);
}

//from a given value, determine the color and luminocity of the heat radiation
vec3 radiate(float gradient){
    float temp = max((heat-600),0);
    temp *= gradient;
    vec3 c = texture(heatTex, temp/8000).rgb;//heatTex has the color encoded as a function of temperature from 0-8000°C
    temp *= 0.003;
    return c* pow(temp, 2); //gives a nicer differentiation in luminocity
}

void main(void) {

    float colS = .4;//size of the color texture for projection mapping
    float lavaS = .4 / (heat/1000); //sive of the lava texture. gets modified by temperature, gives the impression of moving currents

    float colD = .2; //distorsion of color texture
    float lavaD = .2 + (heat/6000); //distortion of lava texture, again modified by temperature for illusion of moving currents

    bNor = normalize(bump(colTex, colS, colD, 1.0));

    //sky reflection
    vec3 refVec = reflect(fEye,bNor).xyz;
    refVec.z = -refVec.z; //cubemap reflection was upway down
    vec3 refDiff = textureLod(worldTex, bNor.xyz, 8).rgb; //fake blurry reflections by using a mipmap level
    vec3 refSpec = textureLod(worldTex, refVec, 0).rgb; //fake blurry reflections by using a mipmap level

    vec4 mapCol =  map(colTex,  vec2(0), colS,  colD);
    vec4 mapLava = map(lavaTex, vec2(0), lavaS, lavaD);

    //base color. multiplied with the lava color and darkened in the end
    vec3 base = mapCol.rgb * mix(mapLava.rgb, vec3(1), .75) * 0.2;

    //specular map with strength 0.2
    float dirt = mix(1, length(mapCol.rgb), 0.2);

    float lambert = diffuse(1, bNor);
    //used for mixing reflections
    float f = fresnel(3, 0.75, 0.05);
    //f=1;
    vec3 sum;

    sum = mix(base*lambert, refSpec*dirt, f); //diffuse+specular reflections
    sum += base*fresnel(1,0.2,0); //give the stone a rough feeling by highlighting the edges
    sum *= shadow(0.4);
    sum += refDiff*base*0.35; //diffuse sky reflection

    float p = phong(80)*f*0.3; //used for sunlight reflection
    sum += vec3(1,0.9,0.8)*p;
    
    float rampStr = 3;
    float g = length(mapLava.rgb)/3;//averadge of rgb

    g *= rampStr;
    g = max(1-g,0);
    g += fresnel(1,0.55,0.1);//gives a tiny hint of translucency of molten parts

    sum += radiate(g+0.3);

    outColor = vec4(sum,1);
}
