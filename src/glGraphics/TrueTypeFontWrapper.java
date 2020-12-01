package glGraphics;

/**
 * 
 * Implementing working font-drawing is a very sophisticated and time consuming task.
 * As to speed up the process of realizing this application, a third-party solution for
 * fonts was implemented. TrueTypeFont objects are used, which are provided by the slick-util
 * library, recommended by the authors of the lwjgl
 * 
 * However, the required context for correctly drawing fonts differ a bit from the
 * present context. So the TrueTypeFont objects are wrapped up, assuring proper
 * context configuration before drawing.
 */

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector4f;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.util.ResourceLoader;

import util.Ref;

public class TrueTypeFontWrapper {
  
  private TrueTypeFont font;
  private boolean loaded;
  
  public TrueTypeFontWrapper(String fn, float size){
    loaded = false;

    try {
      glUseProgram(0);
      glActiveTexture(GL_TEXTURE0);
      Font awtFont = Font.createFont(Font.TRUETYPE_FONT, ResourceLoader.getResourceAsStream(fn));
      awtFont = awtFont.deriveFont(size);
      font = new TrueTypeFont(awtFont, true);
      loaded = true;
    } catch (FontFormatException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void drawText(float x, float y, String text, Vector4f color){
    if(!loaded) throw new IllegalStateException("Font not loaded yet");
    
    GL11.glMatrixMode(GL11.GL_PROJECTION);
    GL11.glLoadIdentity();
    GL11.glOrtho(0, Ref.xRes, Ref.yRes, 0, 1, -1);
    GL11.glMatrixMode(GL11.GL_MODELVIEW);
    
    glUseProgram(0);
    glActiveTexture(GL_TEXTURE0);
    //Slick keeps track of bound textures. So tell slick to re-bind its own damn stuff
    TextureImpl.bindNone();
    font.drawString(x, y, text, new Color(color.x, color.y, color.z, color.w));
  }

  public int getTextWidth(String t){
    return font.getWidth(t);
  }
  
  public int getTextHeight(String t){
    return font.getHeight(t);
  }
}
