# Vestige v3.0 Maven MCP and Updated Optifine Edition

### About
Vestige-v3.0 is based on Maven MCP 1.8.9 which is an updated version of MCP. It has a clean code structure, supports Linux and version control in your projects! (regular old MCP also supports linux macos windows idk why they said that) 

### About the structure
The code is split into two groups: Resources (assets, graphics, shaders etc.) and code.
Libraries are loaded from Maven.

### Setting up workspace
1. Clone the repository
2. Let it setup and index (just wait)
4. Specify project SDK to any Full JDK higher than **Java 8** and set Language Level to **8 Lambdas, type annotations etc.**
5. Once it indexes, the project should be ready to go! :)

### Building
To build a working .jar file, which later can be put to `/versions` in MC folder, you just need to run `mvn clean package` command.
You can also use the Maven menu *on the right side*, or add a new run configuration, and run it from there (my favourite way).
Once the process is complete, artifacts will be in `/target` directory.
There's no requirement to delete MANIFEST from the jar before putting to MC folder.

### Running
To launch the client in the IDE, you need to execute Start.java, **and specify working directory to `./test_run/`**.

### Credits
FontRenderer: "https://github.com/Godwhitelight/FontRenderer"<br>
Lombok: "https://projectlombok.org/"<br>
Maven MCP: "https://github.com/Marcelektro/MavenMCP-1.8.9"<br>
Optifine: "https://optifine.net/copyright"

### DMCA Takedown
Contact me at github.progress070@slmails.com<br>
Please allow up to 10 business days for a reply<br>
I'll take down the resource

An example run configuration.
<img src="https://developers.marcloud.net/i/launchConfig.png"/>
**May 1.8.9 survive!**
