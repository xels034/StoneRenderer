package util;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.util.vector.Vector;

public class Vector4i extends Vector{

  private static final long serialVersionUID = 1L;
  
  public int x,y,z,w;
  
  public Vector4i(){}
  
  public Vector4i(int x, int y, int z, int w){
    this.x=x;
    this.y=y;
    this.z=z;
    this.y=y;
  }
  
  @Override
  public float lengthSquared() {
    return x*x+y*y+z*z+w*w;
  }

  @Override
  public Vector load(FloatBuffer arg0) {
    throw new NotImplementedMethodException("lolnope");
  }
  
  public Vector load(IntBuffer ib){
    throw new NotImplementedMethodException("too lazy, but doable");
  }

  @Override
  public Vector negate() {
    return new Vector4i(-x,-y, -z, -w);
  }

  @Override
  public Vector scale(float arg0) {
    return new Vector4i((int)(x*arg0), (int)(y*arg0), (int)(z*arg0), (int)(w*arg0));
  }

  @Override
  public Vector store(FloatBuffer arg0) {
    throw new NotImplementedMethodException("lolnope");
  }
  
  public Vector store(IntBuffer ib){
    throw new NotImplementedMethodException("too lazy, but doable");
  }

}
