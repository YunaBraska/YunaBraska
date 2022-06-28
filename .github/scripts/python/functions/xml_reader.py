#!/usr/bin/env python3
import argparse
import os
import re

import lxml.etree as etree

filter_pjv = [
    '*//artifactId[text()="maven-compiler-plugin"][1]/parent::*//source',
    '*//artifactId[text()="maven-compiler-plugin"][1]/parent::*//target',
    '*//properties/java-version',
    '*//properties/java.version',
]


def parse_args():
    default_xml = os.path.join(os.getcwd(), "pom.xml")
    parser = argparse.ArgumentParser(description="XML Reader which uses XPath. Mainly build to read pom files")
    parser.add_argument(
        '-f', '--file',
        metavar='',
        type=str,
        required=False,
        nargs='?',
        const=default_xml,
        default=default_xml,
        help='XML file to read - default [' + default_xml + ']'
    )
    parser.add_argument(
        '-xp', '--xpath',
        metavar='',
        action='append',
        required=False,
        nargs='+',
        # default=default_filter,
        default=[],
        help='XPath(s) to find specific elements text - example [*//properties/java-version]'
    )
    parser.add_argument(
        '-pjv', '--pjv',
        metavar='',
        required=False,
        nargs='?',
        type=bool,
        const=True,
        help='Predefined XPath for Pom.JavaVersion - default [' + filter_pjv[0] + ']'
    )
    return parser.parse_args()


def start(args):
    if not os.path.isfile(args.file):
        print(f'Invalid XML file [{args.file}]')
        exit(1)

    if args.pjv:
        result = resolve_xpath(args.file, filter_pjv)
    elif not args.pjv and not args.xpath:
        print(f'Missing arguments see `--help` for more')
        exit(1)
    else:
        result = resolve_xpath(args.file, args.xpath)

    if result is not None:
        print(result.text)
        exit(0)
    else:
        print(f'Result [{result}]')
        exit(1)


def resolve_xpath(file, x_paths):
    doc = etree.fromstring(re.sub(' xmlns="[^"]+"', '', open(file, 'r').read()).encode())
    for x_path in concatenate(x_paths):
        results = doc.xpath(x_path)
        if results and results[0].text:
            if results[0].text.startswith("${"):
                return resolve_xpath(file, [
                    "*//properties/" + results[0].text.removeprefix("${").removesuffix("}"),
                    "*//" + results[0].text.removeprefix("${").removesuffix("}")
                ])
            else:
                return results[0]
    return None


def concatenate(array):
    result = []
    for item in array:
        if type(item) is type([]):
            for i in item:
                result.append(i)
        result.append(item)
    return result


if __name__ == '__main__':
    start(parse_args())
