import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class HttpRequest {
    private String method = "";
    private String URI = "";
    private String version = "";
    private String host = "";
    private String headers = "";
    private String body = "";
    private int bodyLength = 0;
    private static final String CRLF = "\r\n";

    public HttpRequest() {
        // Default constructor
    }

    public HttpRequest(BufferedReader fromClient) throws IOException {
        // Parse message from the client to HTTP format
        String requestLine = fromClient.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            method = "NULL";
            System.out.println("null HTTP request");
            return;
        }

        String[] line = requestLine.split(" ");
        method = line[0];
        // process GET, POST, HEAD method
        if (!method.equals("GET") && !method.equals("POST") && !method.equals("HEAD")) {
            method = "UNKNOWN";
            return;
        }

        StringBuilder headersBuilder = new StringBuilder();
        URI = line[1];
        version = line[2];

        String hostLine = fromClient.readLine();
        String[] hostParts = hostLine.split(" ");
        host = hostParts[1];
        headersBuilder.append(hostLine).append(CRLF);

        while (true) {
            String headerLine = fromClient.readLine();
            if (headerLine.isEmpty())
                break;
            if (headerLine.startsWith("Content-Length:") || headerLine.startsWith("Content-length:"))
                bodyLength = Integer.parseInt(headerLine.trim().substring(16));
            if (headerLine.toLowerCase().startsWith("user-agent:"))
                continue;
            if (headerLine.toLowerCase().startsWith("referer:"))
                continue;
            if (headerLine.toLowerCase().startsWith("upgrade-insecure-requests:"))
                continue;
            headersBuilder.append(headerLine).append(CRLF);
        }
        headers = headersBuilder.toString();

        if (method.equals("POST")) {
            StringBuilder bodyBuilder = new StringBuilder();
            char[] buffer = new char[4096];
            int bytesRead;
            while ((bytesRead = fromClient.read(buffer, 0, buffer.length)) > 0) {
                bodyBuilder.append(buffer, 0, bytesRead);
                if (bodyBuilder.length() >= bodyLength)
                    break;
            }
            body = bodyBuilder.toString();
        }
    }

    public String getRequest() {
        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append(method).append(" ");
        requestBuilder.append(URI).append(" ");
        requestBuilder.append(version).append(CRLF);
        requestBuilder.append(headers).append(CRLF);
        if (method.equals("POST")) {
            requestBuilder.append(body);
        }
        String request = requestBuilder.toString();
        return request;
    }

    public void sendRequest(BufferedWriter toServer) throws IOException {
        toServer.write(getRequest());
        toServer.flush();
    }

    public void sendBody(BufferedWriter toServer) throws IOException {
        toServer.write(body);
    }

    public String getHost() {
        return this.host;
    }

    public String getMethod() {
        return this.method;
    }

    public String getURI() {
        return this.URI;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getBodyLength() {
        return this.bodyLength;
    }

    public String getBody() {
        return this.body;
    }
}
