#!/usr/bin/python3
# -*- coding: utf-8 -*-
import os
import hashlib 

def getFileList():
    fs=[]
    dirpath='./cronet/'
    for root,dirs,files in os.walk(dirpath):
        for file in files:
            if(file.endswith('.so')):
                fs.append(os.path.join(root,file))
    return fs
def saveMD5(path):
    file=open(path+".js",mode='w',encoding='utf-8')
    file.write(hashlib.md5(open(path,'rb').read()).hexdigest())
    file.flush()
    file.close()
    
for path in getFileList():
    saveMD5(path)