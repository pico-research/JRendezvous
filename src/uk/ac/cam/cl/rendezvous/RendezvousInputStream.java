package uk.ac.cam.cl.rendezvous;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;

/**
 * An InputStream for accessing data written to a Rendezvous point.
 * 
 * @author Max Spencer <ms955@cl.cam.ac.uk>
 *
 */
public class RendezvousInputStream extends InputStream {
	
	private final URL rendezvousUrl;
	private byte[] buffer = new byte[0];
	private int r = 0;
	
	/**
	 * Construct a <code>RendezvousInputStream</code> with the rendezvous
	 * point's URL.
	 * 
	 * @param rendezvousUrl url of the rendezvous point.
	 */
	public RendezvousInputStream(URL rendezvousUrl) {
		this.rendezvousUrl = rendezvousUrl;
	}
	
	private HttpURLConnection attemptRead() throws IOException {
		HttpURLConnection connection = (HttpURLConnection) rendezvousUrl.openConnection();
		
		// Make request...
		final int responseCode = connection.getResponseCode();
		
		if(responseCode == HttpURLConnection.HTTP_OK) {
			return connection;
		} else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
        	throw new IOException("no such channel: " + rendezvousUrl);
        } else {
        	throw new IOException(String.format(
					"inavlid HTTP response code: %d %s",
					responseCode,
					connection.getResponseMessage()));
        }
	}
	
	private int readFromRendezvous() throws IOException {
		while (true) {
			HttpURLConnection connection = attemptRead(); // throws IOException if not 200 OK
			RendezvousResponse response = RendezvousResponse.fromConnection(connection);
			if (response.code == 0) {
				// Read was successful, copy data into buffer and return length
				int len = response.data.length;
				buffer = new byte[len];
				System.arraycopy(response.data, 0, buffer, 0, len);
				r = 0;
				return len;
			} else if (response.code == RendezvousResponse.TIMED_OUT) {
				// Timed out
				// Not sure what to do here, but for now throw an IOException and let the caller 
				// deal with it... Might well benefit from implementing a java.nio Channel
				// subclasses instead of the input and output streams
				throw new IOException("read from rendezvous point timed out");
			} else if (response.code == RendezvousResponse.CLOSED) {
				// Channel was closed
				return -1;
			} else {
				// An error occured (rendezvous layer i.e. not HTTP 404)
				throw new IOException(String.format(
						"inavlid rendezvous response code: %d %s",
						response.code,
						response.message));
			}
		}
	}

	@Override
	public int read() throws IOException {
		if (r >= buffer.length) {
			if (readFromRendezvous() < 0) {
				return -1;
			}
		}
		int mask = 0xff;
		return mask & ((int) buffer[r++]);
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
	
	public static void main(String[] args) throws Exception {
		InputStream is = new RendezvousInputStream(
				new URL("http://rendezvous.pico.cl.cam.ac.uk:8080/channel/test"));
		DataInputStream dis = new DataInputStream(is);
		
		byte[] b1 = IOUtils.toByteArray(dis, dis.readInt());
		byte[] b2 = IOUtils.toByteArray(dis, dis.readInt());
		System.out.println(new String(b1));
		System.out.println(new String(b2));
	}
}
