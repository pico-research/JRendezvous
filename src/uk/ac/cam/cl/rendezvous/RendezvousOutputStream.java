package uk.ac.cam.cl.rendezvous;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import uk.ac.cam.cl.rendezvous.org.apache.commons.codec.binary.Base64;

public class RendezvousOutputStream extends OutputStream {

	private final URL rendezvousUrl;
	
	public RendezvousOutputStream(URL rendezvousUrl) {
		this.rendezvousUrl = rendezvousUrl;
	}
	
	private HttpURLConnection attemptWrite(byte[] bytes) throws IOException {
		// Form request body
		final String b64bytes = Base64.encodeBase64String(bytes);
		final String request = "data=" + b64bytes;
		final byte[] requestBytes = request.getBytes("UTF-8");
		
		// Do request
		final HttpURLConnection connection = (HttpURLConnection) rendezvousUrl.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
		connection.setRequestProperty("Content-Length", Integer.toString(requestBytes.length));
		
		connection.setDoOutput(true);
		OutputStream os = null;
        try {
            os = connection.getOutputStream();
            os.write(requestBytes);
            os.flush();
        } finally {
            if (os != null) {
                os.close();
            }
        }
        
        // Make request...
        final int responseCode = connection.getResponseCode();
        
        if (responseCode == HttpURLConnection.HTTP_OK) {
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
		
		while (true) {
			HttpURLConnection connection = attemptWrite(bytes); // throws IOException if not 200 OK
			RendezvousResponse response = RendezvousResponse.fromConnection(connection);
			if (response.code == 0) {
				// Write was successful, finish
				break;
			} else if (response.code == RendezvousResponse.TIMED_OUT) {
				// Timed out
				// Not sure what to do here, but for now throw an IOException and let the caller 
				// deal with it... Might well benefit from implementing a java.nio Channel
				// subclasses instead of the input and output streams
				throw new IOException("write to rendezvous point timed out");
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
	public void write(int b) throws IOException {
		write(new byte[] { (byte) b}, 0, 1);		
	}
	
	public static void main(String[] args) throws Exception {
		OutputStream os = new RendezvousOutputStream(
				new URL("http://rendezvous.pico.cl.cam.ac.uk:8080/channel/test"));
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
		
		byte[] b1 = new String("hello, world\n").getBytes();
		byte[] b2 = new String("second message here\n").getBytes();
		dos.writeInt(b1.length);
		dos.write(b1);
		dos.writeInt(b2.length);
		dos.write(b2);
		dos.flush();
		dos.close();
	}
}
