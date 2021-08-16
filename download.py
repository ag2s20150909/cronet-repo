#!/usr/bin/python3
# -*- coding: utf-8 -*-
import os
import requests

version="93.0.4577.43"
def download(url,path):
    if os.path.exists(path):
        return
    r = requests.get(url=url,stream=True)
    d=path[:path.rfind("/")]
    createDir(d)
    with open(path, "wb") as f:
        for chunk in r.iter_content(chunk_size=1024):
            if chunk:
                f.write(chunk)

def downloadCronet(version):
    cronet_api="https://storage.googleapis.com/chromium-cronet/android/"+version+"/Release/cronet/cronet_api.jar"
    cronet_api_src="https://storage.googleapis.com/chromium-cronet/android/"+version+"/Release/cronet/cronet_api-src.jar"
    download(cronet_api,'')
    
    cronet_common="https://storage.googleapis.com/chromium-cronet/android/"+version+"/Release/cronet/cronet_impl_common_java.jar"
    cronet_common_src="https://storage.googleapis.com/chromium-cronet/android/"+version+"/Release/cronet/cronet_impl_common_java-src.jar"
    
    cronet_native="https://storage.googleapis.com/chromium-cronet/android/"+version+"/Release/cronet/cronet_impl_native_java.jar"
    cronet_native_src="https://storage.googleapis.com/chromium-cronet/android/"+version+"/Release/cronet/cronet_impl_native_java-src.jar"
    
    cronet_platform="https://storage.googleapis.com/chromium-cronet/android/"+version+"/Release/cronet/cronet_impl_platform_java.jar"
    cronet_platform_src="https://storage.googleapis.com/chromium-cronet/android/"+version+"/Release/cronet/cronet_impl_platform_java-src.jar"
    
    arm64-v8a="https://storage.googleapis.com/chromium-cronet/android/"+version+"/Release/cronet/libs/arm64-v8a/libcronet."+version+".so"
    armeabi-v7a="https://storage.googleapis.com/chromium-cronet/android/"+version+"/Release/cronet/libs/armeabi-v7a/libcronet."+version+".so"
    x86_64="https://storage.googleapis.com/chromium-cronet/android/"+version+"/Release/cronet/libs/x86_64/libcronet."+version+".so"
    x86="https://storage.googleapis.com/chromium-cronet/android/"+version+"/Release/cronet/libs/x86/libcronet."+version+".so"