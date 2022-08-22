#!/bin/bash

UNAME=$(uname)

if [[ "$UNAME" == "Darwin" || "$UNAME" == "Linux" ]]; then
  echo "Info: Setting up $UNAME"

  if [[ "$UNAME" == "Darwin" ]]; then
    echo Using brew to install autoconf and automake
    brew install autoconf automake
  fi

  if [ -d "./build/bdwgc" ]; then
    echo "Warning: ./build/bdwgc exists and is a directory - assuming then all is in"
    echo "         order and doing nothing.  If you need to redo then delete this directory"
    echo "         and rerun"
  else
    echo "Info: Getting bdwgc and building"
    (
      mkdir -p build || exit 1
      cd ./build || exit 1
      git clone https://github.com/ivmai/bdwgc.git || exit 1
      cd bdwgc || exit 1
      git clone https://github.com/ivmai/libatomic_ops.git || exit 1
      ./autogen.sh || exit 1
      ./configure || exit 1
      make -j || exit 1
      make check || exit 1
      make -f Makefile.direct || exit 1
    )
  fi
else
  echo "Error: uname == $UNAME: Unable to setup"
  exit 1
fi
