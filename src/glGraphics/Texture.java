package glGraphics;

//Used for different Texture Implementations such as Texture1D, Texture2D, etc. Stores some useful information along the openGL texture ID 

import util.Vector2i;

public abstract class Texture {

  protected int type;
  protected int texID;
  protected Vector2i dimension;
  
  public int getID(){
    return texID;
  }
  
  public Vector2i getSize(){
    return new Vector2i(dimension.x, dimension.y);
  }
  
  public int getType(){
    return type;
  }
}
