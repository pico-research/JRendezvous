package uk.ac.cam.cl.rendezvous;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


final class RendezvousResponse {
	
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(byte[].class, new ByteArrayGsonSerializer())
			.disableHtmlEscaping()
			.create();
	
	// Response codes
	public static final int CLOSED = -1;
	public static final int OK = 0;
	public static final int TIMED_OUT = 2;
	
	public static RendezvousResponse fromConnection(HttpURLConnection connection)
			throws IOException {
		// Check response code
		if (connection.getResponseCode() != 200) {
			throw new IOException("response code was not 200 OK");
		}
		// Check content is non-empty
		if (connection.getContentLength() <= 0) {
			throw new IOException("response was empty response");
		}
		// TODO replace these checks with guava preconditions methods?
		
		// Read bytes
		byte[] responseBytes = new byte[connection.getContentLength()];
		IOUtils.readFully(connection.getInputStream(), responseBytes);
		
		return GSON.fromJson(new String(responseBytes, "UTF-8"), RendezvousResponse.class);
	}
	
	private RendezvousResponse() {
		super();
	}

	public int code;
	public String status;
	public String message;
	public byte[] data;
	
	@Override
	public String toString() {
		return GSON.toJson(this);
	}
}
