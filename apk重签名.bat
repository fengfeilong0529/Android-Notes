::�˽ű�Ҫ����SDK-build-tools·��ִ�У�
::PATH:D:\work_dirs\soft_dev\SDK\build-tools\30.0.2

@echo off
::�����豸ip
set /p jksPath=������ǩ���ļ�·����
set /p alias=�����������
set /p apkPath=������Ҫǩ����apk·����

::apksigner sign --ks E:\codeNew\linghao2\linghao\signFile\safeguard.jks  --ks-key-alias key D:\Download\chrome\V2.2.3_293_220616_LH_protected.apk
apksigner sign --ks %jksPath%  --ks-key-alias %alias% %apkPath%