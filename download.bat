@echo off
::set Cronet version
chcp 65001
SET CronetVersion="92.0.4515.115"
echo "删除旧文件"
rd/s/q .\cronet\

echo "下载jar"
gsutil -m cp "gs://chromium-cronet/android/%CronetVersion%/Release/cronet/cronet_api.jar" ./cronet/%CronetVersion%/libs/cronet_api.jar
gsutil -m cp "gs://chromium-cronet/android/%CronetVersion%/Release/cronet/cronet_impl_common_java.jar" ./cronet/%CronetVersion%/libs/cronet_impl_common_java.jar
gsutil -m cp "gs://chromium-cronet/android/%CronetVersion%/Release/cronet/cronet_impl_native_java.jar" ./cronet/%CronetVersion%/libs/cronet_impl_native_java.jar
gsutil -m cp "gs://chromium-cronet/android/%CronetVersion%/Release/cronet/cronet_impl_platform_java.jar" ./cronet/%CronetVersion%/libs/cronet_impl_platform_java.jar
echo "下载so"
gsutil -m cp "gs://chromium-cronet/android/%CronetVersion%/Release/cronet/libs/arm64-v8a/libcronet.%CronetVersion%.so" ./cronet/%CronetVersion%/arm64-v8a/libcronet.%CronetVersion%.so
gsutil -m cp "gs://chromium-cronet/android/%CronetVersion%/Release/cronet/libs/armeabi-v7a/libcronet.%CronetVersion%.so" ./cronet/%CronetVersion%/armeabi-v7a/libcronet.%CronetVersion%.so
gsutil -m cp "gs://chromium-cronet/android/%CronetVersion%/Release/cronet/libs/x86_64/libcronet.%CronetVersion%.so" ./cronet/%CronetVersion%/x86_64/libcronet.%CronetVersion%.so
gsutil -m cp "gs://chromium-cronet/android/%CronetVersion%/Release/cronet/libs/x86/libcronet.%CronetVersion%.so" ./cronet/%CronetVersion%/x86/libcronet.%CronetVersion%.so
echo "创建md5"
python filemd5.py

echo "创建并切入分支 latest_branch"
git checkout --orphan latest_branch

echo "添加所有文件"
git add -A

echo "提交改变"
git commit -am "Tiny the git and update cronet %CronetVersion%"

echo "删除 main 分支"
git branch -D main

echo "重命名当前分支为 main"
git branch -m main

echo "强制更新到远程仓库"
git push -f origin main

pause