package uk.ac.cam.cl.rendezvous;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class RendezvousChannel {
	
	private URL url;

	public RendezvousChannel(URL url) {
		this.url = url;
	}
	
	public URL getUrl() {
		return url;
	}

	public InputStream getInputStream() {
		return new RendezvousInputStream(url);
	}
	
	public OutputStream getOutputStream() {
		return new RendezvousOutputStream(url);
	}
}
