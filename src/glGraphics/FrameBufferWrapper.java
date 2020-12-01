package glGraphics;

/**
 * The FrameBufferWrapper holds all the informations to successfully bind a FrameBuffer object
 * in openGL in conjunction with usage of the FrameBufferManager.
 * 
 * The purpose of FrameBuffers in this application is, to allow post-process filtering after the regular drawing.
 * This works sequentially, so each pass reads from a texture, and draws to another one. To support this, the
 * FrameBufferWrapper holds additional information than would be needed to just bind a frameBuffer in openGL.
 * 
 * It additionally stores information about which texture should be read, and which texture should be written.
 * It also stores a shader, that is activated then the buffer gets bound as to easily provide a context to draw in
 * This information is set by the user in the method setupPipeLine. Changing the shader later on is no problem.
 */

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.util.HashMap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import util.Ref;
import util.SimpleLogger;

public class FrameBufferWrapper {
  
  protected int fboID, rboID = -1;
  
  protected int writeTexID = -1;
  protected int readTexID = -1;
  
  protected int readSlot = -1;
  protected int writeSlot = -1;
  
  protected ShaderWrapper shader = null;
  
  protected boolean ready;
  
  protected static HashMap<Integer, Integer> slotMap;
  
  private static void setupSlotMap(){
    slotMap = new HashMap<>();

    slotMap.put(10, GL_TEXTURE10);
    slotMap.put(11, GL_TEXTURE11);
    slotMap.put(12, GL_TEXTURE12);
    slotMap.put(13, GL_TEXTURE13);
    slotMap.put(14, GL_TEXTURE14);
    slotMap.put(15, GL_TEXTURE15);
    slotMap.put(16, GL_TEXTURE16);
    slotMap.put(17, GL_TEXTURE17);
  }
  
  public FrameBufferWrapper(){
    if(slotMap == null) setupSlotMap();
    ready = false;
  }
  
  public FrameBufferWrapper(int ws){
    this(-1, -1, ws);
  }
  
  public FrameBufferWrapper(int rs, int rsID, int ws){
    this();
    
    readSlot = rs;
    readTexID = rsID;
    writeSlot = ws;
    generateFBO();
  }
  
  private void generateFBO(){

    fboID = glGenFramebuffers();
    glBindFramebuffer(GL_FRAMEBUFFER, fboID);
    
    writeTexID = glGenTextures();
    glActiveTexture(slotMap.get(writeSlot));
    glBindTexture(GL11.GL_TEXTURE_2D, writeTexID);
    
    //generate empty texture with allocated mipmaps
    glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL_RGBA16F, Ref.xRes, Ref.yRes, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
    //glTexImage2DMultisample(GL32.GL_TEXTURE_2D_MULTISAMPLE, Ref.msaa, GL_RGBA16F, Ref.xRes, Ref.yRes, true);
    glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
    glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glGenerateMipmap(GL11.GL_TEXTURE_2D);
    //assign texture to the framebuffer
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, writeTexID, 0);

    
    //generate renderbuffer to store the rest
    rboID = glGenRenderbuffers();
    glBindRenderbuffer(GL30.GL_RENDERBUFFER, rboID);
    glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, Ref.xRes,Ref.yRes);
    //glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, Ref.msaa, GL_DEPTH24_STENCIL8, Ref.xRes,Ref.yRes);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, rboID);
    
    if(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE) SimpleLogger.log("Framebuffer complete", 10, FrameBufferWrapper.class, "generateFBO");
    else                                   throw new OpenGLException("Framebuffer incomplete");
    
    

  }
  
  public void setPipeLine(int rs, int rsid, int ws, int wsid, ShaderWrapper sw){
    readSlot = rs;
    writeSlot = ws;
    readTexID = rsid;
    writeTexID = wsid;
    shader = sw;
    
    ready = true;
  }
  
  public void bind(boolean clear){
    if(!ready) throw new IllegalStateException("Framebuffer creation was unsuccessfull! Can't bind");

    //setup texture for reading
    if(readTexID != -1){
      glActiveTexture(slotMap.get(readSlot));
      
      if(readSlot == Ref.fboTargetSlot[0]){//Tex2DMS
        glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, readTexID);
      }else{
        glBindTexture(GL11.GL_TEXTURE_2D, readTexID);
        glGenerateMipmap(GL11.GL_TEXTURE_2D);
      }
    }

    //setup texture for writing
    glActiveTexture(slotMap.get(writeSlot));
    
    if(writeSlot == Ref.fboTargetSlot[0]){//Tex2DMS
      glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, writeTexID);
    }else{
      glBindTexture(GL11.GL_TEXTURE_2D, writeTexID);
    }

    if(shader != null){
          glUseProgram(shader.getShaderID());
          shader.storeUniform("fboTex", readSlot);
    }
 
    glBindFramebuffer(GL_FRAMEBUFFER, fboID);
    if(clear)glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
  }
  
  public void bind(){
    bind(true);
  }
  
  public int getWriteTextureID(){
    return writeTexID;
  }
  
  public void releaseFBO(){
    glDeleteTextures(writeTexID);
    glDeleteFramebuffers(fboID);
  }

}
