package messaging;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Must be implemented by any object, that wants to receive messages
 * 
 */

public interface Handler {

  public void handleMessage(Message m);
}
