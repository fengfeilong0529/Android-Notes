::此脚本要放在SDK-build-tools路径执行：
::PATH:D:\work_dirs\soft_dev\SDK\build-tools\30.0.2

@echo off
::输入设备ip
set /p jksPath=请输入签名文件路径：
set /p alias=请输入别名：
set /p apkPath=请输入要签名的apk路径：

::apksigner sign --ks E:\codeNew\linghao2\linghao\signFile\safeguard.jks  --ks-key-alias key D:\Download\chrome\V2.2.3_293_220616_LH_protected.apk
apksigner sign --ks %jksPath%  --ks-key-alias %alias% %apkPath%