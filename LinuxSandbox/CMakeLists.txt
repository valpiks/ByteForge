cmake_minimum_required(VERSION 3.16)
project(cpp_sandbox)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(${CMAKE_SOURCE_DIR})

add_executable(cpp_sandbox
    main.cpp
    src/socketserver.cpp
    src/sandbox.cpp
)