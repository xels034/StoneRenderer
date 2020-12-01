package glGraphics;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;

import util.Vector2i;
import extern.PNGDecoder;

public class Texture1D extends Texture{

  public Texture1D(String fn){
    FileInputStream fis;
    try {
      type = GL11.GL_TEXTURE_1D;
      
      fis = new FileInputStream(fn);
      PNGDecoder peng = new PNGDecoder(fis);
      
      dimension = new Vector2i(peng.getWidth(), peng.getHeight());

      ByteBuffer beebee = ByteBuffer.allocateDirect(4 * peng.getWidth());
      peng.decode(beebee, peng.getWidth() * 4, PNGDecoder.Format.RGBA);
      beebee.flip();
      fis.close();
      
      texID = glGenTextures();
      glActiveTexture(GL_TEXTURE0);
      glBindTexture(GL11.GL_TEXTURE_1D, texID);
      glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
      GL11.glTexImage1D(GL11.GL_TEXTURE_1D, 0, GL_RGBA, peng.getWidth(), 0, GL_RGBA, GL_UNSIGNED_BYTE, beebee);
      glGenerateMipmap(GL11.GL_TEXTURE_1D);

      glTexParameteri(GL11.GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      glTexParameteri(GL11.GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
      glTexParameteri(GL11.GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
      //glTexParameteri(GL11.GL_TEXTURE_1D, org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 0);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }
  
}
