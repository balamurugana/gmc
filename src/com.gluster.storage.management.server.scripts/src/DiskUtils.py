#  Copyright (c) 2010 Gluster, Inc. <http://www.gluster.com>
#  This file is part of Gluster Storage Platform.
#
#  Gluster Storage Platform is free software; you can redistribute it
#  and/or modify it under the terms of the GNU General Public License
#  as published by the Free Software Foundation; either version 3 of
#  the License, or (at your option) any later version.
#
#  Gluster Storage Platform is distributed in the hope that it will be
#  useful, but WITHOUT ANY WARRANTY; without even the implied warranty
#  of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see
#  <http://www.gnu.org/licenses/>.

import os
import glob
import dbus
import syslog
import Globals
import Common
import time
import Utils
import Disk
import Protocol

ONE_MB_SIZE = 1048576


def _stripDev(device):
    if Utils.isString(device) and device.startswith("/dev/"):
        return device[5:]
    return device


def _addDev(deviceName):
    if Utils.isString(deviceName) and not deviceName.startswith("/dev/"):
        return "/dev/" + deviceName
    return deviceName


def getDeviceName(device):
    if type(device) == type([]):
        nameList = []
        for d in device:
            nameList.append(_stripDev(d))
        return nameList
    return _stripDev(device)


def getDevice(deviceName):
    if Utils.isString(deviceName):
        return _addDev(deviceName)
    if type(deviceName) == type([]):
        nameList = []
        for d in deviceName:
            nameList.append(_addDev(d))
        return nameList
    return _addDev(deviceName)


def getDiskPartitionByUuid(uuid):
    uuidFile = "/dev/disk/by-uuid/%s" % uuid
    if os.path.exists(uuidFile):
        return getDeviceName(os.path.realpath(uuidFile))
    return None


def getUuidByDiskPartition(device):
    for uuidFile in glob.glob("/dev/disk/by-uuid/*"):
        if os.path.realpath(uuidFile) == device:
            return os.path.basename(uuidFile)
    return None


def getDiskPartitionUuid(partition):
    Utils.log("WARNING: getDiskPartitionUuid() is deprecated by getUuidByDiskPartition()")
    return getUuidByDiskPartition(partition)


def getDiskPartitionByLabel(label):
    ## TODO: Finding needs to be enhanced
    labelFile = "/dev/disk/by-label/%s" % label
    if os.path.exists(labelFile):
        if os.path.islink(labelFile):
            return getDeviceName(os.path.realpath(labelFile))
    return None


def getDeviceByLabel(label):
    Utils.log("WARNING: getDeviceByLabel() is deprecated by getDiskPartitionByLabel()")
    return getDiskPartitionByLabel(label)


def getDiskPartitionLabel(device):
    rv = Utils.runCommandFG(["sudo", "e2label", device], stdout=True)
    if rv["Status"] == 0:
        return rv["Stdout"].strip()
    return False


def getRootPartition(fsTabFile=Globals.FSTAB_FILE):
    fsTabEntryList = readFsTab(fsTabFile)
    for fsTabEntry in fsTabEntryList:
        if fsTabEntry["MountPoint"] == "/":
            if fsTabEntry["Device"].startswith("UUID="):
                return getDiskPartitionByUuid(fsTabEntry["Device"].split("UUID=")[-1])
            if fsTabEntry["Device"].startswith("LABEL="):
                partitionName = getDiskPartitionByLabel(fsTabEntry["Device"].split("LABEL=")[-1])
                if partitionName:
                    return partitionName
            return getDeviceName(fsTabEntry["Device"])
    return None


def getOsDisk():
    Utils.log("WARNING: getOsDisk() is deprecated by getRootPartition()")
    return getRootPartition()


