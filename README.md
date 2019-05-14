![Coopr Logo](coopr-docs/docs/source/_images/coopr_logo_fullcolor.png)

## [Coopr](http://cask.co): Modern Cluster Management

Coopr is cluster management software that manages clusters on public, private
clouds. Clusters created with Coopr utilize templates of any hardware and
software stack, from simple standalone LAMP-stack servers and traditional application
servers like JBoss, to full Apache Hadoop clusters comprised of thousands of nodes.
Clusters can be deployed across many cloud providers (Rackspace, Joyent, and OpenStack)
while utilizing common SCM tools (Chef and scripts).


## Releases

### Current:
   * [Codename: Ursa Major, 0.9.9](http://docs.cask.co/coopr/0.9.9/en/release-notes/index.html)

### Older:
   * [Codename: Centaurus, Theme: multi-tenancy, 0.9.8](http://docs.cask.co/coopr/0.9.8/en/release-notes/index.html)

## Getting Started

You can build a standalone version of Coopr that will run on your machine. It requires Java 6 or Java 7 to run the server, Node v0.10.26 or higher for the UI, and Ruby 1.9.0p0 or higher for the provisioner.
```
  > git clone https://github.com/cdapio/coopr.git
  > cd coopr/coopr-standalone
  > git submodule init
  > git submodule update
  > mvn clean package assembly:single -DskipTests
```

The build places a zip of the standalone version of Coopr into the target directory. Unzip it and follow the instructions in the README.md file to start up Coopr on your own machine. It comes pre-packaged with templates for CDAP, Docker, Hadoop, LAMP, MEAN and MongoDB. In order to create an actual cluster, you will need an account with a supported provider (AWS, DigitalOcean, Google, Joyent, Openstack or Rackspace), and perform a couple steps to setup up Coopr to use the right credentials to integrate with your provider. Follow the [Quickstart Guide](http://docs.coopr.io/coopr/current/en/guide/quickstart/index.html#getting-started), which steps through an example of adding workers to your tenant, configuring a provider, and creating a Hadoop cluster.

## Documentation

To learn more about Coopr, here are the resources available:
   * [User Guide](http://docs.coopr.io/coopr/current/en/index.html) - How to install and setup Coopr, as well as how administrators and users can use Coopr
   * [Technical Document](http://docs.coopr.io/coopr/current/en/implementation.html) - How it works
   * [REST Endpoints](http://docs.coopr.io/coopr/current/en/rest/index.html) - REST APIs for managing your Coopr instance
   * [Javadocs](http://docs.coopr.io/coopr/current/en/javadocs/index.html) - Server javadocs
   * Release Documentation
      * [Documentation 0.9.9](http://docs.coopr.io/coopr/current/en/release-notes/index.html)

## Contributing to Coopr

Are you interested in making Coopr better? Our development model is a simple
pull-based model with a consensus building phase, similar to the Apache's voting process.
If you want to help make Coopr better, by adding new features, fixing bugs, or
suggesting improvements to something that's already there, here's how you can contribute:

 * Fork Coopr into your own GitHub repository
 * Create a topic branch with an appropriate name
 * Work on your favorite feature to your content
 * Once you are satisifed, create a pull request by going to the cdapio/coopr project.
 * Address all the review comments
 * Once addressed, the changes will be committed to the cdapio/coopr repository.

Bugs and suggestions should be made by filing a Jira at https://issues.cask.co/browse/COOPR.

## License

   Copyright © 2014-2016 Cask Data, Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
software except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the
License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied. See the License for the specific language governing permissions
and limitations under the License.
