#!/usr/bin/env bash

set -e # fail on errors
#set -x # echo commands run

docker_image=$1

mkdir -p /tmp/docker_nvidia_tmp
#cp /home/shadowop/sr-build-tools/docker/utils/10_nvidia.json /tmp/docker_nvidia_tmp/10_nvidia.json
cd /tmp/docker_nvidia_tmp
wget https://raw.githubusercontent.com/shadow-robot/sr-build-tools/containerising_steam_compact/docker/utils/60-HTC-Vive-perms-Ubuntu.rules
wget https://raw.githubusercontent.com/shadow-robot/sr-build-tools/containerising_steam_compact/docker/utils/99-steam-perms.rules
sudo cp 60-HTC-Vive-perms-Ubuntu.rules /lib/udev/rules.d
sudo cp 99-steam-perms.rules /lib/udev/rules.d
sudo udevadm control --reload-rules && sudo udevadm trigger

echo "{
    \"file_format_version\" : \"1.0.0\",
    \"ICD\" : {
        \"library_path\" : \"libEGL_nvidia.so.0\"
    }
}" >> 10_nvidia.json

touch Dockerfile

echo "FROM $docker_image

LABEL Description=\"This is updated to use OpenGL with nvidia-docker2 and steamvr\" Vendor=\"Shadow Robot\" Version=\"1.0\"

# Docker GPU access
ENV NVIDIA_VISIBLE_DEVICES all
ENV NVIDIA_DRIVER_CAPABILITIES all
RUN apt-get update && apt-get install -y --no-install-recommends \
        git \
        make \
        automake \
        autoconf \
        libtool \
        pkg-config \
        python \
        libxext-dev \
        libx11-dev \
        x11proto-gl-dev && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /opt/libglvnd

RUN git clone --branch=v1.0.0 https://github.com/NVIDIA/libglvnd.git . && \
    ./autogen.sh && \
    ./configure --prefix=/usr/local --libdir=/usr/local/lib/x86_64-linux-gnu && \
    make -j\"$(nproc)\" install-strip && \
    find /usr/local/lib/x86_64-linux-gnu -type f -name 'lib*.la' -delete

RUN dpkg --add-architecture i386 && \
    apt-get update && apt-get install -y --no-install-recommends \
        gcc-multilib \
        libxext-dev:i386 \
        libx11-dev:i386 && \
    rm -rf /var/lib/apt/lists/*

# 32-bit libraries
RUN make distclean && \
    ./autogen.sh && \
    ./configure --prefix=/usr/local --libdir=/usr/local/lib/i386-linux-gnu --host=i386-linux-gnu \"CFLAGS=-m32\" \"CXXFLAGS=-m32\" \"LDFLAGS=-m32\" && \
    make -j\"$(nproc)\" install-strip && \
    find /usr/local/lib/i386-linux-gnu -type f -name 'lib*.la' -delete

COPY 10_nvidia.json /usr/local/share/glvnd/egl_vendor.d/10_nvidia.json

RUN echo '/usr/local/lib/x86_64-linux-gnu' >> /etc/ld.so.conf.d/glvnd.conf && \
    echo '/usr/local/lib/i386-linux-gnu' >> /etc/ld.so.conf.d/glvnd.conf && \
    ldconfig

ENV LD_LIBRARY_PATH /usr/local/lib/x86_64-linux-gnu:/usr/local/lib/i386-linux-gnu${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}


# nvidia-docker hooks
LABEL com.nvidia.volumes.needed=\"nvidia_driver\"

ENV PATH /usr/local/nvidia/bin:${PATH}

ENV LD_LIBRARY_PATH /usr/local/nvidia/lib:/usr/local/nvidia/lib64:${LD_LIBRARY_PATH}

# Install the required dependencies
RUN apt-get update && apt-get install -y \
    curl \
    ca-certificates \
    lib32gcc1 \
    binutils \
    terminator \
    xdg-utils \
    software-properties-common\
    sudo \
    libvulkan1 \
    usbutils

RUN dpkg --add-architecture i386
RUN add-apt-repository multiverse
RUN apt-get update
RUN apt-get dist-upgrade -y

RUN cd /home/user
RUN wget http://mirrors.kernel.org/ubuntu/pool/main/u/udev/libudev0_175-0ubuntu9_amd64.deb
RUN dpkg -i libudev0_175-0ubuntu9_amd64.deb
RUN rm libudev0_175-0ubuntu9_amd64.deb

# Create the user and checkout it
ENV USER_NAME user

RUN echo \$USER_NAME:root | chpasswd
RUN echo root:root | chpasswd
USER \$USER_NAME

# Set the working directory
ENV WD /home/\$USER_NAME
WORKDIR \$WD

# Create the steam directory and download the SteamCMD into it
ENV STEAM_DIR steamcmd
RUN mkdir -p \$STEAM_DIR && \
    curl -sqL \"https://steamcdn-a.akamaihd.net/client/installer/steamcmd_linux.tar.gz\" | \
    tar -C \$STEAM_DIR -zxv

# Install the steam cmd
RUN \$STEAM_DIR/steamcmd.sh +login anonymous +quit


# Bugfix
ENV HIDDEN_DIR_32 \$WD/.steam/sdk32
ENV HIDDEN_DIR_64 \$WD/.steam/sdk64
RUN mkdir -p \$HIDDEN_DIR_32
RUN mkdir -p \$HIDDEN_DIR_64
RUN cp \$STEAM_DIR/linux32/steamclient.so \$HIDDEN_DIR_32
RUN cp \$STEAM_DIR/linux64/steamclient.so \$HIDDEN_DIR_64

# Install SteamVR
RUN /home/user/steamcmd/./steamcmd.sh +login tom_shadow_software shadow_software +force_install_dir /home/user/.steam +app_update 250820 -beta beta validate +quit
USER root

#Another bugfix
RUN setcap CAP_SYS_NICE=eip /home/user/.steam/bin/linux64/vrcompositor-launcher" >> Dockerfile

docker build --tag "$docker_image-steam-nvidia2" .

cd
rm -rf /tmp/docker_nvidia_tmp
