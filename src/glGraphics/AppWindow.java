package glGraphics;

/**
 * AppWindow is the root of the whole application.
 * the main method sits here, and calls a new instance of AppWindow
 * 
 * AppWindow sets up (1) all the openGL stuff and (2) the whole game mechanic
 * rendering and updating every part of the game happens in AppWindows run() method
 *
 * [1]Rendering:
 * Rendering happens in multiple passes. First, the scene is drawn into an offscreen texture.
 * Then follow any number of post-process passes, until the image is finally drawn onto the screen.
 * By using offscreen textures, the use of different brightnesses than 0-1 is possible. This represents
 * effectively a very rudimentary HDR-pipeline. Managing the selection of the correct render targets
 * is the duty of the FrameBufferManager
 * 
 * When the correct FrameBuffer is selected, actual draw calls (instead of just fullscreen post-process shaders)
 * are allowed. Those draw calls are collected by the public static glGraphics glx. It is set up before any java
 * game logic objects are created, and so it is assured, that all access to it happen after its creation.
 * 
 * After collecting all draw calls, its list gets executed, preparing the FrameBufferManager for any post-process passes
 * Each of these passes accepts a shader. It is expected, that this shader draws a screen-aligned quad and reads from the current
 * offscreen image. All that has to be done is to set any variables that the shader needs. Shaders are collected in a map,
 * as to easily give them names in the code.
 * 
 * 
 * 
 * [2]Updating:
 * Each frame the InputHandler (for listening for input), the FSM and the Messenger are updated, to process another batch of data
 * The FSM manages to update the correct state, e.g. Menu or Game. The ingame physics, however, are not updated by this main loop
 * (see PhysicsManager for additional info)
 */

import util.InputHandler;
import util.Ref;
import util.SimpleLogger;
import util.Vector2i;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import messaging.Handler;
import messaging.Message;
import messaging.Message.RW_KB_Param;
import messaging.Message.RW_MS_Param;
import messaging.Messenger;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL32.*;

public class AppWindow implements Handler{

  public static void main(String[] args) {

    AppWindow w = new AppWindow();
    if(w.ready) w.run();
    else{
      SimpleLogger.log("AppWindow Setup failed :( can't start application.", -2, AppWindow.class, "main");
    }
    
    Messenger.unsubscribe(w);
  }
  
  //Render Stuff
  private HashMap<String, ShaderWrapper> shaders;
  private HashMap<String, Texture> textures;
  public static glGraphics glx;
  private FrameBufferManager fbm;
  private InputHandler ip;
  
  private boolean ready;
  private boolean closeRequest;
  
  private long stamp;
  private long frames;
  
  private int maxLod;
  
  private float exposure=1f;
  private float expStep=0.995f;
  private float expLimit= 1.3f;
  
  private int gq = 16;
  
  private float heat=600;
  private float heatLimit=600;
  private float heatStep=0.999f;
  
  private double rotSpeed=1;
  
  public AppWindow(){
    ready = false;
    closeRequest = false;
    
    try{ setupGL(); }
    catch (LWJGLException e) {e.printStackTrace();}
    
    if(ready) setupJAVA();
  }
  
  private void setupGL() throws LWJGLException{
    setupDisplay();
    setupShader();
    
    fbm = new FrameBufferManager();
    glx = new glGraphics();
    loadTextures(); //do this before TrueTypeFonts are generated.
    ready = true;
  }
  
  UUID cid,pid,cube;
  private void setupJAVA(){
    Messenger.subscribe(this, Message.M_TYPE.RAW_KB);
    Messenger.subscribe(this, Message.M_TYPE.RAW_MS);
    
    ip = new InputHandler();

    Construct c = new Construct("res/models/stone.obj");
    cid = glx.registerConstruct(c);
    
    c.scale = new Vector3f(1f, 1f, 1f);
    
    float x = 1e2f;
    
    c = new Construct("res/models/plane.obj");
    pid = glx.registerConstruct(c);
    c.scale = new Vector3f(x,x,1);
    c.position = new Vector3f(0,0,-1);
    
    c = new Construct("res/models/cube.obj");
    cube = glx.registerConstruct(c);

    c.scale = new Vector3f(10,10,10);
  }

  
  
