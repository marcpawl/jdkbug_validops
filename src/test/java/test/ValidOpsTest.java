package test;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;

import org.junit.*;

public class ValidOpsTest {
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

	/** Verify that an operation is not present in valid ops after
	 *  getting a connection, and shutting down input and output.
	 * @param operationToNotSee Operation that should not be present
	 * in the result to {@link SocketChannel#validOps()}.
	 * @throws Exception
	 */
	private void opNotPresentTest(int operationToNotSee) throws Exception {
		Server server = new Server();
		Thread serverThread = new Thread(server);
		serverThread.start();
		serverReady.acquire();

		try (SocketChannel client = SocketChannel.open()) {
			client.connect(new InetSocketAddress("localhost", portNumber));
			client.shutdownInput();
			client.shutdownOutput();
			int ops = client.validOps();
			closeServer.release();
			
			// Verify
			assertEquals("ops is " + ops + " and should not have " + operationToNotSee, 
					0, ops & operationToNotSee );
		}

		// Cleanup
		serverThread.join(60000);
	}

	/** After input has been shutdown read is no longer a valid operation. */
	@Test
	public void testValidOpsDoesNotIncludeRead() throws Exception {
		opNotPresentTest(SelectionKey.OP_READ);
	}

	/** After output has been shutdown write is no longer a valid operation. */
	@Test
	public void testValidOpsDoesNotIncludeWrite() throws Exception {
		opNotPresentTest(SelectionKey.OP_WRITE);
	}
	
	/** Since we are connected we cannot connect again. */
	@Test
	public void testValidOpsDoesNotIncludeConnect() throws Exception {
		opNotPresentTest(SelectionKey.OP_CONNECT);
	}

	/** Since we are not a server we cannot accept connections. */
	@Test
	public void testValidOpsDoesNotIncludeAccept() throws Exception {
		opNotPresentTest(SelectionKey.OP_ACCEPT);
	}

}
