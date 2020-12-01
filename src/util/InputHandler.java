package util;

/**
 * Works each update through any keyboard & mouse inputs that happened during the last update.
 * 
 */



import messaging.Message;
import messaging.Message.M_TYPE;
import messaging.Messenger;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class InputHandler{



  
  
  
  public void update(){
    while(Keyboard.next()){
      int ke = Keyboard.getEventKey();
      boolean st = Keyboard.getEventKeyState();

      Messenger.send(new Message(M_TYPE.RAW_KB, new Message.RW_KB_Param(ke, st)));
      
    }

    while(Mouse.next()){
      Messenger.send(new Message(M_TYPE.RAW_MS, new Message.RW_MS_Param(Mouse.getEventButton(),
                                        Mouse.getEventButtonState(),
                                        Mouse.getDX(),
                                        Mouse.getDY(),
                                        Mouse.getDWheel())));
    }
  }
  
}
