#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <unistd.h>
#include <cstdlib>
#include <cerrno>
#include <cstring>
#include <mutex>
#include <string>
#include <vector>

namespace {
constexpr const char* TAG = "RemoteNodeBridge";
std::mutex g_error_mutex;
std::string g_last_error;

void set_error(const std::string& value) {
    std::lock_guard<std::mutex> lock(g_error_mutex);
    g_last_error = value;
    __android_log_print(ANDROID_LOG_ERROR, TAG, "%s", value.c_str());
}

std::string get_error() {
    std::lock_guard<std::mutex> lock(g_error_mutex);
    return g_last_error;
}

std::string jstring_to_string(JNIEnv* env, jstring value) {
    if (value == nullptr) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

void set_env(const char* key, const std::string& value) {
    if (!value.empty()) setenv(key, value.c_str(), 1);
}
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_logvarremote_data_runtime_NativeNodeBridge_startNode(
        JNIEnv* env,
        jobject,
        jstring libNodePath,
        jstring libcxxPath,
        jstring projectDir,
        jstring entryScript,
        jint port,
        jstring token) {
    const std::string node_path = jstring_to_string(env, libNodePath);
    const std::string libcxx_path = jstring_to_string(env, libcxxPath);
    const std::string project_dir = jstring_to_string(env, projectDir);
    const std::string entry_script = jstring_to_string(env, entryScript);
    const std::string auth_token = jstring_to_string(env, token);

    if (node_path.empty() || project_dir.empty() || entry_script.empty()) {
        set_error("Required runtime path is empty");
        return -10;
    }

    if (!libcxx_path.empty()) {
        void* cxx = dlopen(libcxx_path.c_str(), RTLD_NOW | RTLD_GLOBAL);
        if (!cxx) {
            const char* err = dlerror();
            set_error(std::string("dlopen libc++_shared.so failed: ") + (err ? err : "unknown"));
            return -11;
        }
    }

    void* node_handle = dlopen(node_path.c_str(), RTLD_NOW | RTLD_GLOBAL);
    if (!node_handle) {
        const char* err = dlerror();
        set_error(std::string("dlopen libnode.so failed: ") + (err ? err : "unknown"));
        return -12;
    }

    using NodeStart = int (*)(int, char**);
    dlerror();
    auto node_start = reinterpret_cast<NodeStart>(
        dlsym(node_handle, "_ZN4node5StartEiPPc")
    );
    const char* symbol_error = dlerror();
    if (symbol_error != nullptr || node_start == nullptr) {
        set_error(std::string("node::Start symbol not found: ") +
                  (symbol_error ? symbol_error : "unknown"));
        return -13;
    }

    if (chdir(project_dir.c_str()) != 0) {
        set_error(std::string("chdir failed: ") + std::strerror(errno));
        return -14;
    }

    const std::string port_text = std::to_string(static_cast<int>(port));
    set_env("DANMU_API_HOME", project_dir);
    set_env("DANMU_API_HOST", "127.0.0.1");
    set_env("DANMU_API_PORT", port_text);
    set_env("DANMU_API_PROXY_PORT", std::to_string(static_cast<int>(port) + 1));
    set_env("DANMU_API_VARIANT", "stable");
    set_env("DANMU_API_WORKER", "0");
    set_env("DANMU_API_HOT_RELOAD", "0");
    set_env("TOKEN", auth_token);
    set_env("ADMIN_TOKEN", auth_token);
    set_env("DANMU_API_PUBLIC_PROTO", "http");
    set_env("NODE_PATH", project_dir + "/node_modules");
    set_env("HOME", project_dir);
    set_env("TMPDIR", project_dir + "/tmp");

    std::vector<std::string> args_storage = {"node", entry_script};
    std::vector<char*> argv;
    argv.reserve(args_storage.size());
    for (auto& arg : args_storage) argv.push_back(arg.data());

    __android_log_print(ANDROID_LOG_INFO, TAG, "Starting Node from %s", node_path.c_str());
    const int rc = node_start(static_cast<int>(argv.size()), argv.data());
    set_error(std::string("node::Start returned: ") + std::to_string(rc));
    return rc;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_logvarremote_data_runtime_NativeNodeBridge_lastError(
        JNIEnv* env,
        jobject) {
    const std::string value = get_error();
    return env->NewStringUTF(value.c_str());
}
