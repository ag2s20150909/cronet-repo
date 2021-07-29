@echo off
::set Cronet version
chcp 65001
SET CronetVersion="93.0.4524.3"
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

pause