@echo on  
setlocal  

set BastionPath=%~dp0..\bastion-jsp
set SummerPath=%~dp0..\bastion-summer
set FrameworkPath=%~dp0..\summer-framework

set OutFile=WebRoot/js/version.js
set SummerOutFile=src/main/java/com/ceit/bastion/Version.java
set FrameworkOutFile=src/main/java/com/ceit/bootstrap/Version.java

REM ------- bastion 撤销版本文件修改，pull最新版本

cd "%BastionPath%"
git checkout -- %OutFile%
git pull

REM ------- summer 撤销版本文件修改，pull最新版本

cd "%SummerPath%"
git checkout -- %SummerOutFile%
git pull

REM ------- framework 撤销版本文件修改，pull最新版本

cd "%FrameworkPath%"
git checkout -- %FrameworkOutFile%
git pull

REM ------- framework git 版本  先覆盖，再追加
cd "%FrameworkPath%"

echo var framework_git_version = '$WCREV=7$' ;>"%BastionPath%\%OutFile%" 
echo var framework_git_logcount = '$WCLOGCOUNT$' ;>>"%BastionPath%\%OutFile%" 
echo var framework_git_datetime = '$WCDATE$' ;>>"%BastionPath%\%OutFile%" 

GitWCRev "%FrameworkPath%" "%BastionPath%\%OutFile%" "%BastionPath%\%OutFile%" 
GitWCRev "%FrameworkPath%" "%FrameworkPath%\%FrameworkOutFile%" "%FrameworkPath%\%FrameworkOutFile%" 

echo Git版本信息已更新到 "%BastionPath%\%OutFile%" 

REM ------- bastion git 版本  先覆盖，再追加 -----------------------
cd "%BastionPath%"
echo var bastion_git_version = '$WCREV=7$' ;>>"%BastionPath%\%OutFile%" 
echo var bastion_git_logcount = '$WCLOGCOUNT$' ;>>"%BastionPath%\%OutFile%" 
echo var bastion_git_datetime = '$WCDATE$' ;>>"%BastionPath%\%OutFile%" 

GitWCRev "%BastionPath%" "%BastionPath%\%OutFile%" "%BastionPath%\%OutFile%" 

echo Git版本信息已更新到 "%BastionPath%\%OutFile%" 
 
REM ------- summer git 版本 -------
cd "%SummerPath%"
echo var summer_git_version = '$WCREV=7$' ;>>"%BastionPath%\%OutFile%" 
echo var summer_git_logcount = '$WCLOGCOUNT$' ;>>"%BastionPath%\%OutFile%" 
echo var summer_git_datetime = '$WCDATE$' ;>>"%BastionPath%\%OutFile%" 

GitWCRev "%SummerPath%" "%BastionPath%\%OutFile%" "%BastionPath%\%OutFile%" 
GitWCRev "%SummerPath%" "%SummerPath%\%SummerOutFile%" "%SummerPath%\%SummerOutFile%" 

echo Git版本信息已更新到 "%BastionPath%\%OutFile%" 


REM -------  编译时间 -------

echo var build_datetime = '%date:~0,10% %time%' >> "%BastionPath%\%OutFile%" 

echo 编译时间信息已更新到 "%BastionPath%\%OutFile%"
