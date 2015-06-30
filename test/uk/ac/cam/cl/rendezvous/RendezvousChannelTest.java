package uk.ac.cam.cl.rendezvous;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

public class RendezvousChannelTest {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	private RendezvousChannel channel;

	@Before
	public void setUp() throws Exception {
		final RendezvousClient client = new RendezvousClient("http://127.0.0.1:8082");
		channel = client.newChannel();
	}

	@Test
	public void writeReadHelloWorld() throws IOException {
		final BufferedOutputStream os = new BufferedOutputStream(channel.getOutputStream());
		final InputStream is = channel.getInputStream();
		
		final String helloWorld = "hello world";
		final byte[] helloWorldBytes = helloWorld.getBytes(UTF8);
		
		// Write in another thread
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					os.write(helloWorldBytes);
					os.flush();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}).start();
		
		// Read
		final byte[] bytesRead = new byte[helloWorldBytes.length];
		is.read(bytesRead, 0, bytesRead.length);
		final String stringRead = new String(bytesRead, UTF8);
		
		// Test
		assertArrayEquals(helloWorldBytes, bytesRead);
		assertEquals(helloWorld, stringRead);
	}
	
	@Test
	public void echoHelloWorld() throws Exception {
		final DataOutputStream os = new DataOutputStream(new BufferedOutputStream(channel.getOutputStream()));
		final DataInputStream is = new DataInputStream(channel.getInputStream());
		
		final RendezvousChannel peer = new RendezvousChannel(channel.getUrl());
		final DataOutputStream peerOs = new DataOutputStream(new BufferedOutputStream(peer.getOutputStream()));
		final DataInputStream peerIs = new DataInputStream(peer.getInputStream());
		final byte[] peerBuffer = new byte[1024];
		final boolean peerStop = false;
		
		
		// Peer echoes anything back to the rendezvous point in another thread
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while(!peerStop) {
						final int length = peerIs.readInt();
						peerIs.read(peerBuffer, 0, length);
						peerOs.write(peerBuffer, 0, length);
						peerOs.flush();
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}).start();
		
		// See if something is echoed correctly...
		final String helloWorld = "hello world";
		final byte[] helloWorldBytes = helloWorld.getBytes(UTF8);
		final int length = helloWorldBytes.length;
		
		// Write
		os.writeInt(length);
		os.write(helloWorldBytes, 0, length);
		os.flush();
		
		// Read
		final byte[] bytesRead = new byte[length];
		is.read(bytesRead, 0, length);
		final String stringRead = new String(bytesRead, UTF8);
		
		// Test
		assertArrayEquals(helloWorldBytes, bytesRead);
		assertEquals(helloWorld, stringRead);
	}
}
