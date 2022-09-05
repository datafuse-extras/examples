import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 How to use:
 *
 * 1. create a table in databend
 *

 CREATE TABLE `tb_1` (
 `id` INT,
 `name` VARCHAR,
 `passion` VARCHAR
 );

 *
 * 2. change the connection in this class
 *
 * 3. run this class, you should see the following output:
 *
 */

public class StreamingLoad {
    private final static String DATABEND_HOST = "127.0.0.1";
    private final static String DATABEND_USER = "u1";
    private final static String DATABEND_PASSWORD = "abc123";
    private final static int DATABEND_HTTP_PORT = 8000;

    public static void main(String[] args) throws Exception {
        int id = 1;
        String name = "winter";
        String passion = "cold";

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            stringBuilder.append(id + "\t" + name + "\t" + passion + "\n");
        }
        String loadData = stringBuilder.toString();
        StreamingLoad loader = new StreamingLoad();

        final String loadUrl = String.format("http://%s:%s/v1/streaming_load",
                                             DATABEND_HOST,
                                             DATABEND_HTTP_PORT);

        /// CloseableHttpClient is very expensive and it should be reused
        final HttpClientBuilder httpClientBuilder = HttpClients
            .custom();

        try (CloseableHttpClient client = httpClientBuilder.build()) {
            long start = System.currentTimeMillis();
            loader.sendData(client, loadUrl, loadData.getBytes());
            long end = System.currentTimeMillis();
            System.out.printf("cost %d ms\n", end-start);
        }
    }

    private String basicAuthHeader(String username, String password) {
        final String tobeEncode = username + ":" + password;
        byte[] encoded = Base64.encodeBase64(tobeEncode.getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encoded);
    }

    private void sendData(CloseableHttpClient client, String loadUrl, byte[] content) throws Exception {
            HttpPut put = new HttpPut(loadUrl);
            put.setHeader(HttpHeaders.AUTHORIZATION, basicAuthHeader(DATABEND_USER, DATABEND_PASSWORD));
            put.setHeader("insert_sql", "insert into default.tb_1 format TSV");

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addBinaryBody("upfile", content, ContentType.DEFAULT_BINARY, "test_file");

            HttpEntity entity = builder.build();
            put.setEntity(entity);

            try (CloseableHttpResponse response = client.execute(put)) {
                String loadResult = "";
                if (response.getEntity() != null) {
                    loadResult = EntityUtils.toString(response.getEntity());
                }
                final int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new IOException(
                        String.format("Stream load failed, statusCode=%s load result=%s", statusCode, loadResult));
                }

                System.out.println(loadResult);
            }
    }
}
