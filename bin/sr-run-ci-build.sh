#!/usr/bin/env bash

export toolset_branch=$1
export server_type=$2
export tags_list=$3

git clone https://github.com/shadow-robot/sr-build-tools.git -b "$toolset_branch" sr-build-tools
sudo apt-get update
sudo apt-get install python-dev libxml2-dev libxslt-dev python-pip lcov wget -y
sudo pip install ansible gcovr
cd ./sr-build-tools/ansible

if [ "shippable" == $server_type ]; then
    export extra_variables="shippable_repo_dir=$SHIPPABLE_REPO_DIR  shippable_is_pull_request=$PULL_REQUEST codecov_secure=$CODECOV_TOKEN"
    sudo ansible-playbook -v -i "localhost," -c local docker_site.yml --tags "shippable,$tags_list" -e "$extra_variables"
else
    echo Not supported server type $server_type
fi