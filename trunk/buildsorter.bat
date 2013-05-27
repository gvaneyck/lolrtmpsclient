mkdir build
cd src
javac -d ..\build com\gvaneyck\runesorter\RunePageSorter.java
cd ..\build
jar -cvfm ..\LoLPageSorter.jar ..\LoLPageSorter.MF com
rmdir /S /Q com
cd ..
