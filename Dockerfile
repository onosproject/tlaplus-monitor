FROM openjdk:11
RUN apt-get update && apt-get install -y build-essential libz-dev wget unzip ocaml-nox git ocaml-native-compilers --no-install-recommends

WORKDIR /tmp

RUN mkdir -p /opt/tlaplus/lib && mkdir -p /opt/tlaplus/module
RUN wget https://tla.msr-inria.inria.fr/tlatoolbox/ci/dist/tla2tools.jar && \
    mv tla2tools.jar /opt/tlaplus/lib/tla2tools.jar
RUN wget https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.10.2/jackson-core-2.10.2.jar && \
    mv jackson-core-2.10.2.jar /opt/tlaplus/lib/jackson-core-2.10.2.jar
RUN wget https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.10.2/jackson-databind-2.10.2.jar && \
    mv jackson-databind-2.10.2.jar /opt/tlaplus/lib/jackson-databind-2.10.2.jar
RUN wget https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.10.2/jackson-annotations-2.10.2.jar && \
    mv jackson-annotations-2.10.2.jar /opt/tlaplus/lib/jackson-annotations-2.10.2.jar
RUN wget https://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/2.4.0/kafka-clients-2.4.0.jar && \
    mv kafka-clients-2.4.0.jar /opt/tlaplus/lib/kafka-clients-2.4.0.jar

ADD src/main/resources/modules/JsonUtils.tla /opt/tlaplus/module
ADD src/main/resources/modules/KafkaUtils.tla /opt/tlaplus/module
ADD target/model-checker-0.1.jar /opt/tlaplus/lib

ADD bin/tlc /usr/local/bin/tlc

WORKDIR /root

ENTRYPOINT ["tlc"]
