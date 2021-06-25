LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	jni-util/netty_jni_util.c

LOCAL_SRC_FILES += \
	unix/netty_unix.c \
	unix/netty_unix_buffer.c \
	unix/netty_unix_errors.c \
	unix/netty_unix_filedescriptor.c \
	unix/netty_unix_limits.c \
	unix/netty_unix_socket.c \
	unix/netty_unix_util.c

LOCAL_SRC_FILES += \
	epoll/netty_epoll_linuxsocket.c \
	epoll/netty_epoll_native.c

LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE) \
	$(LOCAL_PATH)/epoll \
	$(LOCAL_PATH)/unix \
	$(LOCAL_PATH)/jni-util

LOCAL_LDFLAGS := -ldl
 
LOCAL_CFLAGS := -fvisibility=hidden

LOCAL_CFLAGS := -Werror \
				-Wno-unused-parameter \
				-Wno-missing-field-initializers \
				-Wno-sometimes-uninitialized \
				-Wno-pointer-arith \
				-Wno-sign-compare \
				-Wno-incompatible-pointer-types-discards-qualifiers \
				-Wno-int-conversion

ARCH_EXTENSION :=
ifeq ($(TARGET_ARCH),arm)
    ARCH_EXTENSION := unknown
endif

ifeq ($(TARGET_ARCH),arm64)
    ARCH_EXTENSION := aarch_64
endif

ifeq ($(TARGET_ARCH),x86)
    ARCH_EXTENSION := x86_32
endif

ifeq ($(TARGET_ARCH),x86_64)
    ARCH_EXTENSION := x86_64
endif

ifeq ($(ARCH_EXTENSION),)
    $(error Unknown architecture: $(TARGET_ARCH))
endif

LOCAL_MODULE := libnetty_transport_native_epoll_$(ARCH_EXTENSION)

LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
