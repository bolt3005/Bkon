import java.util.List;
import java.util.Map;

public class Response {
    private final boolean ok;
    private final int status;
    private final Map<String, List<String>> headerFields;
    private final java.io.ByteArrayOutputStream stream;

    public Response(final boolean ok, final int status, final Map<String, List<String>> headerFields,
                    final java.io.ByteArrayOutputStream stream) {
        this.ok = ok;
        this.status = status;
        this.stream = stream;
        this.headerFields = headerFields;
    }

    public boolean isOk() {
        return ok;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, List<String>> getHeaderFields() {
        return headerFields;
    }

    public byte[] blob() {
        return stream.toByteArray();
    }

    public String text() {
        try {
            return stream.toString("UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }
}

