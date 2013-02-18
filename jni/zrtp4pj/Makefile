# This makefile aim is only for now to address source checkout and patch
zrtp4pj_repo := git://github.com/r3gis3r/ZRTP4PJ.git
zrtp4pj_tag := 1.1.0_3
#10fe242813531daa61088af158b8b64c6fbe787e
zrtpcpp_repo := git://github.com/r3gis3r/ZRTPCPP.git
zrtpcpp_tag := 2.3.0_2


git_update=git fetch --tags; \
git checkout origin; \
git checkout $(1);

all :
	# For now we do nothing to build - it's made by android_toolchain
	
init : sources
	# Init done

sources :
	@git clone $(zrtp4pj_repo) sources; \
	cd sources; \
	$(call git_update, $(zrtp4pj_tag)) \
	cd zsrtp; \
	git clone $(zrtpcpp_repo) zrtp; \
	cd zrtp; \
	$(call git_update, $(zrtpcpp_tag))

update : sources
	@cd sources; \
	$(call git_update, $(zrtp4pj_tag)) \
	cd zsrtp/zrtp; \
	if grep -q "wernerd" .git/config; then git remote set-url origin $(zrtpcpp_repo); fi; \
	$(call git_update, $(zrtpcpp_tag))
