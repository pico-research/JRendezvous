package uk.ac.cam.cl.rendezvous;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;

/**
 * Provides a clean interface for obtaining new rendezvous channels from the rendezvous point.
 */
public class RendezvousClient {

	private static final String NEW_PATH = "new";
	private static final String CHANNEL_PATH = "channel/%s";
	
	private final URL root;

	public RendezvousClient(URL root) {
		// Ensure root URL has no trailing slash
		String r = root.toString();
		if (r.endsWith("/")) {
			try {
				this.root = new URL(r.substring(0, r.length() - 1));
			} catch (MalformedURLException e) {
				throw new RuntimeException(
						"Failed to remove trailing slash from root URL", e);
			}
		} else {
			this.root = root;
		}
	}
	
	public RendezvousClient(String root) throws MalformedURLException {
		this(new URL(root));
	}
	
	public RendezvousChannel newChannel() throws IOException {
		URL newUrl;
		try {
			newUrl = new URL(root, NEW_PATH);
		} catch (MalformedURLException e) {
			throw new RuntimeException(
					"Invalid new channel path", e);
		}
		HttpURLConnection connection = 
				(HttpURLConnection) newUrl.openConnection();
		
		// Make request
		final int code = connection.getResponseCode();
		
		if (code == 200) {
			byte[] b = IOUtils.toByteArray(
					connection.getInputStream(),
					connection.getContentLength());
			String name = new String(b);
			URL rendezvousUrl = new URL(root, String.format(CHANNEL_PATH, name));
			return new RendezvousChannel(rendezvousUrl);
		} else {
			throw new IOException(String.format(
					"New channel request failed: %d %s",
					code,
					connection.getResponseMessage()));
		}
	}
}
