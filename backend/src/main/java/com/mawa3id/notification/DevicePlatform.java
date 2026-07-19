package com.mawa3id.notification;

/** The client platform a {@link DeviceToken} was issued on. */
public enum DevicePlatform {
    ANDROID,
    IOS,
    /** Forward-compatible fallback for clients that send an unrecognised value. */
    UNKNOWN
}
