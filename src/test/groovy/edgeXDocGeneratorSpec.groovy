import spock.lang.Specification

public class EdgeXDocGeneratorSpec extends Specification {

    def edgeXDocGenerator = null

    def setup() {
        edgeXDocGenerator = Spy(new edgeXDocGenerator())
    }

    def "Test generateDocumentation [Should] generate MD file [When] called with expected arguments"() {
        given:
            def config = [
                    sourcePath: 'vars',
                    excludes  : ''
            ]
            def mockFile = GroovyMock(File)
            def mockFile1 = GroovyMock(File)
            def mockFile2 = GroovyMock(File)
            def mockFile3 = GroovyMock(File)
            mockFile1.name >> "file1.groovy"
            mockFile2.name >> "file2.groovy"
            mockFile3.name >> "file3.groovy"
            edgeXDocGenerator.getFileObject(_) >> mockFile
            edgeXDocGenerator.fetchIndexHeader() >> "IndexHeader"
            edgeXDocGenerator.fetchRequiredFiles(_) >> [mockFile1, mockFile2, mockFile3]
        when:
            edgeXDocGenerator.generateDocumentation(config)
        then:
            1 * edgeXDocGenerator.writeMdFile("index", "IndexHeaderGroovy File 1 Line\nGroovy File 2 Line") >> null
            1 * edgeXDocGenerator.writeMdFile("libraries/file1", "Groovy File 1 MD Content") >> null
            1 * edgeXDocGenerator.writeMdFile("libraries/file2", "Groovy File 2 MD Content") >> null
            1 * edgeXDocGenerator.generateMdFileContent(mockFile1) >> "Groovy File 1 MD Content"
            1 * edgeXDocGenerator.generateMdFileContent(mockFile2) >> "Groovy File 2 MD Content"
            1 * edgeXDocGenerator.generateMdFileContent(mockFile3) >> null
            1 * edgeXDocGenerator.writeFileList(mockFile1) >> "Groovy File 1 Line"
            1 * edgeXDocGenerator.writeFileList(mockFile2) >> "Groovy File 2 Line"
            0 * edgeXDocGenerator.writeFileList(mockFile3)
    }

    def "Test fetchRequiredFiles [Should] fetch files [When] called with config values"() {
        given:
            def config = [
                    sourcePath: 'test_vars',
                    excludes  : ['.*DocGenerator.*']
            ]
            def mockFile1 = GroovyMock(File)
            def mockFile2 = GroovyMock(File)
            def mockFile3 = GroovyMock(File)
            def mockFile4 = GroovyMock(File)
            mockFile1.name >> "file1.groovy"
            mockFile2.name >> "file2.groovy"
            mockFile3.name >> "file3.init"
            mockFile4.name >> "DocGenerator.groovy"
            mockFile4.canonicalPath >> "DocGenerator.groovy"
            def spyFile = Spy(new File("."))
            spyFile.listFiles() >> [mockFile1, mockFile2, mockFile3, mockFile4]
            edgeXDocGenerator.getFileObject(_) >> spyFile
        when:
            def files = edgeXDocGenerator.fetchRequiredFiles(config)
        then:
            files == [mockFile1, mockFile2]
    }

    def "Test checkParameters [Should] throw exception [When] called with missing config values"() {
        given:
            def config = [
                    sourcePath: '',
                    excludes  : ''
            ]
        when:
            edgeXDocGenerator.checkParameters(config)
        then:
            Exception exception = thrown()
            exception.message == 'sourcePath is mandatory'
    }

    def "Test checkParameters [Should] not throw exception [When] called with sourcePath in config"() {
        given:
            def config = [
                    sourcePath: 'vars',
                    excludes  : ''
            ]
        when:
            edgeXDocGenerator.checkParameters(config)
        then:
            noExceptionThrown()
    }

    def "Test writeMdFile [Should] write contents to a MD file [When] called with expected arguments"() {
        given:
            def mockFile = GroovyMock(File)
            edgeXDocGenerator.getFileObject(_) >> mockFile
        when:
            edgeXDocGenerator.writeMdFile("test", "test file contents")
        then:
            1 * mockFile.createNewFile()
            1 * mockFile.write("test file contents")
            1 * mockFile.mkdirs()
            0 * mockFile.getText()
    }

    def "Test generateMdFileContent [Should] generate file contents [When] called with expected arguments"() {
        given:
            def mockFile = GroovyMock(File)
            mockFile.text >> "/**\n # edgeXBuildCApp" + "\n" + " Shared Library to build C projects\n */"
        when:
            def comments = edgeXDocGenerator.generateMdFileContent(mockFile)
        then:
            comments == "\n# edgeXBuildCApp\nShared Library to build C projects\n"
    }

    def "Test getFileObject [Should] returns file object [When] called with expected arguments"() {
        given:
            def mockFile = GroovyMock(File)
        when:
            File file = edgeXDocGenerator.getFileObject("testFile")
        then:
            file.getName() == "testFile"
    }

    def "Test writeFileList [Should] write File index list [When] called with expected arguments"() {
        given:
            def mockFile = GroovyMock(File)
            mockFile.name >> "testFile.groovy"
            def fileName
            edgeXDocGenerator.getFileName(mockFile) >> fileName
        when:
            def fileLine = edgeXDocGenerator.writeFileList(mockFile)
        then:
            fileLine == "- [${fileName}](libraries/${fileName}.md)"
    }

    def "Test fetchIndexHeader [Should] fetches index [When] called with expected arguments"() {
        given:
            def mockFile = GroovyMock(File)
            mockFile.text >> "test file contents"
        when:
            def actualText = edgeXDocGenerator.fetchIndexHeader()
        then:
            "test file contents" == actualText
            1 * edgeXDocGenerator.getFileObject("${edgeXDocGenerator.mdDirName}/templates/header.md") >> mockFile
    }
}