cmake_minimum_required(VERSION 3.4.1)

add_library(glutil-lib SHARED
            src/main/cpp/native-lib.cpp )

add_library(matrix4-lib SHARED
            src/main/cpp/Matrix4.h
            src/main/cpp/Matrix4.cpp)

add_library(BufferUtils-lib SHARED
            src/main/cpp/BufferUtils.h
            src/main/cpp/BufferUtils.cpp)

find_library(log-lib log)
find_library(GLESv2-lib GLESv2)

target_link_libraries(glutil-lib matrix4-lib BufferUtils-lib ${log-lib} ${GLESv2-lib})