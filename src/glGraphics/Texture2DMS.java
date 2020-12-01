package glGraphics;


import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;

import static org.lwjgl.opengl.GL32.glTexImage2DMultisample;

import org.lwjgl.opengl.GL32;

import util.Ref;
import util.Vector2i;

public class Texture2DMS extends Texture{

  public Texture2DMS(int xr, int yr){
    type = GL32.GL_TEXTURE_2D_MULTISAMPLE;

    dimension = new Vector2i(xr, yr);

    texID = glGenTextures();
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, texID);
    
    glTexImage2DMultisample(GL32.GL_TEXTURE_2D_MULTISAMPLE, Ref.msaa, GL_RGBA16F, xr, yr, true);
  }  
}
