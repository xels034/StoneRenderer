package glGraphics;

/**
 * 
 * The FrameBufferManager handles the different stages for offscreen rendering and post-processing
 * With the use of many separate textures to read from, any number of post-process calls can be done. Shader
 * may olso use more than one texture as a source of input, enabling sophisticated post-processing effects
 * 
 * The FBM has several drawStates during each frame:
 * 
 * [0]Ready To Draw:
 * The first offscreen buffer is cleared and bound as the render target for openGL. The user may now place
 * any draw calls he wishes to appear on the screen
 * 
 * [1]Ready for Post-Process
 * Drawing of any geometry is finished. FrameBuffer 1 is marked as the default read-source. From here on, simple
 * post-processing calls with single read-sources may be called while automatically iterating through all available
 * FrameBuffers. It is also possible to set read and write sources indiviually to offer more possibilities regarding
 * shaders. After all post-processing is done, the image is drawn on the screen.
 * 
 * Drawing on the screen requires also a shader.
 * Applying a last post-process pass. This is usually an FXAA pass as to reduce AA artifacts. Ordinary MSAA doesn't
 * seem to get rid of aliasing in many cases. However, the user may assign a "blank" shader, that just draws the
 * current offscreen texture 1:1 on the screen
 * 
 * [2]All passes finished
 * The actual image is drawn on the screen. Any draw calls an any additional passes are done. To do this, a
 * screen.aligned quad is used. To store the geometry and texture coordinates, a special SA_QuadWrapper object
 * is used. It is significantly different than ordinary drawable objects (Constructs), as those only permit lines
 * and vertex colors, as opposed to faces and texture coordinates. The FBM now has to be reset to begin accepting new,
 * fresh draw calls for geometry. 
 */

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import util.Ref;
import util.SimpleLogger;

public class FrameBufferManager {
  
  //Object to easily hold all geometry for the screen-aligned Quad needed for post-processing
  //a construct is just too different
  private class SA_QuadWrapper {
    int vaScreen;
    int vbScreen;
    int ebScreen;
    
    public SA_QuadWrapper(){
      setupVAO();
      setupAttribLocs();
    }
    
    public void execute(){
      glBindVertexArray(vaScreen);
      glDrawElements(GL_TRIANGLES, quadElems.length, GL_UNSIGNED_INT, 0);
    }
    
    private void setupVAO(){
      vaScreen = glGenVertexArrays();
      glBindVertexArray(vaScreen);
      
      vbScreen = glGenBuffers();
      glBindBuffer(GL_ARRAY_BUFFER, vbScreen);
      
      FloatBuffer buff = (ByteBuffer.allocateDirect(screenVerts.length*(Float.SIZE/8)).order(ByteOrder.nativeOrder())).asFloatBuffer();
      buff.put(screenVerts);
      buff.flip();
      glBufferData(GL_ARRAY_BUFFER, buff, GL_STATIC_DRAW);
      
      ebScreen = glGenBuffers();
      glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebScreen);
      
