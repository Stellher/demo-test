cmake_minimum_required(VERSION 3.22.1)
project(logvar_remote_demo)

add_library(remote_node_bridge SHARED node_bridge.cpp)

target_link_libraries(remote_node_bridge
    android
    log
    dl
)
