package messaging;

/**
 * 
 * Messaging Hub. Any Object implementing the Handler interface may register itself
 * to this static class to receive custom-filtered messages. sending messages and 
 * registering any objects implementing the handler interface is possible from
 * anywhere and under any circumstances inside the application.
 * 
 * Each subscriber has to offer a list of message types he's interested in. Whenever a
 * message is sent, all handlers that have announced to listen to the type of the sent
 * message will receive it.
 * 
 * Delivering of messages happens once per frame, when the update() method is called from the
 * main loop in AppWindow. Special out-of-order delivery between frames can also be forced, but
 * should be used wisely
 * 
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import util.SimpleLogger;

public class Messenger {

  private static HashMap<Message.M_TYPE,LinkedList<Handler>> handleMap = new HashMap<>();
  private static LinkedList<Message> mQueue = new LinkedList<>();

  
  public static void subscribe(Handler h, Message.M_TYPE msgT){
    if(!handleMap.containsKey(msgT)){
      handleMap.put(msgT, new LinkedList<Handler>());
    }
    handleMap.get(msgT).add(h);
  }
  
  public static void subscribe(Handler h, Collection<Message.M_TYPE> msgT){
    for(Message.M_TYPE i : msgT){
      subscribe(h, i);
    }
  }
  
  public static void unsubscribe(Handler h, Message.M_TYPE msgT){
    if(handleMap.containsKey(msgT)){
      handleMap.get(msgT).remove(h);
    }
  }
  
  public static void unsubscribe(Handler h){
    SimpleLogger.log("Handler "+h+" unsubscribed completly", 0, Messenger.class, "unsubscribe");
    for(Message.M_TYPE msgT : handleMap.keySet()){
      handleMap.get(msgT).remove(h);
    }
  }
  
  @SuppressWarnings("unchecked")
  public static void fire(Message m){
    if(handleMap.containsKey(m.getMsgType())){
      LinkedList<Handler> localCopy = (LinkedList<Handler>)handleMap.get(m.getMsgType()).clone();
      for(Handler h : localCopy){
        h.handleMessage(m);
      }
    }else{
      SimpleLogger.log("Note: Message of type "+m.getMsgType()+" is requested by no-one!", 1, Messenger.class, "fire");
    }
  }
  
  public static void send(Message m){
    mQueue.add(m);
  }
  
  @SuppressWarnings("unchecked")
  public static void update(){
    LinkedList<Message> workList = (LinkedList<Message>)mQueue.clone();
    mQueue.clear();
    for(Message m : workList){
      fire(m);
    }
  }
}
