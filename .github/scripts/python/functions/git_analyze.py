#!/usr/bin/env python3
import argparse
import json
import os
import subprocess

import semver

DEVNULL = subprocess.DEVNULL


# TODO:
#   custom 'dependencies'
#   custom 'tags'
#   custom 'workflow'
#   custom 'documentation'

def parse_args():
    parser = argparse.ArgumentParser(description="Git analyzer (.github/version.txt will overwrite [tag_new])")
    parser.add_argument(
        '-e', '--encoding',
        metavar='',
        type=str,
        required=False,
        nargs='?',
        default='UTF-8',
        const='UTF-8',
        help='CLI encoding default [UTF-8]'
    )
    parser.add_argument(
        '-o', '--output',
        metavar='',
        type=str,
        required=False,
        nargs='?',
        default='ALL',
        const='ALL',
        help='Outputs the value from the given key',
    )
    parser.add_argument(
        '-tf', '--tag_fallback',
        metavar='',
        type=str,
        required=False,
        nargs='?',
        default=None,
        const=None,
        help='Tag fallback if no tag was found default [0.0.0]'
    )
    parser.add_argument(
        '-i', '--increase',
        metavar='',
        type=str,
        required=False,
        nargs='?',
        const='patch',
        choices=['major', 'minor', 'patch', 'rc', 'meta', 'last'],
        help='SemVer increase version strategy [\'major\', \'minor\', \'patch\', \'rc\', \'meta\'] default [patch]'
    )
    parser.add_argument(
        '-p', '--do_update_pom',
        metavar='',
        type=str,
        required=False,
        nargs='?',
        const='true',
        choices=['true', 'false', 'tag_needed', 'has_changes'],
        help='Update pom version'
             ' with [tag_new]'
             ' default [false]'
             ' values [\'true\', \'false\', \'tag_needed\', \'has_changes\']',
    )
    parser.add_argument(
        '-c', '--do_commit',
        metavar='',
        type=str,
        required=False,
        nargs='?',
        const='true',
        choices=['true', 'false', 'has_changes', 'tag_needed'],
        help='Commit'
             ' with [commit_msg]'
             ' default [false]'
             ' values [\'true\', \'false\', \'tag_needed\', \'has_changes\']',
    )
    parser.add_argument(
        '-t', '--do_tag',
        metavar='',
        type=str,
        required=False,
        nargs='?',
        const='true',
        choices=['true', 'false', 'has_changes', 'tag_needed'],
        help='Commit'
             ' with [tag_new]'
             ' default [false]'
             ' values [\'true\', \'false\', \'tag_needed\', \'has_changes\']',
    )
    parser.add_argument(
        '-s', '--set_tag',
        metavar='',
        type=str,
        required=False,
        nargs='?',
        default=None,
        const=None,
        help='if valid value: triggers [do_update_pom, do_commit, do_tag]',
    )
    return parser.parse_args()


def start(args):
    result = {}
    changes = set()
    result['encoding'] = args.encoding if args.increase is not None else 'UTF-8'
    result['output'] = args.output
    result['tag_needed'] = False
    result['tag_set'] = args.set_tag
    result['tag_increase'] = args.increase if args.increase is not None else 'patch'
    result['tag_fallback'] = args.tag_fallback if args.tag_fallback is not None else '0.0.0'
    result['do_pom_update'] = args.do_update_pom if args.do_update_pom is not None else 'false'
    result['do_commit'] = args.do_commit if args.do_commit is not None else 'false'
    result['do_tag'] = args.do_tag if args.do_tag is not None else 'false'
    result['branch'] = subprocess.check_output([
        "git", "rev-parse", "--abbrev-ref", "HEAD"
    ]).decode(result['encoding']).strip()
    subprocess.call(["git", "add", "."], stderr=DEVNULL, stdout=DEVNULL)
    result['sha_latest'] = subprocess.check_output(["git", "rev-parse", "HEAD"]).decode(result['encoding']).strip()
    git_status = subprocess.check_output(["git", "status", "--porcelain"]).decode(result['encoding'])
    result['has_changes'] = git_status.strip() != ""

    set_tag_sha_latest(result)

    if result['sha_latest'] != result['sha_latest_tag']:
        add_changes(changes, subprocess.check_output([
            "git", "diff", result['sha_latest'], result['sha_latest_tag'], "--name-only"
        ]).decode(result['encoding']), result)
        read_diff(result['encoding'], changes, result)

    add_changes(changes, git_status, result)

    handle_set_version(changes, result)

    if 'tag_new' not in result.keys():
        result['tag_new'] = tag_increase(result)
    result['changes'] = list(changes)
    result['commit_msg'] = "\n".join(result['changes'])

    do_pom_update(result)
    do_commit(result)
    do_tag(result)

    if result['output'] in result.keys():
        return result[result['output']]
    else:
        return json.dumps(dict(sorted(result.items())))


