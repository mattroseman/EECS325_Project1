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
            String[] tokens;
            String destFullURL;
            String destRelativeURL = "";
            String destHostname;
            int destPort;
            InetAddress ip;
            
            // Reads the first line of the request and pulls out the URL
            if ((inputLine = reader.readLine()) != null) {

                System.out.println("test1");

                // pulls out the URL part of HTTP request
                tokens = inputLine.split("\\s+");
                destFullURL = tokens[1];

                System.out.println("test2");

                // splits up the URL and port which has the format URL:port
                tokens = destFullURL.split(":");
                // get cut off the port part of the URL
                destFullURL = destFullURL.substring(0, destFullURL.lastIndexOf(tokens[tokens.length -1]) - 1);
                // get the last element of the split which will be the port
                destPort = (int)Integer.valueOf(tokens[tokens.length -1]);

                System.out.println("test3");

                // splits the URL into the hostname and the rest of the url
                // URL will have the format hostname/restofurl
                tokens = destFullURL.split("/");
                destHostname = tokens[0];
                // if there is a relativeURL part to the full URL
                if (tokens.length > 1) {
                    destRelativeURL = destFullURL.substring(tokens[0].length() - 1);
                }

                System.out.println("Full URL: " + destFullURL);
                System.out.println("Port: " + destPort);
                System.out.println("Hostname: " + destHostname);
                System.out.println("Relative URL: " + destRelativeURL);

                // do a DNS lookup of the hostname
                try {
                    ip = InetAddress.getByName(destHostname);
                } catch (UnknownHostException e) {
                    throw new UnknownHostException("the browser tried to connect to an unknown host");
                }

                System.out.println("IP: " + ip.getHostAddress());
            }

            while ((inputLine = reader.readLine()) != null) {
                System.out.println(inputLine);
            }


        } catch (Exception e) {

        }

    }

}