      IntBuffer buffi = (ByteBuffer.allocateDirect(quadElems.length*(Float.SIZE/8)).order(ByteOrder.nativeOrder())).asIntBuffer();
      buffi.put(quadElems);
      buffi.flip();
      glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffi, GL_STATIC_DRAW);
    }
    
    private void setupAttribLocs(){
      glBindVertexArray(vaScreen);
      glBindBuffer(GL_ARRAY_BUFFER,vbScreen);
      
      glVertexAttribPointer(Ref.shPosAttrib, 3, GL_FLOAT, false, 8*(Float.SIZE/8), 0L);
      glEnableVertexAttribArray(Ref.shPosAttrib);  
      glVertexAttribPointer(Ref.shNorAttrib, 3, GL_FLOAT, false, 8*(Float.SIZE/8), 3*(Float.SIZE/8));
      glEnableVertexAttribArray(Ref.shNorAttrib);
      glVertexAttribPointer(Ref.shTexAttrib, 2, GL_FLOAT, false, 8*(Float.SIZE/8), 6*(Float.SIZE/8));
      glEnableVertexAttribArray(Ref.shTexAttrib);
    }
    
    public void releaseVAO(){
      glDeleteBuffers(vbScreen);
      glDeleteBuffers(ebScreen);
      glDeleteVertexArrays(vaScreen);
    }
    
    public final float[] screenVerts = {
      // X     Y     Z     x     y     z     U     V
      -1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
       1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f,
       1.0f,  1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
      -1.0f,  1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f 
    };
  
    public final int[] quadElems = {0, 1, 2,
                    2, 3, 0
    };
  }

  private ScreenBufferWrapper sbo;
  private SA_QuadWrapper saq;
  
  private FrameBufferWrapper[] fbo;
  private int[] texID;
  
  int activeFBO;
  int prevFBO;
  
  //0 ready to draw
  //1 ready to postPro
  //2 finalized
  //int drawState;
  
  public FrameBufferManager(){ //shader that has the vertexShader for the screen aligned quad, so that attrib mapping can be done
    
    int buffers = Ref.FBO_CNT;
    if(buffers < 1 || buffers > Ref.MAX_FBO) throw new IllegalArgumentException("Number of buffer must be betwenn 1 and "+Ref.MAX_FBO);
    
    fbo = new FrameBufferWrapper[buffers];
    texID = new int[buffers];
    
    fbo[0] = new FrameBufferWrapperMS(Ref.fboTargetSlot[0]);
    texID[0] = fbo[0].getWriteTextureID();
    
    for(int i = 1; i< buffers; i++){
      fbo[i] = new FrameBufferWrapper(Ref.fboTargetSlot[i-1], texID[i-1], Ref.fboTargetSlot[i]);
      texID[i] = fbo[i].getWriteTextureID();
    }
    
    sbo = new ScreenBufferWrapper(Ref.fboTargetSlot[buffers-1], texID[buffers-1]);
    
    saq = new SA_QuadWrapper();
    //drawState = -1;
  }
  
  private void setNextFBO(){
    setNextFBO(activeFBO+1);
  }
  
  private void setNextFBO(int active){
    activeFBO = active;
    prevFBO = activeFBO -1 ;
    
    activeFBO%=fbo.length;
    prevFBO%=fbo.length;
  }
  
  public void setSourceRender(){
    setCustomSourceRender(0);
  }
  
  public void setCustomSourceRender(int dest){
    if(dest == -1){
      sbo.setPipeLine(-1, -1, -1, -1, null);
      sbo.bind();
    }else{
      //if(drawState == 0)throw new IllegalStateException("The Manager is already set up to draw");
      //if(drawState == 1)throw new IllegalStateException("There is still undisplayed data in the buffer");
      
      setNextFBO(dest);
      
      fbo[activeFBO].setPipeLine(-1, -1, Ref.fboTargetSlot[activeFBO], texID[activeFBO], null);
      fbo[activeFBO].bind();
      //drawState = 0;
    }
    
  }
  
  public void finalizeSource(){
    //if(drawState != 0) throw new IllegalStateException("Can't finalize source, not in draw mode");
    AppWindow.glx.execute();
    //drawState = 1;
    
    //System.out.println("drawn into "+activeFBO);
  }
  
  //automatic FrameBuffer switching
  public void doNextPostPro(ShaderWrapper sw){
    //if(drawState != 1)throw new IllegalStateException("Can't do post-process, source not finalized");
    
    setNextFBO();
    
    fbo[activeFBO].setPipeLine(Ref.fboTargetSlot[prevFBO], texID[prevFBO], Ref.fboTargetSlot[activeFBO], texID[activeFBO], sw);
    fbo[activeFBO].bind();
    saq.execute();
  }
  
  //custom defined read and write targets
  public void doCustomPostPro(int source, int dest, ShaderWrapper sw){
    if(source == -1)                  throw new IllegalArgumentException("source = -1 is drawMode. PostPro needs a valid read source");
    if(source >= fbo.length || dest >= fbo.length) throw new IllegalArgumentException("source or destination index too high! (max="+(fbo.length-1)+")");
    if(dest == -1) {
      customFinalizeImage(source, sw);
      SimpleLogger.log("Warning: using doCustomPostPro to finalize the image. Use customFinalizeImage() for that!", 0, FrameBufferManager.class, "doCustomPostPro");
    }else{
      setNextFBO(dest);
      
      fbo[dest].setPipeLine(Ref.fboTargetSlot[source], texID[source], Ref.fboTargetSlot[dest], texID[dest], sw);
      fbo[dest].bind();
      saq.execute();
    }
  }
  
  public void finalizeImage(ShaderWrapper sw){
    customFinalizeImage(activeFBO, sw);
  }
  
  public void customFinalizeImage(int source, ShaderWrapper sw){
    //if(drawState != 1)throw new IllegalStateException("Can't finalize image, source not finalized");
    if(source == -1) throw new IllegalStateException("Image finalization needs a valid read source!");
    sbo.setPipeLine(Ref.fboTargetSlot[source], texID[source], -1, -1, sw);
    sbo.bind();
    

    saq.execute();
    //drawState = 2;
  }
  
  public int getFrameBufferCount(){
    return fbo.length;
  }
  
  public void releaseAll(){
    saq.releaseVAO();
    for(FrameBufferWrapper fbw : fbo){
      fbw.releaseFBO();
    }
  }
}
