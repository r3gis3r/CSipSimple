#!/usr/bin/python

import re
import sys, os

def remove_comments(text):
    """ remove c-style comments.
        text: blob of text with comments (can include newlines)
        returns: text with comments removed
    """
    pattern = r"""
                            ##  --------- COMMENT ---------
           /\*              ##  Start of /* ... */ comment
           [^*]*\*+         ##  Non-* followed by 1-or-more *'s
           (                ##
             [^/*][^*]*\*+  ##
           )*               ##  0-or-more things which don't start with /
                            ##    but do end with '*'
           /                ##  End of /* ... */ comment
         |                  ##  -OR-  various things which aren't comments:
           (                ## 
                            ##  ------ " ... " STRING ------
             "              ##  Start of " ... " string
             (              ##
               \\.          ##  Escaped char
             |              ##  -OR-
               [^"\\]       ##  Non "\ characters
             )*             ##
             "              ##  End of " ... " string
           |                ##  -OR-
                            ##
                            ##  ------ ' ... ' STRING ------
             '              ##  Start of ' ... ' string
             (              ##
               \\.          ##  Escaped char
             |              ##  -OR-
               [^'\\]       ##  Non '\ characters
             )*             ##
             '              ##  End of ' ... ' string
           |                ##  -OR-
                            ##
                            ##  ------ ANYTHING ELSE -------
             .              ##  Anything other char
             [^/"'\\]*      ##  Chars which doesn't start a comment, string
           )                ##    or escape
    """
    regex = re.compile(pattern, re.VERBOSE|re.MULTILINE|re.DOTALL)
    noncomments = [m.group(2) for m in regex.finditer(text) if m.group(2)]

    return "".join(noncomments)


def remove_not_decls(text):
	""" Remove not extern declarations
"""
	pattern = r"PJ_BEGIN_DECL(.*?)PJ_END_DECL"
	regex = re.compile(pattern, re.MULTILINE|re.DOTALL)
	onlydecls = [m.group(1) for m in regex.finditer(text) if m.group(1)]
	return "".join(onlydecls)


def remove_useless_methods(text):
	""" Remove useless methods that we don't want to see exported
"""
	folder = os.path.dirname(os.path.realpath(__file__))
	ignored_methods_file = open(os.path.join(folder, "ignored_methods.txt"))
	for method_name in ignored_methods_file.readlines():
		if len(method_name) > 0:
			pattern = re.compile(method_name, re.MULTILINE)
			text = re.sub(pattern, "", text)
	return text

def remove_blank_lines(text):
	return "\n".join([l for l in text.split("\n") if l.strip()])

def add_pjsua_rename(text):
	pattern = r"PJ_DECL.*?\s+((pjsua_([A-Za-z1-9_\-]+))\s*\([^)]+\));"
	regex = re.compile(pattern, re.MULTILINE|re.DOTALL)
	with_decls = [ ( "%%rename(%s) %s;\n%%javamethodmodifiers %s \"public synchronized\";\n" % ( m.group(3) , m.group(2), m.group(1) ) ) for m in regex.finditer(text) ]
	return  "".join(with_decls) + "\n" + text

if __name__ == '__main__':
	filename = sys.argv[1]
	code_w_comments = open(filename).read()
	code_wo_comments = remove_comments(code_w_comments)
	code_w_decls = remove_not_decls(code_wo_comments)
	code_necessary = remove_useless_methods(code_w_decls)
	code_stripped = remove_blank_lines(code_necessary)
	code_renamed = add_pjsua_rename(code_stripped)
	print(code_renamed)

