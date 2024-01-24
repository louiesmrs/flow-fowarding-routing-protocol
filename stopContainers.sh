#!/bin/zsh
docker stop endpoint1
docker stop endpoint2
docker stop router1
docker stop router2
docker stop router3


docker network remove end1Network
docker network remove internalNetwork1
docker network remove end2Network
docker network remove internalNetwork2
# docker network remove end3Network


docker rm endpoint1
docker rm router1
docker rm router2
docker rm endpoint2
docker rm router3
# docker rm endpoint4
# docker rm endpoint3
