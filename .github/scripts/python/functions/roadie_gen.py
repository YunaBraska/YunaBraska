#!/usr/bin/env python3
import argparse
import json
import os
import subprocess
import yaml

import semver

DEVNULL = subprocess.DEVNULL


def start(args):
    file_name = "catalog-info.yaml"
    with open(file_name, "r") as stream:
        try:
            remote_origin_url = subprocess.check_output(['git', 'config', '--local', 'remote.origin.url']).decode('UTF-8').strip()
            github_slug = remote_origin_url.replace('https://github.com/', '').replace('.git', '')
            print(
                github_slug
                  )
            print(yaml.safe_load(stream))
        except yaml.YAMLError as exc:
            print(exc)


    stream2 = open(file_name, 'r')
    data = yaml.safe_load(stream2)
    print(data['apiVersion'])

    # data['instances'][0]['host'] = '1.2.3.4'
    # data['instances'][0]['username'] = 'Username'
    # data['instances'][0]['password'] = 'Password'
    #
    # with open(file_name, 'w') as yaml_file:
    #     yaml_file.write(yaml.dump(data, default_flow_style=False))
    return 'hello'


def run(args):
    print(start(args))
    exit(0)


if __name__ == '__main__':
    run('adsa')
