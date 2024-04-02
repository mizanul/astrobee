#!/bin/sh

#####################
# Bootstrap scripts #
#####################

# update env files
bash /tmp/bootstrap/copy_objects.sh

# batch
patch /src/astrobee/src/description/media/astrobee_iss/urdf/model.urdf /tmp/bootstrap/model.urdf.patch

# run
/astrobee_init.sh roslaunch astrobee sim.launch dds:=false robot:=sim_pub rviz:=true &
python3 /tmp/crew_operation_module/crew_operation.py