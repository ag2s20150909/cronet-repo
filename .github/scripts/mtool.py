#!/usr/bin/python3
# -*- coding: utf-8 -*-
from datetime import datetime, timezone
import os
import requests
import hashlib 

#github 时间字符串转utc datetime
def s2time(s):
    #s='2021-07-28T04:10:57Z'
    time=datetime.strptime(s,'%Y-%m-%dT%H:%M:%S%z')
    return time
    #now=datetime.now(tz=timezone.utc)
    #print((now-time).seconds<86164)
def getMd5(path):
    return hashlib.md5(open(path,'rb').read()).hexdigest()
def getSha1(path):
    return hashlib.sha1(open(path,'rb').read()).hexdigest()
def createDir(path):
    if not os.path.exists(path):
        os.makedirs(path)


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


def genMode(basePath,vsesion,name,jarurl,srcurl):
    createDir(basePath)
    createDir(basePath+"/"+vsesion)
    createDir(basePath+"/"+vsesion+"/"+name)
    download(jarurl,basePath+"/"+vsesion+"/"+name+"/"+name+".jar")
    download(srcurl,basePath+"/"+vsesion+"/"+name+"/"+name+"-sources.jar")
    
