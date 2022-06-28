import argparse
import os
import unittest.mock
from os.path import dirname
from unittest.mock import patch

from functions import xml_reader as xr

pom_file = os.path.join(dirname(dirname(dirname(dirname(dirname(os.path.realpath(__file__)))))), "pom.xml")


class MyTestCase(unittest.TestCase):

    @patch('builtins.print')
    def test_pom_java_version(self, mock_print):
        with self.assertRaises(SystemExit) as cli:
            xr.start(argparse.Namespace(file=pom_file, pjv=True, xpath=[]))
        mock_print.assert_called_with('11')
        self.assertEqual(cli.exception.code, 0)

    @patch('builtins.print')
    def test_compiler_java_version(self, mock_print):
        with self.assertRaises(SystemExit) as cli:
            xr.start(argparse.Namespace(
                file=pom_file,
                pjv=False,
                xpath=['*//artifactId[text()="maven-compiler-plugin"][1]/parent::*//source']
            ))
        mock_print.assert_called_with('11')
        self.assertEqual(cli.exception.code, 0)

    @patch('builtins.print')
    def test_property_java_version(self, mock_print):
        with self.assertRaises(SystemExit) as cli:
            xr.start(argparse.Namespace(
                file=pom_file,
                pjv=False,
                xpath=['*//java.version']
            ))
        mock_print.assert_called_with('11')
        self.assertEqual(cli.exception.code, 0)

    @patch('builtins.print')
    def test_non_existing_tag(self, mock_print):
        with self.assertRaises(SystemExit) as cli:
            xr.start(argparse.Namespace(
                file=pom_file,
                pjv=False,
                xpath=['*//not-exists']
            ))
        mock_print.assert_called_with("Result [None]")
        self.assertEqual(cli.exception.code, 1)

    @patch('builtins.print')
    def test_no_parameters(self, mock_print):
        with self.assertRaises(SystemExit) as cli:
            xr.start(argparse.Namespace(
                file=pom_file,
                pjv=False,
                xpath=[]
            ))
        mock_print.assert_called_with("Missing arguments see `--help` for more")
        self.assertEqual(cli.exception.code, 1)

    @patch('builtins.print')
    def test_invalid_file(self, mock_print):
        with self.assertRaises(SystemExit) as cli:
            xr.start(argparse.Namespace(
                file=os.getcwd(),
                pjv=False,
                xpath=[]
            ))
        mock_print.assert_called_with("Invalid XML file [" + os.getcwd() + "]")
        self.assertEqual(cli.exception.code, 1)


if __name__ == '__main__':
    unittest.main()
