package messaging;

/**
 * Messages consist of at least a type. This type then dictates the class
 * of which the parameter (if present) is. Each Handler has to make sure
 * as to know how to cast the parameter Object depending on the message type.
 * Some message Type have dedicated parameter classes, some don't, if it isn't
 * necessary.
 *
 */

public class Message {
  public enum M_TYPE{
    CHANGE_STATE,
    RAW_KB,
    RAW_MS,
    CONTROL_CMD,
  }
  

  
  public static class RW_KB_Param{
    public int key;
    public boolean pressed;
    
    public RW_KB_Param(int k, boolean p){
      key=k;
      pressed=p;
    }
  }
  
  public static class RW_MS_Param{
    public int button;
    public boolean pressed;
    
    public int dx;
    public int dy;
    public int dw;
    
    public RW_MS_Param(int b, boolean p, int x, int y, int w){
      button=b;
      pressed=p;
      dx=x;
      dy=y;
      dw=w;
    }
  }

  private M_TYPE msgType;
  private Object params;
  
  public Message(M_TYPE m){
    msgType=m;
  }
  
  public Message(M_TYPE m, Object o){
    msgType = m;
    params = o;
  }
  
  public M_TYPE getMsgType(){
    return msgType;
  }
  
  public Object getParam(){
    return params;
  }
  
  public void setParams(Object o){
    params = o;
  }
  
  public Message copy(){
    return new Message(msgType, params);
  }
}
