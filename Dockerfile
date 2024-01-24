FROM ubuntu
WORKDIR /compnets
COPY . .
RUN apt-get update
RUN apt-get install -y net-tools netcat tcpdump inetutils-ping openjdk-18-jdk
CMD ["/bin/bash"]
