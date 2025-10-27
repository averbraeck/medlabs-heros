# medlabs-heros

## 5. medlabs-heros code

### 5.1. Generic setup of the code

The code has been developed in java, and builds heavily on the DSOL simulation library for fast and efficient execution of large-scale object-oriented simulation models. medlabs-heros is a small shell around the medlabs library, which contains the logic for executing activity-based disease modeling in cities. The medlabs-heros library adds the specifics for the Covid-19 disease transmission model and disease progression model, as well as some extra person types and policies. Some input files are different from the standard medlabs input files, so a specific model factory (`ConstructHerosModel`) is available to instantiate the model. See the [literature list](6-literature.md) for further references for the medlabs library and the DSOL simulation library. 

The medlabs and DSOL libraries build on further libraries that are used. A partial dependency stack of the of the libraries can be depicted as follows:

<table>
  <tr><td>HERoS library for Covid-19 spread in cities (https://www.heros-project.eu/)</td></tr>
  <tr><td>MEDLABS disease transmission library (https://github.com/averbraeck/medlabs)</td></tr>
  <tr><td>DSOL simulation library (https://github.com/averbraeck/dsol / https://simulation.tudelft.nl)</td></tr>
  <tr><td>djutils (https://github.com/averbraeck/djutils / https://djutils.org)</td></tr>
  <tr><td>djunits (https://github.com/averbraeck/djunits / https://djunits.org)</td></tr>
  <tr><td>djutils-base (https://github.com/averbraeck/djutils-base / https://djutils.org)</td></tr>
</table>


### 5.2. Maven usage

Using [Apache Maven](https://maven.apache.org/), all libraries are resolved automatically for the HERoS project library. There is an 'executable jar' in the folder `jar` that contains the heros library plus all libraries on which it is dependent, as well as a set of input files for The Hague to run the model on 490,000 citizens.


### 5.3. Java version

HERoS and the libraries on which it is dependent, use a Java version 17 or higher. The library is typically used with an openjdk version 17.

For **Windows**, install OpenJDK version 17, 21 or 25 (an LTS = Long Term Support version). Download the zip for Java 17, 21 or 25 at [https://jdk.java.net/archive/](https://jdk.java.net/archive/) for your operating system. All code has been developed and tested with Java version 17, but should run using later Java versions as well. The best way to install Java is to unpack the Java zip in a folder without spaces in the folder name, e.g., C:\app\jdk17. Make sure to add Java to the current 'Path' (on Windows-10 or Windows-11 go to Windows Settings - System - About and click 'Advanced Systems Settings' on the right. Click 'Environment Variables' in the 'Systems Properties' screen. Edit the 'Path' entry and add `C:\app\jdk17\bin` as an entry (adapt for your chosen location and Java version). You can move the entry to before 'C:\Windows\system32' to override a Java client in Windows. Add or modify an entry `JAVA_HOME` and set the value to `C:\app\jdk17` (adapt for your chosen location and Java version). You can test whether Java works by opening a Command prompt (CMD) and typing `java -version`. If Java responds with with the correct version, the installation has succeeded.

For **MacOS**, install OpenJDK version 17, 21 or 25, e.g., using the following instruction: [https://stackoverflow.com/questions/69875335/macos-how-to-install-java-17](https://stackoverflow.com/questions/69875335/macos-how-to-install-java-17) or [https://docs.oracle.com/en/java/javase/17/install/installation-jdk-macos.html](https://docs.oracle.com/en/java/javase/17/install/installation-jdk-macos.html) or [https://www.codejava.net/java-core/install-openjdk-17-on-macos](https://www.codejava.net/java-core/install-openjdk-17-on-macos).

For **Debian / Ubuntu** versions of Linux, use the command `sudo apt install openjdk-17-jdk` to install OpenJDK version 17. Adapt for later versions of Java.

For **CentOS / RedHat** versions of Linux, use the command `sudo yum install java-17-openjdk-devel` or `sudo dnf install java-17-openjdk-devel` to install OpenJDK version 17. If you want to know which java installations are available on CentOS / RedHat, type: `yum search jdk` or `dnf search jdk`, and choose the one you want to install.


### 5.4. IDE: Eclipse

