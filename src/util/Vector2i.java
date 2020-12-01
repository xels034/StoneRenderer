package util;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.util.vector.Vector;

public class Vector2i extends Vector{

  private static final long serialVersionUID = 1L;
  
  public int x,y;
  
  public Vector2i(){}
  
  public Vector2i(int x, int y){
    this.x=x;
    this.y=y;
  }
  
  @Override
  public float lengthSquared() {
    return x*x+y*y;
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
    return new Vector2i(-x,-y);
  }

  @Override
  public Vector scale(float arg0) {
    return new Vector2i((int)(x*arg0), (int)(y*arg0));
  }

  @Override
  public Vector store(FloatBuffer arg0) {
    throw new NotImplementedMethodException("lolnope");
  }
  
  public Vector store(IntBuffer ib){
    throw new NotImplementedMethodException("too lazy, but doable");
  }

}
