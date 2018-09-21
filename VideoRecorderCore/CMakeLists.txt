cmake_minimum_required(VERSION 3.4.1)

add_library(glutil-lib SHARED
            src/main/cpp/native-lib.cpp )

add_library(matrix4-lib SHARED
            src/main/cpp/Matrix4.h
            src/main/cpp/Matrix4.cpp)

add_library(BufferUtils-lib SHARED
            src/main/cpp/BufferUtils.h
            src/main/cpp/BufferUtils.cpp)

add_library(gdx2d-lib SHARED
            src/main/cpp/gdx2d/stb_image.h
            src/main/cpp/gdx2d/jpgd_c.h
            src/main/cpp/gdx2d/jpgd_c.cpp
            src/main/cpp/gdx2d/jpgd.h
            src/main/cpp/gdx2d/jpgd.cpp
            src/main/cpp/gdx2d/gdx2d.h
            src/main/cpp/gdx2d/gdx2d.c)

find_library(log-lib log)
find_library(GLESv2-lib GLESv2)

target_link_libraries(glutil-lib matrix4-lib BufferUtils-lib gdx2d-lib ${log-lib} ${GLESv2-lib})