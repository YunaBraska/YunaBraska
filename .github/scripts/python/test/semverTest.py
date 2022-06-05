import argparse
import unittest.mock
from unittest.mock import patch

from functions import semver


class MyTestCase(unittest.TestCase):

    @patch('builtins.print')
    def test_invalid_semver(self, mock_print):
        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase=None, print=None, version='1.2.3@rc.4+m5'))
        mock_print.assert_called_with('Invalid semantic version [1.2.3@rc.4+m5] see [https://semver.org]')
        self.assertEqual(cli.exception.code, 1)

    @patch('builtins.print')
    def test_valid_semver(self, mock_print):
        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase=None, print=None, version='1.2.3-rc.4+m5'))
        mock_print.assert_called_with('1.2.3-rc.4+m5')
        self.assertEqual(cli.exception.code, 0)

    @patch('builtins.print')
    def test_valid_semver_with_v(self, mock_print):
        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase=None, print=None, version='v1.2.3-rc.4+m5'))
        mock_print.assert_called_with('1.2.3-rc.4+m5')
        self.assertEqual(cli.exception.code, 0)

    @patch('builtins.print')
    def test_print_semver(self, mock_print):
        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase=None, print=True, version='1.2.3-alpha1'))
        self.assertEqual(cli.exception.code, 0)
        mock_print.assert_called_with("sem_ver [{'original': '1.2.3-alpha1', 'major': 1, 'minor': 2, 'patch': 3, "
                                      "'rc_str': 'alpha', 'rc_int': 1, 'meta_str': '', 'meta_int': None, "
                                      "'rc': 'alpha1', 'meta': None, 'result': '1.2.3-alpha1'}]")

    # TestMap:
    #  1.2.3-rc.4+m5 -> increase major = 2.0.0
    #  1.2.3-rc.4+m5 -> increase minor = 1.3.0
    #  1.2.3-rc.4+m5 -> increase patch = 1.2.4
    #  1.2.3-rc.4+m5 -> increase rc    = 1.2.3-rc.5
    #  1.2.3-rc.4+m5 -> increase meta  = 1.2.3-rc.4+m6
    @patch('builtins.print')
    def test_increase(self, mock_print):
        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase='major', print=None, version='1.2.3-rc.4+m5'))
        mock_print.assert_called_with("2.0.0")
        self.assertEqual(cli.exception.code, 0)

        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase='minor', print=None, version='1.2.3-rc.4+m5'))
        mock_print.assert_called_with("1.3.0")
        self.assertEqual(cli.exception.code, 0)

        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase='patch', print=None, version='1.2.3-rc.4+m5'))
        mock_print.assert_called_with("1.2.4")
        self.assertEqual(cli.exception.code, 0)

        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase='rc', print=None, version='1.2.3-rc.4+m5'))
        mock_print.assert_called_with("1.2.3-rc.5")
        self.assertEqual(cli.exception.code, 0)

        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase='meta', print=None, version='1.2.3-rc.4+m5'))
        mock_print.assert_called_with("1.2.3-rc.4+m6")
        self.assertEqual(cli.exception.code, 0)

    # TestMap: tests
    #  1.2.3+m1      -> increase meta  = 1.2.3+m2
    #  1.2.3-rc.4    -> increase meta  = 1.2.3-rc.4+meta.1
    #  1.2.3         -> increase meta  = 1.2.3+meta.1
    #  1.2.3+meta.1  -> increase rc    = 1.2.3-rc.1+meta.1
    #  1.2.3         -> increase rc    = 1.2.3-rc.1
    #  1.2.3-rc.4    -> increase rc    = 1.2.3-rc.5
    @patch('builtins.print')
    def test_increase_special_cases(self, mock_print):
        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase='meta', print=None, version='1.2.3+m1'))
        mock_print.assert_called_with("1.2.3+m2")
        self.assertEqual(cli.exception.code, 0)

        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase='meta', print=None, version='1.2.3-rc.4'))
        mock_print.assert_called_with("1.2.3-rc.4+meta.1")
        self.assertEqual(cli.exception.code, 0)

        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase='meta', print=None, version='1.2.3'))
        mock_print.assert_called_with("1.2.3+meta.1")
        self.assertEqual(cli.exception.code, 0)

        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase='rc', print=None, version='1.2.3+meta.1'))
        mock_print.assert_called_with("1.2.3-rc.1")
        self.assertEqual(cli.exception.code, 0)

        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase='rc', print=None, version='1.2.3'))
        mock_print.assert_called_with("1.2.3-rc.1")
        self.assertEqual(cli.exception.code, 0)

        with self.assertRaises(SystemExit) as cli:
            semver.start(argparse.Namespace(increase='rc', print=None, version='1.2.3-rc.4'))
        mock_print.assert_called_with("1.2.3-rc.5")
        self.assertEqual(cli.exception.code, 0)


if __name__ == '__main__':
    unittest.main()
