# Astrobee
		 	 	 		
			
				
					
## Running on your own machine
					
You can also run the program on your own machine. 

This provides a procedure to set up the Astrobee simulator. 
					

### Requirements
					
The following requirements are needed to set up a simulation environment on your machine.
					
64-bit processor
8GBRAM(16GBRAM recommended)
Ubuntu16.04(64-bitversion)(http://releases.ubuntu.com/16.04/)
					
### Setting up the Astrobee Robot Software
					
					
Run the follwing script in the Ubuntu terminal
								
```
#!/bin/bash

# Author: Mizanul Hoq Chowdhury, MIT

sudo apt-get install build-essential git
export ASTROBEE_WS=$HOME/astrobee

git clone https://github.com/nasa/astrobee.git $ASTROBEE_WS/src
pushd $ASTROBEE_WS/src
git submodule update --init --depth 1 description/media
popd

git submodule update --init --depth 1 submodules/android

pushd $ASTROBEE_WS
cd src/scripts/setup
./add_ros_repository.sh
sudo apt-get update
cd debians
./build_install_debians.sh
cd ../
./install_desktop_packages.sh
sudo rosdep init
rosdep update
popd

export WORKSPACE_PATH=$ASTROBEE_WS
export INSTALL_PATH=$ASTROBEE_WS/install

source /opt/ros/kinetic/setup.sh
cd /src/astrobee
catkin clean --setup-files
ls -la $ASTROBEE_WS/src/cmake
CMAKE_PREFIX_PATH=$CMAKE_PREFIX_PATH:$ASTROBEE_WS/src/cmake
cd $ASTROBEE_WS
./src/scripts/configure.sh -l -F -D -p -T $INSTALL_PATH -w $WORKSPACE_PATH
catkin build --status-rate 0.01


```
					
			
				
### Run Astrobee Simulator				

```
pushd $BUILD_PATH
source devel/setup.bash
popd
roslaunch astrobee sim.launch dds:=false robot:=sim_pub rviz:=true

```				
				
			
			 			
