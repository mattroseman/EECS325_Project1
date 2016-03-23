import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

public class ConnectionThread extends Thread {

    private Socket clientSocket; // socket for client proxy communication
    private Socket serverSocket; // socket for proxy server communication
    
    private InputStream clientis; // client -> proxy 
    private BufferedReader clientReader; // client -> proxy (char[])
    private InputStream serveris; // proxy -> server
    private OutputStream clientos; // proxy -> client
    private OutputStream serveros; // server -> proxy

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

    private final int bufferSize = 8000; // size of the input stream buffer in bytes
    private byte[] buffer = new byte[bufferSize];
    private int bytesRead;
    private byte[] tempBytes;

    private boolean contentLengthUsed = false;
    private boolean transferEncodingUsed = false;
    private int msgSize;

    public ConnectionThread(Socket socket) {

        super();
        clientSocket = socket;
    }

    public void run() {

        try {
            clientis = clientSocket.getInputStream();
            clientos = clientSocket.getOutputStream();
            clientReader = new BufferedReader(new InputStreamReader(clientis));

            handleRequest(clientReader.readLine());

            System.out.println("HTTP request has finished being sent");

            handleResponse();

            System.out.println("HTTP response has finished being sent");

            clientSocket.close();
            serverSocket.close();

        } catch (IOException e) {
            System.out.println("An I/O Exception has occured creating the client-proxy inputstream or buffered reader");
            System.out.println(e);
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
            System.out.println(HTTPReqFirstLine);
            if (Pattern.matches("^(GET|POST).*", HTTPReqFirstLine)) {
                System.out.println("GET POST pattern match");
                parseDestURL(HTTPReqFirstLine);
            }

            // sets up a new socket to the servers ip and port on the local address on any free port
            System.out.println(destIp);
            System.out.println(destPort);
            serverSocket = new Socket(destIp, destPort);
            System.out.println("Server Socket created");
            serveros = serverSocket.getOutputStream();
            System.out.println("Server Socket output stream created");
            serveris = serverSocket.getInputStream();
            System.out.println("Server Socket input stream created");
            System.out.println("Writing: " + shortInputLine + " to the server");
            shortInputLine = shortInputLine.concat("\r\n");
            tempBytes = shortInputLine.getBytes(StandardCharsets.US_ASCII);
            serveros.write(tempBytes, 0, tempBytes.length);

            // Runs through all the Header lines
            while ((inputLine = clientReader.readLine()) != null && !inputLine.equals("")) {
                inputLine = checkHeaders(inputLine);
                System.out.println("Writing: " + inputLine + " to the server");
                inputLine = inputLine.concat("\r\n");
                tempBytes = inputLine.getBytes(StandardCharsets.US_ASCII);
                serveros.write(tempBytes, 0, tempBytes.length);
            }

            serveros.write("\r\n".getBytes(StandardCharsets.US_ASCII), 0, "\r\n".length());

            System.out.println("the headers have finished writing to the server");

            /* TODO so what I need to do is check to see if there is a message body, if there is then I need to 
            figure its length and when I've reached that length, then I as the proxy need to close the connection
            since the browser wants to use persistent connections*/

            // Now all thats left is the message which can be passed to the OutputStream directly
            if (contentLengthUsed) {
                transferContentLengthMessage();
            }

            serveros.flush();

            System.out.println("The client request has finished being sent to the server");
        } catch (IOException e) {
            System.out.println("An I/O exception has occured while creating the proxy-server outputstream" + 
                               " or while reading from client-proxy inputstream");
            System.out.println(e);
            System.exit(-1);
        }
    }

    /**
    *When a message body is included in a request and the length is specified with a Content-Length field
    *this method transfers that message
    */
    private void transferContentLengthMessage() throws IOException {
        while (msgSize > 0) {
            bytesRead = clientis.read(buffer, 0, bufferSize);
            if (bytesRead > 0) {
                System.out.println("writing " + bytesRead + " bytes to the server");
                serveros.write(buffer, 0, bytesRead);
            }
            msgSize = msgSize - bytesRead;
        }
    }

    /**
    *Handles a response from the server to the client
    *@param HTTPRespFirstLine the first line of the response
    */
    private void handleResponse() {

        try {
            // immediately sends server's response to the client with no modification
            System.out.println("Now reading server response");

            while ((bytesRead = serveris.read(buffer, 0, bufferSize)) != -1) {
                System.out.println(bytesRead + " bytes where read from the server and written to client");
                clientos.write(buffer, 0, bytesRead);
            }

            clientos.flush();
        } catch (IOException e) {
            System.out.println("An I/O exception has occured while creating the server-proxy inputstream" + 
                               " or proxy-client outputstream");
            System.out.println(e);
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
                destRelativeURL = destFullURL.substring(tokens[0].length());
            }
        // otherwise the hostname is the second part
        } else {
            destHostname = tokens[2];

            // if there is more info after the hostname
            if (tokens.length > 2) {
                destRelativeURL = destFullURL.substring(tokens[0].length() + 2 + tokens[2].length());
            }
        }

        try {
            destURL = new URL(destFullURL);
        } catch (MalformedURLException e) {
            System.out.println("Browser tried connecting to an incorrectly formatted URL");
            System.out.println(e);
        }
        destIp = getDNSLookup(destHostname);

        // the first line the proxy will send to the server
        shortInputLine = requestType + " " + destRelativeURL + " " + HTTPVersion;
    }

    /**
    *This method takes in a header line of an HTTP request and makes any necessary changes
    *Changes made: set Connection and Proxy-connection to "close"
    *method also checks to see if a Content-Length header is present and if so sets msgSize
    *@param line the input line that the method is currently checking
    *@return returns the new string of any changes made to the line, and if no changes are made returns
    *the original line
    */
    private String checkHeaders(String line) {

        // if the line begins with "Connection" or "Proxy-connection" make the property "close"
        if (Pattern.matches("^(Connection:|Proxy-connection:).*", line)) {
            return line.split(":")[0] + ": close";
        } 
        else if (Pattern.matches("^Content-Length:.*", line)) {
            contentLengthUsed = true;
            tokens = line.split(" ");
            msgSize = (int)Integer.valueOf(tokens[1]);
        }
        else if (Pattern.matches("^TE:.*", line)) {
            transferEncodingUsed = true;
            System.out.println("Transfer encoding used, not implemented yet");
        }

        return line;
    }

    /** 
    *Performs a DNS lookup on a hostname string and returns the found ip address
    *@param hostname the hostname to lookup
    *@return the InetAddress object for the cooresponding ip address
    */
    private static InetAddress getDNSLookup(String hostname) throws UnknownHostException {

        InetAddress ipAddress = null;

        // do a DNS lookup of the hostname
        try {
            ipAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            System.out.println("the browser tried to connect to an unknown host");
            System.out.println(e);
        }       

        return ipAddress;
    }
}
