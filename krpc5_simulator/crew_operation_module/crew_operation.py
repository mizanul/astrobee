#! /usr/bin/env python
# coding:UTF-8

import os
import json
import rospy
import rosparam
import subprocess
from std_msgs.msg import Bool
from ff_msgs.msg import GuestScienceData

TARGET_ITEM_NAMESPACE = '/target_item/robot_description'
TARGET_ITEM_MODEL_NAME = 'target_item_model'
GAZEBO_TARGET_ITEM_PATH =f'{os.path.dirname(__file__)}/model_gaz.urdf'
GAZEBO_TARGET_ITEM_POS = ['11.143', '-6.1607', '4.8654']
RVIZ_TARGET_ITEM_PATH = f'{os.path.dirname(__file__)}/model_rviz.urdf'

class CrewOparationProvider():
  def __init__(self):
    self.NODE_NAME         = 'kibo_rpc_crew_operation'
    self.GDS_MSG_TOPIC     = '/gs/data'
    self.GS_MSG_COMPLETE     = 'rounding is complete'

  def start(self) -> None:
    rospy.init_node(self.NODE_NAME)
    self.start_subscriber()
    rospy.spin()

  def start_subscriber(self):
    rospy.Subscriber(self.GDS_MSG_TOPIC, GuestScienceData, self.gs_callback, queue_size=1)
    rospy.loginfo(f"{self.NODE_NAME} Ready")

  # Callback from GDS messages.
  def gs_callback(self, data) -> None:
    # from binary of data to json
    request = json.loads(data.data)

    # When data status is "rounding is complete", Spawn model
    rospy.loginfo(request)
    if 'status' in request and request['status'] == self.GS_MSG_COMPLETE:
      rospy.loginfo('spawn qr model is done')
      self.spawn_qr_model()
    else:
      rospy.loginfo(f'spawn qr model is not done. data.status={request}')

  # Spawn qr model
  def spawn_qr_model(self) -> None:
    rospy.loginfo('exec spawn_target_item')
    # spawn model to rviz
    subprocess.run(["rosparam", "set", TARGET_ITEM_NAMESPACE, "-t", RVIZ_TARGET_ITEM_PATH])

    # rvizのスポーンが完了する前にgazeboのスポーンが始まるとrviz上にオブジェクトが表示されないため
    # rvizのスポーンが完了するまで待機する
    rospy.sleep(1.0)

    # spawn model to gazebo
    rosparam.set_param(TARGET_ITEM_NAMESPACE, open(GAZEBO_TARGET_ITEM_PATH).read())
    subprocess.run(["rosrun", "gazebo_ros", "spawn_model", "-urdf", "-param", TARGET_ITEM_NAMESPACE, "-model", 
      TARGET_ITEM_MODEL_NAME, "-x", GAZEBO_TARGET_ITEM_POS[0], "-y", GAZEBO_TARGET_ITEM_POS[1], "-z", GAZEBO_TARGET_ITEM_POS[2]])

def main():
  phaseProvider = CrewOparationProvider()

  try:
    phaseProvider.start()
  except rospy.ROSInterruptException:
    # For [ctrl+c]
    pass

if __name__ == '__main__':
  main()
