#!/usr/bin/env bash

set -e # fail on errors
#set -x # echo commands run

docker_image=$1

mkdir -p /tmp/docker_nvidia_tmp
#cp /home/shadowop/sr-build-tools/docker/utils/10_nvidia.json /tmp/docker_nvidia_tmp/10_nvidia.json
cd /tmp/docker_nvidia_tmp
echo "{
    \"file_format_version\" : \"1.0.0\",
    \"ICD\" : {
        \"library_path\" : \"libEGL_nvidia.so.0\"
    }
}" >> 10_nvidia.json

touch Dockerfile

echo "FROM $docker_image

#USER root


LABEL Description=\"This is updated to use OpenGL with nvidia-docker2\" Vendor=\"Shadow Robot\" Version=\"1.0\"

# Docker GPU access
ENV NVIDIA_VISIBLE_DEVICES all
ENV NVIDIA_DRIVER_CAPABILITIES all
#RUN su -
#RUN sudo -i
#USER root
# OpenGL using libglvnd
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
    sudo

# Create the user and checkout it
ENV STEAM_USER steam
RUN useradd -ms /bin/bash \$STEAM_USER
RUN usermod -aG sudo \$STEAM_USER


RUN echo steam:root | chpasswd
RUN echo root:root | chpasswd
USER \$STEAM_USER

# Set the working directory
ENV WD /home/\$STEAM_USER
WORKDIR \$WD

# Create the steam directory and download the SteamCMD into it
ENV STEAM_DIR steamcmd
RUN mkdir -p \$STEAM_DIR && \
    curl -sqL \"https://steamcdn-a.akamaihd.net/client/installer/steamcmd_linux.tar.gz\" | \
    tar -C \$STEAM_DIR -zxv

# Install the steam cmd
RUN \$STEAM_DIR/steamcmd.sh +login anonymous +quit

# Bugfix, for default when the game server is started, it searches 
# where the steam client is (for default search in the .steam/sdk32
# directory
ENV HIDDEN_DIR \$WD/.steam/sdk32
RUN mkdir -p \$HIDDEN_DIR
RUN cp \$STEAM_DIR/linux32/steamclient.so \$HIDDEN_DIR" >> Dockerfile

docker build --tag "$docker_image-steam-nvidia2" .

cd
#rm -rf /tmp/docker_nvidia_tmp