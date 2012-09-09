#!/usr/bin/python
import getopt, sys
import re
from string import Template

def type_to_signature(itype):
	if len(itype) > 2:
		if itype[-2:] == '[]':
			return "[%s" % type_to_signature(itype[:-2])
	if itype == "int":
		return "I"
	if itype == "long":
		return "J"
	if itype == "void":
		return "V"
	if itype == "boolean":
		return "Z"
	if itype == "byte":
		return "B"
	if itype == "char":
		return "C"
	if itype == "short":
		return "S"
	if itype == "float":
		return "F"
	if itype == "double":
		return "D"
	if itype == "String":
		return "Ljava/lang/String;"
	if itype == "Object":
		return "Ljava/lang/Object;"
	return "Lorg/pjsip/pjsua/%s;" % itype

def parse_java_file(input_stream, package, module):
	outputs = []
	package_prefix = "Java_%s_%sJNI" % (package.replace(".", "_"), module)
	for line in input_stream:
		definition = re.match(r'.*public final static native ([^\( ]*) ([^\)]*)\(([^)]*)\).*',line)
		if definition is not None:
			retour = definition.group(1)
			name = definition.group(2)
			args = definition.group(3)
			
			args_sigs = []
			args_frags = args.split(',')
			for args_frag in args_frags:
				argf = re.match(r'(\b)?([^ ]+) .*', args_frag.strip())
				if argf is not None:
					args_sigs.append(type_to_signature(argf.group(2)))
		
			sig = "(%s)%s" % (''.join(args_sigs), type_to_signature(retour))
			outputs.append("{\"%s\", \"%s\", (void*)& %s_%s}" % (name, sig, package_prefix, name.replace('_', '_1')))
	return outputs

def render_to_template(defs, template_string):
	template = Template(template_string)
	return template.substitute(defs= ",\r\n".join(defs) )


if __name__ == "__main__":
	try:
		opts, args = getopt.getopt(sys.argv[1:], "i:o:t:m:p:", ["input=", "output=", "template=", "module=", "package="])
	except getopt.GetoptError, err:
		# print help information and exit:
		print str(err) # will print something like "option -a not recognized"
		sys.exit(2)
	
	input_stream = None
	output_file = None
	template_string = None
	package = ""
	module = ""
	for o, a in opts:
		if o in ("-i", "--input"):
			input_stream = open(a)
		if o in ("-o", "--output"):
			output_file = open(a, "w")
		if o in ("-t", "--template"):
			template_string = open(a).read()
		if o in ("-m", "--module"):
			module = a
		if o in ("-p", "--package"):
			package = a

	defs = parse_java_file(input_stream, package, module)
	output_file.write(render_to_template(defs, template_string))
	output_file.close()
	input_stream.close()


