
'''
Created on Oct 10, 2016

@author: gambi
'''
import xml.etree.ElementTree as ET
from xml.dom import minidom
from os.path import os
from distutils.version import LooseVersion

class CUT:
    staticJUnitVersion = '4.12'
    staticJUnitGroupId = 'junit'
    staticJUnitArtifactId = 'junit'
    
    # TODO Avoid hard coded values later
    staticCutProfileData = """
    <profiles>
        <profile>
            <id>local</id>
            <dependencies>
                <dependency>
                    <groupId>jcloudscale</groupId>
                    <artifactId>jcloudscale.core</artifactId>
                    <version>0.5.0-SNAPSHOT</version>
                </dependency>
                <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.12</version>
                    <scope>test</scope>
                </dependency>
            </dependencies>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-surefire-plugin</artifactId>
                        <version>2.17</version>
                        <dependencies>
                            <dependency>
                                <groupId>de.unisaarland.cs.st</groupId>
                                <artifactId>cut</artifactId>
                                <version>1.0-SNAPSHOT</version>
                            </dependency>
                        </dependencies>
                        <configuration>
                            <properties>
                                <property>
                                    <name>jcloudscale.configuration</name>
                                    <value>de.unisaarland.cs.st.cut.config.JCSConfig</value>
                                </property>
                            </properties>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
        <!-- -->
        <profile>
            <id>docker</id>
            <dependencies>
                <dependency>
                    <groupId>jcloudscale</groupId>
                    <artifactId>jcloudscale.core</artifactId>
                    <version>0.5.0-SNAPSHOT</version>
                </dependency>
                <dependency>
                    <groupId>jcloudscale</groupId>
                    <artifactId>jcloudscale.docker</artifactId>
                    <version>0.5.0-SNAPSHOT</version>
                </dependency>
                <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.12</version>
                    <scope>test</scope>
                </dependency>
            </dependencies>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-surefire-plugin</artifactId>
                        <version>2.17</version>
                        <dependencies>
                            <dependency>
                                <groupId>de.unisaarland.cs.st</groupId>
                                <artifactId>cut</artifactId>
                                <version>1.0-SNAPSHOT</version>
                            </dependency>
                        </dependencies>
                        <configuration>
                            <systemProperties>
                                <property>
                                    <name>cut.configuration</name>
                                    <value>at.ac.tuwien.infosys.jcloudscale.vm.docker.DockerCloudPlatformConfiguration</value>
                                </property>
                                <property>
                                    <name>dockerHost</name>
                                    <value>http://134.96.235.133:2375</value>
                                </property>
                                <property>
                                    <name>imageName</name>
                                    <value>alessio/jcs:0.5.0-SNAPSHOT</value>
                                </property>
                                <property>
                                    <name>mq.address</name>
                                    <value>134.96.235.133</value>
                                </property>
                            </systemProperties>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
        <profile>
            <id>cloud-manager</id>
            <dependencies>
                <dependency>
                    <groupId>jcloudscale</groupId>
                    <artifactId>jcloudscale.core</artifactId>
                    <version>0.5.0-SNAPSHOT</version>
                </dependency>
                <dependency>
                    <groupId>jcloudscale</groupId>
                    <artifactId>jcloudscale.cloud-manager-client</artifactId>
                    <version>0.5.0-SNAPSHOT</version>
                </dependency>
                <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.12</version>
                    <scope>test</scope>
                </dependency>
            </dependencies>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-surefire-plugin</artifactId>
                        <version>2.17</version>
                        <dependencies>
                            <dependency>
                                <groupId>de.unisaarland.cs.st</groupId>
                                <artifactId>cut</artifactId>
                                <version>1.0-SNAPSHOT</version>
                            </dependency>
                        </dependencies>
                        <configuration>
                            <systemProperties>
                                <property>
                                    <name>jcloudscale.configuration</name>
                                    <value>de.unisaarland.cs.st.cut.config.JCSConfig</value>
                                </property>
                                <property>
                                    <name>cut.configuration</name>
                                    <value>at.ac.tuwien.infosys.jcloudscale.vm.cloudmanager.CloudManagerPlatformConfiguration</value>
                                </property>
                                <property>
                                    <name>cloudConfigurationClass</name>
                                    <value>at.ac.tuwien.infosys.jcloudscale.vm.docker.DockerCloudPlatformConfiguration</value>
                                </property>
                                <property>
                                    <name>cloud-manager.cp</name>
                                    <value>/Users/gambi/.m2/repository/jcloudscale/jcloudscale.docker/0.5.0-SNAPSHOT/jcloudscale.docker-0.5.0-SNAPSHOT.jar:/Users/gambi/.m2/repository/jcloudscale/jcloudscale.app/0.5.0-SNAPSHOT/jcloudscale.app-0.5.0-SNAPSHOT.jar:/Users/gambi/.m2/repository/jcloudscale/jcloudscale.core/0.5.0-SNAPSHOT/jcloudscale.core-0.5.0-SNAPSHOT.jar:/Users/gambi/.m2/repository/org/aspectj/aspectjrt/1.7.4/aspectjrt-1.7.4.jar:/Users/gambi/.m2/repository/org/apache/activemq/activemq-all/5.6.0/activemq-all-5.6.0.jar:/Users/gambi/.m2/repository/cglib/cglib/2.2.2/cglib-2.2.2.jar:/Users/gambi/.m2/repository/asm/asm/3.3.1/asm-3.3.1.jar:/Users/gambi/.m2/repository/com/espertech/esper/4.6.0/esper-4.6.0.jar:/Users/gambi/.m2/repository/log4j/log4j/1.2.16/log4j-1.2.16.jar:/Users/gambi/.m2/repository/org/antlr/antlr-runtime/3.2/antlr-runtime-3.2.jar:/Users/gambi/.m2/repository/org/antlr/stringtemplate/3.2/stringtemplate-3.2.jar:/Users/gambi/.m2/repository/antlr/antlr/2.7.7/antlr-2.7.7.jar:/Users/gambi/.m2/repository/cglib/cglib-nodep/2.2/cglib-nodep-2.2.jar:/Users/gambi/.m2/repository/org/fusesource/sigar/1.6.4/sigar-1.6.4.jar:/Users/gambi/.m2/repository/com/spotify/docker-client/3.5.12/docker-client-3.5.12-shaded.jar:/Users/gambi/.m2/repository/org/slf4j/slf4j-api/1.7.12/slf4j-api-1.7.12.jar:/Users/gambi/.m2/repository/com/google/guava/guava/18.0/guava-18.0.jar:/Users/gambi/.m2/repository/com/fasterxml/jackson/jaxrs/jackson-jaxrs-json-provider/2.6.0/jackson-jaxrs-json-provider-2.6.0.jar:/Users/gambi/.m2/repository/com/fasterxml/jackson/jaxrs/jackson-jaxrs-base/2.6.0/jackson-jaxrs-base-2.6.0.jar:/Users/gambi/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.6.0/jackson-core-2.6.0.jar:/Users/gambi/.m2/repository/com/fasterxml/jackson/module/jackson-module-jaxb-annotations/2.6.0/jackson-module-jaxb-annotations-2.6.0.jar:/Users/gambi/.m2/repository/com/fasterxml/jackson/datatype/jackson-datatype-guava/2.6.0/jackson-datatype-guava-2.6.0.jar:/Users/gambi/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.6.0/jackson-databind-2.6.0.jar:/Users/gambi/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.6.0/jackson-annotations-2.6.0.jar:/Users/gambi/.m2/repository/org/glassfish/jersey/core/jersey-client/2.19/jersey-client-2.19.jar:/Users/gambi/.m2/repository/javax/ws/rs/javax.ws.rs-api/2.0.1/javax.ws.rs-api-2.0.1.jar:/Users/gambi/.m2/repository/org/glassfish/jersey/core/jersey-common/2.19/jersey-common-2.19.jar:/Users/gambi/.m2/repository/javax/annotation/javax.annotation-api/1.2/javax.annotation-api-1.2.jar:/Users/gambi/.m2/repository/org/glassfish/jersey/bundles/repackaged/jersey-guava/2.19/jersey-guava-2.19.jar:/Users/gambi/.m2/repository/org/glassfish/hk2/osgi-resource-locator/1.0.1/osgi-resource-locator-1.0.1.jar:/Users/gambi/.m2/repository/org/glassfish/hk2/hk2-api/2.4.0-b25/hk2-api-2.4.0-b25.jar:/Users/gambi/.m2/repository/org/glassfish/hk2/hk2-utils/2.4.0-b25/hk2-utils-2.4.0-b25.jar:/Users/gambi/.m2/repository/org/glassfish/hk2/external/aopalliance-repackaged/2.4.0-b25/aopalliance-repackaged-2.4.0-b25.jar:/Users/gambi/.m2/repository/org/glassfish/hk2/external/javax.inject/2.4.0-b25/javax.inject-2.4.0-b25.jar:/Users/gambi/.m2/repository/org/glassfish/hk2/hk2-locator/2.4.0-b25/hk2-locator-2.4.0-b25.jar:/Users/gambi/.m2/repository/org/javassist/javassist/3.18.1-GA/javassist-3.18.1-GA.jar:/Users/gambi/.m2/repository/org/glassfish/jersey/connectors/jersey-apache-connector/2.19/jersey-apache-connector-2.19.jar:/Users/gambi/.m2/repository/org/glassfish/jersey/media/jersey-media-json-jackson/2.19/jersey-media-json-jackson-2.19.jar:/Users/gambi/.m2/repository/org/glassfish/jersey/ext/jersey-entity-filtering/2.19/jersey-entity-filtering-2.19.jar:/Users/gambi/.m2/repository/org/apache/commons/commons-compress/1.9/commons-compress-1.9.jar:/Users/gambi/.m2/repository/org/apache/httpcomponents/httpclient/4.5/httpclient-4.5.jar:/Users/gambi/.m2/repository/org/apache/httpcomponents/httpcore/4.4.1/httpcore-4.4.1.jar:/Users/gambi/.m2/repository/commons-logging/commons-logging/1.2/commons-logging-1.2.jar:/Users/gambi/.m2/repository/commons-codec/commons-codec/1.9/commons-codec-1.9.jar:/Users/gambi/.m2/repository/com/github/jnr/jnr-unixsocket/0.8/jnr-unixsocket-0.8.jar:/Users/gambi/.m2/repository/com/github/jnr/jnr-ffi/2.0.3/jnr-ffi-2.0.3.jar:/Users/gambi/.m2/repository/com/github/jnr/jffi/1.2.9/jffi-1.2.9.jar:/Users/gambi/.m2/repository/com/github/jnr/jffi/1.2.9/jffi-1.2.9-native.jar:/Users/gambi/.m2/repository/org/ow2/asm/asm/5.0.3/asm-5.0.3.jar:/Users/gambi/.m2/repository/org/ow2/asm/asm-commons/5.0.3/asm-commons-5.0.3.jar:/Users/gambi/.m2/repository/org/ow2/asm/asm-analysis/5.0.3/asm-analysis-5.0.3.jar:/Users/gambi/.m2/repository/org/ow2/asm/asm-tree/5.0.3/asm-tree-5.0.3.jar:/Users/gambi/.m2/repository/org/ow2/asm/asm-util/5.0.3/asm-util-5.0.3.jar:/Users/gambi/.m2/repository/com/github/jnr/jnr-x86asm/1.0.2/jnr-x86asm-1.0.2.jar:/Users/gambi/.m2/repository/com/github/jnr/jnr-constants/0.8.7/jnr-constants-0.8.7.jar:/Users/gambi/.m2/repository/com/github/jnr/jnr-enxio/0.9/jnr-enxio-0.9.jar:/Users/gambi/.m2/repository/com/github/jnr/jnr-posix/3.0.12/jnr-posix-3.0.12.jar:/Users/gambi/.m2/repository/commons-lang/commons-lang/2.6/commons-lang-2.6.jar:/Users/gambi/.m2/repository/org/bouncycastle/bcpkix-jdk15on/1.52/bcpkix-jdk15on-1.52.jar:/Users/gambi/.m2/repository/org/bouncycastle/bcprov-jdk15on/1.52/bcprov-jdk15on-1.52.jar:/Users/gambi/.m2/repository/junit/junit/4.11/junit-4.11.jar:/Users/gambi/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:/Users/gambi/.m2/repository/jcloudscale/jcloudscale.app/0.5.0-SNAPSHOT/jcloudscale.app-0.5.0-SNAPSHOT-tests.jar</value>
                                </property>
                                <property>
                                    <name>dockerHost</name>
                                    <value>http://134.96.235.133:2375</value>
                                </property>
                                <property>
                                    <name>imageName</name>
                                    <value>alessio/jcs:0.5.0-SNAPSHOT</value>
                                </property>
                                <property>
                                    <name>mq.address</name>
                                    <value>134.96.235.133</value>
                                </property>
                            </systemProperties>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
    """
    
    cutProfileData = ''
    
    def __init__(self, profileDataFile=None):
        if profileDataFile is None or not os.path.exists(profileDataFile):
            self.cutProfileData = self.staticCutProfileData
