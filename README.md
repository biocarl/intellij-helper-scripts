# IntelliJ Helper Scripts


## A loose collection of scripts which automate certain tasks in IntellJ

### [Script 1](scripts/ImportModule.ws.kts): Import single module from multi-module repository

- This is a Kotlin Worksheet Script you need to place in the root folder of your IntelliJ project.
- For running it you need to right-click on the file (not in the Playround-Play-Button) and click Play
- If the sources etc. still do not work, just delete the `*.iml` file and import module from existing sources. This is often the case for Maven projects - read [here](https://stackoverflow.com/a/36542677) on how to do this




### For the providing side
- In order to make this work you need a `.iml` in each module (for multi-module repo you need to make sure that those are not on .gitignore)
