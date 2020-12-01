package glGraphics;

/**
 * 
 * The ScreenBufferWrapper is a special from of a FrameBufferWrapper, specifically created
 * to draw on the screen. Because the screen target can't be configured as a typical texture,
 * special handling is necessary.
 */

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

import util.Ref;
import util.SimpleLogger;

public class ScreenBufferWrapper extends FrameBufferWrapper {

  public ScreenBufferWrapper(int rs, int rsID){
    super();
    readSlot = rs;
    readTexID = rsID;
    
    ready=true;
  }
  
  @Override
  public void bind(boolean clear){
    if(!ready) throw new IllegalStateException("Screenbuffer creation was unsuccessfull! Can't bind");
    
    if(readSlot != -1){
      glActiveTexture(slotMap.get(readSlot));
      
      if(readSlot == Ref.fboTargetSlot[0]){
        glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, readTexID);
      }else{
        glBindTexture(GL11.GL_TEXTURE_2D, readTexID);
        glGenerateMipmap(GL11.GL_TEXTURE_2D);
      }
      
      if(shader != null){
            glUseProgram(shader.getShaderID());
            shader.storeUniform("fboTex", readSlot);
      }
    }
    


    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    if(clear) glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    
  }
  
  @Override
  public void setPipeLine(int rs, int rsid, int ws, int wsid, ShaderWrapper sw){
    readSlot = rs;
    readTexID = rsid;
    shader = sw;
    ready = true;
  }
  
  @Override
  public int getWriteTextureID(){
    SimpleLogger.log("Warning: attempting to get texture ID of screenbuffer. Its always 0 and can't be read!", 0, this.getClass(), "getWriteTextureID");
    return 0;
  }
  
  @Override
  public void releaseFBO(){
    
  }
}
