import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyCache {
    // Port for the proxy
    private static int localPort;
    final static int httpPort = 80;
    // Create socket for client connection
    private static ServerSocket proxy;
    final static int timeOut = 300000;
    private static Config config = new Config("./Config.json");

    public static void init(int localport) {
        try {
            localPort = localport;
            proxy = new ServerSocket(localPort, 100, null);
        } catch (IOException e) {
            System.out.println("Error creating soket");
            System.exit(-1);
        }
    }

    @Deprecated
    // Handle client connection
    public static void handleClient(Socket clientSocket) {
        try {
            BufferedReader fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter toServer = null;
            DataInputStream fromServer = null;
            DataOutputStream toClient = null;
            HttpRequest httpRequest = null;
            HttpResponse httpResponse = null;
            Socket clientProxy = null;
            boolean isConnected = false;
            while (true) {
                httpRequest = new HttpRequest(fromClient);
                /** Check request by config file */
                if (httpRequest.getMethod().equals("NULL")) {
                    return;
                }
                if (!config.checkRequest(httpRequest)) {
                    httpRequest.setMethod("UNKNOWN");
                }
                if (httpRequest.getMethod().equals("UNKNOWN")) {
                    httpResponse = new HttpResponse(403);
                    toClient = new DataOutputStream(clientSocket.getOutputStream());
                    httpResponse.sendResponse(toClient);
                    clientSocket.close();
                    toClient.close();
                    return;
                }
                Caching image = new Caching(httpRequest);
                image.setCacheTime(config.getCacheTime());
                if (image.excuteQuery(httpRequest)) {
                    toClient = new DataOutputStream(clientSocket.getOutputStream());
                    HttpResponse response = new HttpResponse(200);
                    response.setBody(new FileInputStream(image.getPath()));
                    response.sendResponse(toClient);
                    System.out.println("Query Cache");
                    toClient.close();
                    clientSocket.close();
                    return;
                }
                /** Connection if needed */
                String serverHost = httpRequest.getHost();
                if (!isConnected || clientProxy.isClosed()
                        || !((InetSocketAddress) clientProxy.getRemoteSocketAddress()).getHostString()
                                .equals(serverHost)) {
                    clientProxy = new Socket(serverHost, httpPort);
                    clientProxy.setKeepAlive(true);
                    clientProxy.setSoTimeout(timeOut);
                    clientProxy.setTcpNoDelay(true);
                    toServer = new BufferedWriter(new OutputStreamWriter(clientProxy.getOutputStream()));
                    fromServer = new DataInputStream(clientProxy.getInputStream());
                    toClient = new DataOutputStream(clientSocket.getOutputStream());
                    System.out.println("Connecting to " + httpRequest.getHost() + " port: " + httpPort);
                    isConnected = true;
                } else {
                    System.out.println("Reuse socket sending to server");
                }
                httpRequest.sendRequest(toServer);
                httpResponse = new HttpResponse(fromServer);
                httpResponse.sendResponse(toClient);
                /** Update caching */
                final HttpResponse response = httpResponse;
                Runnable innerThread = () -> {
                    try {
                        image.update(response);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };
                Thread thread = new Thread(innerThread);
                thread.start();
                /* Close socket if server close connection */
                if (!httpResponse.isKeepAlive()) {
                    // *Close connection */
                    clientProxy.close();
                    clientSocket.close();
                    /** Close stream to free memory buffer */
                    toServer.close();
                    fromServer.close();
                    toClient.close();
                    fromClient.close();
                    return;
                }
            }
        } catch (Exception e) {
            // System.out.println(e);
        }
    }

    public static void main(String[] args) {
        int localport = 0;
        try {
            localport = Integer.parseInt(args[0]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Need port number as argument");
            System.exit(-1);
        } catch (NumberFormatException e) {
            System.out.println("Please give port number as integer.");
            System.exit(-1);
        }
        init(localport);
        while (true) {
            try {
                Socket clientSocket = proxy.accept();
                clientSocket.setTcpNoDelay(true);
                Runnable outerThread = () -> {
                    try {
                        handleClient(clientSocket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };
                Thread thread = new Thread(outerThread);
                thread.start();
            } catch (Exception e) {
                System.out.println(e);
                continue;
            }
        }
    }
}