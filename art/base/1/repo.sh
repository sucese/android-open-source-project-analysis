#!/bin/bash   
#FileName  jkYishon.sh  
PATH=~/bin:$PATH   
repo init -u git://aosp.tuna.tsinghua.edu.cn/android/platform/manifest -b android-7.1.1_r1 
repo sync   
while [ $? = 1 ]; do   
echo "================sync failed, re-sync again ====="   
sleep 3   
repo sync   
done 