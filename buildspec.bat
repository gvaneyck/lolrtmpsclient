mkdir build
cd src
javac -d ..\build com\gvaneyck\spectate\SpectateAnyone.java
cd ..\build
jar -cvfm ..\SpectateAnyone.jar ..\SpectateAnyone.MF com
rmdir /S /Q com
cd ..
