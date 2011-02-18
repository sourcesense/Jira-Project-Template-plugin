You have successfully created a plugin using the JIRA plugin archetype. What to do now:

1. CUSTOMISE THE PLUGIN

- Run 'mvn eclipse:eclipse' to generate an Eclipse project file.
- Edit pom.xml. Add information about your project, its developers and your organisation.
- Add dependencies as necessary
- Edit the plugin descriptor, src/main/resources/atlassian-plugin.xml. Add or modify plugin modules in your project.
- Edit the plugin code in src/main/java/ or the unit tests in src/test/java/.

2. BUILD THE PLUGIN

Building with your plugin with Maven is really easy:

- Run 'mvn compile' to compile the plugin.
- Run 'mvn test' to run the unit tests.
- Run 'mvn integration-test' to run the integration tests.
- Run 'mvn package' to produce the JAR.

For the above commands to skip the various test phases add one of the following options to the command:

    -Dmaven.test.skip=true - skips both unit and integration tests
    -Dmaven.test.unit.skip=true - skips unit tests
    -Dmaven.test.it.skip=true - skips integration tests

XML data that is usually imported into the JIRA instance by the tests should be placed in:

    src/test/xml/ directory provided
    
Packages containing integration tests should start with it. For instance:
    it.com.atlassian.jira.plugins.fisheye.TestSomething
    it.com.atlassian.jira.plugins.calendar.TestSomethingElse

Hence these classes should go under src/test/java/it/directory

The following system properties are available to control the behaviour of the integration test harness:

		tomcat.installer.url - path to the tomcat zip file to install. Default: http://repository.atlassian.com/maven2/org/apache/tomcat/apache-tomcat/5.5.20/apache-tomcat-5.5.20-jdk14.zip
		
		cargo.wait - if this is set o true - tomcat will be started up and then cargo would suspend allowing you to manually access this JIRA from the browser.
		
		jira.version - version of JIRA to compile and test against. default is 3.10.2
		
		jira.data.version - version of the test resource bundle that contains the bre basic JIRA configuration data for the integration test environment. These versions mimic the actual JIRA versions. However we might only modify and release this project for the reasons of non-backwards compatibility of the new versions of JIRA. Therefore not every version of JIRA will have a corresponding version of the resource bundle. By default this is set to 3.10 which should be migrated and work correctly with the newer versions. 
		
		jira.test-lib.version - version of jira-func-tests JAR to compile and test against. default is 3.10-DEV
		
		http.port - port on which this instance of JIRA will be accessible on. Default: 8989
		
		jira.url - base URL for this instance of JIRA. Default: http://localhost:{http.port}/jira/
		rmi.port

 
Please remove this file before releasing your plugin.
