
# How to run HLP Guest Science Trajectory in Simulator

## Assumption:

1.	Assumes initial Android emulator is setup through https://github.com/nasa/astrobee_android/blob/master/emulator.md
  a.	Android Virtual Device has already been added to Android Studio (name: Nexus_5_API_25)
  b.	Guest Science Manager is default running when Android Studio is launched
  c.	HLP, MLP, LLP IP addresses have been set in /etc/hosts and home/freeflyer_android/scripts/emulator_setup.sh
  d.	assumes adb is installed
2.	assumes Astrobee Simulator setup accomplished through astrobee_env.sh script (custom script to set environment variables)

## Open Terminal 1 

In Terminal 1 Launch Android Studio
```
	android-studio/bin/studio.sh
  
```  

## Open Terminal 2

In Terminal 2 Launching Android Emulator

```
. astrobee_env.sh
export ANDROID_PATH=$HOME/freeflyer_android
export EMULATOR=$HOME/Android/Sdk/tools/emulator
export AVD="Nexus_5_API_25"
cd $ANDROID_PATH/scripts
./launch_emulator.sh
  
```

## Open Terminal 3

In Terminal 3 set up Android network

```
export ANDROID_PATH=$HOME/freeflyer_android
adb pull /system/etc/hosts $HOME
adb root
adb remount
adb push ~/hosts /system/etc
adb push $ANDROID_PATH/scripts/emulator_setup_net.sh /cache
adb shell
su
sh /cache/emulator_setup_net.sh
exit	
ping -c3 hlp
ping -c3 llp
exit
ping -c3 llp
ping -c3 hlp
  
```	

## Open Terminal 4

In Terminal set up Guest Science Manager, set up simple Guest Science trajectory

```
. astrobee_env.sh

```
Note:

Only required to initially set up guest science manager on Android emulator
  ```
  
export ANDROID_PATH=$HOME/freeflyer_android
cd $ANDROID_PATH/core_apks/guest_science_manager
ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleDebug
adb install -gr activity/build/outputs/apk/activity-debug.apk
  
  ```
	
Skip to here if guest science manager has already been set up and want to move on to selecting GS file

```

cd $ANDROID_PATH/gs_examples/test_simple_trajectory
ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleDebug
adb install -gr app/build/outputs/apk/app-debug.apk

```

## Open Terminal 5

In Terminal 5 set up simulator
	
 ```
export ROS_IP=$(getent hosts llp | awk '{ print $1 }')
export ROS_MASTER_RUI=http://${ROS_IP}:11311
echo $ROS_IP
. astrobee_env.sh
roslaunch astrobee sim.launch robot:=sim_pub gviz:=true

```

## Open Terminal 6

In Terminal 6 set up guest science commander, run guest science trajectory

```
. astrobee_env.sh
export ROS_IP=$(getent hosts llp | awk '{ print $1 }')
export ROS_MASTER_RUI=http://${ROS_IP}:11311
echo $ROS_IP
export ANDROID_PATH=$HOME/freeflyer_android
$ANDROID_PATH/scripts/gs_manager.sh start
cd freeflyer/tools/gds_helper
python gds_simulator.py

```

Then follow Guest Science Manager Steps In Console
	

