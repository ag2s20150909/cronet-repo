## Cronet repo

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/ag2s20150909/cronet-repo)](https://github.com/ag2s20150909/cronet-repo/releases)
[![GitHub last commit](https://img.shields.io/github/last-commit/ag2s20150909/cronet-repo)](https://github.com/ag2s20150909/cronet-repo/commits)
[![](https://data.jsdelivr.com/v1/package/gh/ag2s20150909/cronet-repo/badge?style=rounded)](https://www.jsdelivr.com/package/gh/ag2s20150909/cronet-repo)
[![](https://jitpack.io/v/ag2s20150909/cronet-repo.svg)](https://jitpack.io/#ag2s20150909/cronet-repo)

Cronet binary files are
from [Cronet Google Cloud Platform](https://console.cloud.google.com/storage/browser/chromium-cronet/android)

You can find the last cronet version numbert
here:[ChromiumDash](https://chromiumdash.appspot.com/releases?platform=Android)

## Usage

### Stable

Get it from Jitpack

110 version is broken and 111 version need add 
```groovy
implementation 'com.google.protobuf:protobuf-javalite:3.21.12'
```

```groovy
maven { url 'https://jitpack.io' }
```

```groovy
dependencies {
    //All in once
    implementation('com.github.ag2s20150909:cronet-repo:108.0.5359.128') { exclude(group: "org.chromium.net") }

    //or implementation you need
    implementation('com.github.ag2s20150909.cronet-repo:core:108.0.5359.128') { exclude(group: "org.chromium.net") }
    implementation 'com.github.ag2s20150909.cronet-repo:okhttp:108.0.5359.128'
    implementation 'com.github.ag2s20150909.cronet-repo:okhttp-kt:108.0.5359.128'
    implementation 'com.github.ag2s20150909.cronet-repo:glide:108.0.5359.128'
}
```

Get it from Github Action

```groovy
maven { url "https://raw.githubusercontent.com/ag2s20150909/cronet-repo/Stable/repo/" }
maven { url 'https://raw.fastgit.org/ag2s20150909/cronet-repo/Stable/repo/' }
maven { url "https://cdn.staticaly.com/gh/ag2s20150909/cronet-repo/Stable/repo/" }
```

```groovy
def cronet_version = "stable-SNAPSHOT"
implementation("me.ag2s.cronet:core:$cronet_version")
implementation("me.ag2s.cronet:okhttp:$cronet_version")
implementation("me.ag2s.cronet:okhttp-kt:$cronet_version")
implementation("me.ag2s.cronet:glide:$cronet_version")
```

<details>
<summary>Show more branches</summary>

### Beta

```groovy
maven { url "https://raw.githubusercontent.com/ag2s20150909/cronet-repo/Beta/repo/" }
maven { url 'https://raw.fastgit.org/ag2s20150909/cronet-repo/Beta/repo/' }
maven { url "https://cdn.staticaly.com/gh/ag2s20150909/cronet-repo/Beta/repo/" }
```

```groovy
def cronet_version = "beta-SNAPSHOT"
implementation("me.ag2s.cronet:core:$cronet_version")
implementation("me.ag2s.cronet:okhttp:$cronet_version")
implementation("me.ag2s.cronet:okhttp-kt:$cronet_version")
implementation("me.ag2s.cronet:glide:$cronet_version")
```

### Dev

```groovy
maven { url "https://raw.githubusercontent.com/ag2s20150909/cronet-repo/Dev/repo/" }
maven { url 'https://raw.fastgit.org/ag2s20150909/cronet-repo/Dev/repo/' }
maven { url "https://cdn.staticaly.com/gh/ag2s20150909/cronet-repo/Dev/repo/" }
```

```groovy
def cronet_version = "dev-SNAPSHOT"
implementation("me.ag2s.cronet:core:$cronet_version")
implementation("me.ag2s.cronet:okhttp:$cronet_version")
implementation("me.ag2s.cronet:okhttp-kt:$cronet_version")
implementation("me.ag2s.cronet:glide:$cronet_version")
```

### Canary

```groovy
maven { url "https://raw.githubusercontent.com/ag2s20150909/cronet-repo/Canary/repo/" }
maven { url 'https://raw.fastgit.org/ag2s20150909/cronet-repo/Canary/repo/' }
maven { url "https://cdn.staticaly.com/gh/ag2s20150909/cronet-repo/Canary/repo/" }
```

```groovy
def cronet_version = "canary-SNAPSHOT"
implementation("me.ag2s.cronet:core:$cronet_version")
implementation("me.ag2s.cronet:okhttp:$cronet_version")
implementation("me.ag2s.cronet:okhttp-kt:$cronet_version")
implementation("me.ag2s.cronet:glide:$cronet_version")
```

</details>

```kotlin
val cronetEngine: CronetEngine by lazy {
    val builder = MyCronetEngine.Builder(appCtx).apply {
        ....
    }
    builder.build().also {
        //For Glide
        CronetHolder.setEngine(it)
    }
}

```

## Install

#### 1,Get source code

```bash
git clone https://github.com/ag2s20150909/cronet-repo.git
```

#### 2,Edit gradle.properties file

you can edit ```/cronet-repo/gradle.properties```

#### 3,Download Cronet and Apply

```bash

gradlew cronetlib:downloadCronet
```

#### 4,Generate AAR

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



