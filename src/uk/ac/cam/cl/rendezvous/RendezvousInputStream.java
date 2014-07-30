package uk.ac.cam.cl.rendezvous;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * An InputStream for accessing data written to a Rendezvous point.
 * 
 * @author Max Spencer <ms955@cl.cam.ac.uk>
 *
 */
public class RendezvousInputStream extends InputStream implements InterruptibleStream {
	
	private final RendezvousChannel rendezvousChannel;
	private byte[] buffer = new byte[0];
	private int r = 0;
	private volatile boolean isOpen = true;
	
	/**
	 * Construct a <code>RendezvousInputStream</code> with the rendezvous point's URL.
	 * 
	 * @param rendezvousChannel url of the rendezvous point.
	 */
	public RendezvousInputStream(RendezvousChannel rendezvousChannel) {
		this.rendezvousChannel = rendezvousChannel;
	}
	
	private int readFromRendezvous() throws IOException {
		while (isOpen) {
			HttpURLConnection connection = rendezvousChannel.attemptRead(); // throws IOException if
																			// not 200 OK
			RendezvousResponse response = RendezvousResponse.fromConnection(connection);
			if (response.code == 0) {
				// Read was successful, copy data into buffer and return length
				int len = response.data.length;
				buffer = new byte[len];
				System.arraycopy(response.data, 0, buffer, 0, len);
				r = 0;
				return len;
			} else if (response.code == RendezvousResponse.TIMED_OUT) {
				// Timed out, try again, unless closed by another thread.
				// Might well benefit from implementing a java.nio Channel
				// subclasses instead of the input and output streams
			} else if (response.code == RendezvousResponse.CLOSED) {
				// Channel was closed
				return -1;
			} else {
				// An error occurred (rendezvous layer i.e. not HTTP 404)
				throw new IOException(String.format(
						"inavlid rendezvous response code: %d %s",
						response.code,
						response.message));
			}
		}
		// Was closed, tell rendezvous point to close.

		return -1;
	}

	@Override
	public int read() throws IOException {
		if (r >= buffer.length) {
			if (readFromRendezvous() < 0) {
				return -1;
			}
		}
		int mask = 0xff;
		return mask & (buffer[r++]);
	}
	
	@Override 
	public int read(byte[] b, final int off, final int len) throws IOException {
		int read = 0;
		
		while (read < (len - off)) {
			// If if we need to retrieve more data
			if (r >= buffer.length) {
				if (readFromRendezvous() < 0) {
					// Channel closed
					return read;
				}
			}
			
			int amt = Math.min(len - (off + read), buffer.length);
			System.arraycopy(buffer, r, b, off + read, amt);
			read += amt;
			r += amt;
		}
		
		return read;
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}
	
	@Override
	public int available() throws IOException {
		return buffer.length - r;
	}
	
	/* public static void main(String[] args) throws Exception { InputStream is = new
	 * RendezvousInputStream(rendezvousChannel); //new
	 * URL("http://127.0.0.1:8080/channel/04c46c75a41d477ca9b6b67789b0d648")); DataInputStream dis =
	 * new DataInputStream(is);
	 * 
	 * byte[] b1 = IOUtils.toByteArray(dis, dis.readInt()); byte[] b2 = IOUtils.toByteArray(dis,
	 * dis.readInt()); System.out.println(new String(b1)); System.out.println(new String(b2)); } */

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	/**
	 * Closes the input stream, any reads will be closed when they time out.
	 */
	@Override
	public void close() throws IOException {
		super.close();
		isOpen = false;
	}

}
