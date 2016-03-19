import java.io.*;
import java.net.*;

public class ConnectionThread extends Thread {

    Socket socket;

    String inputLine;
    String[] tokens;
    String destFullURL;
    String destRelativeURL = "";
    String destHostname;
    int destPort;

    URL destURL;
    InetAddress ip;
    URLConnection urlConn;

    public ConnectionThread(Socket clientSocket) {

        super();
        socket = clientSocket;
    }

    public void run() {

        try {

            // InputStreamReader takes the byte stream and converts it to a char stream
            // the BufferedReader makes the char stream readable
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            parseDestURL(reader.readLine());

            while ((inputLine = reader.readLine()) != null) {
                System.out.println(inputLine);
            }


        } catch (Exception e) {

        }

    }

    /** 
    *Takes the first line of a HTTP GET or POST request and parses the first line into the hostname, url, port 
    *@param HTTPReqFirstLine the first line of the HTTP request containing the destination URL
    *@return modifies the destFullURL, destRelativeURL, destHostname, destPort, and ip variables
    */
    private void parseDestURL(String HTTPReqFirstLine) throws UnknownHostException, MalformedURLException {

        // Reads the first line of the request and pulls out the URL
        if (HTTPReqFirstLine != null) {

            // pulls out the URL part of HTTP request
            tokens = HTTPReqFirstLine.split("\\s+");
            destFullURL = tokens[1];

            // splits up the URL and port which has the format URL:port
            tokens = destFullURL.split(":");
            // get cut off the port part of the URL
            destFullURL = destFullURL.substring(0, destFullURL.lastIndexOf(tokens[tokens.length -1]) - 1);
            // get the last element of the split which will be the port
            destPort = (int)Integer.valueOf(tokens[tokens.length -1]);

            // splits the URL into the hostname and the rest of the url
            // URL will have the format hostname/restofurl
            tokens = destFullURL.split("/");
            destHostname = tokens[0];
            // if there is a relativeURL part to the full URL
            if (tokens.length > 1) {
                destRelativeURL = destFullURL.substring(tokens[0].length() - 1);
            }

            // this includes the port number
            try {
                destURL = new URL(destFullURL);
            } catch (MalformedURLException e) {
                throw new MalformedURLException("Browser tried connecting to a nonexistent URL");
            }
            ip = getDNSLookup(destHostname);

            /* So at this point I'm not sure if I should use another socket and forward the request and then get the response on the same socket,
            of if I should use the URL.openConnection() built into java and do everything though that*/
        }
    }

    /** 
    *Performs a DNS lookup on a hostname string and returns the found ip address
    *@param hostname the hostname to lookup
    *@return the InetAddress object for the cooresponding ip address
    */
    private static InetAddress getDNSLookup(String hostname) throws UnknownHostException {

        InetAddress ipAddress;

         // do a DNS lookup of the hostname
        try {
            ipAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            throw new UnknownHostException("the browser tried to connect to an unknown host");
        }       

        return ipAddress;
    }

}
