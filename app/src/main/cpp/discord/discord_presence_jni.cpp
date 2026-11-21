// SPDX-License-Identifier: LGPL-3.0-or-later
// Official Discord Social SDK JNI transport, enabled only with its licensed AAR.
#include <jni.h>
#include <cstdint>
#include <chrono>
#include <memory>
#include <string>
#include <mutex>
#define DISCORDPP_IMPLEMENTATION
#include "discordpp.h"

namespace {
std::mutex mutex;
std::shared_ptr<discordpp::Client> client;
std::string fromJava(JNIEnv* env, jstring value) {
    if (!value) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) return {};
    std::string text(chars);
    env->ReleaseStringUTFChars(value, chars);
    return text;
}
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_music_aurelis_data_discord_DiscordSocialPresence_nativeInitialize(
    JNIEnv* env, jobject, jstring id) {
    std::lock_guard<std::mutex> lock(mutex);
    try {
        const auto appId = std::stoull(fromJava(env, id));
        if (!client) client = std::make_shared<discordpp::Client>();
        client->SetApplicationId(appId);
        return JNI_TRUE;
    } catch (...) {
        client.reset();
        return JNI_FALSE;
    }
}
extern "C" JNIEXPORT void JNICALL
Java_com_music_aurelis_data_discord_DiscordSocialPresence_nativeUpdate(
    JNIEnv* env, jobject, jstring title, jstring artist, jlong positionMs,
    jlong durationMs) {
    std::lock_guard<std::mutex> lock(mutex);
    if (!client) return;
    discordpp::Activity activity;
    activity.SetType(discordpp::ActivityTypes::Listening);
    activity.SetName("Aurelis Music");
    activity.SetDetails(fromJava(env, title));
    activity.SetState(fromJava(env, artist));
    activity.SetStatusDisplayType(discordpp::StatusDisplayTypes::Name);
    // No image override: Discord displays the application icon registered in
    // its Developer Portal, not a private MediaStore artwork URI.
    if (durationMs > 0) {
        const auto now = std::chrono::duration_cast<std::chrono::seconds>(
            std::chrono::system_clock::now().time_since_epoch()).count();
        discordpp::ActivityTimestamps stamps;
        stamps.SetStart(now - (positionMs / 1000));
        stamps.SetEnd(now + ((durationMs > positionMs ? durationMs - positionMs : 0) / 1000));
        activity.SetTimestamps(stamps);
    }
    client->UpdateRichPresence(activity, [](discordpp::ClientResult) {});
}
extern "C" JNIEXPORT void JNICALL
Java_com_music_aurelis_data_discord_DiscordSocialPresence_nativeRunCallbacks(
    JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(mutex);
    if (client) discordpp::RunCallbacks();
}
extern "C" JNIEXPORT void JNICALL
Java_com_music_aurelis_data_discord_DiscordSocialPresence_nativeClear(
    JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(mutex);
    if (client) client->ClearRichPresence();
}