  private void setupDisplay() throws LWJGLException{
    
    System.setProperty("org.lwjgl.util.Debug", "false");
    System.setProperty("org.lwjgl.util.NoChecks", "true");
    System.setProperty("org.lwjgl.opengl.Window.undecorated", "false");
    System.setProperty("org.lwjgl.input.Mouse.allowNegativeMouseCoords", "true");
    
    Display.setDisplayMode(new DisplayMode(Ref.xRes,Ref.yRes));
    Display.setResizable(true);
    
                          //bpp,a, z, stenc, multisample->unused(done in textures)
    PixelFormat pf = new PixelFormat(8, 8, 8, 8, 0);
    Display.create(pf, new ContextAttribs());
    
    glEnable(GL_MULTISAMPLE);
    glEnable(GL11.GL_TEXTURE_2D);
    glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
    glEnable(GL_DEPTH_TEST);
    //glDisable(GL_CULL_FACE);
    
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glBlendEquation(GL_FUNC_ADD);
    
    updateMaxLod();
    
    //glEnable(GL11.GL_SCISSOR_TEST);
    //glScissor(150,150,500,300);
  }
  
  private void updateMaxLod(){
    maxLod = (int)(Math.log(Math.min(Ref.xRes, Ref.yRes))/Math.log(2));
  }
  
  private void setupShader(){
    LinkedList<String> at = new LinkedList<>();
    at.add("vPos");
    at.add("vNor");
    at.add("vUV");
    
    shaders = new HashMap<>();
    
    String vsh,gsh,fshg,fshp,fshi,fshVB,fshCB,fshcm;
    
    vsh  = "res/shader/geo/standart.vsh";
    gsh  = "res/shader/geo/standart.gsh";
    fshg = "res/shader/geo/stone.fsh";
    fshi = "res/shader/geo/plane.fsh";
    fshp = "res/shader/post/msaa_to_tex.fsh";
    
    fshVB = "res/shader/post/vectorBlur.fsh";
    fshCB = "res/shader/post/combine.fsh";
    
    fshcm = "res/shader/bg/cubeMap.fsh";
    
    shaders.put("stone", new ShaderWrapper(vsh,gsh,fshg,at));
    shaders.put("plane", new ShaderWrapper(vsh,gsh,fshi,at));
    shaders.put("msaa_to_tex", new ShaderWrapper(vsh,gsh,fshp,at));
    
    shaders.put("vectorBlur", new ShaderWrapper(vsh,gsh,fshVB,at));
    shaders.put("combine", new ShaderWrapper(vsh,gsh,fshCB,at));
    
    shaders.put("skybox", new ShaderWrapper(vsh,gsh,fshcm,at));

    setupUniforms();
    
    //Ref.defaultSW = shaders.get("stone");
    //Ref.ppSW = shaders.get("msaa_to_tex");
  }
  
  private void setupUniforms(){
    ShaderWrapper blanks = shaders.get("stone");
    ShaderWrapper blankp = shaders.get("plane");
    ShaderWrapper blankmtt = shaders.get("msaa_to_tex");
    ShaderWrapper vb = shaders.get("vectorBlur");
    ShaderWrapper cb = shaders.get("combine");
    ShaderWrapper sb = shaders.get("skybox");

    Vector3f lightDir = new Vector3f(-1,-1,-1);
    
    blanks.storeUniform("colTex", 0);
    blanks.storeUniform("lavaTex", 1);
    blanks.storeUniform("worldTex", 2);
    blanks.storeUniform("heatTex", 3);
    blanks.storeUniform("lightDir",lightDir);
    blanks.storeUniform("heat", heat);
    
    blankmtt.storeUniform("fboTex", Ref.fboTargetSlot[0]);
    blankmtt.storeUniform("dim", new Vector2i(Ref.xRes, Ref.yRes));
    blankmtt.storeUniform("exposure", 1f);
    //blankmtt.storeUniform("samples", Ref.msaa); 
    
    blankp.storeUniform("heatTex", 0);
    blankp.storeUniform("sky", 1);
    blankp.storeUniform("lightDir",lightDir);
    blankp.storeUniform("heat", heat);
    
    vb.storeUniform("weights", gauss(32));
    vb.storeUniform("fboTex", Ref.fboTargetSlot[0]);
    vb.storeUniform("quality", 32);
    vb.storeUniform("texDim", new Vector2i(Ref.xRes, Ref.yRes));
    vb.storeUniform("maxLOD", maxLod);

    sb.storeUniform("tex", 0);
    
    cb.storeUniform("time", 0f);
  }
  
