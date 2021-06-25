#!/bin/sh

# This script copies the Netty native code into the Android application.
# We need to do this because there is no way to include all the required
# libraries for each architecture.

# Location of Netty source repository
NETTY=~/dev/netty

# Epoll files
for i in \
    netty_epoll_native.c \
    netty_epoll_linuxsocket.c \
    netty_epoll_linuxsocket.h
do
    cp -v $NETTY/transport-native-epoll/src/main/c/$i app/src/main/jni/epoll
done

# Unix socket library
for i in \
    netty_unix_buffer.c \
    netty_unix_util.c \
    netty_unix_socket.c \
    netty_unix.c \
    netty_unix_limits.c \
    netty_unix_filedescriptor.c \
    netty_unix_errors.c \
    netty_unix.h \
    netty_unix_util.h \
    netty_unix_socket.h \
    netty_unix_filedescriptor.h \
    netty_unix_buffer.h \
    netty_unix_jni.h \
    netty_unix_limits.h \
    netty_unix_errors.h 
do
    cp -v $NETTY/transport-native-unix-common/src/main/c/$i app/src/main/jni/unix
done




