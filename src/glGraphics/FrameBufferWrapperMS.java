package glGraphics;

/**
 * 
 * Special version of the normal Framebuffer. Supports multisampling.
 * 
 */

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import util.Ref;
import util.SimpleLogger;

public class FrameBufferWrapperMS extends FrameBufferWrapper{

  public FrameBufferWrapperMS(){
    super();
  }
  
  public FrameBufferWrapperMS(int ws){
    this(-1, -1, ws);
  }
  
  public FrameBufferWrapperMS(int rs, int rsID, int ws){
    super();
    
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
    glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, writeTexID);
    
    //generate empty texture with allocated mipmaps
    //glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL_RGBA16F, Ref.xRes, Ref.yRes, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
    glTexImage2DMultisample(GL32.GL_TEXTURE_2D_MULTISAMPLE, Ref.msaa, GL_RGBA16F, Ref.xRes, Ref.yRes, true);
    glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
    glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    //glGenerateMipmap(GL11.GL_TEXTURE_2D);
    //assign texture to the framebuffer
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL32.GL_TEXTURE_2D_MULTISAMPLE, writeTexID, 0);
    
    //generate renderbuffer to store the rest
    rboID = glGenRenderbuffers();
    glBindRenderbuffer(GL30.GL_RENDERBUFFER, rboID);
    //glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, Ref.xRes,Ref.yRes);
    glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, Ref.msaa, GL_DEPTH24_STENCIL8, Ref.xRes,Ref.yRes);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, rboID);
    
    if(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE) SimpleLogger.log("Framebuffer complete", 10, FrameBufferWrapperMS.class, "generateFBO");
    else                                   throw new OpenGLException("Framebuffer incomplete");
    
    
    System.out.println("MS size="+Ref.xRes+"x"+Ref.yRes);
  }
}
