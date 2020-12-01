#version 330
in vec4 fPos;
in vec3 fWPos;
in vec4 fNorm;
in vec4 fEye;
in vec2 fUV;

uniform sampler2D glare;
uniform sampler2D original;

uniform float time;

out vec4 outColor;

//bit wrangling, one-at-a-time hash function
//#justBobJenkinsThings
uint hash(uint x){
    x += ( x << 10u);
    x ^= ( x >> 6u);
    x += ( x << 3u);
    x ^= ( x >> 11u);
    x += ( x << 15u);
    return x;
}

uint hash( uvec2 v ) { return hash( v.x ^ v.y             ); } //XOR ALL THE THINGS!
uint hash( uvec3 v ) { return hash( v.x ^ v.y ^ v.z       ); }
uint hash( uvec4 v ) { return hash( v.x ^ v.y ^ v.z ^ v.w ); }

float floatConstruct (uint m) {
    const uint ieeeMantissa = 0x007FFFFFu; //mantissa mask of a float, in unsigned notation
    const uint ieeeOne      = 0x3F800000u; //one in a float, in unsigned notation, according to the IEEE specifications

    //sooo. we have random bits in m, but want a float [0,1]
    //we take the part of the randomBitSausage-thingy that is for the floating part, and preserve it with &=mantissa
    //we then set all the bits for the 1.0 part to 1 with |=one. So there arent any dirty random bytes in there.
    //we can "add" (using or operation) easily, but removing ONLY the one part seems tricky. so we have a 1+random bits in fraction part

    m &= ieeeMantissa; 
    m |= ieeeOne;

    float f = uintBitsToFloat (m); //convert our bitSausage back to a proper float. like (float)unit without any conversions
    return f - 1.0; //subtract the one from before, because now its easy, the GPU knows how to subtract a float (we dont, apparently)
}

float random (float x) {return floatConstruct(hash(floatBitsToUint(x)));} //actually a cast (uint)float without any conversions
float random (vec2  x) {return floatConstruct(hash(floatBitsToUint(x)));}
float random (vec3  x) {return floatConstruct(hash(floatBitsToUint(x)));}
float random (vec4  x) {return floatConstruct(hash(floatBitsToUint(x)));}

void main(void){

    vec3 coords = vec3(fUV, time);

    vec4 noise = vec4(random(coords), random(coords-1), random(coords+1), 1);
    noise = pow(noise,vec4(10))*1; //create more spiky noise

    vec4 color = texture(original, fUV) + texture(glare, fUV);
    color.a=1;

    float luma = 1-min((color.r + color.g + color.b)/3, 1);

    outColor = mix(color, noise, luma*0.05);
}