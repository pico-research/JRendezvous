package uk.ac.cam.cl.rendezvous;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import uk.ac.cam.cl.rendezvous.org.apache.commons.codec.binary.Base64;

public class RendezvousChannel {
	
	private final URL url;
	private volatile boolean isOpen = true;

	private RendezvousInputStream inputStream = null;
	private RendezvousOutputStream outputStream = null;

	public RendezvousChannel(URL url) {
		this.url = url;
	}
	
	public URL getUrl() {
		return url;
	}

	synchronized public InputStream getInputStream() {
		if (isOpen && inputStream == null) {
			inputStream = new RendezvousInputStream(this);
		}
		return inputStream;
	}
	
	synchronized public OutputStream getOutputStream() {
		if (isOpen && outputStream == null) {
			outputStream = new RendezvousOutputStream(this);
		}
		return outputStream;
	}

	public void close() throws IOException {
		isOpen = false;

		attemptWriteRequestString("close=true");

		if (outputStream != null) {
			outputStream.close();
			outputStream = null;
		} else if (inputStream != null) {
			inputStream.close();
			inputStream = null;
		}
		
	}

	private HttpURLConnection attemptWriteRequestString(String requestString) throws IOException {
		final byte[] requestBytes = requestString.getBytes("UTF-8");
		// Do request
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded; charset=utf-8");
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
			throw new IOException("no such channel: " + url);
		} else {
			throw new IOException(String.format(
					"inavlid HTTP response code: %d %s",
					responseCode,
					connection.getResponseMessage()));
		}
	}

	synchronized HttpURLConnection attemptRead() throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		// Make request...
		final int responseCode = connection.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			return connection;
		} else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
			throw new IOException("no such channel: " + url);
		} else {
			throw new IOException(String.format(
					"inavlid HTTP response code: %d %s",
					responseCode,
					connection.getResponseMessage()));
		}
	}

	synchronized HttpURLConnection attemptWrite(byte[] bytes) throws IOException {
		// Form request body
		final String b64bytes = Base64.encodeBase64String(bytes);
		return attemptWriteRequestString("data=" + b64bytes);
	}
}
