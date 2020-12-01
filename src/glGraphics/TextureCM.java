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

import org.lwjgl.opengl.GL13;

import util.Ref;
import util.Vector2i;
import extern.PNGDecoder;

public class TextureCM extends Texture{

  public TextureCM(String fn){
    FileInputStream fis;
    try {
      type = GL13.GL_TEXTURE_CUBE_MAP;

      ByteBuffer[] faces = new ByteBuffer[6];
      
      for(int i=0; i<6;i++){
        fis = new FileInputStream(fn+i+".png");
        PNGDecoder peng = new PNGDecoder(fis);
        dimension = new Vector2i(peng.getWidth(), peng.getHeight());
        
        faces[i] = ByteBuffer.allocateDirect(4 * peng.getWidth() * peng.getHeight());
        peng.decode(faces[i], peng.getWidth() * 4, PNGDecoder.Format.RGBA);
        faces[i].flip();
        fis.close();
      }

      texID = glGenTextures();
      glActiveTexture(GL_TEXTURE0);
      glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, texID);
      glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
      
      for(int i=0; i<6; i++){
        glTexImage2D(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X+i, 0, GL_RGBA, dimension.x, dimension.y, 0, GL_RGBA, GL_UNSIGNED_BYTE, faces[i]);
      }
      glGenerateMipmap(GL13.GL_TEXTURE_CUBE_MAP);
      glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
      glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
      glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, Ref.aniso);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }
  
}
