/*

   ant2ivy 
   -------
   A script to "kick-start" java projects using the ivy plug-in 

   License
   -------

   Copyright 2011 Mark O'Connor

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
//
// Dependencies
// ============

import groovy.xml.MarkupBuilder
import groovy.xml.NamespaceBuilder
import groovy.json.JsonSlurper
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Grapes([
    @Grab(group='org.slf4j', module='slf4j-simple', version='1.6.6') 
])

//
// Classes
// =======

class Ant2Ivy {

    Logger log = LoggerFactory.getLogger(this.class.name);
    String groupId
    String artifactId
    String repoUrl

    Ant2Ivy(groupId, artifactId, repoUrl) {
        this.groupId = groupId
        this.artifactId = artifactId
        this.repoUrl = repoUrl

        log.debug "groupId: {}, artifactId: {}", groupId, artifactId
    }

    //
    // Given a directory, find all jar and search Nexus
    // based on the file's checksum
    //
    // Return a data structure containing the GAV coordinates of each jar
    //
    def search(File inputDir) {
        def results = [:]
        results["found"] = []
        results["missing"] = []

        log.info "Searching: {} ...", repoUrl

        def ant = new AntBuilder()
        ant.fileset(id:"jars", dir:inputDir.absolutePath, includes:"**/*.jar")

        ant.project.references.jars.each {
            def jar = new File(inputDir, it.name)
		
            // Checksum URL
            ant.checksum(file:jar.absolutePath, algorithm:"SHA1", property:jar.name)
            def searchUrl = "${repoUrl}solrsearch/select?q=1:\"${ant.project.properties[jar.name]}\"&rows=20&wt=json"
            log.info "SearchUrl: {}, File: {}", searchUrl, jar.name
            // Search for the first result
			def returnpage = new URL(searchUrl).getText()
			def json = new JsonSlurper().parseText returnpage
            def artifact = json.response["numFound"]
            if (artifact == 1) {
                log.info "Found: {}", jar.name
                results["found"].add([file:jar.name, groupId:json.response.docs[0]['g'], artifactId:json.response.docs[0]['a'], version:json.response.docs[0]['v']])
            }
			
            else {
                log.info "Not Found: {}", jar.name
                results["missing"].add([file:jar.name, fileObj:jar])
            }
        }

        return results
    }

    //
    // Given an input direcory, search for the GAV coordinates 
    // and use this information to write two XML files:
    //
    // ivy.xml          Contains the ivy dependency declarations
    // ivysettings.xml  Resolver configuration
    //
    def generateAnt(File inputDir, File outputDir) {
        outputDir.mkdir()

        def antFile = new File(outputDir, "build.xml")
        def ivyFile = new File(outputDir, "ivy.xml")
        def ivySettingsFile = new File(outputDir, "ivysettings.xml")
        def localRepo = new File(outputDir, "lib")
        def results = search(inputDir)

        //
        // Generate the ant build file
        //
        log.info "Generating ant file: {} ...", antFile.absolutePath
        def antContent = new MarkupBuilder(antFile.newPrintWriter())

        antContent.project(name: "Sample ivy build", default:"resolve", "xmlns:ivy":"antlib:org.apache.ivy.ant" ) {
            target(name:"install", description:"Install ivy") {
                mkdir(dir:"\${user.home}/.ant/lib")
                get(dest:"\${user.home}/.ant/lib/ivy.jar", src:"http://search.maven.org/remotecontent?filepath=org/apache/ivy/ivy/2.2.0/ivy-2.2.0.jar")
            }
            target(name:"resolve", description:"Resolve 3rd party dependencies") {
                "ivy:resolve"()
            }
            target(name:"clean", description:"Remove all build files") {
                "ivy:cleancache"()
            }
        }

        // 
        // Generate the ivy file
        //
        log.info "Generating ivy file: {} ...", ivyFile.absolutePath
        def ivyConfig = new MarkupBuilder(ivyFile.newPrintWriter())

        ivyConfig."ivy-module"(version:"2.0") {
            info(organisation:this.groupId, module:this.artifactId) 
            configurations(defaultconfmapping:"compile->master(default)") {
                conf(name:"compile", description:"Compile dependencies")
                conf(name:"runtime", description:"Runtime dependencies", extends:"compile")
                conf(name:"test", description:"Test dependencies", extends:"runtime")
            }
            dependencies() {
                results.found.each {
                    dependency(org:it.groupId, name:it.artifactId, rev:it.version)
                }
                results.missing.each {
                    dependency(org:"NA", name:it.file, rev:"NA")
                }
            }
        }

        // 
        // Generate the ivy settings file
        //
        log.info "Generating ivy settings file: {} ...", ivySettingsFile.absolutePath
        def ivySettings = new MarkupBuilder(ivySettingsFile.newPrintWriter())
        def ant = new AntBuilder()

        ivySettings.ivysettings() {
            settings(defaultResolver:"maven-repos") 
            resolvers() {
                chain(name:"maven-repos") {
                    // TODO: Make this list of Maven repos configurable
                    ibiblio(name:"central", m2compatible:"true")
                }
                if (results.missing.size() > 0) {
                    filesystem(name:"local") {
                        artifact(pattern:"\${ivy.settings.dir}/${localRepo.name}/[artifact]")
                    }
					
                }
				
            }
            if (results.missing.size() > 0) {
				modules() {
					module(organisation:"NA", resolver:"local")
				}
			}

        }

        //
        // Files which are not identified are saved locally as an ivy repository
        //
        results.missing.each {
             ant.copy(file:it.fileObj.absolutePath, tofile:"${localRepo.absolutePath}/${it.file}")
        }
		print results.missing.size();
    }

    //
    // Using the generated ivy file create a Maven POM file
    //
    def generateMaven(File ivyFile, File outputDir) {
        def ant = new AntBuilder()
        def ivy = NamespaceBuilder.newInstance(ant, "antlib:org.apache.ivy.ant")
        def pomFile = new File(outputDir, "pom.xml")

        ant.property(name:"ivy.pom.version", value:"1.0-SNAPSHOT")

        ivy.makepom(ivyfile:ivyFile.absolutePath, pomFile:pomFile.absolutePath)
    }
}

// 
// Main program
// ============
def cli = new CliBuilder(usage: 'ant2ivy')
cli.with {
    h longOpt: 'help', 'Show usage information'
    g longOpt: 'groupid',    args: 1, 'Module groupid', required: true
    a longOpt: 'artifactid', args: 1, 'Module artifactid', required: true
    s longOpt: 'sourcedir',  args: 1, 'Source directory containing jars', required: true
    t longOpt: 'targetdir',  args: 1, 'Target directory where write ivy build files', required: true
    r longOpt: 'mavenUrl',   args: 1, 'Alternative Maven repository URL'
}
                                                                
def options = cli.parse(args)
if (!options) {
    return
}

if (options.help) {
    cli.usage()
}

def mavenUrl = (options.mavenUrl) ? options.mavenUrl : "http://search.maven.org/"

// 
// Generate ivy configuration
//
def ant2ivy = new Ant2Ivy(options.groupid, options.artifactid, mavenUrl)
def srcDir  = new File(options.sourcedir)
def trgDir  = new File(options.targetdir)

ant2ivy.generateAnt(srcDir, trgDir)
ant2ivy.generateMaven(new File(trgDir, "ivy.xml"), trgDir)

