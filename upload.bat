@echo off
chcp 65001

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

echo "强制更新到远程仓库"
git push -f origin main

pause