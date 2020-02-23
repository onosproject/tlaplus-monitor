# TLA+ Conformance Monitoring Tools

This project provides tools for conformance monitoring with [TLA+].

## Docker Image

This project provides a [Docker] image for running the TLA+ model checker (TLC).

Maven is used to build the Docker image:

```bash
$ mvn clean package
```

Building the project with Maven will build the `onosproject/tlaplus-monitor:latest` image.

When running the image with `docker run`, [command-line arguments](https://lamport.azurewebsites.net/tla/current-tools.pdf)
are passed through to the `tlc` command:

```bash
$ docker run -v ~/Foo:/opt/tlaplus/model -it onosproject/tlaplus-monitor:latest /opt/tlaplus/model/Foo.tla
```

## TLA+ Operators

This project also provides custom [TLA+] operators to assist in conformance
monitoring with TLC in Docker.

### JsonUtils

The `JsonUtils` module provides operators for reading and writing JSON files in
TLA+. JSON input files must contain a valid JSON string on each newline, and JSON
is output in the same format. The utility can read and write records, sequences,
strings, and integers.

To read or write JSON files, create an instance of the `JsonUtils` module:

```
INSTANCE JsonUtils
```

The `JsonDeserialize` operator reads a JSON file into a sequence of values:

```
LET records == JsonDeserialize("/path/to/input.json")
IN ...
```

The `JsonSerialize` operator writes a sequence of values to a JSON file:

```
LET records == <<[id |-> 1, value |-> "foo"], [id |-> 2, value |-> "bar"], [id |-> 3, value |-> "baz"]>>
IN JsonSerialize("/path/to/output.json", records)
```

#### KafkaUtils

The `KafkaUtils` module provides operators for producing to and consuming from
[Kafka] topics. Kafka messages are produced and consumed in JSON format using
the `JsonUtils` parser.

To use the Kafka operators, create an instance of the `KafkaUtils` module:

```
INSTANCE KafkaUtils
```

To consume messages from a Kafka topic, use the `KafkaConsume` operator with
the name of the topic:

```
LET record == KafkaConsume("my-topic")
IN ...
```

To produce messages to a Kafka topic, use the `KafkaProduce` operator with the
name of the topic and the value to send:

```
LET record == [id |-> 1, value |-> "foo"]
IN KafkaProduce("my-topic", record)
```

The `KafkaProduce` operator returns `TRUE` for convenient use in predicates.

[Docker]: https://www.docker.com/
[TLA+]: https://lamport.azurewebsites.net/tla/tla.html
[Kafka]: https://kafka.apache.org/
