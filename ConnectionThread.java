import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.regex.Pattern;

public class ConnectionThread extends Thread {

    private Socket socket;
    
    private InputStream clientis;
    private BufferedReader clientReader;
    private InputStream serveris;
    private OutputStream clientos;
    private OutputStream serveros;

    private String inputLine; // first line read from the HTTP Request
    private String shortInputLine; // first line read with only the relative URL
    private String[] tokens; // used multiple times when parsing the first line
    private String destFullURL; // the full URL for the destination address
    private String destRelativeURL = ""; // just the relative part of the destination URL
    private String destHostname; // just the hostname part of the destination URL
    private int destPort; // the port of the destination (default is 80)

    private URL destURL;
    private InetAddress destIp;
    private URLConnection urlConn;

    private final int bufferSize = 100; // size of the input stream buffer in bytes
    private byte[] buffer = new byte[bufferSize];
    private int bytesRead;

    public ConnectionThread(Socket clientSocket) {

        super();
        socket = clientSocket;
    }

    public void run() {

        try {
            clientis= socket.getInputStream();
            clientReader = new BufferedReader(new InputStreamReader(clientis));

            if ((inputLine = clientReader.readLine()) != null && Pattern.matches("^HTTP", inputLine)) {
                handleResponse(inputLine);
            } else {
                handleRequest(inputLine);
            }
        } catch (IOException e) {
            System.out.println("An I/O Exception has occured creating the client-proxy inputstream or buffered reader");
            System.exit(-1);
        }
    }

    /**
    *Handles a request from the client to the server
    *@param HTTPReqFirstLine the first line of the request
    */
    private void handleRequest(String HTTPReqFirstLine) throws UnknownHostException, MalformedURLException {

        try {
            // if the request begins with GET, or POST then the URL must be parsed
            if (Pattern.matches("^(GET|POST)", inputLine)) {
                parseDestURL(inputLine);
            }

            // sets up a new socket to the servers ip and port on the local address on any free port
            serveros = new Socket(destIp, destPort, null, 0).getOutputStream();
            serveros.write(shortInputLine.getBytes());

            // Runs through all the Header lines
            while ((inputLine = clientReader.readLine()) != null && inputLine != "") {
                inputLine = checkHeaders(inputLine);
                serveros.write(inputLine.getBytes());
            }

            // Now all thats left is the message which can be passed to the OutputStream directly
            do {
                bytesRead = clientis.read(buffer);
                serveros.write(buffer);
            } while (bytesRead != -1);
        } catch (IOException e) {
            System.out.println("An I/O exception has occured while creating the proxy-server outputstream" + 
                               " or while reading from client-proxy inputstream");
            System.exit(-1);
        }
    }

    /**
    *Handles a response from the server to the client
    *@param HTTPRespFirstLine the first line of the response
    */
    private void handleResponse(String HTTPRespFirstLine) {

        try {
            clientos= socket.getOutputStream();
        } catch (IOException e) {
            System.out.println("An I/O exception has occured while creating the server-proxy inputstream" + 
                               " or proxy-client outputstream");
            System.exit(-1);
        }
    }

    /** 
    *Takes the first line of a HTTP GET or POST request and parses the first line into the hostname, url, port 
    *@param HTTPReqFirstLine the first line of the HTTP request containing the destination URL
    *@return modifies the destFullURL, destRelativeURL, destHostname, destPort, and ip variables
    */
    private void parseDestURL(String HTTPReqFirstLine) throws UnknownHostException, MalformedURLException {

        String requestType; // Either GET or POST
        String HTTPVersion; // HTTP/1.1 most likely

        System.out.println(HTTPReqFirstLine);

        // pulls out the URL part of HTTP request first line
        tokens = HTTPReqFirstLine.split("\\s+");
        System.out.println(Arrays.deepToString(tokens));
        requestType = tokens[0];
        destFullURL = tokens[1];
        HTTPVersion = tokens[2];

        // splits up the URL and port which has the format URL:port
        tokens = destFullURL.split(":");
        System.out.println(Arrays.deepToString(tokens));

        // get the last element of the split which will be the port
        try {
            destPort = (int)Integer.valueOf(tokens[tokens.length - 1]);

            // cut off the port part of the URL
            destFullURL = destFullURL.substring(0, destFullURL.lastIndexOf(tokens[tokens.length -1]) - 1);
        // if there is no port specified, and tokens[tokens.length -1] isn't a number, use port 80
        } catch (Exception e) {
            destPort = 80;
        }

        // splits the URL into the hostname and the rest of the url
        // URL will have the format hostname/restofurl
        tokens = destFullURL.split("/");

        System.out.println(Arrays.deepToString(tokens));

        // if the http: part isn't included in the URL hostname is the first element
        if (!tokens[0].equals("http:")) {
            destHostname = tokens[0];

            // if there is more info after the hostname
            if (tokens.length > 1) {
                destRelativeURL = destFullURL.substring(tokens[0].length() -1);
            }
        // otherwise the hostname is the second part
        } else {
            destHostname = tokens[1];

            // if there is more info after the hostname
            if (tokens.length > 2) {
                destRelativeURL = destFullURL.substring(tokens[0].length() + 2 + tokens[1].length() -1);
            }
        }

        try {
            destURL = new URL(destFullURL);
        } catch (MalformedURLException e) {
            throw new MalformedURLException("Browser tried connecting to an incorrectly formatted URL");
        }
        destIp = getDNSLookup(destHostname);

        // the first line the proxy will send to the server
        shortInputLine = requestType + " " + destRelativeURL + " " + HTTPVersion;
    }

    /**
    *This method takes in a header line of an HTTP request and makes any necessary changes
    *Changes made: set Connection and Proxy-connection to "close"
    *@param line the input line that the method is currently checking
    *@return returns the new string of any changes made to the line, and if no changes are made returns
    *the original line
    */
    private static String checkHeaders(String line) {

        // if the line begins with "Connection" or "Proxy-connection" make the property "close"
        if (Pattern.matches("^(Connection:|Proxy-connection:)", line)) {
            return line.split(":")[0] + "close";
        }

        return line;
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
