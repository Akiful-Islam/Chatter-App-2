import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    ServerSocket serverSocket;


    Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            listenForClients();
        } catch (IOException e) {
            e.printStackTrace();
            closeEverything();
        }
    }

    private void closeEverything() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForClients() throws IOException {
        while (!serverSocket.isClosed()) {
            Socket socket = serverSocket.accept();
            SocketInstance socketInstance = new SocketInstance(socket);
            Thread thread = new Thread(socketInstance);
            thread.start();
        }
    }

}
