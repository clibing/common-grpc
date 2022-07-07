#########################################################################
# File Name: build.sh
# Author: clibing
# mail: wmsjhappy@@gmail.com
# Created Time: 四  7/ 7 13:10:38 2022
#########################################################################
#!/bin/bash
protoc -I/usr/local/include -I. -I$GOPATH/src \
       -I$GOPATH/src/proto/third_party/googleapis \
       --go_out=$GOPATH/src/ \
       --go-grpc_out=$GOPATH/src/ \
       ./common.proto
        # --include_imports --include_source_info 
