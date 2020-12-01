package util;

/**
 *
 * Simple collection of application wide constants
 *
 */

public class Ref {
  
  public static final int LOG_LEVEL = -1;
  
  //how long, in ms, it takes for a Construct to be discarded if it isn't drawn
  public static final long renderPause = 10000;
  
  //map GL_ACTIVE_TEXTURE slots for up to 8 FrameBuffers
  public static final int[] fboTargetSlot = {10, 11, 12, 13, 14, 15, 16, 17};
  public static final int MAX_FBO = 8;
  public static final int FBO_CNT = 5;
  
  //Display, FBOs, Shader
  public static int xRes = 1240;
  public static int yRes = 720;
  public static int msaa = 1;
  public static int aniso = 16;
  public static final int maxFPS = 600;

  //glsl in variable positions and shaderProgram idx
  public static final int shPosAttrib = 0;
  public static final int shNorAttrib = 1;
  public static final int shTexAttrib = 2;
}
