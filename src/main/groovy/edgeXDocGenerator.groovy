import groovy.io.FileType
import java.util.regex.Pattern

mdDirName = "docs_src"

def config = [
    sourcePath: args[0],
    excludes  : args[1].replaceAll("\\[", "").replaceAll("\\]", "").split(", ")
]

generateDocumentation(config)

def generateDocumentation(config){
    checkParameters(config)
    def groovyFiles = fetchRequiredFiles(config)
    def indexFileContent = fetchIndexHeader()
    def generatedFiles = []

    for(file in groovyFiles) {
        def mdFileContent = generateMdFileContent(file)

        if(mdFileContent == null) {
            continue
        }

        generatedFiles << writeFileLine(file)
        writeMdFile("libraries/${getFileName(file)}", mdFileContent)

        println "Generated MD file for ${file.name}"
    }

    indexFileContent += generatedFiles.join('\n')
    writeMdFile("index", indexFileContent)
}

def checkParameters(config) {
    if(!config.sourcePath){
        throw new Exception("sourcePath is mandatory")
    }
}

def fetchRequiredFiles(config) {
    def files = []
    def dir = new File(config.sourcePath)
    dir.eachFileRecurse(FileType.FILES) { file ->
        if (!file.name.contains("groovy")){
            return
        }
        def excludeMatch = false
        for (excludeRegex in config.excludes){
            excludeMatch = file.canonicalPath ==~ excludeRegex
            if(excludeMatch){
                break
            }
        }
        if(excludeMatch){
            return
        }
        files << file
    }
    return files
}

def fetchIndexHeader() {
    return new File("${mdDirName}/templates/header.md").text
}

def writeFileLine(file) {
    def mdFileName = getFileName(file)
    return "- [${mdFileName}](libraries/${mdFileName}.md)"
}

def generateMdFileContent(file) {
    String commentRegex = "(\\/\\*\\*[\\d\\D]*?\\*\\/)"
    def pattern = Pattern.compile(commentRegex)
    def matchers = pattern.matcher(file.text)
    def comment
    if (matchers.find()) {
        comment = matchers.group().replaceAll("^/\\*\\*","").replaceAll(".*?\\*+/\$","").stripIndent(1)
    }
    return comment
}

def getFileName(file) {
    return file.name.replace(".groovy", "")
}

def writeMdFile(fileName, fileContent) {
    def mdDir = new File(mdDirName)
    if (!mdDir.exists()) {
        mdDir.mkdirs()
    }

    def file = new File( "${mdDirName}/${fileName}.md")
    file.createNewFile()
    file.write fileContent
}