  private float[] gauss(int l){
    float[] g = new float[l];
    float fac;
    float s=0.35f;
    
    for(int i=0; i<l; i++){
      fac = (float)i/(float)l;
      g[i]=(float)Math.exp(-(fac*fac)/(2*s*s));
      //System.out.println(g[i]);
    }
    
    return g;
  }
  
  private void loadTextures(){
    textures = new HashMap<>();

    textures.put("color", new Texture2D("res/texture/stone.png"));
    textures.put("lava", new Texture2D("res/texture/stone.png"));
    textures.put("sky", new TextureCM("res/texture/skyBox/box"));
    textures.put("heat", new Texture1D("res/texture/heat.png"));
  }
  
  private void run(){
    while(!Display.isCloseRequested() && !closeRequest){
      update();
      glx.rotate(rotSpeed);
      
      if(Display.wasResized()){
        Ref.xRes=Display.getWidth();
        Ref.yRes=Display.getHeight();
        updateMaxLod();
        
        glViewport(0,0,Ref.xRes, Ref.yRes);

        glx.setupProjMat();
        fbm.releaseAll();
        fbm = new FrameBufferManager();
        shaders.get("msaa_to_tex").storeUniform("dim", new Vector2i(Ref.xRes, Ref.yRes));
        shaders.get("vectorBlur").storeUniform("maxLOD", maxLod);
        
      }
      
      render();
      Display.update();
      Display.sync(Ref.maxFPS);
      
    }
    
    cleanUp();
  }
  
  private void update(){
    updateFPSCounter();
    ip.update();
    Messenger.update();
  }
  
  private void updateFPSCounter(){
    if(System.currentTimeMillis() - stamp > 1000){
      Display.setTitle("Stone  ||  FPS: "+frames);
      frames = 0;
      stamp = System.currentTimeMillis();
    }
    frames++;
  }
  
  private void render(){

    glx.changeMask(false);
    glx.drawConstruct(cube, shaders.get("skybox"), new Texture[]{textures.get("sky")});
    glx.changeMask(true);
    glx.drawConstruct(pid, shaders.get("plane"),   new Texture[]{textures.get("heat"),
                                   textures.get("sky")});
    glx.drawConstruct(cid, shaders.get("stone"),   new Texture[]{textures.get("color"),
                                     textures.get("lava"),
                                     textures.get("sky"),
                                     textures.get("heat")});

    //draw geometry to target 0
    fbm.setSourceRender();
    //fbm.setCustomSourceRender(-1);
    //finalize draws all glx orders
    fbm.finalizeSource();
    

    //convert the Tex2DMS to a ordinary Tex2D, because of mipmap capability
    //also apply exposure at this point
    shaders.get("msaa_to_tex").storeUniform("exposure", exposure);
    fbm.doCustomPostPro(0, 1, shaders.get("msaa_to_tex"));


    //blur in x direction
    shaders.get("vectorBlur").storeUniform("strength", 1f);
    shaders.get("vectorBlur").storeUniform("threshold", 0.05f);
    shaders.get("vectorBlur").storeUniform("dir", new Vector2f(70,0));
    fbm.doCustomPostPro(1, 2, shaders.get("vectorBlur"));
    //blur in y direction
    shaders.get("vectorBlur").storeUniform("dir", new Vector2f(0,70));
    fbm.doCustomPostPro(2, 3, shaders.get("vectorBlur"));
    
    //combine blur pass and original onto screen
    shaders.get("combine").storeUniform("original", Ref.fboTargetSlot[1]);
    shaders.get("combine").storeUniform("glare", Ref.fboTargetSlot[3]);
    fbm.doCustomPostPro(1, 4, shaders.get("combine"));
    
    shaders.get("vectorBlur").storeUniform("strength", 1f);
    shaders.get("vectorBlur").storeUniform("threshold", 0.05f);
    shaders.get("vectorBlur").storeUniform("dir", new Vector2f(300,0));
    fbm.doCustomPostPro(1, 2, shaders.get("vectorBlur"));

    shaders.get("vectorBlur").storeUniform("dir", new Vector2f(0,300));
    fbm.doCustomPostPro(2, 3, shaders.get("vectorBlur"));

    //read out exposure for adjusting next frame
    updateExposure();
    updateHeat();
    
    shaders.get("combine").storeUniform("original", Ref.fboTargetSlot[3]);
    shaders.get("combine").storeUniform("glare", Ref.fboTargetSlot[4]);

    fbm.customFinalizeImage(3, shaders.get("combine"));
    
    Vector4f col = new Vector4f(1f,1f,1f,1f);
    if(exposure > 1) col = new Vector4f(0f,0f,0f,1f);
    
    glx.drawText(10, 10, String.format("[E,R] Glow Quality: %d", gq).replace(",","."), col);
    glx.drawText(10, 50, String.format("[D,F] Heat: %.3f (%.3f)", heat, heatLimit).replace(",","."), col);
    glx.drawText(10, 90, String.format("[C,V] Exposure: %.3f (%.3f)", exposure, expLimit).replace(",","."), col);

    glx.execute();

    int err = glGetError();
    if(err != GL_NO_ERROR) throw new OpenGLException(GLU.gluErrorString(err) + "("+err+")");
  }
  
