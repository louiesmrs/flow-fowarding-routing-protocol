!/bin/zsh
docker build -t routingimage .
docker network create -d bridge --subnet 172.20.20.0/24 end1Network
docker network create -d bridge --subnet 172.20.17.0/24 internalNetwork1
docker network create -d bridge --subnet 172.20.19.0/24 internalNetwork2
docker network create -d bridge --subnet 172.20.18.0/24 end2Network
docker network create -d bridge --subnet 172.20.1.0/24 end3Network


docker create -ti --name endpoint1 --cap-add=all -v .:/compnets routingimage /bin/bash
docker create -ti --name router1 --cap-add=all -v .:/compnets routingimage /bin/bash
docker create -ti --name router2 --cap-add=all -v .:/compnets routingimage /bin/bash
docker create -ti --name router3 --cap-add=all -v .:/compnets routingimage /bin/bash
docker create -ti --name endpoint2 --cap-add=all -v .:/compnets routingimage /bin/bash
docker create -ti --name endpoint3 --cap-add=all -v .:/compnets routingimage /bin/bash
docker create -ti --name endpoint4 --cap-add=all -v .:/compnets routingimage /bin/bash

docker network connect end1Network endpoint1
docker network connect end1Network router1
docker network connect internalNetwork1 router1
docker network connect internalNetwork1 router2
docker network connect internalNetwork2 router2
docker network connect internalNetwork2 router3
docker network connect end2Network router3
docker network connect end2Network endpoint2
docker network connect end1Network endpoint3
docker network connect end3Network router3
docker network connect end3Network endpoint4

docker start -i endpoint1