#             print('Using default values for cutProfileData')
        else:
#             print('Using provided values for cutProfileData')
            self.cutProfileData = open(profileDataFile).read()
    
    
    def hasProfilesElement(self, root):
        """Check if the given XML tree (pom.xml) contains the profiles element in it
        """
        profilesNode = root.find('{http://maven.apache.org/POM/4.0.0}profiles')
        if profilesNode is None:
#             print('Cannot find profiles node')
            return False
        
#         print("Found Profiles Node")
#         print (profilesNode)
        return True 

    def parsePomFile(self, pomFile):
        """ Return the root element
        """
         # This shall remove the annoing ns0: namespace from the output
        # See http://stackoverflow.com/questions/10757702/python-2-7-type-object-elementtree-has-no-attribute-register-namespace
        ET.register_namespace("", "http://maven.apache.org/POM/4.0.0")
        # TODO Check that pomFile exists and is a pom.xml file 
        return ET.parse(pomFile).getroot()
        
    def injectCutProfiles(self, pomFile):
        """Inject into a pom file the CUT profiles local and docker
        """
        root = self.parsePomFile(pomFile)
        
        if not self.hasProfilesElement(root):
            # Add the Profiles node to Root
            profilesNode = ET.Element('{http://maven.apache.org/POM/4.0.0}profiles')
            root.append(profilesNode)
    
            # This is a look but can be a single call
        for profiles in root.findall('{http://maven.apache.org/POM/4.0.0}profiles',):
            for cutProfileNode in ET.fromstring(self.cutProfileData):
                profiles.append(cutProfileNode)
        
        
        return root
    
