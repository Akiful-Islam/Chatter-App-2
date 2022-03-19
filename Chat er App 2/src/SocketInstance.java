import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class SocketInstance implements Runnable {
    Socket socket;
    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;
    String username = "";
    public static ArrayList<SocketInstance> socketInstanceArrayList = new ArrayList<>();

    SocketInstance(Socket socket) {
        this.socket = socket;
        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            socketInstanceArrayList.add(this);
        } catch (IOException e) {
            e.printStackTrace();
            closeEverything();
        }
    }

    @Override
    public void run() {
        try {
            while (socket.isConnected()) {
                String header = dataInputStream.readUTF();
                if (header.equals(Constants.GET_CLIENT_LIST)) {
                    getClientList();
                } else if (header.equals(Constants.BROADCAST_MESSAGE)) {
                    broadcastMessage();
                } else if (header.equals(Constants.PRIVATE_MESSAGE)) {
                    privateMessage();
                } else if (header.equals(Constants.SET_USERNAME)) {
                    setUsername();
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
            closeEverything();
        }
    }

    private void setUsername() throws IOException {
        username = dataInputStream.readUTF();
        System.out.println("New Client Connected! Welcome \""+username+"\".");
        broadcastNewUser();
    }

    private void privateMessage() throws IOException {
        String userToSend = dataInputStream.readUTF();
        String message = dataInputStream.readUTF();
        for (SocketInstance socketInstance : socketInstanceArrayList) {
            if (socketInstance.username.equals(userToSend) && socketInstance != this) {
                socketInstance.dataOutputStream.writeUTF(Constants.PRIVATE_MESSAGE);
                socketInstance.dataOutputStream.writeUTF(username);
                socketInstance.dataOutputStream.writeUTF(message);
            }
        }
    }

    private void broadcastMessage() throws IOException {
        String message = dataInputStream.readUTF();
        for (SocketInstance socketInstance : socketInstanceArrayList) {
            if (socketInstance != this) {
                socketInstance.dataOutputStream.writeUTF(Constants.BROADCAST_MESSAGE);
                socketInstance.dataOutputStream.writeUTF(username);
                socketInstance.dataOutputStream.writeUTF(message);
            }
        }
    }

    private void getClientList() throws IOException {
        ArrayList<String> clientNames = new ArrayList<>();
        for (SocketInstance socketInstance : socketInstanceArrayList) {
            if (!socketInstance.equals(this)){
                clientNames.add(socketInstance.username);
            }
        }
        String clientList = clientNames.stream().collect(Collectors.joining(", "));
        dataOutputStream.writeUTF(Constants.GET_CLIENT_LIST);
        dataOutputStream.writeUTF(clientList);
    }
    private void broadcastNewUser() throws IOException{
        for (SocketInstance socketInstance : socketInstanceArrayList) {
            if (!socketInstance.equals(this)){
                socketInstance.dataOutputStream.writeUTF(Constants.NEW_USER_JOINED);
                socketInstance.dataOutputStream.writeUTF(username);
            }
        }
    }

    private void closeEverything() {

        try {
            if (socket != null) {
                socket.close();
            }
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }

            socketInstanceArrayList.remove(this);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
