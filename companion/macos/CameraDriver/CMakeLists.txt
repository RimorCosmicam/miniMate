cmake_minimum_required(VERSION 3.20)
project(MiniMateCamera LANGUAGES C CXX OBJC OBJCXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(NOT DEFINED DAL_SOURCE)
    message(FATAL_ERROR "DAL_SOURCE must point to the pinned CoreMediaIO DAL source")
endif()

set(DAL_FILES
    "${DAL_SOURCE}/Device.mm"
    "${DAL_SOURCE}/ObjectStore.mm"
    "${DAL_SOURCE}/PlugIn.mm"
    "${DAL_SOURCE}/PlugInInterface.mm"
    "${DAL_SOURCE}/PlugInMain.mm"
    "${DAL_SOURCE}/Stream.mm"
    "${CMAKE_CURRENT_SOURCE_DIR}/FrameSource.mm"
)

add_library(MiniMateCamera MODULE ${DAL_FILES})
target_include_directories(MiniMateCamera PRIVATE "${DAL_SOURCE}")
target_compile_options(MiniMateCamera PRIVATE "-fobjc-arc")
target_link_libraries(MiniMateCamera PRIVATE
    "-framework AppKit"
    "-framework CoreFoundation"
    "-framework CoreMedia"
    "-framework CoreMediaIO"
    "-framework CoreVideo"
    "-framework ImageIO"
)

set_target_properties(MiniMateCamera PROPERTIES
    OUTPUT_NAME "MiniMateCamera"
    BUNDLE true
    BUNDLE_EXTENSION "plugin"
    PREFIX ""
    SUFFIX ""
    MACOSX_BUNDLE ON
    MACOSX_BUNDLE_INFO_PLIST "${CMAKE_CURRENT_SOURCE_DIR}/Info.plist.in"
    MACOSX_BUNDLE_BUNDLE_NAME "MiniMate Camera"
    MACOSX_BUNDLE_BUNDLE_VERSION "1.0.0"
    MACOSX_BUNDLE_GUI_IDENTIFIER "com.minimate.camera.dal"
    MACOSX_BUNDLE_SHORT_VERSION_STRING "1.0.0"
)
