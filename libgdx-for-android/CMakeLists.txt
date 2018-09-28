cmake_minimum_required(VERSION 3.4.1)

add_library(gdx SHARED
            src/main/cpp/android/AndroidGL20.h
            src/main/cpp/android/AndroidGL20.cpp
            src/main/cpp/etc1/etc1_utils.h
            src/main/cpp/etc1/etc1_utils.cpp
            src/main/cpp/gdx2d/gdx2d.h
            src/main/cpp/gdx2d/gdx2d.c
            src/main/cpp/gdx2d/jpgd.h
            src/main/cpp/gdx2d/jpgd.cpp
            src/main/cpp/gdx2d/jpgd_c.h
            src/main/cpp/gdx2d/jpgd_c.cpp
            src/main/cpp/gdx2d/stb_image.h
            src/main/cpp/BufferUtils.h
            src/main/cpp/BufferUtils.cpp
            src/main/cpp/ETC1.h
            src/main/cpp/ETC1.cpp
            src/main/cpp/Gdx2DPixmap.h
            src/main/cpp/Gdx2DPixmap.cpp
            src/main/cpp/Matrix4.h
            src/main/cpp/Matrix4.cpp
            src/main/cpp/memcpy_wrap.c)

find_library(log-lib log)
find_library(GLESv2-lib GLESv2)
find_library(math m)

target_link_libraries(gdx ${log-lib} ${GLESv2-lib} ${math})