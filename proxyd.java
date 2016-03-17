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

        String port = getPort(args);

        System.out.println(port);
    }

    private static String getPort(String[] args) {

        Boolean nextStringPort = false;
        String port = "";

        for (String s: args) {
            if (nextStringPort) {
                port = s;
            }
            if (s.equals("-port")) {
                nextStringPort = true;
            }
        }

        return port;
    }
}



