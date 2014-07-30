package uk.ac.cam.cl.rendezvous;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

public class RendezvousOutputStream extends OutputStream implements InterruptibleStream {

	private final RendezvousChannel rendezvousChannel;
	private volatile boolean isOpen = true;

	public RendezvousOutputStream(RendezvousChannel rendezvousChannel) {
		this.rendezvousChannel = rendezvousChannel;
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		// Get the desired bytes
		byte[] bytes;
		if (off > 0 || len < b.length) {
			bytes = new byte[len-off];
			System.arraycopy(b, off, bytes, 0, len);
		} else {
			bytes = b;
		}
		
		while (isOpen) {
			HttpURLConnection connection = rendezvousChannel.attemptWrite(bytes); // throws
																				  // IOException if
																				  // not 200 OK
			RendezvousResponse response = RendezvousResponse.fromConnection(connection);
			if (response.code == 0) {
				// Write was successful, finish
				return;
			} else if (response.code == RendezvousResponse.TIMED_OUT) {
				// Timed out, try again, unless closed by another thread.
				// Might well benefit from implementing a java.nio Channel
				// subclasses instead of the input and output streams
			} else {
				// An error occured (rendezvous layer i.e. not HTTP 404)
				throw new IOException(String.format(
						"inavlid rendezvous response code: %d %s",
						response.code,
						response.message));
			}
		}
		// Close the Rendezvous Point
		throw new IOException("write to rendezvous which has been closed");
	}
	
	@Override
	public void write(int b) throws IOException {
		write(new byte[] { (byte) b}, 0, 1);		
	}
	
	/* public static void main(String[] args) throws Exception { OutputStream os = new
	 * RendezvousOutputStream( new URL(
	 * "http://127.0.0.1:8080/channel/04c46c75a41d477ca9b6b67789b0d648")); DataOutputStream dos =
	 * new DataOutputStream(new BufferedOutputStream(os)); // DataOutputStream dos = new
	 * DataOutputStream(os);
	 * 
	 * byte[] b1 = new String("hello, world\n").getBytes(); byte[] b2 = new
	 * String("second message here\n").getBytes(); dos.writeInt(b1.length); dos.write(b1);
	 * dos.writeInt(b2.length); dos.write(b2); dos.flush(); dos.close(); } */

	/**
	 * Closes the output stream, any writes that have not been acknowledged will be cancelled when
	 * they time out.
	 */
	@Override
	public void close() throws IOException {
		isOpen = false;
		super.close();
	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}
}
