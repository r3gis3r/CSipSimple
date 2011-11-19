#!/bin/sh

move_generic_lib() {
	libs_files=$(ls libs/*/${1}.so 2> /dev/null | wc -l)
	if [ "$libs_files" != "0" ]; then
		for lib_folder in libs/*; do
			mkdir -p ../${2}/${lib_folder};
			mv ${lib_folder}/${1}.so ../${2}/${lib_folder}/${1}.so;
		done
		echo "[OK]";
	else
		echo "[KO] - lib not built"
	fi
}

move_lib() {
	echo -n "Moving $1 to $2 project ... "
	libs_files=$(ls libs/*/libpj_${1}_codec.so 2> /dev/null | wc -l)
	if [ "$libs_files" != "0" ]; then
		for lib_folder in libs/*; do
			mkdir -p ../CSipSimpleCodec${2}/${lib_folder};
			mv ${lib_folder}/libpj_${1}_codec.so ../CSipSimpleCodec${2}/${lib_folder}/libpj_${1}_codec.so;
		done
		echo "[OK]";
	else
		echo "[KO] - lib not built"
	fi
}

move_lib "g7221" "G221"
move_lib "codec2" "Codec2"
move_lib "silk" "Silk"