def handle_set_version(changes, result):
    try:
        new_version = semver.start(argparse.Namespace(version=result['tag_set'], increase=None, output='result'))
        if new_version:
            changes.add("updated version")
            result['tag_new'] = new_version
            result['tag_needed'] = True
            result['has_changes'] = True
            result['do_tag'] = "true"
            result['do_commit'] = "true"
            result['do_pom_update'] = "true"
    except:
        pass


def set_tag_sha_latest(result):
    if subprocess.call(["git", "describe", "--tags", "--abbrev=0"], stderr=DEVNULL, stdout=DEVNULL):
        result['tag_latest'] = result['tag_fallback']
        result['sha_latest_tag'] = result['sha_latest']
    else:
        result['tag_latest'] = subprocess.check_output([
            "git", "describe", "--tags", "--abbrev=0"
        ]).decode(result['encoding']).strip()
        result['sha_latest_tag'] = subprocess.check_output([
            "git", "rev-list", "-n", "1", result['tag_latest']
        ]).decode(result['encoding']).strip()


def tag_increase(result):
    try:
        return semver.start(argparse.Namespace(
            increase=result['tag_increase'] if result['tag_increase'] is not None and result[
                'tag_increase'] != "" else "patch",
            output='result',
            version=result['tag_latest'] if result['tag_latest'] is not None and result['tag_latest'] != "" else "0.0.0"
        ))
    except Exception as e:
        print(str(e))
        exit(1)


def do_pom_update(result):
    pom_file = os.path.join(os.getcwd(), "pom.xml")
    mvn_cmd = os.path.join(os.getcwd(), "mvnw.cmd") if os.name == 'nt' else os.path.join(os.getcwd(), "mvnw")
    if true_or_key(result, result['do_pom_update']) and os.path.isfile(pom_file) and os.path.isfile(mvn_cmd):
        subprocess.call(
            [mvn_cmd, "versions:set", "-DnewVersion=" + result['tag_new'], "-DgenerateBackupPoms=false"],
            stderr=DEVNULL, stdout=DEVNULL)


def do_commit(result):
    # TODO: hardcode set version
    # TODO: git squash https://stackoverflow.com/questions/33901565/how-can-i-squash-a-range-of-git-commits-together-given-starting-and-ending-shas
    if true_or_key(result, result['do_commit']):
        subprocess.call(["git", "add", "."], stderr=DEVNULL, stdout=DEVNULL)
        subprocess.call(
            ["git", "commit", "-m", result['commit_msg']], stderr=DEVNULL, stdout=DEVNULL)


def do_tag(result):
    if true_or_key(result, result['do_tag']):
        subprocess.call(
            ["git", "tag", result['tag_new']], stderr=DEVNULL, stdout=DEVNULL)


def true_or_key(result, value):
    if value is not None and (
            (value in result.keys() and str(result[value]).lower() == 'true')
            or str(value).lower() == 'true'
    ):
        return True
    else:
        return False


def read_diff(encoding, changes, result):
    diff = subprocess.check_output([
        "git", "log", result['sha_latest'] + "..." + result['sha_latest_tag'], "--pretty=format:%s"
    ]).decode(encoding).strip()
    for line in diff.splitlines():
        changes.add(line)


def add_changes(changes, changes_log, result):
    for line in changes_log.splitlines():
        line = " " + line
        if " pom.xml" in line or " gradle.build" in line or " .gradle" in line or " gradle" in line or " gradlew" in line or " settings.gradle" in line or " .mvn" in line or " mvnw" in line:
            changes.add("updated dependencies")
            result['tag_needed'] = True
        elif " src/main" in line or " src/test" in line:
            result['tag_needed'] = True
        elif " .github/version.txt" in line:
            version_file = os.path.join(os.getcwd(), ".github/version.txt")
            if os.path.isfile(version_file):
                with open(version_file) as file:
                    result['tag_new'] = file.readline().strip()
                    if result['tag_latest'] != result['tag_new']:
                        result['tag_needed'] = True
                        changes.add("updated version")
        elif " .github" in line:
            changes.add("updated workflow")
        elif line.endswith(".md"):
            changes.add("updated documentation")


def run(args):
    print(start(args))
    exit(0)


if __name__ == '__main__':
    run(parse_args())
