cmake_minimum_required(VERSION 3.20)
project(MiniMateAudioDriver CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_BUILD_TYPE Release CACHE STRING "" FORCE)

add_subdirectory(vendor/libASPL)

add_library(MiniMateAudio MODULE Driver.cpp)
target_link_libraries(MiniMateAudio PRIVATE libASPL)
target_include_directories(MiniMateAudio PRIVATE vendor/libASPL/include)

set_target_properties(MiniMateAudio PROPERTIES
    OUTPUT_NAME "MiniMateAudio"
    BUNDLE true
    BUNDLE_EXTENSION "driver"
    PREFIX ""
    SUFFIX ""
    MACOSX_BUNDLE ON
    MACOSX_BUNDLE_INFO_PLIST "${CMAKE_CURRENT_SOURCE_DIR}/Info.plist.in"
    MACOSX_BUNDLE_BUNDLE_NAME "MiniMateAudio"
    MACOSX_BUNDLE_BUNDLE_VERSION "1.0.0"
    MACOSX_BUNDLE_GUI_IDENTIFIER "com.minimate.audio.driver"
    MACOSX_BUNDLE_SHORT_VERSION_STRING "1.0.0"
)

