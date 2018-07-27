cmake_minimum_required(VERSION 3.4.1)

add_library(glutil-lib SHARED
            src/main/cpp/native-lib.cpp )

find_library(log-lib log)
find_library(GLESv2-lib GLESv2)

target_link_libraries(glutil-lib ${log-lib} ${GLESv2-lib})