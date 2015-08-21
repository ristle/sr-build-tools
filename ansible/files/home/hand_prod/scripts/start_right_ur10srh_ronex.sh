#!/bin/bash
source ~/workspace/devel/setup.bash
source $(rosstack find sr_config)/../bashrc/env_variables.bashrc
roslaunch sr_robot_launch right_srhand_ur10arm.launch sim:=false hand_serial:=1178 eth_port:=eth1_eth0