#     def versiontuple(self, v):
#         """http://stackoverflow.com/questions/11887762/compare-version-strings
#         """
#         return tuple(map(int, (v.split("."))))
    
    def updateJUnitVersion(self, pomFile):
        """Given the pom file update the JUnit version to 4.12 if smaller
        """
        root = self.parsePomFile(pomFile)
        # Ideally here I will use the xpath syntax but using [text()='junit'] results in a invalid predicate error
        # Anyway the getParent is not there so we need necessarily to work at dependency node level
         
        for node in root.findall('.//{http://maven.apache.org/POM/4.0.0}dependency'): # This will match also the inner inner nodes, for example, exclusiosn
            junitDependencyNode = None
#             print( node.tag )
            for cNode in node.getiterator():  # This includes the node itself !
                # print( "  " + cNode.tag + " -- " + cNode.text + " " + self.staticJUnitGroupId)
                if cNode.tag == '{http://maven.apache.org/POM/4.0.0}groupId' and cNode.text == self.staticJUnitGroupId:
                    junitDependencyNode = node
                    break
                
            if junitDependencyNode is None:
                continue
            
            # Build a list of content of child nodes - the value of group id must be there
            if not self.staticJUnitGroupId in [elem.text for elem in junitDependencyNode]:
                continue
            
            for cNode in junitDependencyNode.findall('{http://maven.apache.org/POM/4.0.0}version'):
                jUnitVersion = cNode.text
                if LooseVersion(jUnitVersion) < LooseVersion(self.staticJUnitVersion):
#                 if self.versiontuple(jUnitVersion) < self.versiontuple(self.staticJUnitVersion):
                    print ("Update version of the node " + jUnitVersion + " -> " + self.staticJUnitVersion + " for node " + junitDependencyNode.tag)
                    cNode.text = self.staticJUnitVersion
                    break
#                 else:
#                     print("Version is Ok" + " for node " + junitDependencyNode.tag)
        return root

    def prettify(self, elem):
        """Return a pretty-printed XML string for the Element.
        """
        
        return ET.tostring(elem, encoding='utf8', method='xml')
    
def main():
     # display some lines
    if __name__ == "__main__": main()
    
