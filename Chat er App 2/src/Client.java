import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Client {
    Socket socket;
    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;

    String username ="";
    String broadcastKeyword = "here";

    String[] clientNames;

    Client(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            listenForMessage();
            setUsername();
            sendMessage();
        } catch (IOException e) {
            e.printStackTrace();
            closeEverything();

        }
    }

    private void setUsername() throws IOException {

        boolean flag = true;
        Scanner scanner = new Scanner(System.in);
        while (flag) {
            dataOutputStream.writeUTF(Constants.GET_CLIENT_LIST);
            System.out.print("Enter Username: ");
            username = scanner.nextLine();
            if (username.trim().isBlank()) {
                System.out.println("Error! Username cannot be empty.");
                username ="";
            } else if (Arrays.stream(clientNames).anyMatch(username::equals)) {
                System.out.println("Error! User " + username + " already exists. Please try another Username.");
                username ="";
            } else if (username.equals(broadcastKeyword)) {
                System.out.println("Error! Username cannot be \"here\" ");
                username ="";
            } else {
                flag = false;
            }
        }
        dataOutputStream.writeUTF(Constants.SET_USERNAME);
        dataOutputStream.writeUTF(username);
        dataOutputStream.flush();
    }

    private void sendMessage() throws IOException {
        Scanner scanner = new Scanner(System.in);
        while (socket.isConnected()) {
            String input = scanner.nextLine();
            String[] inputs = input.split(" ", 2);
            if (inputs[0].charAt(0) == '@') {
                String msgReceiver = inputs[0].replace("@", "");
                String msgText = inputs[1];
                if (msgReceiver.equals(broadcastKeyword)) {
                    broadcastMessage(msgText);
                } else {
                    dataOutputStream.writeUTF(Constants.GET_CLIENT_LIST);
                    privateMessage(msgReceiver, msgText);
                }
            } else {
                System.out.println("Error! No user mentioned.");
            }

        }
    }

    private void privateMessage(String msgReceiver, String msgText) throws IOException {
        if (Arrays.stream(clientNames).anyMatch(msgReceiver::equals)) {
            dataOutputStream.writeUTF(Constants.PRIVATE_MESSAGE);
            dataOutputStream.writeUTF(msgReceiver);
            dataOutputStream.writeUTF(msgText);
            dataOutputStream.flush();
        } else {
            System.out.println("Error! User does not exist.");
        }
    }

    private void broadcastMessage(String msgText) throws IOException {
        dataOutputStream.writeUTF(Constants.BROADCAST_MESSAGE);
        dataOutputStream.writeUTF(msgText);
        dataOutputStream.flush();
    }

    private void listenForMessage() {
        Thread thread = new Thread(() -> {
            try {
                while (socket.isConnected()) {
                    String header = dataInputStream.readUTF();
                    if (header.equals(Constants.GET_CLIENT_LIST)) {
                        clientNames = dataInputStream.readUTF().split(", ");
                    } else if (header.equals(Constants.PRIVATE_MESSAGE)) {
                        String msgSender = dataInputStream.readUTF();
                        String msgText = dataInputStream.readUTF();
                        System.out.println("(WHISPER) " + msgSender + ": " + msgText);
                    } else if (header.equals(Constants.BROADCAST_MESSAGE)) {
                        String msgSender = dataInputStream.readUTF();
                        String msgText = dataInputStream.readUTF();
                        System.out.println("(SHOUT) " + msgSender + ": " + msgText);
                    } else if (header.equals(Constants.NEW_USER_JOINED)) {
                        dataOutputStream.writeUTF(Constants.GET_CLIENT_LIST);
                        System.out.println("\nAnnouncement: User " + dataInputStream.readUTF() + " has joined!");
                        if (username.trim().isBlank())
                            System.out.print("Enter Username: ");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                closeEverything();
            }

        });
        thread.start();

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