def getDiskInfo(diskDeviceList=None):
    diskDeviceList = getDevice(diskDeviceList)
    if Utils.isString(diskDeviceList):
        diskDeviceList = [diskDeviceList]

    dbusSystemBus = dbus.SystemBus()
    halObj = dbusSystemBus.get_object("org.freedesktop.Hal",
                                      "/org/freedesktop/Hal/Manager")
    halManager = dbus.Interface(halObj, "org.freedesktop.Hal.Manager")
    storageUdiList = halManager.FindDeviceByCapability("storage")

    diskInfo = {}
    diskList = []
    totalDiskSpace = 0
    totalDiskUsage = 0
    for udi in storageUdiList:
        halDeviceObj = dbusSystemBus.get_object("org.freedesktop.Hal", udi)
        halDevice = dbus.Interface(halDeviceObj,
                                   "org.freedesktop.Hal.Device")
        if halDevice.GetProperty("storage.drive_type") in ["cdrom", "floppy"] or \
                halDevice.GetProperty("block.is_volume"):
            continue

        disk = {}
        disk["Device"] = str(halDevice.GetProperty('block.device'))
        if diskDeviceList and disk["Device"] not in diskDeviceList:
            continue
        disk["Description"] = str(halDevice.GetProperty('storage.vendor')) + " " + str(halDevice.GetProperty('storage.model'))
        if halDevice.GetProperty('storage.removable'):
            disk["Size"] = long(halDevice.GetProperty('storage.removable.media_size'))
        else:
            disk["Size"] = long(halDevice.GetProperty('storage.size')) / 1024**2
        disk["Interface"] = str(halDevice.GetProperty('storage.bus'))
        disk["DriveType"] = str(halDevice.GetProperty('storage.drive_type'))
        partitionList = []
        partitionUdiList = halManager.FindDeviceStringMatch("info.parent", udi)
        diskSpaceInUse = 0
        for partitionUdi in partitionUdiList:
            partitionHalDeviceObj = dbusSystemBus.get_object("org.freedesktop.Hal",
                                                             partitionUdi)
            partitionHalDevice = dbus.Interface(partitionHalDeviceObj,
                                                "org.freedesktop.Hal.Device")
            if not partitionHalDevice.GetProperty("block.is_volume"):
                continue
            partition = {}
            partition["Device"] = str(partitionHalDevice.GetProperty('block.device'))
            partition["Uuid"] = str(partitionHalDevice.GetProperty('volume.uuid'))
            partition["Size"] = long(partitionHalDevice.GetProperty('volume.size')) / 1024**2
            partition["Fstype"] = str(partitionHalDevice.GetProperty('volume.fstype'))
            partition["Fsversion"] = str(partitionHalDevice.GetProperty('volume.fsversion'))
            partition["Label"] = str(partitionHalDevice.GetProperty('volume.label'))
            partition["mountPoint"] = str(partitionHalDevice.GetProperty('volume.mount_point'))
            partition["readOnlyAccess"] = str(partitionHalDevice.GetProperty('volume.is_mounted_read_only'))
            partition["Used"] = 0L
            if partitionHalDevice.GetProperty("volume.is_mounted"):
                rv = Utils.runCommandFG(["df", str(partitionHalDevice.GetProperty('volume.mount_point'))], stdout=True)
                if rv["Status"] == 0:
                    try:
                        partition["Used"] = long(rv["Stdout"].split("\n")[1].split()[2]) / 1024
                        diskSpaceInUse += partition["Used"]
                    except IndexError:
                        pass
                    except ValueError:
                        pass
            partitionList.append(partition)
        disk["Partitions"] = partitionList
        disk["Used"] = diskSpaceInUse
        totalDiskSpace += disk["Size"]
        totalDiskUsage += disk["Used"]
        diskList.append(disk)
    diskInfo["disks"] = diskList
    diskInfo["totalDiskSpace"] = totalDiskSpace
    diskInfo["diskSpaceInUse"] = totalDiskUsage
    return diskInfo

def getDiskList(diskDeviceList=None):
    return diskInfo["disks"]

def readFsTab(fsTabFile=Globals.FSTAB_FILE):
    try:
        fsTabfp = open(fsTabFile)
    except IOError, e:
        Utils.log("readFsTab(): " + str(e))
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
        

def checkDiskMountPoint(diskMountPoint):
    try:
        fstabEntries = open(Globals.FSTAB_FILE).readlines()
    except IOError:
        fstabEntries = []
    found = False
    for entry in fstabEntries:
        entry = entry.strip()
        if not entry:
            continue
        entries = entry.split()
        if entries and len(entries) > 1 and entries[0].startswith("UUID=") and entries[1].upper() == diskMountPoint.upper():
            return True
    return False


def getMountPointByUuid(partitionUuid):
    # check uuid in etc/fstab
    try:
        fstabEntries = open(Globals.FSTAB_FILE).readlines()
    except IOError:
        fstabEntries = []
    found = False
    for entry in fstabEntries:
        entry = entry.strip()
        if not entry:
            continue
        if entry.split()[0] == "UUID=" + partitionUuid:
            return entry.split()[1]
    return None


def getDiskSizeInfo(partition):
    # get values from df output
    total = None
    used = None
    free = None
    command = "df -kl -t ext3 -t ext4 -t xfs"
    rv = Utils.runCommandFG(command, stdout=True, root=True)
    message = Common.stripEmptyLines(rv["Stdout"])
    if rv["Stderr"]:
        Common.Utils.log(syslog.LOG_ERR, "failed to get disk details. %s" % Common.stripEmptyLines(rv["Stdout"]))
        return None, None, None
    for line in rv["Stdout"].split("\n"):
        tokens = line.split()
        if len(tokens) < 4:
            continue
        if tokens[0] == partition:
            total = int(tokens[1]) / 1024.0
            used  = int(tokens[2]) / 1024.0
            free  = int(tokens[3]) / 1024.0
            break

    if total:
        return total, used, free
    
    # get total size from parted output
    for i in range(len(partition), 0, -1):
        pos = i - 1
        if not partition[pos].isdigit():
            break
    disk = partition[:pos+1]
    partitionNumber = partition[pos+1:]
    if not partitionNumber.isdigit():
        return None, None, None
    
    number = int(partitionNumber)
    command = "parted -ms %s unit kb print" % disk
    rv = Utils.runCommandFG(command, stdout=True, root=True)
    message = Common.stripEmptyLines(rv["Stdout"])
    if rv["Stderr"]:
        Common.Utils.log(syslog.LOG_ERR, "failed to get disk details. %s" % Common.stripEmptyLines(rv["Stdout"]))
        return None, None, None
    
    lines = rv["Stdout"].split(";\n")
    if len(lines) < 3:
        return None,None,None
    
    for line in lines[2:]:
        tokens = line.split(':')
        if len(tokens) < 4:
            continue
        if tokens[0] == str(number):
            total = int(tokens[3].split('kB')[0]) / 1024.0
            break
    return total, used, free


