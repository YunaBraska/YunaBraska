#!/usr/bin/env python3
import argparse
import re
import json

# e.g. [1.2.3-rc.1+30]
SEMANTIC_PATTERN = re.compile(
    "^(?P<major>0|[1-9]\d*)\.(?P<minor>0|[1-9]\d*)\.(?P<patch>0|[1-9]\d*)(?:-(?P<prerelease>(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+(?P<buildmetadata>[0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$")
SEMANTIC_INT = re.compile("(?P<str>.*?)?(?P<int>(\d*$))")


def parse_args():
    parser = argparse.ArgumentParser(description="Version updater")
    parser.add_argument(
        '-v', '--version',
        metavar='',
        type=str,
        required=False,
        nargs='?',
        default='0.0.0',
        help='Input SemVer to process'
    )
    parser.add_argument(
        '-i', '--increase',
        metavar='',
        type=str,
        required=False,
        nargs='?',
        const='patch',
        choices=['major', 'minor', 'patch', 'rc', 'meta', 'last'],
        help='SemVer increase version strategy [\'major\', \'minor\', \'patch\', \'rc\', \'meta\']'
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
    return parser.parse_args()


def parse_sem_ver(args):
    if m := SEMANTIC_PATTERN.match(args.version[args.version.lower().startswith("v") and 1:]):
        sem_ver = {}
        m_meta = SEMANTIC_INT.match(group(m, 'buildmetadata', ''))
        m_release = SEMANTIC_INT.match(group(m, 'prerelease', ''))
        sem_ver['original'] = args.version
        sem_ver['major'] = group_int(m, 'major')
        sem_ver['minor'] = group_int(m, 'minor')
        sem_ver['patch'] = group_int(m, 'patch')
        sem_ver['rc_str'] = group(m_release, 'str')
        sem_ver['rc_int'] = to_int(group_int(m_release, 'int'))
        sem_ver['meta_str'] = group(m_meta, 'str')
        sem_ver['meta_int'] = to_int(group_int(m_meta, 'int'))
        sem_ver['rc'] = concat(sem_ver['rc_str'], sem_ver['rc_int'])
        sem_ver['meta'] = concat(sem_ver['meta_str'], sem_ver['meta_int'])
        sem_ver['output'] = args.output
        return sem_ver
    raise Exception(f'Invalid semantic version [{args.version}] see [https://semver.org]')


def concat(*texts):
    result = ''
    for text in texts:
        if text:
            result = result + str(text)
    if result:
        return result
    return None


def group_int(matcher, name, fallback=None):
    result = group(matcher, name)
    if result:
        return int(result)
    return fallback


def group(matcher, name, fallback=None):
    if matcher and name in matcher.groupdict():
        result = matcher.group(name)
        if result is None:
            return fallback
        return result
    return fallback


def count_up(sem_ver, name):
    order = ('major', 'minor', 'patch', 'rc', 'meta')
    index = order.index(name)

    if name in ('rc', 'meta'):
        if sem_ver[name] is None:
            sem_ver[name + '_str'] = to_string(sem_ver[name + '_str'], name + '.')
        sem_ver[name + '_int'] = to_int(sem_ver[name + '_int'], 0) + 1
    else:
        sem_ver[name] = sem_ver[name] + 1
    for i, zero in enumerate(order):
        if i > index and sem_ver[zero]:
            if zero in ('rc', 'meta'):
                sem_ver[zero] = None
                sem_ver[zero + '_str'] = None
                sem_ver[zero + '_int'] = None
            else:
                sem_ver[zero] = 0


def start(args):
    if args.version is not None:
        sem_ver = parse_sem_ver(args)

        if args.increase:
            count_up(sem_ver, args.increase)

        add_result(sem_ver)

        if args.output == 'ALL' or args.output == 'all':
            return json.dumps(sem_ver)
        elif args.output in sem_ver.keys():
            return json.dumps(sem_ver[args.output])
        else:
            return sem_ver['result']


def add_result(sem_ver):
    result = f"{sem_ver['major']}.{sem_ver['minor']}.{sem_ver['patch']}"
    if sem_ver['rc_str'] or sem_ver['rc_int']:
        result = result + '-' + to_string(sem_ver['rc_str'], '') + to_string(sem_ver['rc_int'], '')
    if sem_ver['meta_str'] or sem_ver['meta_int']:
        result = result + '+' + to_string(sem_ver['meta_str'], '') + to_string(sem_ver['meta_int'], '')
    sem_ver['result'] = result


def to_int(opt, fallback=None):
    if opt:
        return int(opt)
    return fallback


def to_string(opt, fallback=None):
    if opt:
        return str(opt)
    return fallback


def run(args):
    try:
        print(start(args))
        exit(0)
    except Exception as e:
        print(str(e))
        exit(1)


if __name__ == '__main__':
    run(parse_args())
