ROOT_DIR=glustermg-1.0.0
DIR_NAME=${ROOT_DIR}/gmg-scripts
TAR_NAME=${ROOT_DIR}.tar

prepare-script-dir()
{
	if [ -d ${DIR_NAME} ]; then
		rm -rf ${DIR_NAME}
	fi
	mkdir -p ${DIR_NAME}
}

get-scripts()
{
	cd ${DIR_NAME}
	cp ../../src/com.gluster.storage.management.gateway.scripts/src/*.* .
	chmod +x *
	cd -
}

#---------------------------------------------
# Main Action Body
#---------------------------------------------
echo "Packaging Gluster Management Console Scripts..."

prepare-script-dir
get-scripts

/bin/rm -rf ${TAR_NAME} ${TAR_NAME}.gz
tar cvf ${TAR_NAME} ${ROOT_DIR}
gzip ${TAR_NAME}

echo "Done!"