  private void updateExposure(){
    //read out 1 pixel out of the offscreen buffer
    //the floatBuffer filled with that information is bigger than neccecary
    //however, the JVM crashes, if the buffer is too short. I found no downside to having a bigger buffer,
    //so its on the safe side
    FloatBuffer fb = (ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder())).asFloatBuffer();
    glActiveTexture(GL_TEXTURE11);
    glGetTexImage(GL11.GL_TEXTURE_2D, maxLod, GL_RGBA, GL_FLOAT, fb);
    fb = fb.asReadOnlyBuffer();
    float r = fb.get();
    float g = fb.get();
    float b = fb.get();
    Vector3f color = new Vector3f(r,g,b);
    
    if(color.length() > expLimit){
      exposure *= expStep;
    }else if(color.length() < expLimit*0.9){
      exposure /= expStep;
    }
    
    //used for time variable. as its static noise, it doesn't matter. but would need to be a proper time value in other cases
    
    long ts = System.currentTimeMillis();
    ts = (ts & 0x000000000FFFFFFFL); //take the least significant bits large enough to fit into a 32bit float. so we don't truncate the changing bits
    float f = ts; //lets just hope it does what we want;
    
    shaders.get("combine").storeUniform("time", f);
  }
  
  private void updateHeat(){
    
    if(heat > heatLimit) heat *= heatStep;
    else if(heat < heatLimit*0.99) heat /= heatStep;
    
    shaders.get("stone").storeUniform("heat", heat);
    shaders.get("plane").storeUniform("heat", heat);
  }
  
  private void cleanUp(){
    glx.waitForRelease();
    
    glx.deconstructAll();
    fbm.releaseAll();
    for(ShaderWrapper sw : shaders.values()){
      sw.releaseShader();
    }
    shaders.clear();
  }

  @Override
  public void handleMessage(Message m) {
    
    if(m.getMsgType() == Message.M_TYPE.RAW_MS){
      handleMouse(m);
    }else{
      handleKeyboard(m);
    }

  }
  
  boolean pressing;
  private void handleMouse(Message m){
    RW_MS_Param p = (RW_MS_Param)m.getParam();
    
    if(p.pressed && p.button==0){
      pressing = true;
    }else if(!p.pressed && p.button==0){
      pressing = false;
    }
    
    if(pressing){
      rotSpeed = p.dx/2f;
    }
  }
  
  private void handleKeyboard(Message m){
    RW_KB_Param p = (RW_KB_Param)m.getParam();
    
    if(!p.pressed){  
      if(p.key == Keyboard.KEY_E){
        if(gq > 1) gq /=2;
        
        shaders.get("vectorBlur").storeUniform("quality", gq);
      }
      if(p.key == Keyboard.KEY_R){
        gq *=2 ;
        
        shaders.get("vectorBlur").storeUniform("quality", gq);
      }
      
      if(p.key == Keyboard.KEY_F) heatLimit*=1.05f;
      if(p.key == Keyboard.KEY_D) heatLimit/=1.05f;
      if(heatLimit < 600) heatLimit = 600;
      
      if(p.key == Keyboard.KEY_V) expLimit*=1.2;
      if(p.key == Keyboard.KEY_C) expLimit/=1.2;
    }
  }
}