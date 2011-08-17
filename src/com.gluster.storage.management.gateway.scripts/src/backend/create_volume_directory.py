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
from XmlHandler import ResponseXml
import DiskUtils
import Utils

def createDirectory(disk, volumeName):
    # Retrieving disk uuid
    diskUuid = DiskUtils.getUuidByDiskPartition(DiskUtils.getDevice(disk))

    rs = ResponseXml()
    if not diskUuid:
        Utils.log("failed to find disk:%s uuid" % disk)
        rs.appendTagRoute("status.code", "-1")
        rs.appendTagRoute("status.message", "Error: Failed to find disk uuid")
        return rs.toprettyxml()

    # Retrieving disk mount point using disk uuid
    diskMountPoint = DiskUtils.getMountPointByUuid(diskUuid)
    if not os.path.exists(diskMountPoint):
        Utils.log("failed to retrieve disk:%s mount point" % disk) 
        rs.appendTagRoute("status.code", "-1")
        rs.appendTagRoute("status.message", "Error: Failed to retrieve disk details")
        return rs.toprettyxml()

    # creating volume directory under disk mount point
    volumeDirectory = "%s/%s" % (diskMountPoint, volumeName)
    if os.path.exists(volumeDirectory):
        Utils.log("Volume directory:%s already exists" % (volumeDirectory))
        rs.appendTagRoute("status.code", "-2")
        rs.appendTagRoute("status.message", "Volume directory already exists!")
        return rs.toprettyxml()

    rv = Utils.runCommand("mkdir %s" % volumeDirectory, output=True, root=True)
    if rv["Status"] != 0:
        rs.appendTagRoute("status.code", rv["Status"])
        rs.appendTagRoute("status.message", "Failed to create volume directory")
        return rs.toprettyxml()

    rs.appendTagRoute("status.code", rv["Status"])
    rs.appendTagRoute("status.message", volumeDirectory)
    return rs.toprettyxml()

def main():
    if len(sys.argv) != 3:
        sys.stderr.write("usage: %s <disk name> <volume name>\n" % os.path.basename(sys.argv[0]))
        sys.exit(-1)

    disk = sys.argv[1]
    volumeName = sys.argv[2]
    print createDirectory(disk, volumeName)
    sys.exit(0)

if __name__ == "__main__":
    main()