
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {
    private static final Map<String, List<String>> HEADER_FIELDS = new HashMap<String, List<String>>() { {
        put("Content-Type", Arrays.asList("application/json", "charset=utf-8"));
    } };

    private static final Set<String> HTTP_METHODS = new HashSet<String>(
            Arrays.asList("GET", "POST", "PUT", "DELETE"));

    private static final int BUFFER_LENGTH_1024 = 1024;

    private static final int RESPONSE_CODE_400 = 400;





    public static Response fetch(final String url) {
        return fetch(url, null, null, HEADER_FIELDS, null, null);
    }

    public static Response fetch(final String url, final String method, final String body,
                                 final Map<String, List<String>> headerFields, final Integer connectTimeout, final Integer readTimeout) {
        final String localMethod;
        if (HTTP_METHODS.contains(method)) {
            localMethod = method;
        } else {

            localMethod = "GET";
        }
        try {

            final java.net.URL localUrl = new java.net.URL(url);

            final java.net.HttpURLConnection connection = (java.net.HttpURLConnection) localUrl.openConnection();
            connection.setRequestMethod(localMethod);
            if (connectTimeout != null) {
                connection.setConnectTimeout(connectTimeout);
            }
            if (readTimeout != null) {
                connection.setReadTimeout(readTimeout);
            }
            if (connection instanceof javax.net.ssl.HttpsURLConnection) {
                ((javax.net.ssl.HttpsURLConnection) connection).setHostnameVerifier(new NoHostNameContr());
            }
            if (headerFields != null) {
                for (final Map.Entry<String, List<String>> header : headerFields.entrySet()) {
                    connection.setRequestProperty(header.getKey(), join(header.getValue(), ";"));
                }
            }
            if (body != null) {
                connection.setDoOutput(true);
                final java.io.DataOutputStream outputStream =
                        new java.io.DataOutputStream(connection.getOutputStream());
                outputStream.writeBytes(body);
                outputStream.flush();
                outputStream.close();
            }
            final int responseCode = connection.getResponseCode();

            final java.io.InputStream inputStream;
            if (responseCode < RESPONSE_CODE_400) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            final java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
            final byte[] buffer = new byte[BUFFER_LENGTH_1024];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            inputStream.close();
            return new Response(responseCode < RESPONSE_CODE_400, responseCode, connection.getHeaderFields(),
                    result);
        } catch (java.io.IOException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    static <T> String join(final Iterable<T> iterable, final String separator) {
        final StringBuilder sb = new StringBuilder();
        int index = 0;
        for (final T item : iterable) {
            if (index > 0) {
                sb.append(separator);
            }
            sb.append(item.toString());
            index += 1;
        }
        return sb.toString();
    }

    private static List<LinkItem> parseLinksFile(String linksFileName) {
        List<LinkItem> result = new ArrayList<LinkItem>();
        Path linksFilePath = Paths.get(linksFileName);
        try {
            List<String> lines = Files.readAllLines(linksFilePath, Charset.forName("UTF-8"));
            for (String line : lines) {
                if (line.matches("\\S+\\s+\\S+")) {
                    LinkItem linkItem = new LinkItem();
                    String[] linkItems = line.split("\\s+");
                    linkItem.url = linkItems[0];
                    linkItem.nameFiles = linkItems[1];
                    result.add(linkItem);
                }
            }
        } catch (IOException ex) {
        }
        return result;
    }

    public static void downloadFiles(String threads, String outputFolder, String linksFileName, Callback callback) {
        List<Void> result = new ArrayList<Void>();

        final ExecutorService executor = Executors.newFixedThreadPool(Integer.parseInt(threads));
        final List<Callable<Void>> callables = new ArrayList<Callable<Void>>();
        final List<LinkItem> linkItems = parseLinksFile(linksFileName);

        Statistics.totalFilesCount.set(linkItems.size());
        for (final LinkItem linkItem : linkItems) {
            callables.add(new Callml(outputFolder, linkItem, callback));
        }
        try {


            for (Future<Void> future : executor.invokeAll(callables)) {
                try {
                    result.add(future.get());
                } catch (ExecutionException ex) {
                    System.out.println("ExecutionException - " + ex.getMessage());
                }
            }
        } catch (InterruptedException ex) {
            System.out.println("InterruptedException - " + ex.getMessage());
        }
        executor.shutdown();
    }

    public static void main(String[] args) {
        String threads = "2";
        String outputFolder = "DownLoad";


        String linksFileName = "links.txt";
        if (args.length == 0) {
            System.out.println("Usage: java -jar utility.jar 2 nameFolder links.txt");
        } else {
            if (args.length >= 1) {
                threads = args[0];
            } else if (args.length >= 2) {
                outputFolder = args[1];
            } else if (args.length >= 3) {
                linksFileName = args[2];
            }
            Callback callback = new Callback() {
                public void report(String data) {
                    System.out.println(data);
                }
            };

            downloadFiles(threads, outputFolder, linksFileName, callback);
        }
    }
}

