package glGraphics;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;

import util.Ref;
import util.Vector2i;
import extern.PNGDecoder;

public class Texture2D extends Texture{

  public Texture2D(String fn){
    FileInputStream fis;
    try {
      type = GL11.GL_TEXTURE_2D;
      
      fis = new FileInputStream(fn);
      PNGDecoder peng = new PNGDecoder(fis);
      
      dimension = new Vector2i(peng.getWidth(), peng.getHeight());
      
      ByteBuffer beebee = ByteBuffer.allocateDirect(4 * peng.getWidth() * peng.getHeight());
      peng.decode(beebee, peng.getWidth() * 4, PNGDecoder.Format.RGBA);
      beebee.flip();
      fis.close();
      
      texID = glGenTextures();
      glActiveTexture(GL_TEXTURE0);
      glBindTexture(GL11.GL_TEXTURE_2D, texID);
      glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
      glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL_RGBA, peng.getWidth(), peng.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, beebee);
      glGenerateMipmap(GL11.GL_TEXTURE_2D);

      glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
      glTexParameteri(GL11.GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
      glTexParameteri(GL11.GL_TEXTURE_2D, org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, Ref.aniso);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }
  
}
