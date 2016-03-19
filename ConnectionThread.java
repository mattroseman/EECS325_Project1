import java.io.*;
import java.net.*;

public class ConnectionThread extends Thread {

    Socket socket;

    public ConnectionThread(Socket clientSocket) {

        super();
        socket = clientSocket;
    }

    public void run() {

        try {

            // InputStreamReader takes the byte stream and converts it to a char stream
            // the BufferedReader makes the char stream readable
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine;

            while ((inputLine = reader.readLine()) != null) {
                System.out.println(inputLine):
            }


        } catch (Exception e) {

        }

    }

}
