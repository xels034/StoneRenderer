package glGraphics;

/**
 * 
 * glGraphics is the main hub for collecting draw calls. glGraphics then gets executed by the ApPWindow object
 * and performs the actual openGL commands to draw any geometry.
 * Draw calls are limited to 2 kinds of objects: tris & Text
 * 
 * glGraphics manages a list of all rendered tri-geometry. For rendering text, a third-party solution is used.
 * As the control of how the slick-unit TrueTypeFont makes it's draw calls is severely limited, only rudimentary
 * information is stored.
 * 
 * In the case of tri-geometry, it is advised to use as many re-usable objects as possible. For this, pre-build
 * objects can be registered. They are stored in the glGraphics object and also on the graphics card. This data
 * can be re-used each draw call and thus reducing cpu-gpu communication overhead.
 * 
 * Object data ready to be sent to the gpu are stored in so called Constructs.
 * They essentially represent an entity system for renderable objects, together with TrueTyoeFontWrappers.
 * 
 * Draw calls reference a registered object, determinating the order or draw calls. Eventually, the execute() method
 * is called. It is expected, that he FrameBufferManager is in the correct drawState the execute() is called. The
 * whole drawOrders list is traversed and drawn into the offscreen texture. After drawing, and the workOrder list is cleared.
 * 
 * Any object, that isn't drawn for 10 seconds gets also deleted from the gpu. However, it stays in the list of registered
 * constructs. This entry has to be deleted manually by any draw caller. When a registered object gets deleted, the corresponding
 * VertexArrayObject on the gpu is also deleted. This prevents a memory leak on the cpu aswell as on the gpu, when the same
 * objects may get recreated in the course of multiple game sessions over a very
 * long period of time.
 * 
 * There are some methods needed for mltuthrading. It isn't used in this project but is a leftover from a gameproject, where this class
 * was first developed.
 * 
 * glGraphics has also the ability to load constructs directly from files. This approach should be the default as to not clutter the code
 * with manual construct creation
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import util.Ref;
import util.SimpleLogger;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

public class glGraphics {

  private class ConstructEntry{
    private UUID pointer;
    private Matrix4f model;
    private ShaderWrapper s;
    private int[] tex;
    private int[] texT;
    
    public ConstructEntry(UUID uuid, Matrix4f m, ShaderWrapper sw, Texture[] t){
      pointer = uuid;
      model = new Matrix4f(m);
      s = sw;
      
      tex = new int[t.length];
      texT= new int[t.length];
      for(int i=0; i<t.length;i++){
        tex[i]=t[i].getID();
        texT[i] = t[i].getType();
      }
      
    }
  }
  
  private class TextEntry{
    private Vector2f position;
    private Vector4f color;
    private String text;
    
    public TextEntry(Vector2f p, Vector4f c, String t){
      position = new Vector2f(p);
      color = new Vector4f(c);
      text = t;
    }
  }

  private Matrix4f viewMat, projMat;
  
  //holds all registered constructs if not explicitly freed
  private HashMap<UUID, Construct> assets;
  private HashMap<Integer, Boolean> depthMask;
  //holds all assets that are currently on the gpu
  private ArrayList<UUID> gpuLoaded;
  private LinkedList<UUID> toRelease;
  //linked list of construct idx and separate matrix
  private LinkedList<ConstructEntry> workOrders;
  private LinkedList<TextEntry> textOrders;
  private TrueTypeFontWrapper font;

  private boolean wait;
  
  public glGraphics(){
    wait = false;
    
    assets = new HashMap<>();
    depthMask = new HashMap<>();
    gpuLoaded = new ArrayList<>();
    toRelease = new LinkedList<>();
    
    workOrders = new LinkedList<>();
    textOrders = new LinkedList<>();
    font = new TrueTypeFontWrapper("res/font/trench100free.otf", 35f);
    
    viewMat = new Matrix4f();
    viewMat.translate(new Vector3f(0,0,-3f));
    viewMat.rotate(-(float)(Math.PI/4)*1.5f, new Vector3f(1,0,0));
    viewMat.rotate(45, new Vector3f(0,0,1));

    setupProjMat();
  }
  
  float perc;
  
  public void setupProjMat(){
    projMat = new Matrix4f();
    
    float fov = 90f;
    float aspectRatio = (float)(Ref.xRes)/(float)(Ref.yRes);
    float near_plane = 0.1f;
    float far_plane = 100;
    
    //to radiants
    float asd = 180f / (float)((fov / 2f) *2 *Math.PI);
    //1/tan
    float y_scale = 1f / (float)Math.tan(asd);
    float x_scale = y_scale / aspectRatio;
    float frustum_length = far_plane - near_plane;
    
    projMat.m00 = x_scale;
    projMat.m11 = y_scale;
    projMat.m22 = -((far_plane + near_plane) / frustum_length);
    projMat.m23 = -1;
    projMat.m32 = -((2 * near_plane * far_plane) / frustum_length);
    projMat.m33 = 0;
  }
  
  public UUID registerConstruct(Construct c){
    UUID u = UUID.randomUUID();
    assets.put(u, c);
    return u;
  }
  
  
  public Construct getConstruct(UUID u){
    Construct c = assets.get(u);
    if(c == null) throw new OpenGLException("no such construct registered");
    return assets.get(u);
  }
  
  public void releaseConstruct(UUID u){
    toRelease.add(u);
  }
  
  private void gpuUpload(UUID u){
    assets.get(u).bake();
    gpuLoaded.add(u);
  }
  
  
  public void drawConstruct(UUID idx, ShaderWrapper sw, Texture[] t){
    //derives the matrix from the loc/rot/scale attributes of the construct
    if(idx == null) throw new IllegalArgumentException("Error: idx is null!");
    if(!assets.containsKey(idx)) throw new IllegalArgumentException("No construct with idx="+idx+" registered");
    if(!gpuLoaded.contains(idx)) gpuUpload(idx);

    Matrix4f m = new Matrix4f();
    Construct c = assets.get(idx);
    
    m.translate(c.position);
    
    Matrix4f.mul(m, new Matrix4f().rotate(c.rotation.x, new Vector3f(1,0,0)), m);
    Matrix4f.mul(m, new Matrix4f().rotate(c.rotation.y, new Vector3f(0,1,0)), m);
    Matrix4f.mul(m, new Matrix4f().rotate(c.rotation.z, new Vector3f(0,0,1)), m);
    
    Matrix4f.scale(c.scale, m, m);
    
    workOrders.add(new ConstructEntry(idx, m, sw, t));
  }
  
  public void drawText(float x, float y, String t, Vector4f color){
    textOrders.add(new TextEntry(new Vector2f(x,y), color, t));
  }
  
  public void changeMask(boolean enabled){
    depthMask.put(workOrders.size(), enabled);
  }
  
  public void execute(){
    int err;

    
    Matrix4f mvp = new Matrix4f();
    long now = System.currentTimeMillis();

    int ord = 0;
    
    for(ConstructEntry ce : workOrders){

      if(depthMask.containsKey(ord)){
        glDepthMask(depthMask.get(ord));
      }
      
      //set all needed textures into their slots
      for(int i = 0; i<ce.tex.length; i++){
        glActiveTexture(GL_TEXTURE0+i); //texture0 to texture 32 are all behind each other. was too lazy to make a map
        glBindTexture(ce.texT[i], ce.tex[i]);
      }

      Matrix4f.mul(projMat, viewMat, mvp);
      Matrix4f.mul(mvp, ce.model, mvp);

      //assume that every shader has matrix uniforms
      ce.s.storeUniform("MVP", mvp);
      ce.s.storeUniform("M", ce.model);
      ce.s.storeUniform("V", viewMat);
      ce.s.storeUniform("P", projMat);

      assets.get(ce.pointer).draw(now);
      
      err = glGetError();
      if(err != GL_NO_ERROR) SimpleLogger.log(GLU.gluErrorString(err) + "("+err+")", -1, this.getClass(), "execute()");
      
      ord++;
    }
    
    glDepthMask(false);
    glDisable(GL_DEPTH_TEST);
    for(TextEntry te : textOrders){
      font.drawText(te.position.x, te.position.y, te.text, te.color);
    }
    glEnable(GL_DEPTH_TEST);
    glDepthMask(true);
    cleanUp(now);
  }
  
  private void cleanUp(long now){
    workOrders.clear();
    textOrders.clear();
    depthMask.clear();
    
    LinkedList<UUID> olds = new LinkedList<>();

    for(UUID u : gpuLoaded){
      long gap = now - assets.get(u).getLastRendered();
      if(gap > Ref.renderPause) olds.add(u);
    }

    for(UUID u: olds){
      assets.get(u).releaseVBO();
      gpuLoaded.remove(u);
    }
    
    for(UUID u : toRelease){
      assets.get(u).releaseVBO();
      gpuLoaded.remove(u);
      assets.remove(u);
    }
    toRelease.clear();
  }
  
  public void rotate(double d){
    viewMat.rotate((float)(d*0.003), new Vector3f(0,0,1));
  }
  
  public int getTextWidth(String t){
    return font.getTextWidth(t);
  }
  
  public int getTextHeight(String t){
    return font.getTextHeight(t);
  }
  
  public void deconstructAll(){
    gpuLoaded.clear();
    for(Construct c : assets.values()){
      c.releaseVBO();
    }
    assets.clear();
  }
  
  public boolean isLocked(){
    return wait;
  }
  
  public synchronized void takeLock(){
    wait = true;
  }
  
  public synchronized void releaseLock(){
    wait = false;
    notifyAll();
  }
  
  public synchronized void waitForRelease(){
    if(wait)
      try {
        wait();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
  }
}
