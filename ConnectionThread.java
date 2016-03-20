import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.regex.Pattern;

public class ConnectionThread extends Thread {

    Socket socket;

    String inputLine; // first line read from the HTTP Request
    String shortInputLine; // first line read with only the relative URL
    String[] tokens; // used multiple times when parsing the first line
    String destFullURL; // the full URL for the destination address
    String destRelativeURL = ""; // just the relative part of the destination URL
    String destHostname; // just the hostname part of the destination URL
    int destPort; // the port of the destination (default is 80)

    URL destURL;
    InetAddress destIp;
    URLConnection urlConn;

    byte[] buffer = new byte[100];
    int bytesRead;

    public ConnectionThread(Socket clientSocket) {

        super();
        socket = clientSocket;
    }

    public void run() {

        try {

            // InputStreamReader takes the byte stream and converts it to a char stream
            // the BufferedReader makes the char stream readable
            InputStream istream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(istream));

            inputLine = reader.readLine();
            // if the request begins with GET, or POST then the URL must be parsed
            if (Pattern.matches("^(GET|POST)", inputLine)) {
                parseDestURL(inputLine);
            }

            while ((inputLine = reader.readLine()) != null && inputLine != "") {
                inputLine = checkHeaders(inputLine);
            }

            /*TODO at this point I should go through all the headers of the HTTP request and make sure there is no 
            Connection header, or Proxy-Connection, because if there is I need to change it.
            Find a way to figure when the message body begins so I can stop reading in from istream with the
            InputStreamReader*/

            // the output stream for this request
            OutputStream ostream = new Socket(destIp, destPort).getOutputStream();
            ostream.write(shortInputLine.getBytes());

            // until the input stream is finished the bytes are directly sent to the output stream "buffer" bytes at a time
            do {
                bytesRead = istream.read(buffer);
                ostream.write(buffer);
            } while (bytesRead != -1);

        } catch (Exception e) {

        }

    }

    /** 
    *Takes the first line of a HTTP GET or POST request and parses the first line into the hostname, url, port 
    *@param HTTPReqFirstLine the first line of the HTTP request containing the destination URL
    *@return modifies the destFullURL, destRelativeURL, destHostname, destPort, and ip variables
    */
    private void parseDestURL(String HTTPReqFirstLine) throws UnknownHostException, MalformedURLException {

        String requestType;
        String HTTPVersion;

        // Reads the first line of the request and pulls out the URL
        if (HTTPReqFirstLine != null) {

            System.out.println(HTTPReqFirstLine);

            // pulls out the URL part of HTTP request
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

                // if the Integer.valueOf part throws an exception then that means there is no port and this part will
                // be skipped

                // get cut off the port part of the URL
                destFullURL = destFullURL.substring(0, destFullURL.lastIndexOf(tokens[tokens.length -1]) - 1);
            // if there is no port specified, and tokens[tokens.length -1] isn't a number, use port 80
            } catch (Exception e) {
                destPort = 80;
            }

            // splits the URL into the hostname and the rest of the url
            // URL will have the format hostname/restofurl
            tokens = destFullURL.split("/");

            System.out.println(Arrays.deepToString(tokens));

            // if the http: part isn't included in the URL
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

            // this includes the port number
            try {
                destURL = new URL(destFullURL);
            } catch (MalformedURLException e) {
                throw new MalformedURLException("Browser tried connecting to a nonexistent URL");
            }
            destIp = getDNSLookup(destHostname);

            shortInputLine = requestType + " " + destRelativeURL + " " + HTTPVersion;
        }
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
