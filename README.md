# TLA+ Conformance Monitoring Tools

This project provides tools for conformance monitoring with [TLA+].

See [Conformance Monitoring with TLA+](docs/README.md) for information about how to deploy and
use conformance monitoring in µONOS.

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

## Monitoring

The [TLC Docker image](#docker-image) extends TLC to facilitate conformance monitoring and
provide cleaner syntax for conformance monitoring specifications. The TLC image supports
the following additional flags:
* `-monitor` - Runs TLC in monitoring mode
* `-trace [source]` - Sets the traces source URI (e.g. `kafka://kafka-service:9092/traces`)
* `-alert [sink]` - Sets the alerts sink URI (e.g. `kafka://kafka-service:9092/alerts`)
* `-window [duration]` - Sets the duration for sliding windows

```bash
$ docker run -v ~/Foo:/opt/tlaplus/model -it onosproject/tlaplus-monitor:latest /opt/tlaplus/model/Foo.tla -monitor -trace kafka://kafka:9092/traces
```

## TLA+ Operators

This project also provides custom [TLA+] operators to assist in conformance
monitoring with TLC in Docker.

### Traces

The `Traces` module provides operators for consuming traces from an external system
in TLA+. Trace consumer operators block until the next trace becomes available and
parses JSON encoded values into TLA+ types. To use the `Traces` module, you must
set the `-trace` flag when running the [Docker image](#docker-image) or configure
the `TRACES_SOURCE` environment variable with the source URI:

```
$ docker run -v ~/Foo:/opt/tlaplus/model -it onosproject/tlaplus-monitor:latest /opt/tlaplus/model/Foo.tla -monitor -trace kafka://kafka:9092/traces
```

To consume traces, create an instance of the `Traces` module:

```
INSTANCE Traces
```

Call the `Trace` operator to consume the trace at a specific offset:

```
VARIABLE offset

INIT == offset = 0

NEXT ==
    /\ offset' = offset + 1
    /\ LET trace == Trace(offset') IN ...
```

### Alerts

The `Alerts` module provides operators for publishing alerts to an external system
from TLA+. To use the `Alerts` module, you must set the `-alert` flag when running
the [Docker image](#docker-image) or configure the `ALERTS_SINK` environment
variable with the URI to which to publish alerts:

```
$ docker run -v ~/Foo:/opt/tlaplus/model -it onosproject/tlaplus-monitor:latest /opt/tlaplus/model/Foo.tla -monitor -alert kafka://kafka:9092/alerts
```

To publish alerts, create an instance of the `Alerts` module:

```
INSTANCE Alerts
```

Use the `PublishAlert` operator to publish an alert to the configured sink:

```
LET alert == [msg = "Invariant violated", id |-> id]
IN PublishAlert(alert)
```

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
LET record == KafkaConsume("my-topic") IN ...
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
