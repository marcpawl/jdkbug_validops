package test;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;

import org.junit.*;

public class CloseTest {
	private int portNumber;
	private final Semaphore serverReady = new Semaphore(0);
	private final Semaphore closeServer = new Semaphore(0);

	public class Server implements Runnable {
		public void run() {
			try {
				try (ServerSocket serverSocket = new ServerSocket(0)) {
					portNumber = serverSocket.getLocalPort();
					serverReady.release();
					try (Socket clientSocket = serverSocket.accept()) {
						closeServer.acquire();
					}
				}
			} catch (Throwable t) {
				fail(t.getLocalizedMessage());
				System.exit(1);
			}
		}
	}

	/** Verify that socket is closed if both input and output
         *  are shutdown.
	 * @throws Exception
	 */
        @Test
	public  void testSocketClosedOnShutdown() throws Exception {
		Server server = new Server();
		Thread serverThread = new Thread(server);
		serverThread.start();
		serverReady.acquire();

		try (SocketChannel client = SocketChannel.open()) {
			client.connect(new InetSocketAddress("localhost", portNumber));
			client.shutdownInput();
			client.shutdownOutput();
                        // Since there are no more operations available
                        // the socket should be closed.
                        boolean closed = client.socket().isClosed();
			// Verify
			assertTrue("socket should be closed.", closed);
		}

		// Cleanup
		serverThread.join(60000);
	}
}
