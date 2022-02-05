
# Emulator

## Running Simulator
### Setting up environment:
```
pushd $BUILD_PATH
source devel/setup.bash
popd

```
### Running it:
```
roslaunch astrobee sim.launch dds:=false robot:=sim_pub rviz:=true

```

### Running world only:

```
roslaunch astrobee sim.launch default:=false rviz:=true

```

### Spawning astrobee in world:

```
roslaunch astrobee spawn.launch dds:=false robot:=sim_pub ns:=”insert namespace” world:="granite"

```

### Teleoperation commands:

```
rosrun executive teleop_tool -ns "insert namespace" -get_state
rosrun executive teleop_tool -undock
rosrun executive teleop_tool -dock
Reset gravity: rosrun executive teleop_tool -reset_bias

```
### Example for moving: rosrun executive teleop_tool -move -relative -pos "1 2 0.5" Rqt_graph
```
rosrun rqt_graph rqt_graph

```

### Launch Emulator:

```
android-studio/bin/studio.sh
cd $ANDROID_PATH/scripts
./launch_emulator.sh
adb pull /system/etc/hosts $HOME
adb root	# Wait a few seconds
adb remount
adb push ~/hosts /system/etc
adb push $ANDROID_PATH/scripts/emulator_setup_net.sh /cache
adb shell
su
sh /cache/emulator_setup_net.sh
exit

```

## Running Ground Data System

### In Terminal 1 run the following commands:

```
export ROS_IP=$(getent hosts llp | awk '{ print $1 }')
export ROS_MASTER_URI=http://${ROS_IP}:11311
echo $ROS_IP

```

## In Terminal 2 Launch simulation:

 - ### Building/Installing guest science manager:

```
    cd $ANDROID_PATH/core_apks/guest_science_manager
    ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleDebug
    adb install -gr activity/build/outputs/apk/activity-debug.apk

```

 - ### Building/Installing simple trajectory:

```
      cd $ANDROID_PATH/gs_examples/test_simple_trajectory
      ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleDebug
      adb install -gr app/build/outputs/apk/app-debug.apk

    /$ANDROID_PATH/scripts/gs_manager.sh start
    cd $SOURCE_PATH/tools/gds_helper/
    python gds_simulator.py

```
## Operating Guest Science Manager:

### Starting it:

- Press any key to grab control
- Select the Guest Science Application (GSA) you are trying to run
- Type b and press Enter to start the GSA
- Press Enter to stop listening for data
- Press any key to get back to the application menu

### Running simple trajectory:

- Type d and press Enter to send a custom guest science command
- Type 1 and press Enter to run the trajectory

### To stop application:

- If needed, press Enter to stop listening for data
- If needed, press any key to get back to the application menu
- Type c and press Enter to stop the GSA
- Press Enter to stop listening for data
- Press any key to get back to the application menu
- Type f and press Enter to exit the GDS simulator
- In the terminal running the simulator, enter Ctrl+c

