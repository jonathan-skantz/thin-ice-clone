# DD1349 INDA project

### Authors: Jonathan Skantz & Elias Hollstrand

![gif](https://gits-15.sys.kth.se/jskantz/projinda/blob/main/src/images/thin-ice-gameplay.gif)

### Description
A clone of the Club Penguin game "Thin Ice", with the addition of a self-playing mode where the computer calculates the correct solution.

The course consists of ice blocks that the player walks on. The player should from point A to point B and cannot walk on a block they have already walked on. All blocks must not be walked on but the more blocks the more points. Courses are randomly generated.

Screenshot of "Thin Ice":

![image](https://gits-15.sys.kth.se/storage/user/17035/files/b38e07ce-7d9e-4858-b7c3-dcd0d1d06004)

Video of "Thin Ice": https://youtu.be/H8I29P8eiio

### Language
Java

### Framework
Swing

### How to play the game

The goal is to reach the pink block by stepping on as many blocks as possible, turning them into ice (blue).
#### Controls
* Move up: W
* Move down: S
* Move right: D
* Move left: A
* Generate new level: SPACE

### How to run this game

1. Download this repo by cloning it using `git clone git@gits-15.sys.kth.se:jskantz/projinda.git`
2. Navigate to `projinda/src/`
3. Compile all of the files with the command `javac *.java`
4. Run the file `Main.java` using the command `java Main`

### MVP (minimum viable product)
* One level
* Level is represented by squares of different color
* Player should be able to move between squares and complete the level
* Squares that have been walked on cannot be walked on again
* Squares that the player has walked on should change color to blue

### Code and commit message formatting
* Imperative form
* Refer to issues fixed at the end of each commit message

For example: `Add player sprite. Fix #21`
