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
import Globals

def readFsTab(fsTabFile=Globals.FSTAB_FILE):
    try:
        fsTabfp = open(fsTabFile)
    except IOError, e:
        log("readFsTab(): " + str(e))
        return None

    fsTabEntryList = []
    for line in fsTabfp:
        tokens = line.strip().split()
        if not tokens or tokens[0].startswith('#'):
            continue
        fsTabEntry = {}
        fsTabEntry["Device"] = None
        fsTabEntry["MountPoint"] = None
        fsTabEntry["FsType"] = None
        fsTabEntry["Options"] = None
        fsTabEntry["DumpOption"] = 0
        fsTabEntry["fsckOrder"] = 0
        try:
            fsTabEntry["Device"] = tokens[0]
            fsTabEntry["MountPoint"] = tokens[1]
            fsTabEntry["FsType"] = tokens[2]
            fsTabEntry["Options"] = tokens[3]
            fsTabEntry["DumpOption"] = tokens[4]
            fsTabEntry["fsckOrder"] = tokens[5]
        except IndexError:
            pass
        if fsTabEntry["Device"] and fsTabEntry["MountPoint"] and fsTabEntry["FsType"] and fsTabEntry["Options"]:
            fsTabEntryList.append(fsTabEntry)

    fsTabfp.close()
    return fsTabEntryList

def writeFsTab(fsTabEntryList, fsTabFile=Globals.FSTAB_FILE):
    try:
        fsTabfp = open(fsTabFile, "w")
        for fsTabEntry in fsTabEntryList:
            fsTabfp.write("%s\t%s\t%s\t%s\t%s\t%s\n" %
                          (fsTabEntry["Device"], fsTabEntry["MountPoint"],
                           fsTabEntry["FsType"], fsTabEntry["Options"],
                           fsTabEntry["DumpOption"], fsTabEntry["fsckOrder"]))
        fsTabfp.close()
    except IOError, e:
        log("writeFsTab(): " + str(e))
        return False
    return True

def addFsTabEntry(fsTabEntry, fsTabFile=Globals.FSTAB_FILE):
    try:
        fsTabfp = open(fsTabFile, "a")
        fsTabfp.write("%s\t%s\t%s\t%s\t%s\t%s\n" %
                      (fsTabEntry["Device"], fsTabEntry["MountPoint"],
                       fsTabEntry["FsType"], fsTabEntry["Options"],
                       fsTabEntry["DumpOption"], fsTabEntry["fsckOrder"]))
        fsTabfp.close()
    except IOError, e:
        log("addFsTabEntry(): " + str(e))
        return False
    return True

def removeFsTabEntry(fsTabEntry, fsTabFile=Globals.FSTAB_FILE):
    fsTabEntryList = readFsTab(fsTabFile)
    if not fsTabEntryList:
        return False

    try:
        fsTabEntryList.remove(fsTabEntry)
    except ValueError:
        return False

    return writeFsTab(fsTabEntryList, fsTabFile)
