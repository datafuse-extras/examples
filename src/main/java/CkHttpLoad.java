import java.sql.*;
import java.util.Properties;

import ru.yandex.clickhouse.ClickHouseDataSource;

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

public class CkHttpLoad {
    private final static String DATABEND_HOST = "127.0.0.1";
    private final static String DATABEND_USER = "u1";
    private final static String DATABEND_PASSWORD = "abc123";
    private final static int DATABEND_HTTP_PORT = 8124;

    public static void main(String[] args) throws Exception {
        String url = String.format("jdbc:clickhouse://%s:%s/default",
                DATABEND_HOST,
                DATABEND_HTTP_PORT);
        Properties properties = new Properties();

        int id = 1;
        String name = "winter";
        String passion = "cold";

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            stringBuilder.append(id + "\t" + name + "\t" + passion + "\n");
        }
        String loadData = stringBuilder.toString();

        ClickHouseDataSource dataSource = new ClickHouseDataSource(url, properties);
        try (Connection connection = dataSource.getConnection(DATABEND_USER, DATABEND_PASSWORD)) {
            Statement statement = connection.createStatement();
            long start = System.currentTimeMillis();
            statement.execute(String.format("insert into default.tb_1 format TSV %s", loadData));
            long end = System.currentTimeMillis();
            System.out.printf("cost %d ms\n", end-start);
        }
    }
}
