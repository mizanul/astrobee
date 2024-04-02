#!/bin/bash

##############################
# Static values
#

rootdir=$(dirname "$(readlink -f "$0")")
cd $rootdir
bootstrap_dir=${rootdir}/bootstrap
crew_operation_dir=${rootdir}/crew_operation_module

OBJECT_NAME_ARRAY=("lostitem1" "lostitem2" "lostitem3" "lostitem4" "lostitem_target")

XSOCK=/tmp/.X11-unix
XAUTH=/tmp/.docker.xauth
touch $XAUTH
xauth nlist $DISPLAY | sed -e 's/^..../ffff/' | xauth -f $XAUTH nmerge -

##############################
# Run Simulator
#

ROS_IP=$(getent hosts llp | awk '{ print $1 }')

docker run -it --rm --name astrobee \
        --volume=$XSOCK:$XSOCK:rw \
        --volume=$XAUTH:$XAUTH:rw \
        --volume=${bootstrap_dir}:/tmp/bootstrap \
        --volume=${crew_operation_dir}:/tmp/crew_operation_module \
        --env="XAUTHORITY=${XAUTH}" \
        --env="DISPLAY" \
        --env="ROS_MASTER_URI=http://${ROS_IP}:11311" \
        --env="ROS_HOSTNAME=${ROS_IP}" \
        --user="astrobee" \
        --privileged \
        --network=host \
        krpc/astrobee_sim-base:5.0.0 \
        /astrobee_init.sh bash /tmp/bootstrap/run.sh

