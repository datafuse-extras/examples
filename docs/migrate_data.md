# Migrate Data

## Streaming Load

Demo in [here](https://github.com/datafuse-extras/examples/blob/8a3c059233210dc2fc671a1d7027228fc00d964b/src/main/java/StreamingLoad.java)

## ClickHouse JDBC

Demo in [here](https://github.com/datafuse-extras/examples/blob/8a3c059233210dc2fc671a1d7027228fc00d964b/src/main/java/CkHttpLoad.java)

## DataX

### Install DataX

Download from https://datax-opensource.oss-cn-hangzhou.aliyuncs.com/202210/datax.tar.gz

In MySQL

```sql
mysql> create user 'mysqlu1'@'%' identified by '123';
mysql> grant all on *.* to 'mysqlu1'@'%';
mysql> create database db;
mysql> create table db.exec(id int, col1 varchar(10));
mysql> insert into db.exec values(1, 'test1'), (2, 'test2'), (3, 'test3');
```

In Databend

```sql
databend> create database migrate_db;
databend> create table migrate_db.exec(id int null, col1 String null);

```

Begin migrate

```shell
$ cd datax/bin

$ cat <<EOF > ./mysql2databend.json
{
  "job": {
    "content": [
      {
        "reader": {
          "name": "mysqlreader",
          "parameter": {
            "column": [
              "id",
              "col1"
            ],
            "connection": [
              {
                "jdbcUrl": [
                  "jdbc:mysql://127.0.0.1:3306/db?useUnicode=true&characterEncoding=utf-8&useSSL=false"
                ],
                "table": [
                  "exec"
                ]
              }
            ],
            "password": "123",
            "username": "mysqlu1",
            "where": ""
          }
        },
        "writer": {
          "name": "mysqlwriter",
          "parameter": {
            "batchByteSize": 134217728,
            "batchSize": 65536,
            "column": [
              "id",
              "col1"
            ],
            "connection": [
              {
                "jdbcUrl": "jdbc:mysql://127.0.0.1:3307/migrate_db?useUnicode=true&characterEncoding=utf-8&useSSL=false",
                "table": [
                  "exec"
                ]
              }
            ],
            "dryRun": false,
            "password": "123",
            "postSql": [],
            "preSql": [],
            "username": "u1",
            "writeMode": "insert"
          }
        }
      }
    ],
    "setting": {
      "speed": {
        "channel": "5"
      }
    }
  }
}
EOF

$ python3 ./datax.py ./mysql2databend.json

```

Check data

```sql
databend> select * from migrate_db.exec;
+------+-------+
| id   | col1  |
+------+-------+
|    1 | test1 |
|    2 | test2 |
|    3 | test3 |
+------+-------+

```

## Addax

### Install Addax

```shell
# more info https://github.com/wgzhao/Addax
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/wgzhao/Addax/master/install.sh)"

```

### Migrate(from MySQL to Databend)

In MySQL

```sql
mysql> create user 'mysqlu1'@'%' identified by '123';
mysql> grant all on *.* to 'mysqlu1'@'%';
mysql> create database db;
mysql> create table db.exec(id int, col1 varchar(10));
mysql> insert into db.exec values(1, 'test1'), (2, 'test2'), (3, 'test3');
```

In Databend

```sql
databend> create database migrate_db;
databend> create table migrate_db.exec(id int null, col1 String null);

```

Begin migrate

```shell
$ cd /opt/addax/bin

$ cat <<EOF > ./mysql2databend.json
{
  "job": {
    "setting": {
      "speed": {
        "channel": 1
      }
    },
    "content": {
      "writer": {
        "name": "mysqlwriter",
        "parameter": {
          "username": "u1",
          "password": "123",
          "column": [
            "*"
          ],
          "connection": [
            {
              "table": [
                "exec"
              ],
              "jdbcUrl": "jdbc:mysql://127.0.0.1:3307/migrate_db"
            }
          ],
          "preSql": [
            "truncate table migrate_db.exec"
          ]
        }
      },
      "reader": {
        "name": "mysqlreader",
        "parameter": {
          "username": "mysqlu1",
          "password": "123",
          "column": [
            "*"
          ],
          "connection": [
            {
              "jdbcUrl": [
                "jdbc:mysql://127.0.0.1:3306/db"
              ],
              "driver": "com.mysql.jdbc.Driver",
              "table": [
                "exec"
              ]
            }
          ]
        }
      }
    }
  }
}
EOF

$ python3 ./addax.py ./mysql2databend.json

```

Check data

```sql
databend> select * from migrate_db.exec;
+------+-------+
| id   | col1  |
+------+-------+
|    1 | test1 |
|    2 | test2 |
|    3 | test3 |
+------+-------+

```