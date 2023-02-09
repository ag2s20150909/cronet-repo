#bin/sh
echo "fetch release info from https://chromiumdash.appspot.com ..."


echo "$1"
branch="Stable"

lastest_cronet_version=`curl -s "https://chromiumdash.appspot.com/fetch_releases?channel=$branch&platform=Android&num=1&offset=0" | jq .[0].version -r`
lastest_cronet_main_version=${lastest_cronet_version%%\.*}.0.0.0
lastest_cronet_patch_veraion=${lastest_cronet_version##*\.}
lastest_cronet_pre_patch_veraion=${lastest_cronet_version%\.*}

echo "lastest_cronet_version: $lastest_cronet_version"
echo "lastest_cronet_main_version: $lastest_cronet_main_version"
echo "lastest_cronet_pre_patch_veraion: $lastest_cronet_pre_patch_veraion"
echo "lastest_cronet_patch_version: $lastest_cronet_patch_veraion"

function checkVersionExit() {
    local jar_url="https://storage.googleapis.com/chromium-cronet/android/$lastest_cronet_version/Release/cronet/cronet_api.jar"
    statusCode=$(curl -s -I -w %{http_code} "$jar_url" -o /dev/null)
    if [ $statusCode == "404" ];then
        echo "storage.googleapis.com return 404 for cronet $lastest_cronet_version"
        lastest_cronet_version=`curl -s "https://chromiumdash.appspot.com/fetch_releases?channel=$branch&platform=Android&num=1&offset=1" | jq .[0].version -r`
        jar_url="https://storage.googleapis.com/chromium-cronet/android/$lastest_cronet_version/Release/cronet/cronet_api.jar"
        statusCode=$(curl -s -I -w %{http_code} "$jar_url" -o /dev/null)
    fi

    if [ $statusCode == "404" ];then
        echo "storage.googleapis.com return 404 for cronet $lastest_cronet_version"
    fi
}

path=$GITHUB_WORKSPACE/gradle.properties
ls
echo "path: $path"
current_cronet_version=`cat $path | grep CronetVersion | sed s/CronetVersion=//`
echo "current_cronet_version: $current_cronet_version"

if [[  $current_cronet_version == $lastest_cronet_version ]];then
    echo "cronet is already latest"
else
    checkVersionExit
    sed -i s/CronetVersion=.*/CronetVersion=$lastest_cronet_version/ $path
    sed -i s/PROJ_VERSION=.*/PROJ_VERSION=stable-SNAPSHOT/ $path
    #sed "15a* 更新cronet: $lastest_cronet_version" -i $GITHUB_WORKSPACE/app/src/main/assets/updateLog.md
    echo "start download latest cronet"
    chmod +x gradlew
    ./gradlew downloadCronet
    ./gradlew publish
fi