def refreshHal():
    rv = Utils.runCommandFG(["lshal"], stdout=True, root=True)
    if rv["Stderr"]:
        error = Common.stripEmptyLines(rv["Stderr"])
        Common.Utils.log(syslog.LOG_ERR, "failed to execute lshal command. Error: %s" % error)
        return False
    return True


def isDataDiskPartitionFormatted(device):
    #Todo: Proper label needs to be added for data partition
    #if getDiskPartitionLabel(device) != Globals.DATA_PARTITION_LABEL:
    #    return False

    diskObj = Disk.Disk()
    for disk in  diskObj.getMountableDiskList():
        if disk['device'].upper() == device.upper():
            mountPoint = disk['mount_point']
            if not mountPoint:
                return False
            if not os.path.exists(mountPoint):
                return False

    uuid = getUuidByDiskPartition(device)
    if not uuid:
        return False

    for fsTabEntry in readFsTab():
        if fsTabEntry["Device"] == ("UUID=%s" % uuid) and fsTabEntry["MountPoint"] == mountPoint:
            return True
    return False


def getDiskDom(diskDeviceList=None, bootPartition=None, skipDisk=None):
    diskDeviceList = getDevice(diskDeviceList)
    if Utils.isString(diskDeviceList):
        diskDeviceList = [diskDeviceList]

    if skipDisk:
        skipDisk = getDevice(skipDisk)
        if Utils.isString(skipDisk):
            skipDisk = [skipDisk]

    diskInfo = getDiskInfo(diskDeviceList)
    diskList = diskInfo["disks"]
    if not diskList:
        return None

    diskDom = Protocol.XDOM()
    disksTag = diskDom.createTag("disks", None)
    disksTag.appendChild(diskDom.createTag("totalDiskSpace", diskInfo["totalDiskSpace"]))
    disksTag.appendChild(diskDom.createTag("diskSpaceInUse", diskInfo["diskSpaceInUse"]))
    if not bootPartition:
        bootPartition = getRootPartition()
    for disk in diskList:
        if skipDisk and disk["Device"] in skipDisk:
            continue
        diskTag = diskDom.createTag("disk", None)
        diskTag.appendChild(diskDom.createTag("device", getDeviceName(disk["Device"])))
        diskTag.appendChild(diskDom.createTag("description", disk["Description"]))
        diskTag.appendChild(diskDom.createTag("size", str(disk["Size"])))
        diskTag.appendChild(diskDom.createTag("used", str(disk["Used"])))
        diskTag.appendChild(diskDom.createTag("interface", disk["Interface"]))
        if disk["Partitions"]:
            diskTag.appendChild(diskDom.createTag("init", "yes"))
        else:
            diskTag.appendChild(diskDom.createTag("init", "no"))
        for partition in disk["Partitions"]:
            partitionTag = diskDom.createTag("partition", None)
            device =  getDeviceName(partition["Device"])
            partitionTag.appendChild(diskDom.createTag("name", device))
            partitionTag.appendChild(diskDom.createTag("mountPoint", partition['mountPoint']))
            if not partition["Uuid"]:
                partitionTag.appendChild(diskDom.createTag("uuid", getUuidByDiskPartition("/dev/" + device)))
            else:
                partitionTag.appendChild(diskDom.createTag("uuid", partition["Uuid"]))
            partitionTag.appendChild(diskDom.createTag("size", str(partition["Size"])))
            partitionTag.appendChild(diskDom.createTag("free", str((partition["Size"] - partition["Used"]))))
            partitionTag.appendChild(diskDom.createTag("used", partition["Used"]))
            partitionTag.appendChild(diskDom.createTag("filesystem", partition["Fstype"]))
            partitionTag.appendChild(diskDom.createTag("readOnlyAccess", partition["readOnlyAccess"]))
            if partition['mountPoint'] or isDataDiskPartitionFormatted(partition["Device"]):
                partitionTag.appendChild(diskDom.createTag("status", "READY"))
            else:
                partitionTag.appendChild(diskDom.createTag("status", "UNINITIALIZED"))
            if "/export/" in partition["mountPoint"]:
                partitionTag.appendChild(diskDom.createTag("dataDisk", "True"))
            else:
                partitionTag.appendChild(diskDom.createTag("dataDisk", "False"))
            if "/" == partition["mountPoint"]:
                partitionTag.appendChild(diskDom.createTag("boot", "yes"))
            else:
                partitionTag.appendChild(diskDom.createTag("boot", "no"))
            diskTag.appendChild(partitionTag)
        disksTag.appendChild(diskTag)
    diskDom.addTag(disksTag)
    return diskDom
