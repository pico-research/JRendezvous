package uk.ac.cam.cl.rendezvous;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("DELETE");

		// Make request...
		final int responseCode = connection.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			isOpen = false;
			if (outputStream != null) {
				outputStream.close();
				outputStream = null;
			} else if (inputStream != null) {
				inputStream.close();
				inputStream = null;
			}
		} else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
			throw new IOException("no such channel: " + url);
		} else {
			throw new IOException(String.format(
					"inavlid HTTP response code: %d %s",
					responseCode,
					connection.getResponseMessage()));
		}
	}

	HttpURLConnection attemptRead() throws IOException {
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

	HttpURLConnection attemptWrite(byte[] bytes) throws IOException {
		// Do request
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/octet-stream");
		connection.setRequestProperty("Content-Length", Integer.toString(bytes.length));

		connection.setDoOutput(true);
		OutputStream os = null;
		try {
			os = connection.getOutputStream();
			os.write(bytes);
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
}
