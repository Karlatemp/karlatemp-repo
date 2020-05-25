@echo off
git add -A
git commit -m %*
git push coding master
git push origin master
