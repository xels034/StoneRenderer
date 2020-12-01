package util;

/**
 * Very crude implementation of a logger. It's only purpose is to control globally, which logs get
 * printed. This was done because the java utility Logger had too mush overhead to be time efficient
 * ont this project. Each log has a abriatary level. Is the configured level equal or equal or greater
 * than that of the log, it gets printed.
 * 
 * Logs of level less than 0 are considered severe errors and are always printed.
 *
 */

public class SimpleLogger {
  public static boolean log = true;
  public static int level = Ref.LOG_LEVEL; // only things <= level get logged. values <0 are severe errors
  
  @SuppressWarnings("rawtypes")
  public static void log(Object msg, int lvl, java.lang.Class c, String methodName){
    if(log){
      if(lvl < 0){
        System.err.println("["+lvl+"]["+c.getName()+":"+methodName+"]: "+msg);
      }else if(lvl <= level){
        System.out.println("["+lvl+"]["+c.getName()+":"+methodName+"]: "+msg);
      }
    }
  }
}
