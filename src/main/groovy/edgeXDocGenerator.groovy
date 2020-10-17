import groovy.io.FileType
import groovy.transform.Field
import java.util.regex.Pattern

@Field mdDirName = "docs_src"

def config = [
        sourcePath: args[0],
        excludes  : args[1].replaceAll("\\[", "").replaceAll("\\]", "").split(", ")
]

generateDocumentation(config)

def generateDocumentation(config) {
    checkParameters(config)
    def groovyFiles = fetchRequiredFiles(config)
    def indexFileContent = fetchIndexHeader()
    def generatedFiles = []

    for (file in groovyFiles){
        def mdFileContent = generateMdFileContent(file)
        if (mdFileContent == null){
            continue
        }
        generatedFiles << writeFileList(file)
        writeMdFile("libraries/${getFileName(file)}", mdFileContent)
        println "Generated MD file for ${file.name}"
    }

    indexFileContent += generatedFiles.join('\n')
    writeMdFile("index", indexFileContent)
}

def checkParameters(config) {
    if (!config.sourcePath){
        throw new Exception("sourcePath is mandatory")
    }
}

def fetchRequiredFiles(config) {
    def files = []
    def dir = getFileObject(config.sourcePath)
    dir.eachFileRecurse(FileType.FILES) { file ->
        if (!file.name.contains("groovy")){
            return
        }
        def excludeMatch = false
        for (excludeRegex in config.excludes){
            excludeMatch = file.canonicalPath ==~ excludeRegex
            if (excludeMatch){
                break
            }
        }
        if (excludeMatch){
            return
        }
        files << file
    }
    return files
}

def fetchIndexHeader() {
    return getFileObject("${mdDirName}/templates/header.md").text
}

def writeFileList(file) {
    def mdFileName = getFileName(file)
    return "- [${mdFileName}](libraries/${mdFileName}.md)"
}

def generateMdFileContent(file) {
    String commentRegex = "(\\/\\*\\*[\\d\\D]*?\\*\\/)"
    def pattern = Pattern.compile(commentRegex)
    def matchers = pattern.matcher(file.text)
    def comment
    if (matchers.find()){
        comment = matchers.group().replaceAll("^/\\*\\*", "").replaceAll(".*?\\*+/\$", "").stripIndent(1)
    }
    return comment
}

def getFileName(file) {
    return file.name.replace(".groovy", "")
}

def writeMdFile(fileName, fileContent) {
    def mdDir = getFileObject(mdDirName)
    if (!mdDir.exists()){
        mdDir.mkdirs()
    }

    def file = getFileObject("${mdDirName}/${fileName}.md")
    file.createNewFile()
    file.write fileContent
}

def getFileObject(fileName) {
    new File(fileName)
}

