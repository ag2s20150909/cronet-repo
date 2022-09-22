## Cronet repo

 [![GitHub release (latest by date)](https://img.shields.io/github/v/release/ag2s20150909/cronet-repo)](https://github.com/ag2s20150909/cronet-repo/releases)
 [![GitHub last commit](https://img.shields.io/github/last-commit/ag2s20150909/cronet-repo)](https://github.com/ag2s20150909/cronet-repo/commits)
 [![](https://data.jsdelivr.com/v1/package/gh/ag2s20150909/cronet-repo/badge?style=rounded)](https://www.jsdelivr.com/package/gh/ag2s20150909/cronet-repo)

Cronet binary files are from [Cronet Google Cloud Platform](https://console.cloud.google.com/storage/browser/chromium-cronet/android)

You can find the last cronet version numbert here:[ChromiumDash](https://chromiumdash.appspot.com/releases?platform=Android)


## USE

```bash
        maven { url "https://raw.githubusercontent.com/ag2s20150909/cronet-repo/cronet/repo/" }
        maven {url 'https://raw.fastgit.org/ag2s20150909/cronet-repo/cronet/repo/'}
        maven { url "https://cdn.staticaly.com/gh/ag2s20150909/cronet-repo/cronet/repo/" }
```

```bash
    implementation("me.ag2s.cronet:core:0.0.1")
    implementation("me.ag2s.cronet:okhttp:0.0.1")
```


## Install
#### 1,Get source code
```bash
git clone https://github.com/ag2s20150909/cronet-repo.git
```
#### 2,Edit gradle.properties file
you can edit ```/cronet-repo/cronetlib/gradle.properties```

#### 3,Download Cronet and Applay

```bash

gradlew cronetlib:downloadCronet
```
#### 4,Genrate AAR

```bash

gradlew cronetlib:assemble
```



### install Python and gsutil
install [python3](https://www.python.org/downloads/)

If your python dont have pip,install pip.
```python
python -m ensurepip
```
Install gsutil from PyPI
```
pip install gsutil
```

For more infromation see [Gsuti Docs](https://cloud.google.com/storage/docs/gsutil_install)



