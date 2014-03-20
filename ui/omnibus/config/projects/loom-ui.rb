
name "loom-ui"
maintainer "Continuuity"
homepage "http://github.com/continuuity/loom"

replaces        "loom-ui"
install_path    "/opt/loom/ui"
#build_version   Omnibus::BuildVersion.new.semver
build_version "0.9.5"
build_iteration 2

# creates required build directories
dependency "preparation"

# loom-provisioner dependencies/components
# dependency "somedep"
dependency "loom-ui"

# version manifest file
dependency "version-manifest"

exclude "\.git*"
exclude "bundler\/git"
