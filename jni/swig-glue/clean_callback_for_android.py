#!/usr/bin/python


import re
import sys, os

def make_dalvik_compat(text):
	init_text = """pjsuaJNI.Callback_director_connect(this, swigCPtr, swigCMemOwn, true);"""
	final_text = """pjsuaJNI.Callback_director_connect(this, swigCPtr, swigCMemOwn, false);"""
	return text.replace(init_text, final_text)

if __name__ == '__main__':
	filename = sys.argv[1]
	brut_code = open(filename).read()
	code_fixed = make_dalvik_compat(brut_code)
	print code_fixed
