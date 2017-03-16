#!/bin/bash   
#FileName  jkYishon.sh  
PATH=~/bin:$PATH   
repo init -u https://aosp.tuna.tsinghua.edu.cn/platform/manifest -b android-2.3_r1 
repo sync   
while [ $? = 1 ]; do   
echo "================sync failed, re-sync again ====="   
sleep 3   
repo sync   
done 