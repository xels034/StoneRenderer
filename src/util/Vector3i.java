package util;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.util.vector.Vector;

public class Vector3i extends Vector{

  private static final long serialVersionUID = 1L;
  
  public int x,y,z;
  
  public Vector3i(){}
  
  public Vector3i(int x, int y, int z){
    this.x=x;
    this.y=y;
    this.z=z;
  }
  
  @Override
  public float lengthSquared() {
    return x*x+y*y+z*z;
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
    return new Vector3i(-x,-y,-z);
  }

  @Override
  public Vector scale(float arg0) {
    return new Vector3i((int)(x*arg0), (int)(y*arg0), (int)(z*arg0));
  }

  @Override
  public Vector store(FloatBuffer arg0) {
    throw new NotImplementedMethodException("lolnope");
  }
  
  public Vector store(IntBuffer ib){
    throw new NotImplementedMethodException("too lazy, but doable");
  }

}
