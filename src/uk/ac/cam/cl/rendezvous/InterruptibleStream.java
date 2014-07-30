package uk.ac.cam.cl.rendezvous;

/**
 * Streams marked with this should be thread safe, and their close method should work as
 * java.nio.channels.InterruptibleChannel
 * 
 * @author cw471
 * 
 */
public interface InterruptibleStream {
	public boolean isOpen();
}
