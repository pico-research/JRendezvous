package uk.ac.cam.cl.rendezvous;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;


final class StatusResponse {
	// Response codes
	public static final int CLOSED = -1;
	public static final int OK = 0;
	public static final int TIMED_OUT = 2;
	
	public static StatusResponse fromConnection(HttpURLConnection connection)
			throws IOException {
		// Check response code
		if (connection.getResponseCode() != 200) {
			throw new IOException("response code was not 200 OK");
		}
		// Check content is non-empty
		if (connection.getContentLength() <= 0) {
			throw new IOException("response was empty response");
		}
		// Check content is JSON-encoded
		if (!connection.getContentType().equals("application/json")) {
			throw new IOException("status response was not JSON-encoded");
		}
		// TODO replace these checks with guava preconditions methods?
		
		// Read bytes
		byte[] responseBytes = new byte[connection.getContentLength()];
		IOUtils.readFully(connection.getInputStream(), responseBytes);
		
		final String responseString = new String(responseBytes, "UTF-8");
		return new Gson().fromJson(responseString, StatusResponse.class);
	}
	
	private StatusResponse() {
		super();
	}

	public int code;
	public String status;
	public String message;
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
