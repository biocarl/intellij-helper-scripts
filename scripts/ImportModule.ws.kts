import java.io.*
import java.nio.file.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;


// Specify the link of the root folder of the module you want to clone
val githubUrl = "https://github.com/biocarl/UniverseProject/tree/main/ParallelUniverse/Pluto"

// :::::::::OPTIONAL::::::::::
/*
  Name of the module, if not set takes the one from the original (Example here: Pluto)
 */
val targetModuleName = ""
/*
   Relative path from root directory pointing to the folder where the module should be created,
   if not set creates module in project root
 */
val targetParentFolder = ""

// Execute!
importModule(githubUrl = githubUrl, moduleNameOverride = targetModuleName, targetParentFolder = targetParentFolder)

fun importModule(githubUrl: String, moduleNameOverride: String, targetParentFolder : String) {
    val tmpFolder = "tmp"
    val (cloneUrl, repoName, lastFolderPath) = extractGithubInfo(githubUrl)

    val moduleName = if (moduleNameOverride.isBlank()) lastFolderPath.substringAfterLast('/') else  moduleNameOverride;
    val targetModuleRelativePath = if (targetParentFolder.isBlank()) moduleName else "$targetParentFolder/$moduleName"

    // Clone the repository (but sparse)
    ProcessBuilder("git", "clone", "--depth","1", "--filter=blob:none", "--sparse",cloneUrl, tmpFolder)
            .directory(File("."))
            .inheritIO()
            .start()
            .waitFor()

    // Checkout target module
    ProcessBuilder("git", "sparse-checkout", "set",lastFolderPath)
            .directory(File(tmpFolder))
            .inheritIO()
            .start()
            .waitFor()
    // Copy module in root project
    moveModuleToTarget(tmpFolder+"/"+lastFolderPath, targetModuleRelativePath)
    // Delete original repo
    deleteFolder(tmpFolder);
    // Register module in project
    registerModule(targetModuleRelativePath, moduleName)
}

fun extractGithubInfo(githubUrl: String): Triple<String, String, String> {
    val baseUrl = "https://github.com/"
    val branchUrl = "/tree/"
    val baseUrlIndex = githubUrl.indexOf(baseUrl)
    val branchUrlIndex = githubUrl.indexOf(branchUrl)

    if (baseUrlIndex == -1 || branchUrlIndex == -1) {
        throw IllegalArgumentException("Invalid GitHub URL")
    }

    val repoLink = githubUrl.substring(baseUrlIndex + baseUrl.length, branchUrlIndex)
    val repoName = repoLink.substringAfterLast('/')
    val lastFolderWithBranch = githubUrl.substringAfter("/tree/","")
    val lastFolderPath = lastFolderWithBranch.substringAfter("/","")

    return Triple("https://github.com/$repoLink.git", repoName, lastFolderPath)
}

fun moveModuleToTarget(sourcePath: String, destinationRelativePath: String) {
    val source = Paths.get(sourcePath).toAbsolutePath()
    val destination = Paths.get(destinationRelativePath).toAbsolutePath()
    Files.createDirectories(destination.parent)

    println("source $source")
    println("destination $destination")
    Files.move(source, destination)
}

fun deleteFolder(sourcePath : String){
    val path = File(sourcePath)
    path.deleteRecursively();
}

fun registerModule(targetModuleRelativePath: String, moduleName: String) {
    val rootModulesConfig = Paths.get(".idea/modules.xml")

    // Check if modules.xml file exists
    if (!Files.exists(rootModulesConfig)) {
        println("Error: .idea/modules.xml file not found.")
        return
    }

    // Parse modules.xml file and check if <modules> element exists
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val doc = builder.parse(rootModulesConfig.toFile())
    val modulesElement = doc.getElementsByTagName("modules").item(0)
    if (modulesElement == null) {
        println("Error: <modules> element not found in .idea/modules.xml file.")
        return
    }

    // Create new module element and append it to <modules> element
    val newModuleElement = doc.createElement("module")
    newModuleElement.setAttribute("fileurl", "file://\$PROJECT_DIR\$/$targetModuleRelativePath/$moduleName.iml")
    newModuleElement.setAttribute("filepath", "\$PROJECT_DIR\$/$targetModuleRelativePath/$moduleName.iml")
    modulesElement.appendChild(newModuleElement)

    // Write modified XML back to file
    val source = DOMSource(doc)
    val transformerFactory = TransformerFactory.newInstance()
    val transformer: Transformer = transformerFactory.newTransformer()
    val result = StreamResult(rootModulesConfig.toAbsolutePath().toString())
    transformer.transform(source, result)
    println("Module element added to .idea/modules.xml file successfully.")
}
