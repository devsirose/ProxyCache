import java.io.*;

public class Caching {
    /** Caching communicate with HTTP request and HTTP response */
    /** Caching supported only image (png, jpg, jpeg,...) */
    private String path = ""; // path to image
    /* Define a map to store a key-value : HttpRequest and time last modified */
    private int timeCache = 0;

    public Caching(HttpRequest httpRequest) {
        // Get uri to query path to file image
        path = parsePath(httpRequest.getURI(), httpRequest.getHost());
    }

    public boolean excuteQuery(HttpRequest httpRequest) {
        // * Return true if image exist in storage */
        if (path.lastIndexOf("/") == path.length() - 1)
            return false;
        File file = new File(path);
        return file.exists();
    }

    public void update(HttpResponse httpResponse) throws InterruptedException {
        // store httpResponse has content-type: image/...
        if (!httpResponse.getContentType().startsWith("image"))
            return;
        /** Save image */
        File directory = new File(path).getParentFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
        try {
            OutputStream imageOutputStream = new FileOutputStream(path);
            httpResponse.saveBody(imageOutputStream);
            imageOutputStream.close();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(-1);
        }
        /** Sleep wait until timeCache to delete cache file */
        Thread.sleep(timeCache);
        File pathFile = new File(path);
        try {
            if (pathFile.exists()) {
                pathFile.delete();
                System.out.println("Deleted file image in cache repository");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void setCacheTime(int timeCache) {
        this.timeCache = timeCache * 1000;
    }

    public String parsePath(String uri, String host) {
        StringBuilder pathBuilder = new StringBuilder("./cache/");
        pathBuilder.append(host);
        String fileName = null;
        if (uri.lastIndexOf("&") != -1) {
            fileName = uri.trim().substring(uri.lastIndexOf("/"), uri.lastIndexOf("&"));
        } else
            fileName = uri.trim().substring(uri.lastIndexOf("/"));
        pathBuilder.append(fileName);
        return pathBuilder.toString();
    }

    public String getPath() {
        return this.path;
    }
}