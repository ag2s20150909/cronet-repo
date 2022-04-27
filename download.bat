@echo off
::set Cronet version
chcp 65001
SET CronetVersion="SS"

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
echo "删除所有本地TAG"
for /f "delims=" %%a in ('git tag -l') do git tag -d %%a
echo "命名新tag"
git tag -a "%CronetVersion%" -m "Cronet Version %CronetVersion%"

echo "强制更新到远程仓库"
git push -f origin main --tags

pause