#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

BRANCH_NAME="master"

if ! [ -z "$1" ]
  then
    BRANCH_NAME="$1"
    echo "branch: $BRANCH_NAME"
fi

sed -ie 's?version.*?version : '"$BRANCH_NAME"'?g' requirements.yml

ansible-galaxy install -r ${DIR}/requirements.yml -p . --force -vvvv
rm -rf ${DIR}/.galaxy/
mv ${DIR}/ec2-galaxy ${DIR}/.galaxy