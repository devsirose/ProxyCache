import java.io.*;

public class HttpResponse {
    String version;
    int statusCode;
    String statusLine = "";
    String contentType = "";
    String headers = "";
    byte[] body = new byte[MAX_OBJECT_SIZE];
    int bodyLength = 0;
    boolean isKeepAlive = true;
    String transferEncoding = "";
    final static String CRLF = "\r\n";
    final static int BUF_SIZE = 262144;
    final static int MAX_OBJECT_SIZE = 2097152; // 2MB

    public HttpResponse(int code) throws Exception {
        if (code == 403) {
            statusCode = 403;
            statusLine = "HTTP/1.1 403 FORBIDDEN";
            InputStream inputStream = new FileInputStream("./Response_page/Forbidden_403.html");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int bytesReader = 0;
            byte[] buffer = new byte[BUF_SIZE];
            while ((bytesReader = inputStream.read(buffer, 0, BUF_SIZE)) != -1) {
                outputStream.write(buffer, 0, bytesReader);
            }
            body = outputStream.toByteArray();
            bodyLength = body.length;
            StringBuilder headersBuilder = new StringBuilder("Content-Type: text/html; charset=ISO-8859-\r\n");
            headersBuilder.append("Content-length: ").append(bodyLength).append(CRLF);
            headers = headersBuilder.toString();
            outputStream.close();
            inputStream.close();
        } else if (code == 200) {
            statusCode = 200;
            statusLine = "HTTP/1.1 200 OK";
            contentType = "image/*";
        }
    }

    @Deprecated
    public HttpResponse(DataInputStream fromServer) {
        /* Length of the object */
        int length = -1;
        boolean gotStatusLine = false;
        /* First read status line and response headers */
        try {
            String line = fromServer.readLine();
            while (line.length() != 0) {
                if (line.startsWith("Transfer-Encoding:")) {
                    String[] parts = line.split(" ");
                    transferEncoding = parts[1];
                    line = fromServer.readLine();
                }
                if (!gotStatusLine) {
                    statusLine = line;
                    String[] parts = line.split(" ");
                    version = parts[0];
                    statusCode = Integer.parseInt(parts[1]);
                    gotStatusLine = true;
                } else {
                    headers += line + CRLF;
                }
                if (line.toLowerCase().startsWith("content-length:")) {
                    String[] parts = line.split(" ");
                    length = Integer.parseInt(parts[1]);
                }
                if (line.toLowerCase().startsWith("content-type:")) {
                    String[] parts = line.split(" ");
                    contentType = parts[1];
                }
                if (line.toLowerCase().equalsIgnoreCase("connection: close")) {
                    isKeepAlive = false;
                }
                line = fromServer.readLine();
            }
            /** Case: HTTP response have 2 status line */
            if (statusLine.equals("HTTP/1.1 100 Continue")) {
                length = -1;
                gotStatusLine = false;
                headers = "";
                statusLine = "";
                line = fromServer.readLine();
                while (line.length() != 0) {
                    if (!gotStatusLine) {
                        statusLine = line;
                        String[] parts = line.split(" ");
                        version = parts[0];
                        statusCode = Integer.parseInt(parts[1]);
                        gotStatusLine = true;
                    } else {
                        headers += line + CRLF;
                    }
                    if (line.toLowerCase().startsWith("content-length:")) {
                        String[] parts = line.split(" ");
                        length = Integer.parseInt(parts[1]);
                    }
                    if (line.toLowerCase().startsWith("content-type:")) {
                        String[] parts = line.split(" ");
                        contentType = parts[1];
                    }
                    if (line.toLowerCase().equalsIgnoreCase("connection: close")) {
                        isKeepAlive = false;
                    }
                    line = fromServer.readLine();
                }
            }
        } catch (Exception e) {
            return;
        }

        try {
            int bytesRead = 0;
            byte buf[] = new byte[BUF_SIZE];
            boolean loop = false;
            /*
             * If we didn't get Content-Length header, body is encode. Just handle chunked
             * encoding.
             */
            if (length == -1 && transferEncoding.equals("chunked")) {
                String line;
                byte[] buffer = new byte[BUF_SIZE]; // You can adjust the buffer size as needed
                ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();

                while ((line = fromServer.readLine()) != null) {
                    // Check if this is the chunk size line (it should be a hexadecimal number)
                    if (isHexadecimalNumber(line)) {
                        int chunkSize = Integer.parseInt(line, 16);
                        if (chunkSize == 0) {
                            line = fromServer.readLine(); // read last empty line
                            break;
                        }
                        int totalBytesRead = 0;
                        while (totalBytesRead < chunkSize) {
                            int remainingBytes = chunkSize - totalBytesRead;
                            int bytesToRead = Math.min(remainingBytes, buffer.length);
                            int bytesReadNow = fromServer.read(buffer, 0, bytesToRead);
                            if (bytesReadNow == -1) {
                                break; // End of stream
                            }
                            bodyStream.write(buffer, 0, bytesReadNow);
                            totalBytesRead += bytesReadNow;
                        }
                        fromServer.readLine();
                    }
                }
                bodyStream.close();
                bodyLength = bodyStream.size();
                body = bodyStream.toByteArray();
                return;
            }
            if (length == -1) {
                loop = true;
            }
            while (bytesRead < length || loop) {
                /* Read it in as binary data */
                int res = fromServer.read(buf);
                if (res == -1) {
                    break;
                }
                for (int i = 0; i < res && (i + bytesRead) < MAX_OBJECT_SIZE; i++) {
                    body[i + bytesRead] = buf[i];
                }
                bytesRead += res;
            }
            bodyLength = bytesRead;
        } catch (IOException e) {
            System.out.println("Error reading response body: " + e);
            return;
        }
    }

    public void sendResponse(DataOutputStream toClient) throws IOException {
        String response = toString();
        toClient.writeBytes(response);
        toClient.write(body, 0, getBodyLength());
        toClient.flush();
    }

    // *get body from file cache */
    void setBody(FileInputStream fileIn) throws IOException {
        bodyLength = 0;
        int bytesReader = 0;
        while ((bytesReader = fileIn.read(body, bodyLength, BUF_SIZE)) != -1) {
            bodyLength += bytesReader;
        }
        fileIn.close();
    }

    // *save body to file */
    public void saveBody(OutputStream fileOut) throws IOException {
        fileOut.write(body, 0, getBodyLength());
        fileOut.flush();
    }

    public void sendHeaders(OutputStream fileOut) throws IOException {
        String response = toString();
        fileOut.write(response.getBytes());
        fileOut.flush();
    }

    public String toString() {
        // encapsulate content send to client
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(statusLine).append(CRLF)
                .append("Content-Length: ").append(bodyLength).append(CRLF)
                .append("Content-Type: ").append(contentType).append(CRLF);
        stringBuilder.append(headers).append(CRLF);
        String response = stringBuilder.toString();
        // end encapsulate
        return response;
    }

    public int getBodyLength() {
        return this.bodyLength;
    }

    public String getContentType() {
        return this.contentType;
    }

    public boolean isKeepAlive() {
        return this.isKeepAlive;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    /* Supported function */
    public boolean isHexadecimalNumber(String str) {
        try {
            Integer.parseInt(str, 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void copyRangeArray(byte[] arrayA, int startA, int endA, byte[] arrayB, int startB) {
        int elementsToCopy = Math.min(endA - startA + 1, arrayB.length - startB);
        for (int i = 0; i < elementsToCopy; i++) {
            arrayA[startA + i] = arrayB[startB + i];
        }
    }
}
