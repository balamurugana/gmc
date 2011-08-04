#!/usr/bin/python
#  Copyright (C) 2011 Gluster, Inc. <http://www.gluster.com>
#  This file is part of Gluster Management Gateway.
#

import os
import sys
p1 = os.path.abspath(os.path.dirname(sys.argv[0]))
p2 = "%s/common" % os.path.dirname(p1)
if not p1 in sys.path:
    sys.path.append(p1)
if not p2 in sys.path:
    sys.path.append(p2)
import Utils


cifsVolumeFile = "/opt/glustermg/etc/volumes.cifs"


def removeVolumeCifsConf(volumeName):
    try:
        fp = open(cifsVolumeFile)
        content = fp.read()
        fp.close()
    except IOError, e:
        Utils.log("failed to read file %s: %s" % (cifsVolumeFile, str(e)))
        content = ""

    try:
        fp = open(cifsVolumeFile, "w")
        for line in content.split():
            if line.split(":")[0] != volumeName:
                fp.write("%s\n" % line)
        fp.close()
    except IOError, e:
        Utils.log("failed to write file %s: %s" % (cifsVolumeFile, str(e)))
        return False
    return True


def main():
    if len(sys.argv) < 3:
        sys.stderr.write("usage: %s SERVER_FILE VOLUME_NAME\n" % os.path.basename(sys.argv[0]))
        sys.exit(-1)

    serverFile = sys.argv[1]
    volumeName = sys.argv[2]

    rv = Utils.runCommand(["grun.py", serverFile, "delete_volume_cifs.py", volumeName])
    if rv == 0:
        if not removeVolumeCifsConf(volumeName):
            sys.exit(11)
    sys.exit(rv)


if __name__ == "__main__":
    main()
