# Sling Dynamic Include in AEMaaCS

This tutorial will quickly allow you to deploy a fork of the AEM Guides WKND project configured for Sling Dynamic Include to understand concepts and try out settings. The example can be deployed to a local AEMaaCS SDK Stack (preferred) or an AEMaaCS Sandbox.

> Skill level: Intermediate to Advanced

Prerequisites

* A local AEMaaCS SDK Author/Publish/Dispatcher stack (preferred) or AEMaaCS Sandbox
* Your preferred Git client
* Visual Studio Code or other IDE
* Ability to build and deploy AEM projects using Maven


## Background
The purpose of Sling Dynamic Include, or SDI, is to dynamically replace sling includes with either server side includes, edge side includes or even JavaScript includes. This can be a very powerful tool, especially in the context of aem's dispatcher, as it pushes cache life granularity to the component level versus the page level.

To consider a use case, a website homepage may be cached in the dispatcher for hours at a time until content is changed or AEMaaCS Pods recycle. But what if there is a requirement to have the three latest news articles from an external platform on the homepage which must update every 10 minutes?

With Sling Dynamic Include, the entire page can be cached with only the more dynamic component being marked for dynamic inclusion. The dynamic section could be set to never be cached, thus requesting rendering from publish on every request. Alternately it could be set with its own short TTL (Time to Live) in the Dispatcher cache. 

## But what about AEMaaCS?
Support for SDI in in AEM as a Cloud Service has been in a grey area of support, with confusion of whether it can be recommended. Some clarity has been added as it's underlying Dispatcher module, mod_include, has been added to Supported Apache Modules in documentation. This does contain a caveat
> mod_include (no directives supported)

Beyond this addition, SDI documentation found in Experience League is targeting AEM 6.5, where there are some unclear differences compared to setting up in AEMaaCS.

## Basic Configuration and Code Example
A branch is prepared for this tutorial at [https://github.com/holmsj/aem-guides-wknd/tree/feature/sling-dynamic-include](https://github.com/holmsj/aem-guides-wknd/tree/feature/sling-dynamic-include). Check this branch out, and open the top level project folder in your IDE

Adding SDI to WKND for this tutorial took place in two commits. The first commit was largely boilerplate:

* The Hello World component was modified to return the current time and date. This is helpful to see that this component is being loaded dynamically and is not cached with the rest of the page.
* As WKND uses the Maven Enforcer plug in, this change required numerous updates to pom file and bundle versions. 
* It is not necesary for this tutorial, but you can optionally (view the commit here)[https://github.com/adobe/aem-guides-wknd/commit/1838b5f05993b72976a96e0ce955bb8227323b70].

The second commit contained the changes specific to enabling Sling Dynamic Include in an AEMaaCS project. It contained 7 changed files, 2 of which are for tutorial purposes. This means SDI can be enabled for AEMaaCS with only 5 changed files! [https://github.com/adobe/aem-guides-wknd/commit/f89ae42ed5b124b81bdc57ff3a812a4d77970641](https://github.com/adobe/aem-guides-wknd/commit/f89ae42ed5b124b81bdc57ff3a812a4d77970641). 

* **pom.xml**
    * Added top level dependency for **org.apache.sling.dynamic-include**
* **all/pom.xml**
    * Use filevault-package-maven-plugin to embed **org.apahce.sling.dynamic-include** and set application specific target for install to AEMaaCS environments.
    * Add dependency for the above package.
* **/dispatcher/src/conf.d/available_vhosts/wknd.vhost** 
    * Add configurations to enable SDI Server Side Includes.
    * Removed Cache-Control header related directives. This is because Edge Side Include is not yet available in the AEMaaCS CDN, so for the purposes of this example we will simply avoid caching HTML outside the Dispatcher layer.
* **/dispatcher/src/conf.dispatcher.d/available_farms/wknd.vhost** *(Tutorial purpose only)*
    * Add allowedClients rules to allow dispatcher flush from localhost. This is for the purpose of localhost testing and should not be deployed to Cloud environments in real world use cases.
* **/dispatcher/src/conf.dispatcher.d/cache/rules.any**
    * Added a rule to disable caching of SDI included components using the ***.nocache.html** selector
* **ui.config/src/main/content/jcr_root/apps/wknd/osgiconfig/config.publish/org.apache.sling.dynamicinclude.Configuration~content.cfg.json**
    * Created OSGI configuration for publish runmode. We will look at this configuration in more detail, but at a high level it is configuring component **wknd/components/helloworld** for Sling Dynamic Include at the Dispatcher layer.
* **ui.content.sample/src/main/content/jcr_root/content/wknd/us/en/.content.xml** *(Tutorial purpose only)*
    * Add Hello World component to WKND /us/en home page sample content. 