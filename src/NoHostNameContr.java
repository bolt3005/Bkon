 class NoHostNameContr implements javax.net.ssl.HostnameVerifier {
    public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
        return true;
    }
}

