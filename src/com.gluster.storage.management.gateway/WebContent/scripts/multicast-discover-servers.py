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
import socket
import select
import signal
import random
import string
import Utils
import Globals

running = True


def exitHandler(signum, frame):
    running = False


def sendMulticastRequest(idString):
    multicastSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    multicastSocket.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 2)
    multicastSocket.sendto("%s,%s,%s\n" % (Globals.GLUSTER_PROBE_STRING, Globals.GLUSTER_PROBE_VERSION, idString),
                           (Globals.MULTICAST_GROUP, Globals.MULTICAST_PORT))
    return multicastSocket


def openServerSocket():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(('', Globals.SERVER_PORT))
    server.listen(Globals.DEFAULT_BACKLOG)
    return server


def main():
    signal.signal(signal.SIGINT, exitHandler)
    signal.signal(signal.SIGTERM, exitHandler)
    signal.signal(signal.SIGALRM, exitHandler)

    idString = ''.join(random.choice(string.ascii_lowercase +
                                     string.ascii_uppercase +
                                     string.digits) for x in range(Globals.DEFAULT_ID_LENGTH))

    multicastSocket = sendMulticastRequest(idString)

    serverInfoDict = {}
    serverSocket = openServerSocket()
    rlist = [serverSocket]
    signal.alarm(Globals.DEFAULT_TIMEOUT)
    while running:
        try:
            ilist,olist,elist = select.select(rlist, [], [], 0.25)
        except select.error, e:
            break
        for sock in ilist:
            # handle new connection
            if sock == serverSocket:
                clientSocket, address = serverSocket.accept()
                #print "connection from %s on %s" % (address, clientSocket)
                rlist.append(clientSocket)
                continue

            # handle all other sockets
            data = sock.recv(Globals.DEFAULT_BUFSIZE)
            if not data:
                #print "closing socket %s" % sock
                sock.close()
                rlist.remove(sock)
            tokens =  data.strip().split(",")
            if len(tokens) != 6:
                continue
            if not (tokens[0] == Globals.GLUSTER_PROBE_STRING and \
                    tokens[1] == Globals.GLUSTER_PROBE_VERSION and \
                    tokens[2] == idString):
                continue
            serverInfoDict[tokens[3]] = [tokens[4], tokens[5]]
            #print "closing socket %s" % sock
            sock.close()
            rlist.remove(sock)

    for sock in rlist:
        sock.close()

    for k, v in serverInfoDict.iteritems():
        if v[0]:
            print v[0]
        else:
            print k

    sys.exit(0)


if __name__ == "__main__":
    main()
