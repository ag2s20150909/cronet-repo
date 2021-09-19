#!/usr/bin/python3
# -*- coding: utf-8 -*-
import os
import requests
import mtool

class Item:
    def __init__(self, version, time,des):
        
        self.version = version
        self.time =time
        self.des=des
        self.soName="libcronet.%s.so" %(version)
        self.mavenDir="../../maven/me/ag2s/cronet/%s/" %(version)
        self.baseUrl="https://storage.googleapis.com/chromium-cronet/android/%s/Release/cronet/" %(version)
        mtool.createDir(self.mavenDir)
    def getApiUrl(self):
        #https://storage.googleapis.com/chromium-cronet/android/94.0.4590.0/Release/cronet/cronet_api.jar
        return self.baseUrl+"cronet_api.jar"
    def getApiSrcUrl(self):
        #https://storage.googleapis.com/chromium-cronet/android/94.0.4590.0/Release/cronet/cronet_api.jar
        return self.baseUrl+"cronet_api-src.jar"
    def genAPI(self):
        mtool.genMode('../../maven/me/ag2s/cronet',self.version,'api',self.getApiUrl(),self.getApiSrcUrl())
        
    def getCommonUrl(self):
        #https://storage.googleapis.com/chromium-cronet/android/94.0.4590.0/Release/cronet/cronet_api.jar
        return self.baseUrl+"cronet_impl_common_java.jar"
    def getCommonSrcUrl(self):
        #https://storage.googleapis.com/chromium-cronet/android/94.0.4590.0/Release/cronet/cronet_api.jar
        return self.baseUrl+"cronet_impl_common_java-src.jar"
        
    def getNativeUrl(self):
        #https://storage.googleapis.com/chromium-cronet/android/94.0.4590.0/Release/cronet/cronet_api.jar
        return self.baseUrl+"cronet_api.jar"
    def getNativeSrcUrl(self):
        #https://storage.googleapis.com/chromium-cronet/android/94.0.4590.0/Release/cronet/cronet_api.jar
        return self.baseUrl+"cronet_api.jar"
        
     