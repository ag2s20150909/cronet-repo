#!/usr/bin/python3
# -*- coding: utf-8 -*-
import os
import requests
import re
import mtool
from Item import Item

from bs4 import BeautifulSoup 



def getNewVersions():
    ls=[]
    url="https://github.com/chromium/chromium/tags"
    html = requests.get(url=url).content
    bs = BeautifulSoup(html,"html.parser")
    for item in bs.find_all("div",class_='Box-row'):
        title=item.find("h4",class_='commit-title').find('a').text.strip()
        des=item.find('div',class_='commit-desc').find('pre').text.strip()
        time=item.find('relative-time')['datetime']
        m=Item(title,time,des)
        m.genAPI()
        print(m.getApiUrl())
        ls.append(m)
        #print(title)
        #print(des)
        #print(time)
        ##print(item)
    return ls
print(getNewVersions())



#testTime()


