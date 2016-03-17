import java.io.*;
import java.net.*;

/* 
*  socket to use = 5000 + 26 = 5026
*  must hand GET and POST requests
*  no pipelining but need to have persistent connection
*  have to handle multiple parallel connections
*/

public class proxyd {


    public static void main(String[] args) {

        proxyd proxy = new proxyd(args);
    }

    public proxyd(String[] args) {

        int port = getPort(args);

        System.out.println(port);
    }

    /**
    *Takes the arguments passed into the program and returns the port number
    *@param args an array of strings that are passed into this program
    *@return the number of the port that was passed in
    */
    private static int getPort(String[] args) {
        
        int port = 0;

        if (args.length != 2) {
            throw new IllegalArgumentException("insufficient arguments");
        }

        if (!args[0].trim().equals("-port")) {
            throw new IllegalArgumentException("incorrect arguments");
        }

        try {
            port = (int)Integer.valueOf(args[1]);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("port must be an integer");
        }

        return port;
    }
}
