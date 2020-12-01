package glGraphics;

/**
 * 
 * Construct entries registered in a glGraphics object. Allows loading of wavefront .obj files.
 * However, ONLY triagulated objects for normals and UV coordinates are supported. crashes otherwise
 * 
 */

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;

import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import util.Ref;
import util.SimpleLogger;

import static org.lwjgl.opengl.GL11.*;


public class Construct {

  private float[] data;
  private int[] elems;
  
  public Vector3f scale;
  public Vector3f rotation;
  public Vector3f position;
  
  //used to determine, if an object can be discarded, because it hasn't been drawn for an amount of time (see Ref.java)
  private long lastRendered;
  
  //needed references of openGL calls, -10 if not yet generated
  private int vaPointer;
  private int vbPointer;
  private int ebPointer;
  private int ebLength;
  
  
  public Construct(String fn){
    lastRendered = System.currentTimeMillis();
    vaPointer = -10;
    vbPointer = -10;
    ebPointer = -10;
    ebLength = -10;

    fillArrays(fn);
    
    scale = new Vector3f(1,1,1);
    rotation = new Vector3f(0,0,0);
    position = new Vector3f(0,0,0);
  }
  
  
  public void draw(long now){
    if(vaPointer == -10) throw new IllegalStateException("No valid VertexArrayObject assigned.");
    
    glBindVertexArray(vaPointer);
    glDrawElements(GL_TRIANGLES, ebLength, GL_UNSIGNED_INT, 0);

    lastRendered = System.currentTimeMillis();
  }
  
  public void bake(){
    bakeFixed();
  }
  
  private void bakeFixed(){
    //glUseProgram(Ref.defaultSW.getShaderID());
    
    vaPointer = glGenVertexArrays();
    glBindVertexArray(vaPointer);
    
    vbPointer = glGenBuffers();
    glBindBuffer(GL_ARRAY_BUFFER, vbPointer);
    
    //fill the vertexBuffer and the elementBuffer
    
    FloatBuffer buff = (ByteBuffer.allocateDirect(data.length*(Float.SIZE/8)).order(ByteOrder.nativeOrder())).asFloatBuffer();
    buff.put(data);
    buff.flip();
    glBufferData(GL_ARRAY_BUFFER, buff, GL_STATIC_DRAW);
    
    ebPointer = glGenBuffers();
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebPointer);
    
    ebLength = elems.length;
    
    IntBuffer buffi = (ByteBuffer.allocateDirect(elems.length*(Float.SIZE/8)).order(ByteOrder.nativeOrder())).asIntBuffer();
    buffi.put(elems);
    buffi.flip();
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffi, GL_STATIC_DRAW);
    
    //define attribPositions in each vertexArray
    glVertexAttribPointer(Ref.shPosAttrib, 3, GL_FLOAT, false, 8*(Float.SIZE/8), 0L);
    glEnableVertexAttribArray(Ref.shPosAttrib);
    glVertexAttribPointer(Ref.shNorAttrib, 3, GL_FLOAT, false, 8*(Float.SIZE/8), 3*(Float.SIZE/8));  
    glEnableVertexAttribArray(Ref.shNorAttrib);
    glVertexAttribPointer(Ref.shTexAttrib, 2, GL_FLOAT, false, 8*(Float.SIZE/8), 6*(Float.SIZE/8));
    glEnableVertexAttribArray(Ref.shTexAttrib);
    
    int err = glGetError();
    if(err != 0) {
      SimpleLogger.log(GLU.gluErrorString(err) + "("+err+")", -1, this.getClass(), "bakeFixed");
    }
  }
  

  
  public long getLastRendered(){
    return lastRendered;
  }
  
  public void releaseVBO(){
    SimpleLogger.log("deleting construct", 10, Construct.class, "releaseVBO");
    glDeleteBuffers(vbPointer);
    glDeleteBuffers(ebPointer);
    glDeleteVertexArrays(vaPointer);
  }
  
  private void fillArrays(String fn){
    String line;
    String[] lineElems;
    String[] faceLine;
    
    float v1,v2,v3;
    int i1,i2,i3;
    
    try (BufferedReader br = new BufferedReader(new FileReader(fn));){
      
      
      line = br.readLine();
      LinkedList<Vector3f> verts = new LinkedList<>();
      LinkedList<Vector3f> norms = new LinkedList<>();
      LinkedList<Vector2f> texts = new LinkedList<>();
      
      LinkedList<Float> dataList = new LinkedList<>();
      
      while(line != null){
        lineElems = line.split(" ");
        if(lineElems[0].equals("v")){
          v1 = Float.parseFloat(lineElems[1]);
          v2 = Float.parseFloat(lineElems[2]);
          v3 = Float.parseFloat(lineElems[3]);
          verts.add(new Vector3f(v1,v2,v3));
        }else if(lineElems[0].equals("vn")){
          v1 = Float.parseFloat(lineElems[1]);
          v2 = Float.parseFloat(lineElems[2]);
          v3 = Float.parseFloat(lineElems[3]);
          norms.add(new Vector3f(v1,v2,v3));
        }else if(lineElems[0].equals("vt")){
          v1 = Float.parseFloat(lineElems[1]);
          v2 = Float.parseFloat(lineElems[2]);
          texts.add(new Vector2f(v1,v2));
        }else if(lineElems[0].equals("f")){
          for(int i=1;i<4;i++){
            faceLine = lineElems[i].split("/");
            i1 = Integer.parseInt(faceLine[0]);
            i2 = Integer.parseInt(faceLine[2]); //vn & vt are flipped
            i3 = Integer.parseInt(faceLine[1]);
            
            dataList.add(verts.get(i1-1).x);
            dataList.add(verts.get(i1-1).y);
            dataList.add(verts.get(i1-1).z);
            
            dataList.add(norms.get(i2-1).x);
            dataList.add(norms.get(i2-1).y);
            dataList.add(norms.get(i2-1).z);
            
            dataList.add(texts.get(i3-1).x);
            dataList.add(texts.get(i3-1).y);
          }
        }
        
        line = br.readLine();
      }
      
      //because array size is unknown before reaching EOF
      data = new float[dataList.size()];
      int i=0;
      for(Float f : dataList){
        data[i] = f;
        i++;
      }
      
      //each vertex has 8 values, so there are data.length/8 elements
      elems = new int[data.length/8];
      for(i=0; i<elems.length; i++){
        elems[i] = i;
      }
      
    } catch (IOException x){
      System.err.println("bullshit file not found or something: " + x);
    }
  }
}
