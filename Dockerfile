FROM openjdk:11
RUN apt-get update && apt-get install -y build-essential libz-dev wget unzip ocaml-nox git ocaml-native-compilers --no-install-recommends

WORKDIR /tmp

RUN mkdir -p /opt/tlaplus/lib && mkdir -p /opt/tlaplus/module
RUN wget https://tla.msr-inria.inria.fr/tlatoolbox/ci/dist/tla2tools.jar && \
    mv tla2tools.jar /opt/tlaplus/lib/tla2tools.jar
ADD target/model-checker-0.1-jar-with-dependencies.jar /opt/tlaplus/lib

ADD src/main/resources/modules/JsonUtils.tla /opt/tlaplus/module
ADD src/main/resources/modules/KafkaUtils.tla /opt/tlaplus/module

ADD bin/tlc /usr/local/bin/tlc

WORKDIR /root

ENTRYPOINT ["tlc"